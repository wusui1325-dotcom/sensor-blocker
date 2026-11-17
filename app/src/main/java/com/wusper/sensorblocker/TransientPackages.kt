package com.wusper.sensorblocker

/**
 * Packages that appear in the foreground briefly (recents, launcher, IME,
 * this app, permission / volume overlays) must not change the remembered
 * real app. Otherwise LIST mode would `enable` sensors in recents, and a
 * 摇一摇 ad could fire.
 */
object TransientPackages {
    val EXTRA_IGNORED: Set<String> = setOf(
        "android",
        "com.android.systemui",
        "com.android.intentresolver",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.vpndialogs",
        "com.android.systemui.accessibility.accessibilitymenu"
    )

    val KNOWN_LAUNCHERS: Set<String> = setOf(
        "com.android.launcher",
        "com.android.launcher2",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.google.android.apps.pixel.standalone.launcher",
        "com.miui.home",
        "com.mi.android.globallauncher",
        "com.oppo.launcher",
        "com.oplus.launcher",
        "com.coloros.launcher",
        "com.bbk.launcher2",
        "com.vivo.launcher",
        "com.huawei.android.launcher",
        "com.huawei.hivlauncher",
        "com.hihonor.android.launcher",
        "com.sec.android.app.launcher",
        "com.sec.android.app.twlauncher",
        "net.oneplus.launcher",
        "com.oneplus.launcher",
        "com.realme.launcher",
        "com.meizu.flyme.launcher",
        "com.smartisanos.launcher",
        "com.nothing.launcher",
        "com.microsoft.launcher",
        "com.teslacoilsw.launcher",
        "com.transsion.XOSLauncher",
        "com.transsion.hilauncher"
    )

    fun isTransient(
        packageName: String?,
        selfPackage: String,
        launchers: Set<String> = emptySet(),
        imes: Set<String> = emptySet()
    ): Boolean {
        if (packageName.isNullOrBlank()) return true
        if (selfPackage.isNotBlank() && packageName == selfPackage) return true
        if (packageName in EXTRA_IGNORED) return true
        if (packageName in KNOWN_LAUNCHERS) return true
        if (packageName in launchers) return true
        if (packageName in imes) return true
        return false
    }
}
