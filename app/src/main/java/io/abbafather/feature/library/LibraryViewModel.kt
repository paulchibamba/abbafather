package io.abbafather.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.abbafather.domain.model.Prayer
import io.abbafather.domain.model.PrayerFilter
import io.abbafather.domain.model.PrayerGroup
import io.abbafather.domain.model.PrayerKind
import io.abbafather.domain.model.PrayerTheme
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
        savedStateHandle.selectedNames(SelectedGroupsKey),
        savedStateHandle.selectedNames(SelectedKindsKey),
        savedStateHandle.selectedNames(SelectedThemesKey),
    ) { groupNames, kindNames, themeNames ->
        PrayerFilter(
            groups = groupNames.mapNotNullTo(mutableSetOf()) { name -> PrayerGroup.entries.byName(name) },
            kinds = kindNames.mapNotNullTo(mutableSetOf()) { name -> PrayerKind.entries.byName(name) },
            themes = themeNames.mapNotNullTo(mutableSetOf()) { name -> PrayerTheme.entries.byName(name) },
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
            is LibraryAction.ToggleKind -> savedStateHandle.toggle(SelectedKindsKey, action.kind.name)
            is LibraryAction.ToggleGroup -> savedStateHandle.toggle(SelectedGroupsKey, action.group.name)
            is LibraryAction.ToggleTheme -> savedStateHandle.toggle(SelectedThemesKey, action.theme.name)
            LibraryAction.ClearNarrowing -> {
                savedStateHandle[SearchQueryKey] = ""
                savedStateHandle[SelectedGroupsKey] = ""
                savedStateHandle[SelectedKindsKey] = ""
                savedStateHandle[SelectedThemesKey] = ""
            }
            // Reaching for a prayer from the shelf counts as opening it, exactly as it does on Home.
            is LibraryAction.OpenPrayer -> viewModelScope.launch { recordPrayerOpened(action.prayerId) }
        }
    }

    private companion object {
        const val SearchQueryKey = "librarySearchQuery"
        const val SelectedGroupsKey = "librarySelectedGroups"
        const val SelectedKindsKey = "librarySelectedKinds"
        const val SelectedThemesKey = "librarySelectedThemes"
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
            kindTiles = catalogue.countsBy(Prayer::kind).map { (kind, count) ->
                KindTile(kind = kind, prayerCount = count, isSelected = kind in filter.kinds)
            },
            groupTiles = catalogue.countsBy(Prayer::group).map { (group, count) ->
                GroupTile(group = group, prayerCount = count, isSelected = group in filter.groups)
            },
            themeChips = catalogue.themesPresent().map { theme ->
                ThemeChip(theme = theme, isSelected = theme in filter.themes)
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

private fun List<Prayer>.themesPresent(): List<PrayerTheme> =
    flatMapTo(mutableSetOf()) { it.themes }.sortedBy(PrayerTheme::ordinal)
