package com.example.durtlesreborn.notify

/** What is waiting: lessons, reviews due now, and when the newest due review became available. */
data class AlertContext(
    val numLessons: Int = 0,
    val numReviews: Int = 0,
    val newestAvailableAt: Long = 0,
    val upcomingAvailableAt: Long = 0,
) {
    val isEmpty get() = numLessons == 0 && numReviews == 0
}

enum class NotificationFrequency { ONLY_NEW_REVIEWS, ONCE_PER_HOUR, CONTINUOUSLY }

/** [post] says the notification should be (re)posted or, if nothing is waiting, cancelled; [sound] that it should alert. */
data class NotificationDecision(val post: Boolean, val sound: Boolean)

/** Port of the original's NotificationWorker.processAlarmHelper decision. */
object NotificationPolicy {
    private const val HOUR = 3_600_000L

    fun topOfHour(ts: Long) = ts - ts % HOUR

    fun decide(
        enabled: Boolean,
        frequency: NotificationFrequency,
        now: Long,
        notificationShown: Boolean,
        last: AlertContext,
        lastUpdateTopOfHour: Long,
        current: AlertContext,
    ): NotificationDecision {
        if (!enabled) return NotificationDecision(post = false, sound = false)
        var post = false
        var sound = false

        // Something is still shown although nothing is waiting any more.
        if (notificationShown && current.isEmpty) post = true
        // A review became available after the last notification.
        if (current.newestAvailableAt > last.newestAvailableAt) {
            post = true
            sound = true
        }
        // Everything was done since the last notification.
        if (current.isEmpty && !last.isEmpty) post = true

        val countsChanged = last.numLessons != current.numLessons || last.numReviews != current.numReviews
        when (frequency) {
            NotificationFrequency.ONLY_NEW_REVIEWS -> Unit
            NotificationFrequency.ONCE_PER_HOUR -> if (topOfHour(now) != lastUpdateTopOfHour && countsChanged) post = true
            NotificationFrequency.CONTINUOUSLY -> if (countsChanged) post = true
        }
        return NotificationDecision(post, sound)
    }

    /** Title and text, as in the original. */
    fun message(ctx: AlertContext): Pair<String, String> =
        if (ctx.numLessons > 0) {
            "New lessons and reviews available" to "${ctx.numLessons} lessons and ${ctx.numReviews} reviews available"
        } else {
            "New reviews available" to "${ctx.numReviews} reviews available"
        }
}
