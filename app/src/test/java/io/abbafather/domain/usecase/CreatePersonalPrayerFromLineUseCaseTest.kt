package io.abbafather.domain.usecase

import io.abbafather.domain.model.PrayerTheme
import io.abbafather.domain.model.SavedLine
import io.abbafather.testing.CountingIdGenerator
import io.abbafather.testing.FakePersonalPrayerRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CreatePersonalPrayerFromLineUseCaseTest {

    private val writtenAt = Instant.parse("2026-08-25T19:30:00Z")
    private val personalPrayerRepository = FakePersonalPrayerRepository()
    private val createPersonalPrayerFromLine = CreatePersonalPrayerFromLineUseCase(
        personalPrayerRepository = personalPrayerRepository,
        idGenerator = CountingIdGenerator(),
        clock = Clock.fixed(writtenAt, ZoneOffset.UTC),
    )
    private val savedLine = SavedLine(
        id = "saved-1",
        text = "You were with me, and I was not with you.",
        sourcePrayerId = "augustine-late-have-i-loved-you",
        sourcePrayerTitle = "Late Have I Loved You",
        sourceAttribution = "Augustine of Hippo, Confessions X",
        sourceLineIndex = 5,
        themes = setOf(PrayerTheme.Presence),
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun `the kept line opens the draft, with room to write after it`() = runTest {
        val draft = createPersonalPrayerFromLine(savedLine)

        assertEquals(listOf(savedLine.text), draft.lines)
        assertTrue(draft.body.endsWith("\n\n"))
        assertEquals("After Late Have I Loved You", draft.title)
        assertEquals(savedLine.themes, draft.themes)
        assertEquals(savedLine.id, draft.seededFromSavedLineId)
        assertEquals(writtenAt.toEpochMilli(), draft.createdAt)
    }

    @Test
    fun `the draft is stored, so the compose screen edits a row rather than a plan`() = runTest {
        val draft = createPersonalPrayerFromLine(savedLine)

        assertEquals(listOf(draft), personalPrayerRepository.storedPrayers)
    }
}
