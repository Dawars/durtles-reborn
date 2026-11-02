package com.example.durtlesreborn.session

import com.example.durtlesreborn.answer.AnswerVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SrsSystemTest {
    // Stages 0 (initiate), 1..4 apprentice, 5..6 guru (passed from 5), 7 master, 8 enlightened, 9 burned.
    private val srs = SrsSystem(firstStartedIndex = 1, passedIndex = 5, completedIndex = 9)

    @Test
    fun perfectAnswerAdvancesOneStage() {
        assertEquals(2, srs.newStageIndex(1, 0))
        assertEquals(9, srs.newStageIndex(9, 0))
    }

    @Test
    fun mistakesDropStagesAndPassedItemsDropTwiceAsFar() {
        assertEquals(3, srs.newStageIndex(4, 1))
        assertEquals(1, srs.newStageIndex(4, 5))
        assertEquals(5, srs.newStageIndex(7, 1))
        assertEquals(1, srs.newStageIndex(2, 3))
    }
}

class SessionEngineTest {
    private val srs = SrsSystem(1, 5, 9)

    private class Recorder : SessionListener {
        val reported = mutableListOf<Long>()
        val updated = mutableListOf<Long>()
        val abandoned = mutableListOf<Long>()
        override fun onReport(item: SessionItem) { reported += item.id }
        override fun onUpdate(item: SessionItem) { updated += item.id }
        override fun onAbandon(item: SessionItem) { abandoned += item.id }
    }

    private val ok = AnswerVerdict(ok = true)
    private val wrong = AnswerVerdict.NOK_WITHOUT_RETRY

    private fun item(id: Long, bucket: Int = 0, characters: String? = null) =
        SessionItem(id, bucket, srs, srsStageIndex = 2, characters = characters)

    private fun engine(
        items: List<SessionItem>,
        kinds: List<QuestionKind> = listOf(QuestionKind.KANJI_MEANING),
        listener: SessionListener = Recorder(),
        delayed: Boolean = false,
        type: SessionType = SessionType.REVIEW,
        backToBack: Boolean = false,
        readingFirst: Boolean = false,
        meaningFirst: Boolean = false,
        maxStartedItems: Int = SessionEngine.DEFAULT_MAX_STARTED_ITEMS,
    ): SessionEngine {
        val questions = items.flatMap { i -> kinds.map { Question(i, it) } }
        // Slots without a question are already done, as in the original.
        for (i in items) for (slot in 1..4) if (kinds.none { it.slot == slot }) i.setDone(slot, true)
        return SessionEngine(
            type, items, questions, delayed = delayed, backToBack = backToBack,
            readingFirst = readingFirst, meaningFirst = meaningFirst, maxStartedItems = maxStartedItems,
            random = { 0 }, clock = { 42L }, listener = listener,
        )
    }

    @Test
    fun correctAnswerReportsFinishedItemAndEndsSession() {
        val rec = Recorder()
        val a = item(1)
        val e = engine(listOf(a), listener = rec)
        e.chooseQuestion()
        assertTrue(e.submit("x") { _, _ -> ok }.ok)
        assertEquals(listOf(1L), rec.reported)
        assertEquals(42L, a.lastAnswer)
        e.advance()
        assertEquals(SessionState.FINISHING, e.state)
        assertEquals(1L, e.lastFinishedItemId)
    }

    @Test
    fun aFinishedItemReportsItsStageMove() {
        val e = engine(listOf(item(1)))
        e.chooseQuestion()
        e.submit("x") { _, _ -> ok }
        assertEquals(StageMove(2, 3), e.finishedItemStageMove())
    }

    @Test
    fun aWrongAnswerOnlyMovesTheStageOnceTheItemIsFinallyCorrect() {
        val a = item(1).apply { srsStageIndex = 5 } // Guru 1
        val e = engine(listOf(a))
        e.chooseQuestion()
        e.submit("x") { _, _ -> wrong }
        // A wrong question stays open and is asked again, so the item is not finished yet.
        assertNull(e.finishedItemStageMove())
        e.advance()
        e.chooseQuestion()
        e.submit("y") { _, _ -> ok }
        assertEquals(StageMove(5, 3), e.finishedItemStageMove())
    }

