package io.abbafather.domain.usecase

import io.abbafather.domain.model.PersonalPrayer
import io.abbafather.domain.model.SavedLine
import io.abbafather.domain.repository.PersonalPrayerRepository
import io.abbafather.domain.util.IdGenerator
import java.time.Clock
import javax.inject.Inject

/**
 * "Make it my prayer" — a kept line becomes the opening of something the reader writes. The draft is
 * stored straight away so the compose screen has a row to edit rather than a pending creation, and
 * the blank line after the seed is where they will start writing.
 */
class CreatePersonalPrayerFromLineUseCase @Inject constructor(
    private val personalPrayerRepository: PersonalPrayerRepository,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) {
    suspend operator fun invoke(savedLine: SavedLine): PersonalPrayer {
        val now = clock.millis()
        val personalPrayer = PersonalPrayer(
            id = idGenerator.newId(),
            title = savedLine.sourcePrayerTitle?.let { "After $it" }.orEmpty(),
            body = savedLine.text + "\n\n",
            themes = savedLine.themes,
            seededFromSavedLineId = savedLine.id,
            createdAt = now,
            updatedAt = now,
        )
        personalPrayerRepository.upsertPersonalPrayer(personalPrayer)
        return personalPrayer
    }
}
