package com.example.durtlesreborn.data

import com.example.durtlesreborn.api.ApiResult
import com.example.durtlesreborn.api.WaniKaniClient
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

class SyncEngineTest {
    private lateinit var server: MockWebServer
    private val requests = mutableListOf<RecordedRequest>()
    private val reviewResponses = ArrayDeque<MockResponse>()
    private val overrides = mutableMapOf<String, MockResponse>()

    private val pending = FakePending()
    private val subjects = FakeSubjects()
    private val assignments = FakeAssignments()
    private val statistics = FakeStatistics()
    private val materials = FakeMaterials()
    private val srsSystems = FakeSrsSystems()
    private val state = FakeState()

    private fun json(body: String, code: Int = 200) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private fun page(vararg items: String) =
        """{"object":"collection","pages":{"next_url":null,"per_page":500},"total_count":${items.size},"data":[${items.joinToString(",")}]}"""

    private val responses = mapOf(
        "/v2/user" to """{"object":"user","data":{"username":"kame","level":7,"subscription":{"active":true,"max_level_granted":60}}}""",
        "/v2/subjects" to page(
            """{"id":1,"object":"radical","data_updated_at":"2026-03-01T10:00:00.000000Z","data":{"level":1,"slug":"one","characters":"一"}}""",
            """{"id":2,"object":"kanji","data_updated_at":"2026-03-02T10:00:00.000000Z","data":{"level":2,"slug":"two","characters":"二","hidden_at":"2026-01-01T00:00:00.000000Z"}}""",
        ),
        "/v2/assignments" to page(
            """{"id":10,"object":"assignment","data_updated_at":"2026-03-05T10:00:00.000000Z","data":{"subject_id":1,"subject_type":"radical","srs_stage":2,"unlocked_at":"2026-02-01T00:00:00.000000Z","started_at":"2026-02-02T00:00:00.000000Z","available_at":"2026-03-06T00:00:00.000000Z"}}""",
        ),
        "/v2/review_statistics" to page(
            """{"id":20,"object":"review_statistic","data_updated_at":"2026-03-04T10:00:00.000000Z","data":{"subject_id":1,"subject_type":"radical","meaning_correct":5,"percentage_correct":90}}""",
        ),
        "/v2/spaced_repetition_systems" to page(
            """{"id":1,"object":"spaced_repetition_system","data_updated_at":"2026-02-01T00:00:00.000000Z","data":{"name":"Default","starting_stage_position":1,"passing_stage_position":5,"burning_stage_position":9,"stages":[{"interval":null,"position":0},{"interval":14400,"position":1},{"interval":28800,"position":2},{"interval":null,"position":9}]}}""",
        ),
        "/v2/study_materials" to page(
            """{"id":30,"object":"study_material","data_updated_at":"2026-03-03T10:00:00.000000Z","data":{"subject_id":1,"meaning_note":"note","meaning_synonyms":["uno"]}}""",
        ),
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val path = request.requestUrl!!.encodedPath
                if (request.method != "GET") return reviewResponses.removeFirstOrNull() ?: json("{}", 201)
                overrides[path]?.let { return it }
                return json(responses[path] ?: error("unexpected $path"))
            }
        }
        server.start()
    }

    @After
    fun tearDown() = server.shutdown()

    private fun engine(retries: Int = 0, clock: () -> Long = System::currentTimeMillis) = SyncEngine(
        client = WaniKaniClient(
            OkHttpClient(), { "t" }, server.url("/v2/"),
            maxRateLimitRetries = retries, sleep = {}, allowInsecureBaseUrl = true,
        ),
        pending, subjects, assignments, statistics, materials, srsSystems, state, clock,
    )

    private suspend fun queue(assignmentId: Long, createdAt: Long = 1_700_000_000_000) =
        pending.insert(PendingReview(assignmentId = assignmentId, incorrectMeaningAnswers = 1, incorrectReadingAnswers = 0, createdAt = createdAt))

    @Test
    fun fullSyncStoresEverythingAndRemembersUserLimits() = runBlocking {
        val report = engine().sync()
        assertTrue(report.ok)
        assertEquals(2, subjects.rows.size)
        assertEquals("一", subjects.rows[1]!!.characters)
        assertFalse(subjects.rows[1]!!.hidden)
        assertTrue(subjects.rows[2]!!.hidden)
        assertEquals(2, assignments.rows[10]!!.srsStage)
        assertNotNull(assignments.rows[10]!!.availableAt)
        assertEquals(90, statistics.rows[20]!!.percentageCorrect)
        assertEquals("""["uno"]""", materials.rows[30]!!.meaningSynonymsJson)
        val srs = srsSystems.rows[1]!!.toSrsSystem()
        assertEquals(5, srs.passedIndex)
        assertEquals(14_400_000L, srs.intervalMillis(1))
        assertEquals(28_800_000L, srs.intervalMillis(2))
        assertEquals(0L, srs.intervalMillis(9))
        assertEquals("60", state.rows[SyncEngine.KEY_MAX_LEVEL])
        assertEquals("7", state.rows[SyncEngine.KEY_LEVEL])
    }

    @Test
    fun laterSyncAsksOnlyForChangesSinceTheNewestSeen() = runBlocking {
        engine().sync()
        requests.clear()
        engine().sync()
        val subjectsRequest = requests.first { it.requestUrl!!.encodedPath == "/v2/subjects" }
        assertEquals("2026-03-02T10:00:00.000000Z", subjectsRequest.requestUrl!!.queryParameter("updated_after"))
        val firstRun = requests.first { it.requestUrl!!.encodedPath == "/v2/assignments" }
        assertEquals("2026-03-05T10:00:00.000000Z", firstRun.requestUrl!!.queryParameter("updated_after"))
    }

    @Test
    fun queuedReviewsAreSentOldestFirstBeforeAnythingIsFetched() = runBlocking {
        queue(111, createdAt = 1_700_000_000_000)
        queue(222, createdAt = 1_700_000_100_000)
        val report = engine().sync()
        assertEquals(2, report.reviewsSent)
        assertEquals(0, pending.count())
        val methodsAndPaths = requests.map { it.method to it.requestUrl!!.encodedPath }
        assertEquals("POST" to "/v2/reviews", methodsAndPaths[0])
        assertEquals("POST" to "/v2/reviews", methodsAndPaths[1])
        assertEquals("GET" to "/v2/user", methodsAndPaths[2])
        val first = requests[0].body.readUtf8()
        assertTrue(first.contains("\"assignment_id\":111"))
        assertTrue(first.contains("\"created_at\":\"2023-11-14T22:13:20Z\""))
        assertTrue(requests[1].body.readUtf8().contains("\"assignment_id\":222"))
    }

    @Test
    fun assignmentFromTheReviewResponseIsStored() = runBlocking {
        queue(10)
        reviewResponses += json(
            """{"id":1,"object":"review","resources_updated":{"assignment":{"id":10,"object":"assignment","data_updated_at":"2026-03-07T00:00:00.000000Z","data":{"subject_id":1,"subject_type":"radical","srs_stage":3}}}}""",
            201,
        )
        // Keep the later assignments fetch from overwriting what the response stored.
        overrides["/v2/assignments"] = json(page())
        engine().sync()
        assertEquals(3, assignments.rows[10]!!.srsStage)
    }

    @Test
    fun reviewTheServerRejectsIsDroppedAndTheRestStillSent() = runBlocking {
        queue(111)
        queue(222)
        reviewResponses += json("""{"error":"not available"}""", 422)
        val report = engine().sync()
        assertEquals(1, report.reviewsRejected)
        assertEquals(1, report.reviewsSent)
        assertEquals(0, pending.count())
    }

    @Test
    fun serverErrorKeepsTheQueueAndStopsBeforeFetching() = runBlocking {
        queue(111)
        queue(222)
        reviewResponses += MockResponse().setResponseCode(503)
        val report = engine().sync()
        assertFalse(report.ok)
        assertTrue((report.steps["reviews"] as SyncStep.Failed).result is ApiResult.HttpError)
        assertEquals(2, pending.count())
        assertEquals(1, pending.rows.first { it.assignmentId == 111L }.attempts)
        assertEquals(1, requests.size)
        assertNull(state.rows[SyncEngine.KEY_LEVEL])
    }

    @Test
    fun badTokenStopsTheRun() = runBlocking {
        overrides["/v2/user"] = MockResponse().setResponseCode(401)
        val report = engine().sync()
        assertEquals(setOf("user"), report.steps.keys)
        assertTrue((report.steps["user"] as SyncStep.Failed).result is ApiResult.Unauthorized)
        assertEquals(0, subjects.count())
    }

    @Test
    fun rateLimitMidRunStopsLaterSteps() = runBlocking {
        overrides["/v2/assignments"] = MockResponse().setResponseCode(429)
        val report = engine(retries = 0).sync()
        assertTrue(report.steps["assignments"] is SyncStep.Failed)
        assertFalse(report.steps.containsKey("review_statistics"))
        assertEquals(2, subjects.count())
    }

    @Test
    fun reviewsAreSentBySubjectAndRecentOnesLeaveOutTheTimestamp() = runBlocking {
        val now = 1_700_000_000_000
        pending.insert(PendingReview(subjectId = 7, incorrectMeaningAnswers = 2, createdAt = now - 60_000))
        pending.insert(PendingReview(subjectId = 8, createdAt = now - 10 * 60_000))
        engine(clock = { now }).sync()
        val recent = requests[0].body.readUtf8()
        assertTrue(recent.contains("\"subject_id\":7"))
        assertFalse(recent.contains("assignment_id"))
        assertFalse(recent.contains("created_at"))
        assertTrue(requests[1].body.readUtf8().contains("\"created_at\""))
    }

    @Test
    fun lessonStartIsPutAndTheReturnedAssignmentStored() = runBlocking {
        pending.insert(PendingReview(kind = PendingKind.START_LESSON.name, subjectId = 1, assignmentId = 10, createdAt = System.currentTimeMillis()))
        reviewResponses += json(
            """{"id":10,"object":"assignment","data_updated_at":"2026-03-07T00:00:00.000000Z","data":{"subject_id":1,"subject_type":"radical","srs_stage":1,"started_at":"2026-03-07T00:00:00.000000Z"}}""",
        )
        overrides["/v2/assignments"] = json(page())
        engine().sync()
        val request = requests.first { it.method == "PUT" }
        assertEquals("/v2/assignments/10/start", request.requestUrl!!.encodedPath)
        assertEquals(1, assignments.rows[10]!!.srsStage)
        assertNotNull(assignments.rows[10]!!.startedAt)
        assertEquals(0, pending.count())
    }

    @Test
    fun lessonStartLooksUpTheAssignmentWhenItsIdIsUnknown() = runBlocking {
        pending.insert(PendingReview(kind = PendingKind.START_LESSON.name, subjectId = 1, createdAt = System.currentTimeMillis()))
        overrides["/v2/assignments"] = json(page("""{"id":10,"object":"assignment","data":{"subject_id":1,"subject_type":"radical"}}"""))
        engine().sync()
        val lookup = requests.first { it.method == "GET" && it.requestUrl!!.encodedPath == "/v2/assignments" }
        assertEquals("1", lookup.requestUrl!!.queryParameter("subject_ids"))
        assertEquals("/v2/assignments/10/start", requests.first { it.method == "PUT" }.requestUrl!!.encodedPath)
    }

    @Test
    fun reviewStatisticFromTheResponseIsStored() = runBlocking {
        pending.insert(PendingReview(subjectId = 1, createdAt = System.currentTimeMillis()))
        reviewResponses += json(
            """{"id":1,"object":"review","resources_updated":{"review_statistic":{"id":20,"object":"review_statistic","data_updated_at":"2026-03-07T00:00:00.000000Z","data":{"subject_id":1,"subject_type":"radical","meaning_correct":9,"percentage_correct":95}}}}""",
            201,
        )
        overrides["/v2/review_statistics"] = json(page())
        engine().sync()
        assertEquals(95, statistics.rows[20]!!.percentageCorrect)
    }

    private val noMaterials get() = json(page())

    private fun material(id: Long, subject: Long, vararg synonyms: String) =
        """{"id":$id,"object":"study_material","data_updated_at":"2026-03-08T00:00:00.000000Z","data":{"subject_id":$subject,""" +
            """"meaning_synonyms":[${synonyms.joinToString(",") { "\"$it\"" }}]}}"""

    @Test
    fun aSynonymOnANewSubjectIsPostedAndTheLocalRowReplaced() = runBlocking {
        overrides["/v2/study_materials"] = noMaterials
        Synonyms(materials, pending).add(7, " hot mountain ")
        assertEquals(listOf("hot mountain"), materials.bySubject(7)!!.synonyms())
        assertEquals(-7L, materials.bySubject(7)!!.id)
        reviewResponses += json(material(40, 7, "hot mountain"), 201)
        engine().sync()
        val request = requests.first { it.method == "POST" }
        assertEquals("/v2/study_materials", request.requestUrl!!.encodedPath)
        assertEquals("""{"study_material":{"subject_id":7,"meaning_synonyms":["hot mountain"]}}""", request.body.readUtf8())
        assertEquals(40L, materials.bySubject(7)!!.id)
        assertEquals(1, materials.rows.values.count { it.subjectId == 7L })
        assertEquals(0, pending.count())
    }

    @Test
    fun aSynonymKeepsWhatTheServerAlreadyHas() = runBlocking {
        overrides["/v2/study_materials"] = json(page(material(30, 1, "other device")))
        Synonyms(materials, pending).add(1, "mine")
        reviewResponses += json(material(30, 1, "other device", "mine"))
        engine().sync()
        val request = requests.first { it.method == "PUT" }
        assertEquals("/v2/study_materials/30", request.requestUrl!!.encodedPath)
        assertEquals("""{"study_material":{"meaning_synonyms":["other device","mine"]}}""", request.body.readUtf8())
    }

    @Test
    fun aRefusedSynonymIsForgotten() = runBlocking {
        overrides["/v2/study_materials"] = noMaterials
        Synonyms(materials, pending).add(7, "nope")
        reviewResponses += json("{}", 422)
        engine().sync()
        assertEquals(null, materials.bySubject(7))
        assertEquals(0, pending.count())
    }

    @Test
    fun aSynonymWaitsInTheQueueWhenTheServerFails() = runBlocking {
        overrides["/v2/study_materials"] = json("{}", 500)
        Synonyms(materials, pending).add(7, "later")
        assertEquals(false, engine().sync().ok)
        assertEquals(1, pending.count())
        assertEquals(listOf("later"), materials.bySubject(7)!!.synonyms())
    }
}
