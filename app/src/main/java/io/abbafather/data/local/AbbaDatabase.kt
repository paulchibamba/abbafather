package io.abbafather.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.abbafather.data.local.dao.PersonalPrayerDao
import io.abbafather.data.local.dao.PrayerCollectionDao
import io.abbafather.data.local.dao.PrayerDao
import io.abbafather.data.local.dao.SavedLineDao
import io.abbafather.data.local.entity.CollectionMemberEntity
import io.abbafather.data.local.entity.PersonalPrayerEntity
import io.abbafather.data.local.entity.PrayerCollectionEntity
import io.abbafather.data.local.entity.PrayerEntity
import io.abbafather.data.local.entity.PrayerLineEntity
import io.abbafather.data.local.entity.PrayerThemeEntity
import io.abbafather.data.local.entity.SavedLineEntity

/**
 * The single source of truth. Schemas are exported to `app/schemas/` and committed; every version
 * bump gets a real migration, because this database holds prayers people wrote.
 */
@Database(
    entities = [
        PrayerEntity::class,
        PrayerLineEntity::class,
        PrayerThemeEntity::class,
        SavedLineEntity::class,
        PersonalPrayerEntity::class,
        PrayerCollectionEntity::class,
        CollectionMemberEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(AbbaTypeConverters::class)
abstract class AbbaDatabase : RoomDatabase() {

    abstract fun prayerDao(): PrayerDao

    abstract fun savedLineDao(): SavedLineDao

    abstract fun personalPrayerDao(): PersonalPrayerDao

    abstract fun prayerCollectionDao(): PrayerCollectionDao

    companion object {
        const val NAME = "abba_father.db"
    }
}
