package io.abbafather.domain.usecase

import io.abbafather.domain.model.Prayer
import io.abbafather.domain.model.PrayerTheme
import io.abbafather.domain.model.SavedLine
import io.abbafather.domain.repository.SavedLineRepository
import io.abbafather.domain.util.IdGenerator
import java.time.Clock
import javax.inject.Inject

/**
 * Keeps one line of a prayer. The line's text and where it came from are copied onto the saved row
 * so the Saved screen never has to reach back into the catalogue to read whole.
 */
class SaveLineUseCase @Inject constructor(
    private val savedLineRepository: SavedLineRepository,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        prayer: Prayer,
        lineIndex: Int,
        themes: Set<PrayerTheme> = emptySet(),
        note: String? = null,
    ): SavedLine {
        require(lineIndex in prayer.lines.indices) {
            "Line $lineIndex is outside ${prayer.title}, which has ${prayer.lines.size} lines"
        }
        val now = clock.millis()
        val savedLine = SavedLine(
            id = idGenerator.newId(),
            text = prayer.lines[lineIndex],
            sourcePrayerId = prayer.id,
            sourcePrayerTitle = prayer.title,
            sourceAttribution = prayer.attribution,
            sourceLineIndex = lineIndex,
            themes = themes.ifEmpty { prayer.themes },
            note = note?.takeIf { it.isNotBlank() },
            createdAt = now,
            updatedAt = now,
        )
        savedLineRepository.upsertSavedLine(savedLine)
        return savedLine
    }
}
