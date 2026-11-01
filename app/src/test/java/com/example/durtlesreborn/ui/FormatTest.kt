package com.example.durtlesreborn.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour

    @Test
    fun pastOrZeroIsNow() {
        assertEquals("now", formatUntil(0))
        assertEquals("now", formatUntil(-5 * minute))
    }

    @Test
    fun roundsPartialMinutesUp() {
        assertEquals("in 1 min", formatUntil(1))
        assertEquals("in 25 min", formatUntil(24 * minute + 1))
    }

    @Test
    fun combinesUnits() {
        assertEquals("in 3 h 20 min", formatUntil(3 * hour + 20 * minute))
        assertEquals("in 3 h", formatUntil(3 * hour))
        assertEquals("in 2 d 4 h", formatUntil(2 * day + 4 * hour))
        assertEquals("in 2 d", formatUntil(2 * day))
    }
}
