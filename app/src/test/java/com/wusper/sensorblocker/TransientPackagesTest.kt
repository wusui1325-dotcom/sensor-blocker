package com.wusper.sensorblocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransientPackagesTest {
    private val self = "com.wusper.sensorblocker"
    private val shop = "com.taobao.taobao"
    private val maps = "com.autonavi.minimap"

    @Test
    fun launcherImeSelfAndOverlaysAreTransient() {
        assertTrue(TransientPackages.isTransient("com.miui.home", self))
        assertTrue(TransientPackages.isTransient("com.oppo.launcher", self))
        assertTrue(TransientPackages.isTransient("com.google.android.apps.nexuslauncher", self))
        assertTrue(TransientPackages.isTransient("com.android.systemui", self))
        assertTrue(TransientPackages.isTransient(self, self))
        assertTrue(
            TransientPackages.isTransient(
                "com.google.android.inputmethod.latin",
                self,
                imes = setOf("com.google.android.inputmethod.latin")
            )
        )
        assertTrue(
            TransientPackages.isTransient(
                "com.foo.home",
                self,
                launchers = setOf("com.foo.home")
            )
        )
        assertTrue(TransientPackages.isTransient(null, self))
        assertTrue(TransientPackages.isTransient("", self))
        assertFalse(TransientPackages.isTransient(shop, self))
        assertFalse(TransientPackages.isTransient(maps, self))
    }

    @Test
    fun memoryKeepsLastRealAppWhenLauncherOrImeAppears() {
        val mem = ForegroundMemory(
            selfPackage = self,
            launchers = setOf("com.miui.home"),
            imes = setOf("com.sohu.inputmethod.sogou")
        )
        assertTrue(mem.update(shop, ForegroundSourcePriority.ACCESSIBILITY))
        assertEquals(shop, mem.current)
        assertFalse(mem.update("com.miui.home", ForegroundSourcePriority.ACCESSIBILITY))
        assertEquals(shop, mem.current)
        assertFalse(mem.update("com.sohu.inputmethod.sogou", ForegroundSourcePriority.SHIZUKU))
        assertEquals(shop, mem.current)
        assertFalse(mem.update(self, ForegroundSourcePriority.ACCESSIBILITY))
        assertEquals(shop, mem.current)

        // In LIST mode this must still look like the blocked shop is in front.
        assertEquals(
            DesiredState.RestrictTo(self),
            BlockPolicy.decide(BlockMode.LIST, mem.current, emptySet(), setOf(shop), self)
        )
    }

    @Test
    fun memoryUpdatesWhenARealAppTakesTheForeground() {
        val mem = ForegroundMemory(selfPackage = self, launchers = setOf("com.miui.home"))
        mem.update(shop, ForegroundSourcePriority.ACCESSIBILITY)
        mem.update("com.miui.home", ForegroundSourcePriority.ACCESSIBILITY)
        assertTrue(mem.update(maps, ForegroundSourcePriority.ACCESSIBILITY))
        assertEquals(maps, mem.current)
        assertEquals(
            DesiredState.Normal,
            BlockPolicy.decide(BlockMode.LIST, mem.current, emptySet(), setOf(shop), self)
        )
    }
}
