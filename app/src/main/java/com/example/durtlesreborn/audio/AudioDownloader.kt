package com.example.durtlesreborn.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Downloads many clips a few at a time. Clips already on the phone are skipped, so running it
 * again picks up where an earlier run stopped. [onProgress] is called after every batch with the
 * number of clips handled, the total and how many could not be fetched.
 */
class AudioDownloader(private val cache: AudioCache, private val parallel: Int = 6) {
    /** Downloads the clips of [level]. Returns how many could not be fetched. */
    suspend fun downloadAll(urls: List<String>, level: Int, onProgress: (done: Int, total: Int, failed: Int) -> Unit): Int {
        val total = urls.size
        var done = 0
        var failed = 0
        onProgress(done, total, failed)
        for (batch in urls.chunked(parallel)) {
            currentCoroutineContext().ensureActive()
            val results = coroutineScope {
                batch.map { url -> async(Dispatchers.IO) { cache.fetch(url, level) != null } }.awaitAll()
            }
            done += batch.size
            failed += results.count { !it }
            onProgress(done, total, failed)
        }
        return failed
    }
}
