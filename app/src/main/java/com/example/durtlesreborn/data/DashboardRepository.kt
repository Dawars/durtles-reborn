package com.example.durtlesreborn.data

import androidx.room.Dao
import androidx.room.Query
import com.example.durtlesreborn.session.SubjectKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Months shown in the dashboard activity heatmap: one row per month, one cell per day. */
internal const val HEATMAP_MONTHS = 6

/** A subject of one level with the user's progress on it, as stored. */
data class LevelRow(
    val id: Long,
    val type: String,
    val slug: String?,
    val characters: String?,
    val json: String,
    val srsStage: Int?,
    val unlockedAt: Long?,
    val availableAt: Long?,
)

@Dao
interface LevelDao {
    @Query(
        "SELECT s.id AS id, s.type AS type, s.slug AS slug, s.characters AS characters, s.json AS json, " +
            "a.srsStage AS srsStage, a.unlockedAt AS unlockedAt, a.availableAt AS availableAt " +
            "FROM subject s LEFT JOIN assignment a ON a.subjectId = s.id WHERE s.hidden = 0 AND s.level = :level",
    )
    suspend fun items(level: Int): List<LevelRow>
}

/** One tile on the dashboard or the level screen. */
data class LevelTile(
    val id: Long,
    val kind: SubjectKind,
    val title: String,
    val reading: String?,
    val meaning: String,
    /** SRS stage position, or null if the user has no assignment for it. */
    val stage: Int?,
    /** Locked items are not available to study yet. */
    val unlocked: Boolean,
    val nextReviewAt: Long?,
)

data class LevelContent(
    val level: Int,
    val radicals: List<LevelTile>,
    val kanji: List<LevelTile>,
    val vocabulary: List<LevelTile>,
)

/** Reviews arriving in one hour, and how many will be waiting then in total. */
data class HourRow(val startMillis: Long, val count: Int, val cumulative: Int)

/** Only hours with reviews are listed. */
data class DayForecast(val dayStartMillis: Long, val rows: List<HourRow>) {
    val total get() = rows.sumOf { it.count }
}

/** One day of the activity heatmap: how many items were passed or burned that day. */
data class HeatDay(val date: LocalDate, val count: Int)

/** One month of the activity heatmap: one entry per day of the month, null for a day in the future. */
data class HeatMonth(val label: String, val days: List<HeatDay?>)

/**
 * The last [months] calendar months ending with the month of [today], oldest first. Every month
 * lists one entry per calendar day (28–31 of them), so a row has one cell per day; days after
 * [today] are null so the current month reserves its future cells.
 */
fun activityMonths(times: List<Long>, today: LocalDate, zone: ZoneId, months: Int = HEATMAP_MONTHS): List<HeatMonth> {
    val first = today.withDayOfMonth(1).minusMonths((months - 1).toLong())
    val counts = HashMap<LocalDate, Int>()
    for (t in times) {
        val date = Instant.ofEpochMilli(t).atZone(zone).toLocalDate()
        if (date.isBefore(first) || date.isAfter(today)) continue
        counts[date] = (counts[date] ?: 0) + 1
    }
    return (0 until months).map { offset ->
        val month = first.plusMonths(offset.toLong())
        val days = (1..month.lengthOfMonth()).map { day ->
            val date = month.withDayOfMonth(day)
            if (date.isAfter(today)) null else HeatDay(date, counts[date] ?: 0)
        }
        HeatMonth(month.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH), days)
    }
}

/** 0 for no activity, then 1-4 as a share of the busiest day, like a contribution graph. */
fun heatLevel(count: Int, max: Int): Int = when {
    count <= 0 || max <= 0 -> 0
    else -> (1 + count.toLong() * 3 / max).toInt().coerceIn(1, 4)
}

/** Everything the dashboard shows. */
data class Dashboard(
    val username: String?,
    val level: Int?,
    val counts: HomeCounts,
    val progress: LevelProgress?,
    val radicals: List<LevelTile>,
    val kanji: List<LevelTile>,
    val days: List<DayForecast>,
    val breakdown: List<BreakdownRow>,
    /** One entry per month, oldest first, each with one entry per day of that month. */
    val activity: List<HeatMonth> = emptyList(),
    val vocabulary: List<LevelTile> = emptyList(),
)

internal fun lessonPositionOf(json: String): Int =
    (SubjectContent.parse(json).rawData["lesson_position"] as? JsonPrimitive)?.intOrNull ?: Int.MAX_VALUE

