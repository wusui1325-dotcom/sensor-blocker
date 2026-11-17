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

    /**
     * Reads the Mode line without treating an unreadable dump as a
     * rollback of an already-applied command.
     */
    fun refreshModeLine(timeoutMs: Long = SensorServiceDumper.CONFIRM_TIMEOUT_MS): SensorServiceMode {
        val parsed = readModeLine(timeoutMs)
        if (parsed !is SensorServiceMode.Unknown) {
            lastMode = parsed
        }
        return parsed
    }

    fun queryMode(): SensorServiceMode {
        if (!ShizukuClient.isReady() && lastMode is SensorServiceMode.Unknown) {
            return lastMode
        }
        refreshModeLine()
        return lastMode
    }

    private fun readModeLine(timeoutMs: Long): SensorServiceMode {
        if (!ShizukuClient.isReady()) return SensorServiceMode.Unknown
        val dumped = dumpOrNull(
            SensorCommands.queryDumpArgs(),
            stopAtMode = true,
            timeoutMs = timeoutMs
        )
        var mode = SensorServiceModeParser.parse(dumped ?: "")
        if (mode is SensorServiceMode.Unknown && ShizukuClient.shell != null) {
            val grepped = ShizukuClient.exec(SensorCommands.dumpModeLine())
            mode = SensorServiceModeParser.parse(grepped.output)
        }
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
        var resultOutput = dumpText.orEmpty()
        var commandFailed = dumpText == null

        if (commandFailed && ShizukuClient.shell != null) {
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

        var parsed = SensorServiceModeParser.parse(resultOutput)
        if (!commandFailed && parsed is SensorServiceMode.Unknown && wantConfirm) {
            // Best-effort Mode line only; a timeout must not fail the command.
            parsed = readModeLine(SensorServiceDumper.CONFIRM_TIMEOUT_MS)
        }

        val outcome = ModeConfirmPolicy.afterCommand(desired, commandSucceeded = !commandFailed, parsed)
        lastMode = outcome.mode
        if (outcome.applied) {
            if (desired is DesiredState.Normal) {
                usedPrivacyFallback = false
            }
            lastError = if (outcome.confirmed) null else "mode-line-unreadable"
            return true
        }

        lastError = "restrict-failed"
        Log.w(TAG, "SensorService command failed: $lastError mode=$parsed")

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

    private fun dumpOrNull(
        args: Array<String>,
        stopAtMode: Boolean,
        timeoutMs: Long = SensorServiceDumper.DEFAULT_TIMEOUT_MS
    ): String? {
        return try {
            ShizukuClient.dumpSensorservice(args, stopAtMode = stopAtMode, timeoutMs = timeoutMs)
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
        return ModeConfirmPolicy.matches(desired, mode)
    }

    companion object {
        private const val TAG = "SensorRestrict"
    }
}
