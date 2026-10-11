package app.utterdroid.logic

object HeardAsPrompt {
    fun system(): String = """
        You list likely speech-recognition mishearings of one term.
        Rules:
        - Output only the mishearings, separated by commas.
        - No label, no numbering, no quotes, and no preamble.
        - Do not repeat the correct spelling.
        - At most 6 phrases.
    """.trimIndent()

    fun user(spelling: String): String = "Term: $spelling"
}

object HeardAsSuggestions {
    private const val MAX_SUGGESTIONS = 6
    private val numbering = Regex("""^\d+[\).\s]+""")

    fun parse(modelText: String, spelling: String, existing: List<String>): List<String> {
        val correct = spelling.trim()
        val already = existing.map { it.trim() }.filter { it.isNotEmpty() }
        return DictionaryTerms.parseHeardAs(modelText)
            .map { phrase -> numbering.replace(phrase.trim().trim('"', '\'', '.'), "").trim() }
            .filter { it.isNotEmpty() && it.length <= DictionaryTerms.MAX_CHARS }
            .filter { !it.equals(correct, ignoreCase = true) }
            .filter { candidate -> already.none { it.equals(candidate, ignoreCase = true) } }
            .distinctBy { it.lowercase() }
            .take(MAX_SUGGESTIONS)
    }

    fun merge(existingRaw: String, modelText: String, spelling: String): String {
        val existing = DictionaryTerms.parseHeardAs(existingRaw)
        val added = parse(modelText, spelling, existing)
        return (existing + added).joinToString(", ")
    }
}
