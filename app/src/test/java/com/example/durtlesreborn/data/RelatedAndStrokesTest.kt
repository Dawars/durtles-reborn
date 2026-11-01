package com.example.durtlesreborn.data

import com.example.durtlesreborn.strokes.StrokeRepository
import com.example.durtlesreborn.strokes.parseStrokeTable
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RelatedAndStrokesTest {
    private fun subject(id: Long, type: String, chars: String, level: Int = 5, extra: String = "", hidden: Boolean = false) = SubjectEntity(
        id = id, type = type, level = level, slug = chars, characters = chars, hidden = hidden, dataUpdatedAt = 0,
        json = """{"level":$level,"meanings":[{"meaning":"M$id","primary":true,"accepted_answer":true}],
            "readings":[{"reading":"r$id","primary":true,"accepted_answer":true,"type":"onyomi"}]$extra}""",
    )

    private fun fake() = FakeSubjects().also { s ->
        listOf(
            subject(1, "radical", "一", extra = ""","amalgamation_subject_ids":[10,11]"""),
            subject(10, "kanji", "二", extra = ""","component_subject_ids":[1],"visually_similar_subject_ids":[11,12,13],"amalgamation_subject_ids":[20]"""),
            subject(11, "kanji", "三"),
            subject(12, "kanji", "四", level = 40),
            subject(13, "kanji", "五", hidden = true),
            subject(20, "vocabulary", "二つ", extra = ""","component_subject_ids":[10]"""),
        ).forEach { s.rows[it.id] = it }
    }

    private fun <T> run(block: suspend () -> T) = runBlocking { block() }

    @Test
    fun aKanjiListsItsRadicalsSimilarKanjiAndVocabulary() = run {
        val subjects = fake()
        val content = SubjectContent.parse(subjects.rows.getValue(10).json)
        val groups = subjects.relatedGroups("kanji", content, maxLevel = 60).associateBy { it.label }
        assertEquals(listOf(1L), groups.getValue("Radicals").items.map { it.id })
        // Hidden subjects are never shown.
        assertEquals(listOf(11L, 12L), groups.getValue("Visually similar kanji").items.map { it.id })
        assertEquals(listOf(20L), groups.getValue("Used in vocabulary").items.map { it.id })
        assertEquals("r1 · M1", groups.getValue("Radicals").items.single().subtitle)
    }

    @Test
    fun linksAboveTheGrantedLevelAreLeftOut() = run {
        val subjects = fake()
        val content = SubjectContent.parse(subjects.rows.getValue(10).json)
        val similar = subjects.relatedGroups("kanji", content, maxLevel = 37).first { it.label == "Visually similar kanji" }
        assertEquals(listOf(11L), similar.items.map { it.id })
    }

    @Test
    fun aRadicalListsItsKanjiAndVocabularyListsItsKanji() = run {
        val subjects = fake()
        val radical = subjects.relatedGroups("radical", SubjectContent.parse(subjects.rows.getValue(1).json), 60)
        assertEquals(listOf("Used in kanji"), radical.map { it.label })
        val vocab = subjects.relatedGroups("vocabulary", SubjectContent.parse(subjects.rows.getValue(20).json), 60)
        assertEquals(listOf("Kanji"), vocab.map { it.label })
        assertTrue(subjects.relatedGroups("kana_vocabulary", SubjectContent.parse(subjects.rows.getValue(20).json), 60).isEmpty())
    }

    @Test
    fun longListsAreCappedAndSayHowManyAreLeftOut() = run {
        val subjects = FakeSubjects()
        val ids = (100L..160L).toList()
        (listOf(subject(1, "radical", "一", extra = ""","amalgamation_subject_ids":${ids}""")) + ids.map { subject(it, "kanji", "字$it") }).forEach { subjects.rows[it.id] = it }
        val group = subjects.relatedGroups("radical", SubjectContent.parse(subjects.rows.getValue(1).json), 60).single()
        assertEquals(30, group.items.size)
        assertEquals(31, group.more)
    }

    @Test
    fun strokeTableReadsOneCharacterPerLine() {
        val table = parseStrokeTable("一\tM10,50 L100,50\n二\tM20,30 L90,30|M10,80 L100,80\n\nbad line\n")
        assertEquals(setOf("一", "二"), table.keys)
    }

    @Test
    fun strokesComeBackInOrderOnlyForSingleCharacters() = run {
        val repo = StrokeRepository { "二\tM1,1 L2,2|M3,3 L4,4\n".byteInputStream() }
        assertEquals(listOf("M1,1 L2,2", "M3,3 L4,4"), repo.strokesFor("二"))
        assertNull(repo.strokesFor("三"))
        assertNull(repo.strokesFor("二つ"))
        assertNull(repo.strokesFor(""))
    }
}
