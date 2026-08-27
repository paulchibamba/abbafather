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
import io.abbafather.core.designsystem.theme.AbbaTheme
import io.abbafather.feature.composeprayer.ComposePrayerRoute
import io.abbafather.feature.home.HomeRoute
import io.abbafather.feature.library.LibraryRoute
import io.abbafather.feature.myprayers.MyPrayersRoute
import io.abbafather.feature.reader.ReaderRoute
import io.abbafather.feature.saved.SavedRoute
import io.abbafather.feature.session.SessionRoute

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
        LibraryRoute(
            onOpenReader = { prayerId -> navController.navigate(AbbaRoute.Reader(prayerId)) },
        )
    }
    composable<AbbaRoute.MyPrayers> {
        MyPrayersRoute(
            onOpenPrayer = { personalPrayerId ->
                navController.navigate(AbbaRoute.ComposePrayer(personalPrayerId = personalPrayerId))
            },
            onWriteNewPrayer = { navController.navigate(AbbaRoute.ComposePrayer()) },
        )
    }
    composable<AbbaRoute.Saved> {
        SavedRoute(
            onOpenReader = { prayerId -> navController.navigate(AbbaRoute.Reader(prayerId)) },
            onOpenComposedPrayer = { personalPrayerId ->
                navController.navigate(AbbaRoute.ComposePrayer(personalPrayerId = personalPrayerId))
            },
        )
    }
    composable<AbbaRoute.Reader> {
        ReaderRoute(
            onBack = navController::navigateUp,
            onBeginSession = { prayerId -> navController.navigate(AbbaRoute.Session(prayerId)) },
            onOpenComposedPrayer = { personalPrayerId ->
                navController.navigate(AbbaRoute.ComposePrayer(personalPrayerId = personalPrayerId))
            },
        )
    }
    composable<AbbaRoute.Session> {
        SessionRoute(onFinish = navController::navigateUp)
    }
    composable<AbbaRoute.ComposePrayer> {
        ComposePrayerRoute(onBack = navController::navigateUp)
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
