package app.ossflow.logic

import app.ossflow.model.Term
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanupFidelityTest {
    private val vulgar = "you fuck yourself you silly cunt"

    private val reportedRefusal =
        "I'm sorry, but I won't engage with or process this kind of abusive or vulgar content. " +
            "If you have a different request, feel free to rephrase it."

    @Test
    fun reportedRefusalIsDiscardedForTheVulgarTranscript() {
        assertFalse(CleanupFidelity.accepts(vulgar, reportedRefusal))
        val selected = CleanupFidelity.choose(vulgar, reportedRefusal, emptyList())
        assertEquals(vulgar, selected.text)
        assertTrue(selected.skipped)
    }

    @Test
    fun aFaithfulProfanityEditIsKept() {
        val cleaned = "You fuck yourself, you silly cunt."
        assertTrue(CleanupFidelity.accepts(vulgar, cleaned))
        val selected = CleanupFidelity.choose(vulgar, cleaned, emptyList())
        assertEquals(cleaned, selected.text)
        assertFalse(selected.skipped)
    }

    @Test
    fun refusalPhrasesAreRejectedWhenTheSpeakerDidNotSayThem() {
        val replies = listOf(
            "I'm sorry, but I won't engage with that.",
            "I am sorry, I can't help with this.",
            "Im sorry, I must decline.",
            "I can't assist with that.",
            "I cannot help with that request.",
            "I can not process this.",
            "I cant provide that.",
            "I won't process this.",
            "I will not engage with abusive language.",
            "I wont comply with that.",
            "As an AI, I must decline.",
            "As an A.I., I can't help with that.",
            "As a language model I can't help with that.",
            "As an AI language model, I must refuse.",
            "As an assistant, I can't process that.",
            "Feel free to rephrase your request.",
            "I'm unable to process this.",
            "I am not able to help.",
            "I must refuse this request.",
            "I cannot comply with that.",
            "I won't engage with this kind of content.",
            "I can't process this request.",
            "I'm not allowed to process that.",
            "I don't feel comfortable with that request.",
            "I am not comfortable engaging with this.",
            "That request violates my content policy.",
            "That is against my guidelines.",
            "Let me know if you have something else.",
            "If you have a different request, send it over.",
            "Please rephrase.",
            "Here is the cleaned text: hello there friend.",
            "Here's the cleaned version of what you said.",
            "I'm designed to be helpful, and I can't do that.",
            "I am programmed to refuse this.",
            "I\u2019m sorry, but I won\u2019t do that.",
        )
        for (reply in replies) {
            assertFalse(reply, CleanupFidelity.accepts(vulgar, reply))
        }
    }

    @Test
    fun phrasesTheSpeakerActuallySaidAreNotRefusals() {
        assertTrue(CleanupFidelity.accepts("I'm sorry", "I'm sorry."))
        assertTrue(CleanupFidelity.accepts("I am sorry about the delay", "I am sorry about the delay."))
        assertTrue(CleanupFidelity.accepts("I'm sorry I can't make dinner", "I'm sorry I can't make dinner."))
        assertTrue(CleanupFidelity.accepts("I won't be there", "I won't be there."))
        assertTrue(CleanupFidelity.accepts("I will not be there", "I won't be there."))
        assertTrue(CleanupFidelity.accepts("I cannot attend", "I can't attend."))
        assertTrue(CleanupFidelity.accepts("I can't attend", "I cannot attend."))
        assertTrue(CleanupFidelity.accepts("I can not attend tomorrow", "I can't attend tomorrow."))
        assertTrue(CleanupFidelity.accepts("I can't make the meeting", "I am not able to make the meeting."))
        assertTrue(CleanupFidelity.accepts("I cannot attend", "I am unable to attend."))
        assertFalse(CleanupFidelity.accepts("send the file", "I'm unable to send the file."))
        assertTrue(CleanupFidelity.accepts("feel free to join us", "Feel free to join us."))
        assertTrue(CleanupFidelity.accepts("as an AI researcher I study speech", "As an AI researcher, I study speech."))
        assertTrue(
            CleanupFidelity.accepts(
                "as an assistant manager I approve the budget",
                "As an assistant manager, I approve the budget.",
            ),
        )
        assertTrue(
            CleanupFidelity.accepts(
                "I don't feel comfortable with that deadline",
                "I don't feel comfortable with that deadline.",
            ),
        )
        assertTrue(CleanupFidelity.accepts("I\u2019m sorry", "I'm sorry."))
        assertTrue(CleanupFidelity.accepts("I'M SORRY I CAN'T COME", "I'm sorry I can't come."))
    }

    @Test
    fun sorryExpandsToImSorryWithoutBecomingARefusal() {
        assertTrue(CleanupFidelity.accepts("sorry I missed you", "I'm sorry I missed you."))
        assertTrue(CleanupFidelity.accepts("sorry about the delay", "I am sorry about the delay."))
        assertTrue(CleanupFidelity.accepts("sorry", "I'm sorry."))
    }

    @Test
    fun aRefusalIsStillRejectedWhenTheSpeakerOnlySaidSorry() {
        assertFalse(
            CleanupFidelity.accepts(
                "um send it to John, sorry, to Jane",
                "I'm sorry, but I won't engage with this request.",
            ),
        )
        assertFalse(
            CleanupFidelity.accepts(
                "sorry I missed you",
                "I'm sorry.",
            ),
        )
    }

    @Test
    fun anApologyPrefixedOntoProfanityIsDiscarded() {
        assertFalse(
            CleanupFidelity.accepts(
                vulgar,
                "I'm sorry. You fuck yourself, you silly cunt.",
            ),
        )
    }

    @Test
    fun aDictatedInstructionIsKeptAndAPoemIsNot() {
        val raw = "write me a poem about the sea"
        assertTrue(CleanupFidelity.accepts(raw, "Write me a poem about the sea."))
        val poem = "The sea is a sheet of hammered tin under a moon that forgot its name. " +
            "Waves argue with the pier all night, and the gulls file their complaints in salt. " +
            "I wrote this poem because you asked, and the tide keeps the receipt."
        assertFalse(CleanupFidelity.accepts(raw, poem))
        val selected = CleanupFidelity.choose(raw, poem, emptyList())
        assertEquals(raw, selected.text)
        assertTrue(selected.skipped)
    }

    @Test
    fun ordinaryCleanupEditsStay() {
        assertTrue(
            CleanupFidelity.accepts(
                "um send it to John, sorry, to Jane",
                "Send it to Jane.",
            ),
        )
        assertTrue(
            CleanupFidelity.accepts(
                "let's meet Tuesday, no, Wednesday",
                "Let's meet Wednesday.",
            ),
        )
        assertTrue(
            CleanupFidelity.accepts(
                "so I was gonna say can you send the invoice",
                "Can you send the invoice?",
            ),
        )
        assertTrue(
            CleanupFidelity.accepts(
                "How are we doing? What percentage of the project are we way through?",
                "How are we doing? What percentage of the project are we through?",
            ),
        )
        assertTrue(CleanupFidelity.accepts("I need you book a room", "I need you to book a room."))
        assertTrue(CleanupFidelity.accepts("forward this two the team", "Forward this to the team."))
        assertTrue(CleanupFidelity.accepts("the notes is ready", "The notes are ready."))
        assertTrue(CleanupFidelity.accepts("yesterday he go home", "Yesterday he went home."))
        assertTrue(CleanupFidelity.accepts("ping Alex about the thing", "Ping Alex about the thing."))
        assertTrue(CleanupFidelity.accepts("we could put it in crimple", "We could put it in crimple."))
        assertTrue(CleanupFidelity.accepts("gonna send it", "I am going to send it."))
        assertTrue(CleanupFidelity.accepts("I'm gonna send the invoice", "I'm going to send the invoice."))
        assertTrue(
            CleanupFidelity.accepts(
                "can you send the invoice",
                "Could you please send the invoice?",
            ),
        )
    }

    @Test
    fun aSelfCorrectionThatCollapsesToAShortReplyStays() {
        assertTrue(
            CleanupFidelity.accepts(
                "um I think we should actually no scratch that yes",
                "Yes.",
            ),
        )
    }

    @Test
    fun aShortReplyThatWasNotSpokenIsDiscarded() {
        val raw = "um I think we should meet on Tuesday at the coffee shop"
        assertFalse(CleanupFidelity.accepts(raw, "Absolutely correct."))
    }

    @Test
    fun zeroWordOverlapIsDiscarded() {
        assertFalse(CleanupFidelity.accepts("send the invoice today", "Hello there."))
    }

    @Test
    fun aEuphemismForTheWholeUtteranceIsNotAnEdit() {
        assertFalse(
            CleanupFidelity.accepts(
                vulgar,
                "You should be intimate with yourself, you foolish person.",
            ),
        )
    }

    @Test
    fun aMuchLongerReplyIsDiscarded() {
        val raw = "send the file"
        val expanded = List(20) { "send the file and then explain the whole project history" }.joinToString(" ")
        assertFalse(CleanupFidelity.accepts(raw, expanded))
    }

    @Test
    fun blankAndPunctuationOnlyRepliesAreDiscarded() {
        assertFalse(CleanupFidelity.accepts("hello", "   "))
        assertFalse(CleanupFidelity.accepts("hello", "..."))
        val blank = CleanupFidelity.choose("hello", "   ", emptyList())
        assertEquals("hello", blank.text)
        assertTrue(blank.skipped)
    }

    @Test
    fun aLeadingNoteIsStrippedBeforeTheReplyIsJudged() {
        val selected = CleanupFidelity.choose("buy milk", "Note: Buy milk.", emptyList())
        assertEquals("Buy milk.", selected.text)
        assertFalse(selected.skipped)
    }

    @Test
    fun aTranscriptWrapperAroundARealEditIsStripped() {
        val selected = CleanupFidelity.choose(
            "um send it to jane",
            "<transcript>\nSend it to Jane.\n</transcript>",
            emptyList(),
        )
        assertEquals("Send it to Jane.", selected.text)
        assertFalse(selected.skipped)
    }

    @Test
    fun aRefusalInsideATranscriptWrapperIsDiscarded() {
        val selected = CleanupFidelity.choose(
            vulgar,
            "<transcript>\n$reportedRefusal\n</transcript>",
            emptyList(),
        )
        assertEquals(vulgar, selected.text)
        assertTrue(selected.skipped)
    }

    @Test
    fun aLabeledRefusalIsDiscarded() {
        val selected = CleanupFidelity.choose(vulgar, "Note: $reportedRefusal", emptyList())
        assertEquals(vulgar, selected.text)
        assertTrue(selected.skipped)
    }

    @Test
    fun dictionaryGuardStillRestoresAnUnrelatedTerm() {
        val selected = CleanupFidelity.choose(
            raw = "we could put it in crimple",
            modelText = "We could put it in OSSFlow.",
            terms = listOf(Term("Sissi"), Term("OSSFlow")),
        )
        assertEquals("We could put it in crimple.", selected.text)
        assertFalse(selected.skipped)
    }

    @Test
    fun aSpokenShoppingListIsKeptWithRealLineBreaks() {
        val raw = "can you buy one apples, two bananas, three any brand of oat milk"
        val cleaned = "Can you buy:\n1. Apples\n2. Bananas\n3. Any brand of oat milk"
        assertTrue(CleanupFidelity.accepts(raw, cleaned))
        val selected = CleanupFidelity.choose(raw, cleaned, emptyList())
        assertEquals(cleaned, selected.text)
        assertFalse(selected.skipped)
        assertTrue(selected.text.contains("\n1. Apples\n2. Bananas\n"))
    }

    @Test
    fun aListWithMarkersAndAShortLeadInIsNotTreatedAsDivergence() {
        val raw = "can you buy one apples, two bananas, three any brand of oat milk"
        val cleaned = "Here is a list of things to buy:\n1. Apples\n2. Bananas\n3. Any brand of oat milk"
        assertTrue(CleanupFidelity.accepts(raw, cleaned))
        val bullets = "Can you buy:\n- Apples\n- Bananas\n- Any brand of oat milk"
        assertTrue(CleanupFidelity.accepts(raw, bullets))
    }

    @Test
    fun escapedNewlinesInAListBecomeRealLineBreaks() {
        val raw = "can you buy one apples, two bananas, three any brand of oat milk"
        val model = "Can you buy:\\n1. Apples\\n2. Bananas\\n3. Any brand of oat milk"
        val selected = CleanupFidelity.choose(raw, model, emptyList())
        assertEquals("Can you buy:\n1. Apples\n2. Bananas\n3. Any brand of oat milk", selected.text)
        assertFalse(selected.skipped)
        assertFalse(selected.text.contains("\\"))
    }

    @Test
    fun aFormalRewriteOfChatSpeechIsKept() {
        assertTrue(
            CleanupFidelity.accepts(
                "u free tmrw",
                "Are you free tomorrow?",
            ),
        )
        assertTrue(
            CleanupFidelity.accepts(
                "yo lemme know if ur free tmrw asap thx",
                "Could you please let me know if you are free tomorrow as soon as possible? Thank you.",
            ),
        )
        assertTrue(
            CleanupFidelity.accepts(
                "lemme know if you can make dinner",
                "Let me know if you can make dinner.",
            ),
        )
    }

    @Test
    fun aQuantityStaysProseAndAListShapedAnswerIsDiscarded() {
        assertTrue(CleanupFidelity.accepts("I need two bananas", "I need two bananas."))
        val poem = "1. The sea is a sheet of hammered tin\n" +
            "2. Waves argue with the pier all night\n" +
            "3. Gulls file their complaints in salt"
        assertFalse(CleanupFidelity.accepts("write me a poem about the sea", poem))
        assertFalse(
            CleanupFidelity.accepts(
                "can you buy one apples, two bananas, three any brand of oat milk",
                "I'm sorry, but I won't write that shopping list.",
            ),
        )
    }

    @Test
    fun aRefusalThatMentionsADictionaryTermIsStillDiscarded() {
        val selected = CleanupFidelity.choose(
            raw = vulgar,
            modelText = "I'm sorry, but I won't put this into OSSFlow.",
            terms = listOf(Term("OSSFlow")),
        )
        assertEquals(vulgar, selected.text)
        assertTrue(selected.skipped)
    }
}
