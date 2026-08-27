package io.abbafather.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.abbafather.domain.repository.PrayerRepository
import io.abbafather.domain.usecase.GetGreetingUseCase
import io.abbafather.domain.usecase.GetTodaysSuggestedPrayerUseCase
import io.abbafather.domain.usecase.GetTodaysVerseUseCase
import io.abbafather.domain.usecase.RecordPrayerOpenedUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prayerRepository: PrayerRepository,
    private val getGreeting: GetGreetingUseCase,
    private val getTodaysVerse: GetTodaysVerseUseCase,
    private val recordPrayerOpened: RecordPrayerOpenedUseCase,
    getTodaysSuggestedPrayer: GetTodaysSuggestedPrayerUseCase,
) : ViewModel() {

    /**
     * The greeting and the verse are read on every emission rather than captured once, so a screen
     * left open across the turn of the hour or of the day catches up the next time anything moves.
     */
    val uiState: StateFlow<HomeUiState> = combine(
        getTodaysSuggestedPrayer(),
        prayerRepository.observeRecentlyOpenedPrayers(RecentPrayerCount + 1),
    ) { suggestedPrayer, recentlyPrayed ->
        HomeUiState(
            greeting = getGreeting(),
            verse = getTodaysVerse(),
            suggestedPrayer = suggestedPrayer,
            // Today's prayer already has a card of its own; a row repeating it would read as noise.
            recentlyPrayed = recentlyPrayed
                .filterNot { it.id == suggestedPrayer?.id }
                .take(RecentPrayerCount),
            isCatalogueReady = true,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(greeting = getGreeting(), verse = getTodaysVerse()),
    )

    /**
     * Both moves open a prayer, so both stamp it — the recent rows are the prayers the reader
     * reached for, whether they read it or prayed it.
     */
    fun onAction(action: HomeAction) {
        val prayerId = when (action) {
            is HomeAction.ReadPrayer -> action.prayerId
            is HomeAction.BeginSession -> action.prayerId
            // Opening the About screen is the navigator's alone: nothing was prayed.
            HomeAction.OpenAbout -> return
        }
        viewModelScope.launch { recordPrayerOpened(prayerId) }
    }

    private companion object {
        /** Four rows is what the design shows below the suggested card without the screen crowding. */
        const val RecentPrayerCount = 4
    }
}
