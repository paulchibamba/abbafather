package io.abbafather.feature.about

import androidx.compose.runtime.Immutable
import io.abbafather.domain.model.SessionPacing

/**
 * The one screen that is about the app rather than about a prayer: the two choices the reader has
 * over a session, where the prayers came from, and the licences the type travels under.
 */
@Immutable
data class AboutUiState(
    val sessionPacing: SessionPacing = SessionPacing.Reader,
    val keepsScreenOnDuringSession: Boolean = true,
    val fontLicences: List<FontLicenceUiState> = emptyList(),
    val appVersionName: String = "",
    val isLoaded: Boolean = false,
) {
    /** Every pacing this build offers, in the order they are shown. */
    val pacingChoices: List<SessionPacing> = SessionPacing.entries
}

/**
 * A bundled typeface and its notice. The licence is carried closed and opened on the screen, because
 * it is four thousand words of legal text under a screen about quiet.
 */
@Immutable
data class FontLicenceUiState(
    val fontName: String,
    val copyrightLine: String,
    val text: String,
    val isOpen: Boolean = false,
)

/** What the reader can do here. */
sealed interface AboutAction {

    data class ChoosePacing(val pacing: SessionPacing) : AboutAction

    data class SetKeepsScreenOn(val keepsScreenOn: Boolean) : AboutAction

    /** Open the licence for one font, or close it if it is the one already open. */
    data class ToggleLicence(val fontName: String) : AboutAction

    data object Back : AboutAction
}
