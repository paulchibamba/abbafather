package io.abbafather.domain.usecase

import io.abbafather.domain.model.Prayer
import io.abbafather.domain.repository.PrayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * The one prayer the home screen offers today. Chosen by the date rather than at random, so the
 * suggestion holds still while the reader looks at it and every reader gets the same one — and so a
 * screen rotation never swaps the prayer out from under them.
 */
class GetTodaysSuggestedPrayerUseCase @Inject constructor(
    private val prayerRepository: PrayerRepository,
    private val clock: Clock,
) {
    operator fun invoke(date: LocalDate = LocalDate.now(clock)): Flow<Prayer?> =
        prayerRepository.observePrayers().map { prayers -> prayers.suggestionFor(date) }

    private fun List<Prayer>.suggestionFor(date: LocalDate): Prayer? {
        if (isEmpty()) return null
        val ordered = sortedBy { it.id }
        return ordered[(date.toEpochDay() * GOLDEN_STEP).mod(ordered.size.toLong()).toInt()]
    }

    private companion object {
        /**
         * Co-prime with any catalogue size in practice, so consecutive days land far apart in the
         * catalogue instead of walking it in order.
         */
        const val GOLDEN_STEP = 2654435761L
    }
}
