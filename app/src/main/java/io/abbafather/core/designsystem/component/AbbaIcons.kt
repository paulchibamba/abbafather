package io.abbafather.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

/**
 * The design draws its own icons as thin open strokes rather than using a Material set, so they are
 * transcribed here from its SVG paths. Every one is a 24x24 viewport with an unfilled stroke; colour
 * comes from the caller's tint, not from the vector.
 */
object AbbaIcons {

    val ArrowRight: ImageVector = strokeIcon("ArrowRight") {
        moveTo(5f, 12f); horizontalLineToRelative(14f)
        moveTo(13f, 6f); lineToRelative(6f, 6f); lineToRelative(-6f, 6f)
    }

    val BackChevron: ImageVector = strokeIcon("BackChevron") {
        moveTo(15f, 5f); lineToRelative(-7f, 7f); lineToRelative(7f, 7f)
    }

    val Home: ImageVector = strokeIcon("Home") {
        moveTo(4f, 11f); lineTo(12f, 4f); lineToRelative(8f, 7f)
        moveTo(6f, 10f); verticalLineToRelative(10f); horizontalLineToRelative(12f); verticalLineTo(10f)
    }

    val Book: ImageVector = strokeIcon("Book") {
        moveTo(12f, 7f)
        reflectiveCurveTo(10f, 5f, 3.5f, 5.5f)
        verticalLineToRelative(13f)
        curveTo(10f, 18f, 12f, 20f, 12f, 20f)
        reflectiveCurveToRelative(2f, -2f, 8.5f, -1.5f)
        verticalLineToRelative(-13f)
        curveTo(14f, 5f, 12f, 7f, 12f, 7f)
        close()
        moveTo(12f, 7f); verticalLineToRelative(13f)
    }

    val Pencil: ImageVector = strokeIcon("Pencil") {
        moveTo(4f, 20f); horizontalLineToRelative(16f)
        moveTo(14.5f, 4.5f); lineTo(19f, 9f); lineTo(9f, 19f)
        horizontalLineTo(4.5f); verticalLineTo(14.5f); close()
    }

    val Bookmark: ImageVector = strokeIcon("Bookmark") {
        moveTo(6.5f, 4f); horizontalLineToRelative(11f); verticalLineToRelative(16f)
        lineToRelative(-5.5f, -4.2f); lineTo(6.5f, 20f); close()
    }

    val Plus: ImageVector = strokeIcon("Plus") {
        moveTo(12f, 5f); verticalLineToRelative(14f)
        moveTo(5f, 12f); horizontalLineToRelative(14f)
    }

    val Search: ImageVector = strokeIcon("Search", strokeWidth = 1.7f) {
        moveTo(18f, 11f)
        arcToRelative(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, -14f, 0f)
        arcToRelative(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, 14f, 0f)
        close()
        moveTo(16.5f, 16.5f); lineTo(21f, 21f)
    }
}

private fun strokeIcon(
    name: String,
    strokeWidth: Float = 1.6f,
    pathData: PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    addPath(
        pathData = PathBuilder().apply(pathData).nodes,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
}.build()
