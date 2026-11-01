package com.example.durtlesreborn.session

import com.example.durtlesreborn.answer.AuxiliaryMeaning
import com.example.durtlesreborn.answer.Meaning
import com.example.durtlesreborn.answer.Reading
import com.example.durtlesreborn.answer.SubjectAnswers
import com.example.durtlesreborn.kana.Kana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuizControllerTest {
    private val srs = SrsSystem(1, 5, 9)
    private val reported = mutableListOf<Long>()
    private val listener = object : SessionListener {
        override fun onReport(item: SessionItem) { reported += item.id }
        override fun onUpdate(item: SessionItem) {}
    }

    private val water = SubjectAnswers(
        meanings = listOf(Meaning("Water")),
        auxiliaryMeanings = listOf(AuxiliaryMeaning("ice", whitelist = false)),
        readings = listOf(Reading("すい", "onyomi", acceptedAnswer = true), Reading("みず", "kunyomi")),
    )

    private fun controller(delayed: Boolean = false): QuizController {
        val subject = SessionSubject(1, 10, SubjectKind.KANJI, 2, 2, srs, characters = "水", hasOnYomi = true)
        val engine = SessionPlanner.plan(
            listOf(subject), SessionConfig(SessionType.REVIEW, SubjectOrder.SHUFFLE, delayed = delayed), listener, Random(1),
        )
        return QuizController(engine, mapOf(1L to water))
    }

    private fun QuizController.answer(text: String) = card()!!.let { submit(text) }

    @Test
    fun walksThroughMeaningAndReadingThenFinishes() {
        val quiz = controller()
        var guard = 0
        while (!quiz.finished && guard++ < 10) {
            val card = quiz.card() ?: break
            val feedback = if (card.kanaInput) quiz.submit("すい") else quiz.submit("water")
            assertTrue(feedback.correct)
            quiz.next()
        }
        assertTrue(quiz.finished)
        assertEquals(listOf(1L), reported)
        assertEquals(1, quiz.progress.answeredItems)
    }

    @Test
    fun cardShowsCharactersAndKindAndKanaInputForReadings() {
        val quiz = controller()
        val first = quiz.card()!!
        assertEquals("水", first.characters)
        assertEquals(first.kind.reading, first.kanaInput)
    }

    @Test
    fun wrongAnswerIsRecordedAndRetryIsNot() {
        val quiz = controller()
        val card = quiz.card()!!
        val wrong = if (card.kanaInput) "あああ" else "banana"
        assertFalse(quiz.submit(wrong).correct)
        assertTrue(quiz.answered)
        quiz.next()
    }

    @Test
    fun aReadingTypedForAMeaningQuestionIsOnlyARetry() {
        val quiz = controller()
        var card = quiz.card()!!
        // Find the meaning question.
        while (card.kanaInput) {
            quiz.skip()
            card = quiz.card()!!
        }
        val feedback = quiz.submit("mizu")
        assertTrue(feedback.retry)
        assertFalse(quiz.answered)
    }

    @Test
    fun dontKnowCountsAsWrongAndUndoRestores() {
        val quiz = controller()
        quiz.card()
        val f = quiz.dontKnow()
        assertFalse(f.correct)
        assertTrue(quiz.canUndo)
        quiz.undo()
        assertFalse(quiz.answered)
        assertNotNull(quiz.card())
    }

    @Test
    fun delayedSessionReportsOnlyWhenFinishIsCalled() {
        val quiz = controller(delayed = true)
        while (true) {
            val card = quiz.card() ?: break
            quiz.submit(if (card.kanaInput) "すい" else "water")
            quiz.next()
        }
        assertTrue(reported.isEmpty())
        quiz.finish()
        assertEquals(listOf(1L), reported)
    }

    @Test
    fun progressCountsFinishedItemsOutOfThoseStillInPlay() {
        val subjects = (1L..4L).map { SessionSubject(it, it * 10, SubjectKind.RADICAL, 1, 0, srs, characters = "字$it") }
        val engine = SessionPlanner.plan(subjects, SessionConfig(SessionType.SELF_STUDY, SubjectOrder.SHUFFLE), listener, Random(1))
        val answers = subjects.associate { it.id to SubjectAnswers(meanings = listOf(Meaning("word${it.id}"))) }
        val quiz = QuizController(engine, answers)
        assertEquals(QuizProgress(4, 0, 4, wrappingUp = false), quiz.progress)

        // Answer one wrongly, so it is started but unfinished, then wrap up.
        val first = quiz.card()!!
        quiz.dontKnow()
        quiz.next()
        quiz.wrapUp()
        val p = quiz.progress
        assertEquals(1, p.totalItems)
        assertEquals(0, p.answeredItems)
        assertTrue(p.wrappingUp)

        // Finishing the one remaining item completes the session.
        val card = quiz.card()!!
        assertEquals(first.itemId, card.itemId)
        quiz.submit(answers.getValue(card.itemId).meanings.first().text)
        quiz.next()
        assertEquals(1, quiz.progress.answeredItems)
        assertNull(quiz.card())
        assertEquals(1 to 0, quiz.summary())
    }

    @Test
    fun nothingToAskReturnsNull() {
        val quiz = controller()
        while (quiz.card() != null) {
            quiz.submitAnyCorrect()
        }
        assertNull(quiz.card())
    }

    private fun QuizController.submitAnyCorrect() {
        val card = card()!!
        submit(if (card.kanaInput) "すい" else "water")
        next()
    }
}

class KanaTypingTest {
    @Test
    fun convertsAsTheUserTypes() {
        var text = ""
        for (ch in "kanji") text = Kana.typed(text, text + ch)
        // The trailing "n" stays pending until a consonant follows.
        assertEquals("かんじ", text)
    }

    @Test
    fun pendingNStaysUntilNextKey() {
        var text = ""
        for (ch in "kan") text = Kana.typed(text, text + ch)
        assertEquals("かn", text)
        text = Kana.typed(text, text + "j")
        assertEquals("かんj", text)
    }

    @Test
    fun deletionIsKeptAsTyped() {
        assertEquals("か", Kana.typed("かん", "か"))
    }
}
