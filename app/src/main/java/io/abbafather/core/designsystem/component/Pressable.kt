package io.abbafather.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role

/**
 * The design gives feedback by shifting a surface's colour, not by splashing a ripple over it, so
 * every tappable surface shares this behaviour rather than Material's default indication.
 */
fun Modifier.pressableSurface(
    shape: Shape,
    containerColor: Color,
    pressedContainerColor: Color = containerColor,
    onClick: (() -> Unit)? = null,
    role: Role? = Role.Button,
    enabled: Boolean = true,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedColor by animateColorAsState(
        targetValue = if (isPressed) pressedContainerColor else containerColor,
        animationSpec = tween(durationMillis = 140),
        label = "pressableSurfaceColor",
    )
    this
        .background(color = animatedColor, shape = shape)
        .let { modifier ->
            if (onClick == null) {
                modifier
            } else {
                modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = role,
                    onClick = onClick,
                )
            }
        }
}
