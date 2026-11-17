package com.wusper.sensorblocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeConfirmPolicyTest {
    private val restrict = DesiredState.RestrictTo("com.wusper.sensorblocker")

    @Test
    fun successfulCommandWithoutModeLineStillApplies() {
        val result = ModeConfirmPolicy.afterCommand(
            restrict,
            commandSucceeded = true,
            parsed = SensorServiceMode.Unknown
        )
        assertTrue(result.applied)
        assertFalse(result.confirmed)
        val mode = result.mode as SensorServiceMode.Restricted
        assertEquals("com.wusper.sensorblocker", mode.packageName)
        assertFalse(mode.confirmed)
    }

    @Test
    fun successfulCommandWithModeLineIsConfirmed() {
        val result = ModeConfirmPolicy.afterCommand(
            DesiredState.Normal,
            commandSucceeded = true,
            parsed = SensorServiceMode.Normal()
        )
        assertTrue(result.applied)
        assertTrue(result.confirmed)
        assertTrue((result.mode as SensorServiceMode.Normal).confirmed)
    }

    @Test
    fun failedCommandIsNotApplied() {
        val result = ModeConfirmPolicy.afterCommand(
            restrict,
            commandSucceeded = false,
            parsed = SensorServiceMode.Unknown
        )
        assertFalse(result.applied)
        assertFalse(result.confirmed)
    }

    @Test
    fun unreadableRecheckDoesNotForceReapply() {
        assertFalse(ModeConfirmPolicy.shouldReapplyAfterRecheck(restrict, SensorServiceMode.Unknown))
        assertFalse(
            ModeConfirmPolicy.shouldReapplyAfterRecheck(
                restrict,
                SensorServiceMode.Restricted("com.wusper.sensorblocker", confirmed = false)
            )
        )
        assertFalse(
            ModeConfirmPolicy.shouldReapplyAfterRecheck(
                restrict,
                SensorServiceMode.Restricted("com.wusper.sensorblocker")
            )
        )
        assertTrue(
            ModeConfirmPolicy.shouldReapplyAfterRecheck(
                restrict,
                SensorServiceMode.Normal()
            )
        )
    }
}
