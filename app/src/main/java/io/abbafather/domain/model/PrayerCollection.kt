package io.abbafather.domain.model

/**
 * A reader-made grouping of catalogue prayers — "Morning prayers", "To memorise". Membership is an
 * ordered list because the reader's order is the point of making one.
 */
data class PrayerCollection(
    val id: String,
    val name: String,
    val memberPrayerIds: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
) {
    val prayerCount: Int get() = memberPrayerIds.size
}
