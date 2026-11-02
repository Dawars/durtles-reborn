// Based on Tsurukame (ios/MainViewController.swift, MainTitleView.swift, UpcomingReviewsChartItem.swift, CurrentLevelChartItem.swift),
// Copyright David Sansome, Apache License 2.0: see assets/TSURUKAME-LICENSE.txt.
// Rewritten in Kotlin and changed for this app.
package com.example.durtlesreborn.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.durtlesreborn.R
import com.example.durtlesreborn.data.BreakdownRow
import com.example.durtlesreborn.data.Dashboard
import com.example.durtlesreborn.data.DayForecast
import com.example.durtlesreborn.data.HeatMonth
import com.example.durtlesreborn.data.LevelEstimates
import com.example.durtlesreborn.data.LevelTile
import com.example.durtlesreborn.data.StageGroup
import com.example.durtlesreborn.data.heatLevel
import com.example.durtlesreborn.session.SessionType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class DashboardActions(
    val startReviews: () -> Unit,
    val startLessons: () -> Unit,
    val openSearch: () -> Unit,
    val openLevels: (Int?) -> Unit,
    val openRemaining: (Int?) -> Unit,
    val openSettings: () -> Unit,
    val openStats: () -> Unit,
    val openStage: (StageGroup) -> Unit,
    val refresh: () -> Unit,
    val dismissMessage: () -> Unit,
    val resumeSession: () -> Unit,
    val discardSession: () -> Unit,
)

/**
 * Tsurukame's home screen: the turtle engraving behind a grouped table, with the blue header above
 * it. Sections and rows follow its `MainWaniKaniTabViewController`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(state: UiState.Home, a: DashboardActions) {
    val tk = LocalTk.current
    val d = state.dashboard
    val now = remember(d) { System.currentTimeMillis() }
    Box(Modifier.fillMaxSize().background(tk.background)) {
        Image(painterResource(R.drawable.tk_launch_screen), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.25f)
        Column(Modifier.fillMaxSize()) {
            HomeHeader(d, state.syncing, a)
            PullToRefreshBox(isRefreshing = state.syncing, onRefresh = a.refresh, modifier = Modifier.weight(1f)) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
                    state.message?.let { message ->
                        TkSection { TkRow(message, subtitle = "Tap to dismiss", onClick = a.dismissMessage) }
                    }
                    if (state.dryRun) {
                        TkSection { TkRow("Dry run is on", subtitle = "Sessions are not saved or sent to WaniKani.", titleColor = TkFixed.red) }
                    }
                    if (d == null) {
                        FirstSync(state)
                    } else {
                        HomeTable(d, state.saved, now, a)
                    }
                }
            }
        }
    }
}

/** The gradient bar: avatar, name, level and learned kanji, then search and settings. */
@Composable
private fun HomeHeader(d: Dashboard?, syncing: Boolean, a: DashboardActions) {
    val tk = LocalTk.current
    val learned = d?.breakdown.orEmpty().filter { it.group >= StageGroup.GURU }.sumOf { it.kanji }
    Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(tk.headerGradient)).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(58.dp).padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(R.drawable.tk_avatar),
                null,
                Modifier.size(42.dp).shadow(4.dp, CircleShape).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    d?.username ?: "Urashima",
                    maxLines = 1,
                    style = TextStyle(color = Color.White, fontSize = 22.sp, lineHeight = 27.sp, shadow = Shadow(Color.Black.copy(alpha = 0.4f), Offset(0f, 2f), 8f)),
                )
                if (d?.level != null) {
                    Text(
                        "Level ${d.level} \u00B7 learned $learned kanji",
                        maxLines = 1,
                        style = TextStyle(color = Color.White, fontSize = 13.sp, lineHeight = 16.sp, shadow = Shadow(Color.Black.copy(alpha = 0.2f), Offset(0f, 2f), 4f)),
                    )
                }
            }
            HeaderButton(R.drawable.tk_search, "Search", a.openSearch)
            HeaderButton(R.drawable.tk_settings, "Settings", a.openSettings)
        }
        if (syncing) LinearProgressIndicator(Modifier.fillMaxWidth().height(4.dp), color = Color.White, trackColor = Color.White.copy(alpha = 0.3f))
    }
}

@Composable
private fun HeaderButton(icon: Int, description: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clickable(onClick = onClick).semantics { contentDescription = description }, Alignment.Center) {
        Image(painterResource(icon), null, Modifier.size(26.dp), colorFilter = ColorFilter.tint(Color.White))
    }
}

