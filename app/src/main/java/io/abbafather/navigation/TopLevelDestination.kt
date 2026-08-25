package io.abbafather.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import io.abbafather.R
import io.abbafather.core.designsystem.component.AbbaIcons

/**
 * The four places the bottom bar can take you. Order is the order they sit in the bar, and
 * [TopLevelDestination.entries]`.first()` is the app's start destination.
 */
enum class TopLevelDestination(
    val route: AbbaRoute,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(AbbaRoute.Home, R.string.nav_home, AbbaIcons.Home),
    LIBRARY(AbbaRoute.Library, R.string.nav_library, AbbaIcons.Book),
    MY_PRAYERS(AbbaRoute.MyPrayers, R.string.nav_my_prayers, AbbaIcons.Pencil),
    SAVED(AbbaRoute.Saved, R.string.nav_saved, AbbaIcons.Bookmark),
}

val StartDestination: AbbaRoute = TopLevelDestination.entries.first().route
