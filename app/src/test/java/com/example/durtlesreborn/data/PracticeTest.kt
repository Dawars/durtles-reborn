package com.example.durtlesreborn.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import com.example.durtlesreborn.radicals.withoutCssVariables
import org.junit.Test

class PracticeTest {
    private fun row(meaningWrong: Int, readingWrong: Int, meaningStreak: Int, readingStreak: Int) =
        LeechRow(1, meaningWrong, readingWrong, meaningStreak, readingStreak)

    @Test
    fun aLeechHasManyMistakesForItsShortestStreak() {
        assertTrue(isLeech(row(3, 0, 1, 1)))
        assertTrue(isLeech(row(0, 8, 9, 4)))
        assertFalse(isLeech(row(7, 0, 4, 9)))
    }

    @Test
    fun noStreakIsNotALeech() {
        assertFalse(isLeech(row(20, 20, 0, 3)))
    }

    private fun images(vararg items: String) = SubjectContent.parse("""{"character_images":[${items.joinToString(",")}]}""")

    @Test
    fun onlyAnSvgWithItsOwnStylesIsUsed() {
        val png = """{"url":"https://files.wanikani.com/l","content_type":"image/png","metadata":{"dimensions":"1024x1024"}}"""
        val plain = """{"url":"https://files.wanikani.com/a","content_type":"image/svg+xml","metadata":{"inline_styles":false}}"""
        val styled = """{"url":"https://files.wanikani.com/b","content_type":"image/svg+xml","metadata":{"inline_styles":true}}"""
        assertEquals("https://files.wanikani.com/b", images(png, plain, styled).characterImageUrl)
        assertNull(images(png, plain).characterImageUrl)
    }

    @Test
    fun cssVariablesBecomeTheirFallback() {
        assertEquals(".b{stroke:#000;fill:none}", withoutCssVariables(".b{stroke:var(--color-text, #000);fill:none}"))
    }
}
