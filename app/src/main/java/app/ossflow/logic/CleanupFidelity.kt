package app.ossflow.logic

import app.ossflow.model.Term

/**
 * The text to paste after cleanup, and whether the model reply was discarded.
 *
 * [skipped] is true when the reply was blank, a refusal, or not a faithful edit.
 * The pasted [text] is then the raw transcript. An API failure is handled by the caller,
 * which also pastes the raw transcript and sets this flag.
 */
data class CleanupSelection(val text: String, val skipped: Boolean)

/**
 * Decides whether a cleanup reply is a faithful edit of the dictated words.
 *
 * A reply is discarded when it is blank, when it is a refusal or an assistant-style
 * comment the speaker did not say, or when it diverges from the transcript.
 * Divergence means the reply shares too few words with the transcript, introduces a
 * long run of words that were not spoken, or is far longer or far shorter than the
 * transcript. A short repair still counts: "gonna send it" to "I am going to send it."
 * stays, and a self-correction that keeps only the final words stays.
 *
 * A formatted list is still an edit. Item numbers and bullets are layout, so they are
 * removed before the overlap check. Speech abbreviations are expanded on both sides,
 * so "u" matches "you" and "asap" matches "as soon as possible". That keeps a tone
 * change and a real list. Added facts and a rewritten answer still fail. A refusal is
 * judged on the original words, before that expansion.
 *
 * Refusal phrases include "I'm sorry", "I can't", "I won't", "As an AI", and
 * "feel free to". A phrase is ignored when the speaker said it, including a contraction
 * or a close wording of the same phrase ("I cannot", "I can't", and "I am not able").
 * "I'm sorry" is also kept when the speaker said "sorry" and the reply only adds "I" or "I'm"
 * around words they already said.
 *
 * One substituted word is not treated as divergence. "two" to "to" is a normal repair,
 * and a single softened word has the same shape. The cleanup prompt forbids that
 * softening when keep profanity is on. This check catches a reply that is a different text.
 */
object CleanupFidelity {
    /** Minimum share of cleaned words that must also appear in the transcript. */
    const val MIN_WORD_COVERAGE = 0.30

    /** This many new words, and more new words than preserved words, is a different text. */
    const val MIN_UNSPOKEN_WORDS = 8

    /** Cleaned word count above this multiple of the transcript is not an edit. */
    const val MAX_WORD_GROWTH = 2.5

    /** Word-count growth is ignored until the reply is at least this many words longer. */
    const val MIN_EXTRA_WORDS = 6

    /** Cleaned character count above this multiple of the transcript is not an edit. */
    const val MAX_CHAR_GROWTH = 3.0

    /** Character growth is ignored until the reply is at least this many characters longer. */
    const val MIN_EXTRA_CHARS = 80

    /**
     * A reply shorter than this fraction of the transcript is a summary, unless the words
     * that remain were spoken ([SHRINK_COVERAGE_KEEP]).
     */
    const val MIN_WORD_SHRINK = 0.25

    /** Shrink is only judged once the transcript is long enough for a real summary. */
    const val MIN_RAW_WORDS_FOR_SHRINK = 8

    /** A short reply is still an edit when at least this share of its words were spoken. */
    const val SHRINK_COVERAGE_KEEP = 0.80

    private val WORD = Regex("[a-z0-9]+(?:'[a-z0-9]+)*")

    /** A line that starts an item: "1." or "2)" or a bullet. */
    private val listMarker = Regex("""(?m)^[ \t]*(?:\d{1,3}[.)]|[-*•])[ \t]*""")

