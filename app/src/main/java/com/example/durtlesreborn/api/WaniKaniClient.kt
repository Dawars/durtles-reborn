package com.example.durtlesreborn.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

sealed interface ApiResult<out T> {
    /** [etag] and [lastModified] are sent back on the next request so unchanged data returns 304. */
    data class Success<T>(val value: T, val etag: String? = null, val lastModified: String? = null) : ApiResult<T>

    data object NotModified : ApiResult<Nothing>

    /** The token was rejected (401) or lacks access (403); the user must fix the token. */
    data class Unauthorized(val code: Int) : ApiResult<Nothing>

    data class HttpError(val code: Int, val body: String?) : ApiResult<Nothing>

    /** Rate limit hit and the retries ran out. */
    data class RateLimited(val retryAfterMillis: Long) : ApiResult<Nothing>

    data class NetworkError(val cause: IOException) : ApiResult<Nothing>
}

/** Validators from an earlier response, sent back as If-None-Match and If-Modified-Since. */
data class CacheValidators(val etag: String? = null, val lastModified: String? = null)

/**
 * Client for the WaniKani API v2, following its rules: bearer token, HTTPS only, revision header,
 * conditional requests, cursor paging and the 60 requests per minute limit (HTTP 429, then wait
 * until `RateLimit-Reset`).
 *
 * The token is only ever sent to the host of [baseUrl], including when following paging links.
 */
