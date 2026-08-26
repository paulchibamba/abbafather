package io.abbafather.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.abbafather.domain.model.PrayerPart
import io.abbafather.domain.model.PrayerTag
import io.abbafather.domain.model.PrayerVoice

/**
 * A catalogue prayer. Seeded from the bundled asset and never edited by the reader, so it carries no
 * soft-delete flag; [lastOpenedAt] is the one column the app writes back.
 *
 * Provenance is stored on the row rather than derived, because it is a fact about this prayer's
 * source rather than about the catalogue: a second collection added later brings its own.
 */
@Entity(tableName = "prayers")
data class PrayerEntity(
    @PrimaryKey val id: String,
    val title: String,
    val part: PrayerPart,
    val voice: PrayerVoice,
    @ColumnInfo(name = "catalogue_position") val cataloguePosition: Int,
    @ColumnInfo(name = "original_title") val originalTitle: String,
    @ColumnInfo(name = "original_author") val originalAuthor: String,
    @ColumnInfo(name = "original_source") val originalSource: String,
    @ColumnInfo(name = "original_publication_date") val originalPublicationDate: String,
    @ColumnInfo(name = "copyright_status") val copyrightStatus: String,
    @ColumnInfo(name = "adaptation_type") val adaptationType: String,
    @ColumnInfo(name = "adaptation_note") val adaptationNote: String,
    @ColumnInfo(name = "last_opened_at") val lastOpenedAt: Long? = null,
)

/**
 * One turn of a prayer. The movement is where a session breathes and where the scripture behind the
 * praying is attached.
 */
@Entity(
    tableName = "prayer_movements",
    primaryKeys = ["prayer_id", "movement_index"],
    foreignKeys = [
        ForeignKey(
            entity = PrayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["prayer_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PrayerMovementEntity(
    @ColumnInfo(name = "prayer_id") val prayerId: String,
    @ColumnInfo(name = "movement_index") val movementIndex: Int,
    val heading: String,
)

/**
 * One line of a catalogue prayer, and which movement it belongs to. The break positions are content:
 * they are computed once when the catalogue is built, so a line index means the same thing for as
 * long as a kept line points at it.
 */
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
    @ColumnInfo(name = "movement_index") val movementIndex: Int,
    val text: String,
)

/** What a movement holds theologically, in the words the catalogue states it. */
@Entity(
    tableName = "prayer_movement_themes",
    primaryKeys = ["prayer_id", "movement_index", "position"],
    foreignKeys = [
        ForeignKey(
            entity = PrayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["prayer_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PrayerMovementThemeEntity(
    @ColumnInfo(name = "prayer_id") val prayerId: String,
    @ColumnInfo(name = "movement_index") val movementIndex: Int,
    val position: Int,
    val text: String,
)

/**
 * A passage a movement rests on. The reference and translation only — the verse text itself is never
 * stored or shipped, which is both a licensing line and the right one: the Bible is the reader's own.
 */
@Entity(
    tableName = "prayer_scriptures",
    primaryKeys = ["prayer_id", "movement_index", "position"],
    foreignKeys = [
        ForeignKey(
            entity = PrayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["prayer_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PrayerScriptureEntity(
    @ColumnInfo(name = "prayer_id") val prayerId: String,
    @ColumnInfo(name = "movement_index") val movementIndex: Int,
    val position: Int,
    val reference: String,
    val translation: String,
    val connection: String,
)

/**
 * A tag attached to a catalogue prayer. A table rather than a converted column because the Library
 * filters on it, and filtering belongs in the query.
 */
@Entity(
    tableName = "prayer_tags",
    primaryKeys = ["prayer_id", "tag"],
    indices = [Index("tag")],
    foreignKeys = [
        ForeignKey(
            entity = PrayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["prayer_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PrayerTagEntity(
    @ColumnInfo(name = "prayer_id") val prayerId: String,
    val tag: PrayerTag,
)
