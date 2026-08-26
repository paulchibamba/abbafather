package io.abbafather.feature.session

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.abbafather.R
import io.abbafather.core.designsystem.component.AbbaIcons
import io.abbafather.core.designsystem.component.PillButton
import io.abbafather.core.designsystem.component.PillButtonDefaults
import io.abbafather.core.designsystem.component.RoundIconButton
import io.abbafather.core.designsystem.component.SectionLabel
import io.abbafather.core.designsystem.theme.AbbaShapes
import io.abbafather.core.designsystem.theme.AbbaSpacing
import io.abbafather.core.designsystem.theme.AbbaTheme
import kotlinx.coroutines.delay

/**
 * Binds the ViewModel and performs the two moves that leave the session. Both pop back to whatever
 * opened it — "Amen" and leaving early go to the same place, because the difference between them
 * belongs to the reader rather than to the navigator.
 */
@Composable
fun SessionRoute(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SessionScreen(
        uiState = uiState,
        onAction = { action ->
            viewModel.onAction(action)
            when (action) {
                SessionAction.Leave, SessionAction.Amen -> onFinish()
                else -> Unit
            }
        },
        modifier = modifier,
    )
}

@Composable
fun SessionScreen(
    uiState: SessionUiState,
    onAction: (SessionAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    SessionSystemBars(keepsScreenOn = uiState.keepsScreenOn && uiState.isLoaded)

    // A step that rests moves on by itself; a reader-paced session has no timer at all. The key
    // is what is on screen, so the rest starts again from each new line rather than run through it.
    val dwellMillis = uiState.autoAdvanceAfterMillis
    if (dwellMillis != null) {
        LaunchedEffect(uiState.movementLines, uiState.breathingPause) {
            delay(dwellMillis)
            onAction(SessionAction.Advance)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AbbaTheme.colors.deepForest),
    ) {
        BreathingGlow(modifier = Modifier.align(Alignment.Center))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(AbbaSpacing.SessionPadding),
        ) {
            SessionHeader(
                title = uiState.title,
                attribution = uiState.attribution,
                movementTicks = uiState.movementTicks,
                onLeave = { onAction(SessionAction.Leave) },
            )

            SessionStage(
                uiState = uiState,
                modifier = Modifier.weight(1f),
            )

            SessionControls(uiState = uiState, onAction = onAction)
        }
    }
}

/**
 * The session owns the phone while it is open: the screen stays awake if the reader asked it to,
 * and the bars are told they are sitting on a dark ground. Both are put back exactly as they were,
 * so leaving the session leaves nothing of it behind.
 */
@Composable
private fun SessionSystemBars(keepsScreenOn: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, keepsScreenOn) {
        val window = (view.context as Activity).window
        val bars = WindowCompat.getInsetsController(window, view)
        val hadLightStatusBars = bars.isAppearanceLightStatusBars
        val hadLightNavigationBars = bars.isAppearanceLightNavigationBars
        val wasKeepingScreenOn = view.keepScreenOn

        bars.isAppearanceLightStatusBars = false
        bars.isAppearanceLightNavigationBars = false
        view.keepScreenOn = keepsScreenOn

        onDispose {
            bars.isAppearanceLightStatusBars = hadLightStatusBars
            bars.isAppearanceLightNavigationBars = hadLightNavigationBars
            view.keepScreenOn = wasKeepingScreenOn
        }
    }
}

@Composable
private fun SessionHeader(
    title: String,
    attribution: String,
    movementTicks: List<MovementTick>,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AbbaTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        RoundIconButton(
            icon = AbbaIcons.BackChevron,
            contentDescription = stringResource(R.string.session_leave),
            onClick = onLeave,
            size = 44.dp,
            iconSize = 20.dp,
            containerColor = colors.oatVeil,
            pressedContainerColor = colors.oatVeilPressed,
            contentColor = colors.oatOnForest,
        )
        Spacer(Modifier.height(22.dp))
        Text(
            text = title,
            style = AbbaTheme.type.metaSans,
            color = colors.oatAmbientLabel,
        )
        Text(
            text = attribution,
            style = AbbaTheme.type.metaSans,
            color = colors.oatByline,
        )
        Spacer(Modifier.height(18.dp))
        MovementTicks(ticks = movementTicks)
    }
}

/** How far through the prayer this is, counted in movements. Twenty-nine ticks would be noise. */
@Composable
private fun MovementTicks(
    ticks: List<MovementTick>,
    modifier: Modifier = Modifier,
) {
    val colors = AbbaTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ticks.forEach { tick ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .background(
                        // The movement being prayed is the only one at full strength; the ones
                        // behind it are done with and quiet down. The strength is in the colour
                        // rather than in an alpha layer, which a 3dp bar loses altogether.
                        color = when (tick) {
                            MovementTick.Current -> colors.moss
                            MovementTick.Spent -> colors.moss.copy(alpha = SpentTickAlpha)
                            MovementTick.ToCome -> colors.oatTick
                        },
                        shape = AbbaShapes.Pill,
                    ),
            )
        }
    }
}

/**
 * Either the movement being prayed, or the rest between two of them — never both.
 *
 * A movement can be longer than a phone, so the stage scrolls and follows the line being prayed.
 * It is centred while it fits, which is nearly always, and only moves when it does not.
 */
@Composable
private fun SessionStage(
    uiState: SessionUiState,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(uiState.movementLines, uiState.breathingPause) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = uiState.breathingPause ?: uiState.movementLines,
            transitionSpec = {
                val rise = slideInVertically(tween(FadeUpMillis)) { FadeUpRise }
                (fadeIn(tween(FadeUpMillis)) + rise)
                    .togetherWith(fadeOut(tween(FadeUpMillis / 3)))
            },
            modifier = Modifier.verticalScroll(scrollState),
            label = "sessionStage",
        ) { stage ->
            when (stage) {
                is BreathingPauseUiState -> BreathingPause(pause = stage)
                else -> PrayedLines(lines = uiState.movementLines)
            }
        }
    }
}

