package io.abbafather.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.abbafather.domain.model.Prayer
import io.abbafather.domain.model.PrayerTag
import io.abbafather.domain.model.SavedLine
import io.abbafather.domain.repository.PrayerRepository
import io.abbafather.domain.repository.SavedLineRepository
import io.abbafather.domain.usecase.CreatePersonalPrayerFromLineUseCase
import io.abbafather.domain.usecase.SaveLineUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * Reads one catalogue prayer whole and owns the three sheets that open over it: keeping a line, the
 * passages under a movement, and where the prayer came from.
 *
 * Everything a sheet is — which line or movement it is open on, which of the three things the keep
 * sheet is saying, which tags are ticked and whether the whole vocabulary is showing — lives in
 * [SavedStateHandle], so a rotation with a sheet open comes back with the same sheet rather than
 * dropping the reader back into the prayer.
 */
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val savedLineRepository: SavedLineRepository,
    private val saveLine: SaveLineUseCase,
    private val createPersonalPrayerFromLine: CreatePersonalPrayerFromLineUseCase,
    private val clock: Clock,
    prayerRepository: PrayerRepository,
) : ViewModel() {

    /**
     * The route's own argument, read by the name it is declared with on `AbbaRoute.Reader`. The
     * feature reads the key rather than the route type, so nothing under `feature/` depends on the
     * navigation graph that hosts it.
     */
    private val prayerId: String = checkNotNull(savedStateHandle[PrayerIdKey]) {
        "ReaderViewModel was built without a $PrayerIdKey argument"
    }

    private val keepSheetKeys: Flow<KeepSheetKeys> = combine(
        savedStateHandle.getStateFlow(OpenLineKey, NoLineOpen),
        savedStateHandle.getStateFlow(StageKey, ""),
        savedStateHandle.getStateFlow(SelectedTagsKey, "")
            .map { names -> names.split(NameSeparator).filter(String::isNotEmpty) },
        savedStateHandle.getStateFlow(ShowAllTagsKey, false),
        ::KeepSheetKeys,
    )

    private val metadataSheetKeys: Flow<MetadataSheetKeys> = combine(
        savedStateHandle.getStateFlow(OpenScriptureMovementKey, NoMovementOpen),
        savedStateHandle.getStateFlow(ProvenanceOpenKey, false),
        ::MetadataSheetKeys,
    )

    private val keptLines: Flow<List<SavedLine>> = savedLineRepository.observeSavedLines()
        .map { savedLines -> savedLines.filter { it.sourcePrayerId == prayerId } }

    private val _events = Channel<ReaderEvent>(Channel.BUFFERED)
    val events: Flow<ReaderEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<ReaderUiState> = combine(
        prayerRepository.observePrayer(prayerId),
        keptLines,
        keepSheetKeys,
        metadataSheetKeys,
    ) { prayer, keptLines, keepKeys, metadataKeys ->
        val keptLineIndices = keptLines.mapNotNullTo(mutableSetOf()) { it.sourceLineIndex }
        ReaderUiState(
            prayer = prayer,
            keptLineIndices = keptLineIndices,
            keepSheet = prayer?.keepSheetOrNull(keepKeys),
            scriptureSheet = prayer?.scriptureSheetOrNull(metadataKeys.scriptureMovementIndex),
            provenanceSheet = prayer?.takeIf { metadataKeys.isProvenanceOpen }?.let { open ->
                ProvenanceSheetUiState(adaptedTitle = open.title, provenance = open.provenance)
            },
            isLoaded = prayer != null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReaderUiState(),
    )

    fun onAction(action: ReaderAction) {
        when (action) {
            is ReaderAction.SelectLine -> openSheetOn(action.lineIndex)
            ReaderAction.DismissSheet -> closeSheets()
            is ReaderAction.ToggleTag -> toggleTag(action.tag)
            ReaderAction.ShowMoreTags -> savedStateHandle[ShowAllTagsKey] = true
            ReaderAction.KeepLine -> keepOpenLine()
            ReaderAction.ReleaseKeptLine -> releaseOpenLine()
            ReaderAction.GrowIntoPrayer -> growOpenLineIntoPrayer()
            is ReaderAction.OpenScripture -> openScriptureOn(action.movementIndex)
            ReaderAction.OpenProvenance -> openProvenance()
            // Both belong to the navigator alone; the ViewModel keeps no record of leaving.
            ReaderAction.Back, ReaderAction.PrayThis -> Unit
        }
    }

    /**
     * A line already kept opens straight into what it is, so the sheet never offers to keep a line
     * the reader kept last week.
     */
    private fun openSheetOn(lineIndex: Int) {
        val stage = if (lineIndex in uiState.value.keptLineIndices) {
            KeepLineStage.AlreadyKept
        } else {
            KeepLineStage.Keep
        }
        closeSheets()
        savedStateHandle[OpenLineKey] = lineIndex
        savedStateHandle[StageKey] = stage.name
        // A fresh sheet starts ticked with the prayer's own themes, which is what keeping it plain
        // would have recorded anyway.
        savedStateHandle[SelectedTagsKey] = uiState.value.prayer
            ?.tags
            .orEmpty()
            .joinToString(NameSeparator) { it.name }
    }

    private fun openScriptureOn(movementIndex: Int) {
        closeSheets()
        savedStateHandle[OpenScriptureMovementKey] = movementIndex
    }

    private fun openProvenance() {
        closeSheets()
        savedStateHandle[ProvenanceOpenKey] = true
    }

    private fun closeSheets() {
        savedStateHandle[OpenLineKey] = NoLineOpen
        savedStateHandle[StageKey] = ""
        savedStateHandle[SelectedTagsKey] = ""
        savedStateHandle[ShowAllTagsKey] = false
        savedStateHandle[OpenScriptureMovementKey] = NoMovementOpen
        savedStateHandle[ProvenanceOpenKey] = false
    }

    private fun toggleTag(tag: PrayerTag) {
        val selected = savedStateHandle.get<String>(SelectedTagsKey)
            .orEmpty()
            .split(NameSeparator)
            .filter(String::isNotEmpty)
        val toggled = if (tag.name in selected) selected - tag.name else selected + tag.name
        savedStateHandle[SelectedTagsKey] = toggled.joinToString(NameSeparator)
    }

    private fun keepOpenLine() {
        val uiState = uiState.value
        val prayer = uiState.prayer ?: return
        val sheet = uiState.keepSheet ?: return
        viewModelScope.launch {
            saveLine(
                prayer = prayer,
                lineIndex = sheet.lineIndex,
                tags = sheet.tagChips.filter { it.isSelected }.mapTo(mutableSetOf()) { it.tag },
            )
            savedStateHandle[StageKey] = KeepLineStage.Kept.name
        }
    }

    private fun releaseOpenLine() {
        val lineIndex = uiState.value.keepSheet?.lineIndex ?: return
        viewModelScope.launch {
            savedLineFor(lineIndex)?.let { savedLine ->
                savedLineRepository.deleteSavedLine(savedLine.id, deletedAt = clock.millis())
            }
            closeSheets()
        }
    }

    /**
     * The draft is written before the navigator moves, so the compose screen opens onto a row that
     * already exists rather than onto a creation still in flight.
     */
    private fun growOpenLineIntoPrayer() {
        val lineIndex = uiState.value.keepSheet?.lineIndex ?: return
        viewModelScope.launch {
            val savedLine = savedLineFor(lineIndex) ?: return@launch
            val personalPrayer = createPersonalPrayerFromLine(savedLine)
            closeSheets()
            _events.send(ReaderEvent.OpenComposedPrayer(personalPrayer.id))
        }
    }

    private suspend fun savedLineFor(lineIndex: Int): SavedLine? =
        savedLineRepository.observeSavedLines().first().firstOrNull {
            it.sourcePrayerId == prayerId && it.sourceLineIndex == lineIndex
        }

    private fun Prayer.keepSheetOrNull(keys: KeepSheetKeys): KeepLineSheetUiState? {
        if (keys.lineIndex !in lines.indices) return null
        val stage = KeepLineStage.entries.firstOrNull { it.name == keys.stageName } ?: return null
        val offered = offeredTags(keys.isShowingAllTags)
        return KeepLineSheetUiState(
            stage = stage,
            lineIndex = keys.lineIndex,
            line = lines[keys.lineIndex],
            tagChips = offered.map { tag ->
                KeepTagChip(tag = tag, isSelected = tag.name in keys.tagNames)
            },
            canShowMoreTags = offered.size < PrayerTag.entries.size,
        )
    }

    /**
     * The prayer's own tags, which is all the sheet offers until the reader asks for more. "More
     * tags" widens it to the whole vocabulary with the prayer's own still first — those are what
     * the reader is looking for, and the rest are there for the line that means something the
     * prayer as a whole does not.
     */
    private fun Prayer.offeredTags(isShowingAllTags: Boolean): List<PrayerTag> {
        val own = PrayerTag.entries.filter { it in tags }
        return if (isShowingAllTags) own + PrayerTag.entries.filterNot { it in tags } else own
    }

    private fun Prayer.scriptureSheetOrNull(movementIndex: Int): ScriptureSheetUiState? {
        val movement = movements.getOrNull(movementIndex) ?: return null
        return ScriptureSheetUiState(
            movementIndex = movement.index,
            heading = movement.heading,
            themes = movement.themes,
            passages = movement.scriptures,
        )
    }

    private data class KeepSheetKeys(
        val lineIndex: Int,
        val stageName: String,
        val tagNames: List<String>,
        val isShowingAllTags: Boolean,
    )

    private data class MetadataSheetKeys(
        val scriptureMovementIndex: Int,
        val isProvenanceOpen: Boolean,
    )

    private companion object {
        const val PrayerIdKey = "prayerId"
        const val OpenLineKey = "readerOpenLineIndex"
        const val StageKey = "readerKeepSheetStage"
        const val SelectedTagsKey = "readerSelectedTags"
        const val ShowAllTagsKey = "readerShowAllTags"
        const val OpenScriptureMovementKey = "readerOpenScriptureMovement"
        const val ProvenanceOpenKey = "readerProvenanceOpen"
        const val NameSeparator = ","
        const val NoLineOpen = -1
        const val NoMovementOpen = -1
    }
}
