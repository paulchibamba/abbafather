package io.abbafather.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.abbafather.core.designsystem.theme.AbbaTheme

/** The circular actions: the sage add button on My prayers, and the back chevron on a document. */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    iconSize: Dp = 22.dp,
    containerColor: Color = AbbaTheme.colors.sage,
    pressedContainerColor: Color = AbbaTheme.colors.sagePressed,
    contentColor: Color = AbbaTheme.colors.oat,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .pressableSurface(
                shape = CircleShape,
                containerColor = containerColor,
                pressedContainerColor = pressedContainerColor,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize),
        )
    }
}
