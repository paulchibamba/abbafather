package io.abbafather.feature.library

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.abbafather.domain.model.PrayerPart
import io.abbafather.domain.model.PrayerTag
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
            id = "vov-069-contrition",
            title = "Contrition",
            part = PrayerPart.PenitenceAndDeprecation,
            tags = setOf(PrayerTag.Repentance),
            lines = listOf("Break my heart over what breaks yours."),
        ),
        testPrayer(
            id = "vov-081-penitence",
            title = "Penitence",
            part = PrayerPart.PenitenceAndDeprecation,
            tags = setOf(PrayerTag.Repentance, PrayerTag.Forgiveness),
            lines = listOf("I come to you with nothing to offer but my need."),
        ),
        testPrayer(
            id = "vov-106-morning",
            title = "Morning",
            part = PrayerPart.NeedsAndDevotions,
            tags = setOf(PrayerTag.Grace, PrayerTag.MorningAndEvening),
            lines = listOf("Compassionate Lord, I woke up today because your mercy carried me here."),
        ),
        testPrayer(
            id = "vov-142-contentment",
            title = "Contentment",
            part = PrayerPart.GiftsOfGrace,
            tags = setOf(PrayerTag.Contentment),
            lines = listOf("Teach me to want what you have already given."),
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
            viewModel.onAction(LibraryAction.TogglePart(PrayerPart.GiftsOfGrace))

            val uiState = awaitItem()
            assertEquals(1, uiState.prayers.size)
            assertEquals(
                2,
                uiState.partTiles.first { it.part == PrayerPart.PenitenceAndDeprecation }.prayerCount,
            )
            assertTrue(uiState.partTiles.first { it.part == PrayerPart.GiftsOfGrace }.isSelected)
            assertEquals(catalogue.size, uiState.catalogueSize)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a search matches a title, a part and a line`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)

            viewModel.onAction(LibraryAction.SearchQueryChanged("breaks yours"))
            assertEquals(listOf("vov-069-contrition"), awaitItem().prayers.map { it.id })

            viewModel.onAction(LibraryAction.SearchQueryChanged("Penitence"))
            assertEquals(
                listOf("vov-069-contrition", "vov-081-penitence"),
                awaitItem().prayers.map { it.id },
            )

            viewModel.onAction(LibraryAction.SearchQueryChanged("cont"))
            assertEquals(
                listOf("vov-069-contrition", "vov-142-contentment"),
                awaitItem().prayers.map { it.id },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the search and the chips narrow together`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)

            viewModel.onAction(LibraryAction.SearchQueryChanged("cont"))
            skipItems(1)
            viewModel.onAction(LibraryAction.TogglePart(PrayerPart.GiftsOfGrace))

            val uiState = awaitItem()
            assertEquals(listOf("vov-142-contentment"), uiState.prayers.map { it.id })
            assertTrue(uiState.isNarrowed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `two chips of the same kind widen the shelf rather than emptying it`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)

            viewModel.onAction(LibraryAction.ToggleTag(PrayerTag.Contentment))
            assertEquals(listOf("vov-142-contentment"), awaitItem().prayers.map { it.id })

            viewModel.onAction(LibraryAction.ToggleTag(PrayerTag.Forgiveness))
            assertEquals(
                listOf("vov-081-penitence", "vov-142-contentment"),
                awaitItem().prayers.map { it.id },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tapping a selected tile twice puts it back`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)

            viewModel.onAction(LibraryAction.ToggleTag(PrayerTag.Contentment))
            assertEquals(listOf("vov-142-contentment"), awaitItem().prayers.map { it.id })

            viewModel.onAction(LibraryAction.ToggleTag(PrayerTag.Contentment))
            assertEquals(catalogue.map { it.id }, awaitItem().prayers.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearing drops the query and every chip at once`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(LibraryAction.SearchQueryChanged("cont"))
            skipItems(1)
            viewModel.onAction(LibraryAction.TogglePart(PrayerPart.GiftsOfGrace))
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
            before.onAction(LibraryAction.SearchQueryChanged("cont"))
            skipItems(1)
            before.onAction(LibraryAction.TogglePart(PrayerPart.GiftsOfGrace))
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val after = viewModel(savedStateHandle)

        assertEquals("cont", after.uiState.value.searchQuery)
        after.loadedStates.test {
            val uiState = awaitItem()
            assertEquals(setOf(PrayerPart.GiftsOfGrace), uiState.filter.parts)
            assertEquals(listOf("vov-142-contentment"), uiState.prayers.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `opening a prayer from the shelf stamps it as opened`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(LibraryAction.OpenPrayer("vov-106-morning"))
            awaitItem()

            assertEquals(clock.millis(), prayerRepository.getPrayer("vov-106-morning")?.lastOpenedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
