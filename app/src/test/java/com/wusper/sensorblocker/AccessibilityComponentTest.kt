package com.wusper.sensorblocker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityComponentTest {
    private val pkg = "com.wusper.sensorblocker"
    private val cls = "com.wusper.sensorblocker.WindowChangeAccessibilityService"

    @Test
    fun acceptsFullyQualifiedAndShortComponentNames() {
        assertTrue(AccessibilityComponent.isListed("$pkg/$cls", pkg, cls))
        assertTrue(
            AccessibilityComponent.isListed(
                "$pkg/.WindowChangeAccessibilityService",
                pkg,
                cls
            )
        )
        assertTrue(
            AccessibilityComponent.isListed(
                "foo/bar:$pkg/.WindowChangeAccessibilityService:baz/qux",
                pkg,
                cls
            )
        )
        assertTrue(
            AccessibilityComponent.isListed(
                "foo/bar;$pkg/$cls",
                pkg,
                cls
            )
        )
    }

    @Test
    fun rejectsMissingOrDifferentComponents() {
        assertFalse(AccessibilityComponent.isListed(null, pkg, cls))
        assertFalse(AccessibilityComponent.isListed("", pkg, cls))
        assertFalse(AccessibilityComponent.isListed("com.other/.WindowChangeAccessibilityService", pkg, cls))
        assertFalse(AccessibilityComponent.isListed("$pkg/.OtherService", pkg, cls))
    }
}
