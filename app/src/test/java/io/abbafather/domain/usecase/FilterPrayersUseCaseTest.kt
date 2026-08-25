package io.abbafather.domain.usecase

import io.abbafather.domain.model.PrayerFilter
import io.abbafather.domain.model.PrayerGroup
import io.abbafather.domain.model.PrayerKind
import io.abbafather.domain.model.PrayerTheme
import io.abbafather.testing.FakePrayerRepository
import io.abbafather.testing.testPrayer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterPrayersUseCaseTest {

    private val eveningCollect = testPrayer(
        id = "bcp-collect-for-peace",
        kind = PrayerKind.Evening,
        group = PrayerGroup.BookOfCommonPrayer,
        themes = setOf(PrayerTheme.Peace, PrayerTheme.Protection),
    )
    private val morningPsalm = testPrayer(
        id = "psalm-063",
        kind = PrayerKind.Psalm,
        group = PrayerGroup.Psalter,
        themes = setOf(PrayerTheme.Praise),
    )
    private val filterPrayers =
        FilterPrayersUseCase(FakePrayerRepository(listOf(eveningCollect, morningPsalm)))

    @Test
    fun `an empty filter keeps the whole catalogue`() = runTest {
        assertEquals(listOf(eveningCollect, morningPsalm), filterPrayers(PrayerFilter()).first())
    }

    @Test
    fun `a theme keeps only the prayers carrying it`() = runTest {
        val filter = PrayerFilter(themes = setOf(PrayerTheme.Protection))

        assertEquals(listOf(eveningCollect), filterPrayers(filter).first())
    }

    @Test
    fun `group and kind narrow together`() = runTest {
        val impossible = PrayerFilter(
            groups = setOf(PrayerGroup.Psalter),
            kinds = setOf(PrayerKind.Evening),
        )

        assertEquals(emptyList<Any>(), filterPrayers(impossible).first())
    }
}