@Composable
private fun FirstSync(state: UiState.Home) {
    Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator()
        TkText(state.progress ?: if (state.syncing) "Connecting to WaniKani\u2026" else "Loading\u2026", size = 15.sp)
        if (state.syncing) TkText("The first sync downloads all your items and can take a few minutes.", size = 13.sp, color = LocalTk.current.grey33)
    }
}

@Composable
private fun HomeTable(d: Dashboard, saved: SavedSession?, now: Long, a: DashboardActions) {
    val tk = LocalTk.current
    TkSection("Currently available") {
        if (saved != null) ResumeRows(saved, a)
        CountRow("Lessons", d.counts.lessonsAvailable, a.startLessons)
        CountRow("Reviews", d.counts.reviewsDue, a.startReviews)
    }
    TkSection("Upcoming reviews") {
        UpcomingChart(d.days, d.counts.reviewsDue, now, Modifier.clickable(onClick = a.openStats))
        val level = d.level ?: 1
        val next = LevelEstimates.nextLevelUpReview(d.radicals, d.kanji, level, now)
        TkRow("Next level-up review", value = next?.let { LevelEstimates.format(it, now) } ?: "N/A")
    }
    d.previousLevel?.let { previous ->
        TkSection("Previous level (${previous.level})") {
            LevelPies(previous.radicals, previous.kanji, previous.vocabulary)
            TkRow("Show remaining", chevron = true, onClick = { a.openRemaining(previous.level) })
            TkRow("Show all", chevron = true, onClick = { a.openLevels(previous.level) })
        }
    }
    TkSection(if (d.previousLevel != null) "Current level (${d.level})" else "Current level") {
        LevelPies(d.radicals, d.kanji, d.vocabulary)
        val remaining = LevelEstimates.timeRemaining(d.radicals, d.kanji, d.level ?: 1, now)
        if (remaining != null) {
            TkRow(
                if (remaining.second) "Time remaining (estimated)" else "Time remaining",
                value = LevelEstimates.format(remaining.first, now),
            )
        }
        TkRow("Show remaining", chevron = true, onClick = { a.openRemaining(d.level) })
        TkRow("Show all", chevron = true, onClick = { a.openLevels(d.level) })
    }
    TkSection("All levels") {
        for (group in listOf(StageGroup.APPRENTICE, StageGroup.GURU, StageGroup.MASTER, StageGroup.ENLIGHTENED, StageGroup.BURNED)) {
            val count = d.breakdown.firstOrNull { it.group == group }?.total ?: 0
            TkRow(
                group.label,
                value = "$count",
                titleColor = tk.stage(group),
                chevron = count > 0,
                onClick = if (count > 0) ({ a.openStage(group) }) else null,
                leading = { StageIcon(group) },
            )
        }
        TkRow("Statistics", chevron = true, onClick = a.openStats)
    }
    if (d.activity.isNotEmpty()) {
        TkSection("Activity") { ActivityHeatmap(d.activity) }
    }
}

/** A count that opens a session, greyed out at zero. */
@Composable
private fun CountRow(title: String, count: Int, onClick: () -> Unit) {
    TkRow(title, value = "$count", chevron = true, enabled = count > 0, onClick = onClick)
}

/** Offers to carry on with a session that was left unfinished, or to throw it away. */
@Composable
private fun ResumeRows(saved: SavedSession, a: DashboardActions) {
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val what = if (saved.type == SessionType.LESSON) "lessons" else "reviews"
    TkRow("Resume $what", value = "${saved.doneItems}/${saved.totalItems}", chevron = true, titleColor = TkFixed.tint, onClick = a.resumeSession)
    TkRow("Discard saved session", titleColor = TkFixed.red, onClick = { confirmDiscard = true })
    if (confirmDiscard) {
        TkActionSheet(
            title = "Discard this session?",
            message = "The answers you gave in it are lost. The items stay due, and nothing is sent.",
            action = "Discard",
            onAction = {
                confirmDiscard = false
                a.discardSession()
            },
            onDismiss = { confirmDiscard = false },
        )
    }
}

private const val HOUR_MILLIS = 3_600_000L
private const val CHART_HOURS = 48

