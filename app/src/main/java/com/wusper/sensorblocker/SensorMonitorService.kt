package com.wusper.sensorblocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class SensorMonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val controller = SensorRestrictController()
    private var running = false
    private var lastDesired: DesiredState? = null
    private var lastApplyOk = false

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == AppPreferences.KEY_BLOCKED ||
            key == AppPreferences.KEY_ALLOWED ||
            key == AppPreferences.KEY_MODE
        ) {
            lastApplyOk = false
            scheduleApply("prefs")
        }
    }

    private val foregroundListener: (String?) -> Unit = {
        scheduleApply("foreground")
    }

    private val shizukuListener: () -> Unit = {
        lastApplyOk = false
        scheduleApply("shizuku")
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            executor.execute { refreshForegroundFallbacks() }
            val delay = if (WindowChangeAccessibilityService.isEnabled(this@SensorMonitorService)) {
                POLL_WITH_ACCESSIBILITY_MS
            } else {
                POLL_WITHOUT_ACCESSIBILITY_MS
            }
            handler.postDelayed(this, delay)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        AppPreferences.prefs(this).registerOnSharedPreferenceChangeListener(prefListener)
        ForegroundTracker.addListener(foregroundListener)
        ShizukuClient.addListener(shizukuListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            AppPreferences.setMonitoringEnabled(this, false)
            stopSelf()
            return START_NOT_STICKY
        }
        startAsForeground()
        if (!running) {
            running = true
            isRunning = true
            AppPreferences.setMonitoringEnabled(this, true)
            handler.post(pollRunnable)
        }
        lastApplyOk = false
        scheduleApply("start")
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        AppPreferences.prefs(this).unregisterOnSharedPreferenceChangeListener(prefListener)
        ForegroundTracker.removeListener(foregroundListener)
        ShizukuClient.removeListener(shizukuListener)
        executor.execute {
            if (ShizukuClient.isReady()) {
                controller.apply(DesiredState.Normal)
            }
        }
        executor.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val notification = buildNotification(getString(R.string.notif_monitoring))
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun refreshForegroundFallbacks() {
        if (ShizukuClient.isReady()) {
            controller.getFocusedPackage()?.let { ForegroundTracker.update(it, "shizuku") }
        }
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usm != null) {
            UsageForegroundQuery.latestResumedPackage(usm)?.let {
                // UsageEvents is a supplement; do not overwrite a very recent accessibility event.
                val recent = System.currentTimeMillis() - ForegroundTracker.updatedAt < 1500 &&
                    ForegroundTracker.source == "accessibility"
                if (!recent) {
                    ForegroundTracker.update(it, "usage")
                }
            }
        }
        applyPolicy("poll")
        publishState()
    }

    private fun scheduleApply(reason: String) {
        executor.execute { applyPolicy(reason) }
    }

    private fun applyPolicy(reason: String) {
        if (!running && reason != "start") return
        if (!ShizukuClient.isReady()) {
            ShizukuClient.bindIfPossible()
            handler.post { updateNotification(getString(R.string.notif_waiting_shizuku)) }
            publishState()
            return
        }
        val desired = BlockPolicy.decide(
            mode = AppPreferences.blockMode(this),
            foregroundPackage = ForegroundTracker.current,
            allowlist = AppPreferences.allowedPackages(this),
            blocklist = AppPreferences.blockedPackages(this),
            selfPackage = packageName
        )
        if (desired == lastDesired && lastApplyOk) {
            publishState()
            return
        }
        val ok = controller.apply(desired)
        lastDesired = desired
        lastApplyOk = ok
        val text = when (val mode = controller.lastMode) {
            is SensorServiceMode.Restricted -> getString(
                R.string.notif_restricted,
                mode.packageName ?: getString(R.string.app_name)
            )
            is SensorServiceMode.Normal -> getString(R.string.notif_normal)
            else -> getString(R.string.notif_monitoring)
        }
        handler.post { updateNotification(text) }
        publishState()
    }

    private fun publishState() {
        MonitorStateHub.update(
            MonitorState(
                monitoring = true,
                shizukuReady = ShizukuClient.isReady(),
                shizukuBound = ShizukuClient.isBinderAlive(),
                shizukuPermission = ShizukuClient.hasPermission(),
                sensorMode = controller.lastMode,
                foregroundPackage = ForegroundTracker.current,
                usedPrivacyFallback = controller.usedPrivacyFallback
            )
        )
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val launch = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentIntent(launch)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        const val ACTION_START = "com.wusper.sensorblocker.START"
        const val ACTION_STOP = "com.wusper.sensorblocker.STOP"
        const val CHANNEL_ID = "sensor_monitor"
        const val NOTIFICATION_ID = 1
        private const val POLL_WITHOUT_ACCESSIBILITY_MS = 250L
        private const val POLL_WITH_ACCESSIBILITY_MS = 1000L

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, SensorMonitorService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            AppPreferences.setMonitoringEnabled(context, false)
            context.stopService(Intent(context, SensorMonitorService::class.java).setAction(ACTION_STOP))
        }
    }
}
