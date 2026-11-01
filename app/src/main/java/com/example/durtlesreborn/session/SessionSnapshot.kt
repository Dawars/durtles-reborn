package com.example.durtlesreborn.session

import kotlinx.serialization.Serializable

@Serializable
data class SrsSnapshot(
    val firstStartedIndex: Int,
    val passedIndex: Int,
    val completedIndex: Int,
    val intervalsMillis: List<Long> = emptyList(),
) {
    fun toSystem() = SrsSystem(firstStartedIndex, passedIndex, completedIndex, intervalsMillis)

    companion object {
        fun of(srs: SrsSystem) = SrsSnapshot(srs.firstStartedIndex, srs.passedIndex, srs.completedIndex, srs.intervalsMillis)
    }
}

@Serializable
data class ItemSnapshot(
    val id: Long,
    val bucket: Int,
    val srs: SrsSnapshot,
    val srsStageIndex: Int,
    val characters: String? = null,
    val assignmentId: Long = 0,
    val state: String,
    val numAnswers: Int,
    val lastAnswer: Long,
    val choiceDelay: Int,
    /** Done flags for slots 0..4 (slot 0 is unused). */
    val done: List<Boolean>,
    val incorrect: List<Int>,
)

@Serializable
data class QuestionRef(val itemId: Long, val kind: String)

/**
 * Everything needed to continue a review or lesson session after the app was left or killed. Only
 * the answers so far are in it; nothing has been sent to the server or applied to the phone's
 * assignments until the session is finished.
 */
@Serializable
data class SessionSnapshot(
    val version: Int = VERSION,
    val type: String,
    val savedAt: Long,
    val state: String,
    val presentationIndex: Int,
    val delayed: Boolean,
    val backToBack: Boolean,
    val readingFirst: Boolean,
    val meaningFirst: Boolean,
    val items: List<ItemSnapshot>,
    /** Questions still to ask, in the order the engine keeps them. */
    val questions: List<QuestionRef>,
    /** Questions already answered, oldest first, so undo keeps working after a restart. */
    val history: List<QuestionRef>,
) {
    val sessionType: SessionType? get() = SessionType.entries.firstOrNull { it.name == type }

    /** Items that were not dropped by wrapping up. */
    val totalItems get() = items.count { it.state != SessionItemState.ABANDONED.name }

    /** Items answered completely, whether or not they are sent yet. */
    val doneItems get() = items.count { it.state == SessionItemState.PENDING.name || it.state == SessionItemState.REPORTED.name }

    companion object {
        const val VERSION = 1
    }
}
