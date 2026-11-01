package com.example.durtlesreborn.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A radical, kanji or vocabulary item. The whole API payload is kept in [json] so no field is
 * lost; the columns are the ones queried or sorted on.
 */
@Entity(tableName = "subject", indices = [Index("level"), Index("type")])
data class SubjectEntity(
    @PrimaryKey val id: Long,
    val type: String,
    val level: Int,
    val slug: String?,
    val characters: String?,
    val hidden: Boolean,
    val dataUpdatedAt: Long,
    val json: String,
    /** Lower-case characters, slug, meanings and readings, for searching. */
    @ColumnInfo(defaultValue = "''")
    val searchText: String = "",
)

/** The user's progress on one subject. Times are epoch milliseconds. */
@Entity(tableName = "assignment", indices = [Index("subjectId"), Index("availableAt")])
data class AssignmentEntity(
    @PrimaryKey val id: Long,
    val subjectId: Long,
    val subjectType: String,
    val srsStage: Int,
    val unlockedAt: Long?,
    val startedAt: Long?,
    val passedAt: Long?,
    val burnedAt: Long?,
    val availableAt: Long?,
    val resurrectedAt: Long?,
    val hidden: Boolean,
    val dataUpdatedAt: Long,
)

@Entity(tableName = "review_statistic", indices = [Index("subjectId")])
data class ReviewStatisticEntity(
    @PrimaryKey val id: Long,
    val subjectId: Long,
    val subjectType: String,
    val meaningCorrect: Int,
    val meaningIncorrect: Int,
    val meaningMaxStreak: Int,
    val meaningCurrentStreak: Int,
    val readingCorrect: Int,
    val readingIncorrect: Int,
    val readingMaxStreak: Int,
    val readingCurrentStreak: Int,
    val percentageCorrect: Int,
    val hidden: Boolean,
    val dataUpdatedAt: Long,
)

/** The user's own notes and synonyms for a subject. */
@Entity(tableName = "study_material", indices = [Index("subjectId")])
data class StudyMaterialEntity(
    @PrimaryKey val id: Long,
    val subjectId: Long,
    val meaningNote: String?,
    val readingNote: String?,
    /** Synonyms as a JSON array of strings. */
    val meaningSynonymsJson: String,
    val dataUpdatedAt: Long,
)

/** Small key/value store for sync bookkeeping, such as the last `updated_after` per collection. */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val key: String,
    val value: String,
)

/** Shape and timing of a spaced repetition system, from /srs_systems. */
@Entity(tableName = "srs_system")
data class SrsSystemEntity(
    @PrimaryKey val id: Long,
    val startingPosition: Int,
    val passingPosition: Int,
    val burningPosition: Int,
    /** Review interval per stage position in milliseconds, as a JSON array (0 where there is none). */
    val intervalsMillisJson: String,
    val dataUpdatedAt: Long,
)
