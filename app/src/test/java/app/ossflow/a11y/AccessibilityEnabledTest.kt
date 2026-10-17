package app.ossflow.a11y

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityEnabledTest {
    private val pkg = "app.ossflow"
    private val cls = "app.ossflow.a11y.OSSFlowAccessibilityService"

    @Test
    fun longFormMatches() {
        assertTrue(AccessibilityEnabled.listed("$pkg/$cls", pkg, cls))
    }

    @Test
    fun shortFormMatches() {
        assertTrue(AccessibilityEnabled.listed("$pkg/.a11y.OSSFlowAccessibilityService", pkg, cls))
    }

    @Test
    fun matchesBesideAnotherService() {
        val raw = "com.android.talkback/com.android.talkback.TalkBackService:$pkg/.a11y.OSSFlowAccessibilityService"
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
