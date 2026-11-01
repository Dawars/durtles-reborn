package com.example.durtlesreborn.data

import com.example.durtlesreborn.api.AssignmentData
import com.example.durtlesreborn.api.Resource
import com.example.durtlesreborn.api.ReviewStatisticData
import com.example.durtlesreborn.api.StudyMaterialData
import com.example.durtlesreborn.api.apiJson
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

/** Parses an API timestamp such as "2017-07-10T13:08:24.000000Z" to epoch milliseconds. */
internal fun parseTime(value: String?): Long? = value?.let { Instant.parse(it).toEpochMilli() }

private fun JsonObject.string(name: String): String? =
    (this[name] as? kotlinx.serialization.json.JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull

fun Resource<JsonObject>.toEntity() = SubjectEntity(
    id = id,
    type = type,
    level = data["level"]?.jsonPrimitive?.intOrNull ?: 0,
    slug = data.string("slug"),
    characters = data.string("characters"),
    hidden = data.string("hidden_at") != null,
    dataUpdatedAt = parseTime(dataUpdatedAt) ?: 0,
    json = data.toString(),
    searchText = buildSearchText(data.string("characters"), data.string("slug"), SubjectContent(data)),
)

fun Resource<AssignmentData>.toEntity() = AssignmentEntity(
    id = id,
    subjectId = data.subjectId,
    subjectType = data.subjectType,
    srsStage = data.srsStage,
    unlockedAt = parseTime(data.unlockedAt),
    startedAt = parseTime(data.startedAt),
    passedAt = parseTime(data.passedAt),
    burnedAt = parseTime(data.burnedAt),
    availableAt = parseTime(data.availableAt),
    resurrectedAt = parseTime(data.resurrectedAt),
    hidden = data.hidden,
    dataUpdatedAt = parseTime(dataUpdatedAt) ?: 0,
)

fun Resource<ReviewStatisticData>.toEntity() = ReviewStatisticEntity(
    id = id,
    subjectId = data.subjectId,
    subjectType = data.subjectType,
    meaningCorrect = data.meaningCorrect,
    meaningIncorrect = data.meaningIncorrect,
    meaningMaxStreak = data.meaningMaxStreak,
    meaningCurrentStreak = data.meaningCurrentStreak,
    readingCorrect = data.readingCorrect,
    readingIncorrect = data.readingIncorrect,
    readingMaxStreak = data.readingMaxStreak,
    readingCurrentStreak = data.readingCurrentStreak,
    percentageCorrect = data.percentageCorrect,
    hidden = data.hidden,
    dataUpdatedAt = parseTime(dataUpdatedAt) ?: 0,
)

fun Resource<StudyMaterialData>.toEntity() = StudyMaterialEntity(
    id = id,
    subjectId = data.subjectId,
    meaningNote = data.meaningNote,
    readingNote = data.readingNote,
    meaningSynonymsJson = apiJson.encodeToString(ListSerializer(String.serializer()), data.meaningSynonyms),
    dataUpdatedAt = parseTime(dataUpdatedAt) ?: 0,
)

fun Resource<com.example.durtlesreborn.api.SrsSystemData>.toEntity(): SrsSystemEntity {
    val byPosition = data.stages.associate { it.position to (it.interval ?: 0L) * 1000 }
    val intervals = (0..(byPosition.keys.maxOrNull() ?: 0)).map { byPosition[it] ?: 0L }
    return SrsSystemEntity(
        id = id,
        startingPosition = data.startingStagePosition,
        passingPosition = data.passingStagePosition,
        burningPosition = data.burningStagePosition,
        intervalsMillisJson = apiJson.encodeToString(ListSerializer(Long.serializer()), intervals),
        dataUpdatedAt = parseTime(dataUpdatedAt) ?: 0,
    )
}

fun SrsSystemEntity.toSrsSystem() = com.example.durtlesreborn.session.SrsSystem(
    firstStartedIndex = startingPosition,
    passedIndex = passingPosition,
    completedIndex = burningPosition,
    intervalsMillis = apiJson.decodeFromString(ListSerializer(Long.serializer()), intervalsMillisJson),
)
