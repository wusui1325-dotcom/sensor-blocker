package com.wusper.sensorblocker

import android.content.Context
import android.content.SharedPreferences

object AppPreferences {
    const val PREFS_NAME = "sensor_blocker"
    const val KEY_MODE = "block_mode"
    const val KEY_BLOCKED = "blocked_packages"
    const val KEY_ALLOWED = "allowed_packages"
    const val KEY_MONITORING = "monitoring_enabled"
    const val KEY_RECOMMENDATION_SHOWN = "recommendation_shown"
    const val KEY_BATTERY_HINT_SHOWN = "battery_hint_shown"

    fun prefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun blockMode(context: Context): BlockMode {
        return BlockPolicy.parseMode(prefs(context).getString(KEY_MODE, BlockMode.GLOBAL.name))
    }

    fun setBlockMode(context: Context, mode: BlockMode) {
        prefs(context).edit().putString(KEY_MODE, mode.name).apply()
    }

    fun blockedPackages(context: Context): Set<String> {
        return HashSet(prefs(context).getStringSet(KEY_BLOCKED, emptySet()) ?: emptySet())
    }

    fun allowedPackages(context: Context): Set<String> {
        return HashSet(prefs(context).getStringSet(KEY_ALLOWED, emptySet()) ?: emptySet())
    }

    fun setBlockedPackages(context: Context, packages: Set<String>) {
        prefs(context).edit().putStringSet(KEY_BLOCKED, HashSet(packages)).apply()
    }

    fun setAllowedPackages(context: Context, packages: Set<String>) {
        prefs(context).edit().putStringSet(KEY_ALLOWED, HashSet(packages)).apply()
    }

    fun isMonitoringEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_MONITORING, false)
    }

    fun setMonitoringEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MONITORING, enabled).apply()
    }

    fun recommendationShown(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_RECOMMENDATION_SHOWN, false)
    }

    fun setRecommendationShown(context: Context, shown: Boolean) {
        prefs(context).edit().putBoolean(KEY_RECOMMENDATION_SHOWN, shown).apply()
    }

    fun batteryHintShown(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_BATTERY_HINT_SHOWN, false)
    }

    fun setBatteryHintShown(context: Context, shown: Boolean) {
        prefs(context).edit().putBoolean(KEY_BATTERY_HINT_SHOWN, shown).apply()
    }
}
