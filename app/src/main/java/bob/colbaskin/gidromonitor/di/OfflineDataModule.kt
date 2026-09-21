package bob.colbaskin.gidromonitor.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import bob.colbaskin.gidromonitor.features.analysis.data.local.OfflineAreaDatabase
import bob.colbaskin.gidromonitor.features.analysis.data.local.offlineAreaMigration1To2
import bob.colbaskin.gidromonitor.features.analysis.data.local.offlineAreaMigration2To3
import bob.colbaskin.gidromonitor.features.analysis.data.sync.AreaSyncScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OfflineDataModule {
    @Provides
    @Singleton
    fun provideOfflineAreaDatabase(@ApplicationContext context: Context): OfflineAreaDatabase =
        Room.databaseBuilder(context, OfflineAreaDatabase::class.java, "gidromonitor-offline.db")
            .addMigrations(offlineAreaMigration1To2, offlineAreaMigration2To3)
            .build()

    @Provides
    @Singleton
    fun provideAreaSyncScheduler(@ApplicationContext context: Context): AreaSyncScheduler =
        AreaSyncScheduler(WorkManager.getInstance(context))
}
