package com.example.durtlesreborn.data

import com.example.durtlesreborn.answer.SubjectAnswers
import com.example.durtlesreborn.session.SessionSubject
import com.example.durtlesreborn.session.SrsSystem
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/** What the home screen shows. */
data class HomeCounts(
    val reviewsDue: Int,
    val lessonsAvailable: Int,
    /** When the next review not yet due becomes available, epoch milliseconds. */
    val nextReviewAt: Long?,
)

/** A pronunciation clip and the level its subject belongs to. */
data class AudioClip(val url: String, val level: Int)

/**
 * Reads the local database and prepares what sessions and the quiz need. Only subjects up to the
 * user's `max_level_granted` are offered, so an unsubscribed account never sees locked levels.
 */
class SessionRepository(
    private val subjects: SubjectDao,
    private val assignments: AssignmentDao,
    private val materials: StudyMaterialDao,
    private val srsSystems: SrsSystemDao,
    private val state: SyncStateDao,
    private val clock: () -> Long = System::currentTimeMillis,
    /** Share of an interval that must have passed after due time for a review to count as overdue. */
    private val overdueThreshold: Float = DEFAULT_OVERDUE_THRESHOLD,
    /** Pitch accent lookup; null in tests that do not exercise it. */
    private val pitch: PitchRepository? = null,
) {
    suspend fun maxLevel(): Int = state.get(SyncEngine.KEY_MAX_LEVEL)?.toIntOrNull() ?: 3

    suspend fun userLevel(): Int = state.get(SyncEngine.KEY_LEVEL)?.toIntOrNull() ?: 1

    /**
     * Subjects that count towards the next level: the current level's kanji and their radicals. Empty
     * once the user is at the highest level their subscription allows, as in the original.
     */
    suspend fun levelUpIds(): Set<Long> {
        val level = userLevel()
        if (level >= maxLevel()) return emptySet()
        return subjects.kanjiForLevel(level).flatMap { listOf(it.id) + SubjectContent.parse(it.json).componentSubjectIds }.toSet()
    }

    suspend fun dueReviews(): List<SessionSubject> = toSessionSubjects(assignments.reviewsDue(clock()))

    suspend fun availableLessons(): List<SessionSubject> = toSessionSubjects(assignments.lessonsAvailable())

    suspend fun counts(): HomeCounts {
        val due = dueReviews().size
        val lessons = availableLessons().size
        return HomeCounts(due, lessons, nextReviewAt())
    }

    /** Earliest availability among reviews that are not due yet. */
    private suspend fun nextReviewAt(): Long? {
        val now = clock()
        return assignments.upcoming(now).firstOrNull()?.availableAt
    }

    /** The answers for a subject, with the user's meaning synonyms included. */
    suspend fun answersFor(subjectId: Long): SubjectAnswers? {
        val subject = subjects.byId(subjectId) ?: return null
        val synonyms = materials.bySubject(subjectId)?.meaningSynonymsJson
            ?.let { apiJsonList(it) } ?: emptyList()
        return SubjectContent.parse(subject.json).answers(synonyms)
    }

    /** What to show on the lesson card for a subject. */
    suspend fun lessonInfoFor(subjectId: Long): LessonInfo? {
        val subject = subjects.byId(subjectId) ?: return null
        val content = SubjectContent.parse(subject.json)
        val info = content.lessonInfo(subject.characters ?: subject.slug ?: "?", subject.type, subject.level, subject.id)
            .copy(related = subjects.relatedGroups(subject.type, content, maxLevel()))
        val accents = pitch?.lookup(info.title, info.readings.map { it.text }).orEmpty()
        return info.copy(pitch = accents)
    }

    /** The first pronunciation clip WaniKani hosts for the item, with the level it belongs to. */
    suspend fun audioFor(subjectId: Long): AudioClip? {
        val subject = subjects.byId(subjectId) ?: return null
        val url = SubjectContent.parse(subject.json).pronunciationAudios.firstOrNull() ?: return null
        return AudioClip(url, subject.level)
    }

    /** Every pronunciation clip of one level's vocabulary. */
    suspend fun audioUrlsForLevel(level: Int): List<String> =
        subjects.vocabularyJsonForLevel(level)
            .flatMap { SubjectContent.parse(it).pronunciationAudios }
            .distinct()

    private fun apiJsonList(json: String): List<String> =
        com.example.durtlesreborn.api.apiJson.decodeFromString(ListSerializer(String.serializer()), json)

    private suspend fun toSessionSubjects(candidates: List<AssignmentEntity>): List<SessionSubject> {
        if (candidates.isEmpty()) return emptyList()
        val maxLevel = maxLevel()
        val byId = candidates.map { it.subjectId }.chunked(500).flatMap { subjects.byIds(it) }.associateBy { it.id }
        val systems = srsSystems.all().associate { it.id to it.toSrsSystem() }
        val fallback = systems.values.firstOrNull() ?: SrsSystem(1, 5, 9)
        val now = clock()

        return candidates.mapNotNull { assignment ->
            val subject = byId[assignment.subjectId] ?: return@mapNotNull null
            if (subject.hidden || subject.level > maxLevel) return@mapNotNull null
            sessionSubject(subject, assignment, systems, fallback, now)
        }
    }

    /**
     * Subjects for a self-study quiz: any visible subject up to the granted level, whether or not
     * the user has started it. Without an assignment the subject counts as stage 0.
     */
    suspend fun selfStudySubjects(subjectIds: List<Long>): List<SessionSubject> {
        if (subjectIds.isEmpty()) return emptyList()
        val maxLevel = maxLevel()
        val found = subjectIds.chunked(500).flatMap { subjects.byIds(it) }
        val assignmentsBySubject = subjectIds.chunked(500).flatMap { assignments.bySubjects(it) }.associateBy { it.subjectId }
        val systems = srsSystems.all().associate { it.id to it.toSrsSystem() }
        val fallback = systems.values.firstOrNull() ?: SrsSystem(1, 5, 9)
        val now = clock()
        return found.mapNotNull { subject ->
            if (subject.hidden || subject.level > maxLevel) return@mapNotNull null
            sessionSubject(subject, assignmentsBySubject[subject.id], systems, fallback, now)
        }
    }

    private fun sessionSubject(
        subject: SubjectEntity,
        assignment: AssignmentEntity?,
        systems: Map<Long, SrsSystem>,
        fallback: SrsSystem,
        now: Long,
    ): SessionSubject? {
        val kind = subjectKindOf(subject.type) ?: return null
        val content = SubjectContent.parse(subject.json)
        val srs = content.srsSystemId?.let { systems[it] } ?: fallback
        return SessionSubject(
            id = subject.id,
            assignmentId = assignment?.id ?: 0,
            kind = kind,
            level = subject.level,
            srsStageIndex = assignment?.srsStage ?: 0,
            srs = srs,
            // Image-only radicals have no characters; their slug (e.g. "beggar") stands in until images are shown.
            characters = subject.characters ?: subject.slug,
            overdue = assignment?.let { isOverdue(it, srs, now) } ?: false,
            dueAt = assignment?.availableAt ?: 0,
            hasAcceptedMeanings = content.hasAcceptedMeanings,
            hasAcceptedReadings = content.hasAcceptedReadings,
            hasOnYomi = content.hasOnYomi,
            hasKunYomi = content.hasKunYomi,
        )
    }

    /** Past due by at least [overdueThreshold] of the stage's interval. Initial stage counts as overdue. */
    internal fun isOverdue(assignment: AssignmentEntity, srs: SrsSystem, now: Long): Boolean {
        val availableAt = assignment.availableAt ?: return false
        if (assignment.unlockedAt == null) return false
        if (assignment.srsStage == 0) return true
        if (assignment.srsStage >= srs.completedIndex) return false
        val since = (now - availableAt).toDouble()
        if (since <= 0) return false
        val interval = srs.intervalMillis(assignment.srsStage).toDouble()
        if (interval <= 0) return false
        return since / interval >= overdueThreshold
    }

    companion object {
        const val DEFAULT_OVERDUE_THRESHOLD = 0.20f
    }
}
