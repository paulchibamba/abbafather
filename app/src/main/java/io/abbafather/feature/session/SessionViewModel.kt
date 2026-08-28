package io.abbafather.feature.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.abbafather.domain.model.Prayer
import io.abbafather.domain.model.PrayerSettings
import io.abbafather.domain.repository.PrayerRepository
import io.abbafather.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Walks one prayer a line at a time, straight through from the first to the last.
 *
 * The position is a line index into [Prayer.lines] — the same address a kept line holds — and it
 * lives in [SavedStateHandle], so the reader comes back to the line they were praying.
 *
 * The whole prayer is handed to the screen every time and only the index moves, so the list is
 * equal across an advance and nothing that watches it has to be rebuilt.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    prayerRepository: PrayerRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /**
     * The route's own argument, read by the name it is declared with on `AbbaRoute.Session`, so
     * nothing under `feature/` depends on the navigation graph that hosts it.
     */
    private val prayerId: String = checkNotNull(savedStateHandle[PrayerIdKey]) {
        "SessionViewModel was built without a $PrayerIdKey argument"
    }

    private val lineIndex: StateFlow<Int> = savedStateHandle.getStateFlow(LineIndexKey, 0)

    /**
     * Held as state rather than collected twice: moving through the session needs to know how many
     * lines there are, and that is the prayer's own shape.
     */
    private val prayer: StateFlow<Prayer?> = prayerRepository.observePrayer(prayerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<SessionUiState> = combine(
        prayer,
        settingsRepository.observeSettings(),
        lineIndex,
    ) { prayer, settings, lineIndex ->
        prayer?.toUiState(settings, lineIndex) ?: SessionUiState()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SessionUiState(),
    )

    fun onAction(action: SessionAction) {
        when (action) {
            SessionAction.Advance -> moveBy(1)
            SessionAction.GoBack -> moveBy(-1)
            // Both belong to the navigator alone; the session keeps no record of leaving.
            SessionAction.Leave, SessionAction.Amen -> Unit
        }
    }

    private fun moveBy(lines: Int) {
        val lastLine = prayer.value?.lines?.lastIndex ?: return
        savedStateHandle[LineIndexKey] = (lineIndex.value + lines).coerceIn(0, lastLine)
    }

    private fun Prayer.toUiState(settings: PrayerSettings, lineIndex: Int): SessionUiState {
        val activeLineIndex = lineIndex.coerceIn(0, lines.lastIndex)
        val isAtEnd = activeLineIndex >= lines.lastIndex
        return SessionUiState(
            title = title,
            attribution = attribution,
            lines = lines,
            activeLineIndex = activeLineIndex,
            remainingFraction = remainingFractionAt(activeLineIndex),
            canGoBack = activeLineIndex > 0,
            isAtEnd = isAtEnd,
            // Nothing to move on to at the end, so the last line never times out from under "Amen".
            autoAdvanceAfterMillis = settings.sessionPacing.lineDwellMillis
                .takeIf { it > 0L && !isAtEnd },
            keepsScreenOn = settings.keepsScreenOnDuringSession,
            isLoaded = true,
        )
    }

    /**
     * How much of the prayer is still ahead: a whole one at the first line, none of it at the last.
     * A prayer of a single line has nothing ahead of it at all.
     */
    private fun Prayer.remainingFractionAt(activeLineIndex: Int): Float =
        if (lines.size <= 1) 0f else 1f - activeLineIndex.toFloat() / lines.lastIndex

    private companion object {
        const val PrayerIdKey = "prayerId"
        const val LineIndexKey = "sessionLineIndex"
    }
}
