package com.wusper.sensorblocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundEventsTest {
    @Test
    fun picksLatestResumedNotLastTimeUsedStyleMaxOfAllEvents() {
        val events = listOf(
            ForegroundEvent(ForegroundEvents.ACTIVITY_RESUMED, 10, "com.old"),
            ForegroundEvent(ForegroundEvents.ACTIVITY_PAUSED, 50, "com.old"),
            ForegroundEvent(ForegroundEvents.ACTIVITY_RESUMED, 20, "com.newapp"),
            ForegroundEvent(ForegroundEvents.ACTIVITY_PAUSED, 80, "com.unrelated")
        )
        assertEquals("com.newapp", ForegroundEvents.latestResumedPackage(events))
    }

    @Test
    fun emptyOrNoResumeReturnsNull() {
        assertNull(ForegroundEvents.latestResumedPackage(emptyList()))
        assertNull(
            ForegroundEvents.latestResumedPackage(
                listOf(ForegroundEvent(ForegroundEvents.ACTIVITY_PAUSED, 1, "com.foo"))
            )
        )
    }
}
