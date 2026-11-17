package com.wusper.sensorblocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorServiceModeParserTest {
    @Test
    fun parsesNormal() {
        val dump = """
            Sensor Device:
              Foo
            Mode : NORMAL
            Sensor List:
        """.trimIndent()
        assertEquals(SensorServiceMode.Normal(), SensorServiceModeParser.parse(dump))
    }

    @Test
    fun parsesRestrictedWithPackage() {
        val dump = "Mode : RESTRICTED : com.wusper.sensorblocker\n"
        val mode = SensorServiceModeParser.parse(dump)
        assertEquals(SensorServiceMode.Restricted("com.wusper.sensorblocker"), mode)
    }

    @Test
    fun parsesRestrictedWithoutPackage() {
        val mode = SensorServiceModeParser.parse("Mode : RESTRICTED\n")
        assertEquals(SensorServiceMode.Restricted(null), mode)
    }

    @Test
    fun parsesDataInjectionAndUnknown() {
        assertEquals(SensorServiceMode.DataInjection, SensorServiceModeParser.parse("Mode : DATA_INJECTION"))
        assertEquals(SensorServiceMode.Unknown, SensorServiceModeParser.parse(""))
        assertEquals(SensorServiceMode.Unknown, SensorServiceModeParser.parse("no mode line here"))
    }

    @Test
    fun ignoresCaseAndExtraSpaces() {
        val mode = SensorServiceModeParser.parse("  mode : restricted : com.autonavi.minimap")
        assertEquals(SensorServiceMode.Restricted("com.autonavi.minimap"), mode)
    }

    @Test
    fun firstModeLineWins() {
        val dump = "Mode : RESTRICTED : com.foo\nMode : NORMAL\n"
        assertTrue(SensorServiceModeParser.parse(dump) is SensorServiceMode.Restricted)
    }

    @Test
    fun parsesModeWithoutSpaceBeforeColon() {
        assertEquals(SensorServiceMode.Normal(), SensorServiceModeParser.parse("Mode: NORMAL"))
        assertEquals(
            SensorServiceMode.Restricted("com.wusper.sensortest"),
            SensorServiceModeParser.parse("Mode:RESTRICTED:com.wusper.sensortest")
        )
    }

    @Test
    fun parsesParenthesesPackage() {
        assertEquals(
            SensorServiceMode.Restricted("com.wusper.sensortest"),
            SensorServiceModeParser.parse("Mode : RESTRICTED (com.wusper.sensortest)")
        )
    }

    @Test
    fun parsesOperatingModeAndRestrictedTo() {
        val dump = """
            Operating Mode : RESTRICTED
            Restricted to com.wusper.sensorblocker
        """.trimIndent()
        assertEquals(
            SensorServiceMode.Restricted("com.wusper.sensorblocker"),
            SensorServiceModeParser.parse(dump)
        )
    }

    @Test
    fun parsesRestrictedPackageField() {
        val dump = "Mode: Restricted\nRestricted package: com.android.systemui\n"
        assertEquals(
            SensorServiceMode.Restricted("com.android.systemui"),
            SensorServiceModeParser.parse(dump)
        )
    }

    @Test
    fun parsesRestrictedToAlone() {
        assertEquals(
            SensorServiceMode.Restricted("com.foo.bar"),
            SensorServiceModeParser.parse("restricted to package com.foo.bar")
        )
    }

    @Test
    fun parsesDataInjectionWithSpace() {
        assertEquals(SensorServiceMode.DataInjection, SensorServiceModeParser.parse("Mode : DATA INJECTION"))
    }
}
