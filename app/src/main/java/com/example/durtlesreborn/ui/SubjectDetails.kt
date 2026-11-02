// Based on Tsurukame (ios/SubjectDetailsView.swift, MarkupFormatter.swift, VocabularyHighlighter.swift),
// Copyright David Sansome, Apache License 2.0: see assets/TSURUKAME-LICENSE.txt.
// Rewritten in Kotlin and changed for this app.
package com.example.durtlesreborn.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.durtlesreborn.R
import com.example.durtlesreborn.data.ContextSentence
import com.example.durtlesreborn.data.LessonInfo
import com.example.durtlesreborn.data.PitchAccent
import com.example.durtlesreborn.data.RelatedGroup
import com.example.durtlesreborn.data.RelatedSubject
import com.example.durtlesreborn.data.morae
import com.example.durtlesreborn.data.pitchPattern
import com.example.durtlesreborn.session.SubjectKind

private val DetailsHeaderHeight: Dp = 38.dp
private val MarkupTag = Regex("""[<\[](/?)(vocabulary|reading|ja|jp|kanji|radical|b|em|i|strong|kan|a)(?: [^>\]]*)?[>\]]""", RegexOption.IGNORE_CASE)

/** WaniKani's mnemonic markup as styled text, with Tsurukame's colours for each kind of tag. */
internal fun markupToAnnotated(raw: String, tk: TkColors): AnnotatedString = buildAnnotatedString {
    fun decode(text: String) = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&")

    val source = raw.trim()
    var open = 0
    var index = 0
    for (tag in MarkupTag.findAll(source)) {
        append(decode(source.substring(index, tag.range.first)))
        index = tag.range.last + 1
        if (tag.groupValues[1] == "/") {
            if (open > 0) {
                pop()
                open--
            }
        } else {
            pushStyle(
                when (tag.groupValues[2].lowercase()) {
                    "radical" -> SpanStyle(color = tk.markupRadical.first, background = tk.markupRadical.second)
                    "kanji", "kan" -> SpanStyle(color = tk.markupKanji.first, background = tk.markupKanji.second)
                    "vocabulary" -> SpanStyle(color = tk.markupVocabulary.first, background = tk.markupVocabulary.second)
                    "reading" -> SpanStyle(color = tk.background, background = tk.grey33)
                    "ja", "jp" -> SpanStyle(localeList = JapaneseLocale)
                    "b", "strong" -> SpanStyle(fontWeight = FontWeight.Bold)
                    "i", "em" -> SpanStyle(fontStyle = FontStyle.Italic)
                    else -> SpanStyle()
                },
            )
            open++
        }
    }
    append(decode(source.substring(index)))
}

/** The subject's characters on its gradient, under the status bar, with a back button. */
@Composable
fun SubjectHeader(info: LessonInfo, onBack: (() -> Unit)?, onPlayAudio: ((Long) -> Unit)? = null) {
    val tk = LocalTk.current
    val canPlay = info.hasAudio && onPlayAudio != null
    Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(tk.gradient(info.kind))).statusBarsPadding().height(74.dp)) {
        if (info.kind == SubjectKind.RADICAL && isImageRadicalTitle(info.title)) {
            RadicalGlyph(info.title, 44.dp, Modifier.align(Alignment.Center))
        } else {
            TkText(
                info.title,
                size = 40.sp,
                color = Color.White,
                japanese = true,
                align = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 56.dp)
                    .clickable(enabled = canPlay) { onPlayAudio?.invoke(info.id) }
                    .semantics { contentDescription = if (canPlay) "Play the pronunciation of ${info.title}" else info.title },
            )
        }
        if (onBack != null) {
            Box(Modifier.align(Alignment.TopStart).size(56.dp).clickable(onClick = onBack).semantics { contentDescription = "Back" }, Alignment.Center) {
                Image(painterResource(R.drawable.tk_back), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(Color.White))
            }
        }
    }
}

/**
 * Everything about a subject as a grouped table, in Tsurukame's order: meaning, reading, parts,
 * explanations, then what it is used in. [hideMeaning] and [hideReading] keep back the answers to
 * questions not asked yet, until "Show all information" is tapped. [footer] adds rows at the end.
 */
