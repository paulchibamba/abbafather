package io.abbafather.feature.session

import androidx.compose.runtime.Immutable

/**
 * The session as the screen draws it: the whole prayer, and which part of it is being prayed now.
 *
 * [steps] is every line and every rest, in order — the column the screen scrolls through — and
 * [activeStepIndex] is the only thing an advance changes. Activeness deliberately does not live on
 * the step: if it did, every advance would build a structurally different list and everything keyed
 * on it would fall over. See `docs/DECISIONS.md`.
 */
@Immutable
data class SessionUiState(
    val title: String = "",
    val attribution: String = "",
    val steps: List<SessionStepUiState> = emptyList(),
    val activeStepIndex: Int = 0,
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

/** One item in the column. [key] is its identity in the list and never changes while it is drawn. */
@Immutable
sealed interface SessionStepUiState {

    val key: String

    data class Line(val lineIndex: Int, val text: String) : SessionStepUiState {
        override val key: String get() = "line-$lineIndex"
    }

    /**
     * The rest between two movements. It names the movement being entered, because a movement is a
     * complete turn of the praying and the next one begins something new.
     */
    data class Pause(
        val nextMovementIndex: Int,
        val nextMovementHeading: String,
        val nextMovementNumber: Int,
        val movementCount: Int,
    ) : SessionStepUiState {
        override val key: String get() = "pause-$nextMovementIndex"
    }
}

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
