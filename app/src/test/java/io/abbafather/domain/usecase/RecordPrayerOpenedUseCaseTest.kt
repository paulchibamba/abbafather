package io.abbafather.domain.usecase

import io.abbafather.testing.FakePrayerRepository
import io.abbafather.testing.testPrayer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RecordPrayerOpenedUseCaseTest {

    private val openedAt = Instant.parse("2026-08-25T19:30:00Z")
    private val prayerRepository = FakePrayerRepository(
        listOf(testPrayer(id = "psalm-063"), testPrayer(id = "psalm-121")),
    )
    private val recordPrayerOpened =
        RecordPrayerOpenedUseCase(prayerRepository, Clock.fixed(openedAt, ZoneOffset.UTC))

    @Test
    fun `an opened prayer leads the recent list`() = runTest {
        recordPrayerOpened("psalm-121")

        val recent = prayerRepository.observeRecentlyOpenedPrayers(limit = 5).first()

        assertEquals(listOf("psalm-121"), recent.map { it.id })
        assertEquals(openedAt.toEpochMilli(), recent.single().lastOpenedAt)
    }
}
