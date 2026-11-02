package com.example.durtlesreborn.session

import com.example.durtlesreborn.answer.AnswerVerdict
import com.example.durtlesreborn.data.FileSessionSnapshotStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SessionResumeTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val srs = SrsSystem(1, 5, 9, listOf(0, 14_400_000, 28_800_000))
    private val ok = AnswerVerdict(ok = true)
    private val wrong = AnswerVerdict.NOK_WITHOUT_RETRY

    private class Recorder : SessionListener {
        val reported = mutableListOf<Long>()
        override fun onReport(item: SessionItem) { reported += item.id }
        override fun onUpdate(item: SessionItem) = Unit
    }

    private fun newEngine(listener: SessionListener, type: SessionType = SessionType.REVIEW, lesson: Boolean = false): SessionEngine {
        val items = (1L..4L).map { SessionItem(it, bucket = 0, srs = srs, srsStageIndex = 2, characters = "字$it", assignmentId = it * 10) }
        for (i in items) for (slot in 2..4) i.setDone(slot, true)
        val questions = items.map { Question(it, QuestionKind.KANJI_MEANING) }
        return SessionEngine(
            type, items, questions, delayed = true, random = { 0 }, clock = { 42L },
            listener = listener, startInPresentation = lesson,
        )
    }

    private fun roundTrip(engine: SessionEngine, listener: SessionListener): SessionEngine {
        val store = FileSessionSnapshotStore(File(folder.root, "session.json"))
        store.save(engine.snapshot(1_000L))
        return SessionEngine.restore(store.load()!!, random = { 0 }, clock = { 43L }, listener = listener)!!
    }

    private fun answer(engine: SessionEngine, verdict: AnswerVerdict) {
        engine.chooseQuestion()
        engine.submit("x") { _, _ -> verdict }
        engine.advance()
    }

    @Test
    fun leavingMidSessionKeepsAnswersAndNothingIsReportedUntilTheEnd() {
        val first = Recorder()
        val engine = newEngine(first)
        answer(engine, ok)
        answer(engine, wrong)
        assertTrue("delayed sessions report nothing while running", first.reported.isEmpty())

        val second = Recorder()
        val resumed = roundTrip(engine, second)
        assertEquals(engine.remainingQuestions, resumed.remainingQuestions)
        assertEquals(1, resumed.items.count { it.pending })
        assertEquals(1, resumed.items.count { it.incorrectMeaning == 1 })
        assertEquals(2, resumed.items.count { it.numAnswers > 0 })

        // Finish what is left; only now does anything go out, once per item.
        repeat(10) { if (resumed.state == SessionState.ACTIVE) answer(resumed, ok) }
        assertEquals(SessionState.FINISHING, resumed.state)
        assertTrue(second.reported.isEmpty())
        resumed.reportPending()
        assertEquals(listOf(1L, 2L, 3L, 4L), second.reported.sorted())
    }

    @Test
    fun leavingReportsFinishedItemsOnceAndKeepsTheRestForLater() {
        val first = Recorder()
        val engine = newEngine(first)
        answer(engine, ok)
        answer(engine, wrong)
        engine.reportPending()
        assertEquals(listOf(1L), first.reported)

        val second = Recorder()
        val resumed = roundTrip(engine, second)
        assertEquals(3, resumed.remainingQuestions)
        resumed.reportPending()
        assertTrue(second.reported.isEmpty())
    }

    @Test
    fun anItemSentOnLeavingCannotBeUndoneAfterResuming() {
        val engine = newEngine(Recorder())
        answer(engine, ok)
        engine.reportPending()
        val resumed = roundTrip(engine, Recorder())
        assertFalse(resumed.canUndo)
    }

    @Test
    fun aWrongAnswerIsKeptSoTheItemIsAskedAgainAfterResuming() {
        val engine = newEngine(Recorder())
        answer(engine, wrong)
        val resumed = roundTrip(engine, Recorder())
        assertEquals(4, resumed.remainingQuestions)
        assertEquals(1, resumed.items.sumOf { it.incorrectMeaning })
    }

    @Test
    fun undoStillWorksAfterResuming() {
        val engine = newEngine(Recorder())
        answer(engine, ok)
        val resumed = roundTrip(engine, Recorder())
        assertTrue(resumed.canUndo)
        resumed.undoAndRetry()
        assertEquals(0, resumed.items.count { it.pending })
        assertEquals(4, resumed.remainingQuestions)
    }

    @Test
    fun anAnswerShownButNotMovedPastIsStillCounted() {
        val engine = newEngine(Recorder())
        engine.chooseQuestion()
        engine.submit("x") { _, _ -> ok }
        val resumed = roundTrip(engine, Recorder())
        assertEquals(1, resumed.items.count { it.pending })
        assertEquals(3, resumed.remainingQuestions)
        assertTrue(resumed.canUndo)
    }

    @Test
    fun wrappedUpSessionsComeBackWrappedUp() {
        val engine = newEngine(Recorder())
        answer(engine, wrong)
        engine.chooseQuestion()
        engine.wrapUp()
        val resumed = roundTrip(engine, Recorder())
        assertEquals(3, resumed.items.count { it.abandoned })
        assertEquals(1, resumed.numActiveItems)
        assertFalse(resumed.canBeWrappedUp)
    }

    @Test
    fun lessonsResumeOnTheCardTheyWereLeftOn() {
        val engine = newEngine(Recorder(), SessionType.LESSON, lesson = true)
        engine.moveToNextLessonItem()
        engine.moveToNextLessonItem()
        val resumed = roundTrip(engine, Recorder())
        assertEquals(SessionState.LESSON_PRESENTATION, resumed.state)
        assertEquals(2, resumed.presentationIndex)
        assertEquals(3L, resumed.presentedItem?.id)
    }

    @Test
    fun theSnapshotSummarisesProgress() {
        val engine = newEngine(Recorder())
        answer(engine, ok)
        answer(engine, ok)
        engine.chooseQuestion()
        engine.wrapUp()
        val snapshot = engine.snapshot(5L)
        assertEquals(2, snapshot.doneItems)
        assertEquals(2, snapshot.totalItems)
        assertEquals(SessionType.REVIEW, snapshot.sessionType)
    }

    @Test
    fun aBrokenSnapshotIsRefusedRatherThanRestoredHalfWay() {
        val snapshot = newEngine(Recorder()).snapshot(1L)
        assertNull(SessionEngine.restore(snapshot.copy(questions = listOf(QuestionRef(999, "KANJI_MEANING"))), { 0 }, listener = Recorder()))
        assertNull(SessionEngine.restore(snapshot.copy(state = "NOPE"), { 0 }, listener = Recorder()))
        assertNull(SessionEngine.restore(snapshot.copy(type = "NOPE"), { 0 }, listener = Recorder()))
        assertNotNull(SessionEngine.restore(snapshot, { 0 }, listener = Recorder()))
    }

    @Test
    fun theStoreReadsWhatItWroteAndIgnoresDamage() {
        val file = File(folder.root, "session.json")
        val store = FileSessionSnapshotStore(file)
        assertNull(store.load())
        val snapshot = newEngine(Recorder()).snapshot(9L)
        store.save(snapshot)
        assertEquals(snapshot, store.load())
        store.save(snapshot.copy(savedAt = 10L))
        assertEquals(10L, store.load()?.savedAt)
        assertFalse(File(folder.root, "session.json.tmp").exists())

        file.writeText("{ not json")
        assertNull(store.load())
        file.writeText(com.example.durtlesreborn.data.FileSessionSnapshotStore::class.java.name)
        assertNull(store.load())

        store.save(snapshot.copy(version = 99))
        assertNull(store.load())

        store.save(snapshot)
        store.clear()
        assertNull(store.load())
    }
}
