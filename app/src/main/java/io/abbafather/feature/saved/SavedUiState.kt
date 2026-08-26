package io.abbafather.feature.saved

import androidx.compose.runtime.Immutable
import io.abbafather.domain.model.PrayerTag

/**
 * The kept lines, newest first, as a journal reads. [isLoaded] separates a reader who has kept
 * nothing yet from one whose lines have not arrived from Room — the first is told so, the second is
 * shown nothing rather than an empty state that is about to be wrong.
 */
@Immutable
data class SavedUiState(
    val savedLines: List<SavedLineCardUiState> = emptyList(),
    val isLoaded: Boolean = false,
) {
    val isEmpty: Boolean get() = isLoaded && savedLines.isEmpty()
}

/**
 * One kept line, carrying its own copy of where it came from. The source is what the line was kept
 * from rather than a lookup into the catalogue, so a line still says where it came from even if the
 * prayer behind it has moved on.
 */
@Immutable
data class SavedLineCardUiState(
    val savedLineId: String,
    val line: String,
    val sourcePrayerId: String?,
    val sourcePrayerTitle: String?,
    val sourceAttribution: String?,
    val tags: List<PrayerTag> = emptyList(),
    val keptAt: Long,
) {
    /** A line grown out of the catalogue can be read in its prayer; one without a source cannot. */
    val canOpenSourcePrayer: Boolean get() = sourcePrayerId != null && sourcePrayerTitle != null
}

/** What the reader can do with a line they kept. */
sealed interface SavedAction {

    /** The way back to the prayer the line was kept from. */
    data class OpenSourcePrayer(val prayerId: String) : SavedAction

    /** "Make it my prayer" — the kept line becomes the opening of something the reader writes. */
    data class GrowIntoPrayer(val savedLineId: String) : SavedAction

    /** "Let it go" — the line stops being kept. */
    data class ReleaseLine(val savedLineId: String) : SavedAction
}

/**
 * Growing a line mints the draft before there is an id to navigate to, so that one move arrives as
 * an event rather than from the action itself.
 */
sealed interface SavedEvent {

    data class OpenComposedPrayer(val personalPrayerId: String) : SavedEvent
}
