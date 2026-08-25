package io.abbafather.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.abbafather.domain.model.PrayerGroup
import io.abbafather.domain.model.PrayerKind
import io.abbafather.domain.model.PrayerTheme

/**
 * A catalogue prayer. Seeded from the bundled asset and never edited by the reader, so it carries no
 * soft-delete flag; [lastOpenedAt] is the one column the app writes back.
 */
@Entity(tableName = "prayers")
data class PrayerEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val kind: PrayerKind,
    @ColumnInfo(name = "prayer_group") val group: PrayerGroup,
    @ColumnInfo(name = "breathing_pause_after_line") val breathingPauseAfterLine: Int?,
    @ColumnInfo(name = "catalogue_position") val cataloguePosition: Int,
    @ColumnInfo(name = "last_opened_at") val lastOpenedAt: Long? = null,
)

/** One line of a catalogue prayer. The break positions are content — see `Prayer.lines`. */
@Entity(
    tableName = "prayer_lines",
    primaryKeys = ["prayer_id", "line_index"],
    foreignKeys = [
        ForeignKey(
            entity = PrayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["prayer_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PrayerLineEntity(
    @ColumnInfo(name = "prayer_id") val prayerId: String,
    @ColumnInfo(name = "line_index") val lineIndex: Int,
    val text: String,
)

/**
 * A theme attached to a catalogue prayer. A table rather than a converted column because the Library
 * filters on it, and filtering belongs in the query.
 */
@Entity(
    tableName = "prayer_themes",
    primaryKeys = ["prayer_id", "theme"],
    indices = [Index("theme")],
    foreignKeys = [
        ForeignKey(
            entity = PrayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["prayer_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PrayerThemeEntity(
    @ColumnInfo(name = "prayer_id") val prayerId: String,
    val theme: PrayerTheme,
)
