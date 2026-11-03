package app.ossflow.logic

object CleanupText {
    /**
     * Turns a written escape, or a carriage return, into a real line break.
     * A list then pastes with each item on its own line.
     */
    fun normalizeBreaks(text: String): String {
        if (!text.contains('\\') && !text.contains('\r')) return text
        return text.replace("\r\n", "\n").replace('\r', '\n').replace("\\n", "\n")
    }

    private val leadingLabel = Regex(
        """(?i)^(?:#+\s*)?(?:here(?:'s| is)\s+)?(?:the\s+|your\s+|a\s+)?""" +
            """(?:cleaned\s+|final\s+|corrected\s+)?["'“”*]*""" +
            """(notes|note|transcription|transcript|corrected|correction|message|output|result)""" +
            """["'“”*]*(?:\s*[:.\u2013\u2014\-]\s*|\s*\n+\s*|\s+)?""",
    )

    fun withoutLabel(cleaned: String, raw: String): String {
        val trimmed = cleaned.trim()
        val peeled = peel(trimmed)
        val unwrapped = unwrapTranscript(peeled, raw.trim())
        val hadWrapper = unwrapped != peeled
        val original = if (hadWrapper) unwrapped else trimmed
        var text = if (hadWrapper) peel(unwrapped) else peeled
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

    private val transcriptWrapper = Regex("(?is)^<transcript>\\s*(.*?)\\s*</transcript>$")

    /**
     * Drops a transcript fence the model echoed around the edit.
     * A dictation that itself begins with that tag is left unchanged.
     */
    private fun unwrapTranscript(text: String, raw: String): String {
        if (raw.startsWith("<transcript", ignoreCase = true)) return text
        val inner = transcriptWrapper.find(text)?.groupValues?.get(1)?.trim().orEmpty()
        if (inner.isEmpty()) return text
        return inner
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
