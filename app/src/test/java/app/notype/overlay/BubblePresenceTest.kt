package app.notype.overlay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BubblePresenceTest {
    @Test
    fun hiddenWithNoField() {
        assertFalse(visible(editableFocused = false, password = false, keyboardVisible = true))
    }

    @Test
    fun hiddenWhenTheKeyboardIsDown() {
        assertFalse(visible(editableFocused = true, password = false, keyboardVisible = false))
    }

    @Test
    fun hiddenOnAPasswordField() {
        assertFalse(visible(editableFocused = true, password = true, keyboardVisible = true))
    }

    @Test
    fun shownWhenAFieldAndTheKeyboardAreUp() {
        assertTrue(visible(editableFocused = true, password = false, keyboardVisible = true))
    }

    @Test
    fun staysUpWhileRecordingOrTranscribing() {
        assertTrue(
            visible(
                editableFocused = false,
                password = false,
                keyboardVisible = false,
                phase = BubblePhase.Recording,
            ),
        )
        assertTrue(
            visible(
                editableFocused = false,
                password = false,
                keyboardVisible = false,
                phase = BubblePhase.Working,
            ),
        )
    }

    @Test
    fun hidesAgainOnceIdleReturns() {
        assertFalse(
            visible(
                editableFocused = false,
                password = false,
                keyboardVisible = false,
                phase = BubblePhase.Idle,
            ),
        )
    }

    @Test
    fun snoozeHidesTheIdleBubble() {
        assertFalse(
            visible(
                editableFocused = true,
                password = false,
                keyboardVisible = true,
                snoozed = true,
            ),
        )
    }

    @Test
    fun endingSnoozeShowsTheBubbleAgain() {
        assertTrue(
            visible(
                editableFocused = true,
                password = false,
                keyboardVisible = true,
                snoozed = false,
            ),
        )
    }

    @Test
    fun snoozeDoesNotHideAnActiveDictation() {
        assertTrue(
            visible(
                editableFocused = false,
                password = false,
                keyboardVisible = false,
                phase = BubblePhase.Recording,
                snoozed = true,
            ),
        )
    }

    @Test
    fun snoozeHidesTheBubbleWhenAccessibilityIsOff() {
        assertFalse(
            visible(
                editableFocused = false,
                password = false,
                keyboardVisible = false,
                accessibilityConnected = false,
                snoozed = true,
            ),
        )
    }

    @Test
    fun staysVisibleWithoutAccessibility() {
        assertTrue(
            visible(
                editableFocused = false,
                password = false,
                keyboardVisible = false,
                accessibilityConnected = false,
            ),
        )
    }

    private fun visible(
        editableFocused: Boolean,
        password: Boolean,
        keyboardVisible: Boolean,
        phase: BubblePhase = BubblePhase.Idle,
        accessibilityConnected: Boolean = true,
        snoozed: Boolean = false,
    ) = BubblePresence.visible(
        editableFocused = editableFocused,
        password = password,
        keyboardVisible = keyboardVisible,
        phase = phase,
        accessibilityConnected = accessibilityConnected,
        snoozed = snoozed,
    )
}
