package com.example.durtlesreborn.data

import com.example.durtlesreborn.session.PriorityContext
import com.example.durtlesreborn.session.SessionConfig
import com.example.durtlesreborn.session.SessionListener
import com.example.durtlesreborn.session.SessionItem
import com.example.durtlesreborn.session.SessionPlanner
import com.example.durtlesreborn.session.SessionPriority
import com.example.durtlesreborn.session.SessionSubject
import com.example.durtlesreborn.session.SessionType
import com.example.durtlesreborn.session.SrsSystem
import com.example.durtlesreborn.session.SubjectKind
import com.example.durtlesreborn.session.SubjectOrder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import kotlin.random.Random

class StatsMathTest {
    private val hour = 3_600_000L
    private val day = 24 * hour
    // 2027-01-15 10:20:00 UTC
    private val now = 1_800_008_400_000 - (1_800_008_400_000 % hour) + 20 * 60_000

    @Test
    fun hourlyBucketsStartAtTheCurrentHourAndCountEachReviewOnce() {
        val first = now - now % hour
        val times = listOf(now + 1, first + hour + 5, first + hour + 6, first + 23 * hour + 1, first + 24 * hour, now - hour)
        val h = StatsMath.hourly(times, now)
        assertEquals(24, h.size)
        assertEquals(first, h[0].startMillis)
        assertEquals(1, h[0].count)
        assertEquals(2, h[1].count)
        assertEquals(1, h[23].count)
        // One past the window and one before it are ignored.
        assertEquals(4, h.sumOf { it.count })
    }

    @Test
    fun dailyBucketsFollowTheLocalCalendar() {
        val zone = ZoneOffset.ofHours(10)
        val startOfToday = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val times = listOf(startOfToday + hour, startOfToday + day + 2 * hour, startOfToday + day + 23 * hour, startOfToday + 7 * day + hour)
        val d = StatsMath.daily(times, now, zone)
        assertEquals(7, d.size)
        assertEquals(startOfToday, d[0].startMillis)
        assertEquals(listOf(1, 2, 0, 0, 0, 0, 0), d.map { it.count })
    }

    @Test
    fun breakdownGroupsByStageAndKindAndMergesKanaVocabulary() {
        val counts = listOf(
            StageCount("radical", 1, 3), StageCount("radical", 4, 2),
            StageCount("kanji", 5, 10), StageCount("kanji", 6, 5),
            StageCount("vocabulary", 9, 100), StageCount("kana_vocabulary", 9, 7),
            StageCount("vocabulary", 0, 4),
        )
        val rows = StatsMath.breakdown(counts).associateBy { it.group }
        assertEquals(StageGroup.entries.toList(), StatsMath.breakdown(counts).map { it.group })
        assertEquals(BreakdownRow(StageGroup.APPRENTICE, 5, 0, 0), rows[StageGroup.APPRENTICE])
        assertEquals(15, rows[StageGroup.GURU]!!.kanji)
        assertEquals(107, rows[StageGroup.BURNED]!!.vocabulary)
        assertEquals(4, rows[StageGroup.NOT_STARTED]!!.total)
        assertEquals(0, rows[StageGroup.MASTER]!!.total)
    }

    @Test
    fun levelProgressNeedsNinetyPercentOfKanji() {
        val p = LevelProgress(5, totalKanji = 20, passedKanji = 15)
        assertEquals(18, p.neededKanji)
        assertEquals(3, p.remainingKanji)
        assertFalse(p.canLevelUp)
        assertEquals(15f / 18f, p.fraction, 0.001f)
        assertTrue(LevelProgress(5, 20, 18).canLevelUp)
        // 90% of 11 is 9.9, so 10 are needed.
        assertEquals(10, LevelProgress(5, 11, 0).neededKanji)
        assertFalse(LevelProgress(5, 0, 0).canLevelUp)
        assertEquals(1f, LevelProgress(5, 0, 0).fraction, 0f)
    }
}

class LevelUpIdsTest {
    private val subjects = FakeSubjects()
    private val state = FakeState()

    private fun kanji(id: Long, level: Int, components: List<Long>, hidden: Boolean = false) = SubjectEntity(
        id, "kanji", level, "k$id", "字", hidden, 0,
        """{"level":$level,"component_subject_ids":[${components.joinToString(",")}],"meanings":[],"readings":[]}""",
    )

    private fun repo() = SessionRepository(subjects, FakeAssignments(), FakeMaterials(), FakeSrsSystems(), state, { 0L })

    @Test
    fun levelUpTargetsAreTheCurrentLevelsKanjiAndTheirRadicals() = runBlocking {
        state.put(SyncStateEntity(SyncEngine.KEY_LEVEL, "5"))
        state.put(SyncStateEntity(SyncEngine.KEY_MAX_LEVEL, "60"))
        subjects.upsert(
            listOf(
                kanji(1, 5, listOf(100, 101)), kanji(2, 5, listOf(101, 102)),
                kanji(3, 4, listOf(103)), kanji(4, 6, listOf(104)), kanji(5, 5, listOf(105), hidden = true),
            ),
        )
        assertEquals(setOf(1L, 2L, 100L, 101L, 102L), repo().levelUpIds())
    }

    @Test
    fun noTargetsOnceTheUserReachedTheirSubscriptionLimit() = runBlocking {
        state.put(SyncStateEntity(SyncEngine.KEY_LEVEL, "3"))
        state.put(SyncStateEntity(SyncEngine.KEY_MAX_LEVEL, "3"))
        subjects.upsert(listOf(kanji(1, 3, listOf(100))))
        assertTrue(repo().levelUpIds().isEmpty())
    }

    @Test
    fun levelUpFirstPriorityPutsUnpassedTargetsFirst() {
        val srs = SrsSystem(1, 5, 9)
        val listener = object : SessionListener {
            override fun onReport(item: SessionItem) {}
            override fun onUpdate(item: SessionItem) {}
        }
        fun s(id: Long, stage: Int) = SessionSubject(id, id * 10, SubjectKind.KANJI, 5, stage, srs, characters = "字")
        val subjectsInSession = listOf(s(1, 2), s(2, 2), s(3, 6))
        val engine = SessionPlanner.plan(
            subjectsInSession,
            SessionConfig(
                SessionType.REVIEW, SubjectOrder.LEVEL, priority = SessionPriority.LevelUpFirst,
                priorityContext = PriorityContext(userLevel = 5, maxLevel = 60, levelUpIds = setOf(2L, 3L)),
            ),
            listener, Random(1),
        )
        // Subject 2 is a target and not yet passed; 3 is a target but already passed; 1 is no target.
        assertEquals(0, engine.items.first { it.id == 2L }.bucket)
        assertTrue(engine.items.first { it.id == 1L }.bucket > 0)
        assertEquals(engine.items.first { it.id == 1L }.bucket, engine.items.first { it.id == 3L }.bucket)
    }
}
