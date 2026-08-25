package io.abbafather.domain.repository

import io.abbafather.domain.model.Prayer
import io.abbafather.domain.model.PrayerCollection
import kotlinx.coroutines.flow.Flow

/** The bundled catalogue, plus the reader's collections over it. */
interface PrayerRepository {

    fun observePrayers(): Flow<List<Prayer>>

    fun observePrayer(prayerId: String): Flow<Prayer?>

    /** Most recently opened first, at most [limit] of them. */
    fun observeRecentlyOpenedPrayers(limit: Int): Flow<List<Prayer>>

    suspend fun getPrayer(prayerId: String): Prayer?

    suspend fun recordPrayerOpened(prayerId: String, openedAt: Long)

    fun observeCollections(): Flow<List<PrayerCollection>>

    fun observeCollection(collectionId: String): Flow<PrayerCollection?>

    suspend fun upsertCollection(collection: PrayerCollection)

    suspend fun addPrayerToCollection(collectionId: String, prayerId: String, updatedAt: Long)

    suspend fun removePrayerFromCollection(collectionId: String, prayerId: String, updatedAt: Long)

    suspend fun deleteCollection(collectionId: String, deletedAt: Long)
}
