package io.abbafather.feature.myprayers

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.abbafather.domain.model.PersonalPrayer
import io.abbafather.domain.model.PrayerTag
import io.abbafather.domain.model.SavedLine
import io.abbafather.domain.usecase.CreatePersonalPrayerFromLineUseCase
import io.abbafather.testing.CountingIdGenerator
import io.abbafather.testing.FakePersonalPrayerRepository
import io.abbafather.testing.MainDispatcherRule
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

class MyPrayersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-08-27T09:00:00Z"), ZoneOffset.UTC)

    private val dread = personalPrayer(
        id = "prayer-dread",
        title = "For the morning I keep dreading",
        body = "Father, I am afraid of tomorrow.\n\nGo before me into it.",
        updatedAt = 1_000L,
    )
    private val untitled = personalPrayer(
        id = "prayer-untitled",
        title = "",
        body = "Thank you for the grace you keep showing me.\n\n",
        updatedAt = 2_000L,
    )

    private val personalPrayerRepository = FakePersonalPrayerRepository(listOf(dread, untitled))

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        MyPrayersViewModel(
            savedStateHandle = savedStateHandle,
            personalPrayerRepository = personalPrayerRepository,
            clock = clock,
        )

    private val MyPrayersViewModel.loadedStates: Flow<MyPrayersUiState>
        get() = uiState.filter { it.isLoaded }

    private fun personalPrayer(
        id: String,
        title: String,
        body: String,
        updatedAt: Long,
        tags: Set<PrayerTag> = emptySet(),
        seededFromSavedLineId: String? = null,
    ) = PersonalPrayer(
        id = id,
        title = title,
        body = body,
        tags = tags,
        seededFromSavedLineId = seededFromSavedLineId,
        createdAt = updatedAt,
        updatedAt = updatedAt,
    )

    @Test
    fun `a written prayer reads back with its excerpt and when it was last touched`() = runTest {
        viewModel().loadedStates.test {
            val card = awaitItem().prayers.single { it.personalPrayerId == dread.id }

            assertEquals(dread.title, card.title)
            assertTrue(card.hasTitle)
            assertEquals("Father, I am afraid of tomorrow.", card.excerpt)
            assertEquals(dread.updatedAt, card.lastTouchedAt)
            assertFalse(card.isAwaitingDeleteConfirmation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the most recently touched prayer is at the top`() = runTest {
        viewModel().loadedStates.test {
            assertEquals(
                listOf(untitled.id, dread.id),
                awaitItem().prayers.map { it.personalPrayerId },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a prayer written but not yet named still reads, and says it has no name`() = runTest {
        viewModel().loadedStates.test {
            val card = awaitItem().prayers.single { it.personalPrayerId == untitled.id }

            assertFalse(card.hasTitle)
            assertEquals("Thank you for the grace you keep showing me.", card.excerpt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a draft grown from a kept line is waiting here`() = runTest {
        val savedLine = SavedLine(
            id = "line-grace",
            text = "Thank you for the grace you keep showing me.",
            sourcePrayerId = "vov-amazing-grace",
            sourcePrayerTitle = "Amazing Grace",
            sourceAttribution = "The Valley of Vision, adapted",
            sourceLineIndex = 1,
            tags = setOf(PrayerTag.Grace),
            createdAt = 3_000L,
            updatedAt = 3_000L,
        )
        CreatePersonalPrayerFromLineUseCase(
            personalPrayerRepository,
            CountingIdGenerator(),
            clock,
        ).invoke(savedLine)

        viewModel().loadedStates.test {
            val card = awaitItem().prayers.first()

            assertEquals("After Amazing Grace", card.title)
            assertEquals(savedLine.text, card.excerpt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleting takes two taps and the first only asks`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(MyPrayersAction.AskToDelete(dread.id))

            val asking = awaitItem()
            assertTrue(asking.prayers.single { it.personalPrayerId == dread.id }.isAwaitingDeleteConfirmation)
            // Only the card being asked about; the others carry on as they were.
            assertFalse(asking.prayers.single { it.personalPrayerId == untitled.id }.isAwaitingDeleteConfirmation)
            assertEquals(2, personalPrayerRepository.storedPrayers.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `keeping it puts the question away and leaves the prayer alone`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(MyPrayersAction.AskToDelete(dread.id))
            awaitItem()
            viewModel.onAction(MyPrayersAction.CancelDelete)

            val uiState = awaitItem()
            assertTrue(uiState.prayers.none { it.isAwaitingDeleteConfirmation })
            assertEquals(2, uiState.prayers.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirming removes the prayer and leaves the others alone`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(MyPrayersAction.AskToDelete(dread.id))
            awaitItem()
            viewModel.onAction(MyPrayersAction.ConfirmDelete(dread.id))

            while (true) {
                val uiState = awaitItem()
                if (uiState.prayers.size == 1) {
                    assertEquals(untitled.id, uiState.prayers.single().personalPrayerId)
                    break
                }
            }
            assertEquals(listOf(untitled.id), personalPrayerRepository.storedPrayers.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the question survives a rotation`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = viewModel(savedStateHandle)

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(MyPrayersAction.AskToDelete(dread.id))
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        viewModel(savedStateHandle).loadedStates.test {
            val card = awaitItem().prayers.single { it.personalPrayerId == dread.id }

            assertTrue(card.isAwaitingDeleteConfirmation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an empty shelf says so, but not before the prayers have arrived`() = runTest {
        val viewModel = MyPrayersViewModel(
            savedStateHandle = SavedStateHandle(),
            personalPrayerRepository = FakePersonalPrayerRepository(),
            clock = clock,
        )

        assertFalse(viewModel.uiState.value.isEmpty)

        viewModel.loadedStates.test {
            assertTrue(awaitItem().isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
