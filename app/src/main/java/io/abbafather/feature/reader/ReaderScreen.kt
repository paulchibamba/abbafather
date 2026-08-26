package io.abbafather.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
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
import io.abbafather.domain.model.ScriptureReference

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
                onAboutThisPrayer = { onAction(ReaderAction.OpenProvenance) },
                modifier = Modifier.padding(horizontal = DocumentMargin),
            )
            Spacer(Modifier.height(30.dp))
            PrayerBody(
                prayer = prayer,
                keptLineIndices = uiState.keptLineIndices,
                openLineIndex = uiState.keepSheet?.lineIndex,
                onLineSelected = { lineIndex -> onAction(ReaderAction.SelectLine(lineIndex)) },
                onOpenScripture = { movementIndex ->
                    onAction(ReaderAction.OpenScripture(movementIndex))
                },
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

    // At most one of the three is ever non-null, so these read as one sheet slot rather than three.
    if (uiState.keepSheet != null) {
        KeepLineSheet(sheet = uiState.keepSheet, onAction = onAction)
    }
    if (uiState.scriptureSheet != null) {
        ScriptureSheet(sheet = uiState.scriptureSheet, onAction = onAction)
    }
    if (uiState.provenanceSheet != null) {
        ProvenanceSheet(sheet = uiState.provenanceSheet, onAction = onAction)
    }
}

@Composable
private fun PrayerHeading(
    prayer: Prayer,
    onAboutThisPrayer: () -> Unit,
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
        TextActionButton(
            text = stringResource(R.string.reader_about_this_prayer),
            onClick = onAboutThisPrayer,
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
    onOpenScripture: (Int) -> Unit,
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
            if (movement.scriptures.isNotEmpty()) {
                MovementScriptureAction(
                    passageCount = movement.scriptures.size,
                    onClick = { onOpenScripture(movement.index) },
                )
            }
            if (movement != prayer.movements.last()) {
                BreathingPauseMark()
            }
        }
    }
}

/**
 * What this movement rests on, offered at the end of it rather than shown inside it: the reader
 * finishes the turn of praying first, and asks afterwards if they want to.
 */
