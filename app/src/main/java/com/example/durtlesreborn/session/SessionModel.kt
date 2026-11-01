package com.example.durtlesreborn.session

enum class SessionItemState { ACTIVE, PENDING, REPORTED, ABANDONED }

enum class SessionState { INACTIVE, LESSON_PRESENTATION, ACTIVE, FINISHING }

enum class SessionType { LESSON, REVIEW, SELF_STUDY }

/** The eight question types of the original. [slot] is the item's done/incorrect bookkeeping slot. */
enum class QuestionKind(val slot: Int, val meaning: Boolean, val reading: Boolean) {
    RADICAL_NAME(1, true, false),
    KANJI_MEANING(1, true, false),
    VOCAB_MEANING(1, true, false),
    KANA_VOCAB_MEANING(1, true, false),
    KANJI_READING(2, false, true),
    VOCAB_READING(2, false, true),
    KANJI_ONYOMI(3, false, true),
    KANJI_KUNYOMI(4, false, true),
    ;

    /** Reading questions are answered in kana. */
    val kana get() = reading

    /** WaniKani records pronunciation audio for vocabulary only, never for kanji or radicals. */
    val hasAudio get() = this == VOCAB_MEANING || this == VOCAB_READING || this == KANA_VOCAB_MEANING
}

/**
 * Shape of an SRS system's stages, by index. The original reads these from the API; the
 * new-stage rule is the original's own approximation of WaniKani's penalty.
 */
class SrsSystem(
    val firstStartedIndex: Int,
    val passedIndex: Int,
    val completedIndex: Int,
    /** Time until the next review, per stage index, in milliseconds. Empty if unknown. */
    val intervalsMillis: List<Long> = emptyList(),
) {
    fun intervalMillis(stageIndex: Int): Long = intervalsMillis.getOrElse(stageIndex) { 0L }

    /** New stage index after a finished item with [numIncorrect] wrong answers. */
    fun newStageIndex(current: Int, numIncorrect: Int): Int {
        var index = current
        if (numIncorrect == 0) {
            index++
        } else {
            val penalty = (numIncorrect + 1) / 2
            index -= penalty
            if (current >= passedIndex) index -= penalty
        }
        return index.coerceIn(firstStartedIndex, completedIndex)
    }
}

/** One subject being studied in a session, with the progress of up to four questions. */
class SessionItem(
    val id: Long,
    val bucket: Int,
    val srs: SrsSystem,
    var srsStageIndex: Int,
    /** The subject's characters, needed for the kana-answer guard. */
    val characters: String? = null,
    val assignmentId: Long = 0,
) {
    var state = SessionItemState.ACTIVE
    var numAnswers = 0
    var lastAnswer = 0L
    var choiceDelay = 0
    private val done = BooleanArray(5)
    private val incorrect = IntArray(5)

    val active get() = state == SessionItemState.ACTIVE
    val pending get() = state == SessionItemState.PENDING
    val reported get() = state == SessionItemState.REPORTED
    val abandoned get() = state == SessionItemState.ABANDONED
    val started get() = active && numAnswers > 0

    val finished get() = !active || (1..4).all { done[it] }

    /** Reading and meaning both still to be asked. */
    val hasPendingReadingAndMeaning get() = !done[1] && !(done[2] && done[3] && done[4])

    val totalIncorrect get() = (1..4).sumOf { incorrect[it] }

    /** Wrong meaning answers; this is what the server calls incorrect_meaning_answers. */
    val incorrectMeaning get() = incorrect[1]

    /** Wrong answers over all reading questions. */
    val incorrectReading get() = incorrect[2] + incorrect[3] + incorrect[4]
    val newSrsStageIndex get() = srs.newStageIndex(srsStageIndex, totalIncorrect)

    fun isDone(slot: Int) = done[slot]
    fun setDone(slot: Int, value: Boolean) { done[slot] = value }
    fun incorrectCount(slot: Int) = incorrect[slot]
    fun setIncorrectCount(slot: Int, value: Int) { incorrect[slot] = value }
}

class Question(val item: SessionItem, val kind: QuestionKind) {
    val finished get() = !item.active || item.isDone(kind.slot)
    val canUndo get() = !item.reported && !item.abandoned
}