    private val expansions: Map<String, List<String>> = mapOf(
        "u" to listOf("you"),
        "ur" to listOf("your"),
        "gonna" to listOf("going", "to"),
        "wanna" to listOf("want", "to"),
        "gotta" to listOf("got", "to"),
        "lemme" to listOf("let", "me"),
        "kinda" to listOf("kind", "of"),
        "sorta" to listOf("sort", "of"),
        "cuz" to listOf("because"),
        "cos" to listOf("because"),
        "asap" to listOf("as", "soon", "as", "possible"),
        "thx" to listOf("thank"),
        "thanks" to listOf("thank"),
        "tmrw" to listOf("tomorrow"),
        "tmr" to listOf("tomorrow"),
        "ok" to listOf("okay"),
        "im" to listOf("i", "am"),
        "i'm" to listOf("i", "am"),
        "dont" to listOf("do", "not"),
        "don't" to listOf("do", "not"),
        "cant" to listOf("cannot"),
        "can't" to listOf("cannot"),
        "wont" to listOf("will", "not"),
        "won't" to listOf("will", "not"),
        "i'll" to listOf("i", "will"),
        "i've" to listOf("i", "have"),
        "i'd" to listOf("i", "would"),
        "we're" to listOf("we", "are"),
        "they're" to listOf("they", "are"),
        "you're" to listOf("you", "are"),
        "it's" to listOf("it", "is"),
        "that's" to listOf("that", "is"),
        "what's" to listOf("what", "is"),
        "let's" to listOf("let", "us"),
        "there's" to listOf("there", "is"),
        "here's" to listOf("here", "is"),
        "isn't" to listOf("is", "not"),
        "aren't" to listOf("are", "not"),
        "wasn't" to listOf("was", "not"),
        "weren't" to listOf("were", "not"),
        "hasn't" to listOf("has", "not"),
        "haven't" to listOf("have", "not"),
        "hadn't" to listOf("had", "not"),
        "doesn't" to listOf("does", "not"),
        "didn't" to listOf("did", "not"),
        "couldn't" to listOf("could", "not"),
        "shouldn't" to listOf("should", "not"),
        "wouldn't" to listOf("would", "not"),
    )

    private val SORRY_EXTRAS = setOf("i'm", "im", "i", "am")

    private val refusalGroups: List<RefusalGroup> = listOf(
        group("i'm sorry", "i am sorry", "im sorry", sorryExpansion = true),
        group(
            "i can't",
            "i cannot",
            "i can not",
            "i cant",
            "i'm unable",
            "i am unable",
            "im unable",
            "i'm not able",
            "i am not able",
        ),
        group("i won't", "i will not", "i wont"),
        group("as an ai", "as an a.i"),
        group("as a language model", "as an ai language model"),
        group("as an assistant"),
        group("feel free to"),
        group("i must decline", "i must refuse"),
        group(
            "i can't assist",
            "i cannot assist",
            "i can't help",
            "i cannot help",
            "i can't comply",
            "i cannot comply",
            "i can't provide",
            "i cannot provide",
        ),
        group(
            "i won't engage",
            "i will not engage",
            "i can't engage",
            "i cannot engage",
            "i won't process",
            "i will not process",
            "i can't process",
            "i cannot process",
        ),
        group("i'm not allowed", "i am not allowed", "im not allowed"),
        group(
            "i don't feel comfortable",
            "i do not feel comfortable",
            "i'm not comfortable",
            "i am not comfortable",
        ),
        group("content policy", "against my guidelines", "against my programming"),
        group(
            "let me know if you have",
            "let me know if you need",
            "if you have a different request",
            "please rephrase",
            "here is the cleaned",
            "here's the cleaned",
            "here is your cleaned",
            "here's your cleaned",
        ),
        group("i'm designed to", "i am designed to", "i'm programmed", "i am programmed"),
    )

    /**
     * Label stripping and the dictionary guard run first. A blank, refusal, or divergent
     * reply is replaced with [raw], and [CleanupSelection.skipped] is set.
     */
    fun choose(raw: String, modelText: String, terms: List<Term>): CleanupSelection {
        val presented = CleanupText.withoutLabel(CleanupText.normalizeBreaks(modelText), raw)
        if (presented.isBlank()) return CleanupSelection(text = raw, skipped = true)
        val guarded = DictionaryGuard.apply(raw, presented, terms)
        if (guarded.isBlank() || !accepts(raw, guarded)) {
            return CleanupSelection(text = raw, skipped = true)
        }
        return CleanupSelection(text = guarded, skipped = false)
    }

    /** True when [cleaned] can be pasted as an edit of [raw]. */
    fun accepts(raw: String, cleaned: String): Boolean {
        if (cleaned.isBlank()) return false
        val rawWords = words(raw)
        val cleanedWords = words(cleaned)
        if (cleanedWords.isEmpty()) return false
        if (hasUngroundedRefusal(rawWords, cleanedWords)) return false
        if (diverges(raw, cleaned)) return false
        return true
    }

