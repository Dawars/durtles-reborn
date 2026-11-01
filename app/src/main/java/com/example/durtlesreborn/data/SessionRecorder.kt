package com.example.durtlesreborn.data

import com.example.durtlesreborn.session.SessionItem
import com.example.durtlesreborn.session.SessionListener
import com.example.durtlesreborn.session.SessionType
import com.example.durtlesreborn.session.SrsSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Persists what a session produces. A finished item is first applied to the local assignment and
 * statistic, then added to the [PendingReview] queue; the sync engine sends it later. Items are
 * handled one at a time in the order they were reported, so the queue keeps that order.
 *
 * Self-study results stay on the phone and are never queued. In a [dryRun] nothing is written at all.
 */
class SessionRecorder(
    scope: CoroutineScope,
    private val type: SessionType,
    private val pending: PendingReviewDao,
    private val assignments: AssignmentDao,
    private val statistics: ReviewStatisticDao,
    private val srsFor: (AssignmentEntity) -> SrsSystem,
    private val clock: () -> Long = System::currentTimeMillis,
    private val dryRun: Boolean = false,
) : SessionListener {
    /** A copy of the item at the time it was reported; the session may undo or change it later. */
    private class Report(
        val subjectId: Long,
        val assignmentId: Long,
        val incorrectMeaning: Int,
        val incorrectReading: Int,
        val finishedAt: Long,
    )

    private val reports = Channel<Report>(Channel.UNLIMITED)
    private val worker = scope.launch { for (report in reports) record(report) }

    override fun onReport(item: SessionItem) {
        if (type == SessionType.SELF_STUDY || dryRun) return
        reports.trySend(
            Report(item.id, item.assignmentId, item.incorrectMeaning, item.incorrectReading, item.lastAnswer.takeIf { it > 0 } ?: clock()),
        )
    }

    override fun onUpdate(item: SessionItem) = Unit

    /** Waits until every reported item has been written. Call when the session ends. */
    suspend fun finish() {
        reports.close()
        worker.join()
    }

    private suspend fun record(report: Report) {
        val assignment = assignments.bySubject(report.subjectId)
        when (type) {
            SessionType.LESSON -> {
                if (assignment != null) {
                    assignments.upsert(listOf(LocalProgress.afterLessonStarted(assignment, report.finishedAt, srsFor(assignment))))
                }
                pending.insert(
                    PendingReview(
                        kind = PendingKind.START_LESSON.name,
                        subjectId = report.subjectId,
                        assignmentId = report.assignmentId,
                        createdAt = report.finishedAt,
                    ),
                )
            }
            SessionType.REVIEW -> {
                if (assignment != null) {
                    val outcome = LocalProgress.afterReview(
                        assignment, statistics.bySubject(report.subjectId),
                        report.incorrectMeaning, report.incorrectReading, report.finishedAt, srsFor(assignment),
                    )
                    assignments.upsert(listOf(outcome.assignment))
                    outcome.statistic?.let { statistics.upsert(listOf(it)) }
                }
                pending.insert(
                    PendingReview(
                        kind = PendingKind.REVIEW.name,
                        subjectId = report.subjectId,
                        assignmentId = report.assignmentId,
                        incorrectMeaningAnswers = report.incorrectMeaning,
                        incorrectReadingAnswers = report.incorrectReading,
                        createdAt = report.finishedAt,
                    ),
                )
            }
            SessionType.SELF_STUDY -> Unit
        }
    }
}
