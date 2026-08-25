package io.abbafather.domain.usecase

import io.abbafather.domain.repository.PrayerRepository
import java.time.Clock
import javax.inject.Inject

/** Stamps a prayer as opened so the home screen's recent rows are in the order they were prayed. */
class RecordPrayerOpenedUseCase @Inject constructor(
    private val prayerRepository: PrayerRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(prayerId: String) {
        prayerRepository.recordPrayerOpened(prayerId, clock.millis())
    }
}
