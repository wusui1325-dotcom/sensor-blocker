package com.wusper.sensorblocker

data class FirstRunOffer(
    val mode: BlockMode,
    val allowPackages: Set<String>,
    val blockPackages: Set<String>
) {
    val isEmpty: Boolean get() = allowPackages.isEmpty() && blockPackages.isEmpty()
}

/**
 * First-launch suggestions must match the current mode.
 * GLOBAL: offer maps/games for the allowlist, and may also seed the LIST
 * blocklist for later. LIST: offer shopping / short-video / news for the
 * blocklist. Maps, Alipay, WeChat and QQ are never auto-blocked.
 */
object FirstRunSeeds {
    fun forMode(mode: BlockMode, installed: Set<String>): FirstRunOffer {
        val block = installed.filter { RecommendedApps.isBlockCandidate(it) }.toSet()
        val allow = installed.filter { RecommendedApps.isAllowCandidate(it) }.toSet()
        return when (mode) {
            BlockMode.GLOBAL -> FirstRunOffer(
                mode = mode,
                allowPackages = allow,
                blockPackages = block
            )
            BlockMode.LIST -> FirstRunOffer(
                mode = mode,
                allowPackages = emptySet(),
                blockPackages = block
            )
        }
    }
}
