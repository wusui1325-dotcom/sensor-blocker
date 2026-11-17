package com.wusper.sensorblocker

/**
 * Public Android SensorService dumpsys commands.
 * Restricted mode is a documented SensorService operating mode: only the
 * named package may receive sensor events until [enable] restores NORMAL.
 */
object SensorCommands {
    private val SAFE_PACKAGE = Regex("^[A-Za-z0-9_.]+$")

    fun isSafePackageName(packageName: String): Boolean {
        return packageName.isNotEmpty() && SAFE_PACKAGE.matches(packageName)
    }

    fun restrict(packageName: String): String {
        require(isSafePackageName(packageName)) { "Invalid package name" }
        return "dumpsys sensorservice restrict $packageName"
    }

    fun enable(): String = "dumpsys sensorservice enable"

    fun dump(): String = "dumpsys sensorservice"

    fun dumpModeLine(): String = "dumpsys sensorservice 2>/dev/null | grep -E 'Mode :'"

    fun focusedWindow(): String =
        "dumpsys window 2>/dev/null | grep -E 'mCurrentFocus|mFocusedApp|mFocusedWindow'"

    fun resumedActivity(): String =
        "dumpsys activity activities 2>/dev/null | grep -E 'mResumedActivity|topResumedActivity|mFocusedApp'"

    /**
     * Fallback only: ISensorPrivacyManager via "service call sensor_privacy".
     * Transaction codes shift by API level, and on AOSP this toggle is
     * primarily camera / microphone, not motion sensors.
     */
    fun fallbackPrivacyCommands(enabled: Boolean, sdkInt: Int): List<String> {
        val value = if (enabled) "1" else "0"
        val codes = when {
            sdkInt >= 34 -> listOf(9, 8, 4)
            sdkInt >= 33 -> listOf(8, 4)
            else -> listOf(4)
        }
        return codes.map { "service call sensor_privacy $it i32 $value" }
    }
}
