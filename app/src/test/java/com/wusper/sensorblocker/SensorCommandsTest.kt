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
    fun restrictAndEnableBatchAModeQueryInOneShell() {
        assertEquals(
            "dumpsys sensorservice restrict com.wusper.sensorblocker; dumpsys sensorservice 2>/dev/null | grep -E 'Mode :'",
            SensorCommands.restrictAndQueryMode("com.wusper.sensorblocker")
        )
        assertEquals(
            "dumpsys sensorservice enable; dumpsys sensorservice 2>/dev/null | grep -E 'Mode :'",
            SensorCommands.enableAndQueryMode()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun restrictAndQueryModeRejectsInjection() {
        SensorCommands.restrictAndQueryMode("com.foo; rm -rf /")
    }

    @Test
    fun dumpArgsMatchPublicSensorserviceInterface() {
        assertTrue(
            SensorCommands.restrictDumpArgs("com.wusper.sensortest").contentEquals(
                arrayOf("restrict", "com.wusper.sensortest")
            )
        )
        assertTrue(SensorCommands.enableDumpArgs().contentEquals(arrayOf("enable")))
        assertTrue(SensorCommands.queryDumpArgs().isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun restrictDumpArgsRejectsInjection() {
        SensorCommands.restrictDumpArgs("com.foo; rm -rf /")
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
