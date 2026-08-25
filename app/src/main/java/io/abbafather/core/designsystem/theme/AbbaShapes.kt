package io.abbafather.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Nothing in this design is ruled off; separation comes from colour fields and corner radius, so the
 * radii are part of the vocabulary rather than an afterthought.
 */
object AbbaShapes {
    val Pill = RoundedCornerShape(percent = 50)
    val SavedCard = RoundedCornerShape(30.dp)
    val PersonalPrayerCard = RoundedCornerShape(28.dp)
    val Tile = RoundedCornerShape(24.dp)
    val ListRow = RoundedCornerShape(22.dp)
    val PrayerLine = RoundedCornerShape(14.dp)
    val HomeHeader = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
    val BottomSheet = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
}

internal val AbbaMaterialShapes = Shapes(
    extraSmall = AbbaShapes.PrayerLine,
    small = AbbaShapes.ListRow,
    medium = AbbaShapes.Tile,
    large = AbbaShapes.PersonalPrayerCard,
    extraLarge = AbbaShapes.SavedCard,
)
