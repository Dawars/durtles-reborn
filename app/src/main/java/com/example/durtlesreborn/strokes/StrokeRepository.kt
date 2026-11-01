package com.example.durtlesreborn.strokes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Stroke order from KanjiVG (Ulrich Apel, CC BY-SA 3.0, https://kanjivg.tagaini.net), kept in
 * assets/strokes.txt as one line per character: the character, a tab, then the SVG path of each
 * stroke in drawing order separated by `|`. Paths use KanjiVG's 109 by 109 box.
 */
class StrokeRepository(private val open: () -> InputStream) {
    private val table: Map<String, String> by lazy { open().use { parseStrokeTable(it.bufferedReader(Charsets.UTF_8).readText()) } }

    /** The stroke paths of a single character, or null if it is not a single character or has no data. */
    suspend fun strokesFor(characters: String): List<String>? = withContext(Dispatchers.Default) {
        if (characters.isEmpty() || characters.codePointCount(0, characters.length) != 1) return@withContext null
        table[characters]?.split('|')
    }
}

internal fun parseStrokeTable(text: String): Map<String, String> {
    val result = HashMap<String, String>()
    for (line in text.lineSequence()) {
        val tab = line.indexOf('\t')
        if (tab <= 0 || tab == line.length - 1) continue
        result[line.substring(0, tab)] = line.substring(tab + 1)
    }
    return result
}
