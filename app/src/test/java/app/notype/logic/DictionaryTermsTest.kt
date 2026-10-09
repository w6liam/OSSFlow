package app.notype.logic

import app.notype.model.Term
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryTermsTest {
    @Test
    fun keytermsUseTheSpellingNotTheMishearing() {
        val terms = DictionaryTerms.keyterms(
            listOf(
                Term("  LawVu ", listOf("Law View")),
                Term("Sissi", listOf("CC")),
                Term("   ", listOf("skip")),
            ),
        )
        assertEquals(listOf("LawVu", "Sissi"), terms)
    }

    @Test
    fun keytermsTruncateAndCap() {
        val entries = List(110) { index -> Term("word-$index-" + "x".repeat(60)) }
        val terms = DictionaryTerms.keyterms(entries)
        assertEquals(100, terms.size)
        assertTrue(terms.all { it.length == 50 })
    }

    @Test
    fun duplicateSpellingsCollapse() {
        val terms = DictionaryTerms.keyterms(listOf(Term("LawVu"), Term("LawVu")))
        assertEquals(listOf("LawVu"), terms)
    }

    @Test
    fun promptBlockKeepsHeardAsPhrases() {
        val block = DictionaryTerms.promptBlock(
            listOf(Term("Sissi", listOf(" CC ", "Sissy", "CC"))),
        )
        assertEquals("- Sissi (often heard as: CC, Sissy)", block)
    }

    @Test
    fun whisperPromptIsEmptyWithoutWords() {
        assertEquals("", DictionaryTerms.whisperPrompt(emptyList()))
    }
}
