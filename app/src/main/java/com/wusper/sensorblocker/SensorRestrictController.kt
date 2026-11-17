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
        val dumped = dumpOrNull(SensorCommands.queryDumpArgs(), stopAtMode = true)
        var mode = SensorServiceModeParser.parse(dumped ?: "")
        if (mode is SensorServiceMode.Unknown && ShizukuClient.shell != null) {
            val grepped = ShizukuClient.exec(SensorCommands.dumpModeLine())
            mode = SensorServiceModeParser.parse(grepped.output)
            if (mode is SensorServiceMode.Unknown) {
                val full = ShizukuClient.exec(SensorCommands.dump())
                mode = SensorServiceModeParser.parse(full.output)
            }
        }
        lastMode = mode
        return mode
    }

    fun getFocusedPackage(): String? {
        // Window/activity dumpsys stays a UserService fallback only.
        if (ShizukuClient.shell == null) return null
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
        if (desired is DesiredState.RestrictTo && !SensorCommands.isSafePackageName(desired.packageName)) {
            lastError = "unsafe-package"
            return false
        }

        val args = when (desired) {
            is DesiredState.Normal -> SensorCommands.enableDumpArgs()
            is DesiredState.RestrictTo -> SensorCommands.restrictDumpArgs(desired.packageName)
        }
        val dumpText = dumpOrNull(args, stopAtMode = false)
        var dumpFailed = dumpText == null
        var resultOutput = dumpText.orEmpty()
        var commandFailed = dumpFailed

        if (dumpFailed && ShizukuClient.shell != null) {
            val command = when (desired) {
                is DesiredState.Normal ->
                    if (wantConfirm) SensorCommands.enableAndQueryMode() else SensorCommands.enable()
                is DesiredState.RestrictTo ->
                    if (wantConfirm) {
                        SensorCommands.restrictAndQueryMode(desired.packageName)
                    } else {
                        SensorCommands.restrict(desired.packageName)
                    }
            }
            val result = ShizukuClient.exec(command)
            resultOutput = result.output
            commandFailed = !result.isSuccess
        }

        val confirm = ApplyLoopPolicy.shouldConfirmMode(
            force = force,
            firstApplyForDesired = firstForDesired,
            commandFailed = commandFailed
        )
        var mode: SensorServiceMode = SensorServiceModeParser.parse(resultOutput)
        if (confirm) {
            if (mode is SensorServiceMode.Unknown) {
                mode = queryMode()
            } else {
                lastMode = mode
            }
        } else if (!commandFailed && lastMode !is SensorServiceMode.Unknown) {
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

        lastError = "restrict-unconfirmed"
        Log.w(TAG, "SensorService command did not reach the expected mode: $lastError mode=$mode")

        if (ApplyLoopPolicy.shouldAttemptPrivacyFallback(
                desired,
                alreadyEngaged = usedPrivacyFallback,
                alreadyAttemptedForDesired = fallbackAttemptedForDesired && !force
            )
        ) {
            val fallbacks = SensorCommands.fallbackPrivacyCommands(true, Build.VERSION.SDK_INT)
            for (cmd in fallbacks) {
                if (ShizukuClient.shell == null) break
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

    private fun dumpOrNull(args: Array<String>, stopAtMode: Boolean): String? {
        return try {
            ShizukuClient.dumpSensorservice(args, stopAtMode = stopAtMode)
        } catch (t: Exception) {
            Log.w(TAG, "sensorservice dump failed", t)
            null
        }
    }

    private fun revertPrivacyFallback() {
        if (ShizukuClient.shell == null) {
            usedPrivacyFallback = false
            lastError = null
            return
        }
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
