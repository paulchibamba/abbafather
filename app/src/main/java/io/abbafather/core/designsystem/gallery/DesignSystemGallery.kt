package io.abbafather.core.designsystem.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.abbafather.core.designsystem.component.AbbaIcons
import io.abbafather.core.designsystem.component.BrandEyebrow
import io.abbafather.core.designsystem.component.PillButton
import io.abbafather.core.designsystem.component.PillButtonDefaults
import io.abbafather.core.designsystem.component.RoundIconButton
import io.abbafather.core.designsystem.component.SectionLabel
import io.abbafather.core.designsystem.component.SelectableChip
import io.abbafather.core.designsystem.component.SoftCard
import io.abbafather.core.designsystem.component.TagChip
import io.abbafather.core.designsystem.component.TextActionButton
import io.abbafather.core.designsystem.theme.AbbaShapes
import io.abbafather.core.designsystem.theme.AbbaTheme

/**
 * A scrolling proof sheet for the design language. It exists so the palette, the type scale and every
 * component can be judged against the design on a real device before any screen is built on them.
 * It is not part of the product and is removed once the screens exist.
 */
@Composable
fun DesignSystemGallery(modifier: Modifier = Modifier) {
    val colors = AbbaTheme.colors
    val type = AbbaTheme.type
    var selectedChips by remember { mutableStateOf(setOf("Peace")) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.oat)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 44.dp),
        verticalArrangement = Arrangement.spacedBy(36.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BrandEyebrow()
            Text("Design system", style = type.screenTitle, color = colors.ink)
            Text(
                "Every colour, voice and component the app is built from.",
                style = type.bodySans,
                color = colors.inkSubtle,
            )
        }

        GallerySection("Colour") {
            ColorSwatch("oat", colors.oat, colors.ink)
            ColorSwatch("card", colors.card, colors.ink)
            ColorSwatch("cardPressed", colors.cardPressed, colors.ink)
            ColorSwatch("clay", colors.clay, colors.ink)
            ColorSwatch("sageTint", colors.sageTint, colors.inkOnTint)
            ColorSwatch("moss", colors.moss, colors.deepForest)
            ColorSwatch("mutedSage", colors.mutedSage, colors.oat)
            ColorSwatch("sage", colors.sage, colors.oat)
            ColorSwatch("sagePressed", colors.sagePressed, colors.oat)
            ColorSwatch("ink", colors.ink, colors.oat)
            ColorSwatch("inkOnTint", colors.inkOnTint, colors.oat)
            ColorSwatch("deepForest", colors.deepForest, colors.oat)
        }

        GallerySection("The devotional voice") {
            TypeSpecimen("homeGreeting", "Good evening, Paul.", type.homeGreeting)
            TypeSpecimen("screenTitle", "My prayers", type.screenTitle)
            TypeSpecimen("readerTitle", "A Collect for Peace", type.readerTitle)
            TypeSpecimen("homeVerse", "“Be still, and know that I am God.”", type.homeVerse)
            TypeSpecimen("sessionLine", "Lighten our darkness, we beseech thee, O Lord;", type.sessionLine)
            TypeSpecimen("readerLine", "my soul thirsteth for thee, my flesh longeth for thee.", type.readerLine)
            TypeSpecimen("savedLine", "You were with me, and I was not with you.", type.savedLine)
            TypeSpecimen("cardTitle", "Aid Against All Perils", type.cardTitle)
            TypeSpecimen("prayerExcerpt", "You know the years I cannot see.", type.prayerExcerpt)
        }

        GallerySection("The functional voice") {
            TypeSpecimen("bodySans", "Your own words, kept as you wrote them.", type.bodySans)
            TypeSpecimen("hintSans", "Tap a line to keep it.", type.hintSans)
            TypeSpecimen("metaSans", "Book of Common Prayer, 1662", type.metaSans)
            TypeSpecimen("chipLabel", "Thanksgiving", type.chipLabel)
            TypeSpecimen("navLabel", "LIBRARY", type.navLabel)
            TypeSpecimen("sectionLabel", "FOR THIS EVENING", type.sectionLabel)
        }

        GallerySection("Buttons") {
            PillButton(
                text = "Begin prayer",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                height = 64.dp,
                textStyle = type.primaryButtonLabel,
                trailingIcon = {
                    Icon(AbbaIcons.ArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
            PillButton("Keep this line", {}, Modifier.fillMaxWidth(), height = 56.dp)
            PillButton("Make it my prayer", {}, Modifier.fillMaxWidth(), PillButtonDefaults.card, 54.dp)
            PillButton("Pray this", {}, colors = PillButtonDefaults.tinted, height = 46.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoundIconButton(AbbaIcons.Plus, "Write a new prayer", {})
                RoundIconButton(
                    icon = AbbaIcons.BackChevron,
                    contentDescription = "Go back",
                    onClick = {},
                    size = 44.dp,
                    containerColor = Color.Transparent,
                    pressedContainerColor = colors.card,
                    contentColor = colors.ink,
                )
            }
            TextActionButton("Make it my prayer", {}, trailingIcon = { tint ->
                Icon(AbbaIcons.ArrowRight, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
            })
        }

        GallerySection("On the deep forest") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AbbaShapes.SavedCard)
                    .background(colors.deepForest)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Ambient sound off", style = type.metaSans, color = colors.oatAmbientLabel)
                Text(
                    "Lighten our darkness, we beseech thee, O Lord;",
                    style = type.sessionLine,
                    color = colors.oat,
                )
                Text(
                    "and by thy great mercy defend us from all perils.",
                    style = type.sessionLine,
                    color = colors.oatSpent,
                )
                Text("Stay here a moment. Breathe.", style = type.bodySans, color = colors.moss)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                    repeat(5) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(AbbaShapes.Pill)
                                .background(if (index < 3) colors.moss else colors.oatTick),
                        )
                    }
                }
                PillButton("Amen", {}, Modifier.fillMaxWidth(), PillButtonDefaults.oatOnForest, 58.dp, type.amenButtonLabel)
                PillButton("Read again", {}, Modifier.fillMaxWidth(), PillButtonDefaults.translucentOnForest, 48.dp)
            }
        }

        GallerySection("Chips") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Praise", "Thanksgiving", "Confession", "Peace", "Guidance", "Anxiety").forEach { theme ->
                    SelectableChip(
                        label = theme,
                        isSelected = theme in selectedChips,
                        onToggle = {
                            selectedChips = if (theme in selectedChips) selectedChips - theme else selectedChips + theme
                        },
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                TagChip("Family")
                TagChip("Healing")
                TagChip("God’s presence")
            }
        }

        GallerySection("Cards") {
            SoftCard(
                modifier = Modifier.fillMaxWidth(),
                shape = AbbaShapes.SavedCard,
                contentPadding = PaddingValues(24.dp),
                onClick = {},
            ) {
                Text("Aid Against All Perils", style = type.suggestedCardTitle, color = colors.ink)
                Spacer(Modifier.height(10.dp))
                Text("Evening prayer · Book of Common Prayer, 1662", style = type.metaSans, color = colors.inkMeta)
                Spacer(Modifier.height(16.dp))
                Text("Lighten our darkness, we beseech thee, O Lord;", style = type.suggestedExcerpt, color = colors.inkSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CollectionTile("Morning prayers", "7 prayers", colors.sageTint, Modifier.weight(1f))
                CollectionTile("To memorise", "3 prayers", colors.clay, Modifier.weight(1f))
            }
        }

        GallerySection("Icons") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                GalleryIcon("home", AbbaIcons.Home)
                GalleryIcon("book", AbbaIcons.Book)
                GalleryIcon("pencil", AbbaIcons.Pencil)
                GalleryIcon("bookmark", AbbaIcons.Bookmark)
                GalleryIcon("arrow", AbbaIcons.ArrowRight)
                GalleryIcon("back", AbbaIcons.BackChevron)
                GalleryIcon("plus", AbbaIcons.Plus)
                GalleryIcon("search", AbbaIcons.Search)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GallerySection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionLabel(title)
        content()
    }
}

