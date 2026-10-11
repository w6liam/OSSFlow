package app.saydroid.logic

object TimeSaved {
    const val TYPING_WPM = 40.0
    private const val FALLBACK_SPEAKING_WPM = 150L

    fun wordCount(text: String): Int =
        text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

    fun speakingMs(words: Int, measuredMs: Long): Long {
        if (measuredMs > 0L) return measuredMs
        if (words <= 0) return 0L
        return words * 60_000L / FALLBACK_SPEAKING_WPM
    }

    fun minutes(words: Long, speakingMs: Long): Double {
        if (words <= 0L) return 0.0
        val speakingMinutes = speakingMs.coerceAtLeast(0L) / 60_000.0
        return (words / TYPING_WPM - speakingMinutes).coerceAtLeast(0.0)
    }

    fun typingMinutes(words: Long): Double =
        if (words <= 0L) 0.0 else words / TYPING_WPM

    fun formatMinutes(minutes: Double): String {
        if (minutes < 0.05) return "0 min"
        if (minutes < 60.0) {
            return if (minutes < 10.0) {
                String.format("%.1f min", minutes)
            } else {
                "${minutes.toInt()} min"
            }
        }
        val hours = (minutes / 60.0).toInt()
        val remainder = (minutes % 60.0).toInt()
        return if (remainder == 0) "$hours hr" else "$hours hr $remainder min"
    }
}