    @Test
    fun aTwoQuestionItemMovesOnlyWhenItsLastQuestionIsAnswered() {
        val e = engine(
            listOf(item(1)),
            kinds = listOf(QuestionKind.KANJI_MEANING, QuestionKind.KANJI_READING),
        )
        e.chooseQuestion()
        e.submit("x") { _, _ -> ok }
        assertNull(e.finishedItemStageMove())
        e.advance()
        e.chooseQuestion()
        e.submit("あ") { _, _ -> ok }
        assertEquals(StageMove(2, 3), e.finishedItemStageMove())
    }

    @Test
    fun aBurnedItemHasNoMoveLeft() {
        val a = item(1).apply { srsStageIndex = 9 }
        val e = engine(listOf(a))
        e.chooseQuestion()
        e.submit("x") { _, _ -> ok }
        assertNull(e.finishedItemStageMove())
    }

    @Test
    fun delayedSessionMakesItemsPendingInsteadOfReporting() {
        val rec = Recorder()
        val a = item(1)
        val e = engine(listOf(a), listener = rec, delayed = true)
        e.chooseQuestion()
        e.submit("x") { _, _ -> ok }
        assertTrue(a.pending)
        assertTrue(rec.reported.isEmpty())
        assertTrue(e.canUndo)
    }

    @Test
    fun selfStudyIsAlwaysDelayed() {
        assertTrue(engine(listOf(item(1)), type = SessionType.SELF_STUDY).isDelayed)
    }

    @Test
    fun wrongAnswerCountsDelaysTheItemAndKeepsQuestion() {
        val a = item(1)
        val b = item(2)
        val e = engine(listOf(a, b))
        e.chooseQuestion()
        e.submit("x") { _, _ -> wrong }
        assertEquals(1, a.incorrectCount(1))
        assertFalse(e.correct)
        e.advance()
        assertEquals(3, a.choiceDelay)
        assertEquals(2, e.remainingQuestions)
    }

    @Test
    fun delayedItemIsAvoidedWhileUndelayedOnesExistInTheBucket() {
        val a = item(1)
        val b = item(2)
        val e = engine(listOf(a, b))
        e.chooseQuestion()
        e.submit("x") { _, _ -> wrong }
        e.advance()
        e.chooseQuestion()
        assertEquals(2L, e.currentItem!!.id)
        assertEquals(2, a.choiceDelay)
    }

    @Test
    fun retryVerdictDoesNotRecordAnything() {
        val a = item(1)
        val e = engine(listOf(a))
        e.chooseQuestion()
        val v = e.submit("x") { _, _ -> AnswerVerdict.NOK_WITH_RETRY }
        assertTrue(v.retry)
        assertFalse(e.answered)
        assertEquals(0, a.numAnswers)
    }

    @Test
    fun emptyAnswerIsRetry() {
        val e = engine(listOf(item(1)))
        e.chooseQuestion()
        assertTrue(e.submit("") { _, _ -> ok }.retry)
    }

    @Test
    fun questionsAreChosenFromTheLowestBucketFirst() {
        val e = engine(listOf(item(1, bucket = 0), item(2, bucket = 1)))
        e.chooseQuestion()
        assertEquals(1L, e.currentItem!!.id)
    }

    @Test
    fun readingFirstPrefersReadingWhileBothArePending() {
        val a = item(1)
        val e = engine(
            listOf(a),
            kinds = listOf(QuestionKind.KANJI_MEANING, QuestionKind.KANJI_READING),
            readingFirst = true,
        )
        e.chooseQuestion()
        assertEquals(QuestionKind.KANJI_READING, e.currentQuestion!!.kind)
    }

    @Test
    fun aNewItemWaitsWhileTheBatchIsFull() {
        val waiting = item(1)
        val first = item(2).also { it.numAnswers = 1 }
        val second = item(3).also { it.numAnswers = 1 }
        val full = engine(listOf(waiting, first, second), kinds = listOf(QuestionKind.KANJI_MEANING, QuestionKind.KANJI_READING), maxStartedItems = 2)
        full.chooseQuestion()
        assertEquals(2L, full.currentItem!!.id)
    }

    @Test
    fun aNewItemIsAskedWhenTheBatchHasRoom() {
        val waiting = item(1)
        val first = item(2).also { it.numAnswers = 1 }
        val room = engine(listOf(waiting, first), kinds = listOf(QuestionKind.KANJI_MEANING, QuestionKind.KANJI_READING), maxStartedItems = 2)
        room.chooseQuestion()
        assertEquals(1L, room.currentItem!!.id)
    }

