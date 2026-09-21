package bob.colbaskin.gidromonitor.features.analysis.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import bob.colbaskin.gidromonitor.features.analysis.data.AnalysisRepositoryRemoteImpl
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

class AreaSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = EntryPointAccessors.fromApplication(
            applicationContext,
            AreaSyncWorkerEntryPoint::class.java
        ).repository()
        return if (repository.syncPendingAndRefresh()) Result.success() else Result.retry()
    }

    companion object {
        const val IMMEDIATE_WORK = "area-offline-sync"
        const val PERIODIC_WORK = "area-periodic-refresh"
        fun networkConstraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AreaSyncWorkerEntryPoint {
    fun repository(): AnalysisRepositoryRemoteImpl
}

class AreaSyncScheduler(private val workManager: WorkManager) {
    fun scheduleImmediate() {
        val request = OneTimeWorkRequestBuilder<AreaSyncWorker>()
            .setConstraints(AreaSyncWorker.networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(AreaSyncWorker.IMMEDIATE_WORK, ExistingWorkPolicy.KEEP, request)
    }

    fun schedulePeriodicRefresh() {
        val request = PeriodicWorkRequestBuilder<AreaSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(AreaSyncWorker.networkConstraints())
            .build()
        workManager.enqueueUniquePeriodicWork(
            AreaSyncWorker.PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
