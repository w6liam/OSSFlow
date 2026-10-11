package app.utterdroid.a11y

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityEnabledTest {
    private val pkg = "app.utterdroid"
    private val cls = "app.utterdroid.a11y.UtterDroidAccessibilityService"

    @Test
    fun longFormMatches() {
        assertTrue(AccessibilityEnabled.listed("$pkg/$cls", pkg, cls))
    }

    @Test
    fun shortFormMatches() {
        assertTrue(AccessibilityEnabled.listed("$pkg/.a11y.UtterDroidAccessibilityService", pkg, cls))
    }

    @Test
    fun matchesBesideAnotherService() {
        val raw = "com.android.talkback/com.android.talkback.TalkBackService:$pkg/.a11y.UtterDroidAccessibilityService"
        assertTrue(AccessibilityEnabled.listed(raw, pkg, cls))
    }

    @Test
    fun unrelatedServiceDoesNotMatch() {
        assertFalse(AccessibilityEnabled.listed("com.other/.OtherService", pkg, cls))
    }

    @Test
    fun blankValueDoesNotMatch() {
        assertFalse(AccessibilityEnabled.listed(null, pkg, cls))
        assertFalse(AccessibilityEnabled.listed("", pkg, cls))
        assertFalse(AccessibilityEnabled.listed("   ", pkg, cls))
    }
}
