package io.abbafather.feature.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.abbafather.domain.model.Prayer
import io.abbafather.domain.model.PrayerSettings
import io.abbafather.domain.model.SessionStep
import io.abbafather.domain.repository.PrayerRepository
import io.abbafather.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Walks one prayer a step at a time, resting at every movement boundary.
 *
 * The session's position is a **step** index rather than a line index, because a breathing pause
 * is a place the session can be in: a rotation taken at a pause has to come back to the pause
 * rather than to the line before it. See `docs/DECISIONS.md`. It lives in [SavedStateHandle], so
 * the reader comes back to the line they were praying.
 *
 * The whole prayer is handed to the screen every time, and only the index moves — so the list is
 * equal across an advance and nothing that watches it has to be redrawn.
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

    private val stepIndex: StateFlow<Int> = savedStateHandle.getStateFlow(StepIndexKey, 0)

    /**
     * Held as state rather than collected twice: moving through the session needs to know how many
     * steps there are, and that is the prayer's own shape.
     */
    private val prayer: StateFlow<Prayer?> = prayerRepository.observePrayer(prayerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<SessionUiState> = combine(
        prayer,
        settingsRepository.observeSettings(),
        stepIndex,
    ) { prayer, settings, stepIndex ->
        prayer?.toUiState(settings, stepIndex) ?: SessionUiState()
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

    private fun moveBy(steps: Int) {
        val lastStep = prayer.value?.sessionSteps?.lastIndex ?: return
        savedStateHandle[StepIndexKey] = (stepIndex.value + steps).coerceIn(0, lastStep)
    }

    private fun Prayer.toUiState(settings: PrayerSettings, stepIndex: Int): SessionUiState {
        val activeStepIndex = stepIndex.coerceIn(0, sessionSteps.lastIndex)
        val isAtEnd = activeStepIndex >= sessionSteps.lastIndex
        return SessionUiState(
            title = title,
            attribution = attribution,
            steps = sessionSteps.map { step -> toStepUiState(step) },
            activeStepIndex = activeStepIndex,
            movementTicks = movementTicks(sessionSteps[activeStepIndex]),
            canGoBack = activeStepIndex > 0,
            isAtEnd = isAtEnd,
            // Nothing to move on to at the end, so the last step never times out from under "Amen".
            autoAdvanceAfterMillis = settings.sessionPacing.lineDwellMillis
                .takeIf { it > 0L && !isAtEnd },
            keepsScreenOn = settings.keepsScreenOnDuringSession,
            isLoaded = true,
        )
    }

    private fun Prayer.toStepUiState(step: SessionStep): SessionStepUiState = when (step) {
        is SessionStep.Line -> SessionStepUiState.Line(
            lineIndex = step.lineIndex,
            text = lines[step.lineIndex],
        )

        is SessionStep.Pause -> SessionStepUiState.Pause(
            nextMovementIndex = step.nextMovementIndex,
            nextMovementHeading = movements[step.nextMovementIndex].heading,
            nextMovementNumber = step.nextMovementIndex + 1,
            movementCount = movements.size,
        )
    }

    private fun Prayer.movementTicks(step: SessionStep): List<MovementTick> {
        val currentMovementIndex = when (step) {
            is SessionStep.Line -> movementOfLine(step.lineIndex).index
            // A pause belongs to the movement it opens onto, so the tick moves with the reader.
            is SessionStep.Pause -> step.nextMovementIndex
        }
        return movements.map { movement ->
            when {
                movement.index < currentMovementIndex -> MovementTick.Spent
                movement.index == currentMovementIndex -> MovementTick.Current
                else -> MovementTick.ToCome
            }
        }
    }

    private companion object {
        const val PrayerIdKey = "prayerId"
        const val StepIndexKey = "sessionStepIndex"
    }
}
