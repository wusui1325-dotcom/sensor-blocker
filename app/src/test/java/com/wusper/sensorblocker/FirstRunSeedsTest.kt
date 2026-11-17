package com.wusper.sensorblocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunSeedsTest {
    private val installed = setOf(
        "com.taobao.taobao",
        "com.ss.android.ugc.aweme",
        "com.autonavi.minimap",
        "com.tencent.mm",
        "com.eg.android.AlipayGphone",
        "com.tencent.mobileqq"
    )

    @Test
    fun globalOffersAllowlistMapsAndSeedsBlocklistForLater() {
        val offer = FirstRunSeeds.forMode(BlockMode.GLOBAL, installed)
        assertEquals(BlockMode.GLOBAL, offer.mode)
        assertTrue("com.autonavi.minimap" in offer.allowPackages)
        assertTrue("com.taobao.taobao" in offer.blockPackages)
        assertTrue("com.ss.android.ugc.aweme" in offer.blockPackages)
        assertFalse("com.autonavi.minimap" in offer.blockPackages)
        assertFalse("com.tencent.mm" in offer.blockPackages)
        assertFalse("com.eg.android.AlipayGphone" in offer.blockPackages)
        assertFalse("com.tencent.mobileqq" in offer.blockPackages)
        assertFalse("com.tencent.mm" in offer.allowPackages)
    }

    @Test
    fun listOffersOnlyShoppingVideoNewsBlocklist() {
        val offer = FirstRunSeeds.forMode(BlockMode.LIST, installed)
        assertTrue(offer.allowPackages.isEmpty())
        assertTrue("com.taobao.taobao" in offer.blockPackages)
        assertTrue("com.ss.android.ugc.aweme" in offer.blockPackages)
        assertFalse("com.autonavi.minimap" in offer.blockPackages)
        assertFalse("com.tencent.mm" in offer.blockPackages)
        assertFalse("com.eg.android.AlipayGphone" in offer.blockPackages)
    }

    @Test
    fun emptyWhenNothingRelevantIsInstalled() {
        assertTrue(FirstRunSeeds.forMode(BlockMode.GLOBAL, setOf("com.tencent.mm")).isEmpty)
        assertTrue(FirstRunSeeds.forMode(BlockMode.LIST, setOf("com.autonavi.minimap")).isEmpty)
    }
}
