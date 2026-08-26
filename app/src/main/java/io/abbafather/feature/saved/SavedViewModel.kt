package io.abbafather.feature.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.abbafather.domain.model.PrayerTag
import io.abbafather.domain.model.SavedLine
import io.abbafather.domain.repository.SavedLineRepository
import io.abbafather.domain.usecase.CreatePersonalPrayerFromLineUseCase
import io.abbafather.domain.usecase.RecordPrayerOpenedUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * The kept lines. There is nothing to narrow and nothing to open over the list, so this screen holds
 * no state of its own — everything it draws comes from [SavedLineRepository], and a line let go of
 * disappears because the repository says so rather than because the screen removed it.
 */
@HiltViewModel
class SavedViewModel @Inject constructor(
    private val savedLineRepository: SavedLineRepository,
    private val createPersonalPrayerFromLine: CreatePersonalPrayerFromLineUseCase,
    private val recordPrayerOpened: RecordPrayerOpenedUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _events = Channel<SavedEvent>(Channel.BUFFERED)
    val events: Flow<SavedEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<SavedUiState> = savedLineRepository.observeSavedLines()
        .map { savedLines ->
            SavedUiState(savedLines = savedLines.map(SavedLine::toCard), isLoaded = true)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SavedUiState(),
        )

    fun onAction(action: SavedAction) {
        when (action) {
            // Reaching for a prayer from a kept line counts as opening it, as it does on Home.
            is SavedAction.OpenSourcePrayer ->
                viewModelScope.launch { recordPrayerOpened(action.prayerId) }
            is SavedAction.GrowIntoPrayer -> growIntoPrayer(action.savedLineId)
            is SavedAction.ReleaseLine -> viewModelScope.launch {
                savedLineRepository.deleteSavedLine(action.savedLineId, deletedAt = clock.millis())
            }
        }
    }

    /**
     * The draft is written before the navigator moves, so the compose screen opens onto a row that
     * already exists rather than onto a creation still in flight.
     */
    private fun growIntoPrayer(savedLineId: String) {
        viewModelScope.launch {
            val savedLine = savedLineRepository.getSavedLine(savedLineId) ?: return@launch
            val personalPrayer = createPersonalPrayerFromLine(savedLine)
            _events.send(SavedEvent.OpenComposedPrayer(personalPrayer.id))
        }
    }
}

/** Tags in the vocabulary's own order, so two lines tagged the same read the same. */
private fun SavedLine.toCard() = SavedLineCardUiState(
    savedLineId = id,
    line = text,
    sourcePrayerId = sourcePrayerId,
    sourcePrayerTitle = sourcePrayerTitle,
    sourceAttribution = sourceAttribution,
    tags = tags.sortedBy(PrayerTag::ordinal),
    keptAt = createdAt,
)
