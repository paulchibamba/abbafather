package io.abbafather.feature.reader

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.abbafather.domain.model.PrayerTag
import io.abbafather.domain.usecase.CreatePersonalPrayerFromLineUseCase
import io.abbafather.domain.usecase.SaveLineUseCase
import io.abbafather.testing.CountingIdGenerator
import io.abbafather.testing.FakePersonalPrayerRepository
import io.abbafather.testing.FakePrayerRepository
import io.abbafather.testing.FakeSavedLineRepository
import io.abbafather.testing.MainDispatcherRule
import io.abbafather.testing.testPrayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ReaderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-08-25T08:30:00Z"), ZoneOffset.UTC)

    private val prayer = testPrayer(
        id = "vov-106-morning",
        title = "Morning",
        tags = setOf(PrayerTag.Grace, PrayerTag.MorningAndEvening),
        movementLines = listOf(
            listOf(
                "Compassionate Lord, I woke up today because your mercy carried me here.",
                "Thank you for the gift of another morning.",
            ),
            listOf("Let it matter for my soul."),
        ),
        headings = listOf("Receiving this new day as mercy", "Asking for the day to matter"),
    )

    private val prayerRepository = FakePrayerRepository(listOf(prayer))
    private val savedLineRepository = FakeSavedLineRepository()
    private val personalPrayerRepository = FakePersonalPrayerRepository()

    private fun viewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(mapOf("prayerId" to prayer.id)),
    ) = ReaderViewModel(
        savedStateHandle = savedStateHandle,
        savedLineRepository = savedLineRepository,
        saveLine = SaveLineUseCase(savedLineRepository, CountingIdGenerator(), clock),
        createPersonalPrayerFromLine = CreatePersonalPrayerFromLineUseCase(
            personalPrayerRepository,
            CountingIdGenerator(),
            clock,
        ),
        clock = clock,
        prayerRepository = prayerRepository,
    )

    private val ReaderViewModel.loadedStates: Flow<ReaderUiState>
        get() = uiState.filter { it.isLoaded }

    /**
     * Closing the sheet writes several keys, so the state that matters can be one emission behind
     * the action. This waits for the state the test is talking about rather than for a count.
     */
    private suspend fun ReceiveTurbine<ReaderUiState>.awaitStateWhere(
        predicate: (ReaderUiState) -> Boolean,
    ): ReaderUiState {
        while (true) {
            val uiState = awaitItem()
            if (predicate(uiState)) return uiState
        }
    }

    @Test
    fun `the prayer arrives whole and no sheet is open`() = runTest {
        viewModel().loadedStates.test {
            val uiState = awaitItem()

            assertEquals(prayer.lines, uiState.prayer?.lines)
            assertNull(uiState.keepSheet)
            assertTrue(uiState.keptLineIndices.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tapping a line opens the sheet on it, ticked with the prayer's own tags`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(ReaderAction.SelectLine(1))

            val sheet = awaitItem().keepSheet
            assertEquals(KeepLineStage.Keep, sheet?.stage)
            assertEquals(prayer.lines[1], sheet?.line)
            assertEquals(
                prayer.tags,
                sheet?.tagChips?.filter { it.isSelected }?.map { it.tag }?.toSet(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `keeping a line writes it whole and moves the sheet on`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(ReaderAction.SelectLine(1))
            awaitStateWhere { it.keepSheet != null }
            viewModel.onAction(ReaderAction.ToggleTag(PrayerTag.MorningAndEvening))
            awaitStateWhere { uiState ->
                uiState.keepSheet!!.tagChips.none {
                    it.isSelected && it.tag == PrayerTag.MorningAndEvening
                }
            }

            viewModel.onAction(ReaderAction.KeepLine)

            val uiState = awaitStateWhere { it.keepSheet?.stage == KeepLineStage.Kept }
            assertTrue(1 in uiState.keptLineIndices)

            val savedLine = savedLineRepository.storedLines.single()
            assertEquals(prayer.lines[1], savedLine.text)
            assertEquals(prayer.id, savedLine.sourcePrayerId)
            assertEquals(prayer.title, savedLine.sourcePrayerTitle)
            assertEquals(1, savedLine.sourceLineIndex)
            assertEquals(setOf(PrayerTag.Grace), savedLine.tags)
            assertEquals(clock.millis(), savedLine.createdAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a line kept earlier opens straight into what it already is`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(ReaderAction.SelectLine(0))
            skipItems(1)
            viewModel.onAction(ReaderAction.KeepLine)
            skipItems(1)
            viewModel.onAction(ReaderAction.DismissSheet)
            skipItems(1)

            viewModel.onAction(ReaderAction.SelectLine(0))

            assertEquals(KeepLineStage.AlreadyKept, awaitItem().keepSheet?.stage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `letting a kept line go removes it and closes the sheet`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(ReaderAction.SelectLine(2))
            skipItems(1)
            viewModel.onAction(ReaderAction.KeepLine)
            skipItems(1)

            viewModel.onAction(ReaderAction.ReleaseKeptLine)

            val uiState = awaitStateWhere { it.keepSheet == null }
            assertTrue(uiState.keptLineIndices.isEmpty())
            assertTrue(savedLineRepository.storedLines.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `growing a kept line into a prayer writes the draft and asks for the compose screen`() =
        runTest {
            val viewModel = viewModel()

            viewModel.loadedStates.test {
                skipItems(1)
                viewModel.onAction(ReaderAction.SelectLine(1))
                skipItems(1)
                viewModel.onAction(ReaderAction.KeepLine)
                skipItems(1)

                viewModel.events.test {
                    viewModel.onAction(ReaderAction.GrowIntoPrayer)

                    val event = awaitItem()
                    assertTrue(event is ReaderEvent.OpenComposedPrayer)

                    val draft = personalPrayerRepository.storedPrayers.single()
                    assertEquals(
                        (event as ReaderEvent.OpenComposedPrayer).personalPrayerId,
                        draft.id,
                    )
                    assertEquals("After ${prayer.title}", draft.title)
                    assertTrue(draft.body.startsWith(prayer.lines[1]))
                    cancelAndIgnoreRemainingEvents()
                }

                awaitStateWhere { it.keepSheet == null }
                cancelAndIgnoreRemainingEvents()
            }
        }

    /** A rotation is a new ViewModel over the same handle — the open sheet has to come back with it. */
    @Test
    fun `the open sheet survives a rotation`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("prayerId" to prayer.id))
        val before = viewModel(savedStateHandle)

        before.loadedStates.test {
            skipItems(1)
            before.onAction(ReaderAction.SelectLine(2))
            skipItems(1)
            before.onAction(ReaderAction.ToggleTag(PrayerTag.Grace))
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        viewModel(savedStateHandle).loadedStates.test {
            val sheet = awaitItem().keepSheet

            assertEquals(KeepLineStage.Keep, sheet?.stage)
            assertEquals(2, sheet?.lineIndex)
            assertEquals(prayer.lines[2], sheet?.line)
            // Grace was ticked by default and tapped off again; the sheet comes back as it was.
            assertEquals(
                setOf(PrayerTag.MorningAndEvening),
                sheet?.tagChips?.filter { it.isSelected }?.map { it.tag }?.toSet(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `more tags widens the keep sheet from the prayer's own tags to the whole vocabulary`() =
        runTest {
            val viewModel = viewModel()

            viewModel.loadedStates.test {
                skipItems(1)
                viewModel.onAction(ReaderAction.SelectLine(0))
                val narrow = awaitStateWhere { it.keepSheet != null }.keepSheet!!
                assertEquals(prayer.tags.toList(), narrow.tagChips.map { it.tag })
                assertTrue(narrow.canShowMoreTags)

                viewModel.onAction(ReaderAction.ShowMoreTags)

                val wide = awaitStateWhere { it.keepSheet?.canShowMoreTags == false }.keepSheet!!
                assertEquals(PrayerTag.entries.size, wide.tagChips.size)
                // The prayer's own still come first — those are the ones the reader is looking for.
                assertEquals(
                    prayer.tags.toList(),
                    wide.tagChips.take(prayer.tags.size).map { it.tag },
                )
                assertEquals(PrayerTag.entries.toSet(), wide.tagChips.map { it.tag }.toSet())
                assertEquals(
                    prayer.tags,
                    wide.tagChips.filter { it.isSelected }.map { it.tag }.toSet(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a tag ticked from the wider vocabulary is kept with the line`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(ReaderAction.SelectLine(0))
            awaitStateWhere { it.keepSheet != null }
            viewModel.onAction(ReaderAction.ShowMoreTags)
            awaitStateWhere { it.keepSheet?.canShowMoreTags == false }

            viewModel.onAction(ReaderAction.ToggleTag(PrayerTag.Thanksgiving))
            awaitStateWhere { uiState ->
                uiState.keepSheet!!.tagChips.any {
                    it.tag == PrayerTag.Thanksgiving && it.isSelected
                }
            }
            viewModel.onAction(ReaderAction.KeepLine)
            awaitStateWhere { it.keepSheet?.stage == KeepLineStage.Kept }

            assertEquals(
                prayer.tags + PrayerTag.Thanksgiving,
                savedLineRepository.storedLines.single().tags,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `nothing but the prayer is open until a sheet is asked for`() = runTest {
        viewModel().loadedStates.test {
            val uiState = awaitItem()

            assertNull(uiState.keepSheet)
            assertNull(uiState.scriptureSheet)
            assertNull(uiState.provenanceSheet)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a movement's passages open with what it holds and what it rests on`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(ReaderAction.OpenScripture(1))

            val sheet = awaitStateWhere { it.scriptureSheet != null }.scriptureSheet!!
            val movement = prayer.movements[1]
            assertEquals(1, sheet.movementIndex)
            assertEquals(movement.heading, sheet.heading)
            assertEquals(movement.themes, sheet.themes)
            assertEquals(movement.scriptures, sheet.passages)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `about this prayer opens on where the prayer came from`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(ReaderAction.OpenProvenance)

            val sheet = awaitStateWhere { it.provenanceSheet != null }.provenanceSheet!!
            assertEquals(prayer.title, sheet.adaptedTitle)
            assertEquals(prayer.provenance, sheet.provenance)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `opening one sheet closes whichever sheet was open`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(ReaderAction.OpenProvenance)
            awaitStateWhere { it.provenanceSheet != null }

            viewModel.onAction(ReaderAction.OpenScripture(0))
            val overScripture = awaitStateWhere { it.scriptureSheet != null }
            assertNull(overScripture.provenanceSheet)

            viewModel.onAction(ReaderAction.SelectLine(0))
            val overKeep = awaitStateWhere { it.keepSheet != null }
            assertNull(overKeep.scriptureSheet)
            assertNull(overKeep.provenanceSheet)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `both metadata sheets survive a rotation`() = runTest {
        val scriptureHandle = SavedStateHandle(mapOf("prayerId" to prayer.id))
        val readingScripture = viewModel(scriptureHandle)
        readingScripture.loadedStates.test {
            skipItems(1)
            readingScripture.onAction(ReaderAction.OpenScripture(1))
            awaitStateWhere { it.scriptureSheet != null }
            cancelAndIgnoreRemainingEvents()
        }

        viewModel(scriptureHandle).loadedStates.test {
            val sheet = awaitStateWhere { it.scriptureSheet != null }.scriptureSheet!!
            assertEquals(prayer.movements[1].heading, sheet.heading)
            assertEquals(prayer.movements[1].scriptures, sheet.passages)
            cancelAndIgnoreRemainingEvents()
        }

        val provenanceHandle = SavedStateHandle(mapOf("prayerId" to prayer.id))
        val readingProvenance = viewModel(provenanceHandle)
        readingProvenance.loadedStates.test {
            skipItems(1)
            readingProvenance.onAction(ReaderAction.OpenProvenance)
            awaitStateWhere { it.provenanceSheet != null }
            cancelAndIgnoreRemainingEvents()
        }

        viewModel(provenanceHandle).loadedStates.test {
            assertEquals(
                prayer.provenance,
                awaitStateWhere { it.provenanceSheet != null }.provenanceSheet?.provenance,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** The whole vocabulary showing is part of the sheet, so it has to come back with it. */
    @Test
    fun `the widened tag picker survives a rotation`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("prayerId" to prayer.id))
        val before = viewModel(savedStateHandle)

        before.loadedStates.test {
            skipItems(1)
            before.onAction(ReaderAction.SelectLine(1))
            awaitStateWhere { it.keepSheet != null }
            before.onAction(ReaderAction.ShowMoreTags)
            awaitStateWhere { it.keepSheet?.canShowMoreTags == false }
            cancelAndIgnoreRemainingEvents()
        }

        viewModel(savedStateHandle).loadedStates.test {
            val sheet = awaitStateWhere { it.keepSheet != null }.keepSheet!!

            assertEquals(PrayerTag.entries.size, sheet.tagChips.size)
            assertFalse(sheet.canShowMoreTags)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `leaving the sheet keeps nothing`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(ReaderAction.SelectLine(0))
            skipItems(1)

            viewModel.onAction(ReaderAction.DismissSheet)

            val uiState = awaitStateWhere { it.keepSheet == null }
            assertFalse(0 in uiState.keptLineIndices)
            assertTrue(savedLineRepository.storedLines.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
