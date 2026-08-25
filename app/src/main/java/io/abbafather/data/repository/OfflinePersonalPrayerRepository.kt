package io.abbafather.data.repository

import io.abbafather.core.common.IoDispatcher
import io.abbafather.data.local.dao.PersonalPrayerDao
import io.abbafather.data.mapper.toDomain
import io.abbafather.data.mapper.toEntity
import io.abbafather.domain.model.PersonalPrayer
import io.abbafather.domain.repository.PersonalPrayerRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflinePersonalPrayerRepository @Inject constructor(
    private val personalPrayerDao: PersonalPrayerDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PersonalPrayerRepository {

    override fun observePersonalPrayers(): Flow<List<PersonalPrayer>> =
        personalPrayerDao.observePersonalPrayers()
            .map { prayers -> prayers.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observePersonalPrayer(personalPrayerId: String): Flow<PersonalPrayer?> =
        personalPrayerDao.observePersonalPrayer(personalPrayerId)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)

    override suspend fun getPersonalPrayer(personalPrayerId: String): PersonalPrayer? =
        withContext(ioDispatcher) { personalPrayerDao.getPersonalPrayer(personalPrayerId)?.toDomain() }

    override suspend fun upsertPersonalPrayer(personalPrayer: PersonalPrayer) =
        withContext(ioDispatcher) { personalPrayerDao.upsertPersonalPrayer(personalPrayer.toEntity()) }

    override suspend fun deletePersonalPrayer(personalPrayerId: String, deletedAt: Long) =
        withContext(ioDispatcher) {
            personalPrayerDao.markPersonalPrayerDeleted(personalPrayerId, deletedAt)
        }
}
