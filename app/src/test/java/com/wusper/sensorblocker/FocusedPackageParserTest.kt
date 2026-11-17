package com.wusper.sensorblocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusedPackageParserTest {
    @Test
    fun parsesCurrentFocus() {
        val line = "  mCurrentFocus=Window{abc u0 com.taobao.taobao/com.taobao.tao.TBMainActivity}"
        assertEquals("com.taobao.taobao", FocusedPackageParser.parse(line))
    }

    @Test
    fun parsesResumedActivity() {
        val line = "mResumedActivity: ActivityRecord{123 u0 com.autonavi.minimap/com.autonavi.map.activity.Splash}"
        assertEquals("com.autonavi.minimap", FocusedPackageParser.parse(line))
    }

    @Test
    fun rejectsJunk() {
        assertNull(FocusedPackageParser.parse("no component here"))
        assertNull(FocusedPackageParser.parse(""))
    }
}
