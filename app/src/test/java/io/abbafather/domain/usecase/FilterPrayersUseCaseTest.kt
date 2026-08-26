package io.abbafather.domain.usecase

import io.abbafather.domain.model.PrayerFilter
import io.abbafather.domain.model.PrayerPart
import io.abbafather.domain.model.PrayerTag
import io.abbafather.testing.FakePrayerRepository
import io.abbafather.testing.testPrayer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterPrayersUseCaseTest {

    private val penitence = testPrayer(
        id = "vov-069-contrition",
        title = "Contrition",
        part = PrayerPart.PenitenceAndDeprecation,
        tags = setOf(PrayerTag.Repentance, PrayerTag.Forgiveness),
    )
    private val devotion = testPrayer(
        id = "vov-106-morning",
        title = "Morning",
        part = PrayerPart.NeedsAndDevotions,
        tags = setOf(PrayerTag.Grace),
    )
    private val filterPrayers =
        FilterPrayersUseCase(FakePrayerRepository(listOf(penitence, devotion)))

    @Test
    fun `an empty filter keeps the whole catalogue`() = runTest {
        assertEquals(listOf(penitence, devotion), filterPrayers(PrayerFilter()).first())
    }

    @Test
    fun `a tag keeps only the prayers carrying it`() = runTest {
        val filter = PrayerFilter(tags = setOf(PrayerTag.Forgiveness))

        assertEquals(listOf(penitence), filterPrayers(filter).first())
    }

    @Test
    fun `two tags widen the shelf rather than narrowing it`() = runTest {
        val filter = PrayerFilter(tags = setOf(PrayerTag.Forgiveness, PrayerTag.Grace))

        assertEquals(listOf(penitence, devotion), filterPrayers(filter).first())
    }

    @Test
    fun `a part and a tag narrow together`() = runTest {
        val impossible = PrayerFilter(
            parts = setOf(PrayerPart.NeedsAndDevotions),
            tags = setOf(PrayerTag.Forgiveness),
        )

        assertEquals(emptyList<Any>(), filterPrayers(impossible).first())
    }
}
