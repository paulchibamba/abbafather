package io.abbafather.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.abbafather.core.designsystem.component.pressableSurface
import io.abbafather.core.designsystem.theme.AbbaShapes
import io.abbafather.core.designsystem.theme.AbbaTheme

private val BarHeight = 68.dp
private val PillHeight = 54.dp
private val BarInset = 7.dp

/**
 * The pill bar: one soft capsule holding four tabs, floating on the oat ground rather than sitting on
 * a ruled-off surface. Only the selected tab names itself — the others are their icon alone, so the
 * bar stays quiet until you look at it.
 */
@Composable
fun AbbaBottomBar(
    selected: TopLevelDestination?,
    onDestinationSelected: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 18.dp)
            .height(BarHeight)
            .clip(AbbaShapes.Pill)
            .pressableSurface(
                shape = AbbaShapes.Pill,
                containerColor = AbbaTheme.colors.card,
                onClick = null,
                role = null,
            )
            .padding(horizontal = BarInset),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopLevelDestination.entries.forEach { destination ->
            BottomBarTab(
                destination = destination,
                isSelected = destination == selected,
                onClick = { onDestinationSelected(destination) },
            )
        }
    }
}

@Composable
private fun BottomBarTab(
    destination: TopLevelDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(destination.labelRes)
    val contentColor = if (isSelected) AbbaTheme.colors.oat else AbbaTheme.colors.inkSubtle
    Row(
        modifier = Modifier
            // The fill is what says "you are here"; a screen reader is told in words.
            .semantics { selected = isSelected }
            .height(PillHeight)
            .defaultMinSize(minWidth = PillHeight)
            .clip(AbbaShapes.Pill)
            .pressableSurface(
                shape = AbbaShapes.Pill,
                containerColor = if (isSelected) AbbaTheme.colors.sage else AbbaTheme.colors.card,
                pressedContainerColor = if (isSelected) AbbaTheme.colors.sagePressed else AbbaTheme.colors.cardPressed,
                onClick = onClick,
                role = Role.Tab,
            )
            .padding(horizontal = if (isSelected) 20.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = if (isSelected) null else label,
            tint = contentColor,
            modifier = Modifier.size(21.dp),
        )
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
        ) {
            Text(
                text = label.uppercase(),
                style = AbbaTheme.type.navLabel,
                color = contentColor,
            )
        }
    }
}

@Preview
@Composable
private fun AbbaBottomBarPreview() {
    AbbaTheme {
        AbbaBottomBar(selected = TopLevelDestination.HOME, onDestinationSelected = {})
    }
}
