package com.wusper.sensorblocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.Reader
import java.io.StringReader

class BinderDumpReaderTest {
    @Test
    fun readsCompleteDump() {
        val text = "Sensor List:\nMode : NORMAL\n"
        assertEquals(text, BinderDumpReader.read(StringReader(text)))
        assertTrue(BinderDumpReader.hasModeLine(text))
    }

    @Test
    fun capsOutput() {
        val text = "abcdefghij"
        assertEquals("abcde", BinderDumpReader.read(StringReader(text), maxBytes = 5))
    }

    @Test
    fun stopsAppendingOnceModeLineIsFoundButStillDrains() {
        val reader = SequentialReader(
            listOf(
                "header\nMode : RESTRICTED : com.wusper.sensortest\n",
                "this-should-not-appear-in-output\n"
            )
        )
        val out = BinderDumpReader.read(reader, stopWhen = BinderDumpReader::hasModeLine)
        assertTrue(out.contains("Mode : RESTRICTED : com.wusper.sensortest"))
        assertFalse(out.contains("this-should-not-appear-in-output"))
        assertEquals(2, reader.reads)
        assertTrue(BinderDumpReader.hasModeLine(out))
        assertEquals(
            "com.wusper.sensortest",
            (SensorServiceModeParser.parse(out) as SensorServiceMode.Restricted).packageName
        )
    }

    @Test
    fun findsModeLineAfterStoredCapViaRollingWindow() {
        val prefix = "S" + "ensor ".repeat(200)
        val reader = SequentialReader(
            listOf(
                prefix,
                "\nMode : RESTRICTED : com.wusper.sensortest\n",
                "tail-should-drain-only\n"
            )
        )
        val out = BinderDumpReader.read(
            reader,
            maxBytes = 64,
            stopWhen = BinderDumpReader::hasModeLine,
            windowChars = 256
        )
        assertTrue(out.contains("Mode : RESTRICTED : com.wusper.sensortest"))
        assertFalse(out.contains("tail-should-drain-only"))
        assertEquals(3, reader.reads)
        assertEquals(
            "com.wusper.sensortest",
            (SensorServiceModeParser.parse(out) as SensorServiceMode.Restricted).packageName
        )
    }

    @Test
    fun hasModeLineFalseUntilComplete() {
        assertFalse(BinderDumpReader.hasModeLine("Sensor List:\n"))
        assertFalse(BinderDumpReader.hasModeLine(""))
    }

    private class SequentialReader(private val chunks: List<String>) : Reader() {
        var reads = 0
            private set
        private var index = 0

        override fun read(cbuf: CharArray, off: Int, len: Int): Int {
            if (index >= chunks.size) return -1
            reads += 1
            val chunk = chunks[index]
            index += 1
            val n = chunk.length
            require(n <= len)
            chunk.toCharArray().copyInto(cbuf, off)
            return n
        }

        override fun close() = Unit
    }
}
