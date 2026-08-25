package io.abbafather.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.abbafather.R
import io.abbafather.core.designsystem.component.AbbaIcons
import io.abbafather.core.designsystem.component.PillButton
import io.abbafather.core.designsystem.component.PillButtonDefaults
import io.abbafather.core.designsystem.component.RoundIconButton
import io.abbafather.core.designsystem.component.SelectableChip
import io.abbafather.core.designsystem.component.SectionLabel
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

/**
 * Binds the ViewModel and performs every move the reader makes from here. Growing a line into a
 * prayer is the one move whose destination is not known until the draft exists, so it arrives as an
 * event rather than as a return value of the action.
 */
@Composable
fun ReaderRoute(
    onBack: () -> Unit,
    onBeginSession: (String) -> Unit,
    onOpenComposedPrayer: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ReaderEvent.OpenComposedPrayer -> onOpenComposedPrayer(event.personalPrayerId)
            }
        }
    }

    ReaderScreen(
        uiState = uiState,
        onAction = { action ->
            viewModel.onAction(action)
            when (action) {
                ReaderAction.Back -> onBack()
                ReaderAction.PrayThis -> uiState.prayer?.let { onBeginSession(it.id) }
                else -> Unit
            }
        },
        modifier = modifier,
    )
}

@Composable
fun ReaderScreen(
    uiState: ReaderUiState,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            // The sides are padded per block rather than on the root, so a prayer line's tinted
            // field can reach past the text into the margin without the text moving with it.
            .padding(vertical = DocumentMargin),
    ) {
        RoundIconButton(
            icon = AbbaIcons.BackChevron,
            contentDescription = stringResource(R.string.reader_back),
            onClick = { onAction(ReaderAction.Back) },
            size = 44.dp,
            iconSize = 20.dp,
            containerColor = AbbaTheme.colors.card,
            pressedContainerColor = AbbaTheme.colors.cardPressed,
            contentColor = AbbaTheme.colors.ink,
            modifier = Modifier.padding(horizontal = DocumentMargin),
        )

        val prayer = uiState.prayer
        if (prayer != null) {
            Spacer(Modifier.height(26.dp))
            PrayerHeading(
                prayer = prayer,
                modifier = Modifier.padding(horizontal = DocumentMargin),
            )
            Spacer(Modifier.height(30.dp))
            PrayerBody(
                prayer = prayer,
                keptLineIndices = uiState.keptLineIndices,
                openLineIndex = uiState.keepSheet?.lineIndex,
                onLineSelected = { lineIndex -> onAction(ReaderAction.SelectLine(lineIndex)) },
                modifier = Modifier.padding(horizontal = DocumentMargin - LineHitAreaInset),
            )
            Spacer(Modifier.height(36.dp))
            PillButton(
                text = stringResource(R.string.reader_pray_this),
                onClick = { onAction(ReaderAction.PrayThis) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DocumentMargin),
                colors = PillButtonDefaults.tinted,
                height = 64.dp,
                textStyle = AbbaTheme.type.primaryButtonLabel,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.reader_keep_hint),
                style = AbbaTheme.type.hintSans,
                color = AbbaTheme.colors.inkSubtle,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 12.dp),
            )
        }
    }

    if (uiState.keepSheet != null) {
        KeepLineSheet(sheet = uiState.keepSheet, onAction = onAction)
    }
}

@Composable
private fun PrayerHeading(
    prayer: Prayer,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = prayer.title,
            style = AbbaTheme.type.readerTitle,
            color = AbbaTheme.colors.ink,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = prayer.attribution,
            style = AbbaTheme.type.metaSans,
            color = AbbaTheme.colors.inkMeta,
        )
    }
}

/**
 * The prayer movement by movement: each one named, then set line by line with every line its own tap
 * target. A line's hit area reaches past the text into the margin, so its tinted field can have air
 * inside it while the text still starts flush with the screen's own margin.
 */
