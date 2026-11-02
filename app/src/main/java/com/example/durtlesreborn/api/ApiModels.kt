package com.example.durtlesreborn.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal val apiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** One object of the API: an id, a type tag and the type-specific [data]. */
@Serializable
data class Resource<T>(
    val id: Long = 0,
    @SerialName("object") val type: String,
    val url: String? = null,
    @SerialName("data_updated_at") val dataUpdatedAt: String? = null,
    val data: T,
)

@Serializable
data class Pages(
    @SerialName("next_url") val nextUrl: String? = null,
    @SerialName("previous_url") val previousUrl: String? = null,
    @SerialName("per_page") val perPage: Int = 0,
)

@Serializable
data class Collection<T>(
    val url: String? = null,
    val pages: Pages = Pages(),
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("data_updated_at") val dataUpdatedAt: String? = null,
    val data: List<Resource<T>> = emptyList(),
)

@Serializable
data class Subscription(
    val active: Boolean = false,
    val type: String? = null,
    /** Highest level the user may study: 3 on the free plan, 60 when subscribed. */
    @SerialName("max_level_granted") val maxLevelGranted: Int = 3,
    @SerialName("period_ends_at") val periodEndsAt: String? = null,
)

@Serializable
data class UserData(
    val username: String = "",
    val level: Int = 1,
    val subscription: Subscription = Subscription(),
)

/** Body of POST /reviews. */
@Serializable
data class ReviewRequest(val review: ReviewBody)

@Serializable
data class ReviewBody(
    /** The API accepts either the subject or the assignment id; the original sends the subject id. */
    @SerialName("subject_id") val subjectId: Long? = null,
    @SerialName("assignment_id") val assignmentId: Long? = null,
    @SerialName("incorrect_meaning_answers") val incorrectMeaningAnswers: Int,
    @SerialName("incorrect_reading_answers") val incorrectReadingAnswers: Int,
    /** Left out for recent reviews so the server's clock is used; set for reviews older than 5 minutes. */
    @SerialName("created_at") val createdAt: String? = null,
)

/** Body of PUT /assignments/{id}/start. */
@Serializable
data class StartAssignmentBody(@SerialName("started_at") val startedAt: String? = null)

@Serializable
data class AssignmentData(
    @SerialName("subject_id") val subjectId: Long,
    @SerialName("subject_type") val subjectType: String,
    @SerialName("srs_stage") val srsStage: Int = 0,
    @SerialName("unlocked_at") val unlockedAt: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("passed_at") val passedAt: String? = null,
    @SerialName("burned_at") val burnedAt: String? = null,
    @SerialName("available_at") val availableAt: String? = null,
    @SerialName("resurrected_at") val resurrectedAt: String? = null,
    val hidden: Boolean = false,
)

@Serializable
data class ReviewStatisticData(
    @SerialName("subject_id") val subjectId: Long,
    @SerialName("subject_type") val subjectType: String,
    @SerialName("meaning_correct") val meaningCorrect: Int = 0,
    @SerialName("meaning_incorrect") val meaningIncorrect: Int = 0,
    @SerialName("meaning_max_streak") val meaningMaxStreak: Int = 0,
    @SerialName("meaning_current_streak") val meaningCurrentStreak: Int = 0,
    @SerialName("reading_correct") val readingCorrect: Int = 0,
    @SerialName("reading_incorrect") val readingIncorrect: Int = 0,
    @SerialName("reading_max_streak") val readingMaxStreak: Int = 0,
    @SerialName("reading_current_streak") val readingCurrentStreak: Int = 0,
    @SerialName("percentage_correct") val percentageCorrect: Int = 0,
    val hidden: Boolean = false,
)

@Serializable
data class StudyMaterialData(
    @SerialName("subject_id") val subjectId: Long,
    @SerialName("meaning_note") val meaningNote: String? = null,
    @SerialName("reading_note") val readingNote: String? = null,
    @SerialName("meaning_synonyms") val meaningSynonyms: List<String> = emptyList(),
)

/** Body of POST and PUT /study_materials; the subject is only sent when creating one. */
@Serializable
data class StudyMaterialRequest(@SerialName("study_material") val studyMaterial: StudyMaterialBody)

@Serializable
data class StudyMaterialBody(
    @SerialName("subject_id") val subjectId: Long? = null,
    @SerialName("meaning_synonyms") val meaningSynonyms: List<String>,
)

/** Response to POST /reviews; the server returns the assignment as it is after the review. */
@Serializable
data class ReviewResponse(
    @SerialName("resources_updated") val resourcesUpdated: ResourcesUpdated? = null,
)

@Serializable
data class ResourcesUpdated(
    val assignment: Resource<AssignmentData>? = null,
    @SerialName("review_statistic") val reviewStatistic: Resource<ReviewStatisticData>? = null,
)

@Serializable
data class SrsStageData(
    /** Seconds until the next review, or null for the initial and burned stages. */
    val interval: Long? = null,
    val position: Int,
)

@Serializable
data class SrsSystemData(
    val name: String = "",
    @SerialName("starting_stage_position") val startingStagePosition: Int = 1,
    @SerialName("passing_stage_position") val passingStagePosition: Int = 5,
    @SerialName("burning_stage_position") val burningStagePosition: Int = 9,
    val stages: List<SrsStageData> = emptyList(),
)
