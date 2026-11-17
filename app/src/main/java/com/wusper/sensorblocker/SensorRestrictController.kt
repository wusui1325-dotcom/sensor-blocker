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
    @Volatile var fallbackAttemptedForDesired: Boolean = false
        private set

    private var lastDesired: DesiredState? = null

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

    fun apply(desired: DesiredState, force: Boolean = false): Boolean {
        lastError = null
        if (!ShizukuClient.isReady()) {
            lastError = "shizuku-not-ready"
            return false
        }
        val firstForDesired = desired != lastDesired
        if (firstForDesired) {
            fallbackAttemptedForDesired = false
            lastDesired = desired
        }

        if (ApplyLoopPolicy.shouldRevertPrivacyFallback(desired, usedPrivacyFallback)) {
            revertPrivacyFallback()
        }

        val wantConfirm = ApplyLoopPolicy.shouldConfirmMode(
            force = force,
            firstApplyForDesired = firstForDesired,
            commandFailed = false
        )
        val command = when (desired) {
            is DesiredState.Normal ->
                if (wantConfirm) SensorCommands.enableAndQueryMode() else SensorCommands.enable()
            is DesiredState.RestrictTo -> {
                if (!SensorCommands.isSafePackageName(desired.packageName)) {
                    lastError = "unsafe-package"
                    return false
                }
                if (wantConfirm) {
                    SensorCommands.restrictAndQueryMode(desired.packageName)
                } else {
                    SensorCommands.restrict(desired.packageName)
                }
            }
        }
        val result = ShizukuClient.exec(command)
        val confirm = ApplyLoopPolicy.shouldConfirmMode(
            force = force,
            firstApplyForDesired = firstForDesired,
            commandFailed = !result.isSuccess
        )
        var mode: SensorServiceMode = SensorServiceModeParser.parse(result.output)
        if (confirm) {
            if (mode is SensorServiceMode.Unknown) {
                mode = queryMode()
            } else {
                lastMode = mode
            }
        } else if (result.isSuccess && lastMode !is SensorServiceMode.Unknown) {
            mode = when (desired) {
                is DesiredState.Normal -> SensorServiceMode.Normal
                is DesiredState.RestrictTo -> SensorServiceMode.Restricted(desired.packageName)
            }
            lastMode = mode
        }

        val ok = matches(desired, mode)
        if (ok) {
            if (desired is DesiredState.Normal) {
                usedPrivacyFallback = false
            }
            return true
        }

        lastError = "restrict-unconfirmed exit=${result.exitCode}"
        Log.w(TAG, "SensorService command did not reach the expected mode: $lastError mode=$mode")

        if (ApplyLoopPolicy.shouldAttemptPrivacyFallback(
                desired,
                alreadyEngaged = usedPrivacyFallback,
                alreadyAttemptedForDesired = fallbackAttemptedForDesired && !force
            )
        ) {
            val fallbacks = SensorCommands.fallbackPrivacyCommands(true, Build.VERSION.SDK_INT)
            for (cmd in fallbacks) {
                val r = ShizukuClient.exec(cmd)
                if (r.isSuccess) {
                    usedPrivacyFallback = true
                    lastError = "privacy-fallback-only"
                    break
                }
            }
            fallbackAttemptedForDesired = true
        }
        return false
    }

    private fun revertPrivacyFallback() {
        val fallbacks = SensorCommands.fallbackPrivacyCommands(false, Build.VERSION.SDK_INT)
        for (cmd in fallbacks) {
            try {
                ShizukuClient.exec(cmd)
            } catch (_: Exception) {
                // Best effort: still clear the flag after attempting all codes.
            }
        }
        usedPrivacyFallback = false
        lastError = null
    }

    fun matches(desired: DesiredState, mode: SensorServiceMode): Boolean {
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
