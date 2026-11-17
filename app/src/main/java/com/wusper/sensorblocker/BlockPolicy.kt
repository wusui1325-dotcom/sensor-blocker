package com.wusper.sensorblocker

enum class BlockMode {
    GLOBAL,
    LIST
}

sealed class DesiredState {
    data object Normal : DesiredState()
    data class RestrictTo(val packageName: String) : DesiredState()
}

/**
 * Pure decision of the desired SensorService mode.
 *
 * Global: keep sensors restricted to this app, except when an allowlisted
 * app is in the foreground (then restrict to that app only).
 * List: restrict only while a blocklisted app is in the foreground.
 */
object BlockPolicy {
    fun decide(
        mode: BlockMode,
        foregroundPackage: String?,
        allowlist: Set<String>,
        blocklist: Set<String>,
        selfPackage: String
    ): DesiredState {
        val fg = foregroundPackage?.takeIf { it.isNotBlank() }
        return when (mode) {
            BlockMode.GLOBAL -> {
                if (fg != null && fg in allowlist) {
                    DesiredState.RestrictTo(fg)
                } else {
                    DesiredState.RestrictTo(selfPackage)
                }
            }
            BlockMode.LIST -> {
                if (fg != null && fg in blocklist) {
                    DesiredState.RestrictTo(selfPackage)
                } else {
                    DesiredState.Normal
                }
            }
        }
    }

    fun parseMode(raw: String?): BlockMode {
        return if (raw.equals(BlockMode.LIST.name, ignoreCase = true)) {
            BlockMode.LIST
        } else {
            BlockMode.GLOBAL
        }
    }
}
