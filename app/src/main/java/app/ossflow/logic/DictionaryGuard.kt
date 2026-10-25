package app.ossflow.logic

import app.ossflow.model.Term
import kotlin.math.abs
import kotlin.math.max

/**
 * Puts raw words back when cleanup swaps in a personal-dictionary term the speaker did not say.
 *
 * A term is kept when the raw span matches one of its heard-as variants, or when that span is
 * close to the spelling. Closeness is the minimum of two normalised Levenshtein distances
 * (distance divided by the longer string), both computed on lowercased letters with spaces and
 * punctuation removed:
 *
 * - Spelling distance at most [ORTHOGRAPHIC_KEEP] (0.34). "oss flow" and "OSSFlow" are 0.
 *   "ossflo" and "OSSFlow" are 1/7, about 0.14. "crimple" and "OSSFlow" are 1. "crimple" and
 *   "Sissi" are 6/7, about 0.86.
 * - A phonetic key, used only when the spelling distance is still at most [PHONETIC_MAX_ORTHOGRAPHIC]
 *   (0.60) and both keys are at least [MIN_PHONETIC_KEY] characters. The key maps vowels to one
 *   letter and collapses repeats. The keys must be equal, or their distance must be at most
 *   [PHONETIC_KEEP] (0.25). This keeps "law view" beside "LawVu" and rejects "sorry" beside "Sissi",
 *   whose spelling distance is 0.80.
 *
 * Only the introduced term is rewritten. Punctuation, fillers, and other cleanup stay as the model
 * wrote them. A self-correction cue in the same span ("sorry", "I mean", "actually", "scratch that")
 * keeps the words after the cue, which is the final intent.
 */
object DictionaryGuard {
    /** Keep a term when the plain letter distance is at or below this. */
    const val ORTHOGRAPHIC_KEEP = 0.34

    /** Keep a term on the phonetic key when that key distance is at or below this. */
    const val PHONETIC_KEEP = 0.25

    /**
     * Phonetic agreement is ignored when the plain spelling distance is above this,
     * so a vaguely similar sound cannot save an unrelated word.
     */
    const val PHONETIC_MAX_ORTHOGRAPHIC = 0.60

    /** Phonetic keys shorter than this are too coarse to trust. */
    const val MIN_PHONETIC_KEY = 4

    private const val MAX_TERM_WINDOW = 8

    private val WORD = Regex("[\\p{L}\\p{N}]+(?:['’][\\p{L}\\p{N}]+)*")

    private val FILLERS = setOf("um", "uh", "er", "ah", "hmm", "hm", "mm", "mhm", "uhh", "umm", "err", "ahh")

    private val CORRECTION_CUES = listOf(
        listOf("scratch", "that"),
        listOf("i", "meant"),
        listOf("i", "mean"),
        listOf("sorry"),
        listOf("actually"),
    )

    /**
     * Returns [cleaned] with each unsupported dictionary term replaced by the raw words it stood in for.
     *
     * An empty [terms] list, or a cleaned transcript with no introduced term, is returned unchanged.
     * When a term was inserted with no raw words behind it, that term is removed and one adjacent
     * space goes with it.
     */
    fun apply(raw: String, cleaned: String, terms: List<Term>): String {
        if (raw == cleaned || cleaned.isEmpty()) return cleaned
        val prepared = prepare(terms)
        if (prepared.isEmpty()) return cleaned
        val rawWords = tokenize(raw)
        val cleanedWords = tokenize(cleaned)
        if (cleanedWords.isEmpty()) return cleaned
        val edits = collectEdits(raw, cleaned, rawWords, cleanedWords, prepared)
        if (edits.isEmpty()) return cleaned
        return applyEdits(cleaned, edits)
    }

    private fun prepare(terms: List<Term>): List<PreparedTerm> =
        terms.mapNotNull { term ->
            val spelling = term.spelling.trim()
            val spellingLetters = letters(spelling)
            if (spellingLetters.isEmpty()) return@mapNotNull null
            val wordCount = spelling.split(Regex("\\s+")).count { letters(it).isNotEmpty() }
            PreparedTerm(
                spelling = spelling,
                letters = spellingLetters,
                wordCount = wordCount.coerceAtLeast(1),
                heardAsLetters = term.heardAs.map(::letters).filter { it.isNotEmpty() }.distinct(),
            )
        }

