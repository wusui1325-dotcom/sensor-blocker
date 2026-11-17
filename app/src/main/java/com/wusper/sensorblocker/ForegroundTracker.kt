package com.wusper.sensorblocker

import java.util.concurrent.CopyOnWriteArrayList

object ForegroundTracker {
    private val ignored = setOf(
        "android",
        "com.android.systemui",
        "com.android.intentresolver",
        "com.android.permissioncontroller"
    )

    @Volatile var current: String? = null
        private set
    @Volatile var source: String = "none"
        private set
    @Volatile var updatedAt: Long = 0L
        private set

    private val listeners = CopyOnWriteArrayList<(String?) -> Unit>()

    fun addListener(listener: (String?) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (String?) -> Unit) {
        listeners.remove(listener)
    }

    fun shouldIgnore(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return true
        return packageName in ignored
    }

    fun update(packageName: String?, source: String) {
        if (shouldIgnore(packageName)) return
        if (packageName == current) {
            this.source = source
            updatedAt = System.currentTimeMillis()
            return
        }
        current = packageName
        this.source = source
        updatedAt = System.currentTimeMillis()
        listeners.forEach { it(packageName) }
    }
}
