package io.abbafather.feature.reader

import androidx.compose.runtime.Immutable
import io.abbafather.domain.model.Prayer
import io.abbafather.domain.model.PrayerProvenance
import io.abbafather.domain.model.PrayerTag
import io.abbafather.domain.model.ScriptureReference

/**
 * The prayer read whole, plus whichever sheet is currently open over it. At most one of the three
 * sheet fields is ever non-null, so the screen never has to decide which of two sheets wins.
 */
@Immutable
data class ReaderUiState(
    val prayer: Prayer? = null,
    val keptLineIndices: Set<Int> = emptySet(),
    val keepSheet: KeepLineSheetUiState? = null,
    val scriptureSheet: ScriptureSheetUiState? = null,
    val provenanceSheet: ProvenanceSheetUiState? = null,
    val isLoaded: Boolean = false,
)

/**
 * The sheet is one field that says three different things, depending on what the tapped line
 * already is to the reader.
 */
@Immutable
data class KeepLineSheetUiState(
    val stage: KeepLineStage,
    val lineIndex: Int,
    val line: String,
    val tagChips: List<KeepTagChip> = emptyList(),
    /** True while [tagChips] is the prayer's own tags alone and the rest are still hidden. */
    val canShowMoreTags: Boolean = false,
)

enum class KeepLineStage {
    /** A line the reader has just tapped: choose its themes and keep it. */
    Keep,

    /** The line has this moment been kept — the sheet says so, and offers to grow it. */
    Kept,

    /** A line kept on some earlier reading: it can be grown, or let go of. */
    AlreadyKept,
}

@Immutable
data class KeepTagChip(val tag: PrayerTag, val isSelected: Boolean)

/**
 * What one movement rests on: what it holds theologically, and the passages it was written from.
 * Asked for from the end of the movement, never shown while the prayer is being read.
 */
@Immutable
data class ScriptureSheetUiState(
    val movementIndex: Int,
    val heading: String,
    val themes: List<String>,
    val passages: List<ScriptureReference>,
)

/** Where this prayer came from before it was ours, said plainly. */
@Immutable
data class ProvenanceSheetUiState(
    val adaptedTitle: String,
    val provenance: PrayerProvenance,
)

/** What the reader can do while reading a prayer. */
sealed interface ReaderAction {

    data object Back : ReaderAction

    data object PrayThis : ReaderAction

    /** Tapping a line opens the sheet on it. */
    data class SelectLine(val lineIndex: Int) : ReaderAction

    /** Closes whichever sheet is open. */
    data object DismissSheet : ReaderAction

    data class ToggleTag(val tag: PrayerTag) : ReaderAction

    /** Widens the keep sheet's chips from the prayer's own tags to the whole vocabulary. */
    data object ShowMoreTags : ReaderAction

    data object KeepLine : ReaderAction

    /** Let go of a line kept earlier. */
    data object ReleaseKeptLine : ReaderAction

    /** "Make it my prayer" — the kept line becomes the opening of something the reader writes. */
    data object GrowIntoPrayer : ReaderAction

    /** The passages under one movement, asked for from the end of it. */
    data class OpenScripture(val movementIndex: Int) : ReaderAction

    /** "About this prayer", beside the byline. */
    data object OpenProvenance : ReaderAction
}

/**
 * A move the ViewModel can only ask for once it has done some work — growing a line into a prayer
 * mints the draft first, and only then is there an id to navigate to.
 */
sealed interface ReaderEvent {

    data class OpenComposedPrayer(val personalPrayerId: String) : ReaderEvent
}
