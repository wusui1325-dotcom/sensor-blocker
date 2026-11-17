package com.wusper.sensorblocker

data class AppInfo(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val isRecommendedBlock: Boolean,
    val isRecommendedAllow: Boolean
)