/** Reviews in each of the next [CHART_HOURS] hours, starting with the next full hour. */
internal fun upcomingHours(days: List<DayForecast>, now: Long): IntArray {
    val counts = IntArray(CHART_HOURS)
    val thisHour = now - now % HOUR_MILLIS
    for (day in days) {
        for (row in day.rows) {
            val index = ((row.startMillis - thisHour) / HOUR_MILLIS).toInt() - 1
            if (index in 0 until CHART_HOURS) counts[index] += row.count
        }
    }
    return counts
}

/** The largest axis value and the step between grid lines, in round numbers. */
private fun niceAxis(max: Int): Pair<Int, Int> {
    val target = max.coerceAtLeast(5)
    val step = listOf(1, 2, 5, 10, 20, 25, 50, 100, 200, 250, 500, 1000, 2000, 5000).firstOrNull { target / it <= 5 } ?: 10000
    return (ceil(target.toDouble() / step).toInt() * step) to step
}

/** Bars for the reviews in each hour on their own scale, a line for the running total on the axis' scale. */
@Composable
private fun UpcomingChart(days: List<DayForecast>, dueNow: Int, now: Long, modifier: Modifier = Modifier) {
    val tk = LocalTk.current
    val measurer = rememberTextMeasurer()
    val hours = remember(days, now) { upcomingHours(days, now) }
    val locale = Locale.getDefault()
    val formatter = remember(locale) { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "ha"), locale) }
    val axisStyle = TextStyle(color = tk.label, fontSize = 10.sp)
    val valueStyle = TextStyle(color = tk.label, fontSize = 8.sp)
    val lineColor = tk.vocabulary[1]
    val barColor = tk.radical[1]
    val gridColor = tk.grey80
    Canvas(modifier.fillMaxWidth().height(120.dp).padding(horizontal = 16.dp, vertical = 11.dp).semantics { contentDescription = "Upcoming reviews. Opens the statistics" }) {
        val cumulative = IntArray(CHART_HOURS + 1)
        cumulative[0] = dueNow
        for (i in hours.indices) cumulative[i + 1] = cumulative[i] + hours[i]
        val (axisMax, step) = niceAxis(cumulative.last())
        val barMax = (hours.maxOrNull() ?: 0).coerceAtLeast(1) * 1.15f
        val left = 28.dp.toPx()
        val bottomSpace = 15.dp.toPx()
        val chartWidth = size.width - left
        val chartHeight = size.height - bottomSpace
        val slot = chartWidth / (CHART_HOURS + 1)
        var tick = 0
        while (tick <= axisMax) {
            val y = chartHeight * (1f - tick.toFloat() / axisMax)
            drawLine(gridColor, Offset(left, y), Offset(size.width, y), 0.5.dp.toPx())
            drawLabel(measurer, "$tick", axisStyle, Offset(left - 4.dp.toPx(), y), alignEnd = true)
            tick += step
        }
        for (i in hours.indices) {
            if (hours[i] <= 0) continue
            val x = left + slot * (i + 1)
            val height = chartHeight * hours[i] / barMax
            drawRect(barColor, Offset(x + slot * 0.08f, chartHeight - height), Size(slot * 0.84f, height))
            drawLabel(measurer, "${hours[i]}", valueStyle, Offset(x + slot / 2, chartHeight - height - 6.dp.toPx()), centred = true)
        }
        var previous: Offset? = null
        for (i in cumulative.indices) {
            val point = Offset(left + slot * i + slot / 2, chartHeight * (1f - cumulative[i].toFloat() / axisMax))
            previous?.let { drawLine(lineColor, it, point, 1.dp.toPx()) }
            drawCircle(lineColor, 1.5.dp.toPx(), point)
            previous = point
        }
        val thisHour = now - now % HOUR_MILLIS
        for (i in 8 until CHART_HOURS + 1 step 8) {
            val time = Instant.ofEpochMilli(thisHour + i * HOUR_MILLIS).atZone(ZoneId.systemDefault())
            drawLabel(measurer, formatter.format(time), axisStyle, Offset(left + slot * i + slot / 2, size.height - 6.dp.toPx()), centred = true)
        }
    }
}

private fun DrawScope.drawLabel(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    at: Offset,
    alignEnd: Boolean = false,
    centred: Boolean = false,
) {
    val layout = measurer.measure(text, style)
    val x = when {
        alignEnd -> at.x - layout.size.width
        centred -> at.x - layout.size.width / 2f
        else -> at.x
    }
    drawText(layout, topLeft = Offset(x.coerceAtLeast(0f), at.y - layout.size.height / 2f))
}

