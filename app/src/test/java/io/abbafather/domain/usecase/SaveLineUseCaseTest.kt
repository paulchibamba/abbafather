package io.abbafather.domain.usecase

import io.abbafather.domain.model.PrayerTag
import io.abbafather.testing.CountingIdGenerator
import io.abbafather.testing.FakeSavedLineRepository
import io.abbafather.testing.testPrayer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SaveLineUseCaseTest {

    private val savedAt = Instant.parse("2026-08-25T19:30:00Z")
    private val savedLineRepository = FakeSavedLineRepository()
    private val saveLine = SaveLineUseCase(
        savedLineRepository = savedLineRepository,
        idGenerator = CountingIdGenerator(),
        clock = Clock.fixed(savedAt, ZoneOffset.UTC),
    )
    private val prayer = testPrayer(
        id = "vov-106-morning",
        tags = setOf(PrayerTag.Grace),
        lines = listOf(
            "Compassionate Lord, I woke up today because your mercy carried me here.",
            "Thank you for the gift of another morning.",
        ),
    )

    @Test
    fun `keeps the line with its own copy of where it came from`() = runTest {
        val saved = saveLine(prayer, lineIndex = 1)

        assertEquals("Thank you for the gift of another morning.", saved.text)
        assertEquals(prayer.id, saved.sourcePrayerId)
        assertEquals(prayer.title, saved.sourcePrayerTitle)
        assertEquals(prayer.attribution, saved.sourceAttribution)
        assertEquals(1, saved.sourceLineIndex)
        assertEquals(savedAt.toEpochMilli(), saved.createdAt)
        assertEquals(savedAt.toEpochMilli(), saved.updatedAt)
        assertFalse(saved.isDeleted)
        assertEquals(listOf(saved), savedLineRepository.storedLines)
    }

    @Test
    fun `inherits the prayer's tags when the reader chooses none`() = runTest {
        val saved = saveLine(prayer, lineIndex = 0)

        assertEquals(prayer.tags, saved.tags)
    }

    @Test
    fun `the reader's own tags win over the prayer's`() = runTest {
        val saved = saveLine(prayer, lineIndex = 0, tags = setOf(PrayerTag.Suffering))

        assertEquals(setOf(PrayerTag.Suffering), saved.tags)
    }

    @Test
    fun `a blank note is no note`() = runTest {
        assertEquals(null, saveLine(prayer, lineIndex = 0, note = "   ").note)
    }

    @Test
    fun `the kept line then reads as kept`() = runTest {
        saveLine(prayer, lineIndex = 1)

        assertTrue(savedLineRepository.observeIsLineSaved(prayer.id, 1).first())
        assertFalse(savedLineRepository.observeIsLineSaved(prayer.id, 0).first())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a line outside the prayer is refused`() = runTest {
        saveLine(prayer, lineIndex = 9)
    }
}