@Composable
private fun MovementScriptureAction(
    passageCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spelled = stringArrayResource(R.array.reader_movement_passages_spelled)
    val numbered = pluralStringResource(
        R.plurals.reader_movement_passages,
        passageCount,
        passageCount,
    )
    TextActionButton(
        text = spelled.getOrNull(passageCount - 1) ?: numbered,
        onClick = onClick,
        contentColor = AbbaTheme.colors.mutedSage,
        modifier = modifier.padding(start = LineHitAreaInset, top = 2.dp),
    )
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
@Composable
private fun KeepLineSheet(
    sheet: KeepLineSheetUiState,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ReaderSheet(onDismiss = { onAction(ReaderAction.DismissSheet) }, modifier = modifier) {
        Text(
            text = sheet.line,
            style = AbbaTheme.type.sheetLine,
            color = AbbaTheme.colors.ink,
        )
        Spacer(Modifier.height(26.dp))

        when (sheet.stage) {
            KeepLineStage.Keep -> KeepStage(sheet = sheet, onAction = onAction)
            KeepLineStage.Kept -> KeptStage(onAction = onAction)
            KeepLineStage.AlreadyKept -> AlreadyKeptStage(onAction = onAction)
        }
    }
}

/**
 * What one movement rests on: what it holds, then each passage as a reference and the
 * translation it was read in, with our own note on why it stands here. The verse text itself is
 * never carried — that stays in the reader's own Bible.
 */
@Composable
private fun ScriptureSheet(
    sheet: ScriptureSheetUiState,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AbbaTheme.colors
    ReaderSheet(onDismiss = { onAction(ReaderAction.DismissSheet) }, modifier = modifier) {
        Text(
            text = sheet.heading,
            style = AbbaTheme.type.sheetLine,
            color = colors.ink,
        )
        if (sheet.themes.isNotEmpty()) {
            Spacer(Modifier.height(26.dp))
            SectionLabel(text = stringResource(R.string.reader_scripture_themes))
            sheet.themes.forEach { theme ->
                Spacer(Modifier.height(AbbaSpacing.SectionLabelGap))
                Text(
                    text = theme,
                    style = AbbaTheme.type.bodySans,
                    color = colors.inkProse,
                )
            }
        }
        if (sheet.passages.isNotEmpty()) {
            Spacer(Modifier.height(26.dp))
            SectionLabel(text = stringResource(R.string.reader_scripture_passages))
            sheet.passages.forEach { passage ->
                Spacer(Modifier.height(AbbaSpacing.SectionLabelGap))
                Text(
                    // A reference is functional, so it is set in the sans face — and the serif's
                    // old-style figures made "1 John 2:15-17" read as prose rather than as a place.
                    text = stringResource(
                        R.string.reader_scripture_reference,
                        passage.reference,
                        passage.translation,
                    ),
                    style = AbbaTheme.type.chipLabel,
                    color = colors.sage,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = passage.connection,
                    style = AbbaTheme.type.bodySans,
                    color = colors.inkProse,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        SheetCloseAction(onClick = { onAction(ReaderAction.DismissSheet) })
    }
}

/**
 * Where this prayer came from before it was ours. The adaptation note is shown verbatim: it is the
 * catalogue's own sentence about what was and was not carried over, and paraphrasing it here would
 * be the app claiming something the corpus does not.
 */
@Composable
private fun ProvenanceSheet(
    sheet: ProvenanceSheetUiState,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AbbaTheme.colors
    val provenance = sheet.provenance
    ReaderSheet(onDismiss = { onAction(ReaderAction.DismissSheet) }, modifier = modifier) {
        Text(
            text = sheet.adaptedTitle,
            style = AbbaTheme.type.sheetLine,
            color = colors.ink,
        )
        Spacer(Modifier.height(26.dp))

        SectionLabel(text = stringResource(R.string.reader_provenance_adapted_from))
        Spacer(Modifier.height(AbbaSpacing.SectionLabelGap))
        Text(
            text = provenance.originalTitle,
            style = AbbaTheme.type.collectionName,
            color = colors.ink,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = provenance.originalAuthor,
            style = AbbaTheme.type.metaSans,
            color = colors.inkMeta,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${provenance.originalSource}, ${provenance.originalPublicationDate}",
            style = AbbaTheme.type.metaSans,
            color = colors.inkMeta,
        )

        Spacer(Modifier.height(26.dp))
        SectionLabel(text = stringResource(R.string.reader_provenance_copyright))
        Spacer(Modifier.height(AbbaSpacing.SectionLabelGap))
        Text(
            text = provenance.copyrightStatus,
            style = AbbaTheme.type.metaSans,
            color = colors.inkMeta,
        )

        Spacer(Modifier.height(26.dp))
        SectionLabel(text = stringResource(R.string.reader_provenance_note))
        Spacer(Modifier.height(AbbaSpacing.SectionLabelGap))
        Text(
            text = provenance.adaptationNote,
            style = AbbaTheme.type.bodySans,
            color = colors.inkProse,
        )

        Spacer(Modifier.height(20.dp))
        SheetCloseAction(onClick = { onAction(ReaderAction.DismissSheet) })
    }
}

/**
 * The one shell all three sheets are built in, so a reader who has met one has met them all. The
 * content scrolls: forty-eight tag chips and three connection paragraphs both outgrow a phone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AbbaTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = AbbaShapes.BottomSheet,
        containerColor = colors.oat,
        scrimColor = colors.inkScrim,
        dragHandle = { SheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 26.dp, end = 26.dp, bottom = 12.dp),
            content = content,
        )
    }
}

/** The way out of a sheet that only tells the reader something. */
@Composable
private fun SheetCloseAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TextActionButton(
            text = stringResource(R.string.reader_sheet_close),
            onClick = onClick,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
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
        if (sheet.canShowMoreTags) {
            TextActionButton(
                text = stringResource(R.string.reader_sheet_more_tags),
                onClick = { onAction(ReaderAction.ShowMoreTags) },
            )
            Spacer(Modifier.height(14.dp))
        } else {
            Spacer(Modifier.height(26.dp))
        }
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
            themes = listOf(
                "Mercy is not owed; a morning is a gift renewed rather than a day earned.",
            ),
            scriptures = listOf(
                ScriptureReference(
                    reference = "Lamentations 3:22-23",
                    translation = "ESV",
                    connection = "The mercies that are new every morning are the ground " +
                        "of waking thankful rather than anxious.",
                ),
            ),
        ),
        PrayerMovement(
            index = 1,
            heading = "Asking for the day to matter",
            lines = listOf(
                "But I don't want to waste this day by merely getting through it.",
                "Let it matter for my soul.",
            ),
            firstLineIndex = 2,
            themes = listOf("A day is spent either for the soul or merely got through."),
            scriptures = listOf(
                ScriptureReference(
                    reference = "Psalm 90:12",
                    translation = "ESV",
                    connection = "Numbering our days is what turns a morning into a prayer " +
                        "for wisdom.",
                ),
            ),
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
