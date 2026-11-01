// Based on Tsurukame (ios/SubjectsRemainingViewController.swift, SubjectModelItem.swift),
// Copyright David Sansome, Apache License 2.0: see assets/TSURUKAME-LICENSE.txt.
// Rewritten in Kotlin and changed for this app.
package com.example.durtlesreborn.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.durtlesreborn.data.LevelContent
import com.example.durtlesreborn.data.LevelTile
import kotlinx.coroutines.launch

/** Items at or past Guru have nothing left to do for the level. */
internal fun List<LevelTile>.remaining() = filter { (it.stage ?: 0) < 5 }

/**
 * The subject catalogue, one page per level: radicals, kanji and vocabulary as subject rows, locked
 * items in grey. "Show remaining" opens it with only the items not yet at Guru.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LevelsScreen(state: UiState.Levels, onVisible: (Int) -> Unit, onOpenItem: (Long) -> Unit, onBack: () -> Unit) {
    val tk = LocalTk.current
    val pager = rememberPagerState(initialPage = (state.selectedLevel - 1).coerceIn(0, state.maxLevel - 1)) { state.maxLevel }
    val scope = rememberCoroutineScope()
    // Load the visible level and its neighbours, so a swipe does not wait.
    LaunchedEffect(pager.currentPage) {
        val level = pager.currentPage + 1
        onVisible(level)
        if (level > 1) onVisible(level - 1)
        if (level < state.maxLevel) onVisible(level + 1)
    }
    Column(Modifier.fillMaxSize().background(tk.grouped)) {
        TkNavBar(if (state.remainingOnly) "Remaining \u00B7 Level ${pager.currentPage + 1}" else "Level ${pager.currentPage + 1}", onBack = onBack)
        Row(
            Modifier.fillMaxWidth().background(tk.bar).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            for (level in 1..state.maxLevel) {
                val selected = pager.currentPage == level - 1
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(if (selected) TkFixed.tint else Color.Transparent)
                        .clickable { scope.launch { pager.animateScrollToPage(level - 1) } },
                    Alignment.Center,
                ) {
                    TkText("$level", size = 14.sp, color = if (selected) Color.White else tk.label, weight = if (selected) FontWeight.SemiBold else null)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(tk.separator))
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            val content = state.contents[page + 1]
            if (content == null) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            } else {
                LevelPage(content, state.remainingOnly, onOpenItem)
            }
        }
    }
}

@Composable
private fun LevelPage(content: LevelContent, remainingOnly: Boolean, onOpenItem: (Long) -> Unit) {
    fun shown(tiles: List<LevelTile>) = if (remainingOnly) tiles.remaining() else tiles
    val sections = listOf("Radicals" to shown(content.radicals), "Kanji" to shown(content.kanji), "Vocabulary" to shown(content.vocabulary))
    LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(0.5.dp)) {
        for ((title, tiles) in sections) {
            if (tiles.isEmpty()) continue
            item(key = title) { TkSectionHeader(title) }
            items(tiles, key = { it.id }) { tile ->
                SubjectRow(tile.title, tile.kind, tile.reading, tile.meaning, locked = !tile.unlocked, onClick = { onOpenItem(tile.id) })
            }
        }
        if (sections.all { it.second.isEmpty() }) {
            item { Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) { TkText("Nothing left at this level.", color = LocalTk.current.grey33) } }
        }
    }
}
