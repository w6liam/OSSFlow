package app.ossflow.logic

object CleanupText {
    private val leadingLabel = Regex(
        """(?i)^(?:#+\s*)?(?:here(?:'s| is)\s+)?(?:the\s+|your\s+|a\s+)?""" +
            """(?:cleaned\s+|final\s+|corrected\s+)?["'“”*]*""" +
            """(notes|note|transcription|transcript|corrected|correction|message|output|result)""" +
            """["'“”*]*(?:\s*[:.\u2013\u2014\-]\s*|\s*\n+\s*|\s+)?""",
    )

    fun withoutLabel(cleaned: String, raw: String): String {
        val original = cleaned.trim()
        var text = peel(original)
        val spoken = raw.trim()
        var stripped = false
        repeat(3) {
            val match = leadingLabel.find(text) ?: return if (stripped) text else original
            val label = match.groupValues[1]
            if (containsWord(spoken, label)) return if (stripped) text else original
            val rest = peel(text.substring(match.range.last + 1).trim())
            if (rest.isEmpty()) return if (stripped) text else original
            text = rest
            stripped = true
        }
        return text
    }

    private fun containsWord(spoken: String, word: String): Boolean =
        Regex("""(?i)\b${Regex.escape(word)}\b""").containsMatchIn(spoken)

    private fun peel(text: String): String {
        val value = text.trim()
        if (value.length < 2) return value
        val open = value.first()
        val close = value.last()
        val quoted = (open == '"' && close == '"') ||
            (open == '\'' && close == '\'') ||
            (open == '“' && close == '”') ||
            (open == '‘' && close == '’')
        if (quoted) return value.substring(1, value.length - 1).trim()
        if (value.startsWith("**") && value.endsWith("**") && value.length >= 4) {
            return value.substring(2, value.length - 2).trim()
        }
        return value
    }
}
