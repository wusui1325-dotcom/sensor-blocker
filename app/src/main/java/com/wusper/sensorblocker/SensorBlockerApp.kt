package com.wusper.sensorblocker

import android.app.Application
import android.content.Context

class SensorBlockerApp : Application() {
    @Volatile
    private var isMainAppProcess = true

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        isMainAppProcess = ProcessIdentity.isDefaultAppProcess(
            ProcessIdentity.currentProcessName(),
            base.packageName
        )
    }

    override fun onCreate() {
        super.onCreate()
        if (!isMainAppProcess) {
            // UserService / secondary process: do not refresh catalogs,
            // init Shizuku, or start the monitor service.
            return
        }
        ForegroundCatalog.refresh(this)
        ShizukuClient.init(this)
        ShizukuClient.addListener {
            maybeResumeMonitoring()
        }
        maybeResumeMonitoring()
    }

    private fun maybeResumeMonitoring() {
        if (!AppPreferences.isMonitoringEnabled(this)) return
        if (SensorMonitorService.isRunning) return
        if (!ShizukuClient.isReady()) {
            ShizukuClient.bindIfPossible()
            return
        }
        SensorMonitorService.startSafely(this)
    }
}
