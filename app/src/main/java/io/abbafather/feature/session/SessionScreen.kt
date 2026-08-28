package io.abbafather.feature.session

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.abbafather.R
import io.abbafather.core.designsystem.component.AbbaIcons
import io.abbafather.core.designsystem.component.PillButton
import io.abbafather.core.designsystem.component.PillButtonDefaults
import io.abbafather.core.designsystem.component.RoundIconButton
import io.abbafather.core.designsystem.theme.AbbaShapes
import io.abbafather.core.designsystem.theme.AbbaSpacing
import io.abbafather.core.designsystem.theme.AbbaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.sin

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

    // A paced line moves on by itself; a reader-paced session has no timer at all. The key is the
    // line being prayed, so the rest starts again on each one rather than running through them.
    val dwellMillis = uiState.autoAdvanceAfterMillis
    if (dwellMillis != null) {
        LaunchedEffect(uiState.activeLineIndex, dwellMillis) {
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
                remainingFraction = uiState.remainingFraction,
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
 * The session owns the system bars while it is on screen, and hands them back as it leaves — the
 * ground is dark here and light everywhere else. The keep-screen-on flag is the reader's own.
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
    remainingFraction: Float,
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
        SessionProgress(remainingFraction = remainingFraction)
    }
}

/**
 * How much of the prayer is still ahead, as one line across the top that empties as it is prayed.
 *
 * It runs **down** rather than up because what a reader wants mid-prayer is how much is left, and it
 * empties **from the left**, so what is still ahead of them stays ahead of them on the page. The
 * remaining stretch carries a slow wave — the one thing in the header that is alive, at the same
 * unhurried pace as the glow behind the prayer.
 */
