package com.example.durtlesreborn.radicals

import android.graphics.Bitmap
import android.graphics.Canvas
import com.caverock.androidsvg.SVG
import com.example.durtlesreborn.audio.isWaniKaniAudioUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class RadicalImage(val slug: String, val url: String)

private const val SIZE = 256

private val cssVariable = Regex("""var\(--[^,)]+,\s*([^)]+)\)""")

/** WaniKani's SVGs set their colour with `var(--color-text, #000)`, which the renderer cannot read. */
internal fun withoutCssVariables(svg: String) = cssVariable.replace(svg) { it.groupValues[1] }

/**
 * Pictures of the radicals that have no characters, kept on the phone so they work offline. Nothing
 * is bundled and no API token is sent: the files are public, on WaniKani's own host.
 */
class RadicalImages(private val http: OkHttpClient, private val directory: File) {
    private val memory = ConcurrentHashMap<String, Bitmap>()

    private fun file(slug: String) = File(directory, slug.replace(Regex("[^a-z0-9-]"), "_") + ".svg")

    /** The picture to tint and draw, or null if it is not on the phone yet. */
    suspend fun bitmap(slug: String): Bitmap? = memory[slug] ?: withContext(Dispatchers.IO) {
        runCatching { file(slug).takeIf { it.isFile }?.let(::render) }.getOrNull()?.also { memory[slug] = it }
    }

    /** Downloads what is missing. True when every picture is now on the phone. */
    suspend fun fetchMissing(images: List<RadicalImage>): Boolean = withContext(Dispatchers.IO) {
        directory.mkdirs()
        images.map { image ->
            val target = file(image.slug)
            target.isFile || (isWaniKaniAudioUrl(image.url) && runCatching { download(image.url, target) }.getOrDefault(false))
        }.all { it }
    }

    private fun download(url: String, target: File): Boolean {
        http.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return false
            val part = File(target.path + ".part")
            part.outputStream().use { response.body.byteStream().copyTo(it) }
            return part.renameTo(target)
        }
    }

    private fun render(file: File): Bitmap {
        val svg = SVG.getFromString(withoutCssVariables(file.readText()))
        svg.setDocumentWidth(SIZE.toFloat())
        svg.setDocumentHeight(SIZE.toFloat())
        return Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).also { svg.renderToCanvas(Canvas(it)) }
    }
}
