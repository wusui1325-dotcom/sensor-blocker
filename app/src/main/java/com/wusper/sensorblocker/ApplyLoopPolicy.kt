package com.wusper.sensorblocker

/**
 * Controls when to retry SensorService apply / the camera-mic privacy fallback.
 */
object ApplyLoopPolicy {
    fun shouldAttemptPrivacyFallback(
        desired: DesiredState,
        alreadyEngaged: Boolean,
        alreadyAttemptedForDesired: Boolean
    ): Boolean {
        return desired is DesiredState.RestrictTo &&
            !alreadyEngaged &&
            !alreadyAttemptedForDesired
    }

    fun shouldRevertPrivacyFallback(desired: DesiredState, engaged: Boolean): Boolean {
        return desired is DesiredState.Normal && engaged
    }

    /**
     * Skip re-apply when the desired state is unchanged and we already
     * succeeded, or already tried the last-resort fallback for this state.
     * A complete failure can be retried on a slow heartbeat via [force].
     */
    /**
     * Re-query SensorService mode after a command only when needed:
     * first apply of this desired state, a failed command, or a forced checksum.
     */
    fun shouldConfirmMode(
        force: Boolean,
        firstApplyForDesired: Boolean,
        commandFailed: Boolean
    ): Boolean {
        return force || firstApplyForDesired || commandFailed
    }

    fun shouldSkipReapply(
        desired: DesiredState,
        lastDesired: DesiredState?,
        lastApplyOk: Boolean,
        fallbackAttemptedForLast: Boolean,
        force: Boolean
    ): Boolean {
        if (force) return false
        if (desired != lastDesired) return false
        return lastApplyOk || fallbackAttemptedForLast
    }
}
