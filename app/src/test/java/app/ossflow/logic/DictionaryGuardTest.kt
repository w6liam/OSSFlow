package app.ossflow.logic

import app.ossflow.model.Term
import org.junit.Assert.assertEquals
import org.junit.Test

class DictionaryGuardTest {
    private val sissiAndOssFlow = listOf(
        Term("Sissi"),
        Term("OSSFlow"),
    )

    @Test
    fun crimpleIsNotReplacedWithOssFlow() {
        val raw = "We could put it in crimple"
        val cleaned = "We could put it in OSSFlow"
        assertEquals(raw, DictionaryGuard.apply(raw, cleaned, sissiAndOssFlow))
    }

    @Test
    fun crimpleIsNotReplacedWithSissiWhenOssFlowIsAbsent() {
        val raw = "We could put it in crimple"
        val cleaned = "We could put it in Sissi"
        assertEquals(raw, DictionaryGuard.apply(raw, cleaned, listOf(Term("Sissi"))))
    }

    @Test
    fun capitalizationAndTheAddedPeriodStayWhenTheWordIsRestored() {
        assertEquals(
            "We could put it in crimple.",
            DictionaryGuard.apply(
                raw = "we could put it in crimple",
                cleaned = "We could put it in OSSFlow.",
                terms = sissiAndOssFlow,
            ),
        )
    }

    @Test
    fun ossFlowSpokenAsTwoWordsIsKept() {
        val cleaned = "We could put it in OSSFlow."
        assertEquals(
            cleaned,
            DictionaryGuard.apply(
                raw = "we could put it in oss flow",
                cleaned = cleaned,
                terms = sissiAndOssFlow,
            ),
        )
    }

    @Test
    fun ossfloIsCloseEnoughToKeep() {
        val cleaned = "We could put it in OSSFlow."
        assertEquals(
            cleaned,
            DictionaryGuard.apply(
                raw = "we could put it in ossflo",
                cleaned = cleaned,
                terms = listOf(Term("OSSFlow")),
            ),
        )
    }

    @Test
    fun aSpokenDictionaryWordIsKept() {
        val cleaned = "We could put it in Crimple."
        assertEquals(
            cleaned,
            DictionaryGuard.apply(
                raw = "we could put it in crimple",
                cleaned = cleaned,
                terms = listOf(Term("Crimple")),
            ),
        )
    }

    @Test
    fun heardAsVariantIsKeptWhenItDoesNotSoundLikeTheSpelling() {
        val cleaned = "Email Sissi about the draft."
        assertEquals(
            cleaned,
            DictionaryGuard.apply(
                raw = "email cc about the draft",
                cleaned = cleaned,
                terms = listOf(Term("Sissi", listOf("CC"))),
            ),
        )
    }

    @Test
    fun heardAsPhraseKeepsAMultiWordMishearing() {
        val cleaned = "Open LawVu, please."
        assertEquals(
            cleaned,
            DictionaryGuard.apply(
                raw = "open law view please",
                cleaned = cleaned,
                terms = listOf(Term("LawVu", listOf("Law View"))),
            ),
        )
    }

    @Test
    fun multiWordTermIsKeptWhenTheRawPhraseMatches() {
        val cleaned = "Meet in New York tomorrow."
        assertEquals(
            cleaned,
            DictionaryGuard.apply(
                raw = "meet in new york tomorrow",
                cleaned = cleaned,
                terms = listOf(Term("New York")),
            ),
        )
    }

    @Test
    fun multiWordTermIsRevertedWhenTheRawWordDoesNotSupportIt() {
        assertEquals(
            "Meet in crimple tomorrow.",
            DictionaryGuard.apply(
                raw = "meet in crimple tomorrow",
                cleaned = "Meet in New York tomorrow.",
                terms = listOf(Term("New York")),
            ),
        )
    }

    @Test
    fun hyphenatedRawFormOfATermIsKept() {
        val cleaned = "Use OSSFlow today."
        assertEquals(
            cleaned,
            DictionaryGuard.apply(
                raw = "use OSS-Flow today",
                cleaned = cleaned,
                terms = listOf(Term("OSSFlow")),
            ),
        )
    }

    @Test
    fun quotesCommaAndRawCaseStayAroundTheRestoredWord() {
        assertEquals(
            "See \"Crimple\", then.",
            DictionaryGuard.apply(
                raw = "see Crimple then",
                cleaned = "See \"OSSFlow\", then.",
                terms = listOf(Term("OSSFlow")),
            ),
        )
    }

