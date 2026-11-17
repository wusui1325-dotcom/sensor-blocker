package com.wusper.sensorblocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BindRetryPolicyTest {
    @Test
    fun firstConnectUsesLongerTimeout() {
        assertEquals(8_000L, BindRetryPolicy.timeoutMs(0))
        assertEquals(6_000L, BindRetryPolicy.timeoutMs(1))
        assertEquals(6_000L, BindRetryPolicy.timeoutMs(5))
    }

    @Test
    fun backoffGrowsThenCaps() {
        assertEquals(4_000L, BindRetryPolicy.backoffMs(1))
        assertEquals(8_000L, BindRetryPolicy.backoffMs(2))
        assertEquals(16_000L, BindRetryPolicy.backoffMs(3))
        assertEquals(30_000L, BindRetryPolicy.backoffMs(4))
        assertEquals(30_000L, BindRetryPolicy.backoffMs(10))
    }

    @Test
    fun forceIgnoresBackoffWindow() {
        assertFalse(BindRetryPolicy.shouldStartBind(now = 100, nextRetryAt = 500, force = false))
        assertTrue(BindRetryPolicy.shouldStartBind(now = 100, nextRetryAt = 500, force = true))
        assertTrue(BindRetryPolicy.shouldStartBind(now = 500, nextRetryAt = 500, force = false))
    }
}
