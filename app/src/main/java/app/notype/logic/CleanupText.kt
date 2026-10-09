package app.notype.logic

object CleanupText {
    private val leadingLabel = Regex(
        """(?i)^(?:here(?:'s| is)\s+)?(?:the\s+)?(?:cleaned\s+|final\s+|corrected\s+)?""" +
            """(note|notes|message|transcript|transcription|corrected|correction|output|result)""" +
            """\s*(?:[:\-\u2014]\s*|\n+)""",
    )

    fun withoutLabel(cleaned: String, raw: String): String {
        var text = cleaned.trim()
        val spoken = raw.trim()
        repeat(3) {
            val match = leadingLabel.find(text) ?: return text
            val label = match.groupValues[1]
            if (spoken.startsWith(label, ignoreCase = true)) return text
            val rest = text.substring(match.range.last + 1).trim()
            if (rest.isEmpty()) return text
            text = rest
        }
        return text
    }
}