    @Test
    fun possessiveOnAnUnrelatedTermIsRestoredWithTheRawPossessive() {
        assertEquals(
            "That is crimple's draft.",
            DictionaryGuard.apply(
                raw = "that is crimple's draft",
                cleaned = "That is OSSFlow's draft.",
                terms = listOf(Term("OSSFlow")),
            ),
        )
    }

    @Test
    fun emptyDictionaryLeavesCleanupUntouched() {
        val cleaned = "We could put it in OSSFlow."
        assertEquals(
            cleaned,
            DictionaryGuard.apply(
                raw = "we could put it in crimple",
                cleaned = cleaned,
                terms = emptyList(),
            ),
        )
    }

    @Test
    fun blankSpellingsDoNotChangeTheTranscript() {
        val cleaned = "We could put it in crimple."
        assertEquals(
            cleaned,
            DictionaryGuard.apply(
                raw = "we could put it in crimple",
                cleaned = cleaned,
                terms = listOf(Term("   "), Term("", listOf("crimple"))),
            ),
        )
    }

    @Test
    fun fillerRemovalAndSelfCorrectionStayWhenNoDictionaryTermWasIntroduced() {
        val cleaned = "Send it to Jane."
        assertEquals(
            cleaned,
            DictionaryGuard.apply(
                raw = "um send it to john sorry to jane",
                cleaned = cleaned,
                terms = sissiAndOssFlow,
            ),
        )
    }

    @Test
    fun fillerRemovalStaysWhenTheUnrelatedTermIsRestored() {
        assertEquals(
            "We could put it in crimple.",
            DictionaryGuard.apply(
                raw = "um we could put it in crimple",
                cleaned = "We could put it in OSSFlow.",
                terms = listOf(Term("OSSFlow")),
            ),
        )
    }

    @Test
    fun fillerBesideTheReplacedWordStaysRemoved() {
        assertEquals(
            "crimple.",
            DictionaryGuard.apply(
                raw = "um crimple",
                cleaned = "OSSFlow.",
                terms = listOf(Term("OSSFlow")),
            ),
        )
    }

    @Test
    fun youKnowBesideARealFixStaysRemoved() {
        assertEquals(
            "OSSFlow.",
            DictionaryGuard.apply(
                raw = "you know oss flow",
                cleaned = "OSSFlow.",
                terms = listOf(Term("OSSFlow")),
            ),
        )
    }

    @Test
    fun selfCorrectionInTheSameSpanKeepsTheFinalWord() {
        assertEquals(
            "Send it to crimple.",
            DictionaryGuard.apply(
                raw = "send it to john sorry crimple",
                cleaned = "Send it to Sissi.",
                terms = sissiAndOssFlow,
            ),
        )
    }

    @Test
    fun onlyTheUnrelatedOccurrenceIsRestored() {
        assertEquals(
            "OSSFlow beat crimple.",
            DictionaryGuard.apply(
                raw = "oss flow beat crimple",
                cleaned = "OSSFlow beat OSSFlow.",
                terms = listOf(Term("OSSFlow")),
            ),
        )
    }

    @Test
    fun anInsertedTermWithNothingBehindItIsRemoved() {
        assertEquals(
            "We could put it in.",
            DictionaryGuard.apply(
                raw = "we could put it in",
                cleaned = "We could put it in OSSFlow.",
                terms = listOf(Term("OSSFlow")),
            ),
        )
    }

    @Test
    fun sorryIsNotCloseEnoughToSissi() {
        assertEquals(
            "I am sorry.",
            DictionaryGuard.apply(
                raw = "I am sorry",
                cleaned = "I am Sissi.",
                terms = listOf(Term("Sissi")),
            ),
        )
    }

    @Test
    fun flowAloneIsNotCloseEnoughToOssFlow() {
        assertEquals(
            "Go with the flow.",
            DictionaryGuard.apply(
                raw = "go with the flow",
                cleaned = "Go with the OSSFlow.",
                terms = listOf(Term("OSSFlow")),
            ),
        )
    }

    @Test
    fun identicalTextIsUnchanged() {
        val text = "We could put it in OSSFlow."
        assertEquals(text, DictionaryGuard.apply(text, text, sissiAndOssFlow))
    }
}