@Composable
private fun PrayedLines(
    lines: List<SessionLine>,
    modifier: Modifier = Modifier,
) {
    val colors = AbbaTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        lines.forEach { line ->
            Text(
                text = line.text,
                style = AbbaTheme.type.sessionLine,
                color = if (line.isCurrent) colors.oatOnForest else colors.oatSpent,
            )
        }
    }
}

/**
 * A movement is a complete turn of the praying, so the session rests before the next one and names
 * what it is about to ask.
 */
@Composable
private fun BreathingPause(
    pause: BreathingPauseUiState,
    modifier: Modifier = Modifier,
) {
    val colors = AbbaTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(text = stringResource(R.string.session_breathe), color = colors.moss)
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(2.dp)
                .clip(AbbaShapes.Pill)
                .background(colors.moss),
        )
        Spacer(Modifier.height(26.dp))
        Text(
            text = pause.nextMovementHeading,
            style = AbbaTheme.type.sessionLine,
            color = colors.oatOnForest,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(
                R.string.session_movement_position,
                pause.nextMovementNumber,
                pause.movementCount,
            ),
            style = AbbaTheme.type.metaSans,
            color = colors.oatHint,
        )
    }
}

/** Translucent while there is praying left, and oat at the end, where the only move is "Amen". */
@Composable
private fun SessionControls(
    uiState: SessionUiState,
    onAction: (SessionAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AbbaTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AbbaSpacing.WideCardGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (uiState.canGoBack) {
            RoundIconButton(
                icon = AbbaIcons.BackChevron,
                contentDescription = stringResource(R.string.session_previous),
                onClick = { onAction(SessionAction.GoBack) },
                size = 56.dp,
                containerColor = colors.oatVeil,
                pressedContainerColor = colors.oatVeilPressed,
                contentColor = colors.oatOnForest,
            )
        }
        if (uiState.isAtEnd) {
            PillButton(
                text = stringResource(R.string.session_amen),
                onClick = { onAction(SessionAction.Amen) },
                modifier = Modifier.weight(1f),
                colors = PillButtonDefaults.oatOnForest,
                height = 64.dp,
                textStyle = AbbaTheme.type.amenButtonLabel,
            )
        } else {
            PillButton(
                text = stringResource(R.string.session_continue),
                onClick = { onAction(SessionAction.Advance) },
                modifier = Modifier.weight(1f),
                colors = PillButtonDefaults.translucentOnForest,
                height = 64.dp,
                textStyle = AbbaTheme.type.primaryButtonLabel,
            )
        }
    }
}

/**
 * The one thing on this screen that moves on its own: a blurred sage orb breathing behind the
 * prayer, on the design's nine-second `glow` timing.
 */
@Composable
private fun BreathingGlow(modifier: Modifier = Modifier) {
    val breath = rememberInfiniteTransition(label = "glow")
    val animationSpec = infiniteRepeatable<Float>(
        animation = tween(GlowMillis, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse,
    )
    val glowAlpha by breath.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.85f,
        animationSpec = animationSpec,
        label = "glowAlpha",
    )
    val glowScale by breath.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = animationSpec,
        label = "glowScale",
    )
    // Painted as a radial fade rather than a blurred circle: a blur is clipped to its own layer, and
    // the orb showed its square. A gradient has no edge to clip.
    val sage = AbbaTheme.colors.sage
    Box(
        modifier = modifier
            .size(GlowDiameter)
            .offset(y = GlowRise)
            .scale(glowScale)
            .background(
                Brush.radialGradient(
                    colors = listOf(sage.copy(alpha = glowAlpha), Color.Transparent),
                ),
            ),
    )
}

/** The design's `fadeup`: a 14px rise and a fade, about a second. */
private const val FadeUpMillis = 1_000
private const val FadeUpRise = 14

/** The design's `glow`: .5/1.0 to .85/1.06 over nine seconds, breathing both ways. */
private const val GlowMillis = 9_000
private val GlowDiameter = 460.dp
private val GlowRise = (-60).dp

/** Movements already prayed keep their moss, but stand back from the one being prayed now. */
private const val SpentTickAlpha = 0.55f

@Preview
@Composable
private fun SessionScreenPreview() {
    AbbaTheme {
        SessionScreen(
            uiState = SessionUiState(
                title = "Amazing Grace",
                attribution = "The Valley of Vision, adapted",
                movementLines = listOf(
                    SessionLine("Father, you are endlessly generous.", isCurrent = false),
                    SessionLine(
                        "Thank you for the grace you keep showing me, even though I have never " +
                            "deserved it.",
                        isCurrent = true,
                    ),
                ),
                movementTicks = listOf(
                    MovementTick.Spent,
                    MovementTick.Current,
                    MovementTick.ToCome,
                    MovementTick.ToCome,
                ),
                canGoBack = true,
                isLoaded = true,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SessionPausePreview() {
    AbbaTheme {
        SessionScreen(
            uiState = SessionUiState(
                title = "Amazing Grace",
                attribution = "The Valley of Vision, adapted",
                breathingPause = BreathingPauseUiState(
                    nextMovementHeading = "Admitting how deeply I need you",
                    nextMovementNumber = 3,
                    movementCount = 4,
                ),
                movementTicks = listOf(
                    MovementTick.Spent,
                    MovementTick.Spent,
                    MovementTick.Current,
                    MovementTick.ToCome,
                ),
                canGoBack = true,
                isLoaded = true,
            ),
            onAction = {},
        )
    }
}
