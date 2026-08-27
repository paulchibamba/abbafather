package io.abbafather.feature.session

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.abbafather.domain.model.PrayerSettings
import io.abbafather.domain.model.SessionPacing
import io.abbafather.testing.FakePrayerRepository
import io.abbafather.testing.FakeSettingsRepository
import io.abbafather.testing.MainDispatcherRule
import io.abbafather.testing.testPrayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /**
     * Three movements of two, one and two lines: enough for two breathing pauses, a movement the
     * session passes right through, and a last line that has to be the end rather than a pause.
     */
    private val prayer = testPrayer(
        id = "vov-062-amazing-grace",
        title = "Amazing Grace",
        movementLines = listOf(
            listOf(
                "Father, you are endlessly generous.",
                "Thank you for the grace you keep showing.",
            ),
            listOf("No one needs your grace more than I do."),
            listOf("Hold me fast.", "Bring me home."),
        ),
        headings = listOf(
            "Thanking you for grace I did not earn",
            "Admitting how deeply I need you",
            "Asking you to keep me",
        ),
    )

    private val prayerRepository = FakePrayerRepository(listOf(prayer))

    private fun viewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(mapOf("prayerId" to prayer.id)),
        settingsRepository: FakeSettingsRepository = FakeSettingsRepository(),
    ) = SessionViewModel(
        savedStateHandle = savedStateHandle,
        prayerRepository = prayerRepository,
        settingsRepository = settingsRepository,
    )

    private val SessionViewModel.loadedStates: Flow<SessionUiState>
        get() = uiState.filter { it.isLoaded }

    /**
     * A move writes to the handle and the state follows on the test's own dispatcher, so the
     * scheduler is run out before the state is read.
     */
    private fun SessionViewModel.settled(): SessionUiState {
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        return uiState.value
    }

    /** Walks the session forward, returning the state it comes to rest in. */
    private fun SessionViewModel.advance(times: Int): SessionUiState {
        repeat(times) { onAction(SessionAction.Advance) }
        return settled()
    }

    private val SessionUiState.activeStep: SessionStepUiState get() = steps[activeStepIndex]

    private val SessionUiState.activeLineText: String?
        get() = (activeStep as? SessionStepUiState.Line)?.text

    private val SessionUiState.lineTexts: List<String>
        get() = steps.filterIsInstance<SessionStepUiState.Line>().map { it.text }

    @Test
    fun `the session opens on the first line, with the whole prayer under it`() = runTest {
        viewModel().loadedStates.test {
            val uiState = awaitItem()

            assertEquals(prayer.title, uiState.title)
            assertEquals(prayer.attribution, uiState.attribution)
            // Five lines and the two rests between the three movements.
            assertEquals(7, uiState.steps.size)
            assertEquals(0, uiState.activeStepIndex)
            assertEquals(prayer.lines[0], uiState.activeLineText)
            assertFalse(uiState.canGoBack)
            assertFalse(uiState.isAtEnd)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * The column is the prayer, and the prayer does not change while it is being prayed. Put
     * activeness on the step instead and every advance builds a different list — which is what this
     * asserts against, because everything the screen keys on would then thrash.
     */
    @Test
    fun `advancing moves the active step and leaves the column alone`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            val opening = awaitItem()

            val advanced = viewModel.advance(1)

            assertEquals(opening.steps, advanced.steps)
            assertEquals(1, advanced.activeStepIndex)
            assertEquals(prayer.lines[1], advanced.activeLineText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the column carries every line of the prayer, in the order it is prayed`() = runTest {
        viewModel().loadedStates.test {
            assertEquals(prayer.lines, awaitItem().lineTexts)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the session rests at every movement boundary and names what comes next`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)

            // Two lines of the first movement, then the rest before the second.
            val rested = viewModel.advance(2)
            val pause = rested.activeStep as SessionStepUiState.Pause

            assertEquals(prayer.movements[1].heading, pause.nextMovementHeading)
            assertEquals(2, pause.nextMovementNumber)
            assertEquals(3, pause.movementCount)
            // A rest is a place in the prayer, not an emptying of it: every line is still there.
            assertEquals(prayer.lines, rested.lineTexts)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the rest is followed by the movement it named`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)

            val uiState = viewModel.advance(3)

            assertEquals(prayer.movements[1].lines.single(), uiState.activeLineText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the ticks count movements, and a pause belongs to the movement it opens onto`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            assertEquals(
                listOf(MovementTick.Current, MovementTick.ToCome, MovementTick.ToCome),
                viewModel.settled().movementTicks,
            )

            val pause = viewModel.advance(2)

            assertEquals(
                listOf(MovementTick.Spent, MovementTick.Current, MovementTick.ToCome),
                pause.movementTicks,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the last line is the end, and there is no pause after the last movement`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)

            // Five lines and two pauses: six moves reach the last line.
            val end = viewModel.advance(6)

            assertEquals(prayer.lines.last(), end.activeLineText)
            assertTrue(end.isAtEnd)

            // Nowhere further to go, however hard the reader taps.
            assertEquals(end, viewModel.advance(3))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `going back returns to the line before, and never past the beginning`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            viewModel.advance(2)

            viewModel.onAction(SessionAction.GoBack)

            val uiState = viewModel.settled()
            assertEquals(1, uiState.activeStepIndex)
            assertEquals(prayer.lines[1], uiState.activeLineText)

            repeat(5) { viewModel.onAction(SessionAction.GoBack) }
            val beginning = viewModel.settled()
            assertEquals(0, beginning.activeStepIndex)
            assertEquals(prayer.lines[0], beginning.activeLineText)
            assertFalse(beginning.canGoBack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** A rotation is a new ViewModel over the same handle: the session comes back to itself. */
    @Test
    fun `the place in the prayer survives a rotation, a pause included`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("prayerId" to prayer.id))
        val before = viewModel(savedStateHandle)

        before.loadedStates.test {
            skipItems(1)
            before.advance(3)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel(savedStateHandle).loadedStates.test {
            assertEquals(prayer.movements[1].lines.single(), awaitItem().activeLineText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a rotation taken at a pause comes back to the pause`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("prayerId" to prayer.id))
        val before = viewModel(savedStateHandle)

        before.loadedStates.test {
            skipItems(1)
            before.advance(2)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel(savedStateHandle).loadedStates.test {
            val pause = awaitItem().activeStep as SessionStepUiState.Pause
            assertEquals(prayer.movements[1].heading, pause.nextMovementHeading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a paced session carries its dwell, and a reader-paced one carries none`() = runTest {
        viewModel().loadedStates.test {
            assertNull(awaitItem().autoAdvanceAfterMillis)
            cancelAndIgnoreRemainingEvents()
        }

        val steady = viewModel(
            settingsRepository = FakeSettingsRepository(
                PrayerSettings(sessionPacing = SessionPacing.Steady),
            ),
        )
        steady.loadedStates.test {
            assertEquals(
                SessionPacing.Steady.lineDwellMillis,
                awaitItem().autoAdvanceAfterMillis,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Otherwise the end of the prayer would time out from under the reader's "Amen". */
    @Test
    fun `nothing moves on by itself once the prayer has ended`() = runTest {
        val viewModel = viewModel(
            settingsRepository = FakeSettingsRepository(
                PrayerSettings(sessionPacing = SessionPacing.Unhurried),
            ),
        )

        viewModel.loadedStates.test {
            skipItems(1)

            val end = viewModel.advance(6)

            assertTrue(end.isAtEnd)
            assertNull(end.autoAdvanceAfterMillis)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the screen is kept awake only when the reader asked for it`() = runTest {
        val viewModel = viewModel(
            settingsRepository = FakeSettingsRepository(
                PrayerSettings(keepsScreenOnDuringSession = false),
            ),
        )

        viewModel.loadedStates.test {
            assertFalse(awaitItem().keepsScreenOn)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