    private fun hasUngroundedRefusal(rawWords: List<String>, cleanedWords: List<String>): Boolean {
        for (group in refusalGroups) {
            if (group.phrases.none { containsSequence(cleanedWords, it) }) continue
            if (group.phrases.any { containsSequence(rawWords, it) }) continue
            if (group.sorryExpansion && isSorryExpansion(rawWords, cleanedWords)) continue
            return true
        }
        return false
    }

    /**
     * "sorry" expanded to "I'm sorry" or "I am sorry", without any other new words,
     * and without dropping words the speaker said.
     */
    private fun isSorryExpansion(rawWords: List<String>, cleanedWords: List<String>): Boolean {
        if ("sorry" !in rawWords) return false
        val rawSet = rawWords.toSet()
        val cleanedSet = cleanedWords.toSet()
        if (!rawSet.all { it in cleanedSet }) return false
        return cleanedWords.all { it in rawSet || it in SORRY_EXTRAS }
    }

    private fun diverges(raw: String, cleaned: String): Boolean {
        val rawTokens = comparableTokens(raw, stripListMarkers = false)
        val cleanedTokens = comparableTokens(cleaned, stripListMarkers = true)
        if (cleanedTokens.isEmpty()) return true
        val rawSet = rawTokens.toSet()
        val distinct = cleanedTokens.distinct()
        val covered = distinct.count { it in rawSet }
        val coverage = covered.toDouble() / distinct.size
        if (covered == 0) return true
        if (distinct.size >= 4 && coverage < MIN_WORD_COVERAGE) return true
        val unspoken = distinct.size - covered
        if (unspoken >= MIN_UNSPOKEN_WORDS && unspoken > covered) return true
        val rawCount = rawTokens.size
        val cleanedCount = cleanedTokens.size
        if (rawCount > 0 &&
            cleanedCount > rawCount + MIN_EXTRA_WORDS &&
            cleanedCount.toDouble() / rawCount > MAX_WORD_GROWTH
        ) {
            return true
        }
        val rawChars = raw.trim().length
        val cleanedChars = cleaned.trim().length
        if (rawChars > 0 &&
            cleanedChars > rawChars + MIN_EXTRA_CHARS &&
            cleanedChars.toDouble() / rawChars > MAX_CHAR_GROWTH
        ) {
            return true
        }
        if (rawCount >= MIN_RAW_WORDS_FOR_SHRINK &&
            cleanedCount.toDouble() / rawCount < MIN_WORD_SHRINK &&
            coverage < SHRINK_COVERAGE_KEEP
        ) {
            return true
        }
        return false
    }

    /**
     * Words used for the overlap check.
     * List markers are removed only from a reply that is actually a list.
     * Abbreviations are expanded so a style rewrite still shares the speaker's words.
     */
    private fun comparableTokens(text: String, stripListMarkers: Boolean): List<String> {
        val source = if (stripListMarkers) withoutListMarkers(text) else text
        return words(source).flatMap { expandToken(it) }
    }

    private fun withoutListMarkers(text: String): String {
        if (listMarker.findAll(text).count() < 2) return text
        return listMarker.replace(text, "")
    }

    private fun expandToken(token: String): List<String> {
        expansions[token]?.let { return it }
        if (token.endsWith("'s") && token.length > 3) return listOf(token.dropLast(2))
        return listOf(token)
    }

    private fun containsSequence(tokens: List<String>, phrase: List<String>): Boolean {
        if (phrase.isEmpty() || tokens.size < phrase.size) return false
        val last = tokens.size - phrase.size
        for (start in 0..last) {
            var matches = true
            for (offset in phrase.indices) {
                if (tokens[start + offset] != phrase[offset]) {
                    matches = false
                    break
                }
            }
            if (matches) return true
        }
        return false
    }

    private fun words(text: String): List<String> {
        val normalized = text.lowercase()
            .replace('\u2019', '\'')
            .replace('\u2018', '\'')
            .replace('`', '\'')
        return WORD.findAll(normalized).map { it.value }.toList()
    }

    private fun group(vararg phrases: String, sorryExpansion: Boolean = false): RefusalGroup =
        RefusalGroup(phrases.map { words(it) }, sorryExpansion)

    private data class RefusalGroup(val phrases: List<List<String>>, val sorryExpansion: Boolean)
}
