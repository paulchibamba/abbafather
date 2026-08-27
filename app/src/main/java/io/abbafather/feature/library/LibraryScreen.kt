package io.abbafather.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.drop
import io.abbafather.R
import io.abbafather.core.designsystem.component.AbbaIcons
import io.abbafather.core.designsystem.component.SectionLabel
import io.abbafather.core.designsystem.component.SelectableChip
import io.abbafather.core.designsystem.component.SoftCard
import io.abbafather.core.designsystem.component.TextActionButton
import io.abbafather.core.designsystem.component.pressableSurface
import io.abbafather.core.designsystem.theme.AbbaShapes
import io.abbafather.core.designsystem.theme.AbbaSpacing
import io.abbafather.core.designsystem.theme.AbbaTheme
import io.abbafather.domain.model.Prayer
import io.abbafather.domain.model.PrayerMovement
import io.abbafather.domain.model.PrayerPart
import io.abbafather.domain.model.PrayerProvenance
import io.abbafather.domain.model.PrayerTag
import io.abbafather.domain.model.PrayerVoice

/** Binds the ViewModel and turns opening a prayer into the move to the Reader. */
@Composable
fun LibraryRoute(
    onOpenReader: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LibraryScreen(
        uiState = uiState,
        onAction = { action ->
            viewModel.onAction(action)
            if (action is LibraryAction.OpenPrayer) onOpenReader(action.prayerId)
        },
        modifier = modifier,
    )
}

@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onAction: (LibraryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        // Remembered rather than hoisted, so leaving the tab and coming back lands on the same shelf.
        state = rememberLazyListState(),
        contentPadding = AbbaSpacing.ListScreenPadding,
        verticalArrangement = Arrangement.spacedBy(AbbaSpacing.CardGap),
    ) {
        item(key = "header") {
            LibraryHeader(
                searchQuery = uiState.searchQuery,
                catalogueSize = uiState.catalogueSize,
                onQueryChanged = { onAction(LibraryAction.SearchQueryChanged(it)) },
            )
        }

        if (uiState.partTiles.isNotEmpty()) {
            item(key = "parts") {
                TileBlock(label = stringResource(R.string.library_parts)) {
                    TileGrid(uiState.partTiles) { index, tile ->
                        LibraryTile(
                            name = tile.part.displayName,
                            prayerCount = tile.prayerCount,
                            isSelected = tile.isSelected,
                            // The design tints the fourth tile clay; the cycle carries on from there
                            // so no two neighbouring tiles share a colour.
                            containerColor = collectionTileColor(index),
                            onClick = { onAction(LibraryAction.TogglePart(tile.part)) },
                        )
                    }
                }
            }
        }

        if (uiState.tagChips.isNotEmpty()) {
            item(key = "tags") {
                TileBlock(label = stringResource(R.string.library_themes)) {
                    TagChipRow(
                        chips = uiState.tagChips,
                        onToggle = { tag -> onAction(LibraryAction.ToggleTag(tag)) },
                    )
                }
            }
        }

        if (uiState.isCatalogueReady) {
            item(key = "results") {
                Spacer(Modifier.height(AbbaSpacing.BlockGap - AbbaSpacing.CardGap))
                ResultsHeader(uiState = uiState, onAction = onAction)
            }
        }

        items(uiState.prayers, key = { it.id }) { prayer ->
            PrayerRow(
                prayer = prayer,
                onClick = { onAction(LibraryAction.OpenPrayer(prayer.id)) },
            )
        }

        if (uiState.isCatalogueReady && uiState.prayers.isEmpty()) {
            item(key = "nothing-found") {
                Text(
                    text = stringResource(R.string.library_nothing_found),
                    style = AbbaTheme.type.bodySans,
                    color = AbbaTheme.colors.inkSubtle,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    searchQuery: String,
    catalogueSize: Int,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.library_title),
            style = AbbaTheme.type.screenTitle,
            color = AbbaTheme.colors.ink,
        )
        if (catalogueSize > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = pluralStringResource(
                    R.plurals.library_subtitle,
                    catalogueSize,
                    catalogueSize,
                ),
                style = AbbaTheme.type.bodySans,
                color = AbbaTheme.colors.inkSubtle,
            )
        }
        Spacer(Modifier.height(24.dp))
        SearchField(query = searchQuery, onQueryChanged = onQueryChanged)
        Spacer(Modifier.height(AbbaSpacing.BlockGap - AbbaSpacing.CardGap))
    }
}

