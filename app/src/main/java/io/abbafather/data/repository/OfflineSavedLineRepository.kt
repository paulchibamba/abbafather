package io.abbafather.data.repository

import io.abbafather.core.common.IoDispatcher
import io.abbafather.data.local.dao.SavedLineDao
import io.abbafather.data.mapper.toDomain
import io.abbafather.data.mapper.toEntity
import io.abbafather.domain.model.SavedLine
import io.abbafather.domain.repository.SavedLineRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineSavedLineRepository @Inject constructor(
    private val savedLineDao: SavedLineDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SavedLineRepository {

    override fun observeSavedLines(): Flow<List<SavedLine>> =
        savedLineDao.observeSavedLines().map { lines -> lines.map { it.toDomain() } }.flowOn(ioDispatcher)

    override fun observeSavedLine(savedLineId: String): Flow<SavedLine?> =
        savedLineDao.observeSavedLine(savedLineId).map { it?.toDomain() }.flowOn(ioDispatcher)

    override fun observeIsLineSaved(prayerId: String, lineIndex: Int): Flow<Boolean> =
        savedLineDao.observeIsLineSaved(prayerId, lineIndex).flowOn(ioDispatcher)

    override suspend fun getSavedLine(savedLineId: String): SavedLine? = withContext(ioDispatcher) {
        savedLineDao.getSavedLine(savedLineId)?.toDomain()
    }

    override suspend fun upsertSavedLine(savedLine: SavedLine) = withContext(ioDispatcher) {
        savedLineDao.upsertSavedLine(savedLine.toEntity())
    }

    override suspend fun deleteSavedLine(savedLineId: String, deletedAt: Long) =
        withContext(ioDispatcher) { savedLineDao.markSavedLineDeleted(savedLineId, deletedAt) }
}
