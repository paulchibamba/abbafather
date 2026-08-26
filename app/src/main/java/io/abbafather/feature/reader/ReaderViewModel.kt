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
 * Reads one catalogue prayer whole and owns the keep-a-line sheet over it.
 *
 * Everything the sheet is — which line it is open on, which of the three things it is saying, and
 * which themes are ticked — lives in [SavedStateHandle], so a rotation with the sheet open comes
 * back with the same sheet on the same line rather than dropping the reader back into the prayer.
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

    private val openLineIndex: StateFlow<Int> = savedStateHandle.getStateFlow(OpenLineKey, NoLineOpen)
    private val sheetStage: StateFlow<String> = savedStateHandle.getStateFlow(StageKey, "")
    private val selectedTagNames: Flow<List<String>> = savedStateHandle
        .getStateFlow(SelectedTagsKey, "")
        .map { names -> names.split(NameSeparator).filter(String::isNotEmpty) }

    private val keptLines: Flow<List<SavedLine>> = savedLineRepository.observeSavedLines()
        .map { savedLines -> savedLines.filter { it.sourcePrayerId == prayerId } }

    private val _events = Channel<ReaderEvent>(Channel.BUFFERED)
    val events: Flow<ReaderEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<ReaderUiState> = combine(
        prayerRepository.observePrayer(prayerId),
        keptLines,
        openLineIndex,
        sheetStage,
        selectedTagNames,
    ) { prayer, keptLines, openLineIndex, stageName, tagNames ->
        val keptLineIndices = keptLines.mapNotNullTo(mutableSetOf()) { it.sourceLineIndex }
        ReaderUiState(
            prayer = prayer,
            keptLineIndices = keptLineIndices,
            keepSheet = prayer?.keepSheetOrNull(openLineIndex, stageName, tagNames),
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
            ReaderAction.DismissSheet -> closeSheet()
            is ReaderAction.ToggleTag -> toggleTag(action.tag)
            ReaderAction.KeepLine -> keepOpenLine()
            ReaderAction.ReleaseKeptLine -> releaseOpenLine()
            ReaderAction.GrowIntoPrayer -> growOpenLineIntoPrayer()
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
        savedStateHandle[OpenLineKey] = lineIndex
        savedStateHandle[StageKey] = stage.name
        // A fresh sheet starts ticked with the prayer's own themes, which is what keeping it plain
        // would have recorded anyway.
        savedStateHandle[SelectedTagsKey] = uiState.value.prayer
            ?.tags
            .orEmpty()
            .joinToString(NameSeparator) { it.name }
    }

    private fun closeSheet() {
        savedStateHandle[OpenLineKey] = NoLineOpen
        savedStateHandle[StageKey] = ""
        savedStateHandle[SelectedTagsKey] = ""
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
            closeSheet()
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
            closeSheet()
            _events.send(ReaderEvent.OpenComposedPrayer(personalPrayer.id))
        }
    }

    private suspend fun savedLineFor(lineIndex: Int): SavedLine? =
        savedLineRepository.observeSavedLines().first().firstOrNull {
            it.sourcePrayerId == prayerId && it.sourceLineIndex == lineIndex
        }

    private fun Prayer.keepSheetOrNull(
        openLineIndex: Int,
        stageName: String,
        tagNames: List<String>,
    ): KeepLineSheetUiState? {
        if (openLineIndex !in lines.indices) return null
        val stage = KeepLineStage.entries.firstOrNull { it.name == stageName } ?: return null
        return KeepLineSheetUiState(
            stage = stage,
            lineIndex = openLineIndex,
            line = lines[openLineIndex],
            // The prayer's own tags, ticked as they came. Task 9 gives the reader the rest.
            tagChips = tags.map { tag -> KeepTagChip(tag = tag, isSelected = tag.name in tagNames) },
        )
    }

    private companion object {
        const val PrayerIdKey = "prayerId"
        const val OpenLineKey = "readerOpenLineIndex"
        const val StageKey = "readerKeepSheetStage"
        const val SelectedTagsKey = "readerSelectedTags"
        const val NameSeparator = ","
        const val NoLineOpen = -1
    }
}