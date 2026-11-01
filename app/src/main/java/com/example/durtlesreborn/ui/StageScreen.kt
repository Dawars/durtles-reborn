package com.example.durtlesreborn.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * Every item in one SRS group, reached from the stage tiles on the home screen. Filter by kind,
 * tap an item to open it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StageScreen(state: UiState.StageItems, onKind: (SubjectKind?) -> Unit, onOpen: (Long) -> Unit, onBack: () -> Unit) {
    val items = state.items
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Back") }
            Text(
                if (items == null) state.group.label else "${state.group.label} · ${items.size}",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        if (items == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        // Only kinds that have items in this group get a chip.
        val kinds = remember(items) { SubjectKind.entries.filter { kind -> items.any { it.kind == kind } } }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.kind == null, onClick = { onKind(null) }, label = { Text("All ${items.size}") })
            for (kind in kinds) {
                FilterChip(
                    selected = state.kind == kind,
                    onClick = { onKind(kind) },
                    label = { Text("${kind.pluralLabel()} ${items.count { it.kind == kind }}") },
                )
            }
        }
        val shown = remember(items, state.kind) { items.ofKind(state.kind) }
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Nothing in this group yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(shown, key = { it.id }) { item -> StageRow(item, onOpen) }
            }
        }
    }
}

@Composable
private fun StageRow(item: SearchResult, onOpen: (Long) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onOpen(item.id) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            item.title,
            fontSize = if (item.title.length > 3) 18.sp else 28.sp,
            color = subjectColor(item.kind),
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.width(88.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(item.meaning.ifEmpty { "—" }, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(item.reading, "Level ${item.level}").joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
