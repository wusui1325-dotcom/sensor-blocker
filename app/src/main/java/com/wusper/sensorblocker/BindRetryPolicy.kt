package com.wusper.sensorblocker

/**
 * UserService bind timing. A first connect may be slower on some OEM
 * phones, but after a miss we must surface BIND_FAILED and back off
 * instead of rebinding every few seconds (which keeps the UI on
 * 「正在连接」 forever).
 */
object BindRetryPolicy {
    const val FIRST_TIMEOUT_MS = 8_000L
    const val LATER_TIMEOUT_MS = 6_000L
    const val MAX_BACKOFF_MS = 30_000L

    fun timeoutMs(failedAttempts: Int): Long {
        return if (failedAttempts <= 0) FIRST_TIMEOUT_MS else LATER_TIMEOUT_MS
    }

    fun backoffMs(failedAttempts: Int): Long {
        val count = failedAttempts.coerceAtLeast(1)
        val delay = 4_000L shl (count - 1).coerceAtMost(3)
        return delay.coerceAtMost(MAX_BACKOFF_MS)
    }

    fun shouldStartBind(now: Long, nextRetryAt: Long, force: Boolean): Boolean {
        if (force) return true
        return now >= nextRetryAt
    }
}
