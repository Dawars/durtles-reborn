package com.example.durtlesreborn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Pitch accent for one word and reading. [positions] are the accent positions the source lists:
 * 0 is heiban (no drop), 1 drops after the first mora, and n drops after the nth mora.
 * Data: Kanjium (https://github.com/mifunetoshiro/kanjium), CC BY-SA 4.0, by Uros O.
 */
data class PitchAccent(val reading: String, val positions: List<Int>)

/**
 * Looks words up in the bundled Kanjium accents file: `word<TAB>reading<TAB>positions`, one line
 * per reading, 124,137 words in all. Parsed once per process on a background thread the first
 * time an item is opened, and kept as one compact string per word so the table stays small.
 */
class PitchRepository(private val open: () -> InputStream) {
    /** word -> `reading:positions|reading:positions`. */
    private val table: Map<String, String> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { load() }

    /** Results of the kana-only fallback, by word. */
    private val fallbacks = HashMap<String, List<PitchAccent>>()
    private val fallbackLock = Any()

    /**
     * The accents for [word] read as any of [readings], in the order of [readings]. The source
     * spells words the way a dictionary does, so the word and the reading must both match;
     * kana-only words fall back to the reading when that reading has a single pattern in the
     * source (おはよう is listed as お早う), never guessing between homophones.
     */
    /** Reads the table ahead of time, so opening the first item does not wait for it. */
    suspend fun preload() {
        withContext(Dispatchers.Default) { table.size }
    }

    suspend fun lookup(word: String, readings: List<String>): List<PitchAccent> = withContext(Dispatchers.Default) {
        val wanted = readings.filter { it.isNotBlank() }.distinct()
        if (wanted.isEmpty()) return@withContext emptyList()
        val encoded = table[word]
        when {
            encoded != null -> decode(encoded, wanted)
            isKana(word) -> fallbackFor(word, wanted)
            else -> emptyList()
        }
    }

    private fun decode(encoded: String, wanted: List<String>): List<PitchAccent> {
        val byReading = HashMap<String, List<Int>>(4)
        for (part in encoded.split('|')) {
            val colon = part.lastIndexOf(':')
            if (colon <= 0) continue
            byReading[part.substring(0, colon)] = part.substring(colon + 1).split(',')
                .mapNotNull { it.trim().toIntOrNull() }
        }
        return wanted.mapNotNull { reading -> byReading[reading]?.let { PitchAccent(reading, it) } }
    }

    /**
     * The source does not have [word] itself, but may have the same reading under its dictionary
     * spelling. Only a reading whose entries all agree on one pattern is used: はい has a dozen
     * spellings with different accents, so nothing is shown rather than a wrong guess.
     */
    private fun fallbackFor(word: String, wanted: List<String>): List<PitchAccent> = synchronized(fallbackLock) {
        fallbacks[word]?.let { return it }
        val wantedSet = wanted.toSet()
        val patterns = HashMap<String, MutableList<List<Int>>>(4)
        open().bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (line in lines) {
                val first = line.indexOf('\t')
                if (first <= 0) continue
                val second = line.indexOf('\t', first + 1)
                if (second <= first) continue
                val reading = line.substring(first + 1, second)
                if (reading !in wantedSet) continue
                val positions = line.substring(second + 1).trim().split(',')
                    .mapNotNull { it.trim().toIntOrNull() }
                if (positions.isNotEmpty()) patterns.getOrPut(reading) { ArrayList(2) }.add(positions)
            }
        }
        val result = wanted.mapNotNull { reading ->
            val found = patterns[reading]?.distinct() ?: return@mapNotNull null
            if (found.size == 1) PitchAccent(reading, found.single()) else null
        }
        fallbacks[word] = result
        result
    }

    private fun load(): Map<String, String> {
        val map = HashMap<String, String>(200_000)
        open().bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (line in lines) {
                val first = line.indexOf('\t')
                if (first <= 0) continue
                val second = line.indexOf('\t', first + 1)
                if (second <= first) continue
                val positions = line.substring(second + 1).trim()
                if (positions.isEmpty()) continue
                val word = line.substring(0, first)
                val entry = line.substring(first + 1, second) + ':' + positions
                val old = map[word]
                map[word] = if (old == null) entry else "$old|$entry"
            }
        }
        return map
    }
}

/** Whether the text is entirely kana, which is how kana-only vocabulary is written. */
private fun isKana(text: String) = text.isNotEmpty() && text.all { it in 'ぁ'..'ゖ' || it in 'ァ'..'ヺ' || it == 'ー' }

/** Small kana that belong to the mora before them, in hiragana and katakana. */
private val SMALL_KANA = setOf(
    'ぁ', 'ぃ', 'ぅ', 'ぇ', 'ぉ', 'ゃ', 'ゅ', 'ょ', 'ゎ', 'ゕ', 'ゖ',
    'ァ', 'ィ', 'ゥ', 'ェ', 'ォ', 'ャ', 'ュ', 'ョ', 'ヮ', 'ヵ', 'ヶ',
)

/**
 * Splits a kana reading into morae, which is what pitch patterns count: きょう is きょ + う,
 * がっこう is が + っ + こ + う. Anything that is not kana (a stray mark) counts on its own.
 */
fun morae(reading: String): List<String> {
    val out = ArrayList<String>(reading.length)
    for (ch in reading) {
        if (out.isNotEmpty() && ch in SMALL_KANA) {
            out[out.lastIndex] += ch
        } else {
            out += ch.toString()
        }
    }
    return out
}

/**
 * The high/low pattern of one accent position, one letter per mora (H high, L low).
 * 0 is heiban: the first mora is low and the rest are high. 1 is atamadaka: the first mora is
 * high and the rest are low. n drops after the nth mora. A position past the last mora, which
 * some of the source's entries carry, is read as a drop after the last mora.
 */
fun pitchPattern(moraCount: Int, accent: Int): String {
    if (moraCount <= 0) return ""
    val drop = accent.coerceIn(0, moraCount)
    return buildString(moraCount) {
        for (mora in 1..moraCount) {
            val high = when {
                drop == 0 -> mora > 1
                drop == 1 -> mora == 1
                else -> mora in 2..drop
            }
            append(if (high) 'H' else 'L')
        }
    }
}
