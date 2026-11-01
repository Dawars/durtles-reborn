// Based on Tsurukame (ios/LessonsViewController.swift, LessonsPageControl.swift),
// Copyright David Sansome, Apache License 2.0: see assets/TSURUKAME-LICENSE.txt.
// Rewritten in Kotlin and changed for this app.
package com.example.durtlesreborn.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.durtlesreborn.data.LessonInfo
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Lesson cards in pages: the subject on its gradient with its details below, and a row of chips at
 * the bottom with one chip per subject and a Quiz chip. Swiping past the last card starts the quiz.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun LessonScreen(
    state: UiState.Lesson,
    onGoTo: (Int) -> Unit,
    onCancel: () -> Unit,
    loadInfo: suspend (Long) -> LessonInfo?,
    onPlayAudio: (Long) -> Unit,
) {
    val tk = LocalTk.current
    var openId by remember { mutableStateOf<Long?>(null) }
    BackHandler(enabled = openId == null, onBack = onCancel)
    val pageCount = state.total + 1
    val pager = rememberPagerState(initialPage = state.index) { pageCount }
    val scope = rememberCoroutineScope()
    val currentIndex by androidx.compose.runtime.rememberUpdatedState(state.index)
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collectLatest { page -> if (page != currentIndex) onGoTo(page) }
    }
    Box(Modifier.fillMaxSize().background(tk.grouped)) {
        Column(Modifier.fillMaxSize()) {
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
                val chip = state.chips.getOrNull(page)
                if (chip != null) {
                    val info by produceState(if (page == state.index) state.info else null, chip.id) {
                        if (value == null) value = loadInfo(chip.id)
                    }
                    val loaded = info
                    if (loaded == null) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            SubjectHeader(loaded, onCancel, onPlayAudio)
                            SubjectDetailsTable(loaded, Modifier.weight(1f), onOpen = { openId = it }, onPlayAudio = onPlayAudio)
                        }
                    }
                } else {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                }
            }
            FlowRow(
                Modifier.fillMaxWidth().background(tk.cell).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                state.chips.forEachIndexed { index, chip ->
                    SubjectChipBox(chip.title, tk.gradient(chip.kind), dimmed = index != pager.currentPage) {
                        scope.launch { pager.animateScrollToPage(index) }
                    }
                }
                SubjectChipBox("Quiz", listOf(tk.grey80, tk.grey80), japanese = false) {
                    scope.launch { pager.animateScrollToPage(state.total) }
                }
            }
        }
        openId?.let { SubjectOverlay(it, loadInfo, onDismiss = { openId = null }, onPlayAudio = onPlayAudio) }
    }
}
