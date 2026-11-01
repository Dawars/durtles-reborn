package com.example.durtlesreborn.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

enum class PendingKind { REVIEW, START_LESSON }

/**
 * A finished review or lesson waiting to be sent. It is written before anything is sent, so a
 * session never depends on the network. Sent in [id] order.
 */
@Entity(tableName = "pending_review")
data class PendingReview(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String = PendingKind.REVIEW.name,
    val subjectId: Long = 0,
    /** 0 when unknown; a lesson start then looks the assignment up by subject first. */
    val assignmentId: Long = 0,
    val incorrectMeaningAnswers: Int = 0,
    val incorrectReadingAnswers: Int = 0,
    /** When the item was finished on the phone, epoch milliseconds. */
    val createdAt: Long,
    /** Failed send attempts, for back-off and diagnostics. */
    val attempts: Int = 0,
)

@Dao
interface PendingReviewDao {
    @Insert
    suspend fun insert(review: PendingReview): Long

    @Query("SELECT * FROM pending_review ORDER BY id")
    suspend fun all(): List<PendingReview>

    @Query("SELECT COUNT(*) FROM pending_review")
    suspend fun count(): Int

    @Query("DELETE FROM pending_review WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE pending_review SET attempts = attempts + 1 WHERE id = :id")
    suspend fun markAttempted(id: Long)
}
