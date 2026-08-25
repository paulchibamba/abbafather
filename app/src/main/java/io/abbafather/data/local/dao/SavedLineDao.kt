package io.abbafather.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.abbafather.data.local.entity.SavedLineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLineDao {

    @Query("SELECT * FROM saved_lines WHERE is_deleted = 0 ORDER BY created_at DESC")
    fun observeSavedLines(): Flow<List<SavedLineEntity>>

    @Query("SELECT * FROM saved_lines WHERE id = :savedLineId AND is_deleted = 0")
    fun observeSavedLine(savedLineId: String): Flow<SavedLineEntity?>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM saved_lines WHERE source_prayer_id = :prayerId " +
            "AND source_line_index = :lineIndex AND is_deleted = 0)",
    )
    fun observeIsLineSaved(prayerId: String, lineIndex: Int): Flow<Boolean>

    @Query("SELECT * FROM saved_lines WHERE id = :savedLineId AND is_deleted = 0")
    suspend fun getSavedLine(savedLineId: String): SavedLineEntity?

    @Upsert
    suspend fun upsertSavedLine(savedLine: SavedLineEntity)

    /** Soft delete: the row stays so a later sync layer can carry the removal with it. */
    @Query("UPDATE saved_lines SET is_deleted = 1, updated_at = :deletedAt WHERE id = :savedLineId")
    suspend fun markSavedLineDeleted(savedLineId: String, deletedAt: Long)
}
