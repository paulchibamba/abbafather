package io.abbafather.feature.reader

import androidx.compose.runtime.Immutable
import io.abbafather.domain.model.Prayer
import io.abbafather.domain.model.PrayerTag

/**
 * The prayer read whole, plus whatever the keep-a-line sheet is currently saying. [keepSheet] is
 * null exactly when no sheet is open, so the screen never has to ask a second question about it.
 */
@Immutable
data class ReaderUiState(
    val prayer: Prayer? = null,
    val keptLineIndices: Set<Int> = emptySet(),
    val keepSheet: KeepLineSheetUiState? = null,
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

/** What the reader can do while reading a prayer. */
sealed interface ReaderAction {

    data object Back : ReaderAction

    data object PrayThis : ReaderAction

    /** Tapping a line opens the sheet on it. */
    data class SelectLine(val lineIndex: Int) : ReaderAction

    data object DismissSheet : ReaderAction

    data class ToggleTag(val tag: PrayerTag) : ReaderAction

    data object KeepLine : ReaderAction

    /** Let go of a line kept earlier. */
    data object ReleaseKeptLine : ReaderAction

    /** "Make it my prayer" — the kept line becomes the opening of something the reader writes. */
    data object GrowIntoPrayer : ReaderAction
}

/**
 * A move the ViewModel can only ask for once it has done some work — growing a line into a prayer
 * mints the draft first, and only then is there an id to navigate to.
 */
sealed interface ReaderEvent {

    data class OpenComposedPrayer(val personalPrayerId: String) : ReaderEvent
}
