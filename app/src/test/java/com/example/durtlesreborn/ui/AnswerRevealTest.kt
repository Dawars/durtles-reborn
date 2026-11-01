package com.example.durtlesreborn.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnswerRevealTest {
    @Test
    fun aCorrectAnswerShowsTheReading() {
        assertEquals("ゆうすい", confirmationReading(answered = true, correct = true, reading = "ゆうすい"))
    }

    @Test
    fun aWrongAnswerNeverShowsTheReading() {
        assertNull(confirmationReading(answered = true, correct = false, reading = "ゆうすい"))
    }

    @Test
    fun theReadingStaysHiddenUntilTheQuestionIsAnswered() {
        assertNull(confirmationReading(answered = false, correct = true, reading = "ゆうすい"))
        assertNull(confirmationReading(answered = false, correct = null, reading = "ゆうすい"))
    }

    @Test
    fun anAnsweredQuestionWithNoVerdictShowsNothing() {
        assertNull(confirmationReading(answered = true, correct = null, reading = "ゆうすい"))
    }

    @Test
    fun anItemWithoutAReadingShowsNothing() {
        assertNull(confirmationReading(answered = true, correct = true, reading = null))
    }
}
