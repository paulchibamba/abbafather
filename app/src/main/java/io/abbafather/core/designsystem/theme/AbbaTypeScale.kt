package io.abbafather.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Text styles named for what they say rather than for a Material slot, because the design's scale is
 * driven by voice — a prayer line, a byline, a section label — not by a heading hierarchy.
 */
@Immutable
data class AbbaTypeScale(
    val homeGreeting: TextStyle,
    val screenTitle: TextStyle,
    val readerTitle: TextStyle,
    val homeVerse: TextStyle,
    val sessionLine: TextStyle,
    val readerLine: TextStyle,
    val savedLine: TextStyle,
    val sheetLine: TextStyle,
    val cardTitle: TextStyle,
    val suggestedCardTitle: TextStyle,
    val suggestedExcerpt: TextStyle,
    val recentRowTitle: TextStyle,
    val collectionName: TextStyle,
    val prayerExcerpt: TextStyle,
    val composeTitleField: TextStyle,
    val composeBodyField: TextStyle,
    val primaryButtonLabel: TextStyle,
    val amenButtonLabel: TextStyle,
    val bodySans: TextStyle,
    val hintSans: TextStyle,
    val metaSans: TextStyle,
    val chipLabel: TextStyle,
    val tagLabel: TextStyle,
    val navLabel: TextStyle,
    val sectionLabel: TextStyle,
    val brandEyebrow: TextStyle,
)

private val EvenlyDistributedLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun serif(
    size: Int,
    lineHeight: Double,
    weight: FontWeight = FontWeight.Light,
    letterSpacing: Double = 0.0,
    style: FontStyle = FontStyle.Normal,
) = TextStyle(
    fontFamily = CormorantGaramond,
    fontWeight = weight,
    fontStyle = style,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.em,
    lineHeightStyle = EvenlyDistributedLineHeight,
)

private fun sans(
    size: Int,
    lineHeight: Double,
    weight: FontWeight = FontWeight.Light,
    letterSpacing: Double = 0.0,
) = TextStyle(
    fontFamily = WorkSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.em,
    lineHeightStyle = EvenlyDistributedLineHeight,
)

val AbbaDefaultTypeScale = AbbaTypeScale(
    homeGreeting = serif(size = 42, lineHeight = 46.2, letterSpacing = -0.01),
    screenTitle = serif(size = 38, lineHeight = 41.8),
    readerTitle = serif(size = 36, lineHeight = 40.3),
    homeVerse = serif(size = 20, lineHeight = 30.0, style = FontStyle.Italic),
    sessionLine = serif(size = 30, lineHeight = 42.6, letterSpacing = -0.005),
    readerLine = serif(size = 23, lineHeight = 36.8),
    savedLine = serif(size = 25, lineHeight = 36.3),
    sheetLine = serif(size = 24, lineHeight = 36.0),
    cardTitle = serif(size = 23, lineHeight = 27.6, weight = FontWeight.Normal),
    suggestedCardTitle = serif(size = 27, lineHeight = 32.4, weight = FontWeight.Normal),
    suggestedExcerpt = serif(size = 19, lineHeight = 29.5),
    recentRowTitle = serif(size = 21, lineHeight = 26.3, weight = FontWeight.Normal),
    collectionName = serif(size = 19, lineHeight = 22.8, weight = FontWeight.Normal),
    prayerExcerpt = serif(size = 17, lineHeight = 27.2),
    composeTitleField = serif(size = 32, lineHeight = 38.4, weight = FontWeight.Normal),
    composeBodyField = serif(size = 21, lineHeight = 33.6),
    primaryButtonLabel = serif(size = 22, lineHeight = 22.0, weight = FontWeight.Normal, letterSpacing = 0.04),
    amenButtonLabel = serif(size = 20, lineHeight = 20.0, weight = FontWeight.Normal, letterSpacing = 0.06),
    bodySans = sans(size = 15, lineHeight = 25.5),
    hintSans = sans(size = 13, lineHeight = 22.1),
    metaSans = sans(size = 12, lineHeight = 16.8),
    chipLabel = sans(size = 13, lineHeight = 13.0, weight = FontWeight.Normal),
    tagLabel = sans(size = 11, lineHeight = 11.0, weight = FontWeight.Normal),
    navLabel = sans(size = 11, lineHeight = 11.0, weight = FontWeight.Normal, letterSpacing = 0.1),
    sectionLabel = sans(size = 10, lineHeight = 10.0, weight = FontWeight.Medium, letterSpacing = 0.2),
    brandEyebrow = sans(size = 10, lineHeight = 10.0, weight = FontWeight.Medium, letterSpacing = 0.22),
)

/**
 * Material components reach for [Typography] rather than [AbbaTypeScale], so the slots they use are
 * pointed at the nearest equivalent voice.
 */
internal val AbbaMaterialTypography = with(AbbaDefaultTypeScale) {
    Typography(
        displayLarge = homeGreeting,
        displayMedium = screenTitle,
        displaySmall = readerTitle,
        headlineLarge = screenTitle,
        headlineMedium = readerTitle,
        headlineSmall = suggestedCardTitle,
        titleLarge = cardTitle,
        titleMedium = recentRowTitle,
        titleSmall = collectionName,
        bodyLarge = bodySans,
        bodyMedium = hintSans,
        bodySmall = metaSans,
        labelLarge = chipLabel,
        labelMedium = navLabel,
        labelSmall = sectionLabel,
    )
}

val LocalAbbaTypeScale = staticCompositionLocalOf { AbbaDefaultTypeScale }
