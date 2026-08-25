package io.abbafather.data.repository

import io.abbafather.core.common.IoDispatcher
import io.abbafather.data.local.dao.PrayerCollectionDao
import io.abbafather.data.local.dao.PrayerDao
import io.abbafather.data.mapper.toDomain
import io.abbafather.data.mapper.toEntity
import io.abbafather.data.mapper.toMemberEntities
import io.abbafather.domain.model.Prayer
import io.abbafather.domain.model.PrayerCollection
import io.abbafather.domain.repository.PrayerRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflinePrayerRepository @Inject constructor(
    private val prayerDao: PrayerDao,
    private val collectionDao: PrayerCollectionDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PrayerRepository {

    override fun observePrayers(): Flow<List<Prayer>> =
        prayerDao.observePrayers().map { prayers -> prayers.map { it.toDomain() } }.flowOn(ioDispatcher)

    override fun observePrayer(prayerId: String): Flow<Prayer?> =
        prayerDao.observePrayer(prayerId).map { it?.toDomain() }.flowOn(ioDispatcher)

    override fun observeRecentlyOpenedPrayers(limit: Int): Flow<List<Prayer>> =
        prayerDao.observeRecentlyOpenedPrayers(limit)
            .map { prayers -> prayers.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getPrayer(prayerId: String): Prayer? = withContext(ioDispatcher) {
        prayerDao.getPrayer(prayerId)?.toDomain()
    }

    override suspend fun recordPrayerOpened(prayerId: String, openedAt: Long) =
        withContext(ioDispatcher) { prayerDao.updateLastOpenedAt(prayerId, openedAt) }

    override fun observeCollections(): Flow<List<PrayerCollection>> =
        collectionDao.observeCollections()
            .map { collections -> collections.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeCollection(collectionId: String): Flow<PrayerCollection?> =
        collectionDao.observeCollection(collectionId).map { it?.toDomain() }.flowOn(ioDispatcher)

    override suspend fun upsertCollection(collection: PrayerCollection) = withContext(ioDispatcher) {
        collectionDao.upsertCollectionWithMembers(collection.toEntity(), collection.toMemberEntities())
    }

    override suspend fun addPrayerToCollection(
        collectionId: String,
        prayerId: String,
        updatedAt: Long,
    ) = withContext(ioDispatcher) { collectionDao.addMember(collectionId, prayerId, updatedAt) }

    override suspend fun removePrayerFromCollection(
        collectionId: String,
        prayerId: String,
        updatedAt: Long,
    ) = withContext(ioDispatcher) { collectionDao.removeMember(collectionId, prayerId, updatedAt) }

    override suspend fun deleteCollection(collectionId: String, deletedAt: Long) =
        withContext(ioDispatcher) { collectionDao.markCollectionDeleted(collectionId, deletedAt) }
}
