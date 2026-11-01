package com.example.durtlesreborn.data

import com.example.durtlesreborn.api.WaniKaniClient
import com.example.durtlesreborn.session.QuizController
import com.example.durtlesreborn.session.SessionConfig
import com.example.durtlesreborn.session.SessionPlanner
import com.example.durtlesreborn.session.SessionType
import com.example.durtlesreborn.session.SubjectOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * The whole write path with fakes and a mock server in place of the database and WaniKani:
 * repository -> planner -> quiz -> recorder -> queue -> sync engine -> HTTP.
 */
class EndToEndTest {
    private val hour = 3_600_000L
    private val now = 1_800_000_000_000

    private val subjects = FakeSubjects()
    private val assignments = FakeAssignments()
    private val statistics = FakeStatistics()
    private val materials = FakeMaterials()
    private val srsSystems = FakeSrsSystems()
    private val state = FakeState()
    private val pending = FakePending()

    private lateinit var server: MockWebServer
    private val requests = mutableListOf<RecordedRequest>()
    private var reviewResponse = """{"id":1,"object":"review"}"""
    private var startResponse = ""

    private val kanji = """
        {"level":2,"slug":"water","characters":"水","spaced_repetition_system_id":1,
         "meanings":[{"meaning":"Water","primary":true,"accepted_answer":true}],
         "auxiliary_meanings":[],
         "readings":[{"type":"onyomi","reading":"すい","primary":true,"accepted_answer":true}]}
    """.trimIndent()

    private val radical = """
        {"level":1,"slug":"one","characters":"一","spaced_repetition_system_id":1,
         "meanings":[{"meaning":"One","primary":true,"accepted_answer":true}],"readings":[]}
    """.trimIndent()

