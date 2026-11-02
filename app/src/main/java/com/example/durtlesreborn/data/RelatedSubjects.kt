package com.example.durtlesreborn.data

import com.example.durtlesreborn.session.SubjectKind

/** A subject linked from another one, shown as a small tappable chip. */
data class RelatedSubject(val id: Long, val title: String, val subtitle: String, val kind: SubjectKind?, val level: Int? = null)

/** [more] counts linked subjects left out to keep the list short. */
data class RelatedGroup(val label: String, val items: List<RelatedSubject>, val more: Int = 0)

private const val USED_IN_LIMIT = 30

/** The groups of linked subjects to show for a subject of [type], limited to the levels the user may see. */
internal suspend fun SubjectDao.relatedGroups(type: String, content: SubjectContent, maxLevel: Int): List<RelatedGroup> {
    suspend fun group(label: String, ids: List<Long>, cap: Int = Int.MAX_VALUE): RelatedGroup? {
        if (ids.isEmpty()) return null
        val byId = ids.chunked(500).flatMap { byIds(it) }.filter { !it.hidden && it.level <= maxLevel }.associateBy { it.id }
        val visible = ids.mapNotNull { byId[it] }
        if (visible.isEmpty()) return null
        return RelatedGroup(label, visible.take(cap).map(::relatedSubject), more = (visible.size - cap).coerceAtLeast(0))
    }
    return when (type) {
        "radical" -> listOfNotNull(group("Used in kanji", content.amalgamationSubjectIds, USED_IN_LIMIT))
        "kanji" -> listOfNotNull(
            group("Radicals", content.componentSubjectIds),
            group("Visually similar kanji", content.visuallySimilarSubjectIds),
            group("Used in vocabulary", content.amalgamationSubjectIds, USED_IN_LIMIT),
        )
        "vocabulary" -> listOfNotNull(group("Kanji", content.componentSubjectIds))
        else -> emptyList()
    }
}

private fun relatedSubject(subject: SubjectEntity): RelatedSubject {
    val content = SubjectContent.parse(subject.json)
    val meaning = content.meanings.firstOrNull { it.acceptedAnswer }?.text
    val reading = content.readings.firstOrNull { it.acceptedAnswer }?.reading
    return RelatedSubject(
        id = subject.id,
        title = subject.characters ?: subject.slug ?: "?",
        subtitle = listOfNotNull(reading, meaning).joinToString(" · "),
        kind = subjectKindOf(subject.type),
        level = subject.level,
    )
}
