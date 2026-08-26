package io.abbafather.feature.saved

import app.cash.turbine.test
import io.abbafather.domain.model.PrayerTag
import io.abbafather.domain.model.SavedLine
import io.abbafather.domain.usecase.CreatePersonalPrayerFromLineUseCase
import io.abbafather.domain.usecase.RecordPrayerOpenedUseCase
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

class SavedViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-08-27T09:00:00Z"), ZoneOffset.UTC)

    private val prayer = testPrayer(id = "vov-106-morning", title = "Morning")

    private val mercy = savedLine(
        id = "line-mercy",
        text = "Compassionate Lord, I woke up today because your mercy carried me here.",
        tags = setOf(PrayerTag.MorningAndEvening, PrayerTag.Grace),
        createdAt = 1_000L,
    )
    private val paradox = savedLine(
        id = "line-paradox",
        text = "Let me learn by paradox that the way down is the way up.",
        createdAt = 2_000L,
    )

    private val savedLineRepository = FakeSavedLineRepository(listOf(mercy, paradox))
    private val prayerRepository = FakePrayerRepository(listOf(prayer))
    private val personalPrayerRepository = FakePersonalPrayerRepository()

    private fun viewModel() = SavedViewModel(
        savedLineRepository = savedLineRepository,
        createPersonalPrayerFromLine = CreatePersonalPrayerFromLineUseCase(
            personalPrayerRepository,
            CountingIdGenerator(),
            clock,
        ),
        recordPrayerOpened = RecordPrayerOpenedUseCase(prayerRepository, clock),
        clock = clock,
    )

    private val SavedViewModel.loadedStates: Flow<SavedUiState>
        get() = uiState.filter { it.isLoaded }

    private fun savedLine(
        id: String,
        text: String,
        tags: Set<PrayerTag> = emptySet(),
        createdAt: Long,
        sourcePrayerId: String? = prayer.id,
        sourcePrayerTitle: String? = prayer.title,
        sourceAttribution: String? = prayer.attribution,
    ) = SavedLine(
        id = id,
        text = text,
        sourcePrayerId = sourcePrayerId,
        sourcePrayerTitle = sourcePrayerTitle,
        sourceAttribution = sourceAttribution,
        sourceLineIndex = 0,
        tags = tags,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    @Test
    fun `a kept line reads back with its tags and where it came from`() = runTest {
        viewModel().loadedStates.test {
            val card = awaitItem().savedLines.single { it.savedLineId == mercy.id }

            assertEquals(mercy.text, card.line)
            assertEquals(listOf(PrayerTag.Grace, PrayerTag.MorningAndEvening), card.tags)
            assertEquals(prayer.id, card.sourcePrayerId)
            assertEquals(prayer.title, card.sourcePrayerTitle)
            assertEquals(prayer.attribution, card.sourceAttribution)
            assertEquals(mercy.createdAt, card.keptAt)
            assertTrue(card.canOpenSourcePrayer)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the newest line is kept at the top, as a journal reads`() = runTest {
        viewModel().loadedStates.test {
            assertEquals(
                listOf(paradox.id, mercy.id),
                awaitItem().savedLines.map { it.savedLineId },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a line kept from nowhere still reads, without a way back`() = runTest {
        val orphan = savedLine(
            id = "line-orphan",
            text = "Speak, for your servant is listening.",
            createdAt = 3_000L,
            sourcePrayerId = null,
            sourcePrayerTitle = null,
            sourceAttribution = null,
        )
        savedLineRepository.upsertSavedLine(orphan)

        viewModel().loadedStates.test {
            val card = awaitItem().savedLines.single { it.savedLineId == orphan.id }

            assertEquals(orphan.text, card.line)
            assertFalse(card.canOpenSourcePrayer)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `letting a line go removes it and leaves the others alone`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(SavedAction.ReleaseLine(mercy.id))

            assertEquals(listOf(paradox.id), awaitItem().savedLines.map { it.savedLineId })
            assertEquals(listOf(paradox.id), savedLineRepository.storedLines.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an empty shelf says so rather than showing nothing`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.onAction(SavedAction.ReleaseLine(mercy.id))
            viewModel.onAction(SavedAction.ReleaseLine(paradox.id))

            while (true) {
                val uiState = awaitItem()
                if (uiState.savedLines.isEmpty()) {
                    assertTrue(uiState.isEmpty)
                    break
                }
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `nothing is empty until the lines have arrived`() = runTest {
        assertFalse(viewModel().uiState.value.isEmpty)
    }

    @Test
    fun `growing a line writes the draft and asks for the compose screen`() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(SavedAction.GrowIntoPrayer(mercy.id))

            val event = awaitItem() as SavedEvent.OpenComposedPrayer
            val draft = personalPrayerRepository.storedPrayers.single()
            assertEquals(draft.id, event.personalPrayerId)
            assertEquals("After ${prayer.title}", draft.title)
            assertTrue(draft.body.startsWith(mercy.text))
            assertEquals(mercy.tags, draft.tags)
            assertEquals(mercy.id, draft.seededFromSavedLineId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `growing a line leaves it kept`() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(SavedAction.GrowIntoPrayer(mercy.id))
            awaitItem()

            assertTrue(savedLineRepository.storedLines.any { it.id == mercy.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reaching for the prayer a line came from stamps it as opened`() = runTest {
        val viewModel = viewModel()

        prayerRepository.observePrayer(prayer.id).test {
            assertNull(awaitItem()?.lastOpenedAt)

            viewModel.onAction(SavedAction.OpenSourcePrayer(prayer.id))

            assertEquals(clock.millis(), awaitItem()?.lastOpenedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
