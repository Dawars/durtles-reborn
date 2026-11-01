package com.example.durtlesreborn.data

import com.example.durtlesreborn.session.SessionItem
import com.example.durtlesreborn.session.SessionType
import com.example.durtlesreborn.session.SrsSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val HOUR = 3_600_000L
private const val DAY = 24 * HOUR

class LocalProgressTest {
    // Stage 0 initiate, 1..4 apprentice (4h, 8h, 1d, 2d), 5..6 guru, 7 master, 8 enlightened, 9 burned.
    private val srs = SrsSystem(
        firstStartedIndex = 1, passedIndex = 5, completedIndex = 9,
        intervalsMillis = listOf(0, 4 * HOUR, 8 * HOUR, DAY, 2 * DAY, 7 * DAY, 14 * DAY, 30 * DAY, 120 * DAY, 0),
    )

    private val ts = 1_700_000_000_000 + 25 * 60_000 // 22:38:20 on the 14th, so not on the hour

    private fun assignment(stage: Int, passedAt: Long? = null) = AssignmentEntity(
        id = 1, subjectId = 5, subjectType = "kanji", srsStage = stage,
        unlockedAt = 1, startedAt = 2, passedAt = passedAt, burnedAt = null, availableAt = 3,
        resurrectedAt = null, hidden = false, dataUpdatedAt = 0,
    )

    private fun statistic() = ReviewStatisticEntity(
        id = 9, subjectId = 5, subjectType = "kanji",
        meaningCorrect = 3, meaningIncorrect = 1, meaningMaxStreak = 3, meaningCurrentStreak = 3,
        readingCorrect = 2, readingIncorrect = 2, readingMaxStreak = 2, readingCurrentStreak = 2,
        percentageCorrect = 62, hidden = false, dataUpdatedAt = 0,
    )

    @Test
    fun lessonStartSetsFirstStartedStageAndNextReviewOnTheHour() {
        val a = LocalProgress.afterLessonStarted(assignment(0).copy(startedAt = null, availableAt = null), ts, srs)
        assertEquals(1, a.srsStage)
        assertEquals(ts, a.startedAt)
        val expected = (ts + 4 * HOUR + 30_000).let { it - it % HOUR }
        assertEquals(expected, a.availableAt)
        assertEquals(0L, a.availableAt!! % HOUR)
    }

    @Test
    fun perfectReviewAdvancesAndSchedulesTheNextOne() {
        val outcome = LocalProgress.afterReview(assignment(2), statistic(), 0, 0, ts, srs)
        assertEquals(3, outcome.assignment.srsStage)
        assertEquals((ts + DAY + 30_000).let { it - it % HOUR }, outcome.assignment.availableAt)
        assertNull(outcome.assignment.passedAt)
    }

    @Test
    fun reachingGuruSetsPassedAtOnlyOnce() {
        val first = LocalProgress.afterReview(assignment(4), null, 0, 0, ts, srs).assignment
        assertEquals(5, first.srsStage)
        assertEquals(ts, first.passedAt)
        val second = LocalProgress.afterReview(first, null, 0, 0, ts + DAY, srs).assignment
        assertEquals(ts, second.passedAt)
    }

    @Test
    fun burningClearsAvailabilityAndSetsBurnedAt() {
        val a = LocalProgress.afterReview(assignment(8, passedAt = 1), null, 0, 0, ts, srs).assignment
        assertEquals(9, a.srsStage)
        assertNull(a.availableAt)
        assertEquals(ts, a.burnedAt)
    }

    @Test
    fun mistakesDropTheStage() {
        val a = LocalProgress.afterReview(assignment(4), null, 1, 1, ts, srs).assignment
        assertEquals(3, a.srsStage)
    }

    @Test
    fun statisticTracksStreaksAndPercentage() {
        val good = LocalProgress.afterReview(assignment(2), statistic(), 0, 0, ts, srs).statistic!!
        assertEquals(4, good.meaningCorrect)
        assertEquals(4, good.meaningCurrentStreak)
        assertEquals(4, good.meaningMaxStreak)
        assertEquals(3, good.readingCorrect)
        // 7 correct of 10 answers.
        assertEquals(70, good.percentageCorrect)

        val bad = LocalProgress.afterReview(assignment(2), statistic(), 2, 0, ts, srs).statistic!!
        assertEquals(2, bad.meaningIncorrect)
        assertEquals(0, bad.meaningCurrentStreak)
        assertEquals(3, bad.meaningMaxStreak)
        assertEquals(3, bad.readingCurrentStreak)
    }

    @Test
    fun missingStatisticStaysMissing() {
        assertNull(LocalProgress.afterReview(assignment(2), null, 0, 0, ts, srs).statistic)
    }

    @Test
    fun recorderPatchesLocallyThenQueuesReviewsInOrder() = runBlocking {
        val pending = FakePending()
        val assignments = FakeAssignments().also { it.rows[1] = assignment(2) }
        val statistics = FakeStatistics().also { it.rows[9] = statistic() }
        val recorder = SessionRecorder(
            CoroutineScope(Dispatchers.Unconfined), SessionType.REVIEW, pending, assignments, statistics, { srs }, clock = { ts },
        )
        val a = SessionItem(5, 0, srs, 2, assignmentId = 1).also { it.lastAnswer = ts }
        val b = SessionItem(6, 0, srs, 2, assignmentId = 2).also { it.setIncorrectCount(1, 1); it.setIncorrectCount(2, 2); it.lastAnswer = ts + 1 }
        recorder.onReport(a)
        recorder.onReport(b)
        recorder.finish()

        assertEquals(3, assignments.rows[1]!!.srsStage)
        assertEquals(4, statistics.rows[9]!!.meaningCorrect)
        val queued = pending.all()
        assertEquals(listOf(5L, 6L), queued.map { it.subjectId })
        assertEquals(PendingKind.REVIEW.name, queued[0].kind)
        assertEquals(1, queued[1].incorrectMeaningAnswers)
        assertEquals(2, queued[1].incorrectReadingAnswers)
    }

    @Test
    fun recorderQueuesLessonStartsAndPatchesTheAssignment() = runBlocking {
        val pending = FakePending()
        val assignments = FakeAssignments().also { it.rows[1] = assignment(0).copy(startedAt = null) }
        val recorder = SessionRecorder(
            CoroutineScope(Dispatchers.Unconfined), SessionType.LESSON, pending, assignments, FakeStatistics(), { srs }, clock = { ts },
        )
        recorder.onReport(SessionItem(5, 0, srs, 0, assignmentId = 1).also { it.lastAnswer = ts })
        recorder.finish()
        assertEquals(1, assignments.rows[1]!!.srsStage)
        assertNotNull(assignments.rows[1]!!.startedAt)
        assertEquals(PendingKind.START_LESSON.name, pending.all().single().kind)
    }

    @Test
    fun selfStudyIsNeverQueuedOrPatched() = runBlocking {
        val pending = FakePending()
        val assignments = FakeAssignments().also { it.rows[1] = assignment(2) }
        val recorder = SessionRecorder(
            CoroutineScope(Dispatchers.Unconfined), SessionType.SELF_STUDY, pending, assignments, FakeStatistics(), { srs }, clock = { ts },
        )
        recorder.onReport(SessionItem(5, 0, srs, 2, assignmentId = 1))
        recorder.finish()
        assertTrue(pending.all().isEmpty())
        assertEquals(2, assignments.rows[1]!!.srsStage)
    }
}
