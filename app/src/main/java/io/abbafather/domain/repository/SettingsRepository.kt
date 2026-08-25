package io.abbafather.domain.repository

import io.abbafather.domain.model.PrayerSettings
import io.abbafather.domain.model.SessionPacing
import kotlinx.coroutines.flow.Flow

/** The reader's choices. Never content. */
interface SettingsRepository {

    fun observeSettings(): Flow<PrayerSettings>

    suspend fun setSessionPacing(pacing: SessionPacing)

    suspend fun setAmbientSoundEnabled(isEnabled: Boolean)

    suspend fun setKeepsScreenOnDuringSession(keepsScreenOn: Boolean)
}
