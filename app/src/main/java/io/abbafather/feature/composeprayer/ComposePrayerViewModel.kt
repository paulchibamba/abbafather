package io.abbafather.feature.composeprayer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.abbafather.domain.model.PersonalPrayer
import io.abbafather.domain.model.PrayerTag
import io.abbafather.domain.repository.PersonalPrayerRepository
import io.abbafather.domain.util.IdGenerator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * Writing a prayer, opened three ways: blank from My prayers, on an existing `personalPrayerId` to
 * go on writing it, or on the `seedText` of a line the reader kept.
 *
 * The draft itself lives in [SavedStateHandle] rather than in memory, because this is the one screen
 * whose loss would cost the reader their own words: a rotation, or the process being killed behind
 * another app, comes back to what they had written. The stored row is read exactly once — guarded by
 * [DraftLoadedKey] — so a draft restored after process death is never overwritten by what Room still
 * holds.
 */
@HiltViewModel
class ComposePrayerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val personalPrayerRepository: PersonalPrayerRepository,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) : ViewModel() {

    /** The route's own arguments, read by the names they are declared with on `AbbaRoute.ComposePrayer`. */
    private val personalPrayerId: String? = savedStateHandle[PersonalPrayerIdKey]
    private val seedText: String? = savedStateHandle[SeedTextKey]

    private val _events = Channel<ComposePrayerEvent>(Channel.BUFFERED)
    val events: Flow<ComposePrayerEvent> = _events.receiveAsFlow()

    private val selectedTags: Flow<List<PrayerTag>> =
        savedStateHandle.getStateFlow(SelectedTagsKey, "").map { names ->
            names.split(NameSeparator).filter(String::isNotEmpty).mapNotNull { name ->
                PrayerTag.entries.firstOrNull { it.name == name }
            }
        }

