package com.example.durtlesreborn.data

import androidx.room.Dao
import androidx.room.Query
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil

/** How many assignments of one subject type are at one SRS stage. */
data class StageCount(val type: String, val srsStage: Int, val count: Int)

/** Kanji of one level: how many exist and how many the user has passed. */
data class LevelKanjiCounts(val total: Int, val passed: Int)

@Dao
interface StatsDao {
    @Query(
        "SELECT s.type AS type, a.srsStage AS srsStage, COUNT(*) AS count FROM assignment a JOIN subject s ON s.id = a.subjectId " +
            "WHERE a.hidden = 0 AND s.hidden = 0 AND s.level <= :maxLevel GROUP BY s.type, a.srsStage",
    )
    suspend fun stageCounts(maxLevel: Int): List<StageCount>

    /** When each not-yet-due review becomes available, up to [until]. */
    @Query(
        "SELECT a.availableAt FROM assignment a JOIN subject s ON s.id = a.subjectId WHERE a.hidden = 0 AND s.hidden = 0 " +
            "AND s.level <= :maxLevel AND a.startedAt IS NOT NULL AND a.burnedAt IS NULL AND a.availableAt IS NOT NULL " +
            "AND a.availableAt > :now AND a.availableAt <= :until ORDER BY a.availableAt",
    )
    suspend fun upcoming(maxLevel: Int, now: Long, until: Long): List<Long>

    @Query(
        "SELECT COUNT(*) AS total, COALESCE(SUM(CASE WHEN a.passedAt IS NOT NULL THEN 1 ELSE 0 END), 0) AS passed " +
            "FROM subject s LEFT JOIN assignment a ON a.subjectId = s.id WHERE s.type = 'kanji' AND s.hidden = 0 AND s.level = :level",
    )
    suspend fun levelKanji(level: Int): LevelKanjiCounts

    /** When each assignment was passed or burned, for the activity heatmap. */
    @Query(
        "SELECT passedAt AS at FROM assignment WHERE hidden = 0 AND passedAt IS NOT NULL AND passedAt >= :since " +
            "UNION ALL SELECT burnedAt AS at FROM assignment WHERE hidden = 0 AND burnedAt IS NOT NULL AND burnedAt >= :since",
    )
    suspend fun progressTimes(since: Long): List<Long>
}

/** Reviews becoming available in one hour or one day. */
data class ForecastBucket(val startMillis: Long, val count: Int)

/** Progress towards the next level, which needs 90% of the current level's kanji passed. */
data class LevelProgress(val level: Int, val totalKanji: Int, val passedKanji: Int) {
    val neededKanji get() = ceil(totalKanji * LEVEL_UP_SHARE).toInt()
    val remainingKanji get() = (neededKanji - passedKanji).coerceAtLeast(0)
    val fraction get() = if (neededKanji == 0) 1f else (passedKanji.toFloat() / neededKanji).coerceIn(0f, 1f)
    val canLevelUp get() = totalKanji > 0 && passedKanji >= neededKanji

    companion object {
        const val LEVEL_UP_SHARE = 0.9
    }
}

/** One row of the SRS breakdown: how many subjects of each kind are in a stage group. */
data class BreakdownRow(val group: StageGroup, val radicals: Int, val kanji: Int, val vocabulary: Int) {
    val total get() = radicals + kanji + vocabulary
}

data class Stats(
    val breakdown: List<BreakdownRow>,
    val hourly: List<ForecastBucket>,
    val daily: List<ForecastBucket>,
    val level: LevelProgress?,
    val reviewsDueNow: Int,
)

/** The arithmetic behind the stats screen, free of Android and the database so it can be tested. */
object StatsMath {
    private const val HOUR = 3_600_000L

    /** Counts per clock hour for [hours] hours starting with the hour that contains [now]. */
    fun hourly(times: List<Long>, now: Long, hours: Int = 24): List<ForecastBucket> {
        val first = now - now % HOUR
        val counts = IntArray(hours)
        for (t in times) {
            val index = ((t - first) / HOUR).toInt()
            if (t >= first && index in 0 until hours) counts[index]++
        }
        return counts.mapIndexed { i, c -> ForecastBucket(first + i * HOUR, c) }
    }

    /** Counts per local calendar day for [days] days starting with the day that contains [now]. */
    fun daily(times: List<Long>, now: Long, zone: ZoneId, days: Int = 7): List<ForecastBucket> {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val counts = IntArray(days)
        for (t in times) {
            val offset = java.time.temporal.ChronoUnit.DAYS.between(today, Instant.ofEpochMilli(t).atZone(zone).toLocalDate()).toInt()
            if (offset in 0 until days) counts[offset]++
        }
        return counts.mapIndexed { i, c -> ForecastBucket(startOfDay(today.plusDays(i.toLong()), zone), c) }
    }

    private fun startOfDay(day: LocalDate, zone: ZoneId) = day.atStartOfDay(zone).toInstant().toEpochMilli()

    /** Groups stage counts by SRS group and subject kind; every group appears, in stage order. */
    fun breakdown(counts: List<StageCount>): List<BreakdownRow> =
        StageGroup.entries.map { group ->
            fun sum(vararg types: String) = counts.filter { it.type in types && StageGroup.of(it.srsStage) == group }.sumOf { it.count }
            BreakdownRow(group, sum("radical"), sum("kanji"), sum("vocabulary", "kana_vocabulary"))
        }
}

class StatsRepository(
    private val stats: StatsDao,
    private val alerts: AlertDao,
    private val state: SyncStateDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    suspend fun load(): Stats {
        val now = clock()
        val maxLevel = state.get(SyncEngine.KEY_MAX_LEVEL)?.toIntOrNull() ?: 3
        val userLevel = state.get(SyncEngine.KEY_LEVEL)?.toIntOrNull()
        // A week ahead covers both forecasts; the hourly one only uses the first day.
        val times = stats.upcoming(maxLevel, now, now + 8 * 24 * 3_600_000L)
        val level = userLevel?.let {
            val k = stats.levelKanji(it)
            LevelProgress(it, k.total, k.passed)
        }
        return Stats(
            breakdown = StatsMath.breakdown(stats.stageCounts(maxLevel)),
            hourly = StatsMath.hourly(times, now),
            daily = StatsMath.daily(times, now, zone()),
            level = level,
            reviewsDueNow = alerts.counts(maxLevel, now).numReviews,
        )
    }
}
