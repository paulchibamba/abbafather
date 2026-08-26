package io.abbafather.domain.model

/**
 * A single line the reader chose to keep. It carries its own copy of the text and of where it came
 * from, so a kept line still reads whole once the catalogue moves on around it.
 */
data class SavedLine(
    val id: String,
    val text: String,
    val sourcePrayerId: String?,
    val sourcePrayerTitle: String?,
    val sourceAttribution: String?,
    val sourceLineIndex: Int?,
    val tags: Set<PrayerTag> = emptySet(),
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
)