    private fun collectEdits(
        raw: String,
        cleaned: String,
        rawWords: List<Word>,
        cleanedWords: List<Word>,
        terms: List<PreparedTerm>,
    ): List<Edit> {
        val anchors = lcsAnchors(rawWords.map { it.norm }, cleanedWords.map { it.norm })
        val edits = mutableListOf<Edit>()
        var rawStart = 0
        var cleanedStart = 0
        val boundaries = anchors + (rawWords.size to cleanedWords.size)
        for ((rawEnd, cleanedEnd) in boundaries) {
            if (rawStart < rawEnd || cleanedStart < cleanedEnd) {
                edits += hunkEdits(
                    raw,
                    cleaned,
                    rawWords.subList(rawStart, rawEnd),
                    cleanedWords.subList(cleanedStart, cleanedEnd),
                    terms,
                )
            }
            rawStart = rawEnd + 1
            cleanedStart = cleanedEnd + 1
        }
        return edits
    }

    private fun hunkEdits(
        raw: String,
        cleaned: String,
        rawWords: List<Word>,
        cleanedWords: List<Word>,
        terms: List<PreparedTerm>,
    ): List<Edit> {
        val matches = findTermMatches(cleaned, cleanedWords, terms)
        if (matches.isEmpty()) return emptyList()
        val alignNorms = cleanedWords.mapIndexed { index, word ->
            if (matches.any { index in it.first..it.last }) "\u0000$index" else word.norm
        }
        val anchors = lcsAnchors(rawWords.map { it.norm }, alignNorms)
        return matches.mapNotNull { match ->
            val gap = rawGap(rawWords, match, anchors)
            if (isSupported(raw, gap, match.terms)) return@mapNotNull null
            val focus = focusWords(gap)
            val replacement = if (focus.isEmpty()) "" else raw.substring(focus.first().start, focus.last().end)
            Edit(
                start = cleanedWords[match.first].start,
                end = cleanedWords[match.last].end,
                text = replacement,
            )
        }
    }

    private fun rawGap(rawWords: List<Word>, match: TermMatch, anchors: List<Pair<Int, Int>>): List<Word> {
        var left = -1
        var right = rawWords.size
        for ((rawIndex, cleanedIndex) in anchors) {
            if (cleanedIndex < match.first) left = rawIndex
            if (cleanedIndex > match.last && right == rawWords.size) right = rawIndex
        }
        val from = left + 1
        if (from >= right) return emptyList()
        return rawWords.subList(from, right)
    }

    private fun findTermMatches(text: String, words: List<Word>, terms: List<PreparedTerm>): List<TermMatch> {
        if (words.isEmpty()) return emptyList()
        val matches = mutableListOf<TermMatch>()
        var index = 0
        while (index < words.size) {
            val match = bestMatchAt(text, words, index, terms)
            if (match == null) {
                index++
            } else {
                matches += match
                index = match.last + 1
            }
        }
        return matches
    }

    private fun bestMatchAt(text: String, words: List<Word>, start: Int, terms: List<PreparedTerm>): TermMatch? {
        var best: TermMatch? = null
        val last = minOf(words.size, start + MAX_TERM_WINDOW)
        for (end in start until last) {
            val joined = joinedLetters(text, words.subList(start, end + 1))
            if (joined.isEmpty()) continue
            val matched = terms.filter { it.letters == joined }
            if (matched.isEmpty()) continue
            val candidate = TermMatch(start, end, matched)
            val current = best
            if (current == null || preferWindow(candidate, current)) best = candidate
        }
        return best
    }

