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

    @Test
    fun `the session opens on the first line of the first movement`() = runTest {
        viewModel().loadedStates.test {
            val uiState = awaitItem()

            assertEquals(prayer.title, uiState.title)
            assertEquals(prayer.attribution, uiState.attribution)
            assertEquals(listOf(prayer.lines[0]), uiState.movementLines.map { it.text })
            assertTrue(uiState.movementLines.single().isCurrent)
            assertNull(uiState.breathingPause)
            assertFalse(uiState.canGoBack)
            assertFalse(uiState.isAtEnd)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `earlier lines of the movement stay on screen behind the one being prayed`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)

            val uiState = viewModel.advance(1)

            assertEquals(prayer.lines.take(2), uiState.movementLines.map { it.text })
            assertEquals(listOf(false, true), uiState.movementLines.map { it.isCurrent })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the session rests at every movement boundary and names what comes next`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)

            // Two lines of the first movement, then the rest before the second.
            val pause = viewModel.advance(2)

            assertEquals(prayer.movements[1].heading, pause.breathingPause?.nextMovementHeading)
            assertEquals(2, pause.breathingPause?.nextMovementNumber)
            assertEquals(3, pause.breathingPause?.movementCount)
            // A pause is instead of a line, never alongside one.
            assertTrue(pause.movementLines.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a movement begins on an empty ground rather than under the one before it`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            skipItems(1)

            val uiState = viewModel.advance(3)

            assertEquals(
                listOf(prayer.movements[1].lines.single()),
                uiState.movementLines.map { it.text },
            )
            assertNull(uiState.breathingPause)
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

            assertEquals(prayer.lines.last(), end.movementLines.last().text)
            assertTrue(end.isAtEnd)
            assertNull(end.breathingPause)

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
            assertEquals(prayer.lines.take(2), uiState.movementLines.map { it.text })

            repeat(5) { viewModel.onAction(SessionAction.GoBack) }
            val beginning = viewModel.settled()
            assertEquals(listOf(prayer.lines[0]), beginning.movementLines.map { it.text })
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
            assertEquals(
                listOf(prayer.movements[1].lines.single()),
                awaitItem().movementLines.map { it.text },
            )
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
            assertEquals(
                prayer.movements[1].heading,
                awaitItem().breathingPause?.nextMovementHeading,
            )
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
