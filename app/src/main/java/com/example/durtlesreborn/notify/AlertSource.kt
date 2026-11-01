package com.example.durtlesreborn.notify

import com.example.durtlesreborn.data.AlertDao
import com.example.durtlesreborn.data.SyncEngine
import com.example.durtlesreborn.data.SyncStateDao

/** What is waiting right now, from the local database, for the notification and the widget. */
suspend fun currentAlertContext(alerts: AlertDao, state: SyncStateDao, now: Long): AlertContext {
    val maxLevel = state.get(SyncEngine.KEY_MAX_LEVEL)?.toIntOrNull() ?: 3
    val counts = alerts.counts(maxLevel, now)
    return AlertContext(
        numLessons = counts.numLessons,
        numReviews = counts.numReviews,
        newestAvailableAt = counts.newestAvailableAt ?: 0,
        upcomingAvailableAt = counts.upcomingAvailableAt ?: 0,
    )
}
