package app.ossflow.logic

import app.ossflow.model.Term
import app.ossflow.model.WritingStyle
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
        assertTrue(prompt.endsWith("Not formal, and not casual slang."))
    }

    @Test
    fun repairExamplesCoverMissingAndGarbledWords() {
        val prompt = CleanupPrompt.system(WritingStyle.NORMAL)
        assertTrue(prompt.contains("fill in an obviously missing word"))
        assertTrue(prompt.contains("misheard or garbled"))
        assertTrue(prompt.contains("agreement or tense"))
        assertTrue(prompt.contains("genuinely unclear"))
        assertTrue(prompt.contains("Do not add ideas, facts, or requests"))
        assertTrue(prompt.contains("Do not change meaning, tone, or register"))
        assertTrue(prompt.contains("Examples. Still follow the writing-style line below."))
        assertTrue(
            prompt.contains(
                "\"How are we doing? What percentage of the project are we way through?\" → " +
                    "\"How are we doing? What percentage of the project are we through?\"",
            ),
        )
        assertTrue(prompt.contains("\"I need you book a room\" → \"I need you to book a room.\""))
        assertTrue(prompt.contains("\"forward this two the team\" → \"Forward this to the team.\""))
        assertTrue(prompt.contains("\"the notes is ready\" → \"The notes are ready.\""))
        assertTrue(prompt.contains("\"yesterday he go home\" → \"Yesterday he went home.\""))
        assertTrue(prompt.contains("\"ping Alex about the thing\" → \"Ping Alex about the thing.\""))
        assertTrue(prompt.indexOf("are we way through") < prompt.indexOf("standard English"))
    }

    @Test
    fun selfCorrectionExamplesKeepOnlyTheFinalIntent() {
        val prompt = CleanupPrompt.system(WritingStyle.NORMAL)
        assertTrue(prompt.contains("false start"))
        assertTrue(prompt.contains("drop the superseded words"))
        assertTrue(prompt.contains("final intent"))
        assertTrue(prompt.contains("Remove fillers from the words you keep"))
        assertTrue(prompt.contains("scratch that"))
        assertTrue(prompt.contains("\"um send it to John, sorry, to Jane\" → \"Send it to Jane.\""))
        assertTrue(prompt.contains("\"let's meet Tuesday, no, Wednesday\" → \"Let's meet Wednesday.\""))
        assertTrue(
            prompt.contains(
                "\"so I was gonna say can you send the invoice\" → \"Can you send the invoice?\"",
            ),
        )
    }

    @Test
    fun dictionaryStyleAndCustomInstructionsComposeWithTheRules() {
        val prompt = CleanupPrompt.system(WritingStyle.FORMAL, "British English")
        val informal = CleanupPrompt.system(WritingStyle.INFORMAL, "British English")
        val message = CleanupPrompt.user(
            raw = "um email sissi about law view",
            entries = listOf(
                Term("Sissi", listOf("CC")),
                Term("LawVu", listOf("Law View")),
            ),
        )

        assertTrue(prompt.startsWith("Custom instructions:\nBritish English\n"))
        assertTrue(prompt.contains("override the writing-style line"))
        assertTrue(prompt.contains("Do not mention, quote, or acknowledge them"))
        assertTrue(prompt.contains("polite, professional English"))
        assertTrue(prompt.endsWith("Write in polite, professional English."))
        assertTrue(prompt.indexOf("British English") < prompt.indexOf("You edit a voice transcript"))
        assertTrue(prompt.indexOf("are we way through") < prompt.indexOf("professional English"))
        assertTrue(prompt.contains("Send it to Jane."))
        assertTrue(prompt.contains("Let's meet Wednesday."))
        assertTrue(prompt.contains("personal dictionary"))
        assertTrue(prompt.contains("Do not drop a request the speaker actually made"))
        assertFalse(prompt.contains("um email sissi"))

        assertTrue(informal.contains("contractions"))
        assertTrue(informal.contains("are we way through"))
        assertTrue(informal.endsWith("use contractions."))
        assertFalse(informal.contains("professional English"))

        assertTrue(message.contains("Sissi (often heard as: CC)"))
        assertTrue(message.contains("LawVu (often heard as: Law View)"))
        assertTrue(message.contains("Transcript:\num email sissi about law view"))
        assertFalse(message.contains("Custom instructions"))
        assertFalse(message.contains("British English"))
        assertFalse(message.contains("are we way through"))
        assertFalse(message.contains("Send it to Jane."))
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
    fun dictionaryRuleKeepsAnUnrelatedWord() {
        val prompt = CleanupPrompt.system(WritingStyle.NORMAL)
        assertTrue(prompt.contains("personal dictionary"))
        assertFalse(prompt.contains("Apply the personal dictionary exactly"))
        assertTrue(prompt.contains("sounds like it"))
        assertTrue(prompt.contains("heard-as"))
        assertTrue(prompt.contains("rare terms"))
        assertTrue(prompt.contains("most dictations will not contain them"))
        assertTrue(prompt.contains("When unsure, keep the word as heard"))
        assertTrue(prompt.contains("Never replace a word with an unrelated dictionary term"))
        assertTrue(prompt.contains("unless the word sounds like the term or matches a heard-as variant"))
        assertTrue(
            prompt.contains(
                "\"we could put it in crimple\" → \"We could put it in crimple.\" " +
                    "Not \"OSSFlow\" or \"Sissi\" when those words were not said.",
            ),
        )
    }

    @Test
    fun dictionaryBlockSaysTermsAreRareAndGivesTheNegativeExample() {
        val message = CleanupPrompt.user(
            raw = "we could put it in crimple",
            entries = listOf(
                Term("Sissi"),
                Term("OSSFlow", listOf("oss flow")),
            ),
        )
        assertTrue(
            message.startsWith(
                "Personal dictionary (rare terms; most dictations will not contain any of these):",
            ),
        )
        assertTrue(message.contains("- Sissi"))
        assertTrue(message.contains("- OSSFlow (often heard as: oss flow)"))
        assertTrue(message.contains("Only use one of these terms"))
        assertTrue(message.contains("sounds like it"))
        assertTrue(message.contains("heard-as variants"))
        assertTrue(message.contains("When unsure, keep the word as heard"))
        assertTrue(message.contains("Never replace a word with an unrelated dictionary term"))
        assertTrue(
            message.contains(
                "Example: \"we could put it in crimple\" stays \"crimple\", not \"OSSFlow\" or \"Sissi\".",
            ),
        )
        assertTrue(message.endsWith("Transcript:\nwe could put it in crimple"))
        assertTrue(message.indexOf("Sissi") < message.indexOf("Transcript:"))
    }

    @Test
    fun emptyDictionaryOmitsTheDictionaryBlock() {
        val message = CleanupPrompt.user("hello", emptyList())
        assertFalse(message.contains("Personal dictionary"))
        assertTrue(message.startsWith("Transcript:\nhello"))
    }
}
