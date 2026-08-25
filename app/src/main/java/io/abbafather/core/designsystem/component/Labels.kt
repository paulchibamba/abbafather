package io.abbafather.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.abbafather.core.designsystem.theme.AbbaTheme

/**
 * The small tracked-out uppercase line that names a block of a screen. It replaces the headings and
 * rules the design deliberately does without.
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AbbaTheme.colors.inkMeta,
) {
    Text(
        text = text.uppercase(),
        style = AbbaTheme.type.sectionLabel,
        color = color,
        modifier = modifier,
    )
}

/** The app's own name, set even smaller and wider than a section label. */
@Composable
fun BrandEyebrow(
    modifier: Modifier = Modifier,
    text: String = "Abba, Father",
    color: Color = AbbaTheme.colors.sage,
) {
    Text(
        text = text.uppercase(),
        style = AbbaTheme.type.brandEyebrow,
        color = color,
        modifier = modifier,
    )
}
