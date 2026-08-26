package io.abbafather.feature.session

import androidx.compose.runtime.Immutable

/**
 * The session as the screen needs it: the movement being prayed so far, or the pause between two
 * movements, and the ticks saying how far through the prayer this is.
 *
 * [breathingPause] is null exactly when a line is being prayed, so the screen shows one or the other
 * without asking a second question.
 */
@Immutable
data class SessionUiState(
    val title: String = "",
    val attribution: String = "",
    /** The current movement from its first line down to the one being prayed now. */
    val movementLines: List<SessionLine> = emptyList(),
    val breathingPause: BreathingPauseUiState? = null,
    val movementTicks: List<MovementTick> = emptyList(),
    val canGoBack: Boolean = false,
    /** True at the end of the prayer, where the only move left is "Amen". */
    val isAtEnd: Boolean = false,
    /**
     * How long to rest here before moving on by itself, or null when the reader sets the pace. The
     * timer belongs to the composition rather than to the ViewModel — see `docs/DECISIONS.md`.
     */
    val autoAdvanceAfterMillis: Long? = null,
    val keepsScreenOn: Boolean = true,
    val isLoaded: Boolean = false,
)

/** A line on the session screen. Only one is [isCurrent]; the others are already prayed. */
@Immutable
data class SessionLine(val text: String, val isCurrent: Boolean)

/**
 * Where the session rests between two movements. It names the movement being entered, because a
 * movement is a complete turn of the praying and the next one begins something new.
 */
@Immutable
data class BreathingPauseUiState(
    val nextMovementHeading: String,
    val nextMovementNumber: Int,
    val movementCount: Int,
)

/** One movement's worth of progress. Ticks count movements — twenty-nine of them would be noise. */
enum class MovementTick { Spent, Current, ToCome }

/** What the reader can do while praying. */
sealed interface SessionAction {

    /** On to the next line, or out of a breathing pause into the movement it named. */
    data object Advance : SessionAction

    data object GoBack : SessionAction

    /** Leaving before the end. The prayer is not finished and nothing is recorded. */
    data object Leave : SessionAction

    /** The end of the prayer. */
    data object Amen : SessionAction
}