@Composable
fun SubjectDetailsTable(
    info: LessonInfo,
    modifier: Modifier = Modifier,
    onOpen: ((Long) -> Unit)? = null,
    onPlayAudio: ((Long) -> Unit)? = null,
    hideMeaning: Boolean = false,
    hideReading: Boolean = false,
    footer: (@Composable () -> Unit)? = null,
) {
    val tk = LocalTk.current
    var showAll by remember(info.id) { mutableStateOf(false) }
    val meaningHidden = hideMeaning && !showAll
    val readingHidden = hideReading && !showAll
    val isRadical = info.kind == SubjectKind.RADICAL
    Column(modifier.fillMaxWidth().background(tk.grouped).verticalScroll(rememberScrollState())) {
        if (!meaningHidden) DetailSection("Meaning") { TextCell(info.meanings.joinToString(", ")) }
        if (!readingHidden && info.readings.isNotEmpty()) {
            DetailSection("Reading") { ReadingCell(info, onPlayAudio) }
        }
        val parts = info.related.firstOrNull { it.label == "Radicals" || it.label == "Kanji" }
        if (parts != null) DetailSection(parts.label) { ChipsCell(parts, onOpen) }

        // A reading explanation often gives away the meaning, so it stays hidden with either answer.
        if (!meaningHidden) {
            ExplanationSection(if (isRadical) "Mnemonic" else "Meaning Explanation", info.meaningMnemonicMarkup, info.meaningHintMarkup)
        }
        if (!meaningHidden && !readingHidden) {
            ExplanationSection("Reading Explanation", info.readingMnemonicMarkup, info.readingHintMarkup)
        }
        if (meaningHidden || readingHidden) {
            Box(Modifier.fillMaxWidth().padding(top = 16.dp), Alignment.Center) {
                TkText(
                    "Show all information",
                    color = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(TkFixed.tint).clickable { showAll = true }.padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }
        }

        if (!meaningHidden && info.contextSentences.isNotEmpty()) {
            DetailSection("Context Sentences") {
                for (sentence in info.contextSentences) ContextSentenceCell(info, sentence)
            }
        }
        if (info.partsOfSpeech.isNotEmpty()) DetailSection("Part of Speech") { TextCell(info.partsOfSpeech.joinToString(", ")) }
        info.related.firstOrNull { it.label == "Visually similar kanji" }?.let { RowsSection("Visually Similar Kanji", it, onOpen) }
        info.related.firstOrNull { it.label.startsWith("Used in") }?.let { RowsSection("Used in", it, onOpen) }
        if (info.pitch.isNotEmpty()) DetailSection("Pitch Accent") { PitchCell(info.pitch) }
        if (hasStrokeOrder(info)) {
            DetailSection("Stroke Order") {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp)) { StrokeOrderSection(info) }
            }
        }
        footer?.invoke()
        Spacer(Modifier.height(24.dp).navigationBarsPadding())
    }
}

@Composable
private fun DetailSection(title: String, rows: @Composable () -> Unit) {
    TkSection(title, headerHeight = DetailsHeaderHeight, rows = rows)
}

@Composable
private fun TextCell(text: String) {
    TkText(text, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp))
}

@Composable
private fun ExplanationSection(title: String, text: String?, hint: String?) {
    if (text.isNullOrBlank()) return
    val tk = LocalTk.current
    DetailSection(title) {
        MarkupCell(markupToAnnotated(text, tk))
        if (!hint.isNullOrBlank()) MarkupCell(markupToAnnotated(hint, tk), color = tk.grey33, size = 15)
    }
}

@Composable
private fun MarkupCell(text: AnnotatedString, color: Color = LocalTk.current.label, size: Int = 17) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
        style = TextStyle(color = color, fontSize = size.sp, lineHeight = (size * 1.3f).sp),
    )
}

/** The Japanese with the word picked out, and the translation blurred until the cell is tapped. */
@Composable
private fun ContextSentenceCell(info: LessonInfo, sentence: ContextSentence) {
    val tk = LocalTk.current
    val blurByDefault = LocalBlurContextSentences.current
    var blurred by remember(info.id, sentence) { mutableStateOf(blurByDefault) }
    val hidden by animateFloatAsState(if (blurred) 1f else 0f, tween(200), label = "sentence blur")
    Column(
        Modifier.fillMaxWidth().clickable(enabled = blurred) { blurred = false }.padding(horizontal = 16.dp, vertical = 11.dp)
            .semantics { if (blurred) contentDescription = "${sentence.japanese}. Tap to show the translation" },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            highlightWord(sentence.japanese, info.title),
            style = TextStyle(color = tk.label, fontSize = 17.sp, lineHeight = 22.sp, localeList = JapaneseLocale),
        )
        if (sentence.english.isNotEmpty()) {
            TkText(
                sentence.english,
                color = tk.label.copy(alpha = 1f - 0.25f * hidden),
                // Blurring needs Android 12. Older versions hide the text outright instead.
                modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(5.dp * hidden, BlurredEdgeTreatment.Unbounded)
                } else {
                    Modifier.graphicsLayer { alpha = 1f - hidden }
                },
            )
        }
    }
}

/** The sentence with [word] in red; for an inflected word, its part up to the first kana. */
internal fun highlightWord(sentence: String, word: String): AnnotatedString {
    val stem = word.takeWhile { it !in '\u3040'..'\u309F' }.ifEmpty { word }
    val target = if (word in sentence) word else stem
    return buildAnnotatedString {
        var index = 0
        while (true) {
            val found = sentence.indexOf(target, index)
            if (found < 0 || target.isEmpty()) break
            append(sentence.substring(index, found))
            withStyle(SpanStyle(color = TkFixed.red)) { append(target) }
            index = found + target.length
        }
        append(sentence.substring(index))
    }
}

