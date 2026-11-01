package com.example.durtlesreborn.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class PitchAccentTest {
    private val sample =
        "日本\tにほん\t2\n" +
            "日本\tにっぽん\t3\n" +
            "日\tにち\t1\n" +
            "日\tひ\t0\n" +
            "一\tいち\t0,2\n" +
            "食べる\tたべる\t2\n"

    private fun repository(text: String) = PitchRepository { ByteArrayInputStream(text.toByteArray()) }

    @Test
    fun moraeKeepSmallKanaWithTheKanaBeforeThem() {
        assertEquals(listOf("に", "ほ", "ん"), morae("にほん"))
        assertEquals(listOf("きょ", "う"), morae("きょう"))
        assertEquals(listOf("が", "っ", "こ", "う"), morae("がっこう"))
        assertEquals(listOf("ティ", "ー"), morae("ティー"))
    }

    @Test
    fun patternsFollowTheStandardRules() {
        // 0 is heiban (first low, rest high), 1 drops after the first high mora.
        assertEquals("LHH", pitchPattern(3, 0))
        assertEquals("HLL", pitchPattern(3, 1))
        assertEquals("LHL", pitchPattern(3, 2))
        // A drop after the last mora looks like heiban in the word itself.
        assertEquals("LHH", pitchPattern(3, 3))
        assertEquals("LHHLL", pitchPattern(5, 3))
        // Positions past the end of the word are read as a drop after the last mora.
        assertEquals("LHH", pitchPattern(3, 9))
        assertEquals("", pitchPattern(0, 1))
    }

    @Test
    fun readingsComeBackInTheOrderAskedFor() = runBlocking {
        val accents = repository(sample).lookup("日本", listOf("にっぽん", "にほん"))
        assertEquals(
            listOf("にっぽん" to listOf(3), "にほん" to listOf(2)),
            accents.map { it.reading to it.positions },
        )
    }

    @Test
    fun aReadingWithTwoPatternsKeepsBoth() = runBlocking {
        val accents = repository(sample).lookup("一", listOf("いち"))
        assertEquals(listOf(0, 2), accents.single().positions)
    }

    @Test
    fun readingsTheSourceDoesNotHaveAreSkipped() = runBlocking {
        // The source has にち and ひ for 日, but not じつ.
        val accents = repository(sample).lookup("日", listOf("じつ", "にち", "ひ"))
        assertEquals(listOf("にち", "ひ"), accents.map { it.reading })
    }

    @Test
    fun unknownWordsAndEmptyReadingsReturnNothing() = runBlocking {
        assertTrue(repository(sample).lookup("猫", listOf("ねこ")).isEmpty())
        assertTrue(repository(sample).lookup("日", emptyList()).isEmpty())
    }

    @Test
    fun kanaOnlyWordsFindTheDictionarySpellingWhenOnePatternIsLeft() = runBlocking {
        // The source lists the greeting as お早う / 御早う, both with the same pattern.
        val text = "お早う\tおはよう\t2\n御早う\tおはよう\t2\n"
        assertEquals(listOf(2), repository(text).lookup("おはよう", listOf("おはよう")).single().positions)
    }

    @Test
    fun homophonesOfAKanaOnlyWordAreNeverGuessed() = runBlocking {
        // これ is 此れ [1] and 是 [0]: two patterns, so no answer is better than a wrong one.
        val text = "此れ\tこれ\t1\n是\tこれ\t0\n"
        assertTrue(repository(text).lookup("これ", listOf("これ")).isEmpty())
    }

    @Test
    fun kanjiWordsDoNotUseTheKanaFallback() = runBlocking {
        // 日 is not in the source, and 火's ひ must not be shown for it.
        assertTrue(repository("火\tひ\t1\n").lookup("日", listOf("ひ")).isEmpty())
    }

    @Test
    fun malformedLinesAreIgnored() = runBlocking {
        val text = "no tabs here\n" + "word\treading\n" + "word\treading\tnope\n" + "日\tひ\t0\n"
        assertEquals(listOf("ひ"), repository(text).lookup("日", listOf("ひ")).map { it.reading })
    }
}
