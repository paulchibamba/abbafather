package io.abbafather.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.abbafather.core.designsystem.component.AbbaIcons
import io.abbafather.core.designsystem.component.PillButton
import io.abbafather.core.designsystem.component.PillButtonDefaults
import io.abbafather.core.designsystem.component.RoundIconButton
import io.abbafather.core.designsystem.component.SectionLabel
import io.abbafather.core.designsystem.component.SoftCard
import io.abbafather.core.designsystem.theme.AbbaSpacing
import io.abbafather.core.designsystem.theme.AbbaTheme

/**
 * Temporary. Task 4 builds the shell, not the screens, so every destination stands in as a card of
 * its own name, its arguments and the moves it can make. Each of these is replaced by the real screen
 * in tasks 5–11.
 */
data class PlaceholderAction(val label: String, val onClick: () -> Unit)

@Composable
fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: List<PlaceholderAction> = emptyList(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(AbbaSpacing.ListScreenPadding),
        verticalArrangement = Arrangement.spacedBy(AbbaSpacing.CardGap),
    ) {
        if (onBack != null) {
            RoundIconButton(
                icon = AbbaIcons.BackChevron,
                contentDescription = "Back",
                onClick = onBack,
                containerColor = AbbaTheme.colors.card,
                pressedContainerColor = AbbaTheme.colors.cardPressed,
                contentColor = AbbaTheme.colors.ink,
            )
        }
        Text(text = title, style = AbbaTheme.type.screenTitle, color = AbbaTheme.colors.ink)
        if (subtitle != null) {
            Text(text = subtitle, style = AbbaTheme.type.metaSans, color = AbbaTheme.colors.inkMeta)
        }
        actions.forEach { action ->
            PillButton(
                text = action.label,
                onClick = action.onClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SectionLabel(text = "Scroll me, then change tabs")
        // Enough rows to overflow the screen: switching tabs must come back to the same offset.
        repeat(PlaceholderRowCount) { index ->
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "$title — row ${index + 1}",
                    style = AbbaTheme.type.recentRowTitle,
                    color = AbbaTheme.colors.ink,
                )
            }
        }
    }
}

/** The session is the one destination that takes the whole window, system bars included. */
@Composable
fun SessionPlaceholderScreen(
    prayerId: String,
    onAmen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AbbaTheme.colors.deepForest),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(AbbaSpacing.SessionPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AbbaSpacing.BlockGap),
        ) {
            Text(
                text = "Session",
                style = AbbaTheme.type.sessionLine,
                color = AbbaTheme.colors.oatOnForest,
            )
            Text(
                text = "prayerId = $prayerId",
                style = AbbaTheme.type.metaSans,
                color = AbbaTheme.colors.oatQuiet,
            )
            PillButton(
                text = "Amen",
                onClick = onAmen,
                colors = PillButtonDefaults.oatOnForest,
                textStyle = AbbaTheme.type.amenButtonLabel,
            )
        }
    }
}

private const val PlaceholderRowCount = 14

@Preview
@Composable
private fun PlaceholderScreenPreview() {
    AbbaTheme {
        PlaceholderScreen(title = "Library")
    }
}
