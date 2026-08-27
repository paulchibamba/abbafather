package io.abbafather.feature.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.abbafather.R
import io.abbafather.core.designsystem.component.AbbaIcons
import io.abbafather.core.designsystem.component.BrandEyebrow
import io.abbafather.core.designsystem.component.ChoiceChip
import io.abbafather.core.designsystem.component.RoundIconButton
import io.abbafather.core.designsystem.component.SectionLabel
import io.abbafather.core.designsystem.component.SoftCard
import io.abbafather.core.designsystem.component.TextActionButton
import io.abbafather.core.designsystem.theme.AbbaShapes
import io.abbafather.core.designsystem.theme.AbbaSpacing
import io.abbafather.core.designsystem.theme.AbbaTheme
import io.abbafather.domain.model.SessionPacing

/** Binds the ViewModel; the only move that leaves this screen is going back. */
@Composable
fun AboutRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AboutScreen(
        uiState = uiState,
        onAction = { action ->
            viewModel.onAction(action)
            if (action is AboutAction.Back) onBack()
        },
        modifier = modifier,
    )
}

/**
 * The settings and the notices on one page, in that order: what the reader can change first, then
 * what the app owes the people whose work it carries.
 */
@Composable
fun AboutScreen(
    uiState: AboutUiState,
    onAction: (AboutAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(AbbaSpacing.DocumentScreenPadding),
    ) {
        RoundIconButton(
            icon = AbbaIcons.BackChevron,
            contentDescription = stringResource(R.string.about_back),
            onClick = { onAction(AboutAction.Back) },
            size = 44.dp,
            iconSize = 20.dp,
            containerColor = AbbaTheme.colors.card,
            pressedContainerColor = AbbaTheme.colors.cardPressed,
            contentColor = AbbaTheme.colors.ink,
        )

        Spacer(Modifier.height(26.dp))
        Text(
            text = stringResource(R.string.about_title),
            style = AbbaTheme.type.screenTitle,
            color = AbbaTheme.colors.ink,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.about_subtitle),
            style = AbbaTheme.type.bodySans,
            color = AbbaTheme.colors.inkSubtle,
        )

        Spacer(Modifier.height(AbbaSpacing.WideBlockGap))
        PacingBlock(
            chosenPacing = uiState.sessionPacing,
            choices = uiState.pacingChoices,
            onChoose = { pacing -> onAction(AboutAction.ChoosePacing(pacing)) },
        )

        Spacer(Modifier.height(AbbaSpacing.WideCardGap))
        KeepScreenOnRow(
            keepsScreenOn = uiState.keepsScreenOnDuringSession,
            onSet = { keepsScreenOn -> onAction(AboutAction.SetKeepsScreenOn(keepsScreenOn)) },
        )

        Spacer(Modifier.height(AbbaSpacing.WideBlockGap))
        ProseBlock(
            label = stringResource(R.string.about_prayers_label),
            body = stringResource(R.string.about_prayers_body),
            footnote = stringResource(R.string.about_prayers_copyright),
        )

        Spacer(Modifier.height(AbbaSpacing.BlockGap))
        ProseBlock(
            label = stringResource(R.string.about_scripture_label),
            body = stringResource(R.string.about_scripture_body),
        )

        Spacer(Modifier.height(AbbaSpacing.BlockGap))
        TypeBlock(
            licences = uiState.fontLicences,
            onToggleLicence = { fontName -> onAction(AboutAction.ToggleLicence(fontName)) },
        )

        Spacer(Modifier.height(AbbaSpacing.WideBlockGap))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            BrandEyebrow(color = AbbaTheme.colors.mutedSage)
            if (uiState.appVersionName.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.about_version, uiState.appVersionName),
                    style = AbbaTheme.type.metaSans,
                    color = AbbaTheme.colors.inkMeta,
                )
            }
        }
    }
}

/**
 * The three pacings, with the chosen one's own sentence under them. Only one sentence is shown,
 * because the reader is choosing between three behaviours rather than reading a table of them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PacingBlock(
    chosenPacing: SessionPacing,
    choices: List<SessionPacing>,
    onChoose: (SessionPacing) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionLabel(text = stringResource(R.string.about_pacing_label))
        Spacer(Modifier.height(AbbaSpacing.SectionLabelGap))
        Text(
            text = stringResource(R.string.about_pacing_hint),
            style = AbbaTheme.type.bodySans,
            color = AbbaTheme.colors.inkProse,
        )
        Spacer(Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AbbaSpacing.ChipGap),
            verticalArrangement = Arrangement.spacedBy(AbbaSpacing.ChipGap),
        ) {
            choices.forEach { pacing ->
                ChoiceChip(
                    // Named by the domain, as every other taxonomy in the app is.
                    label = pacing.displayName,
                    isSelected = pacing == chosenPacing,
                    onSelect = { onChoose(pacing) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(chosenPacing.hintResource),
            style = AbbaTheme.type.metaSans,
            color = AbbaTheme.colors.inkMeta,
        )
    }
}

/**
 * The whole card is the switch. The pill on the right says which way it is set — the design has no
 * switches, so "on" is a filled field like everywhere else.
 */
