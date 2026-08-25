package io.abbafather.data.repository

import io.abbafather.data.preferences.SettingsDataStore
import io.abbafather.domain.model.PrayerSettings
import io.abbafather.domain.model.SessionPacing
import io.abbafather.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
) : SettingsRepository {

    override fun observeSettings(): Flow<PrayerSettings> = settingsDataStore.settings

    override suspend fun setSessionPacing(pacing: SessionPacing) =
        settingsDataStore.setSessionPacing(pacing)

    override suspend fun setAmbientSoundEnabled(isEnabled: Boolean) =
        settingsDataStore.setAmbientSoundEnabled(isEnabled)

    override suspend fun setKeepsScreenOnDuringSession(keepsScreenOn: Boolean) =
        settingsDataStore.setKeepsScreenOnDuringSession(keepsScreenOn)
}
