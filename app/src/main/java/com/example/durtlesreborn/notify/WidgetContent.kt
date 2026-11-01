package com.example.durtlesreborn.notify

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Wording for the one line under the widget's two cards. */
object WidgetFormatter {
    private const val DAY = 24 * 3_600_000L

    /** "More at 14:00" within a day, "More in 1.5d" beyond that, nothing if no review is scheduled. */
    internal fun upcomingMessage(upcoming: Long, now: Long, zone: ZoneId, locale: Locale): String? {
        if (upcoming == 0L) return null
        val ahead = upcoming - now
        return if (ahead < DAY) {
            val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).withZone(zone)
            "More at ${time.format(Instant.ofEpochMilli(upcoming))}"
        } else {
            String.format(Locale.ROOT, "More in %.1fd", ahead.toFloat() / DAY)
        }
    }
}
