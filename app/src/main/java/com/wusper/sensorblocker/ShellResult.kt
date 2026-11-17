package com.wusper.sensorblocker

data class ShellResult(
    val exitCode: Int,
    val output: String
) {
    val isSuccess: Boolean get() = exitCode == 0
}

object ShellOutput {
    fun parse(raw: String): ShellResult {
        val nl = raw.indexOf('\n')
        if (raw.startsWith("EXIT:") && nl >= 0) {
            val code = raw.substring(5, nl).toIntOrNull() ?: -1
            return ShellResult(code, raw.substring(nl + 1))
        }
        if (raw.startsWith("EXIT:")) {
            val code = raw.substring(5).toIntOrNull() ?: -1
            return ShellResult(code, "")
        }
        return ShellResult(0, raw)
    }
}
