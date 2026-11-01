package com.example.durtlesreborn.data

import com.example.durtlesreborn.session.SubjectKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRepositoryTest {
    private val now = 1_700_000_000_000
    private val hour = 3_600_000L

    private val subjects = FakeSubjects()
    private val assignments = FakeAssignments()
    private val materials = FakeMaterials()
    private val srsSystems = FakeSrsSystems()
    private val state = FakeState()

    private val kanjiJson = """
        {"level":2,"slug":"water","characters":"水","spaced_repetition_system_id":1,
         "meanings":[{"meaning":"Water","primary":true,"accepted_answer":true}],
         "auxiliary_meanings":[{"type":"whitelist","meaning":"liquid"},{"type":"blacklist","meaning":"ice"}],
         "readings":[{"type":"onyomi","reading":"すい","primary":true,"accepted_answer":true},
                     {"type":"kunyomi","reading":"みず","primary":false,"accepted_answer":false},
                     {"type":"nanori","reading":"み","primary":false,"accepted_answer":false}]}
    """.trimIndent()

    private fun repo(overdueThreshold: Float = 0.2f) =
        SessionRepository(subjects, assignments, materials, srsSystems, state, { now }, overdueThreshold)

    private fun subject(id: Long, type: String = "kanji", level: Int = 2, hidden: Boolean = false, json: String = kanjiJson) =
        SubjectEntity(id, type, level, "slug", "水", hidden, 0, json)

    private fun assignment(id: Long, subjectId: Long, stage: Int = 3, availableAt: Long? = now - hour, started: Boolean = true) =
        AssignmentEntity(
            id, subjectId, "kanji", stage, unlockedAt = 1, startedAt = if (started) 2 else null,
            passedAt = null, burnedAt = null, availableAt = availableAt, resurrectedAt = null, hidden = false, dataUpdatedAt = 0,
        )

    init {
        runBlocking {
            srsSystems.upsert(
                listOf(
                    SrsSystemEntity(1, 1, 5, 9, "[0,14400000,28800000,86400000,172800000,604800000,0,0,0,0]", 0),
                ),
            )
            state.put(SyncStateEntity(SyncEngine.KEY_MAX_LEVEL, "10"))
            state.put(SyncStateEntity(SyncEngine.KEY_LEVEL, "2"))
        }
    }

    @Test
    fun contentParsesMeaningsReadingsAndAuxiliaries() {
        val c = SubjectContent.parse(kanjiJson)
        assertEquals(1L, c.srsSystemId)
        assertTrue(c.hasAcceptedMeanings)
        assertTrue(c.hasOnYomi)
        // The kun'yomi reading is not the accepted one, but it exists, as in the original.
        assertTrue(c.hasKunYomi)
        assertTrue(c.hasAcceptedReadings)
        assertEquals(3, c.readings.size)
        val answers = c.answers(listOf("aqua"))
        assertEquals(listOf("aqua"), answers.synonyms)
        assertEquals(listOf(true, false), answers.auxiliaryMeanings.map { it.whitelist })
    }

    @Test
    fun dueReviewsComeFromStartedAssignmentsAvailableNow() = runBlocking {
        subjects.upsert(listOf(subject(1), subject(2)))
        assignments.upsert(listOf(assignment(10, 1), assignment(11, 2, availableAt = now + hour)))
        val due = repo().dueReviews()
        assertEquals(listOf(1L), due.map { it.id })
        val s = due.single()
        assertEquals(SubjectKind.KANJI, s.kind)
        assertEquals(10L, s.assignmentId)
        assertEquals(5, s.srs.passedIndex)
        assertTrue(s.hasOnYomi)
    }

    @Test
    fun imageRadicalShowsItsSlugWhenItHasNoCharacters() = runBlocking {
        subjects.upsert(listOf(subject(1, type = "radical").copy(characters = null, slug = "beggar")))
        assignments.upsert(listOf(assignment(10, 1)))
        assertEquals("beggar", repo().dueReviews().single().characters)
    }

    @Test
    fun subjectsAboveTheGrantedLevelAndHiddenOnesAreLeftOut() = runBlocking {
        state.put(SyncStateEntity(SyncEngine.KEY_MAX_LEVEL, "3"))
        subjects.upsert(listOf(subject(1, level = 3), subject(2, level = 4), subject(3, hidden = true)))
        assignments.upsert(listOf(assignment(10, 1), assignment(11, 2), assignment(12, 3)))
        assertEquals(listOf(1L), repo().dueReviews().map { it.id })
    }

    @Test
    fun unsubscribedDefaultIsLevelThree() = runBlocking {
        val fresh = FakeState()
        val r = SessionRepository(subjects, assignments, materials, srsSystems, fresh, { now })
        assertEquals(3, r.maxLevel())
    }

    @Test
    fun lessonsAreUnlockedButNotStarted() = runBlocking {
        subjects.upsert(listOf(subject(1), subject(2)))
        assignments.upsert(listOf(assignment(10, 1, stage = 0, availableAt = null, started = false), assignment(11, 2)))
        assertEquals(listOf(1L), repo().availableLessons().map { it.id })
    }

    @Test
    fun countsReportNextUpcomingReview() = runBlocking {
        subjects.upsert(listOf(subject(1), subject(2), subject(3)))
        assignments.upsert(
            listOf(
                assignment(10, 1),
                assignment(11, 2, availableAt = now + 3 * hour),
                assignment(12, 3, availableAt = now + hour),
            ),
        )
        val counts = repo().counts()
        assertEquals(1, counts.reviewsDue)
        assertEquals(now + hour, counts.nextReviewAt)
    }

    @Test
    fun overdueNeedsTheThresholdShareOfTheIntervalToHavePassed() = runBlocking {
        subjects.upsert(listOf(subject(1), subject(2)))
        // Stage 3 interval is 1 day; 20% is 4.8 hours past due.
        assignments.upsert(
            listOf(
                assignment(10, 1, stage = 3, availableAt = now - 5 * hour),
                assignment(11, 2, stage = 3, availableAt = now - 4 * hour),
            ),
        )
        val due = repo().dueReviews().associate { it.id to it.overdue }
        assertTrue(due[1]!!)
        assertFalse(due[2]!!)
    }

    @Test
    fun selfStudyIncludesUnstartedSubjectsAsStageZeroAndSkipsLockedLevelsAndHiddenOnes() = runBlocking {
        state.put(SyncStateEntity(SyncEngine.KEY_MAX_LEVEL, "3"))
        subjects.upsert(listOf(subject(1, level = 2), subject(2, level = 3), subject(3, level = 4), subject(4, hidden = true)))
        // Subject 1 is in progress at stage 3; subject 2 has no assignment at all.
        assignments.upsert(listOf(assignment(10, 1, stage = 3)))
        val result = repo().selfStudySubjects(listOf(1, 2, 3, 4, 99))
        assertEquals(setOf(1L, 2L), result.map { it.id }.toSet())
        val started = result.first { it.id == 1L }
        assertEquals(3, started.srsStageIndex)
        assertEquals(10L, started.assignmentId)
        val unstarted = result.first { it.id == 2L }
        assertEquals(0, unstarted.srsStageIndex)
        assertEquals(0L, unstarted.assignmentId)
        assertFalse(unstarted.overdue)
        assertEquals(emptyList<Long>(), repo().selfStudySubjects(emptyList()).map { it.id })
    }

    @Test
    fun answersIncludeTheUsersSynonyms() = runBlocking {
        subjects.upsert(listOf(subject(1)))
        materials.upsert(listOf(StudyMaterialEntity(1, 1, null, null, """["aqua"]""", 0)))
        assertEquals(listOf("aqua"), repo().answersFor(1)!!.synonyms)
        assertNull(repo().answersFor(99))
    }
}
