package com.wusper.sensorblocker

sealed class SensorServiceMode {
    data object Normal : SensorServiceMode()
    data class Restricted(val packageName: String?) : SensorServiceMode()
    data object DataInjection : SensorServiceMode()
    data object Unknown : SensorServiceMode()

    fun describePackage(): String? = when (this) {
        is Restricted -> packageName
        else -> null
    }
}

object SensorServiceModeParser {
    private val modeLine = Regex(
        """(?im)^\s*Mode\s*:\s*(NORMAL|RESTRICTED|DATA_INJECTION)\s*(?::\s*([A-Za-z0-9_.]+))?"""
    )

    fun parse(dump: String): SensorServiceMode {
        if (dump.isBlank()) return SensorServiceMode.Unknown
        val match = modeLine.find(dump) ?: return SensorServiceMode.Unknown
        val kind = match.groupValues[1].uppercase()
        val pkg = match.groupValues[2].ifBlank { null }
        return when (kind) {
            "NORMAL" -> SensorServiceMode.Normal
            "RESTRICTED" -> SensorServiceMode.Restricted(pkg)
            "DATA_INJECTION" -> SensorServiceMode.DataInjection
            else -> SensorServiceMode.Unknown
        }
    }
}
