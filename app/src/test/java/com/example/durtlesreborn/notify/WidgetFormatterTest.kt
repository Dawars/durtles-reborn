package com.example.durtlesreborn.notify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

class WidgetFormatterTest {
    private val hour = 3_600_000L
    private val day = 24 * hour
    private val now = 1_800_000_000_000
    private val zone: ZoneId = ZoneOffset.UTC

    @Test
    fun footerSaysWhenMoreArrivesWithinADay() {
        val message = WidgetFormatter.upcomingMessage(now + 3 * hour, now, zone, Locale.US)
        assertEquals(true, message!!.startsWith("More at "))
    }

    @Test
    fun footerCountsDaysBeyondADay() {
        assertEquals("More in 1.5d", WidgetFormatter.upcomingMessage(now + day + 12 * hour, now, zone, Locale.US))
        assertEquals("More in 3.0d", WidgetFormatter.upcomingMessage(now + 3 * day, now, zone, Locale.US))
    }

    @Test
    fun noUpcomingReviewMeansNoFooter() {
        assertNull(WidgetFormatter.upcomingMessage(0, now, zone, Locale.US))
    }

    @Test
    fun timeIsShownInTheGivenZone() {
        val upcoming = 10 * hour + 5 * 60_000 // 10:05 UTC
        val utc = WidgetFormatter.upcomingMessage(upcoming + 100 * day, upcoming, ZoneOffset.UTC, Locale.US)
        assertEquals("More in 100.0d", utc)
        val soon = WidgetFormatter.upcomingMessage(upcoming + 2 * hour, upcoming, ZoneOffset.UTC, Locale.US)
        assertEquals(true, soon!!.contains("12:05"))
    }
}
