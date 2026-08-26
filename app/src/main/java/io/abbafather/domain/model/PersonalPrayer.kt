package io.abbafather.domain.model

/**
 * A prayer the reader wrote. [body] is kept exactly as typed — the blank lines they left are how
 * they hear it — and [lines] derives the session breaks from it without changing the stored text.
 */
data class PersonalPrayer(
    val id: String,
    val title: String,
    val body: String,
    val tags: Set<PrayerTag> = emptySet(),
    val seededFromSavedLineId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
) {
    val lines: List<String>
        get() = body.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()

    val excerpt: String get() = lines.firstOrNull().orEmpty()
}
