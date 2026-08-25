package io.abbafather.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.abbafather.core.designsystem.theme.AbbaTheme
import io.abbafather.feature.home.HomeRoute

/**
 * The whole app hangs here: one host, four tabs that keep their own back stacks, and three
 * destinations that take the screen over. The bottom bar belongs to the shell rather than to any
 * screen, so a screen never has to know whether it is being shown with one.
 */
@Composable
fun AbbaNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedTab = currentBackStackEntry?.destination.toTopLevelDestination()

    Scaffold(
        modifier = modifier,
        containerColor = AbbaTheme.colors.oat,
        // Screens paint to the edges and pad themselves; only the bottom bar consumes an inset.
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            AnimatedVisibility(
                visible = selectedTab != null,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
            ) {
                AbbaBottomBar(
                    selected = selectedTab,
                    onDestinationSelected = navController::navigateToTopLevelDestination,
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = StartDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            builder = { abbaDestinations(navController) },
        )
    }
}

/**
 * The graph itself, kept out of [AbbaNavHost] so a test can build it against a controller without
 * standing up a composition.
 */
fun NavGraphBuilder.abbaDestinations(navController: NavHostController) {
    composable<AbbaRoute.Home> {
        HomeRoute(
            onOpenReader = { prayerId -> navController.navigate(AbbaRoute.Reader(prayerId)) },
            onBeginSession = { prayerId -> navController.navigate(AbbaRoute.Session(prayerId)) },
        )
    }
    composable<AbbaRoute.Library> {
        PlaceholderScreen(
            title = "Library",
            actions = listOf(
                PlaceholderAction("Open a reader") {
                    navController.navigate(AbbaRoute.Reader(SamplePrayerId))
                },
            ),
        )
    }
    composable<AbbaRoute.MyPrayers> {
        PlaceholderScreen(
            title = "My prayers",
            actions = listOf(
                PlaceholderAction("Write a prayer") {
                    navController.navigate(AbbaRoute.ComposePrayer())
                },
            ),
        )
    }
    composable<AbbaRoute.Saved> {
        PlaceholderScreen(
            title = "Saved",
            actions = listOf(
                PlaceholderAction("Grow a kept line into a prayer") {
                    navController.navigate(AbbaRoute.ComposePrayer(seedText = SampleSeedText))
                },
            ),
        )
    }
    composable<AbbaRoute.Reader> { backStackEntry ->
        val reader: AbbaRoute.Reader = backStackEntry.toRoute()
        PlaceholderScreen(
            title = "Reader",
            subtitle = "prayerId = ${reader.prayerId}",
            onBack = navController::navigateUp,
            actions = listOf(
                PlaceholderAction("Pray this") {
                    navController.navigate(AbbaRoute.Session(reader.prayerId))
                },
            ),
        )
    }
    composable<AbbaRoute.Session> { backStackEntry ->
        val session: AbbaRoute.Session = backStackEntry.toRoute()
        SessionPlaceholderScreen(
            prayerId = session.prayerId,
            onAmen = navController::navigateUp,
        )
    }
    composable<AbbaRoute.ComposePrayer> { backStackEntry ->
        val compose: AbbaRoute.ComposePrayer = backStackEntry.toRoute()
        PlaceholderScreen(
            title = "Compose",
            subtitle = "personalPrayerId = ${compose.personalPrayerId ?: "—"}\n" +
                "seedText = ${compose.seedText ?: "—"}",
            onBack = navController::navigateUp,
        )
    }
}

/**
 * Switching tabs is a sideways move, not a deeper one: the tab you leave keeps its back stack and its
 * scroll position, and the tab you arrive at is restored rather than rebuilt.
 */
fun NavController.navigateToTopLevelDestination(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Null on the destinations that take the screen over — which is exactly when the bar hides. */
fun NavDestination?.toTopLevelDestination(): TopLevelDestination? {
    val destination = this ?: return null
    return TopLevelDestination.entries.firstOrNull { destination.hasRoute(it.route::class) }
}

private const val SamplePrayerId = "bcp-collect-for-peace"
private const val SampleSeedText = "Be thou my vision, O Lord of my heart."
