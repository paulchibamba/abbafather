package io.abbafather.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The design's palette, named by material rather than by Material 3 role. Material has no slot for
 * "the oat ground" or "the deep forest a prayer session drops into", so these tokens travel beside
 * the [androidx.compose.material3.ColorScheme] rather than inside it.
 */
@Immutable
data class AbbaColorScheme(
    val oat: Color,
    val card: Color,
    val cardPressed: Color,
    val clay: Color,
    val sage: Color,
    val sagePressed: Color,
    val sageTint: Color,
    val sageTintPressed: Color,
    val mutedSage: Color,
    val moss: Color,
    val ink: Color,
    val inkOnTint: Color,
    val deepForest: Color,
) {
    val inkSecondary: Color = ink.copy(alpha = 0.75f)
    val inkMeta: Color = ink.copy(alpha = 0.72f)
    val inkProse: Color = ink.copy(alpha = 0.70f)
    val inkSubtle: Color = ink.copy(alpha = 0.60f)
    val inkPlaceholder: Color = ink.copy(alpha = 0.35f)
    val inkScrim: Color = ink.copy(alpha = 0.40f)
    val inkHandle: Color = ink.copy(alpha = 0.20f)

    val oatOnForest: Color = oat
    val oatByline: Color = oat.copy(alpha = 0.60f)
    val oatQuiet: Color = oat.copy(alpha = 0.55f)
    val oatHint: Color = oat.copy(alpha = 0.45f)
    val oatSpent: Color = oat.copy(alpha = 0.38f)
    val oatTick: Color = oat.copy(alpha = 0.22f)
    val oatVeil: Color = oat.copy(alpha = 0.12f)
    val oatVeilPressed: Color = oat.copy(alpha = 0.22f)
    val oatAmbientLabel: Color = oat.copy(alpha = 0.85f)
}

val AbbaLightColors = AbbaColorScheme(
    oat = Color(0xFFF4EEE4),
    card = Color(0xFFEDE4D6),
    cardPressed = Color(0xFFE6DBC9),
    clay = Color(0xFFE8DDD2),
    sage = Color(0xFF4E5F48),
    sagePressed = Color(0xFF3D4C39),
    sageTint = Color(0xFFDFE4D5),
    sageTintPressed = Color(0xFFD3DBC6),
    mutedSage = Color(0xFF7C8F72),
    moss = Color(0xFFB9C9AC),
    ink = Color(0xFF2B2A26),
    inkOnTint = Color(0xFF2B3327),
    deepForest = Color(0xFF232A22),
)

val LocalAbbaColors = staticCompositionLocalOf { AbbaLightColors }
