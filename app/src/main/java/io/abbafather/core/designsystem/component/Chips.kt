package io.abbafather.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.abbafather.core.designsystem.theme.AbbaShapes
import io.abbafather.core.designsystem.theme.AbbaTheme

/**
 * A theme or category the reader can switch on and off. Selection is carried by the field filling
 * with sage rather than by a tick or an outline.
 */
@Composable
fun SelectableChip(
    label: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AbbaTheme.colors
    Text(
        text = label,
        style = AbbaTheme.type.chipLabel,
        color = if (isSelected) colors.oat else colors.inkProse,
        modifier = modifier
            .clip(AbbaShapes.Pill)
            .pressableSurface(
                shape = AbbaShapes.Pill,
                containerColor = if (isSelected) colors.sage else colors.card,
                pressedContainerColor = if (isSelected) colors.sagePressed else colors.cardPressed,
                onClick = onToggle,
                role = Role.Checkbox,
            )
            .padding(horizontal = 15.dp, vertical = 11.dp),
    )
}

/** A category already attached to a saved line or a written prayer. Nothing to tap, only to read. */
@Composable
fun TagChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = AbbaTheme.colors
    Text(
        text = label,
        style = AbbaTheme.type.tagLabel,
        color = colors.sage,
        modifier = modifier
            .clip(AbbaShapes.Pill)
            .pressableSurface(shape = AbbaShapes.Pill, containerColor = colors.sageTint, role = null)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}
