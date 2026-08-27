package io.abbafather.feature.myprayers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.abbafather.R
import io.abbafather.core.designsystem.component.AbbaIcons
import io.abbafather.core.designsystem.component.RoundIconButton
import io.abbafather.core.designsystem.component.SoftCard
import io.abbafather.core.designsystem.component.TextActionButton
import io.abbafather.core.designsystem.theme.AbbaShapes
import io.abbafather.core.designsystem.theme.AbbaSpacing
import io.abbafather.core.designsystem.theme.AbbaTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Binds the ViewModel and turns the two moves that leave this screen into navigation. */
@Composable
fun MyPrayersRoute(
    onOpenPrayer: (String) -> Unit,
    onWriteNewPrayer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyPrayersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MyPrayersScreen(
        uiState = uiState,
        onAction = { action ->
            viewModel.onAction(action)
            when (action) {
                is MyPrayersAction.OpenPrayer -> onOpenPrayer(action.personalPrayerId)
                MyPrayersAction.WriteNewPrayer -> onWriteNewPrayer()
                else -> Unit
            }
        },
        modifier = modifier,
    )
}

@Composable
fun MyPrayersScreen(
    uiState: MyPrayersUiState,
    onAction: (MyPrayersAction) -> Unit,
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
            MyPrayersHeader(
                writtenPrayerCount = uiState.prayers.size,
                isLoaded = uiState.isLoaded,
                onWriteNewPrayer = { onAction(MyPrayersAction.WriteNewPrayer) },
            )
        }

        items(uiState.prayers, key = { it.personalPrayerId }) { prayer ->
            MyPrayerCard(prayer = prayer, onAction = onAction)
        }

        if (uiState.isEmpty) {
            item(key = "nothing-written") { NothingWrittenYet() }
        }
    }
}

/** The title and the count on the left, the blank page on the right. */
@Composable
private fun MyPrayersHeader(
    writtenPrayerCount: Int,
    isLoaded: Boolean,
    onWriteNewPrayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = AbbaSpacing.BlockGap - AbbaSpacing.WideCardGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.my_prayers_title),
                style = AbbaTheme.type.screenTitle,
                color = AbbaTheme.colors.ink,
            )
            // The count waits for the prayers rather than saying "none" while they are still coming.
            if (isLoaded && writtenPrayerCount > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.my_prayers_subtitle,
                        writtenPrayerCount,
                        writtenPrayerCount,
                    ),
                    style = AbbaTheme.type.bodySans,
                    color = AbbaTheme.colors.inkSubtle,
                )
            }
        }
        RoundIconButton(
            icon = AbbaIcons.Plus,
            contentDescription = stringResource(R.string.my_prayers_write_new),
            onClick = onWriteNewPrayer,
        )
    }
}

/**
 * A written prayer: what it is called, how it opens, and when it was last touched. The card itself
 * is the way back into it — the one text action on it is the one that cannot be undone.
 */
@Composable
private fun MyPrayerCard(
    prayer: MyPrayerCardUiState,
    onAction: (MyPrayersAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AbbaTheme.colors
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = AbbaShapes.PersonalPrayerCard,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
        onClick = { onAction(MyPrayersAction.OpenPrayer(prayer.personalPrayerId)) },
        onClickLabel = stringResource(R.string.my_prayers_open),
    ) {
        Text(
            text = if (prayer.hasTitle) {
                prayer.title
            } else {
                stringResource(R.string.my_prayers_untitled)
            },
            style = AbbaTheme.type.cardTitle,
            color = if (prayer.hasTitle) colors.ink else colors.inkPlaceholder,
        )

        if (prayer.excerpt.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = prayer.excerpt,
                style = AbbaTheme.type.prayerExcerpt,
                color = colors.inkProse,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.my_prayers_last_touched, rememberFormattedDate(prayer.lastTouchedAt)),
            style = AbbaTheme.type.metaSans,
            color = colors.inkMeta,
        )

        Spacer(Modifier.height(4.dp))
        if (prayer.isAwaitingDeleteConfirmation) {
            DeleteQuestion(prayer = prayer, onAction = onAction)
        } else {
            TextActionButton(
                text = stringResource(R.string.my_prayers_delete),
                onClick = { onAction(MyPrayersAction.AskToDelete(prayer.personalPrayerId)) },
                contentColor = colors.inkSubtle,
                pressedContentColor = colors.ink,
            )
        }
    }
}

/**
 * The question, asked on the card rather than in a dialog. This app holds prayers people wrote, so
 * nothing is deleted on one tap — and the answer that keeps it is the one in sage.
 */
@Composable
private fun DeleteQuestion(
    prayer: MyPrayerCardUiState,
    onAction: (MyPrayersAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.my_prayers_delete_question),
            style = AbbaTheme.type.bodySans,
            color = AbbaTheme.colors.ink,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            TextActionButton(
                text = stringResource(R.string.my_prayers_delete_cancel),
                onClick = { onAction(MyPrayersAction.CancelDelete) },
            )
            TextActionButton(
                text = stringResource(R.string.my_prayers_delete_confirm),
                onClick = { onAction(MyPrayersAction.ConfirmDelete(prayer.personalPrayerId)) },
                contentColor = AbbaTheme.colors.inkSubtle,
                pressedContentColor = AbbaTheme.colors.ink,
            )
        }
    }
}

@Composable
private fun rememberFormattedDate(epochMillis: Long): String = remember(epochMillis) {
    DateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}

@Composable
private fun NothingWrittenYet(modifier: Modifier = Modifier) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = AbbaShapes.PersonalPrayerCard,
        containerColor = AbbaTheme.colors.sageTint,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Text(
            text = stringResource(R.string.my_prayers_empty_title),
            style = AbbaTheme.type.suggestedExcerpt,
            color = AbbaTheme.colors.inkOnTint,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.my_prayers_empty_hint),
            style = AbbaTheme.type.bodySans,
            color = AbbaTheme.colors.inkOnTint.copy(alpha = 0.75f),
        )
    }
}

private val DateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@Preview
@Composable
private fun MyPrayersScreenPreview() {
    AbbaTheme {
        MyPrayersScreen(
            uiState = MyPrayersUiState(
                prayers = listOf(
                    MyPrayerCardUiState(
                        personalPrayerId = "1",
                        title = "For the morning I keep dreading",
                        excerpt = "Father, you are endlessly generous, and I have nothing to bring you.",
                        lastTouchedAt = 1_756_252_800_000,
                    ),
                    MyPrayerCardUiState(
                        personalPrayerId = "2",
                        title = "",
                        excerpt = "Thank you for the grace you keep showing me.",
                        lastTouchedAt = 1_756_000_000_000,
                        isAwaitingDeleteConfirmation = true,
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
private fun MyPrayersScreenEmptyPreview() {
    AbbaTheme {
        MyPrayersScreen(uiState = MyPrayersUiState(isLoaded = true), onAction = {})
    }
}
