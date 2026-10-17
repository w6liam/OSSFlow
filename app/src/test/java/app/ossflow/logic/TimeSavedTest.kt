package app.ossflow.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeSavedTest {
    @Test
    fun countsWordsOnWhitespace() {
        assertEquals(4, TimeSaved.wordCount("  email Sissi about LawVu  "))
        assertEquals(0, TimeSaved.wordCount("   "))
    }

    @Test
    fun hundredWordsAtAMeasuredPace() {
        val speakingMs = 40_000L
        val saved = TimeSaved.minutes(100, speakingMs)
        assertEquals(1.833, saved, 0.01)
    }

    @Test
    fun zeroWordsSavesNothing() {
        assertEquals(0.0, TimeSaved.minutes(0, 60_000), 0.0)
    }

    @Test
    fun slowSpeechDoesNotGoNegative() {
        assertEquals(0.0, TimeSaved.minutes(40, 120_000), 0.0)
    }

    @Test
    fun missingDurationUsesOneHundredFiftyWordsPerMinute() {
        assertEquals(40_000L, TimeSaved.speakingMs(100, 0))
        assertEquals(12_000L, TimeSaved.speakingMs(100, 12_000))
    }
}
