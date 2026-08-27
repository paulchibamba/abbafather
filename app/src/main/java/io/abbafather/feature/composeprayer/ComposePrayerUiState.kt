package io.abbafather.feature.composeprayer

import androidx.compose.runtime.Immutable
import io.abbafather.domain.model.PrayerTag

/**
 * What the page draws around the writing: the tags, and whether there is yet anything to keep. The
 * words themselves are deliberately **not** here — see [ComposePrayerDraft]. This value changes when
 * a tag is ticked or when the page first has something in it, and not once per keystroke, so typing
 * never recomposes the picker underneath the reader's hands.
 */
@Immutable
data class ComposePrayerUiState(
    val tagChips: List<ComposeTagChip> = emptyList(),
    /** True while only the ticked tags are showing and the rest of the vocabulary is still hidden. */
    val canShowMoreTags: Boolean = false,
    /** Nothing is kept until something is written; a name on its own is not a prayer. */
    val canKeep: Boolean = false,
    val isLoaded: Boolean = false,
)

/**
 * What the draft held when the page opened it — a blank page, a prayer already written, or a kept
 * line. The fields take their text from this once and edit their own state from there, telling the
 * ViewModel afterwards; nothing flows back into a field while it is being typed in. See
 * `docs/DECISIONS.md`.
 */
@Immutable
data class ComposePrayerDraft(val title: String, val body: String)

@Immutable
data class ComposeTagChip(val tag: PrayerTag, val isSelected: Boolean)

/** What the reader can do while writing. */
sealed interface ComposePrayerAction {

    data class TitleChanged(val title: String) : ComposePrayerAction

    data class BodyChanged(val body: String) : ComposePrayerAction

    data class ToggleTag(val tag: PrayerTag) : ComposePrayerAction

    /** Widens the chips from the ones already ticked to the whole vocabulary. */
    data object ShowMoreTags : ComposePrayerAction

    /** "Keep prayer" — writes the draft to the row it belongs to. */
    data object KeepPrayer : ComposePrayerAction

    data object Back : ComposePrayerAction
}

/**
 * Keeping is a write, so the move away from this screen waits for it: the reader lands back on a
 * shelf that already holds what they wrote rather than on one still catching up.
 */
sealed interface ComposePrayerEvent {

    data object Kept : ComposePrayerEvent
}
