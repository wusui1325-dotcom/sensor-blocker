package com.wusper.sensorblocker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplyLoopPolicyTest {
    private val restrict = DesiredState.RestrictTo("com.wusper.sensorblocker")

    @Test
    fun revertsFallbackOnlyWhenReturningToNormal() {
        assertTrue(ApplyLoopPolicy.shouldRevertPrivacyFallback(DesiredState.Normal, true))
        assertFalse(ApplyLoopPolicy.shouldRevertPrivacyFallback(DesiredState.Normal, false))
        assertFalse(ApplyLoopPolicy.shouldRevertPrivacyFallback(restrict, true))
    }

    @Test
    fun doesNotRetryPrivacyFallbackInATightLoop() {
        assertTrue(ApplyLoopPolicy.shouldAttemptPrivacyFallback(restrict, alreadyEngaged = false, alreadyAttemptedForDesired = false))
        assertFalse(ApplyLoopPolicy.shouldAttemptPrivacyFallback(restrict, alreadyEngaged = true, alreadyAttemptedForDesired = false))
        assertFalse(ApplyLoopPolicy.shouldAttemptPrivacyFallback(restrict, alreadyEngaged = false, alreadyAttemptedForDesired = true))
        assertFalse(ApplyLoopPolicy.shouldAttemptPrivacyFallback(DesiredState.Normal, alreadyEngaged = false, alreadyAttemptedForDesired = false))
    }

    @Test
    fun skipsReapplyWhenStateUnchangedAndAlreadyHandled() {
        assertTrue(
            ApplyLoopPolicy.shouldSkipReapply(restrict, restrict, lastApplyOk = true, fallbackAttemptedForLast = false, force = false)
        )
        assertTrue(
            ApplyLoopPolicy.shouldSkipReapply(restrict, restrict, lastApplyOk = false, fallbackAttemptedForLast = true, force = false)
        )
        assertFalse(
            ApplyLoopPolicy.shouldSkipReapply(DesiredState.Normal, restrict, lastApplyOk = true, fallbackAttemptedForLast = false, force = false)
        )
        assertFalse(
            ApplyLoopPolicy.shouldSkipReapply(restrict, restrict, lastApplyOk = false, fallbackAttemptedForLast = false, force = false)
        )
        assertFalse(
            ApplyLoopPolicy.shouldSkipReapply(restrict, restrict, lastApplyOk = true, fallbackAttemptedForLast = true, force = true)
        )
    }

    @Test
    fun confirmsModeOnlyWhenNeeded() {
        assertTrue(ApplyLoopPolicy.shouldConfirmMode(force = true, firstApplyForDesired = false, commandFailed = false))
        assertTrue(ApplyLoopPolicy.shouldConfirmMode(force = false, firstApplyForDesired = true, commandFailed = false))
        assertTrue(ApplyLoopPolicy.shouldConfirmMode(force = false, firstApplyForDesired = false, commandFailed = true))
        assertFalse(ApplyLoopPolicy.shouldConfirmMode(force = false, firstApplyForDesired = false, commandFailed = false))
    }
}