@Composable
private fun PrayerBody(
    prayer: Prayer,
    keptLineIndices: Set<Int>,
    openLineIndex: Int?,
    onLineSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        prayer.movements.forEach { movement ->
            MovementHeading(heading = movement.heading)
            movement.lines.forEachIndexed { position, line ->
                val lineIndex = movement.firstLineIndex + position
                PrayerLine(
                    line = line,
                    isKept = lineIndex in keptLineIndices,
                    isOpen = lineIndex == openLineIndex,
                    onClick = { onLineSelected(lineIndex) },
                )
            }
            if (movement != prayer.movements.last()) {
                BreathingPauseMark()
            }
        }
    }
}

/** What this turn of the prayer is asking. Quiet enough to read past, there when you look for it. */
@Composable
private fun MovementHeading(
    heading: String,
    modifier: Modifier = Modifier,
) {
    SectionLabel(
        text = heading,
        color = AbbaTheme.colors.mutedSage,
        modifier = modifier.padding(start = LineHitAreaInset, top = 8.dp, bottom = 14.dp),
    )
}

@Composable
private fun PrayerLine(
    line: String,
    isKept: Boolean,
    isOpen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AbbaTheme.colors
    val isTinted = isKept || isOpen
    Text(
        text = line,
        style = AbbaTheme.type.readerLine,
        color = if (isTinted) colors.inkOnTint else colors.inkProse,
        modifier = modifier
            .fillMaxWidth()
            .clip(AbbaShapes.PrayerLine)
            .pressableSurface(
                shape = AbbaShapes.PrayerLine,
                containerColor = if (isTinted) colors.sageTint else colors.oat,
                pressedContainerColor = colors.sageTintPressed,
                onClick = onClick,
                role = Role.Button,
            )
            .padding(horizontal = LineHitAreaInset, vertical = 8.dp),
    )
}

/** Where the session will rest. In the reader it is only a mark: air and a short moss rule. */
@Composable
private fun BreathingPauseMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = LineHitAreaInset, vertical = 18.dp)
            .width(44.dp)
            .height(2.dp)
            .clip(AbbaShapes.Pill)
            .pressableSurface(
                shape = AbbaShapes.Pill,
                containerColor = AbbaTheme.colors.moss,
                role = null,
            ),
    )
}

