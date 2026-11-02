package com.example.durtlesreborn.data

import com.example.durtlesreborn.notify.AlertContext
import com.example.durtlesreborn.session.SubjectKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class DashboardTest {
    private val hour = 3_600_000L
    private val now = 1_800_000_000_000 - (1_800_000_000_000 % (24 * hour)) + 10 * hour + 20 * 60_000 // 10:20 UTC

    private fun json(pos: Int?, meaning: String, reading: String? = null, type: String = "kanji") = buildString {
        append("""{"level":5""")
        if (pos != null) append(""","lesson_position":$pos""")
        append(""","meanings":[{"meaning":"$meaning","primary":true,"accepted_answer":true}]""")
        if (reading != null) append(""","readings":[{"type":"onyomi","reading":"$reading","primary":true,"accepted_answer":true}]""") else append(""","readings":[]""")
        append("}")
    }

    private fun row(id: Long, type: String, chars: String?, slug: String, pos: Int?, meaning: String, reading: String? = null,
                    stage: Int? = 3, unlocked: Long? = 1, available: Long? = null) =
        LevelRow(id, type, slug, chars, json(pos, meaning, reading, type), stage, unlocked, available)

    @Test
    fun tilesAreGroupedByKindAndOrderedByLessonPosition() {
        val rows = listOf(
            row(1, "kanji", "二", "two", 2, "Two", "に"),
            row(2, "radical", "一", "one", 1, "One"),
            row(3, "kanji", "一", "one-k", 1, "One", "いち"),
            row(4, "vocabulary", "二つ", "two-v", 1, "Two Things", "ふたつ"),
            row(5, "kana_vocabulary", "ああ", "aa", 2, "Ah"),
        )
        val c = buildLevelContent(5, rows)
        assertEquals(listOf(2L), c.radicals.map { it.id })
        assertEquals(listOf(3L, 1L), c.kanji.map { it.id })
        // Kana-only vocabulary counts as vocabulary.
        assertEquals(listOf(4L, 5L), c.vocabulary.map { it.id })
    }

    @Test
    fun tileShowsReadingMeaningAndLockedState() {
        val c = buildLevelContent(
            5,
            listOf(
                row(1, "kanji", "水", "water", 1, "Water", "すい", stage = 7, unlocked = 5, available = now + 30 * hour),
                row(2, "kanji", "火", "fire", 2, "Fire", "か", stage = null, unlocked = null),
                row(3, "radical", null, "beggar", 1, "Beggar", null, stage = 2),
            ),
        )
        val water = c.kanji.first { it.id == 1L }
        assertEquals("水", water.title)
        assertEquals("すい", water.reading)
        assertEquals("Water", water.meaning)
        assertTrue(water.unlocked)
        assertEquals(7, water.stage)
        val fire = c.kanji.first { it.id == 2L }
        assertFalse(fire.unlocked)
        assertNull(fire.stage)
        // Image-only radicals show their slug and have no reading.
        val beggar = c.radicals.single()
        assertEquals("beggar", beggar.title)
        assertNull(beggar.reading)
    }

    @Test
    fun itemsWithoutALessonPositionSortLastByIdAndUnknownTypesAreDropped() {
        val c = buildLevelContent(
            5,
            listOf(row(9, "kanji", "九", "nine", null, "Nine"), row(8, "kanji", "八", "eight", null, "Eight"), row(7, "kanji", "七", "seven", 1, "Seven"), row(6, "mystery", "?", "x", 1, "X")),
        )
        assertEquals(listOf(7L, 8L, 9L), c.kanji.map { it.id })
        assertEquals(0, c.radicals.size + c.vocabulary.size)
    }

    @Test
    fun forecastListsOnlyHoursWithReviewsAndTheRunningTotalAcrossDays() {
        val day = 24 * hour
        val times = listOf(now + 40 * 60_000, now + 40 * 60_000 + 5, now + 3 * hour, now + day + hour, now + 2 * day + 2 * hour)
        val days = forecastDays(times, now, ZoneOffset.UTC, dueNow = 10, days = 7)
        assertEquals(7, days.size)
        // Today: 2 reviews at 11:00, 1 at 13:00; 10 are already due.
        assertEquals(listOf(2, 1), days[0].rows.map { it.count })
        assertEquals(listOf(12, 13), days[0].rows.map { it.cumulative })
        // The total keeps growing on later days.
        assertEquals(listOf(14), days[1].rows.map { it.cumulative })
        assertEquals(listOf(15), days[2].rows.map { it.cumulative })
        assertTrue(days[3].rows.isEmpty())
        assertEquals(3, days[0].total + 0)
    }

    private class FakeStats(
        val counts: List<StageCount>, val upcoming: List<Long>, val kanji: LevelKanjiCounts,
        val progress: List<Long> = emptyList(),
    ) : StatsDao {
        override suspend fun stageCounts(maxLevel: Int) = counts
        override suspend fun upcoming(maxLevel: Int, now: Long, until: Long) = upcoming
        override suspend fun levelKanji(level: Int) = kanji
        override suspend fun progressTimes(since: Long) = progress.filter { it >= since }
    }

    @Test
    fun theDashboardCombinesCountsProgressTilesForecastAndBreakdown() = runBlocking {
        val state = FakeState().also {
            it.put(SyncStateEntity(SyncEngine.KEY_LEVEL, "5"))
            it.put(SyncStateEntity(SyncEngine.KEY_MAX_LEVEL, "60"))
            it.put(SyncStateEntity(SyncEngine.KEY_USERNAME, "kame"))
        }
        val levels = FakeLevels().also {
            it.rows[5] = listOf(row(1, "radical", "一", "one", 1, "One"), row(2, "kanji", "二", "two", 1, "Two", "に"), row(3, "vocabulary", "三つ", "three", 1, "Three", "みっつ"))
        }
        val alerts = object : AlertDao {
            override suspend fun counts(maxLevel: Int, now: Long) = AlertCounts(8, 12, now - hour, now + hour)
        }
        val stats = FakeStats(
            counts = listOf(StageCount("kanji", 5, 4), StageCount("radical", 9, 2)),
            upcoming = listOf(now + 2 * hour),
            kanji = LevelKanjiCounts(total = 20, passed = 15),
            progress = listOf(now - hour, now - 2 * hour),
        )
        val d = DashboardRepository(stats, levels, alerts, state, { now }, { ZoneOffset.UTC }).load()
        assertEquals("kame", d.username)
        assertEquals(5, d.level)
        assertEquals(12, d.counts.reviewsDue)
        assertEquals(8, d.counts.lessonsAvailable)
        assertEquals(now + hour, d.counts.nextReviewAt)
        assertEquals(15, d.progress!!.passedKanji)
        assertEquals(18, d.progress!!.neededKanji)
        // The dashboard shows radicals and kanji of the current level; vocabulary stays on the level screen.
        assertEquals(listOf(1L), d.radicals.map { it.id })
        assertEquals(listOf(2L), d.kanji.map { it.id })
        assertEquals(7, d.days.size)
        assertEquals(13, d.days[0].rows.single().cumulative)
        assertEquals(4, d.breakdown.first { it.group == StageGroup.GURU }.kanji)
        assertEquals(2, d.breakdown.first { it.group == StageGroup.BURNED }.radicals)
        assertEquals(2, d.activity.flatMap { it.days }.filterNotNull().sumOf { it.count })
    }

    @Test
    fun withoutASavedLevelTheDashboardStillLoads() = runBlocking {
        val state = FakeState()
        val alerts = object : AlertDao {
            override suspend fun counts(maxLevel: Int, now: Long) = AlertCounts(0, 0, null, null)
        }
        val d = DashboardRepository(FakeStats(emptyList(), emptyList(), LevelKanjiCounts(0, 0)), FakeLevels(), alerts, state, { now }, { ZoneOffset.UTC }).load()
        assertNull(d.level)
        assertNull(d.progress)
        assertNull(d.username)
        assertTrue(d.radicals.isEmpty() && d.kanji.isEmpty())
    }

    @Test
    fun anyLevelCanBeLoadedForTheBrowser() = runBlocking {
        val levels = FakeLevels().also { it.rows[12] = listOf(row(1, "kanji", "二", "two", 1, "Two", "に")) }
        val alerts = object : AlertDao {
            override suspend fun counts(maxLevel: Int, now: Long) = AlertCounts(0, 0, null, null)
        }
        val repo = DashboardRepository(FakeStats(emptyList(), emptyList(), LevelKanjiCounts(0, 0)), levels, alerts, FakeState(), { now })
        assertEquals(1, repo.level(12).kanji.size)
        assertEquals(12, repo.level(12).level)
        assertEquals(0, repo.level(13).kanji.size)
        assertEquals(3, repo.maxLevel())
        assertEquals(AlertContext(), AlertContext())
    }

    @Test
    fun anEarlierLevelIsShownUntilEverythingInItIsPassed() {
        val passed = row(1, "kanji", "一", "one", 1, "One", "いち").copy(passedAt = 5)
        val unpassed = row(2, "kanji", "二", "two", 2, "Two", "に")
        assertEquals(28, unfinishedLevelContent(28, listOf(passed, unpassed))?.level)
        assertEquals(null, unfinishedLevelContent(28, listOf(passed)))
    }
}
