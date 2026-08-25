package io.abbafather.domain.model

/** How long a session rests on a line before it offers the next one. */
enum class SessionPacing(val displayName: String, val lineDwellMillis: Long) {
    Unhurried("Unhurried", 12_000L),
    Steady("Steady", 8_000L),
    Reader("At my own pace", 0L),
}

/** Everything the reader can choose. Settings only — never content. */
data class PrayerSettings(
    val sessionPacing: SessionPacing = SessionPacing.Reader,
    val isAmbientSoundEnabled: Boolean = false,
    val keepsScreenOnDuringSession: Boolean = true,
) {
    companion object {
        val Default = PrayerSettings()
    }
}
