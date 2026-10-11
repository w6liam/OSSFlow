package app.utterdroid.overlay

object BubblePresence {
    fun visible(
        editableFocused: Boolean,
        password: Boolean,
        keyboardVisible: Boolean,
        phase: BubblePhase,
        accessibilityConnected: Boolean,
        snoozed: Boolean = false,
    ): Boolean {
        if (phase != BubblePhase.Idle) return true
        if (snoozed) return false
        if (!accessibilityConnected) return true
        if (password || !editableFocused || !keyboardVisible) return false
        return true
    }
}
