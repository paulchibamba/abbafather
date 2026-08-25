package io.abbafather.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A reader-made grouping of catalogue prayers. */
@Entity(tableName = "prayer_collections")
data class PrayerCollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
)

/** Membership of a collection, in the order the reader put it in. */
@Entity(
    tableName = "collection_members",
    primaryKeys = ["collection_id", "prayer_id"],
    indices = [Index("prayer_id")],
    foreignKeys = [
        ForeignKey(
            entity = PrayerCollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PrayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["prayer_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CollectionMemberEntity(
    @ColumnInfo(name = "collection_id") val collectionId: String,
    @ColumnInfo(name = "prayer_id") val prayerId: String,
    val position: Int,
)
