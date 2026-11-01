package com.example.durtlesreborn.ui

import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerTypeColorsTest {
    @Test
    fun meaningIsCoolAndReadingIsWarm() {
        val meaning = answerTypeColors(LightScheme, reading = false)
        val reading = answerTypeColors(LightScheme, reading = true)
        val meaningHue = hueOf(meaning.background)
        val readingHue = hueOf(reading.background)
        assertTrue("meaning hue $meaningHue", meaningHue in 190f..240f)
        assertTrue("reading hue $readingHue", readingHue in 10f..60f)
        assertNotEquals(meaning.background, reading.background)
    }

    @Test
    fun bothChipsStayReadableOnLightAndDarkThemes() {
        for (scheme in listOf(LightScheme, DarkScheme, CrabcrocScheme, MatchaScheme)) {
            for (reading in listOf(false, true)) {
                val colors = answerTypeColors(scheme, reading)
                assertTrue(
                    "contrast for reading=$reading on ${scheme.background}",
                    contrastRatio(colors.background, colors.content) >= 4.5,
                )
            }
        }
    }

    @Test
    fun darkThemesUseADarkChipWithLightText() {
        for (reading in listOf(false, true)) {
            val colors = answerTypeColors(DarkScheme, reading)
            assertTrue(colors.background.luminance() < 0.25f)
            assertTrue(colors.content.luminance() > 0.5f)
        }
    }

    @Test
    fun theHueLeansTowardsTheThemeAtMost() {
        // Crabcroc's primary is magenta, so both chips may lean, but never past 18 degrees.
        val crab = answerTypeColors(CrabcrocScheme, reading = false)
        val plain = answerTypeColors(LightScheme, reading = false)
        assertTrue(
            hueOf(crab.background) in 192f..228f,
        )
        assertTrue(hueOf(plain.background) in 192f..228f)
    }
}