/** Turns stored rows into tiles, grouped by kind and in the order WaniKani teaches them. */
internal fun buildLevelContent(level: Int, rows: List<LevelRow>): LevelContent {
    val tiles = rows.mapNotNull { row ->
        val kind = subjectKindOf(row.type) ?: return@mapNotNull null
        val content = SubjectContent.parse(row.json)
        val tile = LevelTile(
            id = row.id,
            kind = kind,
            title = row.characters ?: row.slug ?: "?",
            reading = if (kind == SubjectKind.RADICAL) null else content.readings.firstOrNull { it.acceptedAnswer }?.reading
                ?: content.readings.firstOrNull()?.reading,
            meaning = content.meanings.firstOrNull { it.acceptedAnswer }?.text.orEmpty(),
            stage = row.srsStage,
            unlocked = row.unlockedAt != null,
            nextReviewAt = row.availableAt,
        )
        Triple(tile, lessonPositionOf(row.json), row.id)
    }.sortedWith(compareBy({ it.second }, { it.third })).map { it.first }
    return LevelContent(
        level = level,
        radicals = tiles.filter { it.kind == SubjectKind.RADICAL },
        kanji = tiles.filter { it.kind == SubjectKind.KANJI },
        vocabulary = tiles.filter { it.kind == SubjectKind.VOCABULARY || it.kind == SubjectKind.KANA_VOCABULARY },
    )
}

/** Review forecast by day: for each of the next days, the hours with reviews and the running total. */
fun forecastDays(times: List<Long>, now: Long, zone: ZoneId, dueNow: Int, days: Int = 7): List<DayForecast> {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val counts = Array(days) { IntArray(24) }
    for (t in times) {
        val at = Instant.ofEpochMilli(t).atZone(zone)
        val offset = ChronoUnit.DAYS.between(today, at.toLocalDate()).toInt()
        if (offset in 0 until days) counts[offset][at.hour]++
    }
    var running = dueNow
    return (0 until days).map { offset ->
        val day = today.plusDays(offset.toLong())
        val rows = ArrayList<HourRow>()
        for (hour in 0 until 24) {
            val c = counts[offset][hour]
            running += c
            if (c > 0) rows += HourRow(day.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli(), c, running)
        }
        DayForecast(day.atStartOfDay(zone).toInstant().toEpochMilli(), rows)
    }
}

class DashboardRepository(
    private val stats: StatsDao,
    private val levels: LevelDao,
    private val alerts: AlertDao,
    private val state: SyncStateDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    suspend fun maxLevel(): Int = state.get(SyncEngine.KEY_MAX_LEVEL)?.toIntOrNull() ?: 3

    /** The tiles of any one level, for the level browser. */
    suspend fun level(level: Int): LevelContent {
        val rows = levels.items(level)
        return withContext(Dispatchers.Default) { buildLevelContent(level, rows) }
    }

    suspend fun load(): Dashboard {
        val now = clock()
        val zone = zone()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val maxLevel = maxLevel()
        val userLevel = state.get(SyncEngine.KEY_LEVEL)?.toIntOrNull()
        val alert = alerts.counts(maxLevel, now)
        val times = stats.upcoming(maxLevel, now, now + 8 * 24 * 3_600_000L)
        val stageCounts = stats.stageCounts(maxLevel)
        val progress = userLevel?.let { level -> stats.levelKanji(level).let { LevelProgress(level, it.total, it.passed) } }
        val rows = userLevel?.let { levels.items(it) }.orEmpty()
        val username = state.get(SyncEngine.KEY_USERNAME)
        val heatmapStart = today.withDayOfMonth(1).minusMonths((HEATMAP_MONTHS - 1).toLong())
        val progressTimes = stats.progressTimes(heatmapStart.atStartOfDay(zone).toInstant().toEpochMilli())
        return withContext(Dispatchers.Default) {
            val content = buildLevelContent(userLevel ?: 0, rows)
            Dashboard(
                username = username,
                level = userLevel,
                counts = HomeCounts(alert.numReviews, alert.numLessons, alert.upcomingAvailableAt),
                progress = progress,
                radicals = content.radicals,
                kanji = content.kanji,
                days = forecastDays(times, now, zone, alert.numReviews),
                breakdown = StatsMath.breakdown(stageCounts),
                activity = activityMonths(progressTimes, today, zone),
                vocabulary = content.vocabulary,
            )
        }
    }
}
