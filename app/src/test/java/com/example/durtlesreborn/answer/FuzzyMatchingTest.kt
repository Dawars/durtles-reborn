package com.example.durtlesreborn.answer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyMatchingTest {
    private val accept = CloseEnoughAction.ACCEPT_WITH_TOAST

    @Test
    fun osaCountsTranspositionAsOneEdit() {
        assertEquals(1, optimalStringAlignmentDistance("ab", "ba"))
        assertEquals(2, optimalStringAlignmentDistance("kitten", "sittin"))
        assertEquals(3, optimalStringAlignmentDistance("", "abc"))
    }

    @Test
    fun exactMatchIsOkAndNotNear() {
        val v = FuzzyMatching.matches("Water", listOf("water"), emptyList(), accept)
        assertTrue(v.ok)
        assertFalse(v.nearMatch)
        assertEquals("water", v.matchedAnswer)
    }

    @Test
    fun shortAnswersGetNoLenience() {
        val v = FuzzyMatching.matches("dag", listOf("dog"), emptyList(), accept)
        assertFalse(v.ok)
        assertFalse(v.retry)
    }

    @Test
    fun lenienceGrowsWithLength() {
        assertTrue(FuzzyMatching.matches("watar", listOf("water"), emptyList(), accept).nearMatch)
        assertTrue(FuzzyMatching.matches("mountian", listOf("mountain"), emptyList(), accept).ok)
        assertFalse(FuzzyMatching.matches("wxtxr", listOf("water"), emptyList(), accept).ok)
    }

    @Test
    fun closeEnoughActionsDecideNearMatches() {
        fun run(a: CloseEnoughAction) = FuzzyMatching.matches("watar", listOf("water"), emptyList(), a)
        assertTrue(run(CloseEnoughAction.SILENTLY_ACCEPT).ok)
        assertEquals(AnswerVerdict.NOK_WITH_RETRY, run(CloseEnoughAction.SHAKE_AND_RETRY))
        assertEquals(AnswerVerdict.NOK_WITHOUT_RETRY, run(CloseEnoughAction.REJECT))
    }

    @Test
    fun betterRejectedAnswerBlocksTheMatch() {
        val v = FuzzyMatching.matches("fort", listOf("forty"), listOf("fort"), accept)
        assertFalse(v.ok)
    }

    @Test
    fun cleanStringNormalisesLookAlikes() {
        assertEquals("a-b 1", FuzzyMatching.cleanString("  A–B　１ "))
        assertEquals("it's", FuzzyMatching.cleanString("It’s"))
    }
}
