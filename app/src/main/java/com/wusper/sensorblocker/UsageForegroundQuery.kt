package com.wusper.sensorblocker

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager

object UsageForegroundQuery {
    fun latestResumedPackage(
        usm: UsageStatsManager,
        now: Long = System.currentTimeMillis(),
        windowMs: Long = 60_000L
    ): String? {
        val events = try {
            usm.queryEvents(now - windowMs, now)
        } catch (_: SecurityException) {
            return null
        } ?: return null
        val ev = UsageEvents.Event()
        val collected = mutableListOf<ForegroundEvent>()
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            val pkg = ev.packageName ?: continue
            collected.add(ForegroundEvent(ev.eventType, ev.timeStamp, pkg))
        }
        return ForegroundEvents.latestResumedPackage(collected)
    }
}
