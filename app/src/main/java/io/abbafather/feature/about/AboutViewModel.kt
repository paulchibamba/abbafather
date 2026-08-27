package io.abbafather.feature.about

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.abbafather.core.common.AppInfo
import io.abbafather.domain.model.FontLicence
import io.abbafather.domain.model.SessionPacing
import io.abbafather.domain.repository.LicenceRepository
import io.abbafather.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Holds the reader's two session choices and the notices the app is obliged to carry.
 *
 * The licences are read from the bundled assets once, as a flow that emits a single list, so a
 * rotation does not go back to the filesystem for four thousand words that cannot have changed.
 */
@HiltViewModel
class AboutViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    licenceRepository: LicenceRepository,
    appInfo: AppInfo,
) : ViewModel() {

    private val openLicenceFontName: StateFlow<String?> =
        savedStateHandle.getStateFlow(OpenLicenceKey, null)

    private val fontLicences = flow { emit(licenceRepository.getFontLicences()) }

    val uiState: StateFlow<AboutUiState> = combine(
        settingsRepository.observeSettings(),
        fontLicences,
        openLicenceFontName,
    ) { settings, licences, openFontName ->
        AboutUiState(
            sessionPacing = settings.sessionPacing,
            keepsScreenOnDuringSession = settings.keepsScreenOnDuringSession,
            fontLicences = licences.map { it.toUiState(isOpen = it.fontName == openFontName) },
            appVersionName = appInfo.versionName,
            isLoaded = true,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AboutUiState(appVersionName = appInfo.versionName),
    )

    fun onAction(action: AboutAction) {
        when (action) {
            is AboutAction.ChoosePacing -> setPacing(action.pacing)
            is AboutAction.SetKeepsScreenOn -> setKeepsScreenOn(action.keepsScreenOn)
            is AboutAction.ToggleLicence -> toggleLicence(action.fontName)
            // The navigator's alone; this screen keeps no record of leaving.
            AboutAction.Back -> Unit
        }
    }

    private fun setPacing(pacing: SessionPacing) {
        viewModelScope.launch { settingsRepository.setSessionPacing(pacing) }
    }

    private fun setKeepsScreenOn(keepsScreenOn: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepsScreenOnDuringSession(keepsScreenOn) }
    }

    /** One licence is open at a time; tapping the open one closes it. */
    private fun toggleLicence(fontName: String) {
        savedStateHandle[OpenLicenceKey] =
            fontName.takeIf { it != openLicenceFontName.value }
    }

    private fun FontLicence.toUiState(isOpen: Boolean) = FontLicenceUiState(
        fontName = fontName,
        copyrightLine = copyrightLine,
        text = text,
        isOpen = isOpen,
    )

    private companion object {
        const val OpenLicenceKey = "openLicenceFontName"
    }
}
