package com.example.durtlesreborn.data

import com.example.durtlesreborn.kana.Kana
import com.example.durtlesreborn.session.SubjectKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SRS stage groups by position. Both of WaniKani's SRS systems use the same positions for these
 * groups (1-4 apprentice, 5-6 guru, 7 master, 8 enlightened, 9 burned).
 */
enum class StageGroup(val label: String, val min: Int, val max: Int) {
    NOT_STARTED("Not started", 0, 0),
    APPRENTICE("Apprentice", 1, 4),
    GURU("Guru", 5, 6),
    MASTER("Master", 7, 7),
    ENLIGHTENED("Enlightened", 8, 8),
    BURNED("Burned", 9, 9),
    ;

    companion object {
        fun of(stage: Int?): StageGroup = entries.firstOrNull { (stage ?: 0) in it.min..it.max } ?: NOT_STARTED
    }
}

data class SearchCriteria(
    val text: String = "",
    /** Empty means every kind. */
    val kinds: Set<SubjectKind> = emptySet(),
    val minLevel: Int = 1,
    val maxLevel: Int = 60,
    val stage: StageGroup? = null,
)

data class SearchResult(
    val id: Long,
    val kind: SubjectKind,
    val level: Int,
    val title: String,
    val meaning: String,
    val reading: String?,
    val stage: StageGroup,
)

data class ItemDetail(
    val info: LessonInfo,
    val stage: StageGroup,
    val percentageCorrect: Int?,
)

/**
 * The text to look for, in the two spellings used: as typed (lower case, wildcard characters
 * removed) and, when it looks like romaji or katakana, as hiragana so readings can be found.
 */
internal fun queryVariants(text: String): Pair<String, String> {
    val q = text.trim().lowercase().replace("%", "").replace("_", "")
    if (q.isEmpty()) return "" to ""
    val kana = when {
        q.all { it in 'a'..'z' || it == '-' || it == ' ' } -> Kana.simulateInput(q.replace(" ", ""))
        else -> Kana.toHiragana(q)
    }
    // Only useful if the whole text became kana; "water" would end up as the meaningless "わてr".
    val partlyConverted = kana.any { it in 'a'..'z' }
    return q to (if (kana == q || partlyConverted) "" else kana)
}

class SearchRepository(
    private val search: SearchDao,
    private val subjects: SubjectDao,
    private val assignments: AssignmentDao,
    private val statistics: ReviewStatisticDao,
    private val state: SyncStateDao,
    /** Pitch accent lookup; null in tests that do not exercise it. */
    private val pitch: PitchRepository? = null,
) {
    suspend fun find(criteria: SearchCriteria, limit: Int = 200): List<SearchResult> {
        val granted = state.get(SyncEngine.KEY_MAX_LEVEL)?.toIntOrNull() ?: 3
        val (q1, q2) = queryVariants(criteria.text)
        val kinds = criteria.kinds.ifEmpty { SubjectKind.entries.toSet() }
        val types = kinds.flatMap { typeNames(it) }
        val stage = criteria.stage
        return search.search(
            q1 = q1, q2 = q2, types = types,
            minLevel = criteria.minLevel, maxLevel = criteria.maxLevel, maxGrantedLevel = granted,
            stageMin = stage?.min ?: -1, stageMax = stage?.max ?: -1, limit = limit,
        ).let { rows -> withContext(Dispatchers.Default) { rows.mapNotNull { row ->
            val kind = subjectKindOf(row.type) ?: return@mapNotNull null
            val content = SubjectContent.parse(row.json)
            SearchResult(
                id = row.id,
                kind = kind,
                level = row.level,
                title = row.characters ?: row.slug ?: "?",
                meaning = content.meaningsText,
                reading = content.readings.firstOrNull { it.acceptedAnswer }?.reading
                    ?: content.readings.firstOrNull()?.reading,
                stage = StageGroup.of(row.srsStage),
            )
        } } }
    }

    suspend fun detail(subjectId: Long): ItemDetail? {
        val subject = subjects.byId(subjectId) ?: return null
        val content = SubjectContent.parse(subject.json)
        val granted = state.get(SyncEngine.KEY_MAX_LEVEL)?.toIntOrNull() ?: 3
        val info = content.lessonInfo(subject.characters ?: subject.slug ?: "?", subject.type, subject.level, subject.id)
            .copy(related = subjects.relatedGroups(subject.type, content, granted))
        val accents = pitch?.lookup(info.title, info.readings.map { it.text }).orEmpty()
        return ItemDetail(
            info = info.copy(pitch = accents),
            stage = StageGroup.of(assignments.bySubject(subjectId)?.srsStage),
            percentageCorrect = statistics.bySubject(subjectId)?.percentageCorrect,
        )
    }

    private fun typeNames(kind: SubjectKind) = when (kind) {
        SubjectKind.RADICAL -> listOf("radical")
        SubjectKind.KANJI -> listOf("kanji")
        SubjectKind.VOCABULARY -> listOf("vocabulary")
        SubjectKind.KANA_VOCABULARY -> listOf("kana_vocabulary")
    }
}
