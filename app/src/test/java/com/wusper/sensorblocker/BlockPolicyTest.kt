package com.wusper.sensorblocker

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockPolicyTest {
    private val self = "com.wusper.sensorblocker"
    private val maps = "com.autonavi.minimap"
    private val shop = "com.taobao.taobao"

    @Test
    fun globalRestrictsToSelfByDefault() {
        assertEquals(
            DesiredState.RestrictTo(self),
            BlockPolicy.decide(BlockMode.GLOBAL, null, setOf(maps), setOf(shop), self)
        )
        assertEquals(
            DesiredState.RestrictTo(self),
            BlockPolicy.decide(BlockMode.GLOBAL, shop, setOf(maps), emptySet(), self)
        )
    }

    @Test
    fun globalTemporarilyRestrictsToAllowlistedForeground() {
        assertEquals(
            DesiredState.RestrictTo(maps),
            BlockPolicy.decide(BlockMode.GLOBAL, maps, setOf(maps), emptySet(), self)
        )
    }

    @Test
    fun listRestrictsOnlyWhenBlocklistedAppIsForeground() {
        assertEquals(
            DesiredState.RestrictTo(self),
            BlockPolicy.decide(BlockMode.LIST, shop, emptySet(), setOf(shop), self)
        )
        assertEquals(
            DesiredState.Normal,
            BlockPolicy.decide(BlockMode.LIST, maps, emptySet(), setOf(shop), self)
        )
        assertEquals(
            DesiredState.Normal,
            BlockPolicy.decide(BlockMode.LIST, null, emptySet(), setOf(shop), self)
        )
    }

    @Test
    fun parseModeDefaultsToGlobal() {
        assertEquals(BlockMode.GLOBAL, BlockPolicy.parseMode(null))
        assertEquals(BlockMode.GLOBAL, BlockPolicy.parseMode("GLOBAL"))
        assertEquals(BlockMode.LIST, BlockPolicy.parseMode("list"))
    }
}
