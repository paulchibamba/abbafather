package io.abbafather.feature.composeprayer

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.abbafather.domain.model.PersonalPrayer
import io.abbafather.domain.model.PrayerTag
import io.abbafather.testing.CountingIdGenerator
import io.abbafather.testing.FakePersonalPrayerRepository
import io.abbafather.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ComposePrayerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-08-27T09:00:00Z"), ZoneOffset.UTC)

    private val written = PersonalPrayer(
        id = "prayer-dread",
        title = "For the morning I keep dreading",
        body = "Father, I am afraid of tomorrow.\n\nGo before me into it.",
        tags = setOf(PrayerTag.FearAndAnxiety, PrayerTag.Providence),
        createdAt = 1_000L,
        updatedAt = 1_000L,
    )

    private val personalPrayerRepository = FakePersonalPrayerRepository(listOf(written))

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        ComposePrayerViewModel(
            savedStateHandle = savedStateHandle,
            personalPrayerRepository = personalPrayerRepository,
            idGenerator = CountingIdGenerator(),
            clock = clock,
        )

    private val ComposePrayerViewModel.loadedStates: Flow<ComposePrayerUiState>
        get() = uiState.filter { it.isLoaded }

    /** The text the page opens on, once there is any. */
    private suspend fun ComposePrayerViewModel.openedDraft(): ComposePrayerDraft =
        openingDraft.filterNotNull().first()

    @Test
    fun `a blank page opens on nothing, with nothing to keep yet`() = runTest {
        val viewModel = viewModel()

        val draft = viewModel.openedDraft()
        assertEquals("", draft.title)
        assertEquals("", draft.body)

        viewModel.loadedStates.test {
            val uiState = awaitItem()

            assertTrue(uiState.tagChips.isEmpty())
            assertTrue(uiState.canShowMoreTags)
            assertFalse(uiState.canKeep)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a kept line opens on the line, with room to carry on after it`() = runTest {
        val seed = "Thank you for the grace you keep showing me."
        val viewModel = viewModel(SavedStateHandle(mapOf("seedText" to seed)))

        val draft = viewModel.openedDraft()
        assertEquals("$seed\n\n", draft.body)
        assertEquals("", draft.title)

        viewModel.loadedStates.test {
            assertTrue(awaitItem().canKeep)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a prayer already written opens on what was written`() = runTest {
        val viewModel = viewModel(SavedStateHandle(mapOf("personalPrayerId" to written.id)))

        val draft = viewModel.openedDraft()
        assertEquals(written.title, draft.title)
        assertEquals(written.body, draft.body)

        viewModel.loadedStates.test {
            assertEquals(
                written.tags,
                awaitItem().tagChips.filter { it.isSelected }.mapTo(mutableSetOf()) { it.tag },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `keeping a new prayer writes it whole`() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(ComposePrayerAction.TitleChanged("  Before the day starts  "))
            viewModel.onAction(ComposePrayerAction.BodyChanged("Meet me in it.\n"))
            viewModel.onAction(ComposePrayerAction.ShowMoreTags)
            viewModel.onAction(ComposePrayerAction.ToggleTag(PrayerTag.Grace))
            viewModel.onAction(ComposePrayerAction.KeepPrayer)

            assertEquals(ComposePrayerEvent.Kept, awaitItem())
            val kept = personalPrayerRepository.storedPrayers.single { it.id != written.id }
            // The name is tidied; the body is not — the blank lines are how the reader hears it.
            assertEquals("Before the day starts", kept.title)
            assertEquals("Meet me in it.\n", kept.body)
            assertEquals(setOf(PrayerTag.Grace), kept.tags)
            assertEquals(clock.millis(), kept.createdAt)
            assertEquals(clock.millis(), kept.updatedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `keeping a prayer already written saves back to the same row`() = runTest {
        val viewModel = viewModel(SavedStateHandle(mapOf("personalPrayerId" to written.id)))

        viewModel.loadedStates.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.events.test {
            viewModel.onAction(ComposePrayerAction.BodyChanged("${written.body}\n\nAnd hold me in it."))
            viewModel.onAction(ComposePrayerAction.KeepPrayer)
            awaitItem()

            val kept = personalPrayerRepository.storedPrayers.single()
            assertEquals(written.id, kept.id)
            assertEquals(written.title, kept.title)
            assertTrue(kept.body.endsWith("And hold me in it."))
            assertEquals(written.tags, kept.tags)
            // The day it was written stands; only the day it was touched moves.
            assertEquals(written.createdAt, kept.createdAt)
            assertEquals(clock.millis(), kept.updatedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a name on its own is not a prayer`() = runTest {
        val viewModel = viewModel()

        viewModel.openedDraft()
        viewModel.onAction(ComposePrayerAction.TitleChanged("A name"))

        viewModel.loadedStates.test {
            // A name alone does not even change what the page offers, let alone write a row.
            assertFalse(awaitItem().canKeep)
            viewModel.onAction(ComposePrayerAction.KeepPrayer)
            assertEquals(listOf(written.id), personalPrayerRepository.storedPrayers.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the draft survives a rotation, and is never overwritten by the stored row`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("personalPrayerId" to written.id))
        val viewModel = viewModel(savedStateHandle)

        viewModel.openedDraft()
        viewModel.onAction(ComposePrayerAction.TitleChanged("A better name"))
        viewModel.onAction(ComposePrayerAction.BodyChanged("Words not kept yet."))

        // A second ViewModel over the same handle is what a rotation — or a process death the handle
        // was restored from — comes back as.
        val restored = viewModel(savedStateHandle).openedDraft()

        assertEquals("A better name", restored.title)
        assertEquals("Words not kept yet.", restored.body)
    }

    @Test
    fun `the picker shows what is ticked, and the whole vocabulary when asked`() = runTest {
        val viewModel = viewModel(SavedStateHandle(mapOf("personalPrayerId" to written.id)))

        viewModel.loadedStates.test {
            assertEquals(
                listOf(PrayerTag.FearAndAnxiety, PrayerTag.Providence).sortedBy(PrayerTag::ordinal),
                awaitItem().tagChips.map { it.tag },
            )

            viewModel.onAction(ComposePrayerAction.ShowMoreTags)

            val widened = awaitStateWhere { it.tagChips.size > 2 }
            // The whole vocabulary in its own order — ticking one must not move the next one out
            // from under the finger about to tick it.
            assertEquals(PrayerTag.entries, widened.tagChips.map { it.tag })
            assertEquals(
                written.tags,
                widened.tagChips.filter { it.isSelected }.mapTo(mutableSetOf()) { it.tag },
            )
            assertFalse(widened.canShowMoreTags)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an unkept draft leaves nothing behind`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(ComposePrayerAction.BodyChanged("Something written and walked away from."))
            awaitStateWhere { it.canKeep }

            viewModel.onAction(ComposePrayerAction.Back)

            assertEquals(listOf(written.id), personalPrayerRepository.storedPrayers.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<ComposePrayerUiState>.awaitStateWhere(
        predicate: (ComposePrayerUiState) -> Boolean,
    ): ComposePrayerUiState {
        while (true) {
            val uiState = awaitItem()
            if (predicate(uiState)) return uiState
        }
    }
}
