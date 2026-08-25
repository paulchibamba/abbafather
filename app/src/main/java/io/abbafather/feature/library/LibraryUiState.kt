package io.abbafather.feature.library

import androidx.compose.runtime.Immutable
import io.abbafather.domain.model.Prayer
import io.abbafather.domain.model.PrayerFilter
import io.abbafather.domain.model.PrayerGroup
import io.abbafather.domain.model.PrayerKind
import io.abbafather.domain.model.PrayerTheme

/**
 * Everything the Library draws. [prayers] is the catalogue already narrowed by both the search field
 * and the tiles; the tiles themselves carry counts taken from the whole catalogue, so a count never
 * moves under the reader's finger while they narrow.
 */
@Immutable
data class LibraryUiState(
    val searchQuery: String = "",
    val filter: PrayerFilter = PrayerFilter(),
    val kindTiles: List<KindTile> = emptyList(),
    val groupTiles: List<GroupTile> = emptyList(),
    val themeChips: List<ThemeChip> = emptyList(),
    val prayers: List<Prayer> = emptyList(),
    val isCatalogueReady: Boolean = false,
) {
    /** True as soon as the reader has typed or tapped anything — which is when "Clear" earns its place. */
    val isNarrowed: Boolean get() = searchQuery.isNotBlank() || !filter.isEmpty
}

/** An occasion the reader reaches a prayer for. */
@Immutable
data class KindTile(val kind: PrayerKind, val prayerCount: Int, val isSelected: Boolean)

/** A place prayers come from — the Book of Common Prayer, the Psalms, the Puritans. */
@Immutable
data class GroupTile(val group: PrayerGroup, val prayerCount: Int, val isSelected: Boolean)

/** What a prayer is about, narrowed a chip at a time. */
@Immutable
data class ThemeChip(val theme: PrayerTheme, val isSelected: Boolean)

/** What the reader can do in the Library. */
sealed interface LibraryAction {

    data class SearchQueryChanged(val query: String) : LibraryAction

    data class ToggleKind(val kind: PrayerKind) : LibraryAction

    data class ToggleGroup(val group: PrayerGroup) : LibraryAction

    data class ToggleTheme(val theme: PrayerTheme) : LibraryAction

    /** Drop the query and every chip at once, back to the whole catalogue. */
    data object ClearNarrowing : LibraryAction

    data class OpenPrayer(val prayerId: String) : LibraryAction
}
