package io.abbafather.feature.session

import androidx.compose.runtime.Immutable

/**
 * The session as the screen draws it: the whole prayer, and which line is being prayed now.
 *
 * The prayer runs straight through — the movements are still what it is built of, and the reader
 * screen marks the rests between them, but a session prays it as one continuous thing. So [lines]
 * is the prayer's own flat line list and [activeLineIndex] is the only thing an advance changes.
 * Activeness deliberately does not live on the line: if it did, every advance would build a
 * structurally different list and everything keyed on it would fall over. See `docs/DECISIONS.md`.
 */
@Immutable
data class SessionUiState(
    val title: String = "",
    val attribution: String = "",
    val lines: List<String> = emptyList(),
    val activeLineIndex: Int = 0,
    val movementProgress: List<MovementProgress> = emptyList(),
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

/**
 * One movement's worth of progress: how much of it has been prayed, and whether it is the one being
 * prayed now. Progress is counted in movements rather than in lines because twenty-nine ticks would
 * be noise — but a movement fills as it is prayed, so a long one still moves under the reader.
 */
@Immutable
data class MovementProgress(val prayedFraction: Float, val isCurrent: Boolean)

/** What the reader can do while praying. */
sealed interface SessionAction {

    data object Advance : SessionAction

    data object GoBack : SessionAction

    /** Leaving before the end. The prayer is not finished and nothing is recorded. */
    data object Leave : SessionAction

    /** The end of the prayer. */
    data object Amen : SessionAction
}
