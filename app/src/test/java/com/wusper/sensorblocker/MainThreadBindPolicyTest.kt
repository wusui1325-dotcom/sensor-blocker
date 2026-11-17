package com.wusper.sensorblocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainThreadBindPolicyTest {
    @Test
    fun mainThreadAlwaysRunsInlineAndNeverBlocks() {
        assertEquals(MainThreadAction.RUN_INLINE, MainThreadBindPolicy.action(true, false))
        assertEquals(MainThreadAction.RUN_INLINE, MainThreadBindPolicy.action(true, true))
        assertFalse(MainThreadBindPolicy.mayBlockForConnection(true))
    }

    @Test
    fun backgroundPostsAndMayWait() {
        assertEquals(MainThreadAction.POST, MainThreadBindPolicy.action(false, false))
        assertEquals(MainThreadAction.POST_AND_WAIT, MainThreadBindPolicy.action(false, true))
        assertTrue(MainThreadBindPolicy.mayBlockForConnection(false))
    }
}