/**
 * The pill search field: no outline and no label, only the icon, the query and its hint.
 *
 * The field owns its own [TextFieldState] rather than being driven keystroke by keystroke from the
 * ViewModel. A value round-tripping through `SavedStateHandle` and back arrives a frame late, which
 * drops and reorders letters when the reader types quickly; a state-backed field edits at once and
 * tells the ViewModel afterwards. [query] still flows back in, so clearing empties the field.
 */
@Composable
private fun SearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AbbaTheme.colors
    val keyboardController = LocalSoftwareKeyboardController.current
    val textFieldState = rememberTextFieldState(query)
    val currentOnQueryChanged by rememberUpdatedState(onQueryChanged)

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .drop(1)
            .collect { typed -> currentOnQueryChanged(typed) }
    }
    LaunchedEffect(query) {
        if (query != textFieldState.text.toString()) textFieldState.setTextAndPlaceCursorAtEnd(query)
    }

    BasicTextField(
        state = textFieldState,
        modifier = modifier.fillMaxWidth(),
        textStyle = AbbaTheme.type.bodySans.copy(color = colors.ink),
        lineLimits = TextFieldLineLimits.SingleLine,
        cursorBrush = SolidColor(colors.sage),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { keyboardController?.hide() },
        decorator = { textField ->
            Row(
                modifier = Modifier
                    // A minimum, so the field grows with the reader's text size rather than
                    // clipping what they typed.
                    .heightIn(min = SearchFieldHeight)
                    .clip(AbbaShapes.Pill)
                    .pressableSurface(shape = AbbaShapes.Pill, containerColor = colors.card, role = null),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = AbbaIcons.Search,
                    contentDescription = null,
                    tint = colors.mutedSage,
                    modifier = Modifier
                        .padding(start = 22.dp, end = 14.dp)
                        .size(20.dp),
                )
                Box(modifier = Modifier
                    .weight(1f)
                    .padding(end = 22.dp)) {
                    if (textFieldState.text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.library_search_hint),
                            style = AbbaTheme.type.bodySans,
                            color = colors.inkPlaceholder,
                        )
                    }
                    textField()
                }
            }
        },
    )
}

@Composable
private fun TileBlock(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.padding(bottom = AbbaSpacing.BlockGap - AbbaSpacing.CardGap)) {
        SectionLabel(text = label)
        Spacer(Modifier.height(AbbaSpacing.SectionLabelGap))
        content()
    }
}

/**
 * Two to a row. The grid is laid out by hand rather than with a lazy grid because it lives inside a
 * lazy list, and because a shelf of eight tiles is not worth virtualising.
 */
