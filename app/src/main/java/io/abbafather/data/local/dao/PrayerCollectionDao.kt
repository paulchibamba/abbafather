package io.abbafather.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.abbafather.data.local.entity.CollectionMemberEntity
import io.abbafather.data.local.entity.PrayerCollectionEntity
import io.abbafather.data.local.entity.PrayerCollectionWithMembers
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerCollectionDao {

    @Transaction
    @Query("SELECT * FROM prayer_collections WHERE is_deleted = 0 ORDER BY created_at")
    fun observeCollections(): Flow<List<PrayerCollectionWithMembers>>

    @Transaction
    @Query("SELECT * FROM prayer_collections WHERE id = :collectionId AND is_deleted = 0")
    fun observeCollection(collectionId: String): Flow<PrayerCollectionWithMembers?>

    /** The collection and its membership move together, or neither moves. */
    @Transaction
    suspend fun upsertCollectionWithMembers(
        collection: PrayerCollectionEntity,
        members: List<CollectionMemberEntity>,
    ) {
        upsertCollection(collection)
        deleteMembersOf(collection.id)
        insertMembers(members)
    }

    @Upsert
    suspend fun upsertCollection(collection: PrayerCollectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<CollectionMemberEntity>)

    @Query("DELETE FROM collection_members WHERE collection_id = :collectionId")
    suspend fun deleteMembersOf(collectionId: String)

    @Transaction
    suspend fun addMember(collectionId: String, prayerId: String, updatedAt: Long) {
        insertMembers(
            listOf(
                CollectionMemberEntity(
                    collectionId = collectionId,
                    prayerId = prayerId,
                    position = nextPositionIn(collectionId),
                ),
            ),
        )
        touchCollection(collectionId, updatedAt)
    }

    @Transaction
    suspend fun removeMember(collectionId: String, prayerId: String, updatedAt: Long) {
        deleteMember(collectionId, prayerId)
        touchCollection(collectionId, updatedAt)
    }

    @Query(
        "SELECT COALESCE(MAX(position) + 1, 0) FROM collection_members " +
            "WHERE collection_id = :collectionId",
    )
    suspend fun nextPositionIn(collectionId: String): Int

    @Query("DELETE FROM collection_members WHERE collection_id = :collectionId AND prayer_id = :prayerId")
    suspend fun deleteMember(collectionId: String, prayerId: String)

    @Query("UPDATE prayer_collections SET updated_at = :updatedAt WHERE id = :collectionId")
    suspend fun touchCollection(collectionId: String, updatedAt: Long)

    @Query(
        "UPDATE prayer_collections SET is_deleted = 1, updated_at = :deletedAt " +
            "WHERE id = :collectionId",
    )
    suspend fun markCollectionDeleted(collectionId: String, deletedAt: Long)
}
