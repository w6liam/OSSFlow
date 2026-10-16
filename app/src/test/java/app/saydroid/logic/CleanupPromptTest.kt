package app.saydroid.logic

import app.saydroid.model.Term
import app.saydroid.model.WritingStyle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanupPromptTest {
    @Test
    fun normalStyleIsStandardEnglish() {
        val prompt = CleanupPrompt.system(WritingStyle.NORMAL)
        assertTrue(prompt.contains("standard English"))
        assertTrue(prompt.contains("Not formal"))
        assertFalse(prompt.contains("professional English"))
        assertTrue(prompt.contains("um"))
        assertTrue(prompt.contains("sorry"))
        assertTrue(prompt.contains("personal dictionary"))
        assertTrue(prompt.contains("Output only the final text"))
        assertTrue(prompt.contains("Do not begin with a label"))
    }

    @Test
    fun formalAndInformalChangeTheToneLine() {
        assertTrue(CleanupPrompt.system(WritingStyle.FORMAL).contains("professional"))
        assertTrue(CleanupPrompt.system(WritingStyle.INFORMAL).contains("contractions"))
    }

    @Test
    fun userMessageIncludesDictionaryAndTranscript() {
        val message = CleanupPrompt.user(
            raw = "um email sissi about law view",
            entries = listOf(
                Term("Sissi", listOf("CC")),
                Term("LawVu", listOf("Law View")),
            ),
        )
        assertTrue(message.contains("Sissi (often heard as: CC)"))
        assertTrue(message.contains("LawVu (often heard as: Law View)"))
        assertTrue(message.contains("Transcript:\num email sissi about law view"))
    }

    @Test
    fun blankCustomInstructionsLeaveThePromptUnchanged() {
        val prompt = CleanupPrompt.system(WritingStyle.NORMAL, "   ")
        assertFalse(prompt.contains("Custom instructions"))
        assertTrue(prompt.startsWith("You edit a voice transcript"))
    }

    @Test
    fun customInstructionsLeadThePromptAndStaySilent() {
        val prompt = CleanupPrompt.system(WritingStyle.NORMAL, "British English")
        assertTrue(prompt.startsWith("Custom instructions:\nBritish English\n"))
        assertTrue(prompt.contains("Do not mention, quote, or acknowledge them"))
        assertTrue(prompt.contains("override the writing-style line"))
        assertTrue(prompt.indexOf("British English") < prompt.indexOf("You edit a voice transcript"))
        assertTrue(prompt.contains("standard English"))
    }

    @Test
    fun emptyDictionaryOmitsTheDictionaryBlock() {
        val message = CleanupPrompt.user("hello", emptyList())
        assertFalse(message.contains("Personal dictionary"))
        assertTrue(message.startsWith("Transcript:\nhello"))
    }
}
