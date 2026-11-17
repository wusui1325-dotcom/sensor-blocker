package com.wusper.sensorblocker

import org.junit.Assert.assertEquals
import org.junit.Test

class ShizukuUiStatusTest {
    private fun snap(
        binder: Boolean = true,
        permission: ShizukuPermissionState = ShizukuPermissionState.GRANTED,
        shell: Boolean = false,
        binding: Boolean = false,
        bindError: String? = null,
        permError: String? = null
    ) = ShizukuSnapshot(binder, permission, shell, binding, bindError, permError)

    @Test
    fun disconnectedWhenBinderDown() {
        assertEquals(
            ShizukuUiState.DISCONNECTED,
            ShizukuUiStatus.state(snap(binder = false, permission = ShizukuPermissionState.NO_BINDER))
        )
    }

    @Test
    fun needPermissionOnlyWhenDenied() {
        assertEquals(
            ShizukuUiState.NEED_PERMISSION,
            ShizukuUiStatus.state(snap(permission = ShizukuPermissionState.DENIED))
        )
    }

    @Test
    fun grantedButUnboundIsNotNeedPermission() {
        assertEquals(
            ShizukuUiState.BINDING,
            ShizukuUiStatus.state(snap(shell = false, binding = true))
        )
        assertEquals(
            ShizukuUiState.BIND_FAILED,
            ShizukuUiStatus.state(
                snap(shell = false, binding = false, bindError = "bind failed: SecurityException")
            )
        )
        assertEquals(
            ShizukuUiState.READY,
            ShizukuUiStatus.state(snap(shell = true))
        )
    }

    @Test
    fun permissionCheckFailureIsDistinct() {
        assertEquals(
            ShizukuUiState.PERMISSION_ERROR,
            ShizukuUiStatus.state(
                snap(
                    permission = ShizukuPermissionState.CHECK_FAILED,
                    permError = "IllegalStateException"
                )
            )
        )
    }

    @Test
    fun bindingInProgressPreferredOverStaleError() {
        assertEquals(
            ShizukuUiState.BINDING,
            ShizukuUiStatus.state(
                snap(binding = true, bindError = "previous timeout")
            )
        )
    }
}
