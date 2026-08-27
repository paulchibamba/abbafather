package io.abbafather.di

import android.content.Context
import android.content.pm.PackageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.abbafather.core.common.AppInfo
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppInfoModule {

    /**
     * The package manager is asked rather than `BuildConfig` read, so the name shown is the one the
     * installed package actually carries.
     */
    @Provides
    @Singleton
    fun provideAppInfo(@ApplicationContext context: Context): AppInfo {
        val versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
        return AppInfo(versionName = versionName.orEmpty())
    }
}
