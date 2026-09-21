package bob.colbaskin.gidromonitor

import android.app.Application
import bob.colbaskin.gidromonitor.features.analysis.data.sync.AreaSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre
import javax.inject.Inject

@HiltAndroidApp
class GidroMonitorApplication : Application() {
    @Inject lateinit var areaSyncScheduler: AreaSyncScheduler

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        areaSyncScheduler.schedulePeriodicRefresh()
    }
}
