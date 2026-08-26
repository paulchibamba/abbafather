package io.abbafather.feature.library

import androidx.compose.runtime.Immutable
import io.abbafather.domain.model.Prayer
import io.abbafather.domain.model.PrayerFilter
import io.abbafather.domain.model.PrayerPart
import io.abbafather.domain.model.PrayerTag

/**
 * Everything the Library draws. [prayers] is the catalogue already narrowed by both the search field
 * and the tiles; the tiles themselves carry counts taken from the whole catalogue, so a count never
 * moves under the reader's finger while they narrow.
 */
@Immutable
data class LibraryUiState(
    val searchQuery: String = "",
    val filter: PrayerFilter = PrayerFilter(),
    val partTiles: List<PartTile> = emptyList(),
    val tagChips: List<TagChip> = emptyList(),
    val prayers: List<Prayer> = emptyList(),
    val isCatalogueReady: Boolean = false,
) {
    /** True as soon as the reader has typed or tapped anything — which is when "Clear" earns its place. */
    val isNarrowed: Boolean get() = searchQuery.isNotBlank() || !filter.isEmpty

    /** Every prayer belongs to exactly one part, so the tiles already count the catalogue. */
    val catalogueSize: Int get() = partTiles.sumOf { it.prayerCount }
}

/** A part of the collection the prayers came from. */
@Immutable
data class PartTile(val part: PrayerPart, val prayerCount: Int, val isSelected: Boolean)

/** What a prayer is about, narrowed a chip at a time. */
@Immutable
data class TagChip(val tag: PrayerTag, val isSelected: Boolean)

/** What the reader can do in the Library. */
sealed interface LibraryAction {

    data class SearchQueryChanged(val query: String) : LibraryAction

    data class TogglePart(val part: PrayerPart) : LibraryAction

    data class ToggleTag(val tag: PrayerTag) : LibraryAction

    /** Drop the query and every chip at once, back to the whole catalogue. */
    data object ClearNarrowing : LibraryAction

    data class OpenPrayer(val prayerId: String) : LibraryAction
}