    @Before
    fun setUp() {
        runBlocking {
            srsSystems.upsert(listOf(SrsSystemEntity(1, 1, 5, 9, "[0,14400000,28800000,86400000,172800000,604800000,0,0,0,0]", 0)))
            state.put(SyncStateEntity(SyncEngine.KEY_MAX_LEVEL, "60"))
            state.put(SyncStateEntity(SyncEngine.KEY_LEVEL, "2"))
            subjects.upsert(
                listOf(
                    SubjectEntity(1, "kanji", 2, "water", "水", false, 0, kanji),
                    SubjectEntity(2, "radical", 1, "one", "一", false, 0, radical),
                ),
            )
        }
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val path = request.requestUrl!!.encodedPath
                return when {
                    request.method == "POST" && path == "/v2/reviews" -> json(reviewResponse, 201)
                    request.method == "PUT" -> json(startResponse)
                    path == "/v2/user" -> json("""{"object":"user","data":{"username":"t","level":2,"subscription":{"active":true,"max_level_granted":60}}}""")
                    else -> json("""{"object":"collection","pages":{"next_url":null,"per_page":500},"total_count":0,"data":[]}""")
                }
            }
        }
        server.start()
    }

    @After
    fun tearDown() = server.shutdown()

    private fun json(body: String, code: Int = 200) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private fun repository() = SessionRepository(subjects, assignments, materials, srsSystems, state, { now })

    private fun syncEngine() = SyncEngine(
        WaniKaniClient(OkHttpClient(), { "t" }, server.url("/v2/"), maxRateLimitRetries = 0, sleep = {}, allowInsecureBaseUrl = true),
        pending, subjects, assignments, statistics, materials, srsSystems, state, { now },
    )

    private fun assignment(id: Long, subjectId: Long, type: String, stage: Int, started: Boolean = true) = AssignmentEntity(
        id, subjectId, type, stage, unlockedAt = 1, startedAt = if (started) 2 else null, passedAt = null, burnedAt = null,
        availableAt = if (started) now - hour else null, resurrectedAt = null, hidden = false, dataUpdatedAt = 0,
    )

    private fun recorder(type: SessionType, subjectsInSession: List<com.example.durtlesreborn.session.SessionSubject>, dryRun: Boolean = false) = SessionRecorder(
        CoroutineScope(Dispatchers.Unconfined), type, pending, assignments, statistics,
        srsFor = { a -> subjectsInSession.first { it.id == a.subjectId }.srs }, clock = { now }, dryRun = dryRun,
    )

    @Test
    fun aDryRunReviewChangesNothingAndQueuesNothing() = runBlocking {
        assignments.upsert(listOf(assignment(10, 1, "kanji", stage = 2)))
        statistics.upsert(listOf(ReviewStatisticEntity(20, 1, "kanji", 3, 1, 3, 3, 2, 2, 2, 2, 62, false, 0)))
        val repo = repository()
        val due = repo.dueReviews()
        val recorder = recorder(SessionType.REVIEW, due, dryRun = true)
        val engine = SessionPlanner.plan(due, SessionConfig(SessionType.REVIEW, SubjectOrder.SHUFFLE), recorder, Random(1), { now })
        val quiz = QuizController(engine, mapOf(1L to repo.answersFor(1)!!))
        var questions = 0
        while (questions++ < 20) {
            val card = quiz.card() ?: break
            quiz.submit(if (card.kanaInput) "すい" else "water")
            quiz.next()
        }
        assertTrue("the quiz should have finished on its own", questions < 20)
        quiz.finish()
        recorder.finish()

        assertTrue(pending.all().isEmpty())
        assertEquals(2, assignments.rows[10]!!.srsStage)
        assertEquals(3, statistics.rows[20]!!.meaningCorrect)
        // Still due, so a real session can follow.
        assertEquals(listOf(1L), repo.dueReviews().map { it.id })
    }

    @Test
    fun reviewWithOneMistakeIsPatchedLocallyQueuedAndPostedBySubjectId() = runBlocking {
        assignments.upsert(listOf(assignment(10, 1, "kanji", stage = 2)))
        statistics.upsert(listOf(ReviewStatisticEntity(20, 1, "kanji", 3, 1, 3, 3, 2, 2, 2, 2, 62, false, 0)))

        val repo = repository()
        val due = repo.dueReviews()
        assertEquals(listOf(1L), due.map { it.id })

        val recorder = recorder(SessionType.REVIEW, due)
        val engine = SessionPlanner.plan(due, SessionConfig(SessionType.REVIEW, SubjectOrder.SHUFFLE), recorder, Random(1), { now })
        val quiz = QuizController(engine, mapOf(1L to repo.answersFor(1)!!))

        // One wrong meaning answer, then everything right.
        var wrongGiven = false
        while (true) {
            val card = quiz.card() ?: break
            val answer = when {
                card.kanaInput -> "すい"
                !wrongGiven -> "banana".also { wrongGiven = true }
                else -> "water"
            }
            quiz.submit(answer)
            quiz.next()
            // A wrong meaning stays in the queue, so the same card comes back and is answered correctly.
        }
        quiz.finish()
        recorder.finish()

        // Queue: exactly one review with the mistake counted on the meaning side.
        val queued = pending.all().single()
        assertEquals(PendingKind.REVIEW.name, queued.kind)
        assertEquals(1L, queued.subjectId)
        assertEquals(10L, queued.assignmentId)
        assertEquals(1, queued.incorrectMeaningAnswers)
        assertEquals(0, queued.incorrectReadingAnswers)

        // Local patch: one mistake drops stage 2 to 1, next review on the hour after the interval; stats updated.
        val patched = assignments.rows[10]!!
        assertEquals(1, patched.srsStage)
        assertEquals(0L, patched.availableAt!! % hour)
        assertTrue(patched.availableAt!! > now)
        val stat = statistics.rows[20]!!
        assertEquals(3, stat.meaningCorrect)
        assertEquals(2, stat.meaningIncorrect)
        assertEquals(0, stat.meaningCurrentStreak)
        assertEquals(3, stat.readingCorrect)
        // Reviewed items are no longer due.
        assertTrue(repo.dueReviews().isEmpty())

        // Sync: the review is posted by subject id, with the mistake and no timestamp (it is recent).
        reviewResponse = """
            {"id":1,"object":"review","resources_updated":{"assignment":{"id":10,"object":"assignment","data_updated_at":"2026-12-01T00:00:00.000000Z",
              "data":{"subject_id":1,"subject_type":"kanji","srs_stage":1,"started_at":"2026-01-01T00:00:00.000000Z","available_at":"2026-12-01T12:00:00.000000Z"}}}}
        """.trimIndent()
        val report = syncEngine().sync()
        assertTrue(report.ok)
        assertEquals(1, report.reviewsSent)
        assertEquals(0, pending.count())
        val post = requests.first { it.method == "POST" }
        assertEquals("/v2/reviews", post.requestUrl!!.encodedPath)
        val body = post.body.readUtf8()
        assertTrue(body, body.contains("\"subject_id\":1"))
        assertTrue(body, body.contains("\"incorrect_meaning_answers\":1"))
        assertTrue(body, body.contains("\"incorrect_reading_answers\":0"))
        assertFalse(body, body.contains("created_at"))
        // The server's assignment replaced the local patch.
        assertEquals(1, assignments.rows[10]!!.srsStage)
        // The POST came before any collection was fetched.
        assertEquals("POST", requests.first().method)
    }

    @Test
    fun lessonFinishedInTheQuizIsQueuedAsAStartAndPut() = runBlocking {
        assignments.upsert(listOf(assignment(11, 2, "radical", stage = 0, started = false)))
        val repo = repository()
        val lessons = repo.availableLessons()
        assertEquals(listOf(2L), lessons.map { it.id })

        val recorder = recorder(SessionType.LESSON, lessons)
        val engine = SessionPlanner.plan(lessons, SessionConfig(SessionType.LESSON, SubjectOrder.LEVEL_THEN_TYPE), recorder, Random(1), { now })
        val quiz = QuizController(engine, mapOf(2L to repo.answersFor(2)!!))
        assertTrue(quiz.inPresentation)
        assertNull(quiz.card())
        quiz.startQuiz()
        while (true) {
            val card = quiz.card() ?: break
            quiz.submit("one")
            quiz.next()
        }
        quiz.finish()
        recorder.finish()

        val queued = pending.all().single()
        assertEquals(PendingKind.START_LESSON.name, queued.kind)
        assertEquals(2L, queued.subjectId)
        assertEquals(11L, queued.assignmentId)
        val started = assignments.rows[11]!!
        assertEquals(1, started.srsStage)
        assertNotNull(started.startedAt)
        assertTrue(repo.availableLessons().isEmpty())

        startResponse = """{"id":11,"object":"assignment","data_updated_at":"2026-12-01T00:00:00.000000Z","data":{"subject_id":2,"subject_type":"radical","srs_stage":1,"unlocked_at":"2026-01-01T00:00:00.000000Z","started_at":"2026-12-01T00:00:00.000000Z"}}"""
        val report = syncEngine().sync()
        assertTrue(report.ok)
        assertEquals(0, pending.count())
        val put = requests.first { it.method == "PUT" }
        assertEquals("/v2/assignments/11/start", put.requestUrl!!.encodedPath)
        assertEquals(1, assignments.rows[11]!!.srsStage)
    }

    @Test
    fun selfStudyWithMistakesNeverQueuesPatchesOrPosts() = runBlocking {
        // Subject 1 is in progress, subject 2 was never started: both can be practised.
        assignments.upsert(listOf(assignment(10, 1, "kanji", stage = 4)))
        val before = assignments.rows[10]!!
        val repo = repository()
        val subjectsForQuiz = repo.selfStudySubjects(listOf(1, 2))
        assertEquals(setOf(1L, 2L), subjectsForQuiz.map { it.id }.toSet())

        val recorder = recorder(SessionType.SELF_STUDY, subjectsForQuiz)
        val engine = SessionPlanner.plan(
            subjectsForQuiz, SessionConfig(SessionType.SELF_STUDY, SubjectOrder.SHUFFLE), recorder, Random(1), { now },
        )
        assertTrue(engine.isDelayed)
        val quiz = QuizController(engine, mapOf(1L to repo.answersFor(1)!!, 2L to repo.answersFor(2)!!))
        var wrongs = 0
        while (true) {
            val card = quiz.card() ?: break
            // Answer wrongly the first time each question appears, then correctly.
            val answer = if (wrongs < 2) { wrongs++; if (card.kanaInput) "あああ" else "banana" } else if (card.kanaInput) "すい" else if (card.itemId == 1L) "water" else "one"
            quiz.submit(answer)
            quiz.next()
        }
        quiz.finish()
        recorder.finish()

        assertEquals(0, pending.count())
        assertEquals(before, assignments.rows[10]!!)
        assertTrue(statistics.rows.isEmpty())
        // Nothing to send: a sync makes no POST or PUT.
        syncEngine().sync()
        assertTrue(requests.none { it.method == "POST" || it.method == "PUT" })
    }

    @Test
    fun anOfflineSessionKeepsItsResultsAndSendsThemLater() = runBlocking {
        assignments.upsert(listOf(assignment(10, 1, "kanji", stage = 3)))
        val repo = repository()
        val due = repo.dueReviews()
        val recorder = recorder(SessionType.REVIEW, due)
        val engine = SessionPlanner.plan(due, SessionConfig(SessionType.REVIEW, SubjectOrder.SHUFFLE), recorder, Random(1), { now })
        val quiz = QuizController(engine, mapOf(1L to repo.answersFor(1)!!))
        while (true) {
            val card = quiz.card() ?: break
            quiz.submit(if (card.kanaInput) "すい" else "water")
            quiz.next()
        }
        quiz.finish()
        recorder.finish()
        assertEquals(1, pending.count())

        // The server is down: nothing is lost, the run stops before fetching anything.
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse().setResponseCode(503)
        }
        val failed = syncEngine().sync()
        assertFalse(failed.ok)
        assertEquals(1, pending.count())
        assertEquals(1, pending.all().single().attempts)
        assertEquals(4, assignments.rows[10]!!.srsStage)
    }
}