/** Primary readings in bold, the others plain, and a speaker button when the word has audio. */
@Composable
private fun ReadingCell(info: LessonInfo, onPlayAudio: ((Long) -> Unit)?) {
    val tk = LocalTk.current
    val several = info.readings.size > 1
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            buildAnnotatedString {
                info.readings.forEachIndexed { index, reading ->
                    if (index > 0) append(", ")
                    withStyle(SpanStyle(fontWeight = if (reading.primary && several) FontWeight.Bold else FontWeight.Normal)) { append(reading.text) }
                }
            },
            modifier = Modifier.weight(1f).padding(vertical = 11.dp),
            style = TextStyle(color = tk.label, fontSize = 17.sp, lineHeight = 22.sp, localeList = JapaneseLocale),
        )
        if (info.hasAudio && onPlayAudio != null) {
            Box(Modifier.size(44.dp).clickable { onPlayAudio(info.id) }.semantics { contentDescription = "Play the pronunciation" }, Alignment.Center) {
                Image(painterResource(R.drawable.tk_volume), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(TkFixed.tint))
            }
        }
    }
}

/** A kanji's radicals or a word's kanji: chips with the meaning beside each one. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipsCell(group: RelatedGroup, onOpen: ((Long) -> Unit)?) {
    val tk = LocalTk.current
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for (item in group.items) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SubjectChipBox(item.title, tk.gradient(item.kind), onClick = onOpen?.let { { it(item.id) } })
                TkText(item.subtitle.substringAfterLast(" · "), size = 15.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun RowsSection(title: String, group: RelatedGroup, onOpen: ((Long) -> Unit)?) {
    DetailSection(title) {
        for (item in group.items) RelatedRow(item, onOpen)
        if (group.more > 0) TkRow("and ${group.more} more")
    }
}

@Composable
private fun RelatedRow(item: RelatedSubject, onOpen: ((Long) -> Unit)?) {
    val parts = item.subtitle.split(" · ")
    SubjectRow(
        item.title,
        item.kind,
        reading = parts.takeIf { it.size > 1 }?.first(),
        meaning = parts.last(),
        level = item.level,
        onClick = onOpen?.let { { it(item.id) } },
    )
}

/** Pitch accent patterns of the word. The data comes from Kanjium, which the licence requires crediting. */
@Composable
private fun PitchCell(pitch: List<PitchAccent>) {
    val tk = LocalTk.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (entry in pitch) {
            for (accent in entry.positions) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TkText(entry.reading, japanese = true)
                    TkText("[$accent]", size = 13.sp, color = TkFixed.tint)
                    val pattern = pitchPattern(morae(entry.reading).size, accent)
                    Text(
                        buildAnnotatedString {
                            pattern.forEachIndexed { index, letter ->
                                if (index > 0) append(' ')
                                withStyle(SpanStyle(color = if (letter == 'H') TkFixed.tint else tk.grey33)) { append(letter) }
                            }
                        },
                        style = TextStyle(fontSize = 14.sp),
                        modifier = Modifier.semantics { contentDescription = "Pitch: $pattern" },
                    )
                }
            }
        }
        TkText("H high · L low · [n] drop after the nth mora", size = 11.sp, color = tk.grey33)
        TkText("Pitch data: Kanjium (Uros O.), CC BY-SA 4.0.", size = 11.sp, color = tk.grey33)
    }
}

/**
 * A subject opened from a chip during a review or lesson, drawn over the session so it stays as it
 * was. Linked subjects open on top, and Back retraces them.
 */
@Composable
fun SubjectOverlay(
    subjectId: Long,
    loadInfo: suspend (Long) -> LessonInfo?,
    onDismiss: () -> Unit,
    onPlayAudio: ((Long) -> Unit)? = null,
) {
    var trail by remember(subjectId) { mutableStateOf(listOf(subjectId)) }
    val current = trail.last()
    val back = { if (trail.size > 1) trail = trail.dropLast(1) else onDismiss() }
    BackHandler(onBack = back)
    val info by produceState<LessonInfo?>(null, current) { value = loadInfo(current) }
    val tk = LocalTk.current
    Column(Modifier.fillMaxSize().background(tk.grouped).clickable(enabled = false) {}) {
        val loaded = info
        if (loaded == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else {
            SubjectHeader(loaded, back, onPlayAudio)
            SubjectDetailsTable(loaded, Modifier.weight(1f), onOpen = { trail = trail + it }, onPlayAudio = onPlayAudio)
        }
    }
}

@Composable
fun subjectColor(kind: SubjectKind?): Color {
    val palette = LocalSubjectColors.current
    return when (kind) {
        SubjectKind.RADICAL -> palette.radical
        SubjectKind.KANJI -> palette.kanji
        else -> palette.vocabulary
    }
}
