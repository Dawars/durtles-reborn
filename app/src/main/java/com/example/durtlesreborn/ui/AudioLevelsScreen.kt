package com.example.durtlesreborn.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Space used by each level's pronunciation clips, newest level first. Deleting a level frees its
 * folder; the automatic download only fetches levels it has not completed, so a deleted level stays
 * deleted until Download is tapped for it here.
 */
@Composable
fun AudioLevelsScreen(
    state: UiState.AudioLevels,
    download: AudioDownload,
    onBack: () -> Unit,
    onDelete: (Int) -> Unit,
    onDownloadLevel: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    val clips = state.levels.sumOf { it.clips }
    val bytes = state.levels.sumOf { it.bytes }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Audio downloads", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            if (clips == 0) "No clips on this phone yet." else "$clips clips on this phone (${formatSize(bytes)}).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (download.running) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (download.total > 0) "Downloading level ${download.level}: ${download.done} of ${download.total} clips"
                    else "Preparing level ${download.level}…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { if (download.total == 0) 0f else download.done.toFloat() / download.total },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (download.failed > 0) {
                    Text(
                        "${download.failed} could not be downloaded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                TextButton(onClick = onCancel) { Text("Cancel download") }
            }
        }
        Text(
            "Levels you have reached download by themselves. Deleting a level frees its space and it is not fetched again.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            for (item in state.levels) LevelAudioRow(item, state.currentLevel, download, onDelete, onDownloadLevel)
        }
    }
}

@Composable
private fun LevelAudioRow(
    item: LevelAudio,
    currentLevel: Int,
    download: AudioDownload,
    onDelete: (Int) -> Unit,
    onDownloadLevel: (Int) -> Unit,
) {
    val downloading = download.running && download.level == item.level
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                if (item.level == currentLevel) "Level ${item.level} · current" else "Level ${item.level}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                when {
                    downloading && download.total > 0 -> "Downloading ${download.done} of ${download.total} clips"
                    downloading -> "Downloading…"
                    item.clips > 0 -> "${item.clips} clips · ${formatSize(item.bytes)}"
                    else -> "Not downloaded"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            downloading -> Text("…", style = MaterialTheme.typography.bodyLarge)
            item.clips > 0 -> TextButton(onClick = { onDelete(item.level) }) { Text("Delete") }
            else -> TextButton(onClick = { onDownloadLevel(item.level) }) { Text("Download") }
        }
    }
}