@Composable
private fun ColorSwatch(name: String, color: Color, labelColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AbbaShapes.ListRow)
            .background(color)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = AbbaTheme.type.chipLabel, color = labelColor)
        Text(color.toHexLabel(), style = AbbaTheme.type.metaSans, color = labelColor)
    }
}

@Composable
private fun TypeSpecimen(role: String, sample: String, style: TextStyle) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(role, style = AbbaTheme.type.metaSans, color = AbbaTheme.colors.mutedSage)
        Text(sample, style = style, color = AbbaTheme.colors.ink)
    }
}

@Composable
private fun CollectionTile(name: String, count: String, background: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(104.dp)
            .clip(AbbaShapes.Tile)
            .background(background)
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(name, style = AbbaTheme.type.collectionName, color = AbbaTheme.colors.ink)
        Spacer(Modifier.height(6.dp))
        Text(count, style = AbbaTheme.type.tagLabel, color = AbbaTheme.colors.inkMeta)
    }
}

@Composable
private fun GalleryIcon(name: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = AbbaTheme.colors.sage, modifier = Modifier.size(26.dp))
        Text(name, style = AbbaTheme.type.metaSans, color = AbbaTheme.colors.inkMeta)
    }
}

private fun Color.toHexLabel(): String = "#%06X".format(toArgb() and 0xFFFFFF)

@Preview(widthDp = 390, heightDp = 3600)
@Composable
private fun DesignSystemGalleryPreview() {
    AbbaTheme { DesignSystemGallery() }
}
