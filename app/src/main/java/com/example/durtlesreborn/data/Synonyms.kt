package com.example.durtlesreborn.data

import com.example.durtlesreborn.api.apiJson
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

private val listSerializer = ListSerializer(String.serializer())

internal fun StudyMaterialEntity.synonyms(): List<String> = apiJson.decodeFromString(listSerializer, meaningSynonymsJson)

internal fun StudyMaterialEntity.withSynonyms(synonyms: List<String>) =
    copy(meaningSynonymsJson = apiJson.encodeToString(listSerializer, synonyms))

/**
 * Saves the user's own meaning synonyms. It is queued first and then applied here, so it counts as
 * an answer offline; the sync engine sends it. A subject with no study material yet gets a local row
 * with a negative id until the server answers with the real one.
 */
class Synonyms(
    private val materials: StudyMaterialDao,
    private val pending: PendingReviewDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun add(subjectId: Long, text: String) {
        val synonym = text.trim()
        if (synonym.isEmpty()) return
        pending.insert(PendingReview(kind = PendingKind.ADD_SYNONYM.name, subjectId = subjectId, synonym = synonym, createdAt = clock()))
        val row = materials.bySubject(subjectId) ?: StudyMaterialEntity(-subjectId, subjectId, null, null, "[]", 0)
        val known = row.synonyms()
        if (known.none { it.equals(synonym, ignoreCase = true) }) materials.upsert(listOf(row.withSynonyms(known + synonym)))
    }
}
