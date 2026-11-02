// Based on Tsurukame (ios/LocalCachingClient.swift),
// Copyright David Sansome, Apache License 2.0: see assets/TSURUKAME-LICENSE.txt.
// Rewritten in Kotlin and changed for this app.
package com.example.durtlesreborn.data

import androidx.room.Dao
import androidx.room.Query
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

enum class PracticeKind { RECENT_LESSONS, APPRENTICE_LEECHES, ALL_LEECHES, BURNED }

data class PracticeCounts(
    val recentLessons: Int = 0,
    val apprenticeLeeches: Int = 0,
    val allLeeches: Int = 0,
    val burned: Int = 0,
)

data class LeechRow(
    val subjectId: Long,
    val meaningIncorrect: Int,
    val readingIncorrect: Int,
    val meaningCurrentStreak: Int,
    val readingCurrentStreak: Int,
)

/** The leech formula Tsurukame uses, with its default threshold. */
internal fun isLeech(row: LeechRow, threshold: Float = 1f): Boolean {
    val streak = min(row.meaningCurrentStreak, row.readingCurrentStreak)
    return streak > 0 && max(row.meaningIncorrect, row.readingIncorrect) / streak.toFloat().pow(1.5f) >= threshold
}

private const val VISIBLE =
    "FROM assignment a JOIN subject s ON s.id = a.subjectId WHERE a.hidden = 0 AND s.hidden = 0 AND s.level <= :maxLevel "

@Dao
interface PracticeDao {
    /** Learned in lessons and not passed yet. */
    @Query("SELECT a.subjectId " + VISIBLE + "AND a.srsStage BETWEEN 1 AND 4 AND a.passedAt IS NULL")
    suspend fun recentLessons(maxLevel: Int): List<Long>

    /** Passed once, and fallen back to apprentice since. */
    @Query("SELECT a.subjectId " + VISIBLE + "AND a.srsStage BETWEEN 1 AND 4 AND a.passedAt IS NOT NULL")
    suspend fun apprenticeLeeches(maxLevel: Int): List<Long>

    @Query("SELECT a.subjectId " + VISIBLE + "AND a.srsStage = 9")
    suspend fun burned(maxLevel: Int): List<Long>

    @Query(
        "SELECT a.subjectId AS subjectId, r.meaningIncorrect AS meaningIncorrect, r.readingIncorrect AS readingIncorrect, " +
            "r.meaningCurrentStreak AS meaningCurrentStreak, r.readingCurrentStreak AS readingCurrentStreak " +
            "FROM assignment a JOIN subject s ON s.id = a.subjectId JOIN review_statistic r ON r.subjectId = a.subjectId " +
            "WHERE a.hidden = 0 AND s.hidden = 0 AND s.level <= :maxLevel AND a.passedAt IS NOT NULL AND a.burnedAt IS NULL",
    )
    suspend fun leechCandidates(maxLevel: Int): List<LeechRow>
}

class PracticeRepository(private val practice: PracticeDao, private val state: SyncStateDao) {
    private suspend fun maxLevel() = state.get(SyncEngine.KEY_MAX_LEVEL)?.toIntOrNull() ?: 3

    suspend fun counts(): PracticeCounts {
        val level = maxLevel()
        return PracticeCounts(
            recentLessons = practice.recentLessons(level).size,
            apprenticeLeeches = practice.apprenticeLeeches(level).size,
            allLeeches = practice.leechCandidates(level).count { isLeech(it) },
            burned = practice.burned(level).size,
        )
    }

    suspend fun ids(kind: PracticeKind): List<Long> {
        val level = maxLevel()
        return when (kind) {
            PracticeKind.RECENT_LESSONS -> practice.recentLessons(level)
            PracticeKind.APPRENTICE_LEECHES -> practice.apprenticeLeeches(level)
            PracticeKind.ALL_LEECHES -> practice.leechCandidates(level).filter { isLeech(it) }.map { it.subjectId }
            PracticeKind.BURNED -> practice.burned(level)
        }
    }
}
