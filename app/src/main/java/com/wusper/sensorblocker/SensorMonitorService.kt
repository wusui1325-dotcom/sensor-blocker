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
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SensorMonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val applyExecutor = Executors.newSingleThreadExecutor()
    private val pollExecutor = Executors.newSingleThreadExecutor()
    private val controller = SensorRestrictController()
    private var running = false
    private var lastDesired: DesiredState? = null
    private var lastApplyOk = false
    private var lastModeCheckAt = 0L

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
            val a11y = WindowChangeAccessibilityService.isEnabled(this@SensorMonitorService)
            pollExecutor.execute { refreshForegroundFallbacks(a11y) }
            val delay = if (a11y) POLL_WITH_ACCESSIBILITY_MS else POLL_WITHOUT_ACCESSIBILITY_MS
            handler.postDelayed(this, delay)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        ForegroundCatalog.refresh(this)
        AppPreferences.prefs(this).registerOnSharedPreferenceChangeListener(prefListener)
        ForegroundTracker.addListener(foregroundListener)
        ShizukuClient.addListener(shizukuListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            AppPreferences.setMonitoringEnabled(this, false)
            try {
                startAsForeground()
            } catch (_: Exception) {
                // Already in foreground or start not allowed; still stop.
            }
            running = false
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
        restoreSensorsAndUnbind()
        pollExecutor.shutdownNow()
        applyExecutor.shutdown()
        try {
            applyExecutor.awaitTermination(1, TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Apply NORMAL (and revert any privacy fallback) on the worker, wait
     * for it, then unbind the Shizuku UserService. Do not fire-and-forget
     * a task and immediately shut the executor down.
     */
    private fun restoreSensorsAndUnbind() {
        // Drop any in-flight dumpsys so restore is not stuck behind a poll.
        try {
            pollExecutor.shutdownNow()
        } catch (_: Exception) {
        }
        ShizukuClient.cancel()
        try {
            // Do not bind/unbind from this worker: onDestroy runs on the
            // main thread and waits here, so a worker that posts back to
            // main and waits would deadlock.
            if (ShizukuClient.isReady()) {
                val future = applyExecutor.submit<Boolean> {
                    controller.apply(DesiredState.Normal, force = true)
                }
                future.get(5, TimeUnit.SECONDS)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to restore sensors on stop", e)
        } finally {
            try {
                ShizukuClient.unbind()
            } catch (_: Throwable) {
            }
        }
    }

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

    private fun refreshForegroundFallbacks(accessibilityEnabled: Boolean) {
        if (!accessibilityEnabled) {
            if (ShizukuClient.isReady()) {
                controller.getFocusedPackage()?.let {
                    ForegroundTracker.update(it, ForegroundSourcePriority.SHIZUKU)
                }
            }
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usm != null) {
                UsageForegroundQuery.latestResumedPackage(usm)?.let {
                    ForegroundTracker.update(it, ForegroundSourcePriority.USAGE)
                }
            }
        }
        val now = SystemClock.elapsedRealtime()
        var force = false
        if (ShizukuClient.isReady() && now - lastModeCheckAt >= MODE_RECHECK_MS) {
            lastModeCheckAt = now
            if (lastDesired != null && lastApplyOk) {
                val parsed = controller.refreshModeLine()
                if (ModeConfirmPolicy.shouldReapplyAfterRecheck(lastDesired!!, parsed)) {
                    lastApplyOk = false
                    force = true
                }
            }
        }
        applyExecutor.execute {
            applyPolicy("poll", force = force)
            publishState()
        }
    }

    private fun scheduleApply(reason: String) {
        applyExecutor.execute { applyPolicy(reason) }
    }

    private fun applyPolicy(reason: String, force: Boolean = false) {
        if (!running && reason != "start") return
        if (!ShizukuClient.isReady()) {
            ShizukuClient.probeDumpPath()
            if (!ShizukuClient.isReady()) {
                // UserService is optional and must not block the dump path.
                ShizukuClient.bindIfPossible()
                handler.post { updateNotification(getString(R.string.notif_waiting_shizuku)) }
                publishState()
                return
            }
        }
        val desired = BlockPolicy.decide(
            mode = AppPreferences.blockMode(this),
            foregroundPackage = ForegroundTracker.current,
            allowlist = AppPreferences.allowedPackages(this),
            blocklist = AppPreferences.blockedPackages(this),
            selfPackage = packageName
        )
        if (ApplyLoopPolicy.shouldSkipReapply(
                desired,
                lastDesired,
                lastApplyOk,
                controller.fallbackAttemptedForDesired && lastDesired == desired,
                force
            )
        ) {
            publishState()
            return
        }
        val ok = controller.apply(desired, force = force)
        lastDesired = desired
        lastApplyOk = ok || (controller.usedPrivacyFallback && controller.fallbackAttemptedForDesired)
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
                monitoring = running || isRunning,
                shizukuReady = ShizukuClient.isReady(),
                shizukuBound = ShizukuClient.isBinderAlive(),
                shizukuPermission = ShizukuClient.hasPermission(),
                sensorMode = if (ShizukuClient.isReady()) controller.lastMode else SensorServiceMode.Unknown,
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
        val stopIntent = Intent(this, SensorMonitorService::class.java).setAction(ACTION_STOP)
        val stop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentIntent(launch)
            .addAction(0, getString(R.string.notif_action_stop), stop)
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
        private const val TAG = "SensorMonitor"
        private const val POLL_WITHOUT_ACCESSIBILITY_MS = 500L
        private const val POLL_WITH_ACCESSIBILITY_MS = 2_000L
        private const val MODE_RECHECK_MS = 10_000L

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            startSafely(context)
        }

        fun startSafely(context: Context): Boolean {
            val intent = Intent(context, SensorMonitorService::class.java).setAction(ACTION_START)
            return try {
                ContextCompat.startForegroundService(context, intent)
                true
            } catch (e: Exception) {
                Log.w(TAG, "startForegroundService failed", e)
                false
            }
        }

        /**
         * Deliver ACTION_STOP through onStartCommand (stopService() does not).
         * Restore happens in onDestroy, which waits for the worker.
         */
        fun stop(context: Context) {
            AppPreferences.setMonitoringEnabled(context, false)
            val intent = Intent(context, SensorMonitorService::class.java).setAction(ACTION_STOP)
            try {
                if (isRunning) {
                    context.startService(intent)
                } else {
                    context.stopService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "stop failed, falling back to stopService", e)
                try {
                    context.stopService(Intent(context, SensorMonitorService::class.java))
                } catch (_: Exception) {
                }
            }
        }
    }
}
