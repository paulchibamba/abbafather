package io.abbafather.data.local

import androidx.room.TypeConverter
import io.abbafather.domain.model.PrayerPart
import io.abbafather.domain.model.PrayerTag
import io.abbafather.domain.model.PrayerVoice

/**
 * Enums are stored by `name`, never by ordinal, so reordering an enum cannot silently rewrite what
 * rows mean. A tag set that is only ever displayed is stored as one comma-joined column; the
 * catalogue's tags, which are filtered on, live in `prayer_tags` instead.
 */
class AbbaTypeConverters {

    @TypeConverter
    fun prayerPartToName(part: PrayerPart): String = part.name

    @TypeConverter
    fun prayerPartFromName(name: String): PrayerPart = PrayerPart.valueOf(name)

    @TypeConverter
    fun prayerVoiceToName(voice: PrayerVoice): String = voice.name

    @TypeConverter
    fun prayerVoiceFromName(name: String): PrayerVoice = PrayerVoice.valueOf(name)

    @TypeConverter
    fun prayerTagToName(tag: PrayerTag): String = tag.name

    @TypeConverter
    fun prayerTagFromName(name: String): PrayerTag = PrayerTag.valueOf(name)

    @TypeConverter
    fun prayerTagsToNames(tags: Set<PrayerTag>): String = tags.joinToString(SEPARATOR) { it.name }

    /**
     * Lenient on the way in: a name this build does not know is dropped rather than thrown. The
     * catalogue's vocabulary is closed at build time, but a reader's own kept lines were tagged by
     * whatever build wrote them, and losing one tag should never cost them the line.
     */
    @TypeConverter
    fun prayerTagsFromNames(names: String): Set<PrayerTag> =
        if (names.isEmpty()) {
            emptySet()
        } else {
            names.split(SEPARATOR).mapNotNullTo(mutableSetOf()) { name ->
                PrayerTag.entries.firstOrNull { it.name == name }
            }
        }

    private companion object {
        const val SEPARATOR = ","
    }
}