/**
 * One field that says three things: keep this line, it is kept, or it was kept already. The stages
 * share the line and the handle so the sheet reads as one place the reader stays in, rather than as
 * three sheets replacing each other.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeepLineSheet(
    sheet: KeepLineSheetUiState,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AbbaTheme.colors
    ModalBottomSheet(
        onDismissRequest = { onAction(ReaderAction.DismissSheet) },
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = AbbaShapes.BottomSheet,
        containerColor = colors.oat,
        scrimColor = colors.inkScrim,
        dragHandle = { SheetHandle() },
    ) {
        Column(
            modifier = Modifier.padding(start = 26.dp, end = 26.dp, bottom = 12.dp),
        ) {
            Text(
                text = sheet.line,
                style = AbbaTheme.type.sheetLine,
                color = colors.ink,
            )
            Spacer(Modifier.height(26.dp))

            when (sheet.stage) {
                KeepLineStage.Keep -> KeepStage(sheet = sheet, onAction = onAction)
                KeepLineStage.Kept -> KeptStage(onAction = onAction)
                KeepLineStage.AlreadyKept -> AlreadyKeptStage(onAction = onAction)
            }
        }
    }
}

@Composable
private fun SheetHandle(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .padding(vertical = 14.dp)
                .width(40.dp)
                .height(4.dp)
                .clip(AbbaShapes.Pill)
                .pressableSurface(
                    shape = AbbaShapes.Pill,
                    containerColor = AbbaTheme.colors.inkHandle,
                    role = null,
                ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeepStage(
    sheet: KeepLineSheetUiState,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionLabel(text = stringResource(R.string.reader_sheet_tag_it))
        Spacer(Modifier.height(AbbaSpacing.SectionLabelGap))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AbbaSpacing.ChipGap),
            verticalArrangement = Arrangement.spacedBy(AbbaSpacing.ChipGap),
        ) {
            sheet.tagChips.forEach { chip ->
                SelectableChip(
                    label = chip.tag.displayName,
                    isSelected = chip.isSelected,
                    onToggle = { onAction(ReaderAction.ToggleTag(chip.tag)) },
                )
            }
        }
        Spacer(Modifier.height(26.dp))
        SheetActions(
            primaryText = stringResource(R.string.reader_sheet_keep_line),
            onPrimary = { onAction(ReaderAction.KeepLine) },
            quietText = stringResource(R.string.reader_sheet_leave),
            onQuiet = { onAction(ReaderAction.DismissSheet) },
        )
    }
}

@Composable
private fun KeptStage(
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.reader_sheet_kept),
            style = AbbaTheme.type.bodySans,
            color = AbbaTheme.colors.sage,
        )
        Spacer(Modifier.height(20.dp))
        SheetActions(
            primaryText = stringResource(R.string.reader_sheet_make_it_mine),
            onPrimary = { onAction(ReaderAction.GrowIntoPrayer) },
            quietText = stringResource(R.string.reader_sheet_done),
            onQuiet = { onAction(ReaderAction.DismissSheet) },
        )
    }
}

@Composable
private fun AlreadyKeptStage(
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.reader_sheet_already_kept),
            style = AbbaTheme.type.bodySans,
            color = AbbaTheme.colors.sage,
        )
        Spacer(Modifier.height(20.dp))
        SheetActions(
            primaryText = stringResource(R.string.reader_sheet_make_it_mine),
            onPrimary = { onAction(ReaderAction.GrowIntoPrayer) },
            secondaryText = stringResource(R.string.reader_sheet_release),
            onSecondary = { onAction(ReaderAction.ReleaseKeptLine) },
            quietText = stringResource(R.string.reader_sheet_leave),
            onQuiet = { onAction(ReaderAction.DismissSheet) },
        )
    }
}

/** The sheet's buttons always stack the same way: the sage move, a quieter one, then a way out. */
@Composable
private fun SheetActions(
    primaryText: String,
    onPrimary: () -> Unit,
    quietText: String,
    onQuiet: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        PillButton(
            text = primaryText,
            onClick = onPrimary,
            modifier = Modifier.fillMaxWidth(),
            height = 56.dp,
            textStyle = AbbaTheme.type.bodySans,
        )
        if (secondaryText != null && onSecondary != null) {
            Spacer(Modifier.height(AbbaSpacing.CardGap))
            PillButton(
                text = secondaryText,
                onClick = onSecondary,
                modifier = Modifier.fillMaxWidth(),
                colors = PillButtonDefaults.card,
                height = 54.dp,
                textStyle = AbbaTheme.type.bodySans,
            )
        }
        TextActionButton(
            text = quietText,
            onClick = onQuiet,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/** 26 all round for a document, per the design. */
private val DocumentMargin = 26.dp

/** The design lets a line's tinted field breathe past the text without moving the text itself. */
private val LineHitAreaInset = 12.dp

@Preview
@Composable
private fun ReaderScreenPreview() {
    AbbaTheme {
        ReaderScreen(
            uiState = ReaderUiState(
                prayer = previewPrayer,
                keptLineIndices = setOf(1),
                isLoaded = true,
            ),
            onAction = {},
        )
    }
}

private val previewPrayer = Prayer(
    id = "vov-106-morning",
    title = "Morning",
    part = PrayerPart.NeedsAndDevotions,
    voice = PrayerVoice.Personal,
    tags = setOf(PrayerTag.Grace, PrayerTag.MorningAndEvening),
    movements = listOf(
        PrayerMovement(
            index = 0,
            heading = "Receiving this new day as mercy",
            lines = listOf(
                "Compassionate Lord, I woke up today because your mercy carried me here.",
                "Thank you for the gift of another morning.",
            ),
            firstLineIndex = 0,
        ),
        PrayerMovement(
            index = 1,
            heading = "Asking for the day to matter",
            lines = listOf(
                "But I don't want to waste this day by merely getting through it.",
                "Let it matter for my soul.",
            ),
            firstLineIndex = 2,
        ),
    ),
    provenance = PrayerProvenance(
        originalTitle = "Morning",
        originalAuthor = "Unattributed Puritan source (compiled and edited by Arthur Bennett)",
        originalSource = "The Valley of Vision: A Collection of Puritan Prayers and Devotions",
        originalPublicationDate = "1975",
        copyrightStatus = "Compilation in copyright; underlying Puritan sources are public domain.",
        adaptationType = "thematic modern adaptation",
        adaptationNote = "Contemporary prayer based on the themes of the historical source.",
    ),
)
