package com.wusper.sensorblocker

import org.junit.Assert.assertEquals
import org.junit.Test

class ShellOutputParseTest {
    @Test
    fun parsesExitAndBody() {
        val result = ShellOutput.parse("EXIT:0\nMode : NORMAL\n")
        assertEquals(0, result.exitCode)
        assertEquals("Mode : NORMAL\n", result.output)
    }

    @Test
    fun parsesFailure() {
        val result = ShellOutput.parse("EXIT:124\n")
        assertEquals(124, result.exitCode)
        assertEquals("", result.output)
    }
}
