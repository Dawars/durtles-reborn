package com.example.durtlesreborn.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PendingReview::class,
        SubjectEntity::class,
        AssignmentEntity::class,
        ReviewStatisticEntity::class,
        StudyMaterialEntity::class,
        SyncStateEntity::class,
        SrsSystemEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingReviews(): PendingReviewDao
    abstract fun subjects(): SubjectDao
    abstract fun assignments(): AssignmentDao
    abstract fun reviewStatistics(): ReviewStatisticDao
    abstract fun studyMaterials(): StudyMaterialDao
    abstract fun syncState(): SyncStateDao
    abstract fun srsSystems(): SrsSystemDao
    abstract fun search(): SearchDao
    abstract fun alerts(): AlertDao
    abstract fun stats(): StatsDao
    abstract fun levels(): LevelDao
    abstract fun practice(): PracticeDao
}
