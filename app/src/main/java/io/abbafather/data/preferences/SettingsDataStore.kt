package io.abbafather.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.abbafather.domain.model.PrayerSettings
import io.abbafather.domain.model.SessionPacing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Settings only — pacing and the two session switches. Content never comes near this file; it lives
 * in Room, which is the only thing that gets migrated and the only thing worth backing up.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    private val preferences: DataStore<Preferences>,
) {

    val settings: Flow<PrayerSettings> = preferences.data.map { stored ->
        PrayerSettings(
            sessionPacing = stored[SessionPacingKey]?.toSessionPacing() ?: PrayerSettings.Default.sessionPacing,
            isAmbientSoundEnabled = stored[AmbientSoundKey] ?: PrayerSettings.Default.isAmbientSoundEnabled,
            keepsScreenOnDuringSession = stored[KeepScreenOnKey]
                ?: PrayerSettings.Default.keepsScreenOnDuringSession,
        )
    }

    suspend fun setSessionPacing(pacing: SessionPacing) {
        preferences.edit { it[SessionPacingKey] = pacing.name }
    }

    suspend fun setAmbientSoundEnabled(isEnabled: Boolean) {
        preferences.edit { it[AmbientSoundKey] = isEnabled }
    }

    suspend fun setKeepsScreenOnDuringSession(keepsScreenOn: Boolean) {
        preferences.edit { it[KeepScreenOnKey] = keepsScreenOn }
    }

    /** A pacing this build no longer offers falls back to the default rather than crashing. */
    private fun String.toSessionPacing(): SessionPacing? =
        SessionPacing.entries.firstOrNull { it.name == this }

    companion object {
        const val NAME = "prayer_settings"

        private val SessionPacingKey = stringPreferencesKey("session_pacing")
        private val AmbientSoundKey = booleanPreferencesKey("ambient_sound_enabled")
        private val KeepScreenOnKey = booleanPreferencesKey("keeps_screen_on_during_session")
    }
}
