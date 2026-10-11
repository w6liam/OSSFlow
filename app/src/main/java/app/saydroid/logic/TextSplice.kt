package app.saydroid.logic

object TextSplice {
    fun splice(existing: String, selectionStart: Int, selectionEnd: Int, insertion: String): String {
        if (insertion.isEmpty()) return existing
        if (existing.isEmpty()) return insertion
        val hasSelection = selectionStart >= 0 && selectionEnd >= 0
        if (!hasSelection) return join(existing, insertion)
        val start = selectionStart.coerceIn(0, existing.length)
        val end = selectionEnd.coerceIn(start, existing.length)
        val prefix = existing.substring(0, start)
        val suffix = existing.substring(end)
        return join(prefix, insertion) + separator(insertion, suffix) + suffix
    }

    private fun join(left: String, right: String): String = left + separator(left, right) + right

    private fun separator(left: String, right: String): String {
        if (left.isEmpty() || right.isEmpty()) return ""
        if (left.last().isWhitespace() || right.first().isWhitespace()) return ""
        return " "
    }
}
