package com.wusper.sensorblocker

/**
 * Shizuku.bindUserService / unbindUserService must run on the main looper.
 * Background callers may wait for the posted work, but the main thread
 * itself must never block waiting for onServiceConnected.
 */
enum class MainThreadAction {
    RUN_INLINE,
    POST,
    POST_AND_WAIT
}

object MainThreadBindPolicy {
    fun action(isMainThread: Boolean, waitForCompletion: Boolean): MainThreadAction {
        if (isMainThread) return MainThreadAction.RUN_INLINE
        return if (waitForCompletion) MainThreadAction.POST_AND_WAIT else MainThreadAction.POST
    }

    fun mayBlockForConnection(isMainThread: Boolean): Boolean = !isMainThread
}
