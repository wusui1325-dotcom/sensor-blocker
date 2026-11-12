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
        // Try common transaction codes for setSensorPrivacy across versions
        // 8/9 are common on Android 12/13; adjust if needed on real device
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
                Log.d(TAG, "Ran: $cmd -> $output")
                if (output.isNotBlank()) break
            } catch (e: Exception) {
                Log.e(TAG, "Failed cmd $cmd", e)
            }
        }
    }
}
