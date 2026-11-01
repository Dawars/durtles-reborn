package com.example.durtlesreborn.answer

/** What to do with an answer that is not exact but within the typo lenience. */
enum class CloseEnoughAction {
    SILENTLY_ACCEPT,
    ACCEPT_WITH_TOAST,
    ACCEPT_WITH_TOAST_NO_LM,
    SHAKE_AND_RETRY,
    REJECT,
}

data class AnswerVerdict(
    val ok: Boolean,
    /** When not ok, the user may retry without the incorrect answer being recorded. */
    val retry: Boolean = false,
    /** Accepted only because of the typo lenience. */
    val nearMatch: Boolean = false,
    val givenAnswer: String? = null,
    val matchedAnswer: String? = null,
    /** Set when the answer differs from a reading only by regular versus small kana. */
    val digraphMatch: DigraphMatch? = null,
) {
    companion object {
        val NOK_WITH_RETRY = AnswerVerdict(ok = false, retry = true)
        val NOK_WITHOUT_RETRY = AnswerVerdict(ok = false, retry = false)
    }
}
