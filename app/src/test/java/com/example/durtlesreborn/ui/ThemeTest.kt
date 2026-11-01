package com.example.durtlesreborn.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {
    /** WCAG contrast ratio between two colours. */
    private fun contrast(a: Color, b: Color): Double {
        val l1 = a.luminance().toDouble()
        val l2 = b.luminance().toDouble()
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private val fixed = AppTheme.entries.mapNotNull { t -> fixedScheme(t)?.let { t to it } }

    @Test
    fun everyFixedThemeKeepsTextReadable() {
        for ((theme, c) in fixed) {
            val pairs = listOf(
                "text on background" to (c.onBackground to c.background),
                "text on surface" to (c.onSurface to c.surface),
                "secondary text on background" to (c.onSurfaceVariant to c.background),
                "secondary text on variant" to (c.onSurfaceVariant to c.surfaceVariant),
                "button label" to (c.onPrimary to c.primary),
                "label on container" to (c.onPrimaryContainer to c.primaryContainer),
                "link text on background" to (c.primary to c.background),
                "error text on background" to (c.error to c.background),
            )
            for ((what, p) in pairs) {
                val ratio = contrast(p.first, p.second)
                assertTrue("$theme $what has contrast $ratio, needs 4.5", ratio >= 4.5)
            }
            // Borders and icons need 3:1 against the background.
            assertTrue("$theme outline", contrast(c.outline, c.background) >= 3.0)
        }
    }

    @Test
    fun subjectColoursAreReadableAsLargeTextOnTheirBackgrounds() {
        for ((theme, c) in fixed) {
            val palette = paletteFor(theme, isDarkSurface(theme, systemDark = false)).subjects
            for ((name, colour) in listOf("radical" to palette.radical, "kanji" to palette.kanji, "vocabulary" to palette.vocabulary)) {
                val ratio = contrast(colour, c.background)
                assertTrue("$theme $name has contrast $ratio on its background, needs 3", ratio >= 3.0)
            }
        }
    }

    @Test
    fun everyThemesTilesKeepTheirTextReadable() {
        val palettes = fixed.map { (theme, _) -> "$theme" to paletteFor(theme, isDarkSurface(theme, false)) } +
            listOf("DYNAMIC light" to paletteFor(AppTheme.DYNAMIC, darkSurface = false)) +
            listOf("DYNAMIC dark" to paletteFor(AppTheme.DYNAMIC, darkSurface = true))
        for ((name, p) in palettes) {
            val colours = mapOf(
                "radical" to p.subjects.radical, "kanji" to p.subjects.kanji, "vocabulary" to p.subjects.vocabulary,
                "apprentice" to p.stages.apprentice, "guru" to p.stages.guru, "master" to p.stages.master,
                "enlightened" to p.stages.enlightened, "burned" to p.stages.burned,
                "lessons" to p.actions.lessons, "reviews" to p.actions.reviews, "locked" to p.actions.locked,
            )
            for ((what, colour) in colours) {
                val ratio = contrastRatio(contentColorOn(colour), colour)
                assertTrue("$name $what: text contrast $ratio, needs 4.5", ratio >= 4.5)
            }
        }
    }

    @Test
    fun stageColoursStandOutFromTheirBackground() {
        for ((theme, c) in fixed) {
            val stages = paletteFor(theme, isDarkSurface(theme, systemDark = false)).stages
            for ((name, colour) in listOf("apprentice" to stages.apprentice, "guru" to stages.guru, "master" to stages.master, "enlightened" to stages.enlightened, "burned" to stages.burned)) {
                val ratio = contrastRatio(colour, c.background)
                assertTrue("$theme $name stage colour has contrast $ratio on its background, needs 3", ratio >= 3.0)
            }
        }
    }

    @Test
    fun theStagePaletteHasFiveDistinctColours() {
        for (dark in listOf(false, true)) {
            val s = stagePalette(dark)
            assertEquals(5, setOf(s.apprentice, s.guru, s.master, s.enlightened, s.burned).size)
        }
    }

    @Test
    fun blackThemeIsReallyBlack() {
        assertEquals(Color.Black, BlackScheme.background)
        assertEquals(Color.Black, BlackScheme.surface)
    }

    @Test
    fun aChosenThemeLooksTheSameInLightAndDarkSystemMode() {
        for (theme in AppTheme.entries.filter { it != AppTheme.DYNAMIC }) {
            assertEquals("$theme in light mode", isDarkSurface(theme, systemDark = false), isDarkSurface(theme, systemDark = true))
        }
        assertEquals(false, isDarkSurface(AppTheme.MATCHA, systemDark = true))
        assertEquals(true, isDarkSurface(AppTheme.FOREST, systemDark = false))
    }

    @Test
    fun everyThemeHasALabelAndAUniqueName() {
        assertEquals(AppTheme.entries.size, AppTheme.entries.map { it.label }.toSet().size)
        assertEquals(10, AppTheme.entries.size)
        // Only DYNAMIC has no fixed colours of its own.
        assertEquals(listOf(AppTheme.DYNAMIC), AppTheme.entries.filter { fixedScheme(it) == null })
    }

    @Test
    fun everyThemeHasItsOwnBackground() {
        // Dark and Black may share an accent, but no two themes share a background.
        val backgrounds = fixed.map { (_, c) -> c.background }
        assertEquals(backgrounds.size, backgrounds.toSet().size)
    }

    @Test
    fun dynamicThemeFollowsTheSystemForItsSurface() {
        assertEquals(true, isDarkSurface(AppTheme.DYNAMIC, systemDark = true))
        assertEquals(false, isDarkSurface(AppTheme.DYNAMIC, systemDark = false))
        // A dark theme chosen for the light slot is still dark.
        assertEquals(true, isDarkSurface(AppTheme.NORD_POLAR_NIGHT, systemDark = false))
        assertNull(fixedScheme(AppTheme.DYNAMIC))
    }

    @Test
    fun unknownNamesFallBack() {
        assertEquals(AppTheme.DARK, AppTheme.fromName("WK_NOPE", AppTheme.DARK))
        assertEquals(AppTheme.NORD_POLAR_NIGHT, AppTheme.fromName("NORD_POLAR_NIGHT", AppTheme.DARK))
    }
}
