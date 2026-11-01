package com.example.durtlesreborn.notify

import com.example.durtlesreborn.data.AlertDao
import com.example.durtlesreborn.data.SyncStateDao
import com.example.durtlesreborn.data.SyncStateEntity

/** Shows or removes the notification. The Android implementation is [AndroidNotificationSink]. */
interface NotificationSink {
    /** Returns false if nothing was shown, for example because the permission is missing. */
    fun show(title: String, text: String, sound: Boolean, settings: NotificationSettings): Boolean

    fun cancel()
}

data class NotificationSettings(
    val enabled: Boolean,
    val frequency: NotificationFrequency,
    /** The original's priority name: MIN, LOW, DEFAULT, HIGH or MAX. */
    val priority: String = "DEFAULT",
    /** The original's category name, e.g. RECOMMENDATION. */
    val category: String = "RECOMMENDATION",
)

/**
 * Decides, from the local database alone, whether the "reviews available" notification needs to
 * change, and tells the [sink]. Called on the hourly alarm and after every sync and session.
 */
class NotificationUpdater(
    private val alerts: AlertDao,
    private val state: SyncStateDao,
    private val settings: suspend () -> NotificationSettings,
    private val sink: NotificationSink,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun update() {
        val s = settings()
        if (!s.enabled) return
        val now = clock()
        val current = currentAlertContext(alerts, state, now)
        val decision = NotificationPolicy.decide(
            enabled = true,
            frequency = s.frequency,
            now = now,
            notificationShown = state.get(KEY_SHOWN) == "true",
            last = loadLast(),
            lastUpdateTopOfHour = state.get(KEY_LAST_UPDATE)?.toLongOrNull() ?: 0,
            current = current,
        )
        if (!decision.post) return

        if (current.isEmpty) {
            sink.cancel()
            remember(now, current, shown = false)
        } else {
            val (title, text) = NotificationPolicy.message(current)
            // Remember what was shown only if something was, so a missing permission is retried later.
            if (sink.show(title, text, decision.sound, s)) remember(now, current, shown = true)
        }
    }

    private suspend fun remember(now: Long, current: AlertContext, shown: Boolean) {
        state.put(SyncStateEntity(KEY_LAST_UPDATE, NotificationPolicy.topOfHour(now).toString()))
        state.put(SyncStateEntity(KEY_LAST, "${current.numLessons},${current.numReviews},${current.newestAvailableAt},${current.upcomingAvailableAt}"))
        state.put(SyncStateEntity(KEY_SHOWN, shown.toString()))
    }

    private suspend fun loadLast(): AlertContext {
        val parts = state.get(KEY_LAST)?.split(",")?.mapNotNull { it.toLongOrNull() } ?: return AlertContext()
        if (parts.size != 4) return AlertContext()
        return AlertContext(parts[0].toInt(), parts[1].toInt(), parts[2], parts[3])
    }

    private companion object {
        const val KEY_LAST = "notification.last"
        const val KEY_LAST_UPDATE = "notification.last_update"
        const val KEY_SHOWN = "notification.shown"
    }
}
