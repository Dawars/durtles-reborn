package com.example.durtlesreborn.ui

import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackColorsTest {
    private fun assertGreen(hue: Float) {
        assertTrue("hue $hue should stay green", hue in 125f..175f)
    }

    @Test
    fun aCorrectAnswerIsGreenInLightThemes() {
        val colors = correctFeedbackColors(LightScheme)
        assertGreen(hueOf(colors.container))
        // Pale container, dark readable text, like the error container it sits beside.
        assertTrue(colors.container.luminance() > 0.55f)
        assertTrue(colors.content.luminance() < 0.15f)
        assertTrue(contrastRatio(colors.container, colors.content) >= 4.5)
    }

    @Test
    fun aCorrectAnswerIsGreenInDarkThemes() {
        val colors = correctFeedbackColors(DarkScheme)
        assertGreen(hueOf(colors.container))
        assertTrue(colors.container.luminance() < 0.2f)
        assertTrue(colors.content.luminance() > 0.5f)
        assertTrue(contrastRatio(colors.container, colors.content) >= 4.5)
    }

    @Test
    fun theGreenLeansTowardsTheThemesPrimary() {
        // Crabcroc's primary is magenta; the green may shift, but only slightly.
        val colors = correctFeedbackColors(CrabcrocScheme)
        val hue = hueOf(colors.container)
        assertGreen(hue)
        assertTrue("should lean past plain green", hue > 145f)
        assertTrue("but never far", hue <= 157f)
    }

    @Test
    fun aGreenThemeKeepsTheGreenWhereItIs() {
        val colors = correctFeedbackColors(MatchaScheme)
        val hue = hueOf(colors.container)
        assertGreen(hue)
        assertTrue("only a small nudge", kotlin.math.abs(hue - 145f) <= 12f)
    }

    @Test
    fun aWrongAnswerUsesTheThemesRed() {
        assertEquals(WrongLight.container, LightScheme.errorContainer)
        assertEquals(WrongLight.content, LightScheme.onErrorContainer)
        assertTrue(contrastRatio(WrongLight.container, WrongLight.content) >= 4.5)
        assertTrue(contrastRatio(WrongDark.container, WrongDark.content) >= 4.5)
    }

    @Test
    fun harmonizeTakesTheShortWayRoundTheHueWheel() {
        assertEquals(157f, harmonize(145f, 325f, 12f), 0.01f)
        assertEquals(2f, harmonize(350f, 10f, 12f), 0.01f)
        assertEquals(145f, harmonize(145f, 145f, 12f), 0.01f)
    }

    private val WrongLight = wrongFeedbackColors(LightScheme)
    private val WrongDark = wrongFeedbackColors(DarkScheme)
}
