package io.abbafather.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * One committed look. The design is a single warm field per screen rather than a light and a dark
 * variant, so there is no dark scheme and no dynamic colour to dilute it — the prayer session brings
 * its own deep forest surface instead.
 */
@Composable
fun AbbaTheme(content: @Composable () -> Unit) {
    val colors = AbbaLightColors
    CompositionLocalProvider(
        LocalAbbaColors provides colors,
        LocalAbbaTypeScale provides AbbaDefaultTypeScale,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = AbbaMaterialTypography,
            shapes = AbbaMaterialShapes,
            content = content,
        )
    }
}

object AbbaTheme {
    val colors: AbbaColorScheme
        @Composable @ReadOnlyComposable get() = LocalAbbaColors.current

    val type: AbbaTypeScale
        @Composable @ReadOnlyComposable get() = LocalAbbaTypeScale.current
}

private fun AbbaColorScheme.toMaterialColorScheme() = lightColorScheme(
    primary = sage,
    onPrimary = oat,
    primaryContainer = sageTint,
    onPrimaryContainer = inkOnTint,
    secondary = mutedSage,
    onSecondary = oat,
    secondaryContainer = card,
    onSecondaryContainer = ink,
    tertiary = moss,
    onTertiary = deepForest,
    tertiaryContainer = clay,
    onTertiaryContainer = ink,
    background = oat,
    onBackground = ink,
    surface = oat,
    onSurface = ink,
    surfaceVariant = card,
    onSurfaceVariant = inkMeta,
    surfaceContainer = card,
    surfaceContainerHigh = cardPressed,
    surfaceContainerLow = oat,
    outline = inkHandle,
    outlineVariant = inkHandle,
    scrim = inkScrim,
    inverseSurface = deepForest,
    inverseOnSurface = oat,
    error = Color(0xFF8C3A2E),
    onError = oat,
)
