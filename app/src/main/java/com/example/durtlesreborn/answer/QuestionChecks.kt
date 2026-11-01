package com.example.durtlesreborn.answer

import com.example.durtlesreborn.kana.Kana

data class Meaning(val text: String, val acceptedAnswer: Boolean = true)

/** An auxiliary meaning is either a whitelist (accepted) or a blacklist (rejected) entry. */
data class AuxiliaryMeaning(val text: String, val whitelist: Boolean)

/** The parts of a subject that answer checking needs. */
data class SubjectAnswers(
    val meanings: List<Meaning> = emptyList(),
    val auxiliaryMeanings: List<AuxiliaryMeaning> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val readings: List<Reading> = emptyList(),
) {
    val acceptedReadings get() = readings.filter { it.acceptedAnswer }
    val onYomiReadings get() = readings.filter { it.isOnYomi }
    val kunYomiReadings get() = readings.filter { it.isKunYomi }
    val hasAcceptedOnYomi get() = readings.any { it.acceptedAnswer && it.isOnYomi }
    val hasAcceptedKunYomi get() = readings.any { it.acceptedAnswer && it.isKunYomi }

    /** The reading WaniKani accepts, shown as furigana once the answer is in. Null if none is accepted. */
    val confirmationReading: String?
        get() = (readings.firstOrNull { it.acceptedAnswer && it.primary } ?: readings.firstOrNull { it.acceptedAnswer })?.reading
}

/** Ports of the checkAnswer implementations of the original QuestionType enum. */
object QuestionChecks {
    /**
     * Meaning question for radicals, kanji and vocabulary (the original's four copies are identical).
     * An answer that is a valid reading of the subject typed in romaji is a retry, not a failure.
     */
    fun meaning(subject: SubjectAnswers, answer: String, action: CloseEnoughAction): AnswerVerdict {
        val accepted = HashSet<String>()
        val rejected = HashSet<String>()
        for (m in subject.meanings) (if (m.acceptedAnswer) accepted else rejected).add(m.text)
        for (m in subject.auxiliaryMeanings) (if (m.whitelist) accepted else rejected).add(m.text)
        accepted.addAll(subject.synonyms)

        val verdict = FuzzyMatching.matches(answer, accepted, rejected, action)
        if (!verdict.ok && !verdict.retry) {
            val kanaAnswer = Kana.simulateInput(answer)
            if (subject.readings.any { it.matches(kanaAnswer, false) }) return AnswerVerdict.NOK_WITH_RETRY
        }
        return verdict
    }

    private fun ok(answer: String, r: Reading) = AnswerVerdict(true, false, false, answer, r.reading)

    /** Digraph near-misses are wrong but carry the match so the UI can explain the small-kana rule. */
    private fun digraph(answer: String, candidates: List<Reading>): AnswerVerdict? {
        for (r in candidates) {
            val match = r.matchesForDigraph(answer) ?: continue
            return AnswerVerdict(false, false, false, answer, r.reading, digraphMatch = match)
        }
        return null
    }

    fun kanjiReading(subject: SubjectAnswers, answer: String, requireOnInKatakana: Boolean): AnswerVerdict {
        subject.readings.firstOrNull { it.acceptedAnswer && it.matches(answer, requireOnInKatakana) }?.let {
            return ok(answer, it)
        }
        subject.readings.firstOrNull { !it.acceptedAnswer && it.matches(answer, requireOnInKatakana) }?.let {
            return AnswerVerdict(false, true, false, answer, it.reading)
        }
        return digraph(answer, subject.acceptedReadings) ?: AnswerVerdict.NOK_WITHOUT_RETRY
    }

    /** [matchingKanji] is the kanji the vocabulary was built from; its readings give a retry. */
    fun vocabularyReading(
        subject: SubjectAnswers,
        matchingKanji: SubjectAnswers?,
        answer: String,
        requireOnInKatakana: Boolean,
    ): AnswerVerdict {
        subject.readings.firstOrNull { it.acceptedAnswer && it.matches(answer, requireOnInKatakana) }?.let {
            return ok(answer, it)
        }
        subject.readings.firstOrNull { !it.acceptedAnswer && it.matches(answer, requireOnInKatakana) }?.let {
            return AnswerVerdict(false, true, false, answer, it.reading)
        }
        if (matchingKanji?.readings?.any { it.matches(answer, requireOnInKatakana) } == true) {
            return AnswerVerdict.NOK_WITH_RETRY
        }
        return digraph(answer, subject.readings) ?: AnswerVerdict.NOK_WITHOUT_RETRY
    }

    fun kanjiOnYomi(subject: SubjectAnswers, answer: String, requireOnInKatakana: Boolean): AnswerVerdict =
        kanjiSpecificReading(subject, subject.onYomiReadings, subject.hasAcceptedOnYomi, answer, requireOnInKatakana, allReadingsForDigraph = true)

    fun kanjiKunYomi(subject: SubjectAnswers, answer: String, requireOnInKatakana: Boolean): AnswerVerdict =
        kanjiSpecificReading(subject, subject.kunYomiReadings, subject.hasAcceptedKunYomi, answer, requireOnInKatakana, allReadingsForDigraph = false)

    /**
     * On'yomi and kun'yomi questions. If the kanji has no accepted reading of the asked type,
     * any reading of that type counts. A reading of the other type is a retry.
     * The original checks digraphs against all readings for on'yomi and accepted ones for kun'yomi.
     */
    private fun kanjiSpecificReading(
        subject: SubjectAnswers,
        ofType: List<Reading>,
        hasAcceptedOfType: Boolean,
        answer: String,
        requireOnInKatakana: Boolean,
        allReadingsForDigraph: Boolean,
    ): AnswerVerdict {
        ofType.firstOrNull { it.acceptedAnswer && it.matches(answer, requireOnInKatakana) }?.let {
            return ok(answer, it)
        }
        if (!hasAcceptedOfType) {
            ofType.firstOrNull { it.matches(answer, requireOnInKatakana) }?.let { return ok(answer, it) }
        }
        if (subject.readings.any { it.matches(answer, requireOnInKatakana) }) return AnswerVerdict.NOK_WITH_RETRY
        val candidates = if (allReadingsForDigraph) subject.readings else subject.acceptedReadings
        return digraph(answer, candidates) ?: AnswerVerdict.NOK_WITHOUT_RETRY
    }
}
