package com.example.durtlesreborn.data

import com.example.durtlesreborn.session.SubjectKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonInfoTest {
    private val kanji = """
        {"level":2,"slug":"water","characters":"水",
         "meanings":[{"meaning":"Water","primary":true,"accepted_answer":true},{"meaning":"Aqua","primary":false,"accepted_answer":false}],
         "readings":[{"type":"kunyomi","reading":"みず","primary":false,"accepted_answer":false},
                     {"type":"onyomi","reading":"すい","primary":true,"accepted_answer":true}],
         "meaning_mnemonic":"This looks like <radical>water</radical> &amp; <kanji>splash</kanji>.<br>Easy.",
         "reading_mnemonic":"  Say <reading>sui</reading>.  ",
         "meaning_hint":null}
    """.trimIndent()

    private val vocab = """
        {"level":3,"characters":"水曜日","meanings":[{"meaning":"Wednesday","primary":true,"accepted_answer":true}],
         "readings":[{"reading":"すいようび","primary":true,"accepted_answer":true}],
         "parts_of_speech":["noun"],
         "context_sentences":[{"ja":"水曜日に行く。","en":"I go on Wednesday."},{"ja":"水曜日。"}]}
    """.trimIndent()

    @Test
    fun markupIsStrippedAndEntitiesDecoded() {
        assertEquals("a & b\nc", stripMarkup("<radical>a</radical> &amp; b<br/>c"))
        assertEquals("1 < 2", stripMarkup("1 &lt; 2"))
    }

    @Test
    fun kanjiCardHasAcceptedMeaningsAndPrimaryReadingFirst() {
        val info = SubjectContent.parse(kanji).lessonInfo("水", "kanji", 2)
        assertEquals(SubjectKind.KANJI, info.kind)
        assertEquals(listOf("Water"), info.meanings)
        assertEquals(listOf("すい", "みず"), info.readings.map { it.text })
        assertEquals("This looks like water & splash.\nEasy.", info.meaningMnemonic)
        assertEquals("Say sui.", info.readingMnemonic)
        assertNull(info.meaningHint)
        assertNull(info.readingHint)
    }

    @Test
    fun vocabularyCardHasSentencesAndPartsOfSpeech() {
        val info = SubjectContent.parse(vocab).lessonInfo("水曜日", "vocabulary", 3)
        assertEquals(listOf("noun"), info.partsOfSpeech)
        assertEquals(2, info.contextSentences.size)
        assertEquals("I go on Wednesday.", info.contextSentences[0].english)
        assertEquals("", info.contextSentences[1].english)
        assertEquals(listOf("すいようび"), info.readings.map { it.text })
    }

    @Test
    fun vocabularyAudioIsReadAndKanjiHasNone() {
        val withAudio = """
            {"level":1,"characters":"水","pronunciation_audios":[
              {"url":"https://files.wanikani.com/a.mp3","metadata":{"gender":"female"}},
              {"url":"https://files.wanikani.com/b.mp3","metadata":{"gender":"male"}}]}
        """.trimIndent()
        val vod = SubjectContent.parse(withAudio).lessonInfo("水", "vocabulary", 1, id = 42)
        assertTrue(vod.hasAudio)
        assertEquals(42, vod.id)
        assertEquals(
            listOf("https://files.wanikani.com/a.mp3", "https://files.wanikani.com/b.mp3"),
            SubjectContent.parse(withAudio).pronunciationAudios,
        )
        val kanjiInfo = SubjectContent.parse(kanji).lessonInfo("水", "kanji", 2)
        assertFalse(kanjiInfo.hasAudio)
        assertTrue(SubjectContent.parse(kanji).pronunciationAudios.isEmpty())
    }
}
