package io.abbafather.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.abbafather.core.designsystem.theme.AbbaShapes
import io.abbafather.core.designsystem.theme.AbbaTheme

@Immutable
data class PillButtonColors(
    val containerColor: Color,
    val pressedContainerColor: Color,
    val contentColor: Color,
)

object PillButtonDefaults {

    /** The one action a screen most wants you to take: "Begin prayer", "Keep prayer", "Save to Saved". */
    val sage: PillButtonColors
        @Composable get() = with(AbbaTheme.colors) {
            PillButtonColors(sage, sagePressed, oat)
        }

    /** A quieter second choice sitting on the oat ground. */
    val card: PillButtonColors
        @Composable get() = with(AbbaTheme.colors) {
            PillButtonColors(card, cardPressed, ink)
        }

    /** Used where the action belongs to a sage-tinted field, like the reader's "Pray this". */
    val tinted: PillButtonColors
        @Composable get() = with(AbbaTheme.colors) {
            PillButtonColors(sageTint, sageTintPressed, sage)
        }

    /** For the prayer session, where buttons are lit patches of the deep forest rather than objects. */
    val translucentOnForest: PillButtonColors
        @Composable get() = with(AbbaTheme.colors) {
            PillButtonColors(oatVeil, oatVeilPressed, oat)
        }

    /** The session's "Amen" — oat inverted onto the forest. */
    val oatOnForest: PillButtonColors
        @Composable get() = with(AbbaTheme.colors) {
            PillButtonColors(oat, Color.White, deepForest)
        }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: PillButtonColors = PillButtonDefaults.sage,
    height: Dp = 56.dp,
    textStyle: TextStyle = AbbaTheme.type.bodySans,
    contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp),
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    PillButton(
        onClick = onClick,
        modifier = modifier,
        colors = colors,
        height = height,
        contentPadding = contentPadding,
    ) {
        Text(text = text, style = textStyle, color = colors.contentColor)
        if (trailingIcon != null) trailingIcon()
    }
}

@Composable
fun PillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: PillButtonColors = PillButtonDefaults.sage,
    height: Dp = 56.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .height(height)
            .defaultMinSize(minWidth = height)
            .clip(AbbaShapes.Pill)
            .pressableSurface(
                shape = AbbaShapes.Pill,
                containerColor = colors.containerColor,
                pressedContainerColor = colors.pressedContainerColor,
                onClick = onClick,
            )
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.contentColor, content = { content() })
    }
}
