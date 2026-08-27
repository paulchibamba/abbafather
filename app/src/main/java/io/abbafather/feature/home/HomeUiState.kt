package io.abbafather.feature.home

import androidx.compose.runtime.Immutable
import io.abbafather.domain.model.DailyVerse
import io.abbafather.domain.model.Greeting
import io.abbafather.domain.model.Prayer

/**
 * Everything the home screen draws, in one immutable value. [isCatalogueReady] is false only for the
 * first frames while the database opens and seeds; the screen shows the header alone rather than an
 * empty card, because the greeting and the verse are already true.
 */
@Immutable
data class HomeUiState(
    val greeting: Greeting = Greeting.Morning,
    val verse: DailyVerse? = null,
    val suggestedPrayer: Prayer? = null,
    val recentlyPrayed: List<Prayer> = emptyList(),
    val isCatalogueReady: Boolean = false,
)

/** What the reader can do from the home screen. */
sealed interface HomeAction {

    /** Read today's suggested prayer whole before praying it. */
    data class ReadPrayer(val prayerId: String) : HomeAction

    /** Go straight into the line-by-line session. */
    data class BeginSession(val prayerId: String) : HomeAction

    /** The settings, the attribution and the licences. */
    data object OpenAbout : HomeAction
}
