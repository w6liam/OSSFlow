package app.ossflow.logic

import app.ossflow.model.Term

object DictionaryTerms {
    const val MAX_TERMS = 100
    const val MAX_CHARS = 50

    fun parseHeardAs(raw: String): List<String> =
        raw.split(',', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    fun keyterms(entries: List<Term>): List<String> {
        val out = LinkedHashSet<String>()
        for (entry in entries) {
            val spelling = entry.spelling.trim()
            if (spelling.isEmpty()) continue
            out.add(spelling.take(MAX_CHARS))
            if (out.size == MAX_TERMS) break
        }
        return out.toList()
    }

    fun promptBlock(entries: List<Term>): String =
        entries.mapNotNull { entry ->
            val spelling = entry.spelling.trim()
            if (spelling.isEmpty()) return@mapNotNull null
            val heard = entry.heardAs.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            if (heard.isEmpty()) {
                "- $spelling"
            } else {
                "- $spelling (often heard as: ${heard.joinToString(", ")})"
            }
        }.take(MAX_TERMS).joinToString("\n")

    fun whisperPrompt(entries: List<Term>): String {
        val words = keyterms(entries)
        if (words.isEmpty()) return ""
        return "Vocabulary: ${words.joinToString(", ")}".take(800)
    }
}
