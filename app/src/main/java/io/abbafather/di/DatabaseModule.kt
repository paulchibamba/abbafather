package io.abbafather.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.abbafather.core.common.ApplicationScope
import io.abbafather.core.common.IoDispatcher
import io.abbafather.data.local.AbbaDatabase
import io.abbafather.data.local.dao.PersonalPrayerDao
import io.abbafather.data.local.dao.PrayerCollectionDao
import io.abbafather.data.local.dao.PrayerDao
import io.abbafather.data.local.dao.SavedLineDao
import io.abbafather.data.local.seed.CatalogueSeeder
import io.abbafather.data.preferences.SettingsDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * The seeder needs DAOs from the database it is being handed to, so it arrives as a [Provider]
     * and is only resolved inside the callback — by which point the database exists.
     *
     * There is deliberately no `fallbackToDestructiveMigration`: this database holds prayers people
     * wrote, and a missing migration must fail loudly rather than quietly empty their Saved screen.
     */
    @Provides
    @Singleton
    fun provideAbbaDatabase(
        @ApplicationContext context: Context,
        @ApplicationScope applicationScope: CoroutineScope,
        seeder: Provider<CatalogueSeeder>,
    ): AbbaDatabase = Room.databaseBuilder(context, AbbaDatabase::class.java, AbbaDatabase.NAME)
        .addCallback(
            object : RoomDatabase.Callback() {
                override fun onCreate(connection: SQLiteConnection) {
                    super.onCreate(connection)
                    applicationScope.launch { seeder.get().seedCatalogueIfEmpty() }
                }
            },
        )
        .build()

    @Provides
    fun providePrayerDao(database: AbbaDatabase): PrayerDao = database.prayerDao()

    @Provides
    fun provideSavedLineDao(database: AbbaDatabase): SavedLineDao = database.savedLineDao()

    @Provides
    fun providePersonalPrayerDao(database: AbbaDatabase): PersonalPrayerDao =
        database.personalPrayerDao()

    @Provides
    fun providePrayerCollectionDao(database: AbbaDatabase): PrayerCollectionDao =
        database.prayerCollectionDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope applicationScope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(applicationScope.coroutineContext + ioDispatcher),
        produceFile = { context.preferencesDataStoreFile(SettingsDataStore.NAME) },
    )
}
