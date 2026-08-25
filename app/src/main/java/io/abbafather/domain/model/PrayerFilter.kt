package io.abbafather.domain.model

/**
 * The Library's current narrowing. An empty set means "not narrowed by this", so the default filter
 * matches the whole catalogue.
 */
data class PrayerFilter(
    val groups: Set<PrayerGroup> = emptySet(),
    val kinds: Set<PrayerKind> = emptySet(),
    val themes: Set<PrayerTheme> = emptySet(),
) {
    val isEmpty: Boolean get() = groups.isEmpty() && kinds.isEmpty() && themes.isEmpty()

    fun matches(prayer: Prayer): Boolean =
        (groups.isEmpty() || prayer.group in groups) &&
            (kinds.isEmpty() || prayer.kind in kinds) &&
            (themes.isEmpty() || prayer.themes.any { it in themes })
}
