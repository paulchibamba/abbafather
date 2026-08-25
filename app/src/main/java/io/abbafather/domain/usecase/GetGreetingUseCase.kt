package io.abbafather.domain.usecase

import io.abbafather.domain.model.Greeting
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject

/**
 * Which greeting the home screen opens with. Night is its own answer rather than a late evening —
 * someone praying at two in the morning should not be told good evening.
 */
class GetGreetingUseCase @Inject constructor(
    private val clock: Clock,
) {
    operator fun invoke(now: LocalTime = LocalTime.now(clock)): Greeting = when (now.hour) {
        in 5..11 -> Greeting.Morning
        in 12..16 -> Greeting.Afternoon
        in 17..21 -> Greeting.Evening
        else -> Greeting.Night
    }
}
