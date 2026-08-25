package io.abbafather.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.abbafather.R
import io.abbafather.core.designsystem.component.AbbaIcons
import io.abbafather.core.designsystem.component.BrandEyebrow
import io.abbafather.core.designsystem.component.PillButton
import io.abbafather.core.designsystem.component.SectionLabel
import io.abbafather.core.designsystem.component.SoftCard
import io.abbafather.core.designsystem.component.TextActionButton
import io.abbafather.core.designsystem.theme.AbbaShapes
import io.abbafather.core.designsystem.theme.AbbaSpacing
import io.abbafather.core.designsystem.theme.AbbaTheme
import io.abbafather.domain.model.DailyVerse
import io.abbafather.domain.model.Greeting
import io.abbafather.domain.model.Prayer
import io.abbafather.domain.model.PrayerGroup
import io.abbafather.domain.model.PrayerKind

/**
 * The binding wrapper: it holds the ViewModel and turns an action into both the record the ViewModel
 * keeps and the move the navigator makes. [HomeScreen] itself stays stateless and previewable.
 */
@Composable
fun HomeRoute(
    onOpenReader: (String) -> Unit,
    onBeginSession: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onAction = { action ->
            viewModel.onAction(action)
            when (action) {
                is HomeAction.ReadPrayer -> onOpenReader(action.prayerId)
                is HomeAction.BeginSession -> onBeginSession(action.prayerId)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // The header's tint runs to the top of the display, so only the sides and the floor are
            // inset here; the header takes the status bar inside its own field.
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .verticalScroll(rememberScrollState()),
    ) {
        HomeHeader(greeting = uiState.greeting, verse = uiState.verse)

        Column(
            modifier = Modifier.padding(HomeBodyPadding),
            verticalArrangement = Arrangement.spacedBy(AbbaSpacing.WideBlockGap),
        ) {
            if (uiState.suggestedPrayer != null) {
                SuggestedPrayerBlock(prayer = uiState.suggestedPrayer, onAction = onAction)
            }
            if (uiState.recentlyPrayed.isNotEmpty()) {
                RecentlyPrayedBlock(prayers = uiState.recentlyPrayed, onAction = onAction)
            }
        }
    }
}

/**
 * The one warm colour field the screen is built around: a sage-tinted panel rounded off at the
 * bottom, running edge to edge and taking the status bar inset inside itself so the tint reaches the
 * top of the display.
 */
@Composable
private fun HomeHeader(
    greeting: Greeting,
    verse: DailyVerse?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AbbaShapes.HomeHeader)
            .background(AbbaTheme.colors.sageTint)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(AbbaSpacing.HomeHeaderPadding),
    ) {
        BrandEyebrow(color = AbbaTheme.colors.mutedSage)
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(greeting.labelResource),
            style = AbbaTheme.type.homeGreeting,
            color = AbbaTheme.colors.inkOnTint,
        )
        if (verse != null) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = verse.text,
                style = AbbaTheme.type.homeVerse,
                color = AbbaTheme.colors.inkOnTint.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = verse.reference,
                style = AbbaTheme.type.metaSans,
                color = AbbaTheme.colors.mutedSage,
            )
        }
    }
}

@Composable
private fun SuggestedPrayerBlock(
    prayer: Prayer,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionLabel(text = stringResource(R.string.home_todays_prayer))
        Spacer(Modifier.height(AbbaSpacing.SectionLabelGap))
        SoftCard(
            modifier = Modifier.fillMaxWidth(),
            shape = AbbaShapes.SavedCard,
            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 26.dp),
        ) {
            Text(
                text = prayer.title,
                style = AbbaTheme.type.suggestedCardTitle,
                color = AbbaTheme.colors.ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = prayer.attribution,
                style = AbbaTheme.type.metaSans,
                color = AbbaTheme.colors.inkMeta,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = prayer.openingLine,
                style = AbbaTheme.type.suggestedExcerpt,
                color = AbbaTheme.colors.inkProse,
            )
            Spacer(Modifier.height(24.dp))
            PillButton(
                text = stringResource(R.string.home_begin_prayer),
                onClick = { onAction(HomeAction.BeginSession(prayer.id)) },
                modifier = Modifier.fillMaxWidth(),
                height = 64.dp,
                textStyle = AbbaTheme.type.primaryButtonLabel,
            )
            TextActionButton(
                text = stringResource(R.string.home_read_it),
                onClick = { onAction(HomeAction.ReadPrayer(prayer.id)) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun RecentlyPrayedBlock(
    prayers: List<Prayer>,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionLabel(text = stringResource(R.string.home_recently_prayed))
        Spacer(Modifier.height(AbbaSpacing.SectionLabelGap))
        Column(verticalArrangement = Arrangement.spacedBy(AbbaSpacing.CardGap)) {
            prayers.forEach { prayer ->
                RecentPrayerRow(
                    prayer = prayer,
                    onClick = { onAction(HomeAction.ReadPrayer(prayer.id)) },
                )
            }
        }
    }
}

@Composable
private fun RecentPrayerRow(
    prayer: Prayer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = AbbaShapes.ListRow,
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prayer.title,
                    style = AbbaTheme.type.recentRowTitle,
                    color = AbbaTheme.colors.ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = prayer.attribution,
                    style = AbbaTheme.type.metaSans,
                    color = AbbaTheme.colors.inkMeta,
                )
            }
            Icon(
                imageVector = AbbaIcons.ArrowRight,
                contentDescription = stringResource(R.string.home_open_prayer),
                tint = AbbaTheme.colors.mutedSage,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private val Greeting.labelResource: Int
    get() = when (this) {
        Greeting.Morning -> R.string.home_greeting_morning
        Greeting.Afternoon -> R.string.home_greeting_afternoon
        Greeting.Evening -> R.string.home_greeting_evening
        Greeting.Night -> R.string.home_greeting_night
    }

/** The header supplies the top padding itself; the body keeps the list screen's sides and floor. */
private val HomeBodyPadding =
    PaddingValues(start = 24.dp, top = 34.dp, end = 24.dp, bottom = 30.dp)

@Preview
@Composable
private fun HomeScreenPreview() {
    AbbaTheme {
        HomeScreen(
            uiState = HomeUiState(
                greeting = Greeting.Evening,
                verse = DailyVerse("Be still, and know that I am God.", "Psalm 46:10"),
                suggestedPrayer = previewPrayer("A Collect for Peace", "Book of Common Prayer"),
                recentlyPrayed = listOf(
                    previewPrayer("Psalm 23", "Psalms"),
                    previewPrayer("A Prayer in Distress", "After John Bunyan"),
                ),
                isCatalogueReady = true,
            ),
            onAction = {},
        )
    }
}

private fun previewPrayer(title: String, author: String) = Prayer(
    id = title,
    title = title,
    author = author,
    kind = PrayerKind.Evening,
    group = PrayerGroup.BookOfCommonPrayer,
    themes = emptySet(),
    lines = listOf("O God, from whom all holy desires, all good counsels, and all just works do proceed."),
)
