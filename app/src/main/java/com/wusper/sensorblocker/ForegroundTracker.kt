package com.wusper.sensorblocker

import java.util.concurrent.CopyOnWriteArrayList

object ForegroundTracker {
    private val memory = ForegroundMemory()
    private val listeners = CopyOnWriteArrayList<(String?) -> Unit>()

    val current: String? get() = memory.current
    val source: String get() = memory.source
    val updatedAt: Long get() = memory.updatedAt

    fun configure(selfPackage: String, launchers: Set<String>, imes: Set<String>) {
        synchronized(memory) {
            memory.selfPackage = selfPackage
            memory.launchers = launchers
            memory.imes = imes
        }
    }

    fun addListener(listener: (String?) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (String?) -> Unit) {
        listeners.remove(listener)
    }

    fun update(packageName: String?, source: String): Boolean {
        val changed = synchronized(memory) { memory.update(packageName, source) }
        if (changed) {
            val value = memory.current
            listeners.forEach { it(value) }
        }
        return changed
    }
}
