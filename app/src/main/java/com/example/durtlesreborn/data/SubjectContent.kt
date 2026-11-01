package com.example.durtlesreborn.data

import com.example.durtlesreborn.answer.AuxiliaryMeaning
import com.example.durtlesreborn.answer.Meaning
import com.example.durtlesreborn.answer.Reading
import com.example.durtlesreborn.answer.SubjectAnswers
import com.example.durtlesreborn.api.apiJson
import com.example.durtlesreborn.session.SubjectKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

/** What the app reads out of a stored subject's JSON. */
class SubjectContent(private val data: JsonObject) {
    internal val rawData: JsonObject get() = data

    val srsSystemId: Long? get() = (data["spaced_repetition_system_id"] as? JsonPrimitive)?.longOrNull

    private fun objects(name: String): List<JsonObject> =
        (data[name] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

    private fun JsonObject.str(name: String) = (this[name] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.flag(name: String) = (this[name] as? JsonPrimitive)?.booleanOrNull ?: false

    val meanings: List<Meaning>
        get() = objects("meanings").mapNotNull { m ->
            m.str("meaning")?.let { Meaning(it, m.flag("accepted_answer")) }
        }

    val auxiliaryMeanings: List<AuxiliaryMeaning>
        get() = objects("auxiliary_meanings").mapNotNull { m ->
            val text = m.str("meaning") ?: return@mapNotNull null
            when (m.str("type")) {
                "whitelist" -> AuxiliaryMeaning(text, whitelist = true)
                "blacklist" -> AuxiliaryMeaning(text, whitelist = false)
                else -> null
            }
        }

    val readings: List<Reading>
        get() = objects("readings").mapNotNull { r ->
            r.str("reading")?.takeIf { it != "None" }?.let {
                Reading(it, r.str("type"), r.flag("accepted_answer"), r.flag("primary"))
            }
        }

    /** WaniKani's own pronunciation clips, one URL per voice. Vocabulary only; empty elsewhere. */
    val pronunciationAudios: List<String>
        get() = objects("pronunciation_audios").mapNotNull { it.str("url") }

    /** The answers to check against, with the user's own meaning synonyms added. */
    fun answers(synonyms: List<String> = emptyList()) = SubjectAnswers(
        meanings = meanings,
        auxiliaryMeanings = auxiliaryMeanings,
        synonyms = synonyms,
        readings = readings,
    )

    /** Ids of the subjects this one is built from (a kanji's radicals). */
    val componentSubjectIds: List<Long> get() = ids("component_subject_ids")

    /** Kanji that look like this one. */
    val visuallySimilarSubjectIds: List<Long> get() = ids("visually_similar_subject_ids")

    /** Subjects built from this one (a radical's kanji, a kanji's vocabulary). */
    val amalgamationSubjectIds: List<Long> get() = ids("amalgamation_subject_ids")

    private fun ids(name: String): List<Long> =
        (data[name] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.longOrNull } ?: emptyList()

    val hasAcceptedMeanings get() = meanings.any { it.acceptedAnswer }
    val hasAcceptedReadings get() = readings.any { it.acceptedAnswer }
    // Any reading of the type, accepted or not: WaniKani accepts either on'yomi or kun'yomi for a
    // kanji, never both, and the on'yomi/kun'yomi mode still asks for both kinds.
    val hasOnYomi get() = readings.any { it.isOnYomi }
    val hasKunYomi get() = readings.any { it.isKunYomi }

    companion object {
        fun parse(json: String) = SubjectContent(apiJson.parseToJsonElement(json).jsonObject)
    }
}

fun subjectKindOf(type: String): SubjectKind? = when (type) {
    "radical" -> SubjectKind.RADICAL
    "kanji" -> SubjectKind.KANJI
    "vocabulary" -> SubjectKind.VOCABULARY
    "kana_vocabulary" -> SubjectKind.KANA_VOCABULARY
    else -> null
}

/** Text shown on a lesson card, with WaniKani's inline markup (`<radical>`, `<ja>`, ...) removed. */
data class LessonInfo(
    val title: String,
    val kind: SubjectKind?,
    val level: Int,
    val meanings: List<String>,
    val readings: List<ReadingInfo>,
    val meaningMnemonic: String?,
    val meaningHint: String?,
    val readingMnemonic: String?,
    val readingHint: String?,
    val partsOfSpeech: List<String>,
    val contextSentences: List<ContextSentence>,
    /** Linked subjects (radicals, similar kanji, uses). Filled in by the repositories, which can look them up. */
    val related: List<RelatedGroup> = emptyList(),
    /** Pitch accent patterns from the bundled Kanjium data; empty when the source has none for this word. */
    val pitch: List<PitchAccent> = emptyList(),
    /** The subject's id, so the UI can ask for its pronunciation audio. */
    val id: Long = 0,
    /** Whether WaniKani hosts pronunciation audio for this item (vocabulary only). */
    val hasAudio: Boolean = false,
    /** The same mnemonics and hints with WaniKani's markup kept, for the coloured details table. */
    val meaningMnemonicMarkup: String? = null,
    val meaningHintMarkup: String? = null,
    val readingMnemonicMarkup: String? = null,
    val readingHintMarkup: String? = null,
)

data class ReadingInfo(val text: String, val type: String?, val primary: Boolean, val accepted: Boolean)

data class ContextSentence(val japanese: String, val english: String)

/** Removes tags and decodes the few entities WaniKani uses. */
internal fun stripMarkup(text: String): String =
    text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&")
        .trim()

fun SubjectContent.lessonInfo(title: String, type: String, level: Int, id: Long = 0): LessonInfo {
    fun text(name: String) = (rawData[name] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }?.let(::stripMarkup)
    fun markup(name: String) = (rawData[name] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    val primaryFirst = compareByDescending<Meaning> { it.acceptedAnswer }
    return LessonInfo(
        title = title,
        kind = subjectKindOf(type),
        level = level,
        id = id,
        hasAudio = pronunciationAudios.isNotEmpty(),
        meanings = meanings.sortedWith(primaryFirst).filter { it.acceptedAnswer }.map { it.text },
        readings = readings.map { ReadingInfo(it.reading, it.type, it.primary, it.acceptedAnswer) }
            .sortedWith(compareByDescending<ReadingInfo> { it.accepted }.thenByDescending { it.primary }),
        meaningMnemonic = text("meaning_mnemonic"),
        meaningHint = text("meaning_hint"),
        readingMnemonic = text("reading_mnemonic"),
        readingHint = text("reading_hint"),
        meaningMnemonicMarkup = markup("meaning_mnemonic"),
        meaningHintMarkup = markup("meaning_hint"),
        readingMnemonicMarkup = markup("reading_mnemonic"),
        readingHintMarkup = markup("reading_hint"),
        partsOfSpeech = (rawData["parts_of_speech"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList(),
        contextSentences = (rawData["context_sentences"] as? JsonArray)?.mapNotNull { s ->
            val o = s as? JsonObject ?: return@mapNotNull null
            val ja = (o["ja"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            ContextSentence(ja, (o["en"] as? JsonPrimitive)?.contentOrNull.orEmpty())
        } ?: emptyList(),
    )
}
