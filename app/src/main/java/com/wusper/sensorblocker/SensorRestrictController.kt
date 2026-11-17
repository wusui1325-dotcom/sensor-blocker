package com.wusper.sensorblocker

import android.os.Build
import android.util.Log

class SensorRestrictController {
    @Volatile var usedPrivacyFallback: Boolean = false
        private set
    @Volatile var lastMode: SensorServiceMode = SensorServiceMode.Unknown
        private set
    @Volatile var lastError: String? = null
        private set

    fun queryMode(): SensorServiceMode {
        if (!ShizukuClient.isReady()) {
            lastMode = SensorServiceMode.Unknown
            return lastMode
        }
        val grepped = ShizukuClient.exec(SensorCommands.dumpModeLine())
        var mode = SensorServiceModeParser.parse(grepped.output)
        if (mode is SensorServiceMode.Unknown) {
            val full = ShizukuClient.exec(SensorCommands.dump())
            mode = SensorServiceModeParser.parse(full.output)
        }
        lastMode = mode
        return mode
    }

    fun getFocusedPackage(): String? {
        if (!ShizukuClient.isReady()) return null
        val window = ShizukuClient.exec(SensorCommands.focusedWindow())
        FocusedPackageParser.parse(window.output)?.let { return it }
        val activity = ShizukuClient.exec(SensorCommands.resumedActivity())
        return FocusedPackageParser.parse(activity.output)
    }

    fun apply(desired: DesiredState): Boolean {
        usedPrivacyFallback = false
        lastError = null
        if (!ShizukuClient.isReady()) {
            lastError = "shizuku-not-ready"
            return false
        }
        val command = when (desired) {
            is DesiredState.Normal -> SensorCommands.enable()
            is DesiredState.RestrictTo -> {
                if (!SensorCommands.isSafePackageName(desired.packageName)) {
                    lastError = "unsafe-package"
                    return false
                }
                SensorCommands.restrict(desired.packageName)
            }
        }
        val result = ShizukuClient.exec(command)
        val mode = queryMode()
        val ok = matches(desired, mode)
        if (ok) return true

        lastError = "restrict-unconfirmed exit=${result.exitCode}"
        Log.w(TAG, "SensorService command did not reach the expected mode: $lastError mode=$mode")

        if (desired is DesiredState.RestrictTo) {
            // Last-resort global privacy toggle. Do not treat this as success
            // for motion sensors; it is mainly camera/mic on AOSP.
            val fallbacks = SensorCommands.fallbackPrivacyCommands(true, Build.VERSION.SDK_INT)
            for (cmd in fallbacks) {
                val r = ShizukuClient.exec(cmd)
                if (r.isSuccess) {
                    usedPrivacyFallback = true
                    lastError = "privacy-fallback-only"
                    break
                }
            }
        }
        return false
    }

    private fun matches(desired: DesiredState, mode: SensorServiceMode): Boolean {
        return when (desired) {
            is DesiredState.Normal -> mode is SensorServiceMode.Normal
            is DesiredState.RestrictTo ->
                mode is SensorServiceMode.Restricted &&
                    (mode.packageName == null || mode.packageName == desired.packageName)
        }
    }

    companion object {
        private const val TAG = "SensorRestrict"
    }
}
