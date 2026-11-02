// Based on Tsurukame (ios/SubjectModelItem.swift),
// Copyright David Sansome, Apache License 2.0: see assets/TSURUKAME-LICENSE.txt.
// Rewritten in Kotlin and changed for this app.
package com.example.durtlesreborn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.durtlesreborn.data.SearchCriteria
import com.example.durtlesreborn.data.StageGroup
import com.example.durtlesreborn.session.SubjectKind

/** A search bar with Cancel, filters below it, and the results as subject rows. */
@Composable
fun SearchScreen(
    state: UiState.Search,
    onCriteria: (SearchCriteria) -> Unit,
    onOpen: (Long) -> Unit,
    onSelfStudy: () -> Unit,
    onBack: () -> Unit,
) {
    val tk = LocalTk.current
    val c = state.criteria
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(Modifier.fillMaxSize().background(tk.grouped).navigationBarsPadding().imePadding()) {
        Column(Modifier.fillMaxWidth().background(tk.bar).statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = c.text,
                    onValueChange = { onCriteria(c.copy(text = it)) },
                    singleLine = true,
                    textStyle = TextStyle(color = tk.label, fontSize = 17.sp),
                    cursorBrush = SolidColor(TkFixed.tint),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.weight(1f).focusRequester(focus),
                    decorationBox = { inner ->
                        Box(
                            Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(10.dp)).background(if (tk.isDark) Color(0xFF2C2C2E) else Color(0xFFE3E3E8)).padding(horizontal = 10.dp),
                            Alignment.CenterStart,
                        ) {
                            if (c.text.isEmpty()) TkText("Search", color = tk.placeholder, maxLines = 1)
                            inner()
                        }
                    },
                )
                TkBarButton("Cancel", onClick = onBack)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((kind, label) in listOf(SubjectKind.RADICAL to "Radicals", SubjectKind.KANJI to "Kanji", SubjectKind.VOCABULARY to "Vocabulary")) {
                    val selected = kind in c.kinds
                    SearchPill(label, selected, Modifier.weight(1f)) {
                        // Vocabulary stands for kana-only vocabulary too.
                        val group = if (kind == SubjectKind.VOCABULARY) setOf(kind, SubjectKind.KANA_VOCABULARY) else setOf(kind)
                        onCriteria(c.copy(kinds = if (selected) c.kinds - group else c.kinds + group))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StageMenu(c.stage, Modifier.weight(1.4f)) { onCriteria(c.copy(stage = it)) }
                LevelMenu("From", c.minLevel, Modifier.weight(1f)) { onCriteria(c.copy(minLevel = it, maxLevel = maxOf(it, c.maxLevel))) }
                LevelMenu("To", c.maxLevel, Modifier.weight(1f)) { onCriteria(c.copy(maxLevel = it, minLevel = minOf(it, c.minLevel))) }
            }
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(tk.separator))
        }
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TkText(
                if (state.searching) "Searching\u2026" else "${state.results.size}${if (state.results.size >= SEARCH_LIMIT) "+" else ""} results",
                size = 13.sp,
                color = tk.grey33,
                modifier = Modifier.weight(1f),
            )
            // Self-study quizzes whatever the search matches; results are not sent to WaniKani.
            TkBarButton("Quiz these", enabled = state.results.isNotEmpty() && !state.searching, onClick = onSelfStudy)
        }
        // A lazy list stays on the item it was showing, which would hide better matches above it.
        val listState = rememberLazyListState()
        LaunchedEffect(state.results) { listState.scrollToItem(0) }
        LazyColumn(Modifier.fillMaxSize(), state = listState, verticalArrangement = Arrangement.spacedBy(0.5.dp)) {
            items(state.results, key = { it.id }) { r ->
                SubjectRow(r.title, r.kind, r.reading, r.meaning, level = r.level, onClick = { onOpen(r.id) })
            }
        }
    }
}

const val SEARCH_LIMIT = 200

@Composable
private fun SearchPill(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tk = LocalTk.current
    Box(
        modifier.height(30.dp).clip(RoundedCornerShape(8.dp))
            .background(if (selected) TkFixed.tint else if (tk.isDark) Color(0xFF2C2C2E) else Color(0xFFE3E3E8))
            .clickable(onClick = onClick),
        Alignment.Center,
    ) {
        TkText(label, size = 13.sp, color = if (selected) Color.White else tk.label, weight = if (selected) FontWeight.SemiBold else null, maxLines = 1)
    }
}

@Composable
private fun StageMenu(selected: StageGroup?, modifier: Modifier, onPick: (StageGroup?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        SearchPill(selected?.label ?: "Any stage", selected != null, Modifier.fillMaxWidth()) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Any stage") }, onClick = { open = false; onPick(null) })
            for (g in StageGroup.entries) DropdownMenuItem(text = { Text(g.label) }, onClick = { open = false; onPick(g) })
        }
    }
}

@Composable
private fun LevelMenu(label: String, value: Int, modifier: Modifier, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        SearchPill("$label $value", false, Modifier.fillMaxWidth()) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (level in 1..60) DropdownMenuItem(text = { Text("Level $level") }, onClick = { open = false; onPick(level) })
        }
    }
}

/** A subject on its own: its gradient header, its details and its stats. */
@Composable
fun ItemDetailScreen(
    state: UiState.Detail,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onPlayAudio: (Long) -> Unit,
) {
    val tk = LocalTk.current
    val d = state.detail
    Column(Modifier.fillMaxSize().background(tk.grouped)) {
        SubjectHeader(d.info, onBack, onPlayAudio)
        SubjectDetailsTable(
            d.info,
            Modifier.weight(1f),
            onOpen = onOpen,
            onPlayAudio = onPlayAudio,
            footer = {
                TkSection("Stats", headerHeight = 38.dp) {
                    TkRow("WaniKani Level", value = "${d.info.level}")
                    TkRow("SRS Stage", value = d.stage.label)
                    d.percentageCorrect?.let { TkRow("Answered correctly", value = "$it%") }
                }
            },
        )
    }
}
