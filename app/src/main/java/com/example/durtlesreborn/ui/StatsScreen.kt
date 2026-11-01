package com.example.durtlesreborn.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.durtlesreborn.data.BreakdownRow
import com.example.durtlesreborn.data.ForecastBucket
import com.example.durtlesreborn.data.LevelProgress
import com.example.durtlesreborn.data.Stats
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun StatsScreen(stats: Stats?, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Stats", style = MaterialTheme.typography.titleLarge)
        }
        if (stats == null) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
            }
            return@Column
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            stats.level?.let { LevelSection(it) }
            ForecastSection(stats)
            BreakdownSection(stats.breakdown)
        }
    }
}

@Composable
private fun LevelSection(level: LevelProgress) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionTitle("Level ${level.level}")
        LinearProgressIndicator(
            progress = { level.fraction },
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = "${level.passedKanji} of ${level.neededKanji} kanji passed to level up"
            },
        )
        Text(
            when {
                level.totalKanji == 0 -> "No kanji found for this level yet. Sync to load them."
                level.canLevelUp -> "${level.passedKanji} of ${level.totalKanji} kanji passed. Enough to level up."
                else -> "${level.passedKanji} of ${level.totalKanji} kanji passed. ${level.remainingKanji} more to level up."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ForecastSection(stats: Stats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Reviews coming up")
        Text("${stats.reviewsDueNow} due now", style = MaterialTheme.typography.bodyMedium)
        val hourTotal = stats.hourly.sumOf { it.count }
        Text("Next 24 hours: $hourTotal", style = MaterialTheme.typography.labelLarge)
        BarChart(stats.hourly, "Reviews per hour over the next 24 hours")
        Text(
            hourLabels(stats.hourly),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Next 7 days", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
        BarChart(stats.daily, "Reviews per day over the next 7 days")
        Text(
            dayLabels(stats.daily),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BarChart(buckets: List<ForecastBucket>, description: String) {
    val bar = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val max = (buckets.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    Canvas(
        Modifier.fillMaxWidth().height(96.dp).semantics {
            contentDescription = description + ": " + buckets.joinToString(", ") { it.count.toString() }
        },
    ) {
        val gap = 3.dp.toPx()
        val width = (size.width - gap * (buckets.size - 1)) / buckets.size
        buckets.forEachIndexed { i, b ->
            val x = i * (width + gap)
            // Empty periods still show a thin stub, so the chart reads as a row of slots.
            val h = if (b.count == 0) 2.dp.toPx() else (size.height * b.count / max).coerceAtLeast(4.dp.toPx())
            drawRoundRect(
                color = if (b.count == 0) empty else bar,
                topLeft = Offset(x, size.height - h),
                size = Size(width, h),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
    }
}

private fun hourLabels(buckets: List<ForecastBucket>): String {
    if (buckets.isEmpty()) return ""
    val fmt = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault())
    return "From ${fmt.format(Instant.ofEpochMilli(buckets.first().startMillis))}, one bar per hour"
}

private fun dayLabels(buckets: List<ForecastBucket>): String {
    val fmt = DateTimeFormatter.ofPattern("EEE").withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault())
    return buckets.joinToString("  ") { "${fmt.format(Instant.ofEpochMilli(it.startMillis))} ${it.count}" }
}

@Composable
private fun BreakdownSection(rows: List<BreakdownRow>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle("Your items")
        Row(Modifier.fillMaxWidth()) {
            Cell("", 2f)
            Cell("Radicals", 1.3f, bold = true)
            Cell("Kanji", 1f, bold = true)
            Cell("Vocab", 1f, bold = true)
            Cell("All", 1f, bold = true)
        }
        HorizontalDivider()
        for (r in rows) {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Cell(r.group.label, 2f)
                Cell(r.radicals.toString(), 1.3f)
                Cell(r.kanji.toString(), 1f)
                Cell(r.vocabulary.toString(), 1f)
                Cell(r.total.toString(), 1f, bold = true)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Cell(text: String, weight: Float, bold: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier.weight(weight),
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}
