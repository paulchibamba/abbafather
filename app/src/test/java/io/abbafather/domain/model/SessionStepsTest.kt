package io.abbafather.domain.model

import io.abbafather.testing.testPrayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The steps are the whole of what a session moves through, and the session screen now draws all of
 * them at once — so their order and their count are worth pinning down here rather than through a
 * ViewModel.
 */
class SessionStepsTest {

    @Test
    fun `every line is a step, with a rest between each movement and the next`() {
        val prayer = testPrayer(
            id = "vov-062-amazing-grace",
            movementLines = listOf(
                listOf("Father, you are endlessly generous.", "Thank you for the grace."),
                listOf("No one needs your grace more than I do."),
                listOf("Hold me fast.", "Bring me home."),
            ),
        )

        assertEquals(
            listOf(
                SessionStep.Line(0),
                SessionStep.Line(1),
                SessionStep.Pause(1),
                SessionStep.Line(2),
                SessionStep.Pause(2),
                SessionStep.Line(3),
                SessionStep.Line(4),
            ),
            prayer.sessionSteps,
        )
    }

    @Test
    fun `a prayer of one movement rests nowhere`() {
        val prayer = testPrayer(
            id = "vov-001-the-valley-of-vision",
            movementLines = listOf(listOf("Lord, you are high and holy.", "Meet me here.")),
        )

        assertEquals(listOf(SessionStep.Line(0), SessionStep.Line(1)), prayer.sessionSteps)
    }

    /** Otherwise the prayer would end on a rest before a movement that never comes. */
    @Test
    fun `there is no rest after the last movement`() {
        val prayer = testPrayer(
            id = "vov-002-the-trinity",
            movementLines = listOf(listOf("Three in one."), listOf("One in three.")),
        )

        assertTrue(prayer.sessionSteps.last() is SessionStep.Line)
        assertEquals(1, prayer.sessionSteps.count { it is SessionStep.Pause })
    }

    @Test
    fun `the line steps address the prayer's own flat line list, in order`() {
        val prayer = testPrayer(
            id = "vov-003-god-the-all",
            movementLines = listOf(listOf("One.", "Two."), listOf("Three."), listOf("Four.")),
        )

        val lineIndices = prayer.sessionSteps
            .filterIsInstance<SessionStep.Line>()
            .map(SessionStep.Line::lineIndex)

        assertEquals(prayer.lines.indices.toList(), lineIndices)
    }
}
