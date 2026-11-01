package com.example.durtlesreborn.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.durtlesreborn.DurtlesApp
import com.example.durtlesreborn.MainActivity
import com.example.durtlesreborn.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Priority names of the original mapped to Android importance and legacy priority. */
private fun importanceOf(name: String) = when (name) {
    "MIN" -> NotificationManager.IMPORTANCE_MIN
    "LOW" -> NotificationManager.IMPORTANCE_LOW
    "HIGH" -> NotificationManager.IMPORTANCE_HIGH
    "MAX" -> NotificationManager.IMPORTANCE_MAX
    else -> NotificationManager.IMPORTANCE_DEFAULT
}

private fun compatPriorityOf(name: String) = when (name) {
    "MIN" -> NotificationCompat.PRIORITY_MIN
    "LOW" -> NotificationCompat.PRIORITY_LOW
    "HIGH" -> NotificationCompat.PRIORITY_HIGH
    "MAX" -> NotificationCompat.PRIORITY_MAX
    else -> NotificationCompat.PRIORITY_DEFAULT
}

/** The original's notification category names mapped to the platform's. */
private fun categoryOf(name: String): String = when (name) {
    "CALL" -> NotificationCompat.CATEGORY_CALL
    "NAVIGATION" -> NotificationCompat.CATEGORY_NAVIGATION
    "MESSAGE" -> NotificationCompat.CATEGORY_MESSAGE
    "EMAIL" -> NotificationCompat.CATEGORY_EMAIL
    "EVENT" -> NotificationCompat.CATEGORY_EVENT
    "PROMO" -> NotificationCompat.CATEGORY_PROMO
    "ALARM" -> NotificationCompat.CATEGORY_ALARM
    "PROGRESS" -> NotificationCompat.CATEGORY_PROGRESS
    "SOCIAL" -> NotificationCompat.CATEGORY_SOCIAL
    "ERROR" -> NotificationCompat.CATEGORY_ERROR
    "TRANSPORT" -> NotificationCompat.CATEGORY_TRANSPORT
    "SYSTEM" -> NotificationCompat.CATEGORY_SYSTEM
    "SERVICE" -> NotificationCompat.CATEGORY_SERVICE
    "REMINDER" -> NotificationCompat.CATEGORY_REMINDER
    "STATUS" -> NotificationCompat.CATEGORY_STATUS
    else -> NotificationCompat.CATEGORY_RECOMMENDATION
}

class AndroidNotificationSink(private val context: Context) : NotificationSink {
    override fun show(title: String, text: String, sound: Boolean, settings: NotificationSettings): Boolean {
        if (!canPost(context)) return false
        val priorityName = settings.priority
        // A channel's importance cannot change once created, so each priority gets its own channel.
        val channelId = "reviews_${priorityName.lowercase()}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "New reviews", importanceOf(priorityName)).apply {
                description = "New reviews available"
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(compatPriorityOf(priorityName))
            .setCategory(categoryOf(settings.category))
            .setContentIntent(open)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(!sound)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        return true
    }

    override fun cancel() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    companion object {
        const val NOTIFICATION_ID = 1

        /** On Android 13 and later the user must have granted the notification permission. */
        fun canPost(context: Context): Boolean =
            (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                ) && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

/** Fires on the hour, and after boot, to refresh the notification and set the next alarm. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as DurtlesApp
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // After a boot the alarm is gone; after an hourly alarm this sets the next one. The alarm
                // only keeps going while something needs it: notifications or a widget.
                val needed = app.container.settingsStore.current().enableNotifications || WidgetUpdater.hasWidgets(context)
                if (needed) {
                    app.container.refreshAlerts()
                    NotificationScheduler.scheduleNext(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

object NotificationScheduler {
    private const val REQUEST_CODE = 4711

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context, REQUEST_CODE, Intent(context, AlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Next alarm just after the top of the hour, when new reviews become available. The system may delay it slightly. */
    fun scheduleNext(context: Context, now: Long = System.currentTimeMillis()) {
        val next = NotificationPolicy.topOfHour(now) + 3_600_000L + 2_000L
        context.getSystemService(AlarmManager::class.java)?.set(AlarmManager.RTC_WAKEUP, next, pendingIntent(context))
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context))
    }
}