    private fun preferWindow(candidate: TermMatch, current: TermMatch): Boolean {
        val candidateLetters = candidate.terms.first().letters.length
        val currentLetters = current.terms.first().letters.length
        if (candidateLetters != currentLetters) return candidateLetters > currentLetters
        val candidateSkew = candidate.terms.minOf { abs((candidate.last - candidate.first + 1) - it.wordCount) }
        val currentSkew = current.terms.minOf { abs((current.last - current.first + 1) - it.wordCount) }
        return candidateSkew < currentSkew
    }

    private fun isSupported(raw: String, gap: List<Word>, terms: List<PreparedTerm>): Boolean {
        val focus = focusWords(gap)
        if (focus.isEmpty()) return false
        val span = raw.substring(focus.first().start, focus.last().end)
        return terms.any { term ->
            term.heardAsLetters.any { soundsLike(span, it) } || soundsLike(span, term.spelling)
        }
    }

    /**
     * The raw words the dictionary term actually replaced.
     *
     * Edge fillers, including "you know", stay deleted. After a self-correction cue, the words
     * that follow are what the speaker settled on. A cue with nothing after it leaves the words
     * that came before the cue.
     */
    private fun focusWords(words: List<Word>): List<Word> {
        if (words.isEmpty()) return words
        val trimmed = trimFillers(words)
        val base = trimmed.ifEmpty { words }
        val cue = lastCue(base) ?: return base
        val after = trimFillers(base.subList(cue.end, base.size))
        if (after.isNotEmpty()) return after
        val before = trimFillers(base.subList(0, cue.start))
        return before.ifEmpty { base }
    }

    private fun trimFillers(words: List<Word>): List<Word> {
        var start = 0
        var end = words.size
        while (start < end) {
            val width = leadingFillerWidth(words, start, end)
            if (width == 0) break
            start += width
        }
        while (end > start) {
            val width = trailingFillerWidth(words, start, end)
            if (width == 0) break
            end -= width
        }
        return words.subList(start, end)
    }

    private fun leadingFillerWidth(words: List<Word>, start: Int, end: Int): Int {
        if (words[start].norm in FILLERS) return 1
        if (start + 1 < end && words[start].norm == "you" && words[start + 1].norm == "know") return 2
        return 0
    }

    private fun trailingFillerWidth(words: List<Word>, start: Int, end: Int): Int {
        if (words[end - 1].norm in FILLERS) return 1
        if (end - 2 >= start && words[end - 2].norm == "you" && words[end - 1].norm == "know") return 2
        return 0
    }

    private fun lastCue(words: List<Word>): Cue? {
        var found: Cue? = null
        for (start in words.indices) {
            for (cue in CORRECTION_CUES) {
                val end = start + cue.size
                if (end > words.size) continue
                val matches = cue.indices.all { words[start + it].norm == cue[it] }
                if (!matches) continue
                val previous = found
                if (previous == null || end >= previous.end) found = Cue(start, end)
            }
        }
        return found
    }

    private fun applyEdits(cleaned: String, edits: List<Edit>): String {
        val ordered = edits.sortedByDescending { it.start }
        var text = cleaned
        for (edit in ordered) {
            val bounds = if (edit.text.isEmpty()) expandDeletion(text, edit.start, edit.end) else edit.start to edit.end
            text = text.substring(0, bounds.first) + edit.text + text.substring(bounds.second)
        }
        return text
    }

    private fun expandDeletion(text: String, start: Int, end: Int): Pair<Int, Int> {
        if (start > 0 && text[start - 1].isWhitespace()) return start - 1 to end
        if (end < text.length && text[end].isWhitespace()) return start to end + 1
        return start to end
    }

    private fun tokenize(text: String): List<Word> =
        WORD.findAll(text).map { match ->
            val token = match.value
            Word(
                norm = letters(stripPossessive(token)),
                start = match.range.first,
                end = match.range.last + 1,
            )
        }.filter { it.norm.isNotEmpty() }.toList()

    private fun joinedLetters(text: String, words: List<Word>): String = buildString {
        words.forEachIndexed { index, word ->
            val token = text.substring(word.start, word.end)
            val bare = if (index == words.lastIndex) stripPossessive(token) else token
            append(letters(bare))
        }
    }

