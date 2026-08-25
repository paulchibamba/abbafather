package io.abbafather.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.abbafather.domain.model.Prayer
import io.abbafather.domain.model.PrayerFilter
import io.abbafather.domain.model.PrayerPart
import io.abbafather.domain.model.PrayerTag
import io.abbafather.domain.repository.PrayerRepository
import io.abbafather.domain.usecase.FilterPrayersUseCase
import io.abbafather.domain.usecase.RecordPrayerOpenedUseCase
import io.abbafather.domain.usecase.SearchPrayersUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The Library narrows the catalogue two ways at once: the search field and the tiles. Both live in
 * [SavedStateHandle], so a rotation — or the process being killed behind another app — brings the
 * reader back to the same narrowed shelf rather than to the whole catalogue.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val recordPrayerOpened: RecordPrayerOpenedUseCase,
    prayerRepository: PrayerRepository,
    searchPrayers: SearchPrayersUseCase,
    filterPrayers: FilterPrayersUseCase,
) : ViewModel() {

    private val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow(SearchQueryKey, "")

    private val filter: Flow<PrayerFilter> = combine(
        savedStateHandle.selectedNames(SelectedPartsKey),
        savedStateHandle.selectedNames(SelectedTagsKey),
    ) { partNames, tagNames ->
        PrayerFilter(
            parts = partNames.mapNotNullTo(mutableSetOf()) { name -> PrayerPart.entries.byName(name) },
            tags = tagNames.mapNotNullTo(mutableSetOf()) { name -> PrayerTag.entries.byName(name) },
        )
    }

    /**
     * The two narrowings are folded together before the catalogue is read, so one change of the
     * query or of a chip produces exactly one state — never a frame carrying the new query beside
     * the old shelf.
     */
    val uiState: StateFlow<LibraryUiState> = combine(searchQuery, filter, ::LibraryNarrowing)
        .flatMapLatest { narrowing ->
            combine(
                prayerRepository.observePrayers(),
                searchPrayers(narrowing.query),
                filterPrayers(narrowing.filter),
            ) { catalogue, matchingQuery, matchingFilter ->
                narrowing.toUiState(catalogue, matchingQuery, matchingFilter)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryUiState(searchQuery = savedStateHandle[SearchQueryKey] ?: ""),
        )

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.SearchQueryChanged -> savedStateHandle[SearchQueryKey] = action.query
            is LibraryAction.TogglePart -> savedStateHandle.toggle(SelectedPartsKey, action.part.name)
            is LibraryAction.ToggleTag -> savedStateHandle.toggle(SelectedTagsKey, action.tag.name)
            LibraryAction.ClearNarrowing -> {
                savedStateHandle[SearchQueryKey] = ""
                savedStateHandle[SelectedPartsKey] = ""
                savedStateHandle[SelectedTagsKey] = ""
            }
            // Reaching for a prayer from the shelf counts as opening it, exactly as it does on Home.
            is LibraryAction.OpenPrayer -> viewModelScope.launch { recordPrayerOpened(action.prayerId) }
        }
    }

    private companion object {
        const val SearchQueryKey = "librarySearchQuery"
        const val SelectedPartsKey = "librarySelectedParts"
        const val SelectedTagsKey = "librarySelectedTags"
    }
}

/**
 * A selection is kept as its enum names joined by [NameSeparator] rather than as a collection,
 * because a `String` is a type `SavedStateHandle` can always write into a saved state bundle.
 */
private const val NameSeparator = ","

private fun SavedStateHandle.selectedNames(key: String): Flow<List<String>> =
    getStateFlow(key, "").map { names -> names.split(NameSeparator).filter(String::isNotEmpty) }

private fun SavedStateHandle.toggle(key: String, name: String) {
    val selected = get<String>(key).orEmpty().split(NameSeparator).filter(String::isNotEmpty)
    val toggled = if (name in selected) selected - name else selected + name
    set(key, toggled.joinToString(NameSeparator))
}

/** The query and the chips as one value, so they can be applied to the catalogue in one step. */
private data class LibraryNarrowing(val query: String, val filter: PrayerFilter) {

    fun toUiState(
        catalogue: List<Prayer>,
        matchingQuery: List<Prayer>,
        matchingFilter: List<Prayer>,
    ): LibraryUiState {
        val idsMatchingFilter = matchingFilter.mapTo(HashSet(matchingFilter.size)) { it.id }
        return LibraryUiState(
            searchQuery = query,
            filter = filter,
            partTiles = catalogue.countsBy(Prayer::part).map { (part, count) ->
                PartTile(part = part, prayerCount = count, isSelected = part in filter.parts)
            },
            tagChips = catalogue.tagsPresent().map { tag ->
                TagChip(tag = tag, isSelected = tag in filter.tags)
            },
            // The two narrowings agree rather than compete: a prayer is shown when it survives both.
            prayers = matchingQuery.filter { it.id in idsMatchingFilter },
            isCatalogueReady = catalogue.isNotEmpty(),
        )
    }
}

private fun <T : Enum<T>> List<T>.byName(name: String): T? = firstOrNull { it.name == name }

/** Enum order, so the tiles keep the order they are declared in rather than a count order. */
private fun <T : Enum<T>> List<Prayer>.countsBy(property: (Prayer) -> T): List<Pair<T, Int>> =
    groupingBy(property).eachCount()
        .toList()
        .sortedBy { (value, _) -> value.ordinal }

/** Only the tags the catalogue actually uses, in the vocabulary's own order. */
private fun List<Prayer>.tagsPresent(): List<PrayerTag> =
    flatMapTo(mutableSetOf()) { it.tags }.sortedBy(PrayerTag::ordinal)