    val uiState: StateFlow<ComposePrayerUiState> = combine(
        savedStateHandle.getStateFlow(DraftBodyKey, ""),
        selectedTags,
        savedStateHandle.getStateFlow(ShowAllTagsKey, false),
        savedStateHandle.getStateFlow(DraftLoadedKey, false),
    ) { body, tags, isShowingAllTags, isLoaded ->
        val offered = offeredTags(tags, isShowingAllTags)
        ComposePrayerUiState(
            tagChips = offered.map { tag -> ComposeTagChip(tag = tag, isSelected = tag in tags) },
            canShowMoreTags = offered.size < PrayerTag.entries.size,
            // A body that grows by a letter leaves this value equal to the last one, and a
            // `StateFlow` does not re-emit an equal value — so the page is not redrawn as it is
            // typed into.
            canKeep = body.isNotBlank(),
            isLoaded = isLoaded,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ComposePrayerUiState(),
    )

    /**
     * The text the page opens on, emitted once the draft is there. It is read from the handle at
     * that moment rather than followed, so it says what the fields should start with and then holds
     * still — including after a process death the handle was restored from.
     */
    val openingDraft: StateFlow<ComposePrayerDraft?> =
        savedStateHandle.getStateFlow(DraftLoadedKey, false)
            .map { isLoaded ->
                if (!isLoaded) {
                    null
                } else {
                    ComposePrayerDraft(
                        title = savedStateHandle[DraftTitleKey] ?: "",
                        body = savedStateHandle[DraftBodyKey] ?: "",
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

    init {
        loadDraftOnce()
    }

    fun onAction(action: ComposePrayerAction) {
        when (action) {
            is ComposePrayerAction.TitleChanged -> savedStateHandle[DraftTitleKey] = action.title
            is ComposePrayerAction.BodyChanged -> savedStateHandle[DraftBodyKey] = action.body
            is ComposePrayerAction.ToggleTag -> toggleTag(action.tag)
            ComposePrayerAction.ShowMoreTags -> savedStateHandle[ShowAllTagsKey] = true
            ComposePrayerAction.KeepPrayer -> keepPrayer()
            // Leaving belongs to the navigator alone; an unkept draft is dropped on purpose.
            ComposePrayerAction.Back -> Unit
        }
    }

    /**
     * What the page opens on. A prayer already written opens on itself; a kept line opens on the
     * line and a blank line after it, which is where the reader carries on; anything else opens
     * empty. Nothing is read again after this, so the draft outlives the row it came from.
     */
    private fun loadDraftOnce() {
        if (savedStateHandle.get<Boolean>(DraftLoadedKey) == true) return
        if (personalPrayerId == null) {
            // A blank page and a kept line need nothing from Room, so they open at once rather than
            // a frame later — nothing the reader types can be overwritten by a load still in flight.
            openDraftOn(body = seedText?.let { "$it\n\n" }.orEmpty())
            return
        }
        viewModelScope.launch {
            val existing = personalPrayerRepository.getPersonalPrayer(personalPrayerId)
            openDraftOn(
                title = existing?.title.orEmpty(),
                body = existing?.body.orEmpty(),
                tags = existing?.tags.orEmpty(),
            )
        }
    }

    private fun openDraftOn(
        title: String = "",
        body: String = "",
        tags: Set<PrayerTag> = emptySet(),
    ) {
        savedStateHandle[DraftTitleKey] = title
        savedStateHandle[DraftBodyKey] = body
        savedStateHandle[SelectedTagsKey] = tags.joinToString(NameSeparator) { it.name }
        savedStateHandle[DraftLoadedKey] = true
    }

    /**
     * The tags already ticked, which is all the picker shows until the reader asks for more. On a
     * blank page that is nothing at all, and the page stays quiet until they want it.
     *
     * Widened, it is the vocabulary in its own order rather than the ticked ones first: ticking must
     * not move a chip out from under the finger about to tick the next one.
     */
    private fun offeredTags(selected: List<PrayerTag>, isShowingAllTags: Boolean): List<PrayerTag> =
        if (isShowingAllTags) PrayerTag.entries else PrayerTag.entries.filter { it in selected }

    private fun toggleTag(tag: PrayerTag) {
        val selected = tagsInHandle().map { it.name }
        val toggled = if (tag.name in selected) selected - tag.name else selected + tag.name
        savedStateHandle[SelectedTagsKey] = toggled.joinToString(NameSeparator)
    }

    private fun tagsInHandle(): List<PrayerTag> = savedStateHandle.get<String>(SelectedTagsKey)
        .orEmpty()
        .split(NameSeparator)
        .filter(String::isNotEmpty)
        .mapNotNull { name -> PrayerTag.entries.firstOrNull { it.name == name } }

    /**
     * The write. An existing prayer is written back to its own row, keeping the day it was created;
     * a new one is minted here. The move away waits for the write, so the shelf the reader lands on
     * already holds what they wrote.
     */
    private fun keepPrayer() {
        // The draft is read from the handle rather than from `uiState`, which only holds a value
        // while the screen is collecting it. What is kept is what was written, not what was drawn.
        val body: String = savedStateHandle[DraftBodyKey] ?: ""
        if (body.isBlank()) return
        val title: String = savedStateHandle[DraftTitleKey] ?: ""
        viewModelScope.launch {
            val now = clock.millis()
            val existing = personalPrayerId?.let { personalPrayerRepository.getPersonalPrayer(it) }
            personalPrayerRepository.upsertPersonalPrayer(
                PersonalPrayer(
                    id = existing?.id ?: personalPrayerId ?: idGenerator.newId(),
                    title = title.trim(),
                    body = body,
                    tags = tagsInHandle().toSet(),
                    seededFromSavedLineId = existing?.seededFromSavedLineId,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                ),
            )
            _events.send(ComposePrayerEvent.Kept)
        }
    }

    private companion object {
        const val PersonalPrayerIdKey = "personalPrayerId"
        const val SeedTextKey = "seedText"
        const val DraftTitleKey = "composeDraftTitle"
        const val DraftBodyKey = "composeDraftBody"
        const val SelectedTagsKey = "composeSelectedTags"
        const val ShowAllTagsKey = "composeShowAllTags"
        const val DraftLoadedKey = "composeDraftLoaded"
        const val NameSeparator = ","
    }
}
