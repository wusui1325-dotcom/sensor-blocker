package com.wusper.sensorblocker

import java.io.Reader

/**
 * Reads an IBinder.dump pipe without filling the buffer. The writer
 * (sensorservice) and this reader must run on different threads.
 *
 * Stored output is capped at [MAX_BYTES], but a rolling window keeps
 * scanning for a Mode line even after that cap, because flagship dumps
 * can list sensors first and print Mode later.
 */
object BinderDumpReader {
    const val MAX_BYTES = 256 * 1024
    const val MODE_WINDOW_CHARS = 16 * 1024

    fun hasModeLine(text: CharSequence): Boolean {
        return SensorServiceModeParser.parse(text.toString()) !is SensorServiceMode.Unknown
    }

    /**
     * Copy [input] into a string until EOF or [stopWhen] (after a Mode
     * line is observed). After the stored cap, new bytes are only kept
     * in a rolling window. Remaining bytes are still drained so the
     * writer is not blocked on a full pipe.
     */
    fun read(
        input: Reader,
        maxBytes: Int = MAX_BYTES,
        stopWhen: (CharSequence) -> Boolean = { false },
        windowChars: Int = MODE_WINDOW_CHARS
    ): String {
        val output = StringBuilder()
        val window = StringBuilder()
        val buf = CharArray(4096)
        var total = 0
        var append = true
        var foundMode = false
        var drainOnly = false

        fun ingest(chunk: String) {
            if (append) {
                val room = (maxBytes - total).coerceAtLeast(0)
                val take = minOf(chunk.length, room)
                if (take > 0) {
                    output.append(chunk, 0, take)
                    total += take
                }
                if (total >= maxBytes) append = false
            }
            window.append(chunk)
            if (window.length > windowChars) {
                window.delete(0, window.length - windowChars)
            }
            if (!foundMode && hasModeLine(window)) {
                foundMode = true
                if (!hasModeLine(output)) {
                    if (output.isNotEmpty() && output.last() != '\n') output.append('\n')
                    output.append(window)
                }
            }
            if (foundMode && stopWhen(if (hasModeLine(output)) output else window)) {
                drainOnly = true
            }
        }

        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            if (drainOnly) continue
            ingest(String(buf, 0, n))
        }
        if (foundMode && !hasModeLine(output) && window.isNotEmpty()) {
            if (output.isNotEmpty() && output.last() != '\n') output.append('\n')
            output.append(window)
        }
        return output.toString()
    }
}
