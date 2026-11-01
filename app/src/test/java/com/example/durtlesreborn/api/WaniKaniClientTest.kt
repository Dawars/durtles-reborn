package com.example.durtlesreborn.api

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WaniKaniClientTest {
    private lateinit var server: MockWebServer
    private val sleeps = mutableListOf<Long>()
    private var now = 1_000_000L

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
    }

    @After
    fun tearDown() = server.shutdown()

    private fun client(retries: Int = 3) = WaniKaniClient(
        http = OkHttpClient(),
        token = { "secret-token" },
        baseUrl = server.url("/v2/"),
        maxRateLimitRetries = retries,
        clock = { now },
        sleep = { sleeps += it },
        allowInsecureBaseUrl = true,
    )

    private fun json(body: String, code: Int = 200) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private val userJson = """
        {"object":"user","url":"u","data_updated_at":"2026-01-01T00:00:00.000000Z",
         "data":{"username":"kame","level":7,"subscription":{"active":true,"type":"recurring","max_level_granted":60},"extra":1}}
    """.trimIndent()

    private fun pageJson(next: String?, vararg ids: Int): String {
        val items = ids.joinToString(",") { """{"id":$it,"object":"radical","data":{"level":1}}""" }
        val nextJson = if (next == null) "null" else "\"$next\""
        return """{"object":"collection","pages":{"next_url":$nextJson,"per_page":500},"total_count":${ids.size},"data":[$items]}"""
    }

    @Test
    fun sendsTokenAndRevisionAndParsesUser() = runBlocking {
        server.enqueue(json(userJson).setHeader("ETag", "\"abc\""))
        val result = client().user() as ApiResult.Success
        assertEquals("kame", result.value.data.username)
        assertEquals(60, result.value.data.subscription.maxLevelGranted)
        assertEquals("\"abc\"", result.etag)
        val request = server.takeRequest()
        assertEquals("Bearer secret-token", request.getHeader("Authorization"))
        assertEquals(WaniKaniClient.REVISION, request.getHeader("Wanikani-Revision"))
        assertEquals("/v2/user", request.path)
    }

    @Test
    fun sendsValidatorsAndReportsNotModified() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(304))
        val result = client().user(CacheValidators(etag = "\"abc\"", lastModified = "Thu, 01 Jan 2026 00:00:00 GMT"))
        assertEquals(ApiResult.NotModified, result)
        val request = server.takeRequest()
        assertEquals("\"abc\"", request.getHeader("If-None-Match"))
        assertEquals("Thu, 01 Jan 2026 00:00:00 GMT", request.getHeader("If-Modified-Since"))
    }

    @Test
    fun updatedAfterIsSentAsQueryParameter() = runBlocking {
        server.enqueue(json(pageJson(null, 1)))
        client().page("subjects", JsonObject.serializer(), updatedAfter = "2026-01-01T00:00:00Z")
        assertEquals("2026-01-01T00:00:00Z", server.takeRequest().requestUrl!!.queryParameter("updated_after"))
    }

    @Test
    fun allFollowsNextUrlUntilExhausted() = runBlocking {
        server.enqueue(json(pageJson(server.url("/v2/subjects?page_after_id=2").toString(), 1, 2)))
        server.enqueue(json(pageJson(null, 3)))
        val result = client().all("subjects", JsonObject.serializer()) as ApiResult.Success
        assertEquals(listOf(1L, 2L, 3L), result.value.map { it.id })
        assertEquals(2, server.requestCount)
        server.takeRequest()
        assertEquals("2", server.takeRequest().requestUrl!!.queryParameter("page_after_id"))
    }

    @Test
    fun pagesAreHandedOverOneAtATimeWithRunningCounts() = runBlocking {
        server.enqueue(json(pageJson(server.url("/v2/subjects?page_after_id=2").toString(), 1, 2)))
        server.enqueue(json(pageJson(null, 3)))
        val seen = mutableListOf<Triple<List<Long>, Int, Int>>()
        val run = client().pages("subjects", JsonObject.serializer()) { page, fetched, total ->
            seen += Triple(page.map { it.id }, fetched, total)
        } as ApiResult.Success
        assertEquals(3, run.value.count)
        assertEquals(listOf(Triple(listOf(1L, 2L), 2, 2), Triple(listOf(3L), 3, 1)), seen)
    }

    @Test
    fun pagesAlreadyHandedOverStayHandedOverWhenALaterPageFails() = runBlocking {
        server.enqueue(json(pageJson(server.url("/v2/subjects?page_after_id=2").toString(), 1, 2)))
        server.enqueue(MockResponse().setResponseCode(500))
        val seen = mutableListOf<Long>()
        val result = client().pages("subjects", JsonObject.serializer()) { page, _, _ -> seen += page.map { it.id } }
        assertTrue(result is ApiResult.HttpError)
        assertEquals(listOf(1L, 2L), seen)
    }

    @Test
    fun pagingLinkToAnotherHostIsRefusedAndTokenNotSent() = runBlocking {
        server.enqueue(json(pageJson("http://evil.example/v2/subjects?page_after_id=2", 1)))
        val result = client().all("subjects", JsonObject.serializer())
        assertTrue(result is ApiResult.HttpError)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun rateLimitWaitsUntilResetThenRetries() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("RateLimit-Reset", "${now / 1000 + 10}"))
        server.enqueue(json(userJson))
        val result = client().user()
        assertTrue(result is ApiResult.Success)
        assertEquals(listOf(10_000L), sleeps)
    }

    @Test
    fun rateLimitGivesUpAfterTheRetryLimit() = runBlocking {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(429)) }
        val result = client(retries = 2).user()
        assertTrue(result is ApiResult.RateLimited)
        assertEquals(2, sleeps.size)
        assertEquals(3, server.requestCount)
    }

    @Test
    fun rejectedTokenIsReportedAsUnauthorized() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        assertEquals(ApiResult.Unauthorized(401), client().user())
    }

    @Test
    fun otherErrorsCarryTheStatusCode() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("boom"))
        assertEquals(ApiResult.HttpError(500, "boom"), client().user())
    }

    @Test
    fun postReviewSendsTheDocumentedBody() = runBlocking {
        server.enqueue(json("""{"id":1,"object":"review"}""", 201))
        val result = client().postReview(ReviewBody(subjectId = 42, incorrectMeaningAnswers = 1, incorrectReadingAnswers = 2))
        assertTrue(result is ApiResult.Success)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v2/reviews", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"subject_id\":42"))
        assertTrue(body.contains("\"incorrect_meaning_answers\":1"))
        assertTrue(body.contains("\"incorrect_reading_answers\":2"))
        // Unset optional fields are not sent at all.
        assertFalse(body.contains("created_at"))
        assertFalse(body.contains("assignment_id"))
    }

    @Test
    fun startAssignmentPutsToTheStartEndpoint() = runBlocking {
        server.enqueue(json("""{"id":5,"object":"assignment"}"""))
        client().startAssignment(5, startedAt = "2026-01-01T00:00:00Z")
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/v2/assignments/5/start", request.path)
        assertTrue(request.body.readUtf8().contains("\"started_at\":\"2026-01-01T00:00:00Z\""))
    }

    @Test
    fun plainHttpBaseUrlIsRefusedByDefault() {
        val failure = runCatching { WaniKaniClient(OkHttpClient(), { "t" }, "http://api.example/v2/".toHttpUrl()) }
        assertTrue(failure.exceptionOrNull() is IllegalArgumentException)
    }
}
