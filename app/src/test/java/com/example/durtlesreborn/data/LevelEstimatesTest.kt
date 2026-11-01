package com.example.durtlesreborn.data

import com.example.durtlesreborn.session.SubjectKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelEstimatesTest {
    private val hour = 3_600_000L
    private val now = 1_000_000 * hour

    private fun tile(stage: Int?, at: Long? = null, unlocked: Boolean = true, kind: SubjectKind = SubjectKind.KANJI) =
        LevelTile(1, kind, "\u6F22", null, "", stage, unlocked, at)

    @Test
    fun guruDateAddsTheIntervalsAfterTheNextReview() {
        assertEquals(0, LevelEstimates.guruAt(tile(5), 10, now))
        assertEquals(now + hour, LevelEstimates.guruAt(tile(4, now + hour), 10, now))
        assertEquals(now + hour + 47 * hour, LevelEstimates.guruAt(tile(3, now + hour), 10, now))
        assertEquals(now + hour + (8 + 23 + 47) * hour, LevelEstimates.guruAt(tile(1, now + hour), 10, now))
    }

    @Test
    fun aLessonNeedsTheWholeApprenticeLadder() {
        assertEquals(now + 82 * hour, LevelEstimates.guruAt(tile(0), 10, now))
    }

    @Test
    fun theFirstTwoLevelsAreFaster() {
        assertEquals(now + (2 + 4 + 8 + 23) * hour, LevelEstimates.guruAt(tile(0), 2, now))
    }

    @Test
    fun timeRemainingLeavesOutTheSlowestTenPercent() {
        val kanji = List(9) { tile(4, now + it * hour) } + tile(4, now + 500 * hour)
        val (finish, estimate) = LevelEstimates.timeRemaining(emptyList(), kanji, 10, now)!!
        assertEquals(now + 8 * hour, finish)
        assertFalse(estimate)
    }

    @Test
    fun aLockedKanjiThatIsNeededMakesItAnEstimate() {
        val kanji = List(5) { tile(null, unlocked = false) }
        val radicals = listOf(tile(4, now + 10 * hour, kind = SubjectKind.RADICAL))
        val (finish, estimate) = LevelEstimates.timeRemaining(radicals, kanji, 10, now)!!
        assertEquals(now + 10 * hour + 82 * hour, finish)
        assertTrue(estimate)
    }

    @Test
    fun noKanjiMeansNoEstimate() {
        assertNull(LevelEstimates.timeRemaining(emptyList(), emptyList(), 10, now))
    }

    @Test
    fun nextLevelUpReviewIsTheEarliestKanjiStillBelowGuru() {
        val kanji = listOf(tile(6, now + hour), tile(2, now + 5 * hour), tile(1, now + 3 * hour))
        val radicals = listOf(tile(2, now + hour, kind = SubjectKind.RADICAL))
        assertEquals(now + 3 * hour, LevelEstimates.nextLevelUpReview(radicals, kanji, 10, now))
    }

    @Test
    fun radicalsCountWhileNeededKanjiAreLocked() {
        val kanji = List(5) { tile(null, unlocked = false) }
        val radicals = listOf(tile(2, now + hour, kind = SubjectKind.RADICAL))
        assertEquals(now + hour, LevelEstimates.nextLevelUpReview(radicals, kanji, 10, now))
    }

    @Test
    fun durationsShowMinutesOnlyWhenNoHoursAreLeft() {
        assertEquals("Now", LevelEstimates.format(now - hour, now))
        assertEquals("23m", LevelEstimates.format(now + 23 * 60_000, now))
        assertEquals("5h", LevelEstimates.format(now + 5 * hour + 30 * 60_000, now))
        assertEquals("4d 8h", LevelEstimates.format(now + (4 * 24 + 8) * hour, now))
        assertEquals("2d", LevelEstimates.format(now + 48 * hour, now))
    }
}
