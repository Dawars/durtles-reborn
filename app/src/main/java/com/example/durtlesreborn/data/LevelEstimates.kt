// Based on Tsurukame (ios/LevelTimeRemainingItem.swift, CurrentLevelReviewTimeItem.swift),
// Copyright David Sansome, Apache License 2.0: see assets/TSURUKAME-LICENSE.txt.
// Rewritten in Kotlin and changed for this app.
package com.example.durtlesreborn.data

import com.example.durtlesreborn.session.SubjectKind

/**
 * The time estimates on the home screen, following Tsurukame's `LevelTimeRemainingItem` and
 * `CurrentLevelReviewTimeItem`. They are for display only, so they use WaniKani's published SRS
 * intervals instead of the stored system.
 */
object LevelEstimates {
    private const val HOUR = 3_600_000L
    private const val FAR_FUTURE = Long.MAX_VALUE

    /** Time until the next review after reaching a stage, for stages 1 to 4. Levels 1 and 2 are faster. */
    private fun interval(stage: Int, level: Int): Long {
        val accelerated = level <= 2
        return when (stage) {
            1 -> if (accelerated) 2 * HOUR else 4 * HOUR
            2 -> if (accelerated) 4 * HOUR else 8 * HOUR
            3 -> if (accelerated) 8 * HOUR else 23 * HOUR
            4 -> if (accelerated) 23 * HOUR else 47 * HOUR
            else -> 0
        }
    }

    /** The quickest way from reaching [stage] to reaching Guru. */
    private fun fromStageToGuru(stage: Int, level: Int): Long =
        (stage.coerceAtLeast(1)..4).sumOf { interval(it, level) }

    /** When [tile] can reach Guru with no mistakes: the past if it is there already, [FAR_FUTURE] if locked. */
    internal fun guruAt(tile: LevelTile, level: Int, now: Long): Long {
        val stage = tile.stage ?: 0
        return when {
            !tile.unlocked -> FAR_FUTURE
            stage >= 5 -> 0
            stage == 0 -> now + fromStageToGuru(1, level)
            else -> (tile.nextReviewAt ?: now) + if (stage >= 4) 0 else fromStageToGuru(stage + 1, level)
        }
    }

    /** The dates that decide a level-up: 90% of the level's kanji, the slowest 10% left out. */
    private fun neededKanjiDates(kanji: List<LevelTile>, level: Int, now: Long): List<Long> {
        val dates = kanji.map { guruAt(it, level, now) }.sorted()
        return dates.dropLast((dates.size * 0.1).toInt())
    }

    /**
     * When the level can be finished, and whether that is only an estimate because a kanji it needs
     * is still locked behind radicals. Null when the level has no kanji to go by.
     */
    fun timeRemaining(radicals: List<LevelTile>, kanji: List<LevelTile>, level: Int, now: Long): Pair<Long, Boolean>? {
        val last = neededKanjiDates(kanji, level, now).lastOrNull() ?: return null
        if (last != FAR_FUTURE) return last to false
        val lastRadical = radicals.maxOfOrNull { guruAt(it, level, now) }?.takeIf { it != FAR_FUTURE } ?: now
        return lastRadical.coerceAtLeast(now) + fromStageToGuru(1, level) to true
    }

    /** The next review that counts towards levelling up, or null if there is none. */
    fun nextLevelUpReview(radicals: List<LevelTile>, kanji: List<LevelTile>, level: Int, now: Long): Long? {
        val kanjiUnlocked = neededKanjiDates(kanji, level, now).lastOrNull() != FAR_FUTURE
        val candidates = if (kanjiUnlocked) kanji else kanji + radicals
        return candidates
            .filter { it.kind != SubjectKind.VOCABULARY && (it.stage ?: 0) in 1..4 }
            .mapNotNull { it.nextReviewAt }
            .minOrNull()
    }

    /** "Now", "23m", "5h" or "4d 8h": minutes only appear once there are no hours left. */
    fun format(at: Long, now: Long): String {
        val minutes = (at - now) / 60_000
        if (minutes <= 0) return "Now"
        val days = minutes / (24 * 60)
        val hours = minutes / 60 % 24
        return when {
            days > 0 && hours > 0 -> "${days}d ${hours}h"
            days > 0 -> "${days}d"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }
}
