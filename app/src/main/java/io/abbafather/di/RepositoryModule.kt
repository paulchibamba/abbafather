package io.abbafather.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.abbafather.data.repository.AssetLicenceRepository
import io.abbafather.data.repository.DataStoreSettingsRepository
import io.abbafather.data.repository.OfflinePersonalPrayerRepository
import io.abbafather.data.repository.OfflinePrayerRepository
import io.abbafather.data.repository.OfflineSavedLineRepository
import io.abbafather.domain.repository.LicenceRepository
import io.abbafather.domain.repository.PersonalPrayerRepository
import io.abbafather.domain.repository.PrayerRepository
import io.abbafather.domain.repository.SavedLineRepository
import io.abbafather.domain.repository.SettingsRepository
import javax.inject.Singleton

/** The one place the domain's interfaces meet their Room-backed implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPrayerRepository(repository: OfflinePrayerRepository): PrayerRepository

    @Binds
    @Singleton
    abstract fun bindSavedLineRepository(repository: OfflineSavedLineRepository): SavedLineRepository

    @Binds
    @Singleton
    abstract fun bindPersonalPrayerRepository(
        repository: OfflinePersonalPrayerRepository,
    ): PersonalPrayerRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(repository: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindLicenceRepository(repository: AssetLicenceRepository): LicenceRepository
}
