package io.abbafather.core.designsystem.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.abbafather.R

/**
 * Both families ship as single variable fonts, so a weight is an axis position rather than another
 * megabyte of glyphs on disk. Cormorant Garamond carries a 300..700 weight axis, Work Sans 100..900;
 * every weight requested below sits inside its family's range.
 */
@OptIn(ExperimentalTextApi::class)
private fun variableFont(
    resourceId: Int,
    weight: FontWeight,
    style: FontStyle = FontStyle.Normal,
) = Font(
    resId = resourceId,
    weight = weight,
    style = style,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** The devotional face: prayers, headings, anything meant to read like a book. */
val CormorantGaramond = FontFamily(
    variableFont(R.font.cormorant_garamond, FontWeight.Light),
    variableFont(R.font.cormorant_garamond, FontWeight.Normal),
    variableFont(R.font.cormorant_garamond, FontWeight.Medium),
    variableFont(R.font.cormorant_garamond_italic, FontWeight.Light, FontStyle.Italic),
    variableFont(R.font.cormorant_garamond_italic, FontWeight.Normal, FontStyle.Italic),
)

/** The functional face: labels, metadata, navigation — anything that is not a prayer. */
val WorkSans = FontFamily(
    variableFont(R.font.work_sans, FontWeight.Light),
    variableFont(R.font.work_sans, FontWeight.Normal),
    variableFont(R.font.work_sans, FontWeight.Medium),
    variableFont(R.font.work_sans, FontWeight.SemiBold),
)
