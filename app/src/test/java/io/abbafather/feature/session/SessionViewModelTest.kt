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

    private val SessionUiState.activeLine: String get() = lines[activeLineIndex]

    private val SessionUiState.prayedFractions: List<Float>
        get() = movementProgress.map(MovementProgress::prayedFraction)

    @Test
    fun `the session opens on the first line, with the whole prayer under it`() = runTest {
        viewModel().loadedStates.test {
            val uiState = awaitItem()

            assertEquals(prayer.title, uiState.title)
            assertEquals(prayer.attribution, uiState.attribution)
            assertEquals(prayer.lines, uiState.lines)
            assertEquals(0, uiState.activeLineIndex)
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
    fun `advancing moves the active line and leaves the column alone`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            val opening = awaitItem()

            val advanced = viewModel.advance(1)

            assertEquals(opening.lines, advanced.lines)
            assertEquals(1, advanced.activeLineIndex)
            assertEquals(prayer.lines[1], advanced.activeLine)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * The rests between movements belong to the reader screen. A session prays the whole thing
     * through, so advancing off the end of a movement lands on the next movement's first line.
     */
    @Test
    fun `the prayer runs straight through its movements without resting`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)

            // The first movement's two lines, then straight into the second movement's only one.
            val uiState = viewModel.advance(2)

            assertEquals(prayer.movements[1].lines.single(), uiState.activeLine)
            assertEquals(prayer.movements[1].firstLineIndex, uiState.activeLineIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Three movements of two, one and two lines, so a half-prayed movement is a real fraction. */
    @Test
    fun `a movement fills as it is prayed, and stays full once it is behind`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)
            val opening = viewModel.settled()
            assertEquals(listOf(0.5f, 0f, 0f), opening.prayedFractions)
            assertEquals(listOf(true, false, false), opening.movementProgress.map { it.isCurrent })

            val secondMovement = viewModel.advance(2)

            assertEquals(listOf(1f, 1f, 0f), secondMovement.prayedFractions)
            assertEquals(
                listOf(false, true, false),
                secondMovement.movementProgress.map { it.isCurrent },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the last line is the end of the prayer`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)

            // Five lines: four moves reach the last one.
            val end = viewModel.advance(4)

            assertEquals(prayer.lines.last(), end.activeLine)
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
            assertEquals(1, uiState.activeLineIndex)
            assertEquals(prayer.lines[1], uiState.activeLine)

            repeat(5) { viewModel.onAction(SessionAction.GoBack) }
            val beginning = viewModel.settled()
            assertEquals(0, beginning.activeLineIndex)
            assertEquals(prayer.lines[0], beginning.activeLine)
            assertFalse(beginning.canGoBack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** A rotation is a new ViewModel over the same handle: the session comes back to itself. */
    @Test
    fun `the place in the prayer survives a rotation`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("prayerId" to prayer.id))
        val before = viewModel(savedStateHandle)

        before.loadedStates.test {
            skipItems(1)
            before.advance(2)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel(savedStateHandle).loadedStates.test {
            assertEquals(prayer.movements[1].lines.single(), awaitItem().activeLine)
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

            val end = viewModel.advance(4)

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
