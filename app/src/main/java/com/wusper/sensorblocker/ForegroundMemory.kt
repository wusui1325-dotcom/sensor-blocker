package com.wusper.sensorblocker

/**
 * Remembers the last real (non-transient) foreground app.
 * Transient packages and lower-priority stale sources are ignored.
 */
class ForegroundMemory(
    var selfPackage: String = "",
    var launchers: Set<String> = emptySet(),
    var imes: Set<String> = emptySet(),
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    var current: String? = null
        private set
    var source: String = "none"
        private set
    var updatedAt: Long = 0L
        private set

    fun isTransient(packageName: String?): Boolean {
        return TransientPackages.isTransient(packageName, selfPackage, launchers, imes)
    }

    /**
     * @return true if the remembered real app changed
     */
    fun update(packageName: String?, newSource: String): Boolean {
        if (isTransient(packageName)) return false
        val now = clock()
        if (current != null &&
            !ForegroundSourcePriority.shouldAccept(newSource, source, now - updatedAt)
        ) {
            return false
        }
        if (packageName == current) {
            source = newSource
            updatedAt = now
            return false
        }
        current = packageName
        source = newSource
        updatedAt = now
        return true
    }
}
