package com.example.durtlesreborn.data

import com.example.durtlesreborn.session.SrsSystem

/**
 * Applies the effect of a finished lesson or review to the local database straight away, as the
 * original's ReportSessionItemJob does, so the home screen and the next session are right
 * before the server has been told. The server's answer replaces these values on the next sync.
 */
object LocalProgress {
    private const val HOUR = 60L * 60 * 1000
    private const val SERVER_DELAY = 30L * 1000

    /** Reviews become available on the hour, a little after the interval has passed. */
    private fun availableAt(ts: Long, intervalMillis: Long): Long {
        val t = ts + intervalMillis + SERVER_DELAY
        return t - t % HOUR
    }

    fun afterLessonStarted(assignment: AssignmentEntity, ts: Long, srs: SrsSystem): AssignmentEntity {
        val stage = srs.firstStartedIndex
        return assignment.copy(
            srsStage = stage,
            startedAt = ts,
            availableAt = availableAt(ts, srs.intervalMillis(stage)),
        )
    }

    class ReviewOutcome(val assignment: AssignmentEntity, val statistic: ReviewStatisticEntity?)

    /**
     * New SRS stage, availability, passed and burned times, and the review statistic. The
     * statistic is left alone if there is none yet, because its server id is not known.
     */
    fun afterReview(
        assignment: AssignmentEntity,
        statistic: ReviewStatisticEntity?,
        meaningIncorrect: Int,
        readingIncorrect: Int,
        ts: Long,
        srs: SrsSystem,
    ): ReviewOutcome {
        val newStage = srs.newStageIndex(assignment.srsStage, meaningIncorrect + readingIncorrect)
        val completed = newStage >= srs.completedIndex
        val updatedAssignment = assignment.copy(
            srsStage = newStage,
            availableAt = if (completed) null else availableAt(ts, srs.intervalMillis(newStage)),
            burnedAt = assignment.burnedAt ?: if (completed) ts else null,
            passedAt = assignment.passedAt ?: if (newStage >= srs.passedIndex) ts else null,
        )

        val updatedStatistic = statistic?.let {
            val meaningCorrect = it.meaningCorrect + if (meaningIncorrect == 0) 1 else 0
            val meaningWrong = it.meaningIncorrect + if (meaningIncorrect == 0) 0 else 1
            val meaningStreak = if (meaningIncorrect == 0) it.meaningCurrentStreak + 1 else 0
            val readingCorrect = it.readingCorrect + if (readingIncorrect == 0) 1 else 0
            val readingWrong = it.readingIncorrect + if (readingIncorrect == 0) 0 else 1
            val readingStreak = if (readingIncorrect == 0) it.readingCurrentStreak + 1 else 0
            val total = meaningCorrect + meaningWrong + readingCorrect + readingWrong
            it.copy(
                meaningCorrect = meaningCorrect,
                meaningIncorrect = meaningWrong,
                meaningCurrentStreak = meaningStreak,
                meaningMaxStreak = maxOf(it.meaningMaxStreak, meaningStreak),
                readingCorrect = readingCorrect,
                readingIncorrect = readingWrong,
                readingCurrentStreak = readingStreak,
                readingMaxStreak = maxOf(it.readingMaxStreak, readingStreak),
                percentageCorrect = (meaningCorrect + readingCorrect) * 100 / total,
            )
        }
        return ReviewOutcome(updatedAssignment, updatedStatistic)
    }
}
