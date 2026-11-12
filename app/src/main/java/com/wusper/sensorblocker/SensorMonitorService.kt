package com.wusper.sensorblocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class SensorMonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private val blockedPackages = mutableSetOf<String>()
    private var lastTopPackage: String? = null
    private var sensorsCurrentlyOff = false

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            checkForegroundApp()
            handler.postDelayed(this, 800) // faster poll for precision
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        loadBlockedPackages()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running) {
            running = true
            startForeground(1, buildNotification("監控中…"))
            handler.post(pollRunnable)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(pollRunnable)
        if (sensorsCurrentlyOff) {
            ShizukuSensorController.setSensorPrivacy(false)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun loadBlockedPackages() {
        val prefs = getSharedPreferences("sensor_blocker", MODE_PRIVATE)
        blockedPackages.clear()
        blockedPackages.addAll(prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet())
    }

    private fun checkForegroundApp() {
        // Prefer precise Shizuku dumpsys
        var top = ShizukuSensorController.getFocusedPackage()
        if (top == null) {
            // Fallback to UsageStats
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val start = end - 5000
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end)
            top = stats.maxByOrNull { it.lastTimeUsed }?.packageName
        }
        if (top == lastTopPackage) return
        lastTopPackage = top

        val shouldBlock = top != null && blockedPackages.contains(top)
        if (shouldBlock && !sensorsCurrentlyOff) {
            ShizukuSensorController.setSensorPrivacy(true)
            sensorsCurrentlyOff = true
            updateNotification("已封鎖：$top")
        } else if (!shouldBlock && sensorsCurrentlyOff) {
            ShizukuSensorController.setSensorPrivacy(false)
            sensorsCurrentlyOff = false
            updateNotification("感測器已恢復")
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "sensor_monitor",
            "Sensor Monitor",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, "sensor_monitor")
            .setContentTitle("Sensor Blocker")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(1, buildNotification(text))
    }
}
