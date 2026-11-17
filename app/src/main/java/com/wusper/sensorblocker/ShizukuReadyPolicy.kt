package com.wusper.sensorblocker

/**
 * Ready means Shizuku permission is granted and we can talk to
 * SensorService. The binder dump path is enough; a bound UserService
 * is optional.
 */
object ShizukuReadyPolicy {
    fun isReady(
        permissionGranted: Boolean,
        dumpPathReady: Boolean,
        shellConnected: Boolean
    ): Boolean {
        return permissionGranted && (dumpPathReady || shellConnected)
    }
}
