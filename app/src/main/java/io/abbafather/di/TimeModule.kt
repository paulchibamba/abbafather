package io.abbafather.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.abbafather.domain.util.IdGenerator
import java.time.Clock
import java.util.UUID
import javax.inject.Singleton

/**
 * "Now" and "a new id" are injected rather than called, so a test can fix the clock and hand out
 * predictable ids instead of asserting against whatever the machine happened to be doing.
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun provideIdGenerator(): IdGenerator = IdGenerator { UUID.randomUUID().toString() }
}