    @Test
    fun backToBackPrefersStartedItems() {
        val a = item(1)
        val b = item(2)
        val e = engine(
            listOf(a, b),
            kinds = listOf(QuestionKind.KANJI_MEANING, QuestionKind.KANJI_READING),
            backToBack = true,
        )
        e.chooseQuestion()
        val first = e.currentItem!!
        e.submit("x") { _, _ -> ok }
        e.advance()
        e.chooseQuestion()
        assertEquals(first.id, e.currentItem!!.id)
    }

    @Test
    fun undoAndRetryRestoresStateAndAsksAgain() {
        val a = item(1)
        val e = engine(listOf(a))
        e.chooseQuestion()
        e.submit("x") { _, _ -> wrong }
        e.undoAndRetry()
        assertEquals(0, a.incorrectCount(1))
        assertEquals(0, a.numAnswers)
        assertFalse(e.answered)
        assertEquals(a, e.currentItem)
    }

    @Test
    fun undoAfterAdvanceReopensAFinishedSession() {
        val rec = Recorder()
        val a = item(1)
        val e = engine(listOf(a), listener = rec, delayed = true)
        e.chooseQuestion()
        e.submit("x") { _, _ -> ok }
        e.advance()
        assertEquals(SessionState.FINISHING, e.state)
        e.undoAndRetry()
        assertEquals(SessionState.ACTIVE, e.state)
        assertTrue(a.active)
        assertFalse(a.isDone(1))
    }

    @Test
    fun reportedItemCannotBeUndoneAfterAdvance() {
        val a = item(1)
        val b = item(2)
        val e = engine(listOf(a, b))
        e.chooseQuestion()
        e.submit("x") { _, _ -> ok }
        e.advance()
        assertFalse(e.canUndo)
    }

    @Test
    fun undoAndPutBackRequeuesWithDelayAndClearsCurrent() {
        val a = item(1)
        val b = item(2)
        val e = engine(listOf(a, b), delayed = true)
        e.chooseQuestion()
        e.submit("x") { _, _ -> ok }
        e.advance()
        e.undoAndPutBack()
        assertNull(e.currentQuestion)
        assertEquals(3, a.choiceDelay)
        assertEquals(2, e.remainingQuestions)
    }

    @Test
    fun skipDelaysTheItem() {
        val a = item(1)
        val b = item(2)
        val e = engine(listOf(a, b))
        e.chooseQuestion()
        e.skip()
        assertNull(e.currentQuestion)
        assertEquals(3, a.choiceDelay)
    }

    @Test
    fun ignoreTurnsAWrongAnswerIntoACorrectOne() {
        val a = item(1)
        val e = engine(listOf(a), delayed = true)
        e.chooseQuestion()
        e.submit("x") { _, _ -> wrong }
        e.ignore()
        assertTrue(e.correct)
        assertEquals(0, a.incorrectCount(1))
        assertTrue(a.pending)
    }

    @Test
    fun wrapUpAbandonsUnstartedItemsOnly() {
        val rec = Recorder()
        val a = item(1)
        val b = item(2)
        val e = engine(listOf(a, b), listener = rec)
        e.chooseQuestion()
        e.submit("x") { _, _ -> wrong }
        e.advance()
        val started = if (a.started) a else b
        val other = if (a.started) b else a
        e.wrapUp()
        assertTrue(other.abandoned)
        assertTrue(started.active)
        assertEquals(listOf(other.id), rec.abandoned)
        assertEquals(1, e.remainingQuestions)
    }

    @Test
    fun kanaQuestionRejectsNonKanaAndMissingKana() {
        val a = item(1, characters = "お金")
        val e = engine(listOf(a), kinds = listOf(QuestionKind.VOCAB_READING))
        e.chooseQuestion()
        assertTrue(e.submit("okane") { _, _ -> ok }.retry)
        assertTrue(e.submit("かね") { _, _ -> ok }.retry)
        assertTrue(e.submit("おかね") { _, _ -> ok }.ok)
    }

    @Test
    fun kanaGuardTreatsKatakanaAsHiraganaAndEndingNAsHatsuon() {
        val a = item(1, characters = "ほん")
        val e = engine(listOf(a), kinds = listOf(QuestionKind.VOCAB_READING))
        e.chooseQuestion()
        var seen = ""
        e.submit("ホn") { _, answer -> seen = answer; ok }
        assertEquals("ホん", seen)
    }
}
