package com.example.durtlesreborn.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class StageBubbleTest {
    @Test
    fun stageNamesFollowWanikani() {
        assertEquals("Apprentice 1", stageName(1))
        assertEquals("Apprentice 4", stageName(4))
        assertEquals("Guru 1", stageName(5))
        assertEquals("Guru 2", stageName(6))
        assertEquals("Master", stageName(7))
        assertEquals("Enlightened", stageName(8))
        assertEquals("Burned", stageName(9))
    }

    @Test
    fun aMissingStageFallsBackToApprenticeOne() {
        assertEquals("Apprentice 1", stageName(0))
    }
}
