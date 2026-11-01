package com.example.durtlesreborn.data

import com.example.durtlesreborn.api.ApiResult
import com.example.durtlesreborn.api.AssignmentData
import com.example.durtlesreborn.api.Resource
import com.example.durtlesreborn.api.ReviewBody
import com.example.durtlesreborn.api.ReviewResponse
import com.example.durtlesreborn.api.ReviewStatisticData
import com.example.durtlesreborn.api.SrsSystemData
import com.example.durtlesreborn.api.StudyMaterialData
import com.example.durtlesreborn.api.WaniKaniClient
import com.example.durtlesreborn.api.apiJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

/** Result of one step of a sync run. */
sealed interface SyncStep {
    data class Done(val count: Int) : SyncStep

    /** The server said nothing changed. */
    data object Unchanged : SyncStep

    /** The step failed; [result] says why. The run stops when the failure is not recoverable. */
    data class Failed(val result: ApiResult<*>) : SyncStep
}

data class SyncReport(
    val reviewsSent: Int,
    /** Reviews the server refused (HTTP 422); they are dropped because the server wins. */
    val reviewsRejected: Int,
    val steps: Map<String, SyncStep>,
) {
    val ok get() = steps.values.none { it is SyncStep.Failed }
}

/**
 * Brings the local database and the server in step. Order matters: finished reviews are sent
 * first so the assignments fetched afterwards already include them.
 *
 * Collections are fetched with `updated_after` set to the newest `data_updated_at` seen, so a
 * normal run only transfers changes. A run stops at the first failure that retrying will not
 * fix right now (bad token, rate limit, network down).
 */