/**
 * One level pie: locked items in light grey, lessons in darker grey, Apprentice in a paler shade of
 * the subject's colour and Guru or better in the full colour.
 */
@Composable
private fun LevelPies(radicals: List<LevelTile>, kanji: List<LevelTile>, vocabulary: List<LevelTile>) {
    val tk = LocalTk.current
    Row(Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 16.dp)) {
        LevelPie(radicals, tk.radical[1], Modifier.weight(1f))
        LevelPie(kanji, tk.kanji[1], Modifier.weight(1f))
        LevelPie(vocabulary, tk.vocabulary[1], Modifier.weight(1f))
    }
}

@Composable
private fun LevelPie(tiles: List<LevelTile>, base: Color, modifier: Modifier) {
    val tk = LocalTk.current
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = tk.label, fontSize = 10.sp)
    val slices = remember(tiles, base) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(base.toArgb(), hsv)
        val pale = Color.hsv(hsv[0], hsv[1] * 0.6f, hsv[2])
        listOf(
            tiles.count { !it.unlocked } to Color(0xFFCCCCCC),
            tiles.count { it.unlocked && (it.stage ?: 0) == 0 } to Color(0xFF999999),
            tiles.count { (it.stage ?: 0) in 1..4 } to pale,
            tiles.count { (it.stage ?: 0) >= 5 } to base,
        ).filter { it.first > 0 }
    }
    val total = slices.sumOf { it.first }
    val hole = tk.cell
    Canvas(modifier.fillMaxSize().padding(vertical = 8.dp)) {
        if (total == 0) return@Canvas
        val radius = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val topLeft = center - Offset(radius, radius)
        var angle = -90f
        for ((count, color) in slices) {
            val sweep = 360f * count / total
            drawArc(color, angle, sweep, useCenter = true, topLeft = topLeft, size = Size(radius * 2, radius * 2))
            angle += sweep
        }
        angle = -90f
        for ((count, _) in slices) {
            val edge = Math.toRadians(angle.toDouble())
            if (slices.size > 1) drawLine(hole, center, center + Offset(cos(edge).toFloat() * radius, sin(edge).toFloat() * radius), 1.dp.toPx())
            angle += 360f * count / total
        }
        drawCircle(hole.copy(alpha = 0.41f), radius * 0.55f, center)
        drawCircle(hole, radius * 0.2f, center)
        angle = -90f
        for ((count, _) in slices) {
            val sweep = 360f * count / total
            val middle = Math.toRadians((angle + sweep / 2).toDouble())
            drawLabel(measurer, "$count", labelStyle, center + Offset(cos(middle).toFloat() * radius * 0.6f, sin(middle).toFloat() * radius * 0.6f), centred = true)
            angle += sweep
        }
    }
}

@Composable
private fun ActivityHeatmap(months: List<HeatMonth>) {
    if (months.isEmpty()) return
    val max = months.flatMap { it.days }.mapNotNull { it?.count }.maxOrNull() ?: 0
    val empty = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    fun color(level: Int): Color = when (level) {
        0 -> empty
        1 -> accent.copy(alpha = 0.25f)
        2 -> accent.copy(alpha = 0.45f)
        3 -> accent.copy(alpha = 0.7f)
        else -> accent
    }
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Progress heatmap", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    "Last ${months.size} months",
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                )
            }
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val gap = 0.5.dp
                val labelWidth = 24.dp
                val cell = ((maxWidth - labelWidth - gap * 30) / 31).coerceIn(4.dp, 11.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(labelWidth))
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            for (day in 1..31) {
                                Box(Modifier.width(cell), contentAlignment = Alignment.Center) {
                                    Text("$day", fontSize = 7.sp, lineHeight = 7.sp, color = muted)
                                }
                            }
                        }
                    }
                    for (month in months) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                month.label,
                                fontSize = 10.sp,
                                lineHeight = 10.sp,
                                color = muted,
                                modifier = Modifier.width(labelWidth),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                for (day in month.days) {
                                    Box(
                                        Modifier.size(cell).clip(RoundedCornerShape(2.dp))
                                            .background(color(heatLevel(day?.count ?: 0, max))),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Less", style = MaterialTheme.typography.labelSmall, color = muted)
                for (level in 0..4) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color(level)))
                }
                Text("More", style = MaterialTheme.typography.labelSmall, color = muted)
            }
            Text(
                "Items passed or burned each day. Grey means no activity.",
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
        }
    }
}