@Composable
private fun <T> TileGrid(
    tiles: List<T>,
    modifier: Modifier = Modifier,
    tile: @Composable (index: Int, tile: T) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AbbaSpacing.CardGap),
    ) {
        tiles.chunked(2).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(AbbaSpacing.CardGap)) {
                row.forEachIndexed { columnIndex, entry ->
                    Box(modifier = Modifier.weight(1f)) { tile(rowIndex * 2 + columnIndex, entry) }
                }
                // A lone tile on the last row keeps its half of the width rather than stretching.
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LibraryTile(
    name: String,
    prayerCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = AbbaTheme.colors.card,
) {
    val colors = AbbaTheme.colors
    SoftCard(
        modifier = modifier
            .fillMaxWidth()
            // The sage fill is what says "this shelf is the one you are on".
            .semantics { selected = isSelected }
            .heightIn(min = TileMinHeight),
        shape = AbbaShapes.Tile,
        containerColor = if (isSelected) colors.sage else containerColor,
        pressedContainerColor = if (isSelected) colors.sagePressed else colors.cardPressed,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        onClick = onClick,
    ) {
        Text(
            text = name,
            style = AbbaTheme.type.collectionName,
            color = if (isSelected) colors.oat else colors.ink,
        )
        Spacer(Modifier.weight(1f, fill = true))
        Text(
            text = pluralStringResource(R.plurals.library_tile_prayer_count, prayerCount, prayerCount),
            style = AbbaTheme.type.metaSans,
            color = if (isSelected) colors.oat.copy(alpha = 0.75f) else colors.inkSubtle,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagChipRow(
    chips: List<TagChip>,
    onToggle: (PrayerTag) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AbbaSpacing.ChipGap),
        verticalArrangement = Arrangement.spacedBy(AbbaSpacing.ChipGap),
    ) {
        chips.forEach { chip ->
            SelectableChip(
                label = chip.tag.displayName,
                isSelected = chip.isSelected,
                onToggle = { onToggle(chip.tag) },
            )
        }
    }
}

@Composable
private fun ResultsHeader(
    uiState: LibraryUiState,
    onAction: (LibraryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = AbbaSpacing.SectionLabelGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionLabel(
            text = if (uiState.isNarrowed) {
                pluralStringResource(
                    R.plurals.library_matching_prayers,
                    uiState.prayers.size,
                    uiState.prayers.size,
                )
            } else {
                stringResource(R.string.library_all_prayers)
            },
        )
        if (uiState.isNarrowed) {
            Spacer(Modifier.weight(1f))
            TextActionButton(
                text = stringResource(R.string.library_clear),
                onClick = { onAction(LibraryAction.ClearNarrowing) },
            )
        }
    }
}

@Composable
private fun PrayerRow(
    prayer: Prayer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = AbbaShapes.ListRow,
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 20.dp),
        onClick = onClick,
        onClickLabel = stringResource(R.string.library_open_prayer),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prayer.title,
                    style = AbbaTheme.type.cardTitle,
                    color = AbbaTheme.colors.ink,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = prayer.attribution,
                    style = AbbaTheme.type.metaSans,
                    color = AbbaTheme.colors.inkMeta,
                )
            }
            Icon(
                imageVector = AbbaIcons.ArrowRight,
                // The row itself says what the tap does; the arrow only points.
                contentDescription = null,
                tint = AbbaTheme.colors.mutedSage,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Card, tint, card, clay — the design's four-tile cycle, repeated down the shelf. */
@Composable
private fun collectionTileColor(index: Int): Color = when (index % 4) {
    1 -> AbbaTheme.colors.sageTint
    3 -> AbbaTheme.colors.clay
    else -> AbbaTheme.colors.card
}

private val SearchFieldHeight = 52.dp
private val TileMinHeight = 104.dp

@Preview
@Composable
private fun LibraryScreenPreview() {
    AbbaTheme {
        LibraryScreen(
            uiState = LibraryUiState(
                partTiles = PrayerPart.entries.take(4).mapIndexed { index, part ->
                    PartTile(part, prayerCount = 29 - index * 4, isSelected = index == 1)
                },
                tagChips = listOf(
                    TagChip(PrayerTag.Grace, isSelected = false),
                    TagChip(PrayerTag.Repentance, isSelected = true),
                    TagChip(PrayerTag.Assurance, isSelected = false),
                ),
                prayers = listOf(
                    previewPrayer("The Valley of Vision"),
                    previewPrayer("Morning"),
                ),
                isCatalogueReady = true,
            ),
            onAction = {},
        )
    }
}

private fun previewPrayer(title: String) = Prayer(
    id = title,
    title = title,
    part = PrayerPart.NeedsAndDevotions,
    voice = PrayerVoice.Personal,
    tags = setOf(PrayerTag.Grace),
    movements = listOf(
        PrayerMovement(
            index = 0,
            heading = "Receiving this new day as mercy",
            lines = listOf("Compassionate Lord, I woke up today because your mercy carried me here."),
            firstLineIndex = 0,
        ),
    ),
    provenance = previewProvenance,
)

private val previewProvenance = PrayerProvenance(
    originalTitle = "Morning",
    originalAuthor = "Unattributed Puritan source (compiled and edited by Arthur Bennett)",
    originalSource = "The Valley of Vision: A Collection of Puritan Prayers and Devotions",
    originalPublicationDate = "1975",
    copyrightStatus = "Compilation in copyright; underlying Puritan sources are public domain.",
    adaptationType = "thematic modern adaptation",
    adaptationNote = "Contemporary prayer based on the themes of the historical source.",
)
