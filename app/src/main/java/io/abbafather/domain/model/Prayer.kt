package io.abbafather.domain.model

/**
 * A prayer from the bundled catalogue. Read-only to the reader: prayers they write themselves are
 * [PersonalPrayer]s.
 *
 * [lines] is the prayer already broken the way it is prayed — one line is one screen in a session,
 * so the break positions are content, not formatting. [breathingPauseAfterLine] is the index of the
 * line the session rests after; `null` means the prayer is prayed straight through.
 */
data class Prayer(
    val id: String,
    val title: String,
    val author: String?,
    val kind: PrayerKind,
    val group: PrayerGroup,
    val themes: Set<PrayerTheme>,
    val lines: List<String>,
    val breathingPauseAfterLine: Int? = null,
    val lastOpenedAt: Long? = null,
) {
    val openingLine: String get() = lines.first()

    val attribution: String get() = author ?: group.displayName

    fun hasBreathingPauseAfter(lineIndex: Int): Boolean = breathingPauseAfterLine == lineIndex
}
