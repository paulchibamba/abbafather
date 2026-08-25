package io.abbafather.domain.usecase

import io.abbafather.domain.model.DailyVerse
import io.abbafather.domain.model.DailyVerses
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * The verse the home screen sets under the greeting. Chosen by the date the same way today's prayer
 * is, and for the same reason: a verse that changed while it was being read would be restlessness in
 * an app whose whole point is quiet.
 */
class GetTodaysVerseUseCase @Inject constructor(
    private val clock: Clock,
) {
    operator fun invoke(date: LocalDate = LocalDate.now(clock)): DailyVerse =
        DailyVerses[(date.toEpochDay() * GOLDEN_STEP).mod(DailyVerses.size.toLong()).toInt()]

    private companion object {
        /** Odd and large, so consecutive days land far apart in the list rather than walking it. */
        const val GOLDEN_STEP = 2654435761L
    }
}
