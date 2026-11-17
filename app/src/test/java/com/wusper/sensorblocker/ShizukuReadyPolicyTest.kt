package com.wusper.sensorblocker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShizukuReadyPolicyTest {
    @Test
    fun dumpPathAloneIsReady() {
        assertTrue(ShizukuReadyPolicy.isReady(true, dumpPathReady = true, shellConnected = false))
    }

    @Test
    fun userServiceAloneIsStillReadyAsFallback() {
        assertTrue(ShizukuReadyPolicy.isReady(true, dumpPathReady = false, shellConnected = true))
    }

    @Test
    fun permissionOrPathMissingIsNotReady() {
        assertFalse(ShizukuReadyPolicy.isReady(false, dumpPathReady = true, shellConnected = true))
        assertFalse(ShizukuReadyPolicy.isReady(true, dumpPathReady = false, shellConnected = false))
        assertFalse(ShizukuReadyPolicy.isReady(false, dumpPathReady = false, shellConnected = false))
    }
}
