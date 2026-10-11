package app.utterdroid.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class HeardAsSuggestionsTest {
    @Test
    fun keepsNewMishearingsAndDropsTheSpelling() {
        val parsed = HeardAsSuggestions.parse(
            modelText = "Law View, Lavoo, LawVu, 1. law view",
            spelling = "LawVu",
            existing = listOf("Law View"),
        )
        assertEquals(listOf("Lavoo"), parsed)
    }

    @Test
    fun mergeAppendsWithoutReplacing() {
        val merged = HeardAsSuggestions.merge(
            existingRaw = "CC",
            modelText = "Sissy, CC, See see",
            spelling = "Sissi",
        )
        assertEquals("CC, Sissy, See see", merged)
    }

    @Test
    fun dropsPhrasesLongerThanTheTermLimit() {
        val parsed = HeardAsSuggestions.parse(
            modelText = "x".repeat(51) + ", short",
            spelling = "Name",
            existing = emptyList(),
        )
        assertEquals(listOf("short"), parsed)
    }
}
