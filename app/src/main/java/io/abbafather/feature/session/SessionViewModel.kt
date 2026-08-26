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
 * Walks one prayer a line at a time, resting at every movement boundary.
 *
 * The session's position is a **step** index rather than a line index, because a breathing pause
 * is a place the session can be in: a rotation taken at a pause has to come back to the pause
 * rather than to the line before it. See `docs/DECISIONS.md`. It lives in [SavedStateHandle], so
 * the reader comes back to the line they were praying.
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

    /**
     * The steps are rebuilt rather than held, because they are a pure function of the prayer and a
     * held copy could only ever disagree with it.
     */
    private fun moveBy(steps: Int) {
        val lastStep = prayer.value?.sessionSteps()?.lastIndex ?: return
        savedStateHandle[StepIndexKey] = (stepIndex.value + steps).coerceIn(0, lastStep)
    }

    private fun Prayer.toUiState(settings: PrayerSettings, stepIndex: Int): SessionUiState {
        val steps = sessionSteps()
        val step = steps.getOrNull(stepIndex) ?: steps.first()
        val isAtEnd = stepIndex >= steps.lastIndex
        return SessionUiState(
            title = title,
            attribution = attribution,
            movementLines = (step as? SessionStep.Line)?.let { linesUpTo(it) }.orEmpty(),
            breathingPause = (step as? SessionStep.Pause)?.let { pause ->
                BreathingPauseUiState(
                    nextMovementHeading = movements[pause.nextMovementIndex].heading,
                    nextMovementNumber = pause.nextMovementIndex + 1,
                    movementCount = movements.size,
                )
            },
            movementTicks = movementTicks(step),
            canGoBack = stepIndex > 0,
            isAtEnd = isAtEnd,
            // Nothing to move on to at the end, so the last step never times out from under "Amen".
            autoAdvanceAfterMillis = settings.sessionPacing.lineDwellMillis
                .takeIf { it > 0L && !isAtEnd },
            keepsScreenOn = settings.keepsScreenOnDuringSession,
            isLoaded = true,
        )
    }

    /**
     * The current movement from its first line down to the one being prayed. A movement is the unit
     * the screen holds: the pause clears it, and the next movement begins on an empty ground.
     */
    private fun Prayer.linesUpTo(step: SessionStep.Line): List<SessionLine> {
        val movement = movementOfLine(step.lineIndex)
        return (movement.firstLineIndex..step.lineIndex).map { lineIndex ->
            SessionLine(text = lines[lineIndex], isCurrent = lineIndex == step.lineIndex)
        }
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

/**
 * Every line of the prayer, with a rest between each movement and the next. Derived from the prayer
 * on demand: it is a pure function of `movements`, so there is nothing to keep in step.
 */
private sealed interface SessionStep {

    data class Line(val lineIndex: Int) : SessionStep

    data class Pause(val nextMovementIndex: Int) : SessionStep
}

private fun Prayer.sessionSteps(): List<SessionStep> = buildList {
    movements.forEach { movement ->
        movement.lines.indices.forEach { position ->
            add(SessionStep.Line(movement.firstLineIndex + position))
        }
        val nextMovement = movements.getOrNull(movement.index + 1)
        if (nextMovement != null) add(SessionStep.Pause(nextMovement.index))
    }
}
