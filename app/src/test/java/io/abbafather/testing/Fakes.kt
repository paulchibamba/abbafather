package io.abbafather.testing

import io.abbafather.domain.model.FontLicence
import io.abbafather.domain.model.PersonalPrayer
import io.abbafather.domain.model.Prayer
import io.abbafather.domain.model.PrayerCollection
import io.abbafather.domain.model.PrayerMovement
import io.abbafather.domain.model.PrayerPart
import io.abbafather.domain.model.PrayerProvenance
import io.abbafather.domain.model.PrayerSettings
import io.abbafather.domain.model.PrayerTag
import io.abbafather.domain.model.PrayerVoice
import io.abbafather.domain.model.SavedLine
import io.abbafather.domain.model.ScriptureReference
import io.abbafather.domain.model.SessionPacing
import io.abbafather.domain.repository.LicenceRepository
import io.abbafather.domain.repository.PersonalPrayerRepository
import io.abbafather.domain.repository.PrayerRepository
import io.abbafather.domain.repository.SavedLineRepository
import io.abbafather.domain.repository.SettingsRepository
import io.abbafather.domain.util.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Hand-written fakes rather than a mocking framework — see `docs/DECISIONS.md`. They behave like the
 * real repositories (soft deletes hide rows, flows re-emit on write) so a test that passes against
 * them is testing the use case, not the double.
 */
class FakePrayerRepository(prayers: List<Prayer> = emptyList()) : PrayerRepository {

    private val prayers = MutableStateFlow(prayers)
    private val collections = MutableStateFlow<List<PrayerCollection>>(emptyList())

    override fun observePrayers(): Flow<List<Prayer>> = prayers

    override fun observePrayer(prayerId: String): Flow<Prayer?> =
        prayers.map { all -> all.firstOrNull { it.id == prayerId } }

    override fun observeRecentlyOpenedPrayers(limit: Int): Flow<List<Prayer>> =
        prayers.map { all ->
            all.filter { it.lastOpenedAt != null }
                .sortedByDescending { it.lastOpenedAt }
                .take(limit)
        }

    override suspend fun getPrayer(prayerId: String): Prayer? =
        prayers.value.firstOrNull { it.id == prayerId }

    override suspend fun recordPrayerOpened(prayerId: String, openedAt: Long) {
        prayers.update { all ->
            all.map { if (it.id == prayerId) it.copy(lastOpenedAt = openedAt) else it }
        }
    }

    override fun observeCollections(): Flow<List<PrayerCollection>> = collections

    override fun observeCollection(collectionId: String): Flow<PrayerCollection?> =
        collections.map { all -> all.firstOrNull { it.id == collectionId } }

    override suspend fun upsertCollection(collection: PrayerCollection) {
        collections.update { all ->
            all.filterNot { it.id == collection.id } + collection
        }
    }

    override suspend fun addPrayerToCollection(collectionId: String, prayerId: String, updatedAt: Long) {
        collections.update { all ->
            all.map {
                if (it.id == collectionId) {
                    it.copy(memberPrayerIds = it.memberPrayerIds + prayerId, updatedAt = updatedAt)
                } else {
                    it
                }
            }
        }
    }

    override suspend fun removePrayerFromCollection(
        collectionId: String,
        prayerId: String,
        updatedAt: Long,
    ) {
        collections.update { all ->
            all.map {
                if (it.id == collectionId) {
                    it.copy(memberPrayerIds = it.memberPrayerIds - prayerId, updatedAt = updatedAt)
                } else {
                    it
                }
            }
        }
    }

    override suspend fun deleteCollection(collectionId: String, deletedAt: Long) {
        collections.update { all -> all.filterNot { it.id == collectionId } }
    }
}

class FakeSavedLineRepository(savedLines: List<SavedLine> = emptyList()) : SavedLineRepository {

    private val savedLines = MutableStateFlow(savedLines)

    val storedLines: List<SavedLine> get() = this.savedLines.value.filterNot { it.isDeleted }

    override fun observeSavedLines(): Flow<List<SavedLine>> =
        savedLines.map { all -> all.filterNot { it.isDeleted }.sortedByDescending { it.createdAt } }

    override fun observeSavedLine(savedLineId: String): Flow<SavedLine?> =
        savedLines.map { all -> all.firstOrNull { it.id == savedLineId && !it.isDeleted } }

    override fun observeIsLineSaved(prayerId: String, lineIndex: Int): Flow<Boolean> =
        savedLines.map { all ->
            all.any { !it.isDeleted && it.sourcePrayerId == prayerId && it.sourceLineIndex == lineIndex }
        }

    override suspend fun getSavedLine(savedLineId: String): SavedLine? =
        savedLines.value.firstOrNull { it.id == savedLineId && !it.isDeleted }

    override suspend fun upsertSavedLine(savedLine: SavedLine) {
        savedLines.update { all -> all.filterNot { it.id == savedLine.id } + savedLine }
    }

    override suspend fun deleteSavedLine(savedLineId: String, deletedAt: Long) {
        savedLines.update { all ->
            all.map {
                if (it.id == savedLineId) it.copy(isDeleted = true, updatedAt = deletedAt) else it
            }
        }
    }
}

