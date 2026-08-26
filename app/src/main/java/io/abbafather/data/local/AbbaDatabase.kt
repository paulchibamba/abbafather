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
import io.abbafather.data.local.entity.PrayerMovementEntity
import io.abbafather.data.local.entity.PrayerMovementThemeEntity
import io.abbafather.data.local.entity.PrayerScriptureEntity
import io.abbafather.data.local.entity.PrayerTagEntity
import io.abbafather.data.local.entity.SavedLineEntity

/**
 * The single source of truth. Schemas are exported to `app/schemas/` and committed; every version
 * bump gets a real migration, because this database holds prayers people wrote.
 */
@Database(
    entities = [
        PrayerEntity::class,
        PrayerMovementEntity::class,
        PrayerLineEntity::class,
        PrayerMovementThemeEntity::class,
        PrayerScriptureEntity::class,
        PrayerTagEntity::class,
        SavedLineEntity::class,
        PersonalPrayerEntity::class,
        PrayerCollectionEntity::class,
        CollectionMemberEntity::class,
    ],
    version = 2,
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
