package com.example.durtlesreborn.ui

import com.example.durtlesreborn.data.DayForecast
import com.example.durtlesreborn.data.HourRow
import com.example.durtlesreborn.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TsurukameUiTest {
    @Test
    fun markupBecomesStyledTextWithoutTheTags() {
        val text = markupToAnnotated("The <radical>stick</radical> and <kanji>tree</kanji>.", LightTk)
        assertEquals("The stick and tree.", text.text)
        assertEquals(2, text.spanStyles.size)
        assertEquals(LightTk.markupRadical.second, text.spanStyles[0].item.background)
        assertEquals("stick", text.text.substring(text.spanStyles[0].start, text.spanStyles[0].end))
        assertEquals(LightTk.markupKanji.second, text.spanStyles[1].item.background)
    }

    @Test
    fun markupDecodesEntitiesAndLineBreaks() {
        assertEquals("a <b>\nc & d", markupToAnnotated("a &lt;b&gt;<br>c &amp; d", LightTk).text)
    }

    @Test
    fun strayClosingTagsAreIgnored() {
        assertEquals("ok", markupToAnnotated("</kanji>ok</radical>", LightTk).text)
    }

    @Test
    fun aSentenceHighlightsTheWordOrItsStem() {
        val whole = highlightWord("\u7D44\u7E54\u3092\u4F5C\u308B", "\u7D44\u7E54")
        assertEquals("\u7D44\u7E54", whole.text.substring(whole.spanStyles[0].start, whole.spanStyles[0].end))
        val inflected = highlightWord("\u672C\u3092\u8AAD\u3093\u3060", "\u8AAD\u3080")
        assertEquals("\u8AAD", inflected.text.substring(inflected.spanStyles[0].start, inflected.spanStyles[0].end))
        assertEquals("\u672C\u3092\u8AAD\u3093\u3060", inflected.text)
    }

    @Test
    fun everyReviewOrderIsRecognisedOnceChosen() {
        for ((name, apply) in tsurukameReviewOrders) assertEquals(name, tsurukameReviewOrderName(apply(AppSettings())))
    }

    @Test
    fun otherCombinationsAreCustom() {
        assertNull(tsurukameReviewOrderName(AppSettings(reviewOrderPriority = "RADICALS_FIRST")))
    }

    @Test
    fun theChartStartsWithTheNextHour() {
        val hour = 3_600_000L
        val now = 500 * hour + hour / 2
        val days = listOf(
            DayForecast(0, listOf(HourRow(501 * hour, 3, 3), HourRow(503 * hour, 2, 5))),
            DayForecast(0, listOf(HourRow(548 * hour, 7, 12), HourRow(549 * hour, 9, 21))),
        )
        val hours = upcomingHours(days, now)
        assertEquals(48, hours.size)
        assertEquals(3, hours[0])
        assertEquals(0, hours[1])
        assertEquals(2, hours[2])
        assertEquals(7, hours[47])
        assertEquals(12, hours.sum())
    }
}
