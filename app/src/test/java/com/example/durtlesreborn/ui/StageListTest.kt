package com.example.durtlesreborn.ui

import com.example.durtlesreborn.data.SearchResult
import com.example.durtlesreborn.data.StageGroup
import com.example.durtlesreborn.session.SubjectKind
import org.junit.Assert.assertEquals
import org.junit.Test

class StageListTest {
    private fun result(id: Long, kind: SubjectKind) = SearchResult(id, kind, 5, "字$id", "Meaning", null, StageGroup.GURU)

    private val all = listOf(
        result(1, SubjectKind.RADICAL), result(2, SubjectKind.KANJI), result(3, SubjectKind.KANJI), result(4, SubjectKind.VOCABULARY),
    )

    @Test
    fun noKindMeansEveryItem() {
        assertEquals(listOf(1L, 2L, 3L, 4L), all.ofKind(null).map { it.id })
    }

    @Test
    fun aKindKeepsOnlyItsItemsInOrder() {
        assertEquals(listOf(2L, 3L), all.ofKind(SubjectKind.KANJI).map { it.id })
        assertEquals(emptyList<Long>(), all.ofKind(SubjectKind.KANA_VOCABULARY).map { it.id })
    }
}
