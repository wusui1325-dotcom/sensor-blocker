package com.wusper.sensorblocker

import android.app.Application
import android.os.Build
import java.io.File

/**
 * Shizuku starts [com.wusper.sensorblocker.shell.ShellUserService] in
 * `applicationId:shell`. That process still runs Application.onCreate
 * unless we bail out, which can crash or hang the privileged process
 * before the UserService binder is published.
 */
object ProcessIdentity {
    const val USER_SERVICE_SUFFIX = ":shell"

    /**
     * Only the default app process (package name) should run catalog
     * refresh, Shizuku init, or start [SensorMonitorService].
     * An unreadable / empty name is treated as the main process so a
     * cmdline glitch cannot disable the real app; `:shell` is still
     * rejected first when the name is present.
     */
    fun isDefaultAppProcess(processName: String?, packageName: String): Boolean {
        val name = processName?.trim().orEmpty()
        if (name.isEmpty()) return true
        if (isUserServiceProcess(name)) return false
        return name == packageName
    }

    fun isUserServiceProcess(processName: String?): Boolean {
        val name = processName?.trim().orEmpty()
        return name.endsWith(USER_SERVICE_SUFFIX)
    }

    /** Parses `/proc/self/cmdline` (NUL-terminated, sometimes newline-padded). */
    fun parseCmdline(bytes: ByteArray): String {
        var end = bytes.indexOf(0.toByte())
        if (end < 0) {
            end = bytes.indexOf('\n'.code.toByte())
        }
        if (end < 0) end = bytes.size
        return String(bytes, 0, end, Charsets.UTF_8).trim()
    }

    fun currentProcessName(apiLevel: Int = Build.VERSION.SDK_INT, cmdline: () -> ByteArray? = ::readProcSelfCmdline): String {
        if (apiLevel >= 28 && Build.VERSION.SDK_INT >= 28) {
            val fromApi = try {
                Application.getProcessName()
            } catch (_: Throwable) {
                null
            }?.trim().orEmpty()
            if (fromApi.isNotEmpty()) return fromApi
        }
        val bytes = try {
            cmdline()
        } catch (_: Throwable) {
            null
        } ?: return ""
        return parseCmdline(bytes)
    }

    private fun readProcSelfCmdline(): ByteArray? {
        return try {
            File("/proc/self/cmdline").readBytes()
        } catch (_: Exception) {
            null
        }
    }
}
