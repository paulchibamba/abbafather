package io.abbafather.navigation

import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The graph is exercised through a real controller rather than a composition: what matters here is
 * that every route is reachable, that its arguments survive the trip, and that the tabs behave like
 * tabs rather than like a stack.
 */
@RunWith(RobolectricTestRunner::class)
class AbbaNavGraphTest {

    private lateinit var navController: TestNavHostController

    @Before
    fun buildGraph() {
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        navController.graph = navController.createGraph(startDestination = StartDestination) {
            abbaDestinations(navController)
        }
    }

    @Test
    fun `the app opens on Home`() {
        assertEquals(TopLevelDestination.HOME, navController.currentDestination.toTopLevelDestination())
    }

    @Test
    fun `every top-level destination is reachable and keeps the bottom bar`() {
        TopLevelDestination.entries.forEach { destination ->
            navController.navigateToTopLevelDestination(destination)
            assertEquals(destination, navController.currentDestination.toTopLevelDestination())
        }
    }

    @Test
    fun `the reader carries its prayer id and hides the bottom bar`() {
        navController.navigate(AbbaRoute.Reader(prayerId = "bcp-collect-for-peace"))

        assertNull(navController.currentDestination.toTopLevelDestination())
        assertEquals(
            AbbaRoute.Reader(prayerId = "bcp-collect-for-peace"),
            navController.currentBackStackEntry?.toRoute<AbbaRoute.Reader>(),
        )
    }

    @Test
    fun `the session carries its prayer id and hides the bottom bar`() {
        navController.navigate(AbbaRoute.Session(prayerId = "psalm-063"))

        assertNull(navController.currentDestination.toTopLevelDestination())
        assertEquals(
            AbbaRoute.Session(prayerId = "psalm-063"),
            navController.currentBackStackEntry?.toRoute<AbbaRoute.Session>(),
        )
    }

    @Test
    fun `about is reachable from home and hides the bottom bar`() {
        navController.navigate(AbbaRoute.About)

        assertNull(navController.currentDestination.toTopLevelDestination())
        assertEquals(
            AbbaRoute.About,
            navController.currentBackStackEntry?.toRoute<AbbaRoute.About>(),
        )

        navController.navigateUp()
        assertEquals(TopLevelDestination.HOME, navController.currentDestination.toTopLevelDestination())
    }

    @Test
    fun `compose opens blank, on an existing prayer, or seeded from a kept line`() {
        val openings = listOf(
            AbbaRoute.ComposePrayer(),
            AbbaRoute.ComposePrayer(personalPrayerId = "9f1c-personal"),
            AbbaRoute.ComposePrayer(seedText = "Be thou my vision, O Lord of my heart."),
        )

        openings.forEach { opening ->
            navController.navigate(opening)

            assertNull(navController.currentDestination.toTopLevelDestination())
            assertEquals(opening, navController.currentBackStackEntry?.toRoute<AbbaRoute.ComposePrayer>())
            navController.navigateUp()
        }
    }

    @Test
    fun `switching tabs does not stack them up`() {
        navController.navigateToTopLevelDestination(TopLevelDestination.LIBRARY)
        navController.navigateToTopLevelDestination(TopLevelDestination.SAVED)
        navController.navigateToTopLevelDestination(TopLevelDestination.MY_PRAYERS)

        assertEquals(
            listOf(AbbaRoute.Home::class.qualifiedName, AbbaRoute.MyPrayers::class.qualifiedName),
            navController.destinationRoutesOnBackStack(),
        )
    }

    @Test
    fun `leaving a tab and coming back restores its saved state`() {
        navController.navigateToTopLevelDestination(TopLevelDestination.LIBRARY)
        val libraryEntryId = navController.currentBackStackEntry?.id

        navController.navigateToTopLevelDestination(TopLevelDestination.SAVED)
        navController.navigateToTopLevelDestination(TopLevelDestination.LIBRARY)

        assertTrue(libraryEntryId != null)
        assertEquals(libraryEntryId, navController.currentBackStackEntry?.id)
    }

    @Test
    fun `a deep destination pops back to the tab it was opened from`() {
        navController.navigateToTopLevelDestination(TopLevelDestination.LIBRARY)
        navController.navigate(AbbaRoute.Reader(prayerId = "bcp-collect-for-peace"))
        navController.navigate(AbbaRoute.Session(prayerId = "bcp-collect-for-peace"))

        navController.navigateUp()
        navController.navigateUp()

        assertEquals(TopLevelDestination.LIBRARY, navController.currentDestination.toTopLevelDestination())
    }

    private fun NavHostController.destinationRoutesOnBackStack(): List<String?> =
        currentBackStack.value
            .mapNotNull { it.destination.route }
            .filter { route -> route.startsWith(AbbaRoute::class.qualifiedName!!) }
}
