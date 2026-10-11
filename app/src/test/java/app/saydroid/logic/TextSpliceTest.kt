package app.saydroid.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class TextSpliceTest {
    @Test
    fun emptyFieldBecomesTheInsertion() {
        assertEquals("Hello", TextSplice.splice("", -1, -1, "Hello"))
    }

    @Test
    fun unknownCursorAppendsWithASpace() {
        assertEquals("Hello there", TextSplice.splice("Hello", -1, -1, "there"))
    }

    @Test
    fun existingTrailingSpaceIsKept() {
        assertEquals("Hello there", TextSplice.splice("Hello ", -1, -1, "there"))
    }

    @Test
    fun cursorInsertsBetweenWords() {
        assertEquals("the big cat", TextSplice.splice("thecat", 3, 3, "big"))
    }

    @Test
    fun selectionReplacesTheHighlightedWord() {
        assertEquals("the dog sat", TextSplice.splice("the cat sat", 4, 7, "dog"))
    }

    @Test
    fun emptyInsertionLeavesTheFieldAlone() {
        assertEquals("Hello", TextSplice.splice("Hello", 0, 5, ""))
    }
}
