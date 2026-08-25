package io.abbafather.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/** The bar's contents are a table, and the shell trusts its order. */
class TopLevelDestinationTest {

    @Test
    fun `the bar holds the four top-level destinations in design order`() {
        assertEquals(
            listOf(
                AbbaRoute.Home,
                AbbaRoute.Library,
                AbbaRoute.MyPrayers,
                AbbaRoute.Saved,
            ),
            TopLevelDestination.entries.map { it.route },
        )
    }

    @Test
    fun `the app starts on the first tab`() {
        assertEquals(AbbaRoute.Home, StartDestination)
    }

    @Test
    fun `no two tabs share a label or an icon`() {
        assertEquals(TopLevelDestination.entries.size, TopLevelDestination.entries.map { it.labelRes }.toSet().size)
        assertEquals(TopLevelDestination.entries.size, TopLevelDestination.entries.map { it.icon.name }.toSet().size)
    }
}