@Composable
private fun KeepScreenOnRow(
    keepsScreenOn: Boolean,
    onSet: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AbbaTheme.colors
    SoftCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Switch
                toggleableState = ToggleableState(keepsScreenOn)
            },
        shape = AbbaShapes.ListRow,
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 20.dp),
        onClick = { onSet(!keepsScreenOn) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.about_keep_screen_on_label),
                    style = AbbaTheme.type.collectionName,
                    color = colors.ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.about_keep_screen_on_hint),
                    style = AbbaTheme.type.metaSans,
                    color = colors.inkMeta,
                )
            }
            Spacer(Modifier.width(16.dp))
            OnOffPill(isOn = keepsScreenOn)
        }
    }
}

@Composable
private fun OnOffPill(isOn: Boolean, modifier: Modifier = Modifier) {
    val colors = AbbaTheme.colors
    Text(
        text = stringResource(if (isOn) R.string.about_on else R.string.about_off),
        style = AbbaTheme.type.chipLabel,
        color = if (isOn) colors.oat else colors.inkSubtle,
        modifier = modifier
            .clip(AbbaShapes.Pill)
            .background(if (isOn) colors.sage else colors.oat, AbbaShapes.Pill)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}

/** A tracked-out label over a paragraph, with an optional quieter one under it. */
@Composable
private fun ProseBlock(
    label: String,
    body: String,
    modifier: Modifier = Modifier,
    footnote: String? = null,
) {
    Column(modifier = modifier) {
        SectionLabel(text = label)
        Spacer(Modifier.height(AbbaSpacing.SectionLabelGap))
        Text(text = body, style = AbbaTheme.type.bodySans, color = AbbaTheme.colors.inkProse)
        if (footnote != null) {
            Spacer(Modifier.height(12.dp))
            Text(text = footnote, style = AbbaTheme.type.metaSans, color = AbbaTheme.colors.inkMeta)
        }
    }
}

/**
 * The two bundled faces, each with its notice folded away behind a line of text. An OFL notice has
 * to be carried whole, and it is carried whole — just not in the way of a page about quiet.
 */
@Composable
private fun TypeBlock(
    licences: List<FontLicenceUiState>,
    onToggleLicence: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionLabel(text = stringResource(R.string.about_type_label))
        Spacer(Modifier.height(AbbaSpacing.SectionLabelGap))
        Text(
            text = stringResource(R.string.about_type_body),
            style = AbbaTheme.type.bodySans,
            color = AbbaTheme.colors.inkProse,
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(AbbaSpacing.WideCardGap)) {
            licences.forEach { licence ->
                FontLicenceCard(
                    licence = licence,
                    onToggle = { onToggleLicence(licence.fontName) },
                )
            }
        }
    }
}

@Composable
private fun FontLicenceCard(
    licence: FontLicenceUiState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AbbaTheme.colors
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = AbbaShapes.ListRow,
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Text(text = licence.fontName, style = AbbaTheme.type.collectionName, color = colors.ink)
        // Closed, the card says whose the font is; open, the notice says it in its own first line.
        if (!licence.isOpen) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = licence.copyrightLine,
                style = AbbaTheme.type.metaSans,
                color = colors.inkMeta,
            )
        }
        TextActionButton(
            text = stringResource(
                if (licence.isOpen) R.string.about_close_licence else R.string.about_read_licence,
            ),
            onClick = onToggle,
        )
        if (licence.isOpen) {
            Text(
                text = licence.text,
                style = AbbaTheme.type.metaSans,
                color = colors.inkMeta,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val SessionPacing.hintResource: Int
    get() = when (this) {
        SessionPacing.Unhurried -> R.string.about_pacing_unhurried_hint
        SessionPacing.Steady -> R.string.about_pacing_steady_hint
        SessionPacing.Reader -> R.string.about_pacing_reader_hint
    }

@Preview
@Composable
private fun AboutScreenPreview() {
    AbbaTheme {
        AboutScreen(
            uiState = AboutUiState(
                sessionPacing = SessionPacing.Steady,
                keepsScreenOnDuringSession = true,
                fontLicences = listOf(
                    FontLicenceUiState(
                        fontName = "Cormorant Garamond",
                        copyrightLine = "Copyright 2015 the Cormorant Project Authors",
                        text = "SIL OPEN FONT LICENSE Version 1.1",
                    ),
                    FontLicenceUiState(
                        fontName = "Work Sans",
                        copyrightLine = "Copyright 2019 The Work Sans Project Authors",
                        text = "SIL OPEN FONT LICENSE Version 1.1",
                        isOpen = true,
                    ),
                ),
                appVersionName = "1.0",
                isLoaded = true,
            ),
            onAction = {},
        )
    }
}
