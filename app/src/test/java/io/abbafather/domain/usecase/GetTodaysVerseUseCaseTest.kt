package io.abbafather.domain.usecase

import io.abbafather.domain.model.DailyVerses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate

class GetTodaysVerseUseCaseTest {

    private val getTodaysVerse = GetTodaysVerseUseCase(Clock.systemUTC())

    @Test
    fun `the same day always gives the same verse`() {
        val date = LocalDate.of(2026, 8, 25)

        assertEquals(getTodaysVerse(date), getTodaysVerse(date))
    }

    @Test
    fun `consecutive days give different verses`() {
        val date = LocalDate.of(2026, 8, 25)

        assertTrue(getTodaysVerse(date) != getTodaysVerse(date.plusDays(1)))
    }

    @Test
    fun `a fortnight of verses barely repeats`() {
        val start = LocalDate.of(2026, 1, 1)

        val verses = (0..13).map { getTodaysVerse(start.plusDays(it.toLong())) }

        assertTrue("verses repeated too soon: $verses", verses.toSet().size >= 12)
    }

    @Test
    fun `a date far in the past still lands inside the list`() {
        val verse = getTodaysVerse(LocalDate.of(1662, 5, 19))

        assertTrue(verse in DailyVerses)
    }

    @Test
    fun `every verse carries its reference`() {
        assertTrue(DailyVerses.all { it.text.isNotBlank() && it.reference.isNotBlank() })
    }
}
