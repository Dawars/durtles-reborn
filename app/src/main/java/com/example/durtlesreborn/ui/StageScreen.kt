// Based on Tsurukame (ios/SRSStageCategoryItem.swift, SubjectModelItem.swift),
// Copyright David Sansome, Apache License 2.0: see assets/TSURUKAME-LICENSE.txt.
// Rewritten in Kotlin and changed for this app.
package com.example.durtlesreborn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.durtlesreborn.data.SearchResult
import com.example.durtlesreborn.session.SubjectKind

/** The results of [kind], or all of them when [kind] is null. */
fun List<SearchResult>.ofKind(kind: SubjectKind?): List<SearchResult> = if (kind == null) this else filter { it.kind == kind }

private fun SubjectKind.pluralLabel() = when (this) {
    SubjectKind.RADICAL -> "Radicals"
    SubjectKind.KANJI -> "Kanji"
    SubjectKind.VOCABULARY -> "Vocabulary"
    SubjectKind.KANA_VOCABULARY -> "Kana words"
}

/**
 * Every item in one SRS group, reached from the "All levels" rows on the home screen. Filter by
 * kind, tap an item to open it.
 */
@Composable
fun StageScreen(state: UiState.StageItems, onKind: (SubjectKind?) -> Unit, onOpen: (Long) -> Unit, onBack: () -> Unit) {
    val tk = LocalTk.current
    val items = state.items
    Column(Modifier.fillMaxSize().background(tk.grouped)) {
        TkNavBar(if (items == null) state.group.label else "${state.group.label} \u00B7 ${items.size}", onBack = onBack)
        if (items == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        // Only kinds that have items in this group get a choice.
        val kinds = remember(items) { SubjectKind.entries.filter { kind -> items.any { it.kind == kind } } }
        if (kinds.size > 1) {
            TkSegmented(
                listOf<Pair<SubjectKind?, String>>(null to "All") + kinds.map { it to it.pluralLabel() },
                state.kind,
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                onKind,
            )
        }
        val shown = remember(items, state.kind) { items.ofKind(state.kind) }
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { TkText("Nothing in this group yet.", color = tk.grey33) }
        } else {
            LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(0.5.dp)) {
                items(shown, key = { it.id }) { item ->
                    SubjectRow(item.title, item.kind, item.reading, item.meaning, level = item.level, onClick = { onOpen(item.id) })
                }
            }
        }
    }
}
