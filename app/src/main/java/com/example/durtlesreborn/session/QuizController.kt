package com.example.durtlesreborn.session

import com.example.durtlesreborn.answer.AnswerVerdict
import com.example.durtlesreborn.answer.CloseEnoughAction
import com.example.durtlesreborn.answer.QuestionChecks
import com.example.durtlesreborn.answer.SubjectAnswers

/** What the quiz screen needs to show for the current question. */
data class QuizCard(
    val itemId: Long,
    val characters: String,
    val kind: QuestionKind,
    /** Reading questions take kana input; the screen converts romaji while typing. */
    val kanaInput: Boolean,
    val answers: SubjectAnswers,
)

/** Result of submitting an answer. [retry] means nothing was recorded and the user may try again. */
data class QuizFeedback(
    val verdict: AnswerVerdict,
    val retry: Boolean,
    val correct: Boolean,
)

/**
 * The SRS stage index an item moved between when its review was completed: the stage it was on
 * before this review and the stage it will be on after. Indices match [SrsSystem] (1 = Apprentice 1
 * … 9 = Burned), and the bubble turns them into names.
 */
data class StageMove(val from: Int, val to: Int)

/** [totalItems] leaves out items dropped by wrapping up; [wrappingUp] is true once only started items remain. */
data class QuizProgress(
    val remainingQuestions: Int,
    val answeredItems: Int,
    val totalItems: Int,
    val wrappingUp: Boolean = false,
)

/**
 * Connects a [SessionEngine] to the answer checks: picks the question, checks typed answers
 * against the subject's accepted answers, and exposes undo, skip and "don't know".
 */
class QuizController(
    private val engine: SessionEngine,
    private val answers: Map<Long, SubjectAnswers>,
    private val closeEnough: CloseEnoughAction = CloseEnoughAction.ACCEPT_WITH_TOAST,
    private val requireOnInKatakana: Boolean = false,
    private val displayText: Map<Long, String> = emptyMap(),
) {
    val finished get() = engine.state != SessionState.ACTIVE && engine.currentQuestion == null
    val answered get() = engine.answered
    val canUndo get() = engine.canUndo

    /** Lessons show each item first; the quiz starts after the last card. */
    val inPresentation get() = engine.state == SessionState.LESSON_PRESENTATION
    val presentedItemId: Long? get() = engine.presentedItem?.id
    val presentationPosition get() = engine.presentationIndex to engine.items.size
    val isOnFirstLessonItem get() = engine.isOnFirstLessonItem
    val isOnLastLessonItem get() = engine.isOnLastLessonItem
    fun nextLessonItem() = engine.moveToNextLessonItem()
    fun previousLessonItem() = engine.moveToPreviousLessonItem()
    fun startQuiz() = engine.startQuiz()
    fun backToLessonCards() = engine.goBackToPresentation()

    val progress
        get() = QuizProgress(
            remainingQuestions = engine.remainingQuestions,
            answeredItems = engine.items.count { it.reported || it.pending },
            totalItems = engine.items.count { !it.abandoned },
            wrappingUp = engine.state == SessionState.ACTIVE && engine.numStartedItems >= engine.numActiveItems,
        )

    /** The current question, choosing one if none is active. Null when the session is done. */
    fun card(): QuizCard? {
        engine.chooseQuestion()
        val question = engine.currentQuestion ?: return null
        return QuizCard(
            itemId = question.item.id,
            characters = displayText[question.item.id] ?: question.item.characters ?: "?",
            kind = question.kind,
            kanaInput = question.kind.kana,
            answers = answers[question.item.id] ?: SubjectAnswers(),
        )
    }

    fun submit(text: String): QuizFeedback {
        val verdict = engine.submit(text) { question, answer -> check(question, answer) }
        return QuizFeedback(verdict, retry = !verdict.ok && verdict.retry, correct = verdict.ok)
    }

    fun dontKnow(): QuizFeedback {
        engine.submitDontKnow()
        return QuizFeedback(AnswerVerdict.NOK_WITHOUT_RETRY, retry = false, correct = false)
    }

    /** The stage move for the item this answer just completed, or null if it is not done yet. */
    fun finishedStageMove(): StageMove? = engine.finishedItemStageMove()

    /** Number of items in the session and how many were answered without a single mistake. */
    fun summary(): Pair<Int, Int> {
        val played = engine.items.filter { !it.abandoned }
        return played.size to played.count { !it.active && it.totalIncorrect == 0 }
    }

    /** Moves on after an answer was shown. */
    fun next() = engine.advance()

    fun skip() = engine.skip()

    fun undo() = engine.undoAndRetry()

    /** Marks a wrongly judged answer as correct. */
    fun ignoreMistake() = engine.ignore()

    fun wrapUp() = engine.wrapUp()

    fun snapshot(savedAt: Long) = engine.snapshot(savedAt)

    /** Subject ids with their SRS system, for patching assignments when a resumed session finishes. */
    val itemSystems: Map<Long, SrsSystem> get() = engine.items.associate { it.id to it.srs }

    /** Reports items held back by a delayed session. Call when the summary is shown. */
    fun finish() = engine.reportPending()

    private fun check(question: Question, answer: String): AnswerVerdict {
        val subject = answers[question.item.id] ?: return AnswerVerdict.NOK_WITHOUT_RETRY
        return when (question.kind) {
            QuestionKind.RADICAL_NAME,
            QuestionKind.KANJI_MEANING,
            QuestionKind.VOCAB_MEANING,
            QuestionKind.KANA_VOCAB_MEANING,
            -> QuestionChecks.meaning(subject, answer, closeEnough)
            QuestionKind.KANJI_READING -> QuestionChecks.kanjiReading(subject, answer, requireOnInKatakana)
            QuestionKind.VOCAB_READING -> QuestionChecks.vocabularyReading(subject, null, answer, requireOnInKatakana)
            QuestionKind.KANJI_ONYOMI -> QuestionChecks.kanjiOnYomi(subject, answer, requireOnInKatakana)
            QuestionKind.KANJI_KUNYOMI -> QuestionChecks.kanjiKunYomi(subject, answer, requireOnInKatakana)
        }
    }
}
