package io.abbafather.feature.saved

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.abbafather.R
import io.abbafather.core.designsystem.component.SoftCard
import io.abbafather.core.designsystem.component.TagChip
import io.abbafather.core.designsystem.component.TextActionButton
import io.abbafather.core.designsystem.theme.AbbaShapes
import io.abbafather.core.designsystem.theme.AbbaSpacing
import io.abbafather.core.designsystem.theme.AbbaTheme
import io.abbafather.domain.model.PrayerTag
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Binds the ViewModel and performs the two moves that leave this screen. Growing a line into a
 * prayer arrives as an event, because the draft has to exist before there is an id to navigate to.
 */
@Composable
fun SavedRoute(
    onOpenReader: (String) -> Unit,
    onOpenComposedPrayer: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SavedEvent.OpenComposedPrayer -> onOpenComposedPrayer(event.personalPrayerId)
            }
        }
    }

    SavedScreen(
        uiState = uiState,
        onAction = { action ->
            viewModel.onAction(action)
            if (action is SavedAction.OpenSourcePrayer) onOpenReader(action.prayerId)
        },
        modifier = modifier,
    )
}

@Composable
fun SavedScreen(
    uiState: SavedUiState,
    onAction: (SavedAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        // Remembered rather than hoisted, so leaving the tab and coming back lands on the same place.
        state = rememberLazyListState(),
        contentPadding = AbbaSpacing.ListScreenPadding,
        verticalArrangement = Arrangement.spacedBy(AbbaSpacing.WideCardGap),
    ) {
        item(key = "header") {
            SavedHeader(keptLineCount = uiState.savedLines.size, isLoaded = uiState.isLoaded)
        }

        items(uiState.savedLines, key = { it.savedLineId }) { savedLine ->
            SavedLineCard(savedLine = savedLine, onAction = onAction)
        }

        if (uiState.isEmpty) {
            item(key = "nothing-kept") { NothingKeptYet() }
        }
    }
}

@Composable
private fun SavedHeader(
    keptLineCount: Int,
    isLoaded: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = AbbaSpacing.BlockGap - AbbaSpacing.WideCardGap)) {
        Text(
            text = stringResource(R.string.saved_title),
            style = AbbaTheme.type.screenTitle,
            color = AbbaTheme.colors.ink,
        )
        // The count waits for the lines rather than saying "no lines" while they are still coming.
        if (isLoaded && keptLineCount > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = pluralStringResource(R.plurals.saved_subtitle, keptLineCount, keptLineCount),
                style = AbbaTheme.type.bodySans,
                color = AbbaTheme.colors.inkSubtle,
            )
        }
    }
}

/**
 * A kept line, whole: the line itself, what it was tagged with, where it came from and when it was
 * kept, then the three things that can be done with it. Nothing is hidden behind a tap — the shelf
 * is short, and a line the reader chose deserves its actions in the open.
 */
@Composable
private fun SavedLineCard(
    savedLine: SavedLineCardUiState,
    onAction: (SavedAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AbbaTheme.colors
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = AbbaShapes.SavedCard,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text(
            text = savedLine.line,
            style = AbbaTheme.type.savedLine,
            color = colors.ink,
        )

        if (savedLine.tags.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            TagChipRow(tags = savedLine.tags)
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = savedLine.provenanceLine(),
            style = AbbaTheme.type.metaSans,
            color = colors.inkMeta,
        )

        Spacer(Modifier.height(6.dp))
        SavedLineActions(savedLine = savedLine, onAction = onAction)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagChipRow(tags: List<PrayerTag>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AbbaSpacing.ChipGap),
        verticalArrangement = Arrangement.spacedBy(AbbaSpacing.ChipGap),
    ) {
        tags.forEach { tag -> TagChip(label = tag.displayName) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavedLineActions(
    savedLine: SavedLineCardUiState,
    onAction: (SavedAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (savedLine.canOpenSourcePrayer) {
            TextActionButton(
                text = stringResource(R.string.saved_read_the_prayer),
                onClick = {
                    onAction(SavedAction.OpenSourcePrayer(checkNotNull(savedLine.sourcePrayerId)))
                },
            )
        }
        TextActionButton(
            text = stringResource(R.string.saved_make_it_mine),
            onClick = { onAction(SavedAction.GrowIntoPrayer(savedLine.savedLineId)) },
        )
        // Quieter than the other two: letting go is not something the card should invite.
        TextActionButton(
            text = stringResource(R.string.saved_release),
            onClick = { onAction(SavedAction.ReleaseLine(savedLine.savedLineId)) },
            contentColor = AbbaTheme.colors.inkSubtle,
            pressedContentColor = AbbaTheme.colors.ink,
        )
    }
}

/** Where the line came from and when it was kept, as one meta line. */
@Composable
private fun SavedLineCardUiState.provenanceLine(): String {
    val keptOn = rememberFormattedDate(keptAt)
    val source = when {
        sourcePrayerTitle == null -> null
        sourceAttribution.isNullOrBlank() -> sourcePrayerTitle
        else -> stringResource(R.string.saved_source_prayer, sourcePrayerTitle, sourceAttribution)
    }
    return when (source) {
        null -> stringResource(R.string.saved_kept_on, keptOn)
        else -> stringResource(R.string.saved_source_and_date, source, keptOn)
    }
}

@Composable
private fun rememberFormattedDate(epochMillis: Long): String = remember(epochMillis) {
    DateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}

@Composable
private fun NothingKeptYet(modifier: Modifier = Modifier) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = AbbaShapes.SavedCard,
        containerColor = AbbaTheme.colors.sageTint,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Text(
            text = stringResource(R.string.saved_empty_title),
            style = AbbaTheme.type.suggestedExcerpt,
            color = AbbaTheme.colors.inkOnTint,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.saved_empty_hint),
            style = AbbaTheme.type.bodySans,
            color = AbbaTheme.colors.inkOnTint.copy(alpha = 0.75f),
        )
    }
}

private val DateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@Preview
@Composable
private fun SavedScreenPreview() {
    AbbaTheme {
        SavedScreen(
            uiState = SavedUiState(
                savedLines = listOf(
                    SavedLineCardUiState(
                        savedLineId = "1",
                        line = "Compassionate Lord, I woke up today because your mercy carried me here.",
                        sourcePrayerId = "morning",
                        sourcePrayerTitle = "Morning",
                        sourceAttribution = "The Valley of Vision, adapted",
                        tags = listOf(PrayerTag.Grace, PrayerTag.Assurance),
                        keptAt = 1_724_760_000_000,
                    ),
                    SavedLineCardUiState(
                        savedLineId = "2",
                        line = "Let me learn by paradox that the way down is the way up.",
                        sourcePrayerId = null,
                        sourcePrayerTitle = null,
                        sourceAttribution = null,
                        keptAt = 1_724_500_000_000,
                    ),
                ),
                isLoaded = true,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SavedScreenEmptyPreview() {
    AbbaTheme {
        SavedScreen(uiState = SavedUiState(isLoaded = true), onAction = {})
    }
}
