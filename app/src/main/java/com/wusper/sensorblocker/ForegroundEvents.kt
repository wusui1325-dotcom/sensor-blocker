package com.wusper.sensorblocker

/**
 * Pure helper for UsageEvents-style records. ACTIVITY_RESUMED and the
 * deprecated MOVE_TO_FOREGROUND share type value 1 on Android.
 */
data class ForegroundEvent(
    val type: Int,
    val timestamp: Long,
    val packageName: String
)

object ForegroundEvents {
    const val ACTIVITY_RESUMED = 1
    const val ACTIVITY_PAUSED = 2

    fun latestResumedPackage(events: List<ForegroundEvent>): String? {
        return events
            .filter { it.type == ACTIVITY_RESUMED && it.packageName.isNotBlank() }
            .maxByOrNull { it.timestamp }
            ?.packageName
    }
}
