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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.durtlesreborn.data.LevelContent
import com.example.durtlesreborn.data.LevelTile
import kotlinx.coroutines.launch

/** Browse every level up to the one the subscription allows: swipe or tap a level, tap an item for its page. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LevelsScreen(state: UiState.Levels, onVisible: (Int) -> Unit, onOpenItem: (Long) -> Unit, onBack: () -> Unit) {
    val pager = rememberPagerState(initialPage = (state.selectedLevel - 1).coerceIn(0, state.maxLevel - 1)) { state.maxLevel }
    val scope = rememberCoroutineScope()
    // Load the visible level and its neighbours, so a swipe does not wait.
    LaunchedEffect(pager.currentPage) {
        val level = pager.currentPage + 1
        onVisible(level)
        if (level > 1) onVisible(level - 1)
        if (level < state.maxLevel) onVisible(level + 1)
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(AppIcons.ArrowBack, "Back") }
            Text("Level", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
        }
        ScrollableTabRow(selectedTabIndex = pager.currentPage, edgePadding = 8.dp, containerColor = Color.Transparent) {
            for (level in 1..state.maxLevel) {
                Tab(
                    selected = pager.currentPage == level - 1,
                    onClick = { scope.launch { pager.animateScrollToPage(level - 1) } },
                    text = { Text(level.toString(), fontSize = if (pager.currentPage == level - 1) 22.sp else 16.sp) },
                )
            }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            val content = state.contents[page + 1]
            if (content == null) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            } else {
                LevelPage(content, onOpenItem)
            }
        }
    }
}

@Composable
private fun LevelPage(content: LevelContent, onOpenItem: (Long) -> Unit) {
    val palette = LocalSubjectColors.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        section("Radical", content.radicals, palette.radical, onOpenItem)
        section("Kanji", content.kanji, palette.kanji, onOpenItem)
        section("Vocabulary", content.vocabulary, palette.vocabulary, onOpenItem)
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.section(
    title: String,
    tiles: List<LevelTile>,
    color: Color,
    onOpenItem: (Long) -> Unit,
) {
    if (tiles.isEmpty()) return
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(
            "$title (${tiles.size})", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        )
    }
    items(tiles, key = { it.id }) { tile -> BigTile(tile, color, onOpenItem) }
}

@Composable
private fun BigTile(tile: LevelTile, color: Color, onOpen: (Long) -> Unit) {
    val bg = if (tile.unlocked) color else LocalActionColors.current.locked
    val fg = contentColorOn(bg)
    Column(
        Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(8.dp)).background(bg)
            .clickable { onOpen(tile.id) }
            .padding(4.dp)
            .semantics { contentDescription = "${tile.title}, ${tile.reading ?: ""} ${tile.meaning}" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(tile.title, color = fg, maxLines = 1, softWrap = false, fontSize = if (tile.title.length <= 2) 28.sp else if (tile.title.length <= 4) 20.sp else 14.sp)
        tile.reading?.let { Text(it, color = fg, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        Text(tile.meaning, color = fg, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
