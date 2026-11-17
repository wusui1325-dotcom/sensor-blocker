package com.wusper.sensorblocker

/**
 * Firmwares store enabled accessibility services as either
 * `pkg/fully.qualified.Class` or `pkg/.ShortClassName`.
 */
object AccessibilityComponent {
    fun isListed(enabledSetting: String?, packageName: String, className: String): Boolean {
        if (enabledSetting.isNullOrBlank() || packageName.isBlank() || className.isBlank()) {
            return false
        }
        val shortClass = if (className.startsWith("$packageName.")) {
            ".${className.removePrefix("$packageName.")}"
        } else {
            ".${className.substringAfterLast('.')}"
        }
        val full = "$packageName/$className"
        val shortFlat = "$packageName/$shortClass"
        return enabledSetting.split(':', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .any { it.equals(full, ignoreCase = true) || it.equals(shortFlat, ignoreCase = true) }
    }
}