@Composable
private fun SessionProgress(
    remainingFraction: Float,
    modifier: Modifier = Modifier,
) {
    val colors = AbbaTheme.colors
    val remaining by animateFloatAsState(
        targetValue = remainingFraction.coerceIn(0f, 1f),
        animationSpec = tween(FadeMillis),
        label = "sessionProgress",
    )
    val wave = rememberInfiniteTransition(label = "wave")
    val phase by wave.animateFloat(
        initialValue = 0f,
        targetValue = TwoPi,
        animationSpec = infiniteRepeatable(
            animation = tween(WaveMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase",
    )

    Canvas(modifier = modifier.fillMaxWidth().height(WaveBandHeight)) {
        val middle = size.height / 2f
        val stroke = WaveStroke.toPx()
        val amplitude = WaveAmplitude.toPx()
        val wavelength = WaveLength.toPx()
        // What is already prayed is left as a flat line: the wave belongs to what is still to come.
        drawLine(
            color = colors.oatTick,
            start = Offset(0f, middle),
            end = Offset(size.width, middle),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        if (remaining <= 0f) return@Canvas

        val startX = size.width * (1f - remaining)
        val crest = { x: Float -> middle + amplitude * sin(phase + x / wavelength * TwoPi) }
        val path = Path().apply {
            moveTo(startX, crest(startX))
            var x = startX
            while (x < size.width) {
                x = (x + WaveStep).coerceAtMost(size.width)
                lineTo(x, crest(x))
            }
        }
        drawPath(
            path = path,
            color = colors.moss,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/**
 * The whole prayer in one column, with the step being prayed held on the centre of the screen.
 *
 * Nothing is hidden: the lines already prayed are above, the ones still to come are below, and how
 * far a step stands back from the one being prayed is what says where the reader is. The column
 * takes half a viewport of air at each end so the first and last steps can reach the centre like
 * every other one. See `docs/DECISIONS.md`.
 */
@Composable
private fun SessionStage(
    uiState: SessionUiState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // The session opens already centred rather than gliding up from nowhere; every move after that
    // is animated, because the movement is what tells the reader they have been carried on.
    var hasOpened by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val halfViewport = maxHeight / 2
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = halfViewport),
            verticalArrangement = Arrangement.spacedBy(LineGap),
        ) {
            itemsIndexed(
                items = uiState.lines,
                key = { index, _ -> index },
            ) { index, line ->
                PrayedLine(
                    text = line,
                    distanceFromActive = index - uiState.activeLineIndex,
                )
            }
        }

        // The column runs the height of the stage, so it would otherwise slide under the ticks and
        // out from behind the button with a hard cut. It goes into the ground at both ends instead.
        val forest = AbbaTheme.colors.deepForest
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(EdgeFade)
                .background(Brush.verticalGradient(listOf(forest, Color.Transparent))),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(EdgeFade)
                .background(Brush.verticalGradient(listOf(Color.Transparent, forest))),
        )
    }

    LaunchedEffect(uiState.activeLineIndex, uiState.lines.size) {
        if (uiState.lines.isEmpty()) return@LaunchedEffect
        // Nothing has a height before the first pass, and an unmeasured column cannot be centred.
        snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > 0 }
        listState.centreOn(uiState.activeLineIndex, isAnimated = hasOpened)
        hasOpened = true
    }
}

/** Puts the line's middle on the middle of the screen. */
private suspend fun LazyListState.centreOn(index: Int, isAnimated: Boolean) {
    if (offsetFromCentreOf(index) == null) {
        // Too far away to have been measured — land on it first, and correct once it has a height.
        if (isAnimated) animateScrollToItem(index) else scrollToItem(index)
    }
    val distance = offsetFromCentreOf(index)?.toFloat() ?: return
    if (isAnimated) animateScrollBy(distance) else scrollBy(distance)
}

/** How far a line's middle stands from the middle of the screen, or null if it is not measured. */
private fun LazyListState.offsetFromCentreOf(index: Int): Int? {
    val line = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return null
    val viewportCentre = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    return line.offset + line.size / 2 - viewportCentre
}

@Composable
private fun PrayedLine(
    text: String,
    distanceFromActive: Int,
    modifier: Modifier = Modifier,
) {
    val isBeingPrayed = distanceFromActive == 0
    val beingPrayedDescription = stringResource(R.string.session_being_prayed)
    val lineColor by animateColorAsState(
        targetValue = AbbaTheme.colors.oat.copy(alpha = alphaAtDistance(distanceFromActive)),
        animationSpec = tween(FadeMillis),
        label = "sessionLineColor",
    )
    Text(
        text = text,
        style = AbbaTheme.type.sessionLine,
        color = lineColor,
        // Standing forward is the only thing that says "this is the line"; a reader who is being
        // read the prayer is told in words.
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (isBeingPrayed) stateDescription = beingPrayedDescription },
    )
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

/**
 * How far a line stands back from the one being prayed. Behind and ahead read the same: the prayer
 * is one continuous thing, and dimming what is coming harder than what is past would say "do not
 * look ahead" while showing it anyway.
 *
 * Every rung is the value of a token the design already uses — full oat, `oatSpent`, `oatTick`,
 * `oatVeil` — read here as numbers because this is a continuous ladder rather than four roles.
 */
private fun alphaAtDistance(distance: Int): Float = when (abs(distance)) {
    0 -> 1.00f
    1 -> 0.38f
    2 -> 0.22f
    else -> 0.12f
}

/**
 * The design's `fadeup`, kept as a fade alone: the rise is the scroll now. Shorter than the second
 * the design gives, because nothing is being replaced — a line changes weight while the column
 * moves under it, and a fade still running after the scroll has stopped reads as lag.
 */
private const val FadeMillis = 700

private val LineGap = 18.dp

/** The progress wave: slow enough to be alive without being something to watch. */
private const val WaveMillis = 4_000
private const val TwoPi = (2 * Math.PI).toFloat()
private val WaveBandHeight = 10.dp
private val WaveStroke = 3.dp
private val WaveAmplitude = 2.5.dp
private val WaveLength = 16.dp

/** Pixels between points on the drawn wave — small enough that the line reads as a curve. */
private const val WaveStep = 2f

/** How far the column fades into the ground at each end of the stage. */
private val EdgeFade = 52.dp

/** The design's `glow`: .5/1.0 to .85/1.06 over nine seconds, breathing both ways. */
private const val GlowMillis = 9_000
private val GlowDiameter = 460.dp
private val GlowRise = (-60).dp

private val previewLines = listOf(
    "Father, you are endlessly generous.",
    "Thank you for the grace you keep showing me, even though I have never deserved it.",
    "No one needs your grace more than I do.",
    "Hold me fast, and bring me home.",
)

@Preview
@Composable
private fun SessionScreenPreview() {
    AbbaTheme {
        SessionScreen(
            uiState = SessionUiState(
                title = "Amazing Grace",
                attribution = "The Valley of Vision, adapted",
                lines = previewLines,
                activeLineIndex = 1,
                remainingFraction = 0.66f,
                canGoBack = true,
                isLoaded = true,
            ),
            onAction = {},
        )
    }
}

/** The end of the prayer, where the only move left is "Amen". */
@Preview
@Composable
private fun SessionEndPreview() {
    AbbaTheme {
        SessionScreen(
            uiState = SessionUiState(
                title = "Amazing Grace",
                attribution = "The Valley of Vision, adapted",
                lines = previewLines,
                activeLineIndex = previewLines.lastIndex,
                remainingFraction = 0f,
                canGoBack = true,
                isAtEnd = true,
                isLoaded = true,
            ),
            onAction = {},
        )
    }
}