class FakePersonalPrayerRepository(
    personalPrayers: List<PersonalPrayer> = emptyList(),
) : PersonalPrayerRepository {

    private val personalPrayers = MutableStateFlow(personalPrayers)

    val storedPrayers: List<PersonalPrayer> get() = this.personalPrayers.value.filterNot { it.isDeleted }

    override fun observePersonalPrayers(): Flow<List<PersonalPrayer>> =
        personalPrayers.map { all -> all.filterNot { it.isDeleted }.sortedByDescending { it.updatedAt } }

    override fun observePersonalPrayer(personalPrayerId: String): Flow<PersonalPrayer?> =
        personalPrayers.map { all -> all.firstOrNull { it.id == personalPrayerId && !it.isDeleted } }

    override suspend fun getPersonalPrayer(personalPrayerId: String): PersonalPrayer? =
        personalPrayers.value.firstOrNull { it.id == personalPrayerId && !it.isDeleted }

    override suspend fun upsertPersonalPrayer(personalPrayer: PersonalPrayer) {
        personalPrayers.update { all -> all.filterNot { it.id == personalPrayer.id } + personalPrayer }
    }

    override suspend fun deletePersonalPrayer(personalPrayerId: String, deletedAt: Long) {
        personalPrayers.update { all ->
            all.map {
                if (it.id == personalPrayerId) it.copy(isDeleted = true, updatedAt = deletedAt) else it
            }
        }
    }
}

/** The reader's choices, held in memory. Writes re-emit, as DataStore's do. */
class FakeSettingsRepository(settings: PrayerSettings = PrayerSettings.Default) :
    SettingsRepository {

    private val settings = MutableStateFlow(settings)

    override fun observeSettings(): Flow<PrayerSettings> = settings

    override suspend fun setSessionPacing(pacing: SessionPacing) {
        settings.update { it.copy(sessionPacing = pacing) }
    }

    override suspend fun setAmbientSoundEnabled(isEnabled: Boolean) {
        settings.update { it.copy(isAmbientSoundEnabled = isEnabled) }
    }

    override suspend fun setKeepsScreenOnDuringSession(keepsScreenOn: Boolean) {
        settings.update { it.copy(keepsScreenOnDuringSession = keepsScreenOn) }
    }
}

/** The notices, without going near the assets. Counts reads, because they should happen once. */
class FakeLicenceRepository(private val licences: List<FontLicence> = DefaultLicences) :
    LicenceRepository {

    var readCount = 0
        private set

    override suspend fun getFontLicences(): List<FontLicence> {
        readCount++
        return licences
    }

    companion object {
        val DefaultLicences = listOf(
            FontLicence(
                fontName = "Cormorant Garamond",
                copyrightLine = "Copyright 2015 the Cormorant Project Authors",
                text = "Copyright 2015 the Cormorant Project Authors\n\nSIL OPEN FONT LICENSE Version 1.1",
            ),
            FontLicence(
                fontName = "Work Sans",
                copyrightLine = "Copyright 2019 The Work Sans Project Authors",
                text = "Copyright 2019 The Work Sans Project Authors\n\nSIL OPEN FONT LICENSE Version 1.1",
            ),
        )
    }
}

/** Ids in the order a test can predict: `id-1`, `id-2`, … */
class CountingIdGenerator : IdGenerator {
    private var issued = 0
    override fun newId(): String = "id-${++issued}"
}

/**
 * A catalogue prayer for a test to point at. Most tests care about one movement's worth of lines and
 * say so with [lines]; a test about movements — pauses, headings, a line's place in the whole — gives
 * [movementLines] instead and gets the flat line indices computed the way the seeder computes them.
 */
fun testPrayer(
    id: String,
    title: String = "Morning",
    part: PrayerPart = PrayerPart.NeedsAndDevotions,
    voice: PrayerVoice = PrayerVoice.Personal,
    tags: Set<PrayerTag> = setOf(PrayerTag.Grace),
    lines: List<String> = listOf(
        "Compassionate Lord, I woke up today because your mercy carried me here.",
        "Thank you for the gift of another morning.",
    ),
    movementLines: List<List<String>> = listOf(lines),
    headings: List<String> = movementLines.indices.map { "Movement ${it + 1}" },
    provenance: PrayerProvenance = testProvenance,
    lastOpenedAt: Long? = null,
) = Prayer(
    id = id,
    title = title,
    part = part,
    voice = voice,
    tags = tags,
    movements = testMovements(movementLines, headings),
    provenance = provenance,
    lastOpenedAt = lastOpenedAt,
)

fun testMovements(
    movementLines: List<List<String>>,
    headings: List<String> = movementLines.indices.map { "Movement ${it + 1}" },
): List<PrayerMovement> {
    var firstLineIndex = 0
    return movementLines.mapIndexed { index, lines ->
        PrayerMovement(
            index = index,
            heading = headings[index],
            lines = lines,
            firstLineIndex = firstLineIndex,
            themes = listOf("What movement ${index + 1} holds."),
            scriptures = listOf(
                ScriptureReference(
                    reference = "Psalm ${index + 1}:1",
                    translation = "ESV",
                    connection = "Why Psalm ${index + 1} stands under this movement.",
                ),
            ),
        ).also { firstLineIndex += lines.size }
    }
}

val testProvenance = PrayerProvenance(
    originalTitle = "Morning",
    originalAuthor = "Unattributed Puritan source (compiled and edited by Arthur Bennett)",
    originalSource = "The Valley of Vision: A Collection of Puritan Prayers and Devotions",
    originalPublicationDate = "1975",
    copyrightStatus = "Compilation in copyright; underlying Puritan sources are public domain.",
    adaptationType = "thematic modern adaptation",
    adaptationNote = "Contemporary prayer based on the themes of the historical source.",
)
