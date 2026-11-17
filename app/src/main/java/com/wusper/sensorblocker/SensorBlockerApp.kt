package com.wusper.sensorblocker

import android.app.Application

class SensorBlockerApp : Application() {
    override fun onCreate() {
        super.onCreate()
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
