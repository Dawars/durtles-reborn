package com.example.durtlesreborn.session

import com.example.durtlesreborn.answer.Meaning
import com.example.durtlesreborn.answer.SubjectAnswers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LessonSessionTest {
    private val srs = SrsSystem(1, 5, 9)
    private val reported = mutableListOf<Long>()
    private val listener = object : SessionListener {
        override fun onReport(item: SessionItem) { reported += item.id }
        override fun onUpdate(item: SessionItem) {}
    }

    private fun subject(id: Long, kind: SubjectKind = SubjectKind.RADICAL, level: Int = 1) =
        SessionSubject(id, id * 10, kind, level, 0, srs, characters = "字$id")

    private fun lessons(subjects: List<SessionSubject>, order: SubjectOrder = SubjectOrder.LEVEL_THEN_TYPE) =
        SessionPlanner.plan(subjects, SessionConfig(SessionType.LESSON, order), listener, Random(1))

    @Test
    fun lessonSessionsStartInPresentationAndOthersDoNot() {
        assertEquals(SessionState.LESSON_PRESENTATION, lessons(listOf(subject(1))).state)
        val review = SessionPlanner.plan(listOf(subject(1)), SessionConfig(SessionType.REVIEW, SubjectOrder.SHUFFLE), listener, Random(1))
        assertEquals(SessionState.ACTIVE, review.state)
    }

    @Test
    fun cardsAreShownInOrderAndNavigationStaysInRange() {
        val e = lessons(listOf(subject(1, level = 1), subject(2, level = 2), subject(3, level = 3)))
        assertEquals(1L, e.presentedItem!!.id)
        assertTrue(e.isOnFirstLessonItem)
        e.moveToPreviousLessonItem()
        assertEquals(0, e.presentationIndex)
        e.moveToNextLessonItem()
        e.moveToNextLessonItem()
        assertEquals(3L, e.presentedItem!!.id)
        assertTrue(e.isOnLastLessonItem)
        e.moveToNextLessonItem()
        assertEquals(2, e.presentationIndex)
    }

    @Test
    fun noQuestionIsChosenWhileCardsAreShown() {
        val e = lessons(listOf(subject(1)))
        e.chooseQuestion()
        assertNull(e.currentQuestion)
    }

    @Test
    fun startQuizBeginsAskingAndFinishedItemsAreReportedOnce() {
        val e = lessons(listOf(subject(1), subject(2)))
        e.startQuiz()
        assertEquals(SessionState.ACTIVE, e.state)
        assertNull(e.presentedItem)
        while (true) {
            e.chooseQuestion()
            if (e.currentQuestion == null) break
            e.submitAnkiCorrect()
            e.advance()
        }
        assertEquals(listOf(1L, 2L).toSet(), reported.toSet())
        assertEquals(2, reported.size)
    }

    @Test
    fun backToPresentationReturnsToTheLastCard() {
        val e = lessons(listOf(subject(1), subject(2)))
        e.startQuiz()
        e.chooseQuestion()
        e.goBackToPresentation()
        assertEquals(SessionState.LESSON_PRESENTATION, e.state)
        assertEquals(1, e.presentationIndex)
        assertNull(e.currentQuestion)
    }

    @Test
    fun backToPresentationIsOnlyForLessons() {
        val review = SessionPlanner.plan(listOf(subject(1)), SessionConfig(SessionType.REVIEW, SubjectOrder.SHUFFLE), listener, Random(1))
        review.goBackToPresentation()
        assertEquals(SessionState.ACTIVE, review.state)
    }

    @Test
    fun controllerExposesPresentationAndStartsTheQuiz() {
        val e = lessons(listOf(subject(1), subject(2)))
        val answers = mapOf(1L to SubjectAnswers(meanings = listOf(Meaning("one"))), 2L to SubjectAnswers(meanings = listOf(Meaning("two"))))
        val quiz = QuizController(e, answers)
        assertTrue(quiz.inPresentation)
        assertEquals(1L, quiz.presentedItemId)
        assertEquals(0 to 2, quiz.presentationPosition)
        quiz.nextLessonItem()
        assertTrue(quiz.isOnLastLessonItem)
        assertFalse(quiz.isOnFirstLessonItem)
        quiz.startQuiz()
        assertFalse(quiz.inPresentation)
        assertEquals("字1", quiz.card()?.characters ?: quiz.card()?.characters)
    }

    @Test
    fun lessonOrderNamesParse() {
        assertEquals(listOf(SortKey.RADICALS_FIRST, SortKey.LEVEL, SortKey.TYPE), SubjectOrder.fromName("RADICALS_THEN_LEVEL_THEN_TYPE")!!.keys)
        val levelShuffle = SubjectOrder.fromName("LEVEL_THEN_SHUFFLE")!!
        assertEquals(listOf(SortKey.LEVEL), levelShuffle.keys)
        assertTrue(levelShuffle.shuffle)
        assertTrue(SubjectOrder.fromName("TYPE_THEN_SHUFFLE")!!.shuffle)
        assertFalse(SubjectOrder.fromName("LEVEL_THEN_TYPE")!!.shuffle)
    }

    @Test
    fun radicalsFirstOrderPutsRadicalsBeforeLowerLevelKanji() {
        val e = lessons(
            listOf(subject(1, SubjectKind.KANJI, level = 1), subject(2, SubjectKind.RADICAL, level = 5)),
            SubjectOrder.fromName("RADICALS_THEN_LEVEL_THEN_TYPE")!!,
        )
        assertEquals(2L, e.items.first { it.bucket == 0 }.id)
    }
}
