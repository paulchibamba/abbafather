package io.abbafather.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.abbafather.core.designsystem.theme.AbbaTheme

/**
 * A label with no field behind it — "Leave", "Whole prayer", "Make it my prayer". It has nothing to
 * shift on press but its own colour, which is exactly what the design does.
 */
@Composable
fun TextActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = AbbaTheme.colors.sage,
    pressedContentColor: Color = AbbaTheme.colors.inkOnTint,
    textStyle: TextStyle = AbbaTheme.type.hintSans,
    trailingIcon: @Composable ((Color) -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedColor by animateColorAsState(
        targetValue = if (isPressed) pressedContentColor else contentColor,
        animationSpec = tween(durationMillis = 140),
        label = "textActionColor",
    )
    Row(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = textStyle, color = animatedColor)
        if (trailingIcon != null) trailingIcon(animatedColor)
    }
}
