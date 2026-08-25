package io.abbafather.domain.usecase

import io.abbafather.testing.FakePrayerRepository
import io.abbafather.testing.testPrayer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate

class GetTodaysSuggestedPrayerUseCaseTest {

    private val catalogue = (1..30).map { testPrayer(id = "prayer-%02d".format(it)) }
    private val getTodaysSuggestedPrayer =
        GetTodaysSuggestedPrayerUseCase(FakePrayerRepository(catalogue), Clock.systemUTC())

    @Test
    fun `the same day always suggests the same prayer`() = runTest {
        val date = LocalDate.of(2026, 8, 25)

        val first = getTodaysSuggestedPrayer(date).first()
        val second = getTodaysSuggestedPrayer(date).first()

        assertEquals(first, second)
    }

    @Test
    fun `consecutive days suggest different prayers`() = runTest {
        val date = LocalDate.of(2026, 8, 25)

        val today = getTodaysSuggestedPrayer(date).first()
        val tomorrow = getTodaysSuggestedPrayer(date.plusDays(1)).first()

        assertTrue(today != tomorrow)
    }

    @Test
    fun `a fortnight of suggestions covers most of the catalogue`() = runTest {
        val start = LocalDate.of(2026, 1, 1)

        val suggested = (0..13).map { getTodaysSuggestedPrayer(start.plusDays(it.toLong())).first() }

        assertTrue("suggestions repeated too soon: $suggested", suggested.toSet().size >= 12)
    }

    @Test
    fun `an empty catalogue suggests nothing rather than failing`() = runTest {
        val onEmpty = GetTodaysSuggestedPrayerUseCase(FakePrayerRepository(), Clock.systemUTC())

        assertNull(onEmpty(LocalDate.of(2026, 8, 25)).first())
    }
}
