package com.wusper.sensorblocker

enum class ShizukuPermissionState {
    NO_BINDER,
    GRANTED,
    DENIED,
    CHECK_FAILED
}

enum class ShizukuUiState {
    DISCONNECTED,
    NEED_PERMISSION,
    PERMISSION_ERROR,
    BINDING,
    BIND_FAILED,
    DUMP_FAILED,
    READY
}

data class ShizukuSnapshot(
    val binderAlive: Boolean,
    val permission: ShizukuPermissionState,
    val shellConnected: Boolean,
    val bindInProgress: Boolean,
    val lastBindError: String?,
    val lastPermissionError: String?,
    val dumpPathReady: Boolean = false,
    val lastDumpError: String? = null
)

/**
 * Maps raw binder / permission / UserService state to what the UI may show.
 * 「需要授权」 is only used when the binder is up and permission is actually denied.
 */
object ShizukuUiStatus {
    fun state(snapshot: ShizukuSnapshot): ShizukuUiState {
        if (!snapshot.binderAlive) return ShizukuUiState.DISCONNECTED
        return when (snapshot.permission) {
            ShizukuPermissionState.NO_BINDER -> ShizukuUiState.DISCONNECTED
            ShizukuPermissionState.CHECK_FAILED -> ShizukuUiState.PERMISSION_ERROR
            ShizukuPermissionState.DENIED -> ShizukuUiState.NEED_PERMISSION
            ShizukuPermissionState.GRANTED -> when {
                snapshot.dumpPathReady || snapshot.shellConnected -> ShizukuUiState.READY
                snapshot.lastDumpError != null && !snapshot.bindInProgress ->
                    ShizukuUiState.DUMP_FAILED
                snapshot.lastBindError != null && !snapshot.bindInProgress ->
                    ShizukuUiState.BIND_FAILED
                else -> ShizukuUiState.BINDING
            }
        }
    }
}
