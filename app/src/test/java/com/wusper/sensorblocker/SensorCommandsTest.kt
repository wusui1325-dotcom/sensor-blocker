package com.wusper.sensorblocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorCommandsTest {
    @Test
    fun restrictBuildsPublicCommand() {
        assertEquals(
            "dumpsys sensorservice restrict com.wusper.sensorblocker",
            SensorCommands.restrict("com.wusper.sensorblocker")
        )
    }

    @Test
    fun enableAndDumpCommands() {
        assertEquals("dumpsys sensorservice enable", SensorCommands.enable())
        assertEquals("dumpsys sensorservice", SensorCommands.dump())
    }

    @Test
    fun rejectsUnsafePackageNames() {
        assertFalse(SensorCommands.isSafePackageName(""))
        assertFalse(SensorCommands.isSafePackageName("com.foo; rm -rf /"))
        assertFalse(SensorCommands.isSafePackageName("com.foo bar"))
        assertFalse(SensorCommands.isSafePackageName("com.foo&&reboot"))
        assertFalse(SensorCommands.isSafePackageName("com.foo`id`"))
        assertTrue(SensorCommands.isSafePackageName("com.autonavi.minimap"))
        assertTrue(SensorCommands.isSafePackageName("android"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun restrictThrowsOnInjection() {
        SensorCommands.restrict("com.foo; dumpsys")
    }

    @Test
    fun fallbackCodesAreVersionAware() {
        assertEquals(
            listOf("service call sensor_privacy 4 i32 1"),
            SensorCommands.fallbackPrivacyCommands(true, 31)
        )
        assertEquals(
            listOf(
                "service call sensor_privacy 9 i32 0",
                "service call sensor_privacy 8 i32 0",
                "service call sensor_privacy 4 i32 0"
            ),
            SensorCommands.fallbackPrivacyCommands(false, 33)
        )
        assertEquals(
            listOf(
                "service call sensor_privacy 9 i32 1",
                "service call sensor_privacy 8 i32 1",
                "service call sensor_privacy 4 i32 1"
            ),
            SensorCommands.fallbackPrivacyCommands(true, 35)
        )
    }
}
