package com.example.durtlesreborn.notify

import com.example.durtlesreborn.data.AlertCounts
import com.example.durtlesreborn.data.AlertDao
import com.example.durtlesreborn.data.FakeState
import com.example.durtlesreborn.data.SyncEngine
import com.example.durtlesreborn.data.SyncStateEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPolicyTest {
    private val hour = 3_600_000L
    private val now = 10 * hour + 5 * 60_000

    private fun decide(
        current: AlertContext,
        last: AlertContext = AlertContext(),
        frequency: NotificationFrequency = NotificationFrequency.ONLY_NEW_REVIEWS,
        shown: Boolean = false,
        lastUpdate: Long = 0,
        enabled: Boolean = true,
    ) = NotificationPolicy.decide(enabled, frequency, now, shown, last, lastUpdate, current)

    private fun ctx(lessons: Int = 0, reviews: Int = 0, newest: Long = 0) = AlertContext(lessons, reviews, newest)

    @Test
    fun disabledNeverPosts() {
        assertEquals(NotificationDecision(false, false), decide(ctx(reviews = 5, newest = 9 * hour), enabled = false))
    }

    @Test
    fun newReviewsPostWithSound() {
        val d = decide(ctx(reviews = 5, newest = 10 * hour), last = ctx(reviews = 2, newest = 9 * hour))
        assertEquals(NotificationDecision(post = true, sound = true), d)
    }

    @Test
    fun nothingNewDoesNotPostInTheDefaultMode() {
        val same = ctx(reviews = 5, newest = 9 * hour)
        assertEquals(NotificationDecision(false, false), decide(same, last = same.copy(numReviews = 3)))
    }

    @Test
    fun finishingEverythingPostsTheCancellation() {
        val d = decide(ctx(), last = ctx(reviews = 4, newest = 9 * hour), shown = true)
        assertTrue(d.post)
        assertFalse(d.sound)
    }

    @Test
    fun aStaleNotificationIsRemovedEvenWithoutAChange() {
        assertTrue(decide(ctx(), last = ctx(), shown = true).post)
    }

    @Test
    fun oncePerHourUpdatesQuietlyWhenCountsChangedInANewHour() {
        val last = ctx(reviews = 5, newest = 9 * hour)
        val current = ctx(reviews = 4, newest = 9 * hour)
        val d = decide(current, last, NotificationFrequency.ONCE_PER_HOUR, lastUpdate = 9 * hour)
        assertEquals(NotificationDecision(post = true, sound = false), d)
        // Same hour as the last update: wait.
        assertFalse(decide(current, last, NotificationFrequency.ONCE_PER_HOUR, lastUpdate = 10 * hour).post)
        // New hour but nothing changed: nothing to say.
        assertFalse(decide(last, last, NotificationFrequency.ONCE_PER_HOUR, lastUpdate = 9 * hour).post)
    }

    @Test
    fun continuouslyUpdatesOnEveryCountChange() {
        val last = ctx(reviews = 5, newest = 9 * hour)
        assertTrue(decide(ctx(reviews = 4, newest = 9 * hour), last, NotificationFrequency.CONTINUOUSLY, lastUpdate = 10 * hour).post)
        assertFalse(decide(last, last, NotificationFrequency.CONTINUOUSLY).post)
        assertTrue(decide(ctx(lessons = 1, reviews = 5, newest = 9 * hour), last, NotificationFrequency.CONTINUOUSLY).post)
    }

    @Test
    fun messageMentionsLessonsOnlyWhenThereAreSome() {
        assertEquals("New reviews available" to "12 reviews available", NotificationPolicy.message(ctx(reviews = 12)))
        assertEquals(
            "New lessons and reviews available" to "3 lessons and 12 reviews available",
            NotificationPolicy.message(ctx(lessons = 3, reviews = 12)),
        )
    }
}

class NotificationUpdaterTest {
    private val hour = 3_600_000L
    private var now = 10 * hour + 60_000
    private var counts = AlertCounts(0, 0, null, null)
    private val state = FakeState()
    private var enabled = true
    private var frequency = NotificationFrequency.ONLY_NEW_REVIEWS
    private var canShow = true

    private class Shown(val title: String, val text: String, val sound: Boolean)

    private val shown = mutableListOf<Shown>()
    private var cancels = 0

    private val sink = object : NotificationSink {
        override fun show(title: String, text: String, sound: Boolean, settings: NotificationSettings): Boolean {
            if (!canShow) return false
            shown += Shown(title, text, sound)
            return true
        }

        override fun cancel() { cancels++ }
    }

    private val dao = object : AlertDao {
        var lastMaxLevel = -1
        override suspend fun counts(maxLevel: Int, now: Long): AlertCounts {
            lastMaxLevel = maxLevel
            return counts
        }
    }

    private fun updater() = NotificationUpdater(dao, state, { NotificationSettings(enabled, frequency) }, sink, { now })

    @Test
    fun postsOnceForNewReviewsThenStaysQuiet() = runBlocking {
        counts = AlertCounts(0, 7, 10 * hour, 12 * hour)
        updater().update()
        assertEquals(1, shown.size)
        assertEquals("7 reviews available", shown[0].text)
        assertTrue(shown[0].sound)
        now += 30 * 60_000
        updater().update()
        assertEquals(1, shown.size)
    }

    @Test
    fun cancelsWhenEverythingIsDone() = runBlocking {
        counts = AlertCounts(0, 7, 10 * hour, null)
        updater().update()
        counts = AlertCounts(0, 0, null, 14 * hour)
        updater().update()
        assertEquals(1, cancels)
        // And not again afterwards.
        updater().update()
        assertEquals(1, cancels)
    }

    @Test
    fun aNewHourWithMoreReviewsPostsAgainWithSound() = runBlocking {
        counts = AlertCounts(0, 7, 10 * hour, null)
        updater().update()
        now += hour
        counts = AlertCounts(0, 12, 11 * hour, null)
        updater().update()
        assertEquals(2, shown.size)
        assertEquals("12 reviews available", shown[1].text)
        assertTrue(shown[1].sound)
    }

    @Test
    fun disabledDoesNothing() = runBlocking {
        enabled = false
        counts = AlertCounts(0, 7, 10 * hour, null)
        updater().update()
        assertTrue(shown.isEmpty())
        assertEquals(0, cancels)
    }

    @Test
    fun aMissingPermissionIsRetriedOnceItIsGranted() = runBlocking {
        counts = AlertCounts(0, 7, 10 * hour, null)
        canShow = false
        updater().update()
        assertTrue(shown.isEmpty())
        canShow = true
        updater().update()
        assertEquals(1, shown.size)
    }

    @Test
    fun usesTheSavedLevelLimitOrTheFreePlanDefault() = runBlocking {
        updater().update()
        assertEquals(3, dao.lastMaxLevel)
        state.put(SyncStateEntity(SyncEngine.KEY_MAX_LEVEL, "60"))
        updater().update()
        assertEquals(60, dao.lastMaxLevel)
    }
}
