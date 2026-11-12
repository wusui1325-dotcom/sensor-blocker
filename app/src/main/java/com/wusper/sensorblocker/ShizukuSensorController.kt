package com.wusper.sensorblocker

import android.util.Log
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuSensorController {
    private const val TAG = "ShizukuSensor"

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun setSensorPrivacy(enabled: Boolean) {
        if (!isShizukuAvailable()) {
            Log.w(TAG, "Shizuku not available")
            return
        }
        val value = if (enabled) "1" else "0"
        val commands = listOf(
            "service call sensor_privacy 9 i32 $value",
            "service call sensor_privacy 8 i32 $value",
            "service call sensor_privacy 4 i32 $value"
        )
        for (cmd in commands) {
            try {
                val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = reader.readText()
                process.waitFor()
                Log.d(TAG, "Ran: $cmd -> exit=${process.exitValue()} output=$output")
                if (process.exitValue() == 0) break
            } catch (e: Exception) {
                Log.e(TAG, "Failed cmd $cmd", e)
            }
        }
    }

    /**
     * More precise foreground package via dumpsys window / activity.
     * Prefer this over UsageStats when Shizuku is available.
     */
    fun getFocusedPackage(): String? {
        if (!isShizukuAvailable()) return null
        val cmds = listOf(
            "dumpsys window windows | grep -E 'mCurrentFocus|mFocusedApp' | head -1",
            "dumpsys activity activities | grep -E 'mResumedActivity|mFocusedActivity' | head -1"
        )
        for (cmd in cmds) {
            try {
                val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val line = reader.readLine() ?: continue
                process.waitFor()
                // Typical: mCurrentFocus=Window{... com.package.name/...}
                val match = Regex("""([a-zA-Z0-9_.]+)/""").find(line)
                if (match != null) {
                    return match.groupValues[1]
                }
            } catch (e: Exception) {
                Log.e(TAG, "getFocusedPackage failed", e)
            }
        }
        return null
    }
}
