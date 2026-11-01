package com.example.durtlesreborn.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class HeatmapTest {
    private val today = LocalDate.of(2026, 10, 20)

    private fun at(date: LocalDate, zone: ZoneOffset = ZoneOffset.UTC) =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun theLastSixCalendarMonthsAreListedOldestFirstWithOneCellPerDay() {
        val months = activityMonths(emptyList(), today, ZoneOffset.UTC)
        assertEquals(6, months.size)
        assertEquals(listOf("May", "Jun", "Jul", "Aug", "Sep", "Oct"), months.map { it.label })
        assertEquals(listOf(31, 30, 31, 31, 30, 31), months.map { it.days.size })
    }

    @Test
    fun daysAfterTodayAreEmpty() {
        val october = activityMonths(emptyList(), today, ZoneOffset.UTC).last()
        assertNotNull(october.days[19]) // 20 October
        assertNull(october.days[20]) // 21 October
        assertNull(october.days.last()) // 31 October
    }

    @Test
    fun countsLandOnTheDayTheyHappened() {
        val times = listOf(at(LocalDate.of(2026, 10, 1)), at(LocalDate.of(2026, 10, 1)), at(LocalDate.of(2026, 9, 30)))
        val months = activityMonths(times, today, ZoneOffset.UTC)
        assertEquals(2, months.last().days[0]!!.count) // 1 October
        assertEquals(1, months[4].days[29]!!.count) // 30 September
    }

    @Test
    fun daysOutsideTheWindowAreIgnored() {
        val months = activityMonths(listOf(at(LocalDate.of(2026, 4, 30))), today, ZoneOffset.UTC)
        assertTrue(months.flatMap { it.days }.filterNotNull().all { it.count == 0 })
    }

    @Test
    fun theDayIsCountedInTheGivenZone() {
        // 23:30 UTC is already the next day in +02:00.
        val lateEvening = LocalDate.of(2026, 10, 19).atTime(23, 30)
            .atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
        val utc = activityMonths(listOf(lateEvening), today, ZoneOffset.UTC)
        val plusTwo = activityMonths(listOf(lateEvening), today, ZoneOffset.ofHours(2))
        assertEquals(1, utc.last().days[18]!!.count) // 19 October
        assertEquals(0, plusTwo.last().days[18]!!.count)
        assertEquals(1, plusTwo.last().days[19]!!.count) // 20 October
    }

    @Test
    fun theIntensityGrowsWithTheBusiestDay() {
        assertEquals(0, heatLevel(0, 10))
        assertEquals(0, heatLevel(5, 0))
        assertEquals(1, heatLevel(1, 100))
        assertEquals(1, heatLevel(1, 4))
        assertEquals(2, heatLevel(2, 4))
        assertEquals(3, heatLevel(3, 4))
        assertEquals(4, heatLevel(4, 4))
        // Never decreases as the count rises.
        assertTrue((0..20).map { heatLevel(it, 20) }.zipWithNext().all { (a, b) -> a <= b })
    }
}
