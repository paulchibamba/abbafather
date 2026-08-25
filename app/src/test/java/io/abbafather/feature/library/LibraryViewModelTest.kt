package io.abbafather.feature.library

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.abbafather.domain.model.PrayerGroup
import io.abbafather.domain.model.PrayerKind
import io.abbafather.domain.model.PrayerTheme
import io.abbafather.domain.usecase.FilterPrayersUseCase
import io.abbafather.domain.usecase.RecordPrayerOpenedUseCase
import io.abbafather.domain.usecase.SearchPrayersUseCase
import io.abbafather.testing.FakePrayerRepository
import io.abbafather.testing.MainDispatcherRule
import io.abbafather.testing.testPrayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class LibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-08-25T08:30:00Z"), ZoneOffset.UTC)

    private val catalogue = listOf(
        testPrayer(
            id = "bcp-peace",
            title = "A Collect for Peace",
            author = "Book of Common Prayer, 1662",
            kind = PrayerKind.Evening,
            group = PrayerGroup.BookOfCommonPrayer,
            themes = setOf(PrayerTheme.Peace),
            lines = listOf("O God, from whom all holy desires do proceed."),
        ),
        testPrayer(
            id = "bcp-morning",
            title = "A Collect for Grace",
            author = "Book of Common Prayer, 1662",
            kind = PrayerKind.Morning,
            group = PrayerGroup.BookOfCommonPrayer,
            themes = setOf(PrayerTheme.Protection),
            lines = listOf("O Lord, our heavenly Father, Almighty and everlasting God."),
        ),
        testPrayer(
            id = "psalm-23",
            title = "Psalm 23",
            author = null,
            kind = PrayerKind.Psalm,
            group = PrayerGroup.Psalter,
            themes = setOf(PrayerTheme.Peace, PrayerTheme.Guidance),
            lines = listOf("The Lord is my shepherd; I shall not want."),
        ),
        testPrayer(
            id = "puritan-valley",
            title = "The Valley of Vision",
            author = "Puritan, anonymous",
            kind = PrayerKind.Meditation,
            group = PrayerGroup.Puritan,
            themes = setOf(PrayerTheme.Mercy),
            lines = listOf("Lord, high and holy, meek and lowly."),
        ),
    )

    private val prayerRepository = FakePrayerRepository(catalogue)

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) = LibraryViewModel(
        savedStateHandle = savedStateHandle,
        recordPrayerOpened = RecordPrayerOpenedUseCase(prayerRepository, clock),
        prayerRepository = prayerRepository,
        searchPrayers = SearchPrayersUseCase(prayerRepository),
        filterPrayers = FilterPrayersUseCase(prayerRepository),
    )

    /** Skips the first frame, drawn before the catalogue has been read. */
    private val LibraryViewModel.loadedStates: Flow<LibraryUiState>
        get() = uiState.filter { it.isCatalogueReady }

    @Test
    fun `the whole catalogue is on the shelf before anything is typed`() = runTest {
        viewModel().loadedStates.test {
            val uiState = awaitItem()

            assertEquals(catalogue.map { it.id }, uiState.prayers.map { it.id })
            assertFalse(uiState.isNarrowed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the tiles count the whole catalogue, not the narrowed shelf`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(LibraryAction.ToggleGroup(PrayerGroup.Psalter))

            val uiState = awaitItem()
            assertEquals(1, uiState.prayers.size)
            assertEquals(
                2,
                uiState.groupTiles.first { it.group == PrayerGroup.BookOfCommonPrayer }.prayerCount,
            )
            assertTrue(uiState.groupTiles.first { it.group == PrayerGroup.Psalter }.isSelected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a search matches a title, an author and a line`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)

            viewModel.onAction(LibraryAction.SearchQueryChanged("shepherd"))
            assertEquals(listOf("psalm-23"), awaitItem().prayers.map { it.id })

            viewModel.onAction(LibraryAction.SearchQueryChanged("Puritan"))
            assertEquals(listOf("puritan-valley"), awaitItem().prayers.map { it.id })

            viewModel.onAction(LibraryAction.SearchQueryChanged("collect"))
            assertEquals(listOf("bcp-peace", "bcp-morning"), awaitItem().prayers.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the search and the chips narrow together`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)

            viewModel.onAction(LibraryAction.SearchQueryChanged("Collect"))
            skipItems(1)
            viewModel.onAction(LibraryAction.ToggleKind(PrayerKind.Morning))

            val uiState = awaitItem()
            assertEquals(listOf("bcp-morning"), uiState.prayers.map { it.id })
            assertTrue(uiState.isNarrowed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `two chips of the same kind widen the shelf rather than emptying it`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)

            viewModel.onAction(LibraryAction.ToggleTheme(PrayerTheme.Mercy))
            assertEquals(listOf("puritan-valley"), awaitItem().prayers.map { it.id })

            viewModel.onAction(LibraryAction.ToggleTheme(PrayerTheme.Guidance))
            assertEquals(listOf("psalm-23", "puritan-valley"), awaitItem().prayers.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tapping a selected tile twice puts it back`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)

            viewModel.onAction(LibraryAction.ToggleKind(PrayerKind.Psalm))
            assertEquals(listOf("psalm-23"), awaitItem().prayers.map { it.id })

            viewModel.onAction(LibraryAction.ToggleKind(PrayerKind.Psalm))
            assertEquals(catalogue.map { it.id }, awaitItem().prayers.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearing drops the query and every chip at once`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(LibraryAction.SearchQueryChanged("peace"))
            skipItems(1)
            viewModel.onAction(LibraryAction.ToggleGroup(PrayerGroup.Psalter))
            skipItems(1)

            viewModel.onAction(LibraryAction.ClearNarrowing)

            val uiState = awaitItem()
            assertEquals("", uiState.searchQuery)
            assertTrue(uiState.filter.isEmpty)
            assertEquals(catalogue.map { it.id }, uiState.prayers.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a search that matches nothing leaves an empty shelf, not the whole catalogue`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(LibraryAction.SearchQueryChanged("zzzz"))

            val uiState = awaitItem()
            assertTrue(uiState.prayers.isEmpty())
            assertTrue(uiState.isNarrowed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** A rotation is a new ViewModel over the same handle — the narrowing has to come back with it. */
    @Test
    fun `the query and the chips survive a rotation`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val before = viewModel(savedStateHandle)

        before.loadedStates.test {
            skipItems(1)
            before.onAction(LibraryAction.SearchQueryChanged("Collect"))
            skipItems(1)
            before.onAction(LibraryAction.ToggleKind(PrayerKind.Morning))
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val after = viewModel(savedStateHandle)

        assertEquals("Collect", after.uiState.value.searchQuery)
        after.loadedStates.test {
            val uiState = awaitItem()
            assertEquals(setOf(PrayerKind.Morning), uiState.filter.kinds)
            assertEquals(listOf("bcp-morning"), uiState.prayers.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `opening a prayer from the shelf stamps it as opened`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(LibraryAction.OpenPrayer("psalm-23"))
            awaitItem()

            assertEquals(clock.millis(), prayerRepository.getPrayer("psalm-23")?.lastOpenedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
