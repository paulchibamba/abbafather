package io.abbafather.feature.composeprayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.abbafather.R
import io.abbafather.core.designsystem.component.AbbaIcons
import io.abbafather.core.designsystem.component.PillButton
import io.abbafather.core.designsystem.component.PillButtonDefaults
import io.abbafather.core.designsystem.component.RoundIconButton
import io.abbafather.core.designsystem.component.SectionLabel
import io.abbafather.core.designsystem.component.SelectableChip
import io.abbafather.core.designsystem.component.TextActionButton
import io.abbafather.core.designsystem.theme.AbbaSpacing
import io.abbafather.core.designsystem.theme.AbbaTheme
import io.abbafather.domain.model.PrayerTag
import kotlinx.coroutines.flow.drop

/** Binds the ViewModel. Keeping is a write, so the way back waits for the event rather than the tap. */
@Composable
fun ComposePrayerRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ComposePrayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val openingDraft by viewModel.openingDraft.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ComposePrayerEvent.Kept -> onBack()
            }
        }
    }

    ComposePrayerScreen(
        uiState = uiState,
        openingDraft = openingDraft,
        onAction = { action ->
            viewModel.onAction(action)
            if (action is ComposePrayerAction.Back) onBack()
        },
        modifier = modifier,
    )
}

@Composable
fun ComposePrayerScreen(
    uiState: ComposePrayerUiState,
    openingDraft: ComposePrayerDraft?,
    onAction: (ComposePrayerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(AbbaSpacing.DocumentScreenPadding),
    ) {
        RoundIconButton(
            icon = AbbaIcons.BackChevron,
            contentDescription = stringResource(R.string.compose_back),
            onClick = { onAction(ComposePrayerAction.Back) },
            size = 44.dp,
            iconSize = 20.dp,
            containerColor = AbbaTheme.colors.card,
            pressedContainerColor = AbbaTheme.colors.cardPressed,
            contentColor = AbbaTheme.colors.ink,
        )

        // The page waits for the draft rather than opening empty and filling in underneath the
        // reader's hands — the fields take their starting text the first time they are composed.
        if (openingDraft != null) {
            Spacer(Modifier.height(26.dp))
            ComposeFields(openingDraft = openingDraft, onAction = onAction)

            Spacer(Modifier.height(AbbaSpacing.BlockGap))
            TagPicker(uiState = uiState, onAction = onAction)

            Spacer(Modifier.height(AbbaSpacing.WideBlockGap))
            PillButton(
                text = stringResource(R.string.compose_keep_prayer),
                onClick = { onAction(ComposePrayerAction.KeepPrayer) },
                modifier = Modifier.fillMaxWidth(),
                colors = if (uiState.canKeep) PillButtonDefaults.sage else PillButtonDefaults.card,
                height = 46.dp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.compose_kept_hint),
                style = AbbaTheme.type.hintSans,
                color = AbbaTheme.colors.inkSubtle,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 12.dp),
            )
        }
    }
}

/**
 * The two fields, borderless on the oat ground: there is no box around a prayer, only the page.
 *
 * Each field owns its own [TextFieldState] and tells the ViewModel afterwards, rather than being
 * driven keystroke by keystroke from it — a value round-tripping through `SavedStateHandle` arrives
 * a frame late, which drops letters and jumps the cursor. Nothing flows back into a field once it is
 * composed; what the ViewModel keeps is the draft, not the field. See `docs/DECISIONS.md`.
 */
@Composable
private fun ComposeFields(
    openingDraft: ComposePrayerDraft,
    onAction: (ComposePrayerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        DraftField(
            initialText = openingDraft.title,
            onTextChanged = { onAction(ComposePrayerAction.TitleChanged(it)) },
            placeholder = stringResource(R.string.compose_title_placeholder),
            textStyle = AbbaTheme.type.composeTitleField,
            lineLimits = TextFieldLineLimits.SingleLine,
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(22.dp))
        DraftField(
            initialText = openingDraft.body,
            onTextChanged = { onAction(ComposePrayerAction.BodyChanged(it)) },
            placeholder = stringResource(R.string.compose_body_placeholder),
            textStyle = AbbaTheme.type.composeBodyField,
            lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 6),
            imeAction = ImeAction.Default,
            modifier = Modifier.heightIn(min = BodyFieldMinHeight),
        )
    }
}

@Composable
private fun DraftField(
    initialText: String,
    onTextChanged: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    lineLimits: TextFieldLineLimits,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
) {
    val colors = AbbaTheme.colors
    val textFieldState = rememberTextFieldState(initialText)
    val currentOnTextChanged by rememberUpdatedState(onTextChanged)

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .drop(1)
            .collect { typed -> currentOnTextChanged(typed) }
    }

    BasicTextField(
        state = textFieldState,
        modifier = modifier.fillMaxWidth(),
        textStyle = textStyle.copy(color = colors.ink),
        lineLimits = lineLimits,
        cursorBrush = SolidColor(colors.sage),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = imeAction,
        ),
        decorator = { textField ->
            Box {
                if (textFieldState.text.isEmpty()) {
                    Text(text = placeholder, style = textStyle, color = colors.inkPlaceholder)
                }
                textField()
            }
        },
    )
}

/**
 * What the prayer is about. The picker shows only what is already ticked — nothing at all on a blank
 * page — until the reader asks for the rest, so the vocabulary never crowds the writing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagPicker(
    uiState: ComposePrayerUiState,
    onAction: (ComposePrayerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionLabel(text = stringResource(R.string.compose_tag_it))
        if (uiState.tagChips.isNotEmpty()) {
            Spacer(Modifier.height(AbbaSpacing.SectionLabelGap))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AbbaSpacing.ChipGap),
                verticalArrangement = Arrangement.spacedBy(AbbaSpacing.ChipGap),
            ) {
                uiState.tagChips.forEach { chip ->
                    SelectableChip(
                        label = chip.tag.displayName,
                        isSelected = chip.isSelected,
                        onToggle = { onAction(ComposePrayerAction.ToggleTag(chip.tag)) },
                    )
                }
            }
        }
        if (uiState.canShowMoreTags) {
            TextActionButton(
                text = stringResource(R.string.compose_more_tags),
                onClick = { onAction(ComposePrayerAction.ShowMoreTags) },
            )
        }
    }
}

private val BodyFieldMinHeight = 220.dp

@Preview
@Composable
private fun ComposePrayerScreenPreview() {
    AbbaTheme {
        ComposePrayerScreen(
            uiState = ComposePrayerUiState(
                tagChips = listOf(
                    ComposeTagChip(PrayerTag.Grace, isSelected = true),
                    ComposeTagChip(PrayerTag.Thanksgiving, isSelected = true),
                ),
                canShowMoreTags = true,
                canKeep = true,
                isLoaded = true,
            ),
            openingDraft = ComposePrayerDraft(
                title = "After Amazing Grace",
                body = "Father, you are endlessly generous.\n\nI have nothing to bring you " +
                    "this morning but the day itself.",
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun ComposePrayerScreenBlankPreview() {
    AbbaTheme {
        ComposePrayerScreen(
            uiState = ComposePrayerUiState(canShowMoreTags = true, isLoaded = true),
            openingDraft = ComposePrayerDraft(title = "", body = ""),
            onAction = {},
        )
    }
}
