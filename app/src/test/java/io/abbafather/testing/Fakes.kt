package io.abbafather.testing

import io.abbafather.domain.model.PersonalPrayer
import io.abbafather.domain.model.Prayer
import io.abbafather.domain.model.PrayerCollection
import io.abbafather.domain.model.PrayerGroup
import io.abbafather.domain.model.PrayerKind
import io.abbafather.domain.model.PrayerTheme
import io.abbafather.domain.model.SavedLine
import io.abbafather.domain.repository.PersonalPrayerRepository
import io.abbafather.domain.repository.PrayerRepository
import io.abbafather.domain.repository.SavedLineRepository
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

/** Ids in the order a test can predict: `id-1`, `id-2`, … */
class CountingIdGenerator : IdGenerator {
    private var issued = 0
    override fun newId(): String = "id-${++issued}"
}

fun testPrayer(
    id: String,
    title: String = "A Collect for Peace",
    author: String? = "Book of Common Prayer, 1662",
    kind: PrayerKind = PrayerKind.Evening,
    group: PrayerGroup = PrayerGroup.BookOfCommonPrayer,
    themes: Set<PrayerTheme> = setOf(PrayerTheme.Peace),
    lines: List<String> = listOf("O God, from whom all holy desires,", "all good counsels do proceed;"),
    breathingPauseAfterLine: Int? = null,
    lastOpenedAt: Long? = null,
) = Prayer(
    id = id,
    title = title,
    author = author,
    kind = kind,
    group = group,
    themes = themes,
    lines = lines,
    breathingPauseAfterLine = breathingPauseAfterLine,
    lastOpenedAt = lastOpenedAt,
)