    private fun stripPossessive(token: String): String {
        val lower = token.lowercase()
        if (lower.endsWith("'s") || lower.endsWith("’s")) return token.dropLast(2)
        return token
    }

    private fun lcsAnchors(raw: List<String>, cleaned: List<String>): List<Pair<Int, Int>> {
        val rawCount = raw.size
        val cleanedCount = cleaned.size
        if (rawCount == 0 || cleanedCount == 0) return emptyList()
        val lengths = Array(rawCount + 1) { IntArray(cleanedCount + 1) }
        for (rawIndex in rawCount - 1 downTo 0) {
            for (cleanedIndex in cleanedCount - 1 downTo 0) {
                lengths[rawIndex][cleanedIndex] = if (raw[rawIndex] == cleaned[cleanedIndex]) {
                    lengths[rawIndex + 1][cleanedIndex + 1] + 1
                } else {
                    max(lengths[rawIndex + 1][cleanedIndex], lengths[rawIndex][cleanedIndex + 1])
                }
            }
        }
        val anchors = mutableListOf<Pair<Int, Int>>()
        var rawIndex = 0
        var cleanedIndex = 0
        while (rawIndex < rawCount && cleanedIndex < cleanedCount) {
            if (raw[rawIndex] == cleaned[cleanedIndex]) {
                anchors += rawIndex to cleanedIndex
                rawIndex++
                cleanedIndex++
            } else if (lengths[rawIndex + 1][cleanedIndex] >= lengths[rawIndex][cleanedIndex + 1]) {
                rawIndex++
            } else {
                cleanedIndex++
            }
        }
        return anchors
    }

    private fun soundsLike(heard: String, target: String): Boolean {
        val left = letters(heard)
        val right = letters(target)
        if (left.isEmpty() || right.isEmpty()) return false
        if (left == right) return true
        val spellingDistance = normalizedDistance(left, right)
        if (spellingDistance <= ORTHOGRAPHIC_KEEP) return true
        if (spellingDistance > PHONETIC_MAX_ORTHOGRAPHIC) return false
        val leftKey = skeleton(left)
        val rightKey = skeleton(right)
        if (leftKey.length < MIN_PHONETIC_KEY || rightKey.length < MIN_PHONETIC_KEY) return false
        if (leftKey == rightKey) return true
        return normalizedDistance(leftKey, rightKey) <= PHONETIC_KEEP
    }

    private fun letters(text: String): String = buildString(text.length) {
        for (character in text) {
            if (character.isLetterOrDigit()) append(character.lowercaseChar())
        }
    }

    private fun skeleton(letterText: String): String = buildString(letterText.length) {
        letterText.forEachIndexed { index, character ->
            val mapped = when {
                character == 'a' || character == 'e' || character == 'i' || character == 'o' || character == 'u' -> 'a'
                character == 'y' && index > 0 -> 'a'
                else -> character
            }
            if (isEmpty() || last() != mapped) append(mapped)
        }
    }

    private fun normalizedDistance(left: String, right: String): Double {
        val denominator = max(left.length, right.length)
        if (denominator == 0) return 0.0
        return levenshtein(left, right).toDouble() / denominator
    }

    private fun levenshtein(left: String, right: String): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length
        var previous = IntArray(right.length + 1) { it }
        var current = IntArray(right.length + 1)
        for (leftIndex in left.indices) {
            current[0] = leftIndex + 1
            for (rightIndex in right.indices) {
                val substitution = if (left[leftIndex] == right[rightIndex]) 0 else 1
                current[rightIndex + 1] = minOf(
                    current[rightIndex] + 1,
                    previous[rightIndex + 1] + 1,
                    previous[rightIndex] + substitution,
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[right.length]
    }

    private data class Word(val norm: String, val start: Int, val end: Int)

    private data class PreparedTerm(
        val spelling: String,
        val letters: String,
        val wordCount: Int,
        val heardAsLetters: List<String>,
    )

    private data class TermMatch(val first: Int, val last: Int, val terms: List<PreparedTerm>)

    private data class Edit(val start: Int, val end: Int, val text: String)

    private data class Cue(val start: Int, val end: Int)
}
