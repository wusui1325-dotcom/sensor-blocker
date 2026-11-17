package com.wusper.sensorblocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        if (!AppPreferences.isMonitoringEnabled(context)) return
        val start = Intent(context, SensorMonitorService::class.java)
            .setAction(SensorMonitorService.ACTION_START)
        ContextCompat.startForegroundService(context, start)
    }
}
