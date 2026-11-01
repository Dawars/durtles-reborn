package com.example.durtlesreborn.audio

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AudioCacheTest {
    private lateinit var server: MockWebServer
    private lateinit var directory: File

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        directory = Files.createTempDirectory("audio").toFile()
    }

    @After
    fun tearDown() {
        server.shutdown()
        directory.deleteRecursively()
    }

    private fun cache(allowed: (String) -> Boolean = { true }) = AudioCache(OkHttpClient(), directory, allowed)

    @Test
    fun aClipIsDownloadedOnceIntoItsLevelAndThenReusedWithoutTheNetwork() = runBlocking {
        server.enqueue(MockResponse().setBody("mp3-bytes"))
        val url = server.url("/a.mp3").toString()
        val cache = cache()
        val first = cache.fetch(url, level = 7)
        assertNotNull(first)
        assertEquals("mp3-bytes", first!!.readText())
        assertTrue(first.path.replace('\\', '/').contains("/level-7/"))
        assertEquals(first, cache.fetch(url, level = 7))
        assertEquals(1, server.requestCount)
        assertEquals(LevelStats(1, 9), cache.storedByLevel()[7])
        assertEquals(1 to 9L, cache.stored())
    }

    @Test
    fun levelsAreCountedAndDeletedOnTheirOwn() = runBlocking {
        server.enqueue(MockResponse().setBody("aa"))
        server.enqueue(MockResponse().setBody("bbbb"))
        val cache = cache()
        cache.fetch(server.url("/a.mp3").toString(), level = 3)
        cache.fetch(server.url("/b.mp3").toString(), level = 4)
        assertEquals(setOf(3, 4), cache.storedByLevel().keys)
        assertEquals(2, cache.stored().first)
        cache.deleteLevel(3)
        assertEquals(setOf(4), cache.storedByLevel().keys)
        assertEquals(1 to 4L, cache.stored())
    }

    @Test
    fun theDownloadedThroughMarkerRemembersTheLastLevel() {
        val cache = cache()
        assertEquals(0, cache.downloadedThrough())
        cache.setDownloadedThrough(12)
        assertEquals(12, cache.downloadedThrough())
    }

    @Test
    fun theOldFlatLayoutIsClearedButOtherFilesStay() {
        val cache = cache()
        File(directory, "abc.mp3").writeText("old")
        File(directory, "abc.mp3.part").writeText("partial")
        File(directory, "notes.txt").writeText("keep")
        cache.clearLegacyLayout()
        assertFalse(File(directory, "abc.mp3").exists())
        assertFalse(File(directory, "abc.mp3.part").exists())
        assertTrue(File(directory, "notes.txt").exists())
    }

    @Test
    fun aFailedDownloadLeavesNothingBehind() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        assertNull(cache().fetch(server.url("/bad.mp3").toString(), level = 1))
        assertEquals(0, cache().stored().first)
        assertTrue(directory.walk().none { it.isFile && it.name.endsWith(".part") })
    }

    @Test
    fun aHostThatIsNotAllowedIsRefusedBeforeAnyRequest() = runBlocking {
        val url = server.url("/a.mp3").toString()
        assertNull(cache({ false }).fetch(url, level = 1))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun onlyHttpsWaniKaniHostsCountAsAudio() {
        assertTrue(isWaniKaniAudioUrl("https://files.wanikani.com/a.mp3"))
        assertTrue(isWaniKaniAudioUrl("https://wanikani.com/a.mp3"))
        assertFalse(isWaniKaniAudioUrl("http://files.wanikani.com/a.mp3"))
        assertFalse(isWaniKaniAudioUrl("https://example.com/a.mp3"))
        assertFalse(isWaniKaniAudioUrl("https://notwanikani.com/a.mp3"))
        assertFalse(isWaniKaniAudioUrl("not a url"))
    }

    @Test
    fun aLevelDownloadKeepsGoingAfterAFailureAndReportsProgress() = runBlocking {
        server.enqueue(MockResponse().setBody("a"))
        server.enqueue(MockResponse().setResponseCode(404))
        val urls = listOf(server.url("/a.mp3").toString(), server.url("/b.mp3").toString())
        val seen = mutableListOf<Triple<Int, Int, Int>>()
        val failed = AudioDownloader(cache(), parallel = 2).downloadAll(urls, level = 2) { done, total, failures ->
            seen += Triple(done, total, failures)
        }
        assertEquals(Triple(0, 2, 0), seen.first())
        assertEquals(Triple(2, 2, 1), seen.last())
        assertEquals(1, failed)
        assertEquals(1, cache().storedByLevel().getValue(2).clips)
    }
}
