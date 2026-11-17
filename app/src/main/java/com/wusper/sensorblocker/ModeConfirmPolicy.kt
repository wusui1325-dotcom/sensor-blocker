package com.wusper.sensorblocker

/**
 * A successful restrict/enable transact has already changed SensorService.
 * A missing Mode line is only an unconfirmed display state, not a failed apply.
 */
data class ModeConfirmResult(
    val applied: Boolean,
    val mode: SensorServiceMode,
    val confirmed: Boolean
)

object ModeConfirmPolicy {
    fun assumed(desired: DesiredState): SensorServiceMode = when (desired) {
        is DesiredState.Normal -> SensorServiceMode.Normal(confirmed = false)
        is DesiredState.RestrictTo -> SensorServiceMode.Restricted(desired.packageName, confirmed = false)
    }

    fun matches(desired: DesiredState, mode: SensorServiceMode): Boolean {
        return when (desired) {
            is DesiredState.Normal -> mode is SensorServiceMode.Normal
            is DesiredState.RestrictTo ->
                mode is SensorServiceMode.Restricted &&
                    (mode.packageName == null || mode.packageName == desired.packageName)
        }
    }

    fun afterCommand(
        desired: DesiredState,
        commandSucceeded: Boolean,
        parsed: SensorServiceMode
    ): ModeConfirmResult {
        if (!commandSucceeded) {
            return ModeConfirmResult(applied = false, mode = parsed, confirmed = false)
        }
        return if (matches(desired, parsed)) {
            ModeConfirmResult(applied = true, mode = withConfirmed(parsed, true), confirmed = true)
        } else {
            ModeConfirmResult(applied = true, mode = assumed(desired), confirmed = false)
        }
    }

    /**
     * A later Mode-line read may confirm or contradict an already-applied command.
     * An unreadable dump must not be treated as a mismatch.
     */
    fun shouldReapplyAfterRecheck(desired: DesiredState, parsed: SensorServiceMode): Boolean {
        if (parsed is SensorServiceMode.Unknown) return false
        if (!parsed.confirmed) return false
        return !matches(desired, parsed)
    }

    private fun withConfirmed(mode: SensorServiceMode, confirmed: Boolean): SensorServiceMode {
        return when (mode) {
            is SensorServiceMode.Normal -> SensorServiceMode.Normal(confirmed)
            is SensorServiceMode.Restricted -> SensorServiceMode.Restricted(mode.packageName, confirmed)
            else -> mode
        }
    }
}
