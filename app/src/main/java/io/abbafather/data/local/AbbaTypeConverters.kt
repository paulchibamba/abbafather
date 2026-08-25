package io.abbafather.data.local

import androidx.room.TypeConverter
import io.abbafather.domain.model.PrayerGroup
import io.abbafather.domain.model.PrayerKind
import io.abbafather.domain.model.PrayerTheme

/**
 * Enums are stored by `name`, never by ordinal, so reordering an enum cannot silently rewrite what
 * rows mean. A theme set that is only ever displayed is stored as one comma-joined column; the
 * catalogue's themes, which are filtered on, live in `prayer_themes` instead.
 */
class AbbaTypeConverters {

    @TypeConverter
    fun prayerKindToName(kind: PrayerKind): String = kind.name

    @TypeConverter
    fun prayerKindFromName(name: String): PrayerKind = PrayerKind.valueOf(name)

    @TypeConverter
    fun prayerGroupToName(group: PrayerGroup): String = group.name

    @TypeConverter
    fun prayerGroupFromName(name: String): PrayerGroup = PrayerGroup.valueOf(name)

    @TypeConverter
    fun prayerThemeToName(theme: PrayerTheme): String = theme.name

    @TypeConverter
    fun prayerThemeFromName(name: String): PrayerTheme = PrayerTheme.valueOf(name)

    @TypeConverter
    fun prayerThemesToNames(themes: Set<PrayerTheme>): String = themes.joinToString(SEPARATOR) { it.name }

    @TypeConverter
    fun prayerThemesFromNames(names: String): Set<PrayerTheme> =
        if (names.isEmpty()) {
            emptySet()
        } else {
            names.split(SEPARATOR).mapNotNullTo(mutableSetOf()) { name ->
                PrayerTheme.entries.firstOrNull { it.name == name }
            }
        }

    private companion object {
        const val SEPARATOR = ","
    }
}
