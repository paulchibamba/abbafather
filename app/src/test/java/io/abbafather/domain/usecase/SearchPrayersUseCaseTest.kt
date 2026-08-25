package io.abbafather.domain.usecase

import io.abbafather.domain.model.PrayerGroup
import io.abbafather.testing.FakePrayerRepository
import io.abbafather.testing.testPrayer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchPrayersUseCaseTest {

    private val peace = testPrayer(id = "peace", title = "A Collect for Peace")
    private val psalm = testPrayer(
        id = "psalm-063",
        title = "Psalm 63",
        author = "Psalm 63, King James Version",
        group = PrayerGroup.Psalter,
        lines = listOf("O God, thou art my God; early will I seek thee:"),
    )
    private val searchPrayers = SearchPrayersUseCase(FakePrayerRepository(listOf(peace, psalm)))

    @Test
    fun `a blank query is the whole catalogue, not an empty result`() = runTest {
        assertEquals(listOf(peace, psalm), searchPrayers("   ").first())
    }

    @Test
    fun `matches a title regardless of case`() = runTest {
        assertEquals(listOf(peace), searchPrayers("collect for PEACE").first())
    }

    @Test
    fun `matches words inside the prayer itself`() = runTest {
        assertEquals(listOf(psalm), searchPrayers("early will I seek").first())
    }

    @Test
    fun `a query nothing answers returns nothing`() = runTest {
        assertTrue(searchPrayers("liturgy of the hours").first().isEmpty())
    }
}
