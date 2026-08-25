package io.abbafather.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.abbafather.domain.model.PrayerTheme

/** A prayer the reader wrote. [body] is stored exactly as typed, line breaks included. */
@Entity(tableName = "personal_prayers")
data class PersonalPrayerEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val themes: Set<PrayerTheme>,
    @ColumnInfo(name = "seeded_from_saved_line_id") val seededFromSavedLineId: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
)
