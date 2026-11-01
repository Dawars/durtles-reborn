package com.example.durtlesreborn.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

/** WaniKani serves pronunciation clips from its own file host over HTTPS; nothing else is fetched. */
internal fun isWaniKaniAudioUrl(url: String): Boolean {
    val parsed = url.toHttpUrlOrNull() ?: return false
    return parsed.isHttps && (parsed.host == "wanikani.com" || parsed.host.endsWith(".wanikani.com"))
}

/** How many clips one level takes, and how much space. */
data class LevelStats(val clips: Int, val bytes: Long)

/**
 * Pronunciation clips, kept one folder per level (`level-<n>/<hash>.mp3`) so a level's audio can be
 * measured and deleted on its own. Nothing is bundled with the app and no API token is sent: the
 * files are public, and they only cover vocabulary because that is all WaniKani records.
 */
class AudioCache(
    private val http: OkHttpClient,
    private val directory: File,
    private val allowed: (String) -> Boolean = ::isWaniKaniAudioUrl,
) {
    /** The clip of [level], if it is already on the phone. */
    fun existing(url: String, level: Int): File? = file(url, level).takeIf { it.isFile && it.length() > 0 }

    /** The clip, downloading it into its level folder first when needed. */
    suspend fun fetch(url: String, level: Int): File? {
        existing(url, level)?.let { return it }
        if (!allowed(url)) return null
        return withContext(Dispatchers.IO) { download(url, level) }
    }

    /** Clips and bytes per level, leaving out levels with nothing stored. */
    fun storedByLevel(): Map<Int, LevelStats> = directory.listFiles()
        ?.filter { it.isDirectory && it.name.startsWith(LEVEL_PREFIX) }
        ?.mapNotNull { dir ->
            val level = dir.name.removePrefix(LEVEL_PREFIX).toIntOrNull() ?: return@mapNotNull null
            val stats = statsOf(dir)
            if (stats.clips == 0) null else level to stats
        }
        ?.toMap()
        ?: emptyMap()

    /** Everything stored, across all levels. */
    fun stored(): Pair<Int, Long> = storedByLevel().values.fold(0 to 0L) { (clips, bytes), level ->
        clips + level.clips to bytes + level.bytes
    }

    fun deleteLevel(level: Int) {
        levelDir(level).deleteRecursively()
    }

    /** The highest level the automatic download has finished; 0 when none has. */
    fun downloadedThrough(): Int =
        runCatching { File(directory, THROUGH_FILE).readText().trim().toInt() }.getOrDefault(0)

    fun setDownloadedThrough(level: Int) {
        directory.mkdirs()
        runCatching { File(directory, THROUGH_FILE).writeText(level.toString()) }
    }

    /** The first bulk downloader kept clips flat in [directory]; they cannot be attributed to a level. */
    fun clearLegacyLayout() {
        val files = directory.listFiles() ?: return
        for (file in files) {
            if (file.isFile && (file.name.endsWith(".mp3") || file.name.endsWith(".part"))) file.delete()
        }
    }

    private fun statsOf(dir: File): LevelStats {
        val files = dir.listFiles()?.filter { it.isFile && it.name.endsWith(".mp3") } ?: return LevelStats(0, 0)
        return LevelStats(files.size, files.sumOf { it.length() })
    }

    private fun download(url: String, level: Int): File? = runCatching {
        val dir = levelDir(level)
        dir.mkdirs()
        val target = File(dir, name(url))
        val partial = File(dir, target.name + ".part")
        partial.delete()
        http.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body.byteStream().use { input ->
                partial.outputStream().use { output -> input.copyTo(output) }
            }
        }
        if (partial.length() > 0 && partial.renameTo(target)) {
            target
        } else {
            partial.delete()
            null
        }
    }.getOrNull()

    private fun levelDir(level: Int) = File(directory, "$LEVEL_PREFIX$level")

    private fun file(url: String, level: Int) = File(levelDir(level), name(url))

    private fun name(url: String): String =
        MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString("") { "%02x".format(it) } + ".mp3"

    private companion object {
        const val LEVEL_PREFIX = "level-"
        const val THROUGH_FILE = ".downloaded_through"
    }
}
