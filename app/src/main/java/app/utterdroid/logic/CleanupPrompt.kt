package app.utterdroid.logic

import app.utterdroid.model.Term
import app.utterdroid.model.WritingStyle

object CleanupPrompt {
    fun system(style: WritingStyle): String = """
        You edit a voice transcript into the text the speaker meant to type.
        Rules:
        - Remove filler words and sounds such as um, uh, er, ah, and hmm, and filler phrases such as "you know" when they do not change the meaning.
        - Apply self-corrections. If the speaker restarts or says "no", "sorry", "I mean", "actually", or "scratch that", keep only the corrected version.
        - Add punctuation and capitalization.
        - Do not add facts. Do not summarize. Do not drop a request the speaker actually made.
        - Apply the personal dictionary exactly. When the transcript matches a mishearing, replace it with the correct spelling.
        - Output only the final text. No label, no quotes, and no preamble.
        - Do not begin with a label such as Note, Message, Transcript, or Corrected. The first character is the dictated text.
        - ${style.instruction}
    """.trimIndent()

    fun user(raw: String, entries: List<Term>): String {
        val block = DictionaryTerms.promptBlock(entries)
        val dictionary = if (block.isBlank()) "" else "Personal dictionary:\n$block\n\n"
        return "${dictionary}Transcript:\n$raw"
    }
}
