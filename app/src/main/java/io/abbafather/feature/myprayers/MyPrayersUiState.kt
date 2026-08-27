package io.abbafather.feature.myprayers

import androidx.compose.runtime.Immutable

/**
 * The prayers the reader wrote, most recently touched first. [isLoaded] separates a reader who has
 * written nothing from one whose prayers have not arrived from Room yet — the first is told so, the
 * second is shown nothing rather than an empty state that is about to be wrong.
 */
@Immutable
data class MyPrayersUiState(
    val prayers: List<MyPrayerCardUiState> = emptyList(),
    val isLoaded: Boolean = false,
) {
    val isEmpty: Boolean get() = isLoaded && prayers.isEmpty()
}

/**
 * One written prayer as a card. A draft grown from a kept line arrives here the moment it is minted,
 * which is why [excerpt] can be the whole of what is written so far and [title] can be blank.
 */
@Immutable
data class MyPrayerCardUiState(
    val personalPrayerId: String,
    val title: String,
    val excerpt: String,
    val lastTouchedAt: Long,
    /** True while this card is asking whether the reader means it. */
    val isAwaitingDeleteConfirmation: Boolean = false,
) {
    /** A prayer written but not yet named. The card says so rather than showing an empty line. */
    val hasTitle: Boolean get() = title.isNotBlank()
}

/** What the reader can do with the prayers they wrote. */
sealed interface MyPrayersAction {

    /** Open a prayer already written, to go on writing it. */
    data class OpenPrayer(val personalPrayerId: String) : MyPrayersAction

    /** The round add button: a blank page. */
    data object WriteNewPrayer : MyPrayersAction

    /**
     * "Delete" — which only asks. This app holds prayers people wrote, so deleting takes two taps
     * and the question is asked on the card itself rather than in a dialog.
     */
    data class AskToDelete(val personalPrayerId: String) : MyPrayersAction

    data class ConfirmDelete(val personalPrayerId: String) : MyPrayersAction

    data object CancelDelete : MyPrayersAction
}
