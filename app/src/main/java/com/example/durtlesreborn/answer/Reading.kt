package com.example.durtlesreborn.answer

import com.example.durtlesreborn.kana.Kana

/** A regular kana typed where a small one was expected, or the reverse. */
data class DigraphMatch(val regularKana: Char, val smallKana: Char)

data class Reading(
    val reading: String,
    val type: String? = null,
    val acceptedAnswer: Boolean = false,
    val primary: Boolean = false,
) {
    val isOnYomi get() = type == "onyomi"
    val isKunYomi get() = type == "kunyomi"

    /** On'yomi may be typed in hiragana or katakana unless [requireOnInKatakana] is set. */
    fun matches(answer: String, requireOnInKatakana: Boolean): Boolean {
        if (!isOnYomi) return reading == answer
        val katakana = Kana.toKatakana(reading)
        return if (requireOnInKatakana) katakana == answer else reading == answer || katakana == answer
    }

    fun matchesForDigraph(answer: String): DigraphMatch? {
        if (!isOnYomi) return digraphMatch(answer, reading)
        return digraphMatch(answer, reading) ?: digraphMatch(answer, Kana.toKatakana(reading))
    }

    private companion object {
        const val SMALL_KANA = "ぁぃぅぇぉっゃゅょゎゕゖァィゥェォッャュョヮヵヶ"
        const val REGULAR_KANA = "あいうえおつやゆよわかけアイウエオツヤユヨワカケ"

        fun digraphMatch(answer: String, baseline: String): DigraphMatch? {
            if (answer.length != baseline.length) return null
            var regular = 0.toChar()
            var small = 0.toChar()
            for (i in answer.indices) {
                val c1 = answer[i]
                val c2 = baseline[i]
                if (c1 == c2) continue
                val p1 = REGULAR_KANA.indexOf(c1)
                if (p1 >= 0 && SMALL_KANA[p1] == c2) {
                    regular = c1
                    small = c2
                    continue
                }
                val p2 = SMALL_KANA.indexOf(c1)
                if (p2 >= 0 && REGULAR_KANA[p2] == c2) {
                    regular = c2
                    small = c1
                    continue
                }
                return null
            }
            return if (regular != 0.toChar()) DigraphMatch(regular, small) else null
        }
    }
}
