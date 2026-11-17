package com.wusper.sensorblocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class WindowChangeAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }
        val pkg = event.packageName?.toString()
            ?: rootInActiveWindow?.packageName?.toString()
        ForegroundTracker.update(pkg, "accessibility")
    }

    override fun onInterrupt() = Unit

    companion object {
        fun isEnabled(context: android.content.Context): Boolean {
            val expected = "${context.packageName}/${WindowChangeAccessibilityService::class.java.name}"
            val enabled = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.split(':', ';').any { it.equals(expected, ignoreCase = true) }
        }
    }
}
