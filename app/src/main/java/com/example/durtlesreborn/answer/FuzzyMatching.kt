package com.example.durtlesreborn.answer

import java.util.Locale

/** Optimal string alignment distance: Levenshtein plus transposition of adjacent characters. */
fun optimalStringAlignmentDistance(a: CharSequence, b: CharSequence): Int {
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    val d = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) d[i][0] = i
    for (j in 0..b.length) d[0][j] = j
    for (i in a.indices) {
        for (j in b.indices) {
            val cost = if (a[i] == b[j]) 0 else 1
            d[i + 1][j + 1] = minOf(d[i][j + 1] + 1, d[i + 1][j] + 1, d[i][j] + cost)
            if (i > 0 && j > 0 && a[i] == b[j - 1] && a[i - 1] == b[j]) {
                d[i + 1][j + 1] = minOf(d[i + 1][j + 1], d[i - 1][j - 1] + cost)
            }
        }
    }
    return d[a.length][b.length]
}

/** Port of the original FuzzyMatching, which checks free-text (English) answers. */
object FuzzyMatching {
    private val dashes = intArrayOf(
        0x05BE, 0x1400, 0x2010, 0x2011, 0x2012, 0x2013, 0x2014, 0x2015, 0x2E3A, 0x2E3B,
        0x30FC, 0xFE58, 0xFE63, 0xFF0D, 0x2043, 0x058A,
    )

    private val punctuation: Map<Int, Char> = mapOf(
        0x201A to '\'', 0x201E to '"', 0x301D to '"', 0x301E to '"', 0x301F to '"',
        0x055A to '\'', 0x055B to '\'', 0x05F3 to '\'', 0x05F4 to '"', 0x066B to '.',
        0x066C to ',', 0x2032 to '\'', 0x2033 to '"', 0x2035 to '\'', 0x2036 to '"',
        0xFE10 to ',', 0xFE11 to ',', 0xFE12 to '.', 0xFE16 to '?', 0xFE50 to ',',
        0xFE51 to ',', 0xFE52 to '.', 0xFE54 to ';', 0xFE55 to ':', 0xFE56 to '?',
        0xFE57 to '!', 0xFE5F to '#', 0xFE60 to '&', 0xFE61 to '*', 0xFE68 to '\\',
        0xFE6A to '%', 0xFE6B to '@', 0xFF01 to '!', 0xFF02 to '?', 0xFF03 to '#',
        0xFF05 to '%', 0xFF06 to '&', 0xFF07 to '\'', 0xFF0A to '*', 0xFF0C to ',',
        0xFF0E to '.', 0xFF0F to '/', 0xFF1A to ':', 0xFF1B to ';', 0xFF1F to '?',
        0xFF20 to '@', 0xFF3C to '\\', 0xFF61 to '.', 0xFF64 to ',', 0xFF65 to '.',
        0x00B4 to '\'', 0x02DD to '"', 0x02F4 to '\'', 0x02F5 to '"', 0x02F6 to '"',
        0xFF40 to '\'', 0x2018 to '\'', 0x201B to '\'', 0x201C to '"', 0x201F to '"',
        0x2019 to '\'', 0x201D to '"',
    )

    /**
     * Replaces easily mistyped or 'helpfully' substituted characters with plain ASCII.
     * The original lists the zero-to-nine digits of every script by hand; here any Unicode
     * decimal digit maps to its value, which is the same set plus a few scripts it omitted.
     */
    private fun cleanReplace(cp: Int): Int {
        if (cp < 127) return cp
        if (Character.getType(cp) == Character.DECIMAL_DIGIT_NUMBER.toInt()) {
            val digit = Character.digit(cp, 10)
            if (digit >= 0) return '0'.code + digit
        }
        if (cp in dashes) return '-'.code
        return punctuation[cp]?.code ?: cp
    }

    /** Normalises whitespace, maps look-alike characters to ASCII, trims and lower-cases. */
    internal fun cleanString(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val cp = s.codePointAt(i)
            val type = Character.getType(cp)
            if (type == 0 || type in Character.SPACE_SEPARATOR.toInt()..Character.SURROGATE.toInt()) {
                sb.append(' ')
            } else if (type == Character.DECIMAL_DIGIT_NUMBER.toInt() || type >= Character.DASH_PUNCTUATION.toInt()) {
                sb.appendCodePoint(cleanReplace(cp))
            } else {
                sb.appendCodePoint(cp)
            }
            i += Character.charCount(cp)
        }
        return sb.toString().trim(' ').replace(Regex(" {2,}"), " ").lowercase(Locale.ENGLISH)
    }

    /** Edit distance if within the typo lenience for a reference of this length, else null. */
    private fun matchScore(answer: CharSequence, reference: CharSequence): Int? {
        val threshold = when (reference.length) {
            in 0..3 -> 0
            4, 5 -> 1
            6, 7 -> 2
            else -> reference.length / 7 + 2
        }
        val score = optimalStringAlignmentDistance(answer, reference)
        return if (score <= threshold) score else null
    }

    /**
     * Matches [answer] against the accepted answers within a length-dependent edit distance.
     * It fails if a rejected answer is a strictly better match.
     */
    fun matches(
        answer: String,
        accepted: Iterable<String>,
        rejected: Iterable<String>,
        closeEnoughAction: CloseEnoughAction,
    ): AnswerVerdict {
        val cleanAnswer = cleanString(answer)

        val bestRejected = rejected.mapNotNull { matchScore(cleanAnswer, cleanString(it)) }.minOrNull()

        var bestAccepted: Int? = null
        var bestAcceptedAnswer: String? = null
        for (reference in accepted) {
            val score = matchScore(cleanAnswer, cleanString(reference)) ?: continue
            if (bestAccepted == null || score < bestAccepted) {
                bestAccepted = score
                bestAcceptedAnswer = reference
            }
        }

        if (bestAccepted == null || (bestRejected != null && bestAccepted > bestRejected)) {
            return AnswerVerdict.NOK_WITHOUT_RETRY
        }
        if (bestAccepted == 0) {
            return AnswerVerdict(true, false, false, answer, bestAcceptedAnswer)
        }
        return when (closeEnoughAction) {
            CloseEnoughAction.SHAKE_AND_RETRY -> AnswerVerdict.NOK_WITH_RETRY
            CloseEnoughAction.REJECT -> AnswerVerdict.NOK_WITHOUT_RETRY
            else -> AnswerVerdict(true, false, true, answer, bestAcceptedAnswer)
        }
    }
}
