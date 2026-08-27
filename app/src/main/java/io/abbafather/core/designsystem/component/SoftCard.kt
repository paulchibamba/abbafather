package io.abbafather.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.abbafather.core.designsystem.theme.AbbaShapes
import io.abbafather.core.designsystem.theme.AbbaTheme

/**
 * A rounded field of colour holding content. The design has no borders, dividers or elevation — a
 * card is simply a warmer patch of the ground with air inside it.
 */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    shape: Shape = AbbaShapes.Tile,
    containerColor: Color = AbbaTheme.colors.card,
    pressedContainerColor: Color = AbbaTheme.colors.cardPressed,
    contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 20.dp),
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .pressableSurface(
                shape = shape,
                containerColor = containerColor,
                pressedContainerColor = pressedContainerColor,
                onClick = onClick,
                onClickLabel = onClickLabel,
            )
            .padding(contentPadding),
        content = content,
    )
}
