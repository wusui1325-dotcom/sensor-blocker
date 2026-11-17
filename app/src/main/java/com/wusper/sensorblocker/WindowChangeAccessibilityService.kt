package com.wusper.sensorblocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class WindowChangeAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        // TYPE_WINDOWS_CHANGED often has a null package; falling back to
        // rootInActiveWindow can pin the wrong app. Only trust a real
        // package from WINDOW_STATE_CHANGED (or a non-null package on
        // other window events).
        val pkg = event.packageName?.toString()
        if (pkg.isNullOrBlank()) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            ForegroundTracker.update(pkg, ForegroundSourcePriority.ACCESSIBILITY)
        }
    }

    override fun onInterrupt() = Unit

    companion object {
        fun isEnabled(context: android.content.Context): Boolean {
            val enabled = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return AccessibilityComponent.isListed(
                enabled,
                context.packageName,
                WindowChangeAccessibilityService::class.java.name
            )
        }
    }
}
