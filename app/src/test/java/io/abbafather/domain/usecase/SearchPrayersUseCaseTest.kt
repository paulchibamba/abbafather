package io.abbafather.domain.usecase

import io.abbafather.domain.model.PrayerPart
import io.abbafather.domain.model.PrayerTag
import io.abbafather.testing.FakePrayerRepository
import io.abbafather.testing.testPrayer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchPrayersUseCaseTest {

    private val morning = testPrayer(id = "vov-106-morning", title = "Morning")
    private val contrition = testPrayer(
        id = "vov-069-contrition",
        title = "Contrition",
        part = PrayerPart.PenitenceAndDeprecation,
        tags = setOf(PrayerTag.Repentance),
        movementLines = listOf(listOf("Break my heart over what breaks yours.")),
        headings = listOf("Asking for a softened heart"),
    )
    private val searchPrayers = SearchPrayersUseCase(FakePrayerRepository(listOf(morning, contrition)))

    @Test
    fun `a blank query is the whole catalogue, not an empty result`() = runTest {
        assertEquals(listOf(morning, contrition), searchPrayers("   ").first())
    }

    @Test
    fun `matches a title regardless of case`() = runTest {
        assertEquals(listOf(contrition), searchPrayers("CONTRITION").first())
    }

    @Test
    fun `matches a tag, a part and a movement heading`() = runTest {
        assertEquals(listOf(contrition), searchPrayers("repentance").first())
        assertEquals(listOf(contrition), searchPrayers("Penitence").first())
        assertEquals(listOf(contrition), searchPrayers("softened heart").first())
    }

    @Test
    fun `matches words inside the prayer itself`() = runTest {
        assertEquals(listOf(contrition), searchPrayers("breaks yours").first())
    }

    @Test
    fun `a query nothing answers returns nothing`() = runTest {
        assertTrue(searchPrayers("liturgy of the hours").first().isEmpty())
    }
}
