package com.wusper.sensorblocker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendedAppsTest {
    @Test
    fun shoppingShortVideoAndNewsAreBlockCandidates() {
        assertTrue(RecommendedApps.isBlockCandidate("com.taobao.taobao"))
        assertTrue(RecommendedApps.isBlockCandidate("com.xunmeng.pinduoduo"))
        assertTrue(RecommendedApps.isBlockCandidate("com.ss.android.ugc.aweme"))
        assertTrue(RecommendedApps.isBlockCandidate("com.smile.gifmaker"))
        assertTrue(RecommendedApps.isBlockCandidate("com.ss.android.article.news"))
    }

    @Test
    fun mapsPaymentWeChatAndQqAreNeverAutoBlocked() {
        assertFalse(RecommendedApps.isBlockCandidate("com.autonavi.minimap"))
        assertFalse(RecommendedApps.isBlockCandidate("com.baidu.BaiduMap"))
        assertFalse(RecommendedApps.isBlockCandidate("com.tencent.map"))
        assertFalse(RecommendedApps.isBlockCandidate("com.eg.android.AlipayGphone"))
        assertFalse(RecommendedApps.isBlockCandidate("com.tencent.mm"))
        assertFalse(RecommendedApps.isBlockCandidate("com.tencent.mobileqq"))
        assertTrue("com.tencent.mm" in RecommendedApps.doNotAutoBlock)
        assertTrue("com.eg.android.AlipayGphone" in RecommendedApps.doNotAutoBlock)
    }

    @Test
    fun mapsAreAllowCandidates() {
        assertTrue(RecommendedApps.isAllowCandidate("com.autonavi.minimap"))
        assertTrue(RecommendedApps.isAllowCandidate("com.baidu.BaiduMap"))
        assertFalse(RecommendedApps.isAllowCandidate("com.taobao.taobao"))
    }

    @Test
    fun firstLaunchOnlyAutoSelectsBlockCandidatesThatAreInstalled() {
        val installed = setOf(
            "com.taobao.taobao",
            "com.autonavi.minimap",
            "com.tencent.mm",
            "com.eg.android.AlipayGphone",
            "com.ss.android.ugc.aweme"
        )
        val targets = RecommendedApps.firstLaunchBlockTargets(installed)
        assertTrue("com.taobao.taobao" in targets)
        assertTrue("com.ss.android.ugc.aweme" in targets)
        assertFalse("com.autonavi.minimap" in targets)
        assertFalse("com.tencent.mm" in targets)
        assertFalse("com.eg.android.AlipayGphone" in targets)
    }
}
