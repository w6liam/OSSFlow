package app.ossflow.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class CleanupTextTest {
    @Test
    fun stripsALeadingNoteLabel() {
        assertEquals(
            "Meet at six.",
            CleanupText.withoutLabel("Note: Meet at six.", "um meet at six"),
        )
    }

    @Test
    fun stripsAMessageLabelOnItsOwnLine() {
        assertEquals(
            "Buy milk.",
            CleanupText.withoutLabel("Message\nBuy milk.", "buy milk"),
        )
    }

    @Test
    fun keepsANoteTheSpeakerActuallyStartedWith() {
        assertEquals(
            "Note: buy milk.",
            CleanupText.withoutLabel("Note: buy milk.", "note buy milk"),
        )
    }

    @Test
    fun leavesOrdinaryTextAlone() {
        assertEquals("See you there.", CleanupText.withoutLabel("See you there.", "see you there"))
    }

    @Test
    fun stripsAnUnspokenMessageBeforeASpace() {
        assertEquals("Buy milk.", CleanupText.withoutLabel("Message Buy milk.", "buy milk"))
    }

    @Test
    fun stripsAnUnspokenMessageBeforeAPeriod() {
        assertEquals("Buy milk.", CleanupText.withoutLabel("Message. Buy milk.", "buy milk"))
    }

    @Test
    fun stripsAQuotedMessageLabel() {
        assertEquals("Buy milk.", CleanupText.withoutLabel("\"Message: Buy milk.\"", "buy milk"))
    }

    @Test
    fun stripsAPreambleWhenTheLabelWasNotSpoken() {
        assertEquals(
            "Buy milk.",
            CleanupText.withoutLabel("Here's your message: Buy milk.", "buy milk"),
        )
    }

    @Test
    fun keepsMessageWhenTheSpeakerSaidIt() {
        assertEquals(
            "Message to Bob.",
            CleanupText.withoutLabel("Message to Bob.", "send a message to Bob"),
        )
    }

    @Test
    fun stripsATranscriptWrapperTheSpeakerDidNotSay() {
        assertEquals(
            "Buy milk.",
            CleanupText.withoutLabel("<transcript>\nBuy milk.\n</transcript>", "buy milk"),
        )
    }

    @Test
    fun stripsAQuotedTranscriptWrapper() {
        assertEquals(
            "Buy milk.",
            CleanupText.withoutLabel("\"<transcript>Buy milk.</transcript>\"", "buy milk"),
        )
    }

    @Test
    fun escapedNewlinesBecomeRealLineBreaks() {
        assertEquals(
            "Can you buy:\n1. Apples",
            CleanupText.normalizeBreaks("Can you buy:\\n1. Apples"),
        )
        assertEquals("Buy milk.", CleanupText.normalizeBreaks("Buy milk."))
        assertEquals("One\nTwo", CleanupText.normalizeBreaks("One\r\nTwo"))
    }

    @Test
    fun keepsATranscriptTagTheSpeakerActuallyStartedWith() {
        val wrapped = "<transcript>Buy milk.</transcript>"
        assertEquals(wrapped, CleanupText.withoutLabel(wrapped, wrapped))
    }
}