class WaniKaniClient(
    private val http: OkHttpClient,
    private val token: () -> String,
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL,
    private val maxRateLimitRetries: Int = 3,
    private val clock: () -> Long = System::currentTimeMillis,
    private val sleep: suspend (Long) -> Unit = { delay(it) },
    allowInsecureBaseUrl: Boolean = false,
) {
    init {
        require(baseUrl.isHttps || allowInsecureBaseUrl) { "The WaniKani API must be used over HTTPS" }
    }

    suspend fun user(validators: CacheValidators = CacheValidators()): ApiResult<Resource<UserData>> =
        get(baseUrl.resolve("user")!!, validators, Resource.serializer(UserData.serializer()))

    /**
     * Fetches one page of a collection. [path] is relative to the base URL, e.g. "subjects".
     * Pass [updatedAfter] (an ISO 8601 timestamp) to ask only for changes.
     */
    suspend fun <T> page(
        path: String,
        dataSerializer: KSerializer<T>,
        query: Map<String, String> = emptyMap(),
        updatedAfter: String? = null,
        validators: CacheValidators = CacheValidators(),
    ): ApiResult<Collection<T>> {
        val url = baseUrl.newBuilder().addPathSegments(path).apply {
            for ((k, v) in query) addQueryParameter(k, v)
            if (updatedAfter != null) addQueryParameter("updated_after", updatedAfter)
        }.build()
        return get(url, validators, Collection.serializer(dataSerializer))
    }

    /** How many resources a [pages] run delivered. */
    data class PagesRun(val count: Int)

    /**
     * Follows `next_url` until the collection is exhausted, handing each page to [onPage] as it
     * arrives with the number fetched so far and the collection's total. Nothing is kept here, so a
     * large collection never has to fit in memory at once.
     */
    suspend fun <T> pages(
        path: String,
        dataSerializer: KSerializer<T>,
        query: Map<String, String> = emptyMap(),
        updatedAfter: String? = null,
        validators: CacheValidators = CacheValidators(),
        onPage: suspend (page: List<Resource<T>>, fetched: Int, total: Int) -> Unit,
    ): ApiResult<PagesRun> {
        val serializer = Collection.serializer(dataSerializer)
        var first: ApiResult.Success<Collection<T>>? = null
        var fetched = 0
        var next: HttpUrl? = baseUrl.newBuilder().addPathSegments(path).apply {
            for ((k, v) in query) addQueryParameter(k, v)
            if (updatedAfter != null) addQueryParameter("updated_after", updatedAfter)
        }.build()
        while (next != null) {
            // Validators apply to the first page only; later pages are never "not modified" on their own.
            val result = get(next, if (first == null) validators else CacheValidators(), serializer)
            when (result) {
                is ApiResult.Success -> {
                    if (first == null) first = result
                    fetched += result.value.data.size
                    onPage(result.value.data, fetched, result.value.totalCount)
                    val nextUrl = result.value.pages.nextUrl
                    next = if (nextUrl == null) null else sameHostUrl(nextUrl)
                        ?: return ApiResult.HttpError(-1, "Paging link left the API host")
                }
                is ApiResult.NotModified -> return ApiResult.NotModified
                is ApiResult.Unauthorized -> return result
                is ApiResult.HttpError -> return result
                is ApiResult.RateLimited -> return result
                is ApiResult.NetworkError -> return result
            }
        }
        return ApiResult.Success(PagesRun(fetched), first?.etag, first?.lastModified)
    }

    /** Collects every page of a collection. Prefer [pages] for the big ones. */
    suspend fun <T> all(
        path: String,
        dataSerializer: KSerializer<T>,
        query: Map<String, String> = emptyMap(),
        updatedAfter: String? = null,
        validators: CacheValidators = CacheValidators(),
    ): ApiResult<List<Resource<T>>> {
        val items = ArrayList<Resource<T>>()
        return when (val run = pages(path, dataSerializer, query, updatedAfter, validators) { page, _, _ -> items += page }) {
            is ApiResult.Success -> ApiResult.Success(items, run.etag, run.lastModified)
            is ApiResult.NotModified -> ApiResult.NotModified
            is ApiResult.Unauthorized -> run
            is ApiResult.HttpError -> run
            is ApiResult.RateLimited -> run
            is ApiResult.NetworkError -> run
        }
    }

    /** Sends a finished review. Returns the raw response body on success. */
    suspend fun postReview(review: ReviewBody): ApiResult<String> {
        val json = apiJson.encodeToString(ReviewRequest.serializer(), ReviewRequest(review))
        val request = baseRequest(baseUrl.resolve("reviews")!!)
            .post(json.toRequestBody(JSON))
            .build()
        return execute(request) { it }
    }

    /** Starts a lesson item: PUT /assignments/{id}/start. [startedAt] is only for late reports. */
    suspend fun startAssignment(assignmentId: Long, startedAt: String? = null): ApiResult<String> {
        val json = apiJson.encodeToString(StartAssignmentBody.serializer(), StartAssignmentBody(startedAt))
        val request = baseRequest(baseUrl.resolve("assignments/$assignmentId/start")!!)
            .put(json.toRequestBody(JSON))
            .build()
        return execute(request) { it }
    }

    /** Replaces the synonyms of study material [id], or creates it when [id] is null. */
    suspend fun saveSynonyms(id: Long?, subjectId: Long, synonyms: List<String>): ApiResult<String> {
        val body = StudyMaterialBody(subjectId.takeIf { id == null }, synonyms)
        val json = apiJson.encodeToString(StudyMaterialRequest.serializer(), StudyMaterialRequest(body)).toRequestBody(JSON)
        val builder = baseRequest(baseUrl.resolve(if (id == null) "study_materials" else "study_materials/$id")!!)
        return execute(if (id == null) builder.post(json).build() else builder.put(json).build()) { it }
    }

    private fun sameHostUrl(url: String): HttpUrl? {
        val parsed = url.toHttpUrl()
        return if (parsed.host == baseUrl.host && parsed.scheme == baseUrl.scheme && parsed.port == baseUrl.port) parsed else null
    }

    private suspend fun <T> get(url: HttpUrl, validators: CacheValidators, serializer: KSerializer<T>): ApiResult<T> {
        val request = baseRequest(url).apply {
            validators.etag?.let { header("If-None-Match", it) }
            validators.lastModified?.let { header("If-Modified-Since", it) }
        }.get().build()
        return execute(request) { apiJson.decodeFromString(serializer, it) }
    }

    private fun baseRequest(url: HttpUrl) = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer ${token()}")
        .header("Wanikani-Revision", REVISION)

    private suspend fun <T> execute(request: Request, parse: (String) -> T): ApiResult<T> {
        var attempt = 0
        while (true) {
            val response = try {
                withContext(Dispatchers.IO) { http.newCall(request).execute() }
            } catch (e: IOException) {
                return ApiResult.NetworkError(e)
            }
            response.use { r ->
                when {
                    r.code == 304 -> return ApiResult.NotModified
                    r.isSuccessful -> {
                        val body = withContext(Dispatchers.IO) { r.body.string() }
                        return ApiResult.Success(parse(body), r.header("ETag"), r.header("Last-Modified"))
                    }
                    r.code == 401 || r.code == 403 -> return ApiResult.Unauthorized(r.code)
                    r.code == 429 -> {
                        val wait = retryAfterMillis(r)
                        if (attempt++ >= maxRateLimitRetries) return ApiResult.RateLimited(wait)
                        sleep(wait)
                    }
                    else -> return ApiResult.HttpError(r.code, runCatching { r.body.string() }.getOrNull())
                }
            }
        }
    }

    /** `RateLimit-Reset` is an epoch time in seconds; wait until then, at least a second. */
    private fun retryAfterMillis(response: Response): Long {
        val reset = response.header("RateLimit-Reset")?.toLongOrNull() ?: return DEFAULT_RETRY_MILLIS
        return (reset * 1000 - clock()).coerceIn(1000, MAX_RETRY_MILLIS)
    }

    companion object {
        val DEFAULT_BASE_URL: HttpUrl = "https://api.wanikani.com/v2/".toHttpUrl()
        const val REVISION = "20170710"
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private const val DEFAULT_RETRY_MILLIS = 5_000L
        private const val MAX_RETRY_MILLIS = 70_000L
    }
}