class SyncEngine(
    private val client: WaniKaniClient,
    private val pending: PendingReviewDao,
    private val subjects: SubjectDao,
    private val assignments: AssignmentDao,
    private val statistics: ReviewStatisticDao,
    private val materials: StudyMaterialDao,
    private val srsSystems: SrsSystemDao,
    private val state: SyncStateDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /** Called after each downloaded page with the collection's name, how many are in so far and the total. */
    @Volatile
    var onProgress: ((name: String, fetched: Int, total: Int) -> Unit)? = null

    suspend fun sync(): SyncReport {
        val steps = LinkedHashMap<String, SyncStep>()
        val drain = drainQueue()
        drain.failure?.let { steps["reviews"] = SyncStep.Failed(it) }
        if (drain.stop) return SyncReport(drain.sent, drain.rejected, steps)

        val plan: List<Pair<String, suspend () -> SyncStep>> = listOf(
            "user" to { syncUser() },
            "srs_systems" to {
                syncCollection("spaced_repetition_systems", SrsSystemData.serializer(), { it.toEntity() }) { srsSystems.upsert(it) }
            },
            "subjects" to {
                syncCollection("subjects", JsonObject.serializer(), { it.toEntity() }) { subjects.upsert(it) }
            },
            "assignments" to {
                syncCollection("assignments", AssignmentData.serializer(), { it.toEntity() }) { assignments.upsert(it) }
            },
            "review_statistics" to {
                syncCollection("review_statistics", ReviewStatisticData.serializer(), { it.toEntity() }) { statistics.upsert(it) }
            },
            "study_materials" to {
                syncCollection("study_materials", StudyMaterialData.serializer(), { it.toEntity() }) { materials.upsert(it) }
            },
        )
        for ((name, block) in plan) {
            val step = block()
            steps[name] = step
            if (step is SyncStep.Failed && step.result.isFatalForRun()) break
        }
        return SyncReport(drain.sent, drain.rejected, steps)
    }

    private class Drain(val sent: Int, val rejected: Int, val stop: Boolean, val failure: ApiResult<*>? = null)

    /** Sends queued reviews oldest first. Stops at the first failure so order is kept. */
    private suspend fun drainQueue(): Drain {
        var sent = 0
        var rejected = 0
        for (item in pending.all()) {
            val result = send(item)
            when (result) {
                is ApiResult.Success -> {
                    applyResponse(result.value)
                    pending.delete(item.id)
                    sent++
                }
                is ApiResult.HttpError -> when {
                    // The server refuses reviews it cannot accept (already reviewed elsewhere,
                    // not available yet). The server wins, so the local copy is dropped.
                    result.code == 422 -> {
                        pending.delete(item.id)
                        rejected++
                    }
                    result.code >= 500 -> {
                        pending.markAttempted(item.id)
                        return Drain(sent, rejected, stop = true, failure = result)
                    }
                    else -> pending.markAttempted(item.id)
                }
                is ApiResult.NotModified -> pending.markAttempted(item.id)
                else -> return Drain(sent, rejected, stop = true, failure = result)
            }
        }
        return Drain(sent, rejected, stop = false)
    }

    /** Timestamps are only sent for items finished more than five minutes ago, like the original. */
    private fun lateTimestamp(createdAt: Long): String? =
        if (createdAt > 0 && clock() - createdAt > LATE_AFTER_MILLIS) Instant.ofEpochMilli(createdAt).toString() else null

    private suspend fun send(item: PendingReview): ApiResult<String> = when (item.kind) {
        PendingKind.START_LESSON.name -> {
            val assignmentId = item.assignmentId.takeIf { it > 0 } ?: findAssignmentId(item.subjectId)
            if (assignmentId == null) {
                ApiResult.HttpError(404, "No assignment for subject ${item.subjectId}")
            } else {
                client.startAssignment(assignmentId, lateTimestamp(item.createdAt))
            }
        }
        else -> client.postReview(
            ReviewBody(
                subjectId = item.subjectId.takeIf { it > 0 },
                assignmentId = item.assignmentId.takeIf { it > 0 && item.subjectId <= 0 },
                incorrectMeaningAnswers = item.incorrectMeaningAnswers,
                incorrectReadingAnswers = item.incorrectReadingAnswers,
                createdAt = lateTimestamp(item.createdAt),
            ),
        )
    }

    private suspend fun findAssignmentId(subjectId: Long): Long? {
        assignments.bySubject(subjectId)?.let { return it.id }
        val result = client.all("assignments", AssignmentData.serializer(), query = mapOf("subject_ids" to subjectId.toString()))
        return (result as? ApiResult.Success)?.value?.firstOrNull()?.id
    }

    /** Both review and start-assignment responses carry the changed assignment (and statistic). */
    private suspend fun applyResponse(responseBody: String) {
        val json = runCatching { apiJson.parseToJsonElement(responseBody) }.getOrNull()
        val updated = runCatching { apiJson.decodeFromString(ReviewResponse.serializer(), responseBody) }
            .getOrNull()?.resourcesUpdated
        updated?.assignment?.let { assignments.upsert(listOf(it.toEntity())) }
        updated?.reviewStatistic?.let { statistics.upsert(listOf(it.toEntity())) }
        // PUT /assignments/{id}/start answers with the assignment itself.
        val asObject = json as? JsonObject ?: return
        if (updated == null && asObject["object"]?.jsonPrimitive?.contentOrNull == "assignment") {
            runCatching { apiJson.decodeFromString(Resource.serializer(AssignmentData.serializer()), responseBody) }
                .getOrNull()?.let { assignments.upsert(listOf(it.toEntity())) }
        }
    }

    private suspend fun syncUser(): SyncStep {
        return when (val result = client.user()) {
            is ApiResult.Success -> {
                val user = result.value.data
                state.put(SyncStateEntity(KEY_LEVEL, user.level.toString()))
                state.put(SyncStateEntity(KEY_USERNAME, user.username))
                state.put(SyncStateEntity(KEY_MAX_LEVEL, user.subscription.maxLevelGranted.toString()))
                SyncStep.Done(1)
            }
            is ApiResult.NotModified -> SyncStep.Unchanged
            else -> SyncStep.Failed(result)
        }
    }

    private suspend fun <D, E> syncCollection(
        path: String,
        serializer: KSerializer<D>,
        toEntity: (Resource<D>) -> E,
        store: suspend (List<E>) -> Unit,
    ): SyncStep {
        val key = "$path.updated_after"
        var newest: String? = null
        // Each page is saved as it arrives. The "updated after" marker moves only once the whole
        // collection is in, so an interrupted run starts the collection again rather than skipping.
        val result = client.pages(path, serializer, updatedAfter = state.get(key)) { page, fetched, total ->
            if (page.isNotEmpty()) store(page.map(toEntity))
            // ISO 8601 timestamps in one format sort as text, so the newest is the maximum.
            page.mapNotNull { it.dataUpdatedAt }.maxOrNull()?.let { latest -> if (newest.let { it == null || latest > it }) newest = latest }
            onProgress?.invoke(path, fetched, total)
        }
        return when (result) {
            is ApiResult.Success -> {
                if (result.value.count == 0) return SyncStep.Done(0)
                newest?.let { state.put(SyncStateEntity(key, it)) }
                SyncStep.Done(result.value.count)
            }
            is ApiResult.NotModified -> SyncStep.Unchanged
            else -> SyncStep.Failed(result)
        }
    }

    private fun ApiResult<*>.isFatalForRun() = when (this) {
        is ApiResult.Unauthorized, is ApiResult.RateLimited, is ApiResult.NetworkError -> true
        else -> false
    }

    companion object {
        private const val LATE_AFTER_MILLIS = 5 * 60 * 1000L
        const val KEY_USERNAME = "user.username"
        const val KEY_LEVEL = "user.level"
        const val KEY_MAX_LEVEL = "user.max_level_granted"
    }
}
