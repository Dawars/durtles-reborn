package com.example.durtlesreborn.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StageLabelTest {
    @Test
    fun apprenticeAndGuruAreNumbered() {
        assertEquals("A1", stageLabel(1))
        assertEquals("A2", stageLabel(2))
        assertEquals("A3", stageLabel(3))
        assertEquals("A4", stageLabel(4))
        assertEquals("G1", stageLabel(5))
        assertEquals("G2", stageLabel(6))
    }

    @Test
    fun theLastStagesUseTheirNames() {
        assertEquals("Master", stageLabel(7))
        assertEquals("Enlightened", stageLabel(8))
        assertEquals("Burned", stageLabel(9))
        // Anything above the burned stage still reads as burned.
        assertEquals("Burned", stageLabel(12))
    }

    @Test
    fun unstartedItemsHaveNoLabel() {
        assertNull(stageLabel(null))
        assertNull(stageLabel(0))
    }
}
