package io.abbafather.domain.repository

import io.abbafather.domain.model.SavedLine
import kotlinx.coroutines.flow.Flow

/** Lines the reader has kept. Newest first, because the Saved screen reads as a journal. */
interface SavedLineRepository {

    fun observeSavedLines(): Flow<List<SavedLine>>

    fun observeSavedLine(savedLineId: String): Flow<SavedLine?>

    /** Whether this exact line of this prayer has already been kept. */
    fun observeIsLineSaved(prayerId: String, lineIndex: Int): Flow<Boolean>

    suspend fun getSavedLine(savedLineId: String): SavedLine?

    suspend fun upsertSavedLine(savedLine: SavedLine)

    suspend fun deleteSavedLine(savedLineId: String, deletedAt: Long)
}
