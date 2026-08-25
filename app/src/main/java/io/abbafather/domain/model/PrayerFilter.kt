package io.abbafather.domain.model

/**
 * The Library's current narrowing. An empty set means "not narrowed by this", so the default filter
 * matches the whole catalogue.
 */
data class PrayerFilter(
    val parts: Set<PrayerPart> = emptySet(),
    val tags: Set<PrayerTag> = emptySet(),
) {
    val isEmpty: Boolean get() = parts.isEmpty() && tags.isEmpty()

    fun matches(prayer: Prayer): Boolean =
        (parts.isEmpty() || prayer.part in parts) &&
            (tags.isEmpty() || prayer.tags.any { it in tags })
}
