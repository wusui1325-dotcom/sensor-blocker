package com.wusper.sensorblocker

/**
 * Accessibility is near-instant, Shizuku dumpsys is a supplement, and
 * UsageEvents is the lowest-priority fallback. A stale UsageEvents record
 * must not overwrite a recent better source.
 */
object ForegroundSourcePriority {
    const val ACCESSIBILITY = "accessibility"
    const val SHIZUKU = "shizuku"
    const val USAGE = "usage"

    const val BETTER_SOURCE_HOLD_MS = 3_000L

    fun rank(source: String): Int = when (source) {
        ACCESSIBILITY -> 3
        SHIZUKU -> 2
        USAGE -> 1
        else -> 0
    }

    fun shouldAccept(newSource: String, currentSource: String, ageMs: Long): Boolean {
        if (currentSource == "none" || currentSource.isBlank()) return true
        if (newSource == currentSource) return true
        val newRank = rank(newSource)
        val currentRank = rank(currentSource)
        if (newRank >= currentRank) return true
        return ageMs >= BETTER_SOURCE_HOLD_MS
    }
}
