package com.wusper.sensorblocker.shell

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Runs in a Shizuku-started process with ADB/shell privileges so
 * Runtime.exec can invoke dumpsys. Not declared as a regular service.
 */
class ShellUserService : IShellService.Stub() {
    override fun destroy() {
        System.exit(0)
    }

    override fun exec(command: String): String {
        val process = ProcessBuilder("sh", "-c", command)
            .redirectErrorStream(true)
            .start()
        val output = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            val buf = CharArray(4096)
            var n: Int
            var total = 0
            val max = 256 * 1024
            while (reader.read(buf).also { n = it } != -1) {
                val remain = max - total
                if (remain <= 0) break
                val take = minOf(n, remain)
                output.append(buf, 0, take)
                total += take
            }
        }
        val finished = process.waitFor(15, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return "EXIT:124\n$output"
        }
        return "EXIT:${process.exitValue()}\n$output"
    }
}
