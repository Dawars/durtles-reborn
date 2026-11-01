package com.example.durtlesreborn.data

import com.example.durtlesreborn.session.SubjectKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTest {
    private val kanji = """
        {"level":2,"slug":"water","characters":"水","spaced_repetition_system_id":1,
         "meanings":[{"meaning":"Water","primary":true,"accepted_answer":true},{"meaning":"Fluid","primary":false,"accepted_answer":false}],
         "auxiliary_meanings":[{"type":"whitelist","meaning":"Liquid"},{"type":"blacklist","meaning":"Ice"}],
         "readings":[{"type":"onyomi","reading":"すい","primary":true,"accepted_answer":true},
                     {"type":"kunyomi","reading":"みず","primary":false,"accepted_answer":false}]}
    """.trimIndent()

    @Test
    fun searchTextHasCharactersSlugAcceptedAndWhitelistedMeaningsAndReadings() {
        val text = buildSearchText("水", "water", SubjectContent.parse(kanji))
        val parts = text.split("\n")
        assertTrue("水" in parts)
        assertTrue("water" in parts)
        assertTrue("liquid" in parts)
        assertTrue("すい" in parts)
        assertTrue("みず" in parts)
        // Not accepted, and blacklisted, meanings are not searchable.
        assertTrue("fluid" !in parts)
        assertTrue("ice" !in parts)
        assertEquals(text, text.lowercase())
    }

    @Test
    fun queryVariantsAddHiraganaForRomajiAndKatakana() {
        assertEquals("mizu" to "みず", queryVariants("  Mizu "))
        assertEquals("san" to "さん", queryVariants("san"))
        assertEquals("スイ" to "すい", queryVariants("スイ"))
        assertEquals("水" to "", queryVariants("水"))
        assertEquals("" to "", queryVariants("   "))
    }

    @Test
    fun englishWordsGetNoMeaninglessKanaVariant() {
        assertEquals("water" to "", queryVariants("Water"))
        assertEquals("to resist" to "", queryVariants("to resist"))
    }

    @Test
    fun wildcardCharactersAreRemoved() {
        assertEquals("ab", queryVariants("a%_b").first)
    }

    @Test
    fun stageGroupsFollowWaniKaniPositions() {
        assertEquals(StageGroup.NOT_STARTED, StageGroup.of(null))
        assertEquals(StageGroup.NOT_STARTED, StageGroup.of(0))
        assertEquals(StageGroup.APPRENTICE, StageGroup.of(1))
        assertEquals(StageGroup.APPRENTICE, StageGroup.of(4))
        assertEquals(StageGroup.GURU, StageGroup.of(5))
        assertEquals(StageGroup.GURU, StageGroup.of(6))
        assertEquals(StageGroup.MASTER, StageGroup.of(7))
        assertEquals(StageGroup.ENLIGHTENED, StageGroup.of(8))
        assertEquals(StageGroup.BURNED, StageGroup.of(9))
    }

    private class RecordingSearchDao(val rows: List<SearchRow>) : SearchDao {
        var args: List<Any?> = emptyList()
        override suspend fun search(
            q1: String, q2: String, types: List<String>, minLevel: Int, maxLevel: Int,
            maxGrantedLevel: Int, stageMin: Int, stageMax: Int, limit: Int,
        ): List<SearchRow> {
            args = listOf(q1, q2, types, minLevel, maxLevel, maxGrantedLevel, stageMin, stageMax, limit)
            return rows
        }
    }

    private fun repo(dao: SearchDao, subjects: FakeSubjects = FakeSubjects(), state: FakeState = FakeState()) =
        SearchRepository(dao, subjects, FakeAssignments(), FakeStatistics(), state)

    @Test
    fun repositoryPassesCriteriaAndBuildsResults() = runBlocking {
        val dao = RecordingSearchDao(listOf(SearchRow(1, "kanji", 2, "水", "water", 6, kanji)))
        val state = FakeState().also { it.put(SyncStateEntity(SyncEngine.KEY_MAX_LEVEL, "10")) }
        val results = repo(dao, state = state).find(
            SearchCriteria(text = "Mizu", kinds = setOf(SubjectKind.KANJI), minLevel = 2, maxLevel = 5, stage = StageGroup.GURU),
        )
        assertEquals(listOf("mizu", "みず", listOf("kanji"), 2, 5, 10, 5, 6, 200), dao.args)
        val r = results.single()
        assertEquals("水", r.title)
        assertEquals("Water", r.meaning)
        assertEquals("すい", r.reading)
        assertEquals(StageGroup.GURU, r.stage)
        assertEquals(SubjectKind.KANJI, r.kind)
    }

    @Test
    fun noKindsMeansAllAndNoStageMeansAny() = runBlocking {
        val dao = RecordingSearchDao(emptyList())
        repo(dao).find(SearchCriteria())
        @Suppress("UNCHECKED_CAST")
        assertEquals(setOf("radical", "kanji", "vocabulary", "kana_vocabulary"), (dao.args[2] as List<String>).toSet())
        assertEquals(-1, dao.args[6])
        // Without a saved level limit the free plan's level 3 applies.
        assertEquals(3, dao.args[5])
    }

    @Test
    fun resultWithoutAssignmentIsNotStartedAndImageRadicalUsesItsSlug() = runBlocking {
        val radical = """{"level":1,"meanings":[{"meaning":"Beggar","primary":true,"accepted_answer":true}],"readings":[]}"""
        val dao = RecordingSearchDao(listOf(SearchRow(8766, "radical", 1, null, "beggar", null, radical)))
        val r = repo(dao).find(SearchCriteria()).single()
        assertEquals("beggar", r.title)
        assertEquals(StageGroup.NOT_STARTED, r.stage)
        assertEquals(null, r.reading)
    }

    @Test
    fun detailCombinesCardStageAndStatistics() = runBlocking {
        val subjects = FakeSubjects().also { it.rows[1] = SubjectEntity(1, "kanji", 2, "water", "水", false, 0, kanji) }
        val assignments = FakeAssignments().also {
            it.rows[10] = AssignmentEntity(10, 1, "kanji", 7, 1, 2, null, null, null, null, false, 0)
        }
        val statistics = FakeStatistics().also {
            it.rows[20] = ReviewStatisticEntity(20, 1, "kanji", 5, 1, 5, 5, 4, 2, 4, 4, 77, false, 0)
        }
        val detail = SearchRepository(RecordingSearchDao(emptyList()), subjects, assignments, statistics, FakeState()).detail(1)!!
        assertEquals(StageGroup.MASTER, detail.stage)
        assertEquals(77, detail.percentageCorrect)
        assertEquals("水", detail.info.title)
        assertEquals(null, SearchRepository(RecordingSearchDao(emptyList()), subjects, assignments, statistics, FakeState()).detail(99))
    }
}
