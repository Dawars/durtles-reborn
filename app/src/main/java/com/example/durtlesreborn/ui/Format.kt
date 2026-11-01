package com.example.durtlesreborn.ui

/** "Now", "in 25 min", "in 3 h 20 min" or "in 2 d 4 h" for a time span in milliseconds. */
fun formatUntil(millis: Long): String {
    if (millis <= 0) return "now"
    val minutes = (millis + 59_999) / 60_000
    val days = minutes / (24 * 60)
    val hours = minutes % (24 * 60) / 60
    val mins = minutes % 60
    return when {
        days > 0 -> if (hours > 0) "in $days d $hours h" else "in $days d"
        hours > 0 -> if (mins > 0) "in $hours h $mins min" else "in $hours h"
        else -> "in $mins min"
    }
}
