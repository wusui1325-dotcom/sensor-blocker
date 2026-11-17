package com.wusper.sensorblocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundSourcePriorityTest {
    @Test
    fun usageDoesNotOverwriteRecentShizukuOrAccessibility() {
        assertFalse(
            ForegroundSourcePriority.shouldAccept(
                ForegroundSourcePriority.USAGE,
                ForegroundSourcePriority.ACCESSIBILITY,
                200L
            )
        )
        assertFalse(
            ForegroundSourcePriority.shouldAccept(
                ForegroundSourcePriority.USAGE,
                ForegroundSourcePriority.SHIZUKU,
                1_000L
            )
        )
    }

    @Test
    fun shizukuDoesNotOverwriteRecentAccessibility() {
        assertFalse(
            ForegroundSourcePriority.shouldAccept(
                ForegroundSourcePriority.SHIZUKU,
                ForegroundSourcePriority.ACCESSIBILITY,
                400L
            )
        )
        assertTrue(
            ForegroundSourcePriority.shouldAccept(
                ForegroundSourcePriority.ACCESSIBILITY,
                ForegroundSourcePriority.SHIZUKU,
                400L
            )
        )
    }

    @Test
    fun staleBetterSourceCanBeSupplemented() {
        assertTrue(
            ForegroundSourcePriority.shouldAccept(
                ForegroundSourcePriority.USAGE,
                ForegroundSourcePriority.SHIZUKU,
                ForegroundSourcePriority.BETTER_SOURCE_HOLD_MS
            )
        )
    }

    @Test
    fun memoryRejectsStaleUsageFlip() {
        var now = 1_000L
        val mem = ForegroundMemory(
            selfPackage = "com.wusper.sensorblocker",
            clock = { now }
        )
        assertTrue(mem.update("com.taobao.taobao", ForegroundSourcePriority.SHIZUKU))
        now += 500L
        assertFalse(mem.update("com.ss.android.ugc.aweme", ForegroundSourcePriority.USAGE))
        assertEquals("com.taobao.taobao", mem.current)
        now += ForegroundSourcePriority.BETTER_SOURCE_HOLD_MS
        assertTrue(mem.update("com.ss.android.ugc.aweme", ForegroundSourcePriority.USAGE))
        assertEquals("com.ss.android.ugc.aweme", mem.current)
    }
}
