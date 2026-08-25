package io.abbafather.feature.home

import app.cash.turbine.test
import io.abbafather.domain.model.Greeting
import io.abbafather.domain.usecase.GetGreetingUseCase
import io.abbafather.domain.usecase.GetTodaysSuggestedPrayerUseCase
import io.abbafather.domain.usecase.GetTodaysVerseUseCase
import io.abbafather.domain.usecase.RecordPrayerOpenedUseCase
import io.abbafather.testing.FakePrayerRepository
import io.abbafather.testing.MainDispatcherRule
import io.abbafather.testing.testPrayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** A fixed morning, so the greeting and today's suggestion are assertions rather than guesses. */
    private val clock = Clock.fixed(Instant.parse("2026-08-25T08:30:00Z"), ZoneOffset.UTC)
    private val catalogue = (1..12).map { testPrayer(id = "prayer-%02d".format(it), title = "Prayer $it") }
    private val prayerRepository = FakePrayerRepository(catalogue)

    private fun viewModel() = HomeViewModel(
        prayerRepository = prayerRepository,
        getGreeting = GetGreetingUseCase(clock),
        getTodaysVerse = GetTodaysVerseUseCase(clock),
        recordPrayerOpened = RecordPrayerOpenedUseCase(prayerRepository, clock),
        getTodaysSuggestedPrayer = GetTodaysSuggestedPrayerUseCase(prayerRepository, clock),
    )

    /** Skips past the header-only first frame, so a test asserts about a screen that has its content. */
    private val HomeViewModel.loadedStates: Flow<HomeUiState>
        get() = uiState.filter { it.isCatalogueReady }

    @Test
    fun `the greeting and the verse are there before the catalogue is`() {
        val initial = viewModel().uiState.value

        assertEquals(Greeting.Morning, initial.greeting)
        assertNotNull(initial.verse)
        assertFalse(initial.isCatalogueReady)
    }

    @Test
    fun `the catalogue brings today's prayer`() = runTest {
        viewModel().loadedStates.test {
            assertTrue(awaitItem().suggestedPrayer in catalogue)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `today's prayer holds still while the screen is watched`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            val first = awaitItem().suggestedPrayer

            viewModel.onAction(HomeAction.ReadPrayer("prayer-01"))

            assertEquals(first?.id, awaitItem().suggestedPrayer?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `opening a prayer stamps it and lifts it into the recent rows`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            val suggested = awaitItem().suggestedPrayer
            val opened = catalogue.first { it.id != suggested?.id }

            viewModel.onAction(HomeAction.BeginSession(opened.id))

            assertEquals(listOf(opened.id), awaitItem().recentlyPrayed.map { it.id })
            assertEquals(clock.millis(), prayerRepository.getPrayer(opened.id)?.lastOpenedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the most recently opened prayer comes first`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            val suggested = awaitItem().suggestedPrayer
            val (earlier, later) = catalogue.filter { it.id != suggested?.id }.take(2)

            prayerRepository.recordPrayerOpened(earlier.id, openedAt = 1_000L)
            skipItems(1)
            prayerRepository.recordPrayerOpened(later.id, openedAt = 2_000L)

            assertEquals(
                listOf(later.id, earlier.id),
                awaitItem().recentlyPrayed.map { it.id },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `today's prayer never repeats itself in the recent rows`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            val suggested = awaitItem().suggestedPrayer

            prayerRepository.recordPrayerOpened(suggested!!.id, openedAt = 3_000L)

            assertTrue(awaitItem().recentlyPrayed.none { it.id == suggested.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `at most four recent rows are shown`() = runTest {
        catalogue.take(6).forEachIndexed { index, prayer ->
            prayerRepository.recordPrayerOpened(prayer.id, openedAt = (index + 1) * 1_000L)
        }

        viewModel().loadedStates.test {
            assertEquals(4, awaitItem().recentlyPrayed.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
