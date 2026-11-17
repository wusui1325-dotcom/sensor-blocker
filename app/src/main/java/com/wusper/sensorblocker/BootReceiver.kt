package com.wusper.sensorblocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        if (!AppPreferences.isMonitoringEnabled(context)) return
        ForegroundCatalog.refresh(context)
        if (!SensorMonitorService.startSafely(context)) {
            Log.w(TAG, "Unable to start monitor after $action")
        }
    }

    companion object {
        private const val TAG = "SensorBlockerBoot"
    }
}
