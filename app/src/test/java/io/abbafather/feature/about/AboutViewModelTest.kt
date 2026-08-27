package io.abbafather.feature.about

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.abbafather.core.common.AppInfo
import io.abbafather.domain.model.PrayerSettings
import io.abbafather.domain.model.SessionPacing
import io.abbafather.testing.FakeLicenceRepository
import io.abbafather.testing.FakeSettingsRepository
import io.abbafather.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AboutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val licenceRepository = FakeLicenceRepository()

    private fun viewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        settingsRepository: FakeSettingsRepository = FakeSettingsRepository(),
    ) = AboutViewModel(
        savedStateHandle = savedStateHandle,
        settingsRepository = settingsRepository,
        licenceRepository = licenceRepository,
        appInfo = AppInfo(versionName = "1.0"),
    )

    private val AboutViewModel.loadedStates: Flow<AboutUiState>
        get() = uiState.filter { it.isLoaded }

    @Test
    fun `the page opens on the settings as they stand, with both notices closed`() = runTest {
        val viewModel = viewModel(
            settingsRepository = FakeSettingsRepository(
                PrayerSettings(
                    sessionPacing = SessionPacing.Steady,
                    keepsScreenOnDuringSession = false,
                ),
            ),
        )

        viewModel.loadedStates.test {
            val uiState = awaitItem()

            assertEquals(SessionPacing.Steady, uiState.sessionPacing)
            assertFalse(uiState.keepsScreenOnDuringSession)
            assertEquals(
                listOf("Cormorant Garamond", "Work Sans"),
                uiState.fontLicences.map(FontLicenceUiState::fontName),
            )
            assertTrue(uiState.fontLicences.none(FontLicenceUiState::isOpen))
            assertEquals("1.0", uiState.appVersionName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `every pacing the domain offers is on the page`() = runTest {
        val uiState = viewModel().loadedStates.first()

        assertEquals(SessionPacing.entries, uiState.pacingChoices)
    }

    @Test
    fun `choosing a pacing writes it to the settings and comes back changed`() = runTest {
        val settingsRepository = FakeSettingsRepository()
        val viewModel = viewModel(settingsRepository = settingsRepository)

        viewModel.loadedStates.test {
            assertEquals(SessionPacing.Reader, awaitItem().sessionPacing)

            viewModel.onAction(AboutAction.ChoosePacing(SessionPacing.Unhurried))
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(SessionPacing.Unhurried, awaitItem().sessionPacing)
            assertEquals(
                SessionPacing.Unhurried,
                settingsRepository.observeSettings().first().sessionPacing,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the screen-awake switch turns off and on`() = runTest {
        val settingsRepository = FakeSettingsRepository()
        val viewModel = viewModel(settingsRepository = settingsRepository)

        viewModel.loadedStates.test {
            assertTrue(awaitItem().keepsScreenOnDuringSession)

            viewModel.onAction(AboutAction.SetKeepsScreenOn(false))
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
            assertFalse(awaitItem().keepsScreenOnDuringSession)

            viewModel.onAction(AboutAction.SetKeepsScreenOn(true))
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(awaitItem().keepsScreenOnDuringSession)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `one licence is open at a time, and tapping the open one closes it`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            awaitItem()

            viewModel.onAction(AboutAction.ToggleLicence("Cormorant Garamond"))
            assertEquals(listOf("Cormorant Garamond"), awaitItem().openFontNames)

            viewModel.onAction(AboutAction.ToggleLicence("Work Sans"))
            assertEquals(listOf("Work Sans"), awaitItem().openFontNames)

            viewModel.onAction(AboutAction.ToggleLicence("Work Sans"))
            assertEquals(emptyList<String>(), awaitItem().openFontNames)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an open licence survives a rotation`() = runTest {
        val savedStateHandle = SavedStateHandle()
        viewModel(savedStateHandle = savedStateHandle).run {
            loadedStates.first()
            onAction(AboutAction.ToggleLicence("Work Sans"))
        }

        val rebuilt = viewModel(savedStateHandle = savedStateHandle)

        assertEquals(listOf("Work Sans"), rebuilt.loadedStates.first().openFontNames)
    }

    @Test
    fun `the notices are read from the assets once, not on every change`() = runTest {
        val viewModel = viewModel()

        viewModel.loadedStates.test {
            awaitItem()
            viewModel.onAction(AboutAction.ChoosePacing(SessionPacing.Steady))
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()
            viewModel.onAction(AboutAction.ToggleLicence("Work Sans"))
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, licenceRepository.readCount)
    }

    private val AboutUiState.openFontNames: List<String>
        get() = fontLicences.filter(FontLicenceUiState::isOpen).map(FontLicenceUiState::fontName)
}
