package com.wusper.sensorblocker

import java.util.concurrent.CopyOnWriteArrayList

data class MonitorState(
    val monitoring: Boolean,
    val shizukuReady: Boolean,
    val shizukuBound: Boolean,
    val shizukuPermission: Boolean,
    val sensorMode: SensorServiceMode,
    val foregroundPackage: String?,
    val usedPrivacyFallback: Boolean
)

object MonitorStateHub {
    @Volatile var latest: MonitorState? = null
        private set
    private val listeners = CopyOnWriteArrayList<(MonitorState) -> Unit>()

    fun addListener(listener: (MonitorState) -> Unit) {
        listeners.add(listener)
        latest?.let { listener(it) }
    }

    fun removeListener(listener: (MonitorState) -> Unit) {
        listeners.remove(listener)
    }

    fun update(state: MonitorState) {
        latest = state
        listeners.forEach { it(state) }
    }
}
