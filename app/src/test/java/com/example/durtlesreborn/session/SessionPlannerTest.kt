package com.example.durtlesreborn.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SessionPlannerTest {
    private val srs = SrsSystem(firstStartedIndex = 1, passedIndex = 5, completedIndex = 9)
    private val listener = object : SessionListener {
        override fun onReport(item: SessionItem) {}
        override fun onUpdate(item: SessionItem) {}
    }

    private fun subject(
        id: Long,
        kind: SubjectKind = SubjectKind.KANJI,
        level: Int = 1,
        stage: Int = 2,
        overdue: Boolean = false,
        hasOnYomi: Boolean = true,
        hasKunYomi: Boolean = true,
    ) = SessionSubject(
        id = id, assignmentId = id * 10, kind = kind, level = level, srsStageIndex = stage, srs = srs,
        overdue = overdue, hasOnYomi = hasOnYomi, hasKunYomi = hasKunYomi,
    )

    private fun plan(subjects: List<SessionSubject>, config: SessionConfig) =
        SessionPlanner.plan(subjects, config, listener, Random(1))

    @Test
    fun equalRankedSubjectsShareABucketAndHigherRanksComeLater() {
        val e = plan(
            listOf(subject(1, level = 3), subject(2, level = 1), subject(3, level = 1), subject(4, level = 2)),
            SessionConfig(SessionType.REVIEW, SubjectOrder.LEVEL),
        )
        val buckets = e.items.associate { it.id to it.bucket }
        assertEquals(0, buckets[2])
        assertEquals(0, buckets[3])
        assertEquals(1, buckets[4])
        assertEquals(2, buckets[1])
    }

    @Test
    fun shuffleOrderPutsEverythingInOneBucket() {
        val e = plan(List(6) { subject(it + 1L, level = it) }, SessionConfig(SessionType.REVIEW, SubjectOrder.SHUFFLE))
        assertEquals(setOf(0), e.items.map { it.bucket }.toSet())
    }

    @Test
    fun reversedOrderPutsHighLevelsFirst() {
        val e = plan(
            listOf(subject(1, level = 1), subject(2, level = 5)),
            SessionConfig(SessionType.REVIEW, SubjectOrder.LEVEL, orderReversed = true),
        )
        assertEquals(0, e.items.first { it.id == 2L }.bucket)
    }

    @Test
    fun dueOrderPutsTheEarliestDueSubjectFirst() {
        val due = SubjectOrder.fromName("DUE")!!
        assertEquals(listOf(SortKey.DUE), due.keys)
        val e = plan(
            listOf(subject(1).copy(dueAt = 300), subject(2).copy(dueAt = 100), subject(3).copy(dueAt = 200)),
            SessionConfig(SessionType.REVIEW, due),
        )
        val buckets = e.items.associate { it.id to it.bucket }
        assertEquals(0, buckets[2])
        assertEquals(1, buckets[3])
        assertEquals(2, buckets[1])
    }

    @Test
    fun typeThenLevelUsesTheTypeOrderRadicalKanjiVocabulary() {
        val e = plan(
            listOf(subject(1, SubjectKind.VOCABULARY), subject(2, SubjectKind.RADICAL), subject(3, SubjectKind.KANJI)),
            SessionConfig(SessionType.REVIEW, SubjectOrder.TYPE_THEN_LEVEL),
        )
        assertEquals(listOf(2L, 3L, 1L), e.items.sortedBy { it.bucket }.map { it.id })
    }

    @Test
    fun overdueFirstAppliesToReviewsButNotLessons() {
        val subjects = listOf(subject(1, level = 1), subject(2, level = 9, overdue = true))
        val reviews = plan(subjects, SessionConfig(SessionType.REVIEW, SubjectOrder.LEVEL, overdueFirst = true))
        assertEquals(0, reviews.items.first { it.id == 2L }.bucket)
        val lessons = plan(subjects, SessionConfig(SessionType.LESSON, SubjectOrder.LEVEL, overdueFirst = true))
        assertEquals(0, lessons.items.first { it.id == 1L }.bucket)
    }

    @Test
    fun radicalsFirstPriorityBeatsTheOrder() {
        val e = plan(
            listOf(subject(1, SubjectKind.KANJI, level = 1), subject(2, SubjectKind.RADICAL, level = 9)),
            SessionConfig(SessionType.REVIEW, SubjectOrder.LEVEL, priority = SessionPriority.RadicalsFirst),
        )
        assertEquals(0, e.items.first { it.id == 2L }.bucket)
    }

    @Test
    fun levelUpFirstPriorityNeedsTheUserBelowTheirMaximumLevel() {
        val subjects = listOf(subject(1, level = 1), subject(2, level = 9))
        val ctx = PriorityContext(userLevel = 9, maxLevel = 60, levelUpIds = setOf(2L))
        val e = plan(subjects, SessionConfig(SessionType.REVIEW, SubjectOrder.LEVEL, priority = SessionPriority.LevelUpFirst, priorityContext = ctx))
        assertEquals(0, e.items.first { it.id == 2L }.bucket)

        val capped = plan(subjects, SessionConfig(SessionType.REVIEW, SubjectOrder.LEVEL, priority = SessionPriority.LevelUpFirst, priorityContext = ctx.copy(maxLevel = 9)))
        assertEquals(0, capped.items.first { it.id == 1L }.bucket)
    }

    @Test
    fun currentLevelFirstPriority() {
        val e = plan(
            listOf(subject(1, level = 1), subject(2, level = 7)),
            SessionConfig(SessionType.REVIEW, SubjectOrder.LEVEL, priority = SessionPriority.CurrentLevelFirst, priorityContext = PriorityContext(userLevel = 7)),
        )
        assertEquals(0, e.items.first { it.id == 2L }.bucket)
    }

    @Test
    fun maxSizeKeepsTheBestRankedSubjects() {
        val e = plan(
            List(10) { subject(it + 1L, level = 10 - it) },
            SessionConfig(SessionType.REVIEW, SubjectOrder.LEVEL, maxSize = 3),
        )
        assertEquals(setOf(10L, 9L, 8L), e.items.map { it.id }.toSet())
    }

    @Test
    fun shuffleAfterSelectionMergesTheBuckets() {
        val e = plan(
            listOf(subject(1, level = 1), subject(2, level = 2), subject(3, level = 3)),
            SessionConfig(SessionType.LESSON, SubjectOrder.LEVEL, shuffleAfterSelection = true),
        )
        assertEquals(setOf(0), e.items.map { it.bucket }.toSet())
    }

    @Test
    fun questionSetsFollowTheSubjectKindAndOnkunMode() {
        fun kinds(subject: SessionSubject, onkun: Boolean): Set<QuestionKind> {
            val e = plan(listOf(subject), SessionConfig(SessionType.REVIEW, SubjectOrder.SHUFFLE, onkun = onkun))
            val seen = mutableSetOf<QuestionKind>()
            // Drain by answering everything correctly.
            while (true) {
                e.chooseQuestion()
                val q = e.currentQuestion ?: break
                seen += q.kind
                e.submitAnkiCorrect()
                e.advance()
            }
            return seen
        }
        assertEquals(setOf(QuestionKind.RADICAL_NAME), kinds(subject(1, SubjectKind.RADICAL), false))
        assertEquals(setOf(QuestionKind.KANJI_MEANING, QuestionKind.KANJI_READING), kinds(subject(2), false))
        assertEquals(
            setOf(QuestionKind.KANJI_MEANING, QuestionKind.KANJI_ONYOMI, QuestionKind.KANJI_KUNYOMI),
            kinds(subject(3), true),
        )
        assertEquals(setOf(QuestionKind.VOCAB_MEANING, QuestionKind.VOCAB_READING), kinds(subject(4, SubjectKind.VOCABULARY), false))
        assertEquals(setOf(QuestionKind.KANA_VOCAB_MEANING), kinds(subject(5, SubjectKind.KANA_VOCABULARY), false))
    }

    @Test
    fun kanjiWithoutAKunYomiNeverAsksForOne() {
        val e = plan(
            listOf(subject(1, hasKunYomi = false)),
            SessionConfig(SessionType.REVIEW, SubjectOrder.SHUFFLE, onkun = true),
        )
        assertEquals(2, e.remainingQuestions)
        assertFalse(e.items.single().isDone(3))
        assertTrue(e.items.single().isDone(4))
    }

    @Test
    fun reportingPendingItemsDoesItOnceForDelayedSessions() {
        val reported = mutableListOf<Long>()
        val l = object : SessionListener {
            override fun onReport(item: SessionItem) { reported += item.id }
            override fun onUpdate(item: SessionItem) {}
        }
        val e = SessionPlanner.plan(
            listOf(subject(1, SubjectKind.RADICAL)),
            SessionConfig(SessionType.REVIEW, SubjectOrder.SHUFFLE, delayed = true),
            l, Random(1),
        )
        e.chooseQuestion()
        e.submitAnkiCorrect()
        assertTrue(reported.isEmpty())
        e.reportPending()
        e.reportPending()
        assertEquals(listOf(1L), reported)
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptySubjectListIsRefused() {
        plan(emptyList(), SessionConfig(SessionType.REVIEW, SubjectOrder.SHUFFLE))
    }
}
