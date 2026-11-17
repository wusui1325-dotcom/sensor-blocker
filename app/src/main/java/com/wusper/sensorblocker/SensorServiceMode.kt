package com.wusper.sensorblocker

sealed class SensorServiceMode {
    open val confirmed: Boolean get() = true

    data class Normal(override val confirmed: Boolean = true) : SensorServiceMode()
    data class Restricted(
        val packageName: String?,
        override val confirmed: Boolean = true
    ) : SensorServiceMode()
    data object DataInjection : SensorServiceMode()
    data object Unknown : SensorServiceMode() {
        override val confirmed: Boolean get() = false
    }

    fun describePackage(): String? = when (this) {
        is Restricted -> packageName
        else -> null
    }
}

object SensorServiceModeParser {
    private val MODE_KIND = Regex(
        """(?im)(?:operating\s+)?mode\s*:\s*(NORMAL|RESTRICTED|DATA[\s_]?INJECTION)\b""" +
            """(?:\s*[:\-]\s*|\s+to(?:\s+package)?\s+|\s*\(\s*|\s+package\s+)?""" +
            """([A-Za-z][A-Za-z0-9_.]*)?"""
    )
    private val RESTRICTED_TO = Regex(
        """(?im)\brestricted\s+to(?:\s+package)?\s*[:\s]+([A-Za-z][A-Za-z0-9_.]+)"""
    )
    private val RESTRICTED_PACKAGE = Regex(
        """(?im)\brestricted\s+package\s*[:\s]+([A-Za-z][A-Za-z0-9_.]+)"""
    )
    private val LOOSE_NORMAL = Regex("""(?im)\b(?:current\s+)?(?:operating\s+)?mode\b[^\n]{0,40}\bNORMAL\b""")
    private val LOOSE_RESTRICTED = Regex("""(?im)\b(?:current\s+)?(?:operating\s+)?mode\b[^\n]{0,40}\bRESTRICTED\b""")
    private val LOOSE_INJECTION = Regex("""(?im)\bDATA[\s_]?INJECTION\b""")

    fun parse(dump: String): SensorServiceMode {
        if (dump.isBlank()) return SensorServiceMode.Unknown
        MODE_KIND.find(dump)?.let { match ->
            val kind = match.groupValues[1].uppercase().replace(' ', '_')
            val pkg = match.groupValues.getOrNull(2)?.trim()?.trimEnd(')')?.ifBlank { null }
            return when (kind) {
                "NORMAL" -> SensorServiceMode.Normal()
                "RESTRICTED" -> SensorServiceMode.Restricted(pkg ?: findPackage(dump))
                "DATA_INJECTION" -> SensorServiceMode.DataInjection
                else -> SensorServiceMode.Unknown
            }
        }
        RESTRICTED_TO.find(dump)?.let { match ->
            return SensorServiceMode.Restricted(match.groupValues[1])
        }
        RESTRICTED_PACKAGE.find(dump)?.let { match ->
            return SensorServiceMode.Restricted(match.groupValues[1])
        }
        return when {
            LOOSE_INJECTION.containsMatchIn(dump) && LOOSE_RESTRICTED.containsMatchIn(dump).not() &&
                LOOSE_NORMAL.containsMatchIn(dump).not() -> SensorServiceMode.DataInjection
            LOOSE_RESTRICTED.containsMatchIn(dump) -> SensorServiceMode.Restricted(findPackage(dump))
            LOOSE_NORMAL.containsMatchIn(dump) -> SensorServiceMode.Normal()
            else -> SensorServiceMode.Unknown
        }
    }

    private fun findPackage(dump: String): String? {
        RESTRICTED_TO.find(dump)?.groupValues?.getOrNull(1)?.let { return it }
        RESTRICTED_PACKAGE.find(dump)?.groupValues?.getOrNull(1)?.let { return it }
        return null
    }
}
