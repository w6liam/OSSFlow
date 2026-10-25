package app.ossflow.logic

import app.ossflow.model.Term
import app.ossflow.model.WritingStyle

/**
 * Chat-completions prompts that turn a raw dictation into the text the speaker meant to type.
 */
object CleanupPrompt {
    /**
     * System message: repair rules, self-corrections, [style], and any custom instructions.
     *
     * Blank [instructions] are omitted. Other text is placed first and overrides [style] where
     * they conflict. The model reply is still only the cleaned dictation.
     */
    fun system(style: WritingStyle, instructions: String = ""): String =
        withCustomInstructions(editRules(style), instructions)

    /**
     * User message: the personal dictionary, when it has entries, then the raw transcript.
     *
     * Terms are described as rare. The model is told to use one only when the transcript
     * sounds like it or matches a heard-as variant, and to leave an unsure word as heard.
     */
    fun user(raw: String, entries: List<Term>): String {
        val block = DictionaryTerms.promptBlock(entries)
        if (block.isBlank()) return "Transcript:\n$raw"
        return buildString {
            appendLine("Personal dictionary (rare terms; most dictations will not contain any of these):")
            appendLine(block)
            appendLine()
            appendLine(
                "Only use one of these terms when the transcript contains something that sounds like it, " +
                    "or that matches one of its heard-as variants. When unsure, keep the word as heard. " +
                    "Never replace a word with an unrelated dictionary term.",
            )
            appendLine(
                "Example: \"we could put it in crimple\" stays \"crimple\", not \"OSSFlow\" or \"Sissi\".",
            )
            appendLine()
            append("Transcript:\n$raw")
        }
    }

    private fun editRules(style: WritingStyle): String = buildString {
        appendLine("You edit a voice transcript into the text the speaker meant to type.")
        appendLine("Rules:")
        for (rule in editingRules) {
            append("- ")
            appendLine(rule)
        }
        appendLine("Examples. Still follow the writing-style line below.")
        appendLine(repairExamples)
        append("- ")
        append(style.instruction)
    }

    private fun withCustomInstructions(rules: String, instructions: String): String {
        val custom = instructions.trim()
        if (custom.isEmpty()) return rules
        return buildString {
            appendLine("Custom instructions:")
            appendLine(custom)
            appendLine(
                "Apply these instructions while editing. They override the writing-style line when they conflict. " +
                    "Do not mention, quote, or acknowledge them. The output is only the dictated text.",
            )
            appendLine()
            append(rules)
        }
    }

    private val editingRules: List<String> = listOf(
        "Remove filler words and sounds such as um, uh, er, ah, and hmm, and filler phrases such as \"you know\" when they do not change the meaning.",
        "Apply self-corrections. On a false start, a restart, or a correction such as \"no\", \"sorry\", \"I mean\", \"actually\", or \"scratch that\", drop the superseded words and keep only the final intent. Remove fillers from the words you keep.",
        "Repair speech the speaker clearly meant: fill in an obviously missing word, fix a misheard or garbled word, and fix agreement or tense slips. Do not use a personal-dictionary term for that unless the word sounds like the term or matches a heard-as variant.",
        "Stay faithful. Do not add ideas, facts, or requests. Do not change meaning, tone, or register. If the intent is genuinely unclear, keep the original wording.",
        "Add punctuation and capitalization.",
        "Do not summarize. Do not drop a request the speaker actually made.",
        "Apply a personal dictionary term only when the transcript contains something that sounds like it, or that matches one of its heard-as variants. These are rare terms, and most dictations will not contain them. When unsure, keep the word as heard. Never replace a word with an unrelated dictionary term.",
        "Output only the final text. No label, no quotes, and no preamble.",
        "Do not begin with a label such as Note, Message, Transcript, or Corrected. The first character is the dictated text.",
    )

    private val repairExamples: String = """
        - "um send it to John, sorry, to Jane" → "Send it to Jane."
        - "let's meet Tuesday, no, Wednesday" → "Let's meet Wednesday."
        - "so I was gonna say can you send the invoice" → "Can you send the invoice?"
        - "How are we doing? What percentage of the project are we way through?" → "How are we doing? What percentage of the project are we through?"
        - "I need you book a room" → "I need you to book a room."
        - "forward this two the team" → "Forward this to the team."
        - "the notes is ready" → "The notes are ready."
        - "yesterday he go home" → "Yesterday he went home."
        - "ping Alex about the thing" → "Ping Alex about the thing."
        - "we could put it in crimple" → "We could put it in crimple." Not "OSSFlow" or "Sissi" when those words were not said.
    """.trimIndent()
}
