package io.abbafather.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.abbafather.domain.model.PrayerTheme

/**
 * A kept line. Reader-owned, so it carries a UUID id, both timestamps and a soft-delete flag; the
 * source columns are a copy, not a foreign key, so the line survives independently of the catalogue.
 */
@Entity(
    tableName = "saved_lines",
    indices = [Index("source_prayer_id", "source_line_index")],
)
data class SavedLineEntity(
    @PrimaryKey val id: String,
    val text: String,
    @ColumnInfo(name = "source_prayer_id") val sourcePrayerId: String?,
    @ColumnInfo(name = "source_prayer_title") val sourcePrayerTitle: String?,
    @ColumnInfo(name = "source_attribution") val sourceAttribution: String?,
    @ColumnInfo(name = "source_line_index") val sourceLineIndex: Int?,
    val themes: Set<PrayerTheme>,
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
)
