package io.abbafather.domain.usecase

import io.abbafather.domain.model.Greeting
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.LocalTime

class GetGreetingUseCaseTest {

    private val getGreeting = GetGreetingUseCase(Clock.systemUTC())

    @Test
    fun `greets by the part of the day`() {
        assertEquals(Greeting.Morning, getGreeting(LocalTime.of(7, 30)))
        assertEquals(Greeting.Afternoon, getGreeting(LocalTime.of(13, 0)))
        assertEquals(Greeting.Evening, getGreeting(LocalTime.of(20, 15)))
    }

    @Test
    fun `the small hours are night, not a late evening`() {
        assertEquals(Greeting.Night, getGreeting(LocalTime.of(2, 0)))
        assertEquals(Greeting.Night, getGreeting(LocalTime.of(23, 59)))
    }
}
