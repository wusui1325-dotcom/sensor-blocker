package com.wusper.sensorblocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessIdentityTest {
    private val pkg = "com.wusper.sensorblocker"

    @Test
    fun defaultProcessIsMain() {
        assertTrue(ProcessIdentity.isDefaultAppProcess(pkg, pkg))
        assertFalse(ProcessIdentity.isUserServiceProcess(pkg))
    }

    @Test
    fun shellSuffixIsNotMain() {
        val name = "$pkg:shell"
        assertTrue(ProcessIdentity.isUserServiceProcess(name))
        assertFalse(ProcessIdentity.isDefaultAppProcess(name, pkg))
    }

    @Test
    fun otherSecondaryProcessIsNotMain() {
        assertFalse(ProcessIdentity.isDefaultAppProcess("$pkg:other", pkg))
        assertFalse(ProcessIdentity.isUserServiceProcess("$pkg:other"))
    }

    @Test
    fun emptyNameAssumesMainSoRealAppCanStillStart() {
        assertTrue(ProcessIdentity.isDefaultAppProcess("", pkg))
        assertTrue(ProcessIdentity.isDefaultAppProcess(null, pkg))
        assertFalse(ProcessIdentity.isUserServiceProcess(""))
        assertFalse(ProcessIdentity.isUserServiceProcess(null))
    }

    @Test
    fun whitespaceIsTrimmed() {
        assertTrue(ProcessIdentity.isDefaultAppProcess("  $pkg  ", pkg))
        assertTrue(ProcessIdentity.isUserServiceProcess("  $pkg:shell\n"))
        assertFalse(ProcessIdentity.isDefaultAppProcess("  $pkg:shell  ", pkg))
    }

    @Test
    fun parseCmdlineStopsAtNul() {
        val raw = "com.wusper.sensorblocker:shell\u0000junk".toByteArray(Charsets.UTF_8)
        assertEquals("com.wusper.sensorblocker:shell", ProcessIdentity.parseCmdline(raw))
        assertFalse(
            ProcessIdentity.isDefaultAppProcess(ProcessIdentity.parseCmdline(raw), pkg)
        )
    }

    @Test
    fun parseCmdlineMainProcess() {
        val raw = "com.wusper.sensorblocker\u0000".toByteArray(Charsets.UTF_8)
        assertEquals(pkg, ProcessIdentity.parseCmdline(raw))
        assertTrue(ProcessIdentity.isDefaultAppProcess(ProcessIdentity.parseCmdline(raw), pkg))
    }

    @Test
    fun parseCmdlineWithoutTerminator() {
        assertEquals(pkg, ProcessIdentity.parseCmdline(pkg.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun currentProcessNameFallsBackToCmdlineOnApi26() {
        val bytes = "com.wusper.sensorblocker:shell\u0000".toByteArray(Charsets.UTF_8)
        val name = ProcessIdentity.currentProcessName(apiLevel = 26) { bytes }
        assertEquals("com.wusper.sensorblocker:shell", name)
        assertFalse(ProcessIdentity.isDefaultAppProcess(name, pkg))
    }
}
