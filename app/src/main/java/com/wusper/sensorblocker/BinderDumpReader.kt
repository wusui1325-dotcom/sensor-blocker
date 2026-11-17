package com.wusper.sensorblocker

import java.io.Reader

/**
 * Reads an IBinder.dump pipe without filling the buffer. The writer
 * (sensorservice) and this reader must run on different threads.
 */
object BinderDumpReader {
    const val MAX_BYTES = 256 * 1024

    fun hasModeLine(text: CharSequence): Boolean {
        return SensorServiceModeParser.parse(text.toString()) !is SensorServiceMode.Unknown
    }

    /**
     * Copy [input] into a string until EOF, [maxBytes], or [stopWhen].
     * After stopping append, remaining bytes are still drained so the
     * writer is not blocked on a full pipe.
     */
    fun read(
        input: Reader,
        maxBytes: Int = MAX_BYTES,
        stopWhen: (CharSequence) -> Boolean = { false }
    ): String {
        val output = StringBuilder()
        val buf = CharArray(4096)
        var total = 0
        var append = true
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            if (append) {
                val take = minOf(n, (maxBytes - total).coerceAtLeast(0))
                if (take > 0) {
                    output.append(buf, 0, take)
                    total += take
                }
                if (total >= maxBytes || stopWhen(output)) {
                    append = false
                }
            }
        }
        return output.toString()
    }
}
