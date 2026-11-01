// Based on Tsurukame (ios/Style.swift),
// Copyright David Sansome, Apache License 2.0: see assets/TSURUKAME-LICENSE.txt.
// Rewritten in Kotlin and changed for this app.
package com.example.durtlesreborn.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.durtlesreborn.R
import com.example.durtlesreborn.data.StageGroup
import com.example.durtlesreborn.session.SubjectKind

/**
 * Colours and table pieces of Tsurukame's original design, from its `ios/Style.swift` at commit
 * 50ac074 (Apache 2.0, David Sansome), the last one before its 2026 visual refresh.
 */
@Immutable
data class TkColors(
    val isDark: Boolean,
    val background: Color,
    val grouped: Color,
    val cell: Color,
    val bar: Color,
    val separator: Color,
    val label: Color,
    val grey33: Color,
    val grey66: Color,
    val grey80: Color,
    val placeholder: Color,
    val chevron: Color,
    val highlight: Color,
    val headerGradient: List<Color>,
    val radical: List<Color>,
    val kanji: List<Color>,
    val vocabulary: List<Color>,
    val meaning: List<Color>,
    val markupRadical: Pair<Color, Color>,
    val markupKanji: Pair<Color, Color>,
    val markupVocabulary: Pair<Color, Color>,
) {
    fun gradient(kind: SubjectKind?): List<Color> = when (kind) {
        SubjectKind.RADICAL -> radical
        SubjectKind.KANJI -> kanji
        else -> vocabulary
    }

    fun stage(group: StageGroup): Color = when (group) {
        StageGroup.NOT_STARTED -> grey33
        StageGroup.APPRENTICE -> TkFixed.apprentice
        StageGroup.GURU -> TkFixed.guru
        StageGroup.MASTER -> TkFixed.master
        StageGroup.ENLIGHTENED -> TkFixed.enlightened
        StageGroup.BURNED -> if (isDark) label else TkFixed.burned
    }
}

/** The colours that do not change between light and dark. */
object TkFixed {
    val tint = Color(0xFF007AFF)
    val red = Color(0xFFFF3B30)
    val switchOn = Color(0xFF34C759)
    val apprentice = Color(0xFFDE0094)
    val guru = Color(0xFF872B9E)
    val master = Color(0xFF294CDB)
    val enlightened = Color(0xFF0094DE)
    val burned = Color(0xFF424242)
    val locked = listOf(Color(0xFF505050), Color(0xFF484848))
    val reading = listOf(Color(0xFF3C3C3C), Color(0xFF1A1A1A))
    val readingText = Color.White
    val meaningText = Color(0xFF555555)
    val explosion1 = Color(0xFFF7B54A)
    val explosion2 = Color(0xFFE6395B)
}

internal val LightTk = TkColors(
    isDark = false,
    background = Color.White,
    grouped = Color(0xFFF2F2F7),
    cell = Color.White,
    bar = Color(0xFFF9F9F9),
    separator = Color(0x4A3C3C43),
    label = Color.Black,
    grey33 = Color(0xFF555555),
    grey66 = Color(0xFFAAAAAA),
    grey80 = Color(0xFFCCCCCC),
    placeholder = Color(0x4D3C3C43),
    chevron = Color(0xFFC7C7CC),
    highlight = Color(0xFFD1D1D6),
    headerGradient = listOf(Color(0xFF00AAFF), Color(0xFF0093DD)),
    radical = listOf(Color(0xFF00AAFF), Color(0xFF0093DD)),
    kanji = listOf(Color(0xFFFF00AA), Color(0xFFDD0093)),
    vocabulary = listOf(Color(0xFFAA00FF), Color(0xFF9300DD)),
    meaning = listOf(Color(0xFFEEEEEE), Color(0xFFE1E1E1)),
    markupRadical = Color.Black to Color(0xFFD6F1FF),
    markupKanji = Color.Black to Color(0xFFFFD6F1),
    markupVocabulary = Color.Black to Color(0xFFF1D6FF),
)

internal val DarkTk = TkColors(
    isDark = true,
    background = Color.Black,
    grouped = Color.Black,
    cell = Color(0xFF1C1C1E),
    bar = Color(0xFF1C1C1E),
    separator = Color(0xA6545458),
    label = Color.White,
    grey33 = Color(0xFFAAAAAA),
    grey66 = Color(0xFF555555),
    grey80 = Color(0xFF333333),
    placeholder = Color(0x4DEBEBF5),
    chevron = Color(0xFF5A5A5E),
    highlight = Color(0xFF3A3A3C),
    headerGradient = listOf(Color(0xFF006090), Color(0xFF005080)),
    radical = listOf(Color(0xFF006090), Color(0xFF005080)),
    kanji = listOf(Color(0xFF940060), Color(0xFF800050)),
    vocabulary = listOf(Color(0xFF6100AA), Color(0xFF530080)),
    meaning = listOf(Color(0xFFBBBBBB), Color(0xFFAEAEAE)),
    markupRadical = Color(0xFF4AC3FF) to Color(0xFF1C1C1E),
    markupKanji = Color(0xFFFF4AC3) to Color(0xFF1C1C1E),
    markupVocabulary = Color(0xFFC34AFF) to Color(0xFF1C1C1E),
)

val LocalTk = staticCompositionLocalOf { LightTk }

/** Japanese text is tagged so Android picks Japanese glyphs for the kanji, not the Chinese forms. */
val JapaneseLocale = LocaleList("ja-JP")

/** Light or dark, with a Material scheme for the few Material pieces left (menus, dialogs). */
@Composable
fun TsurukameTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val tk = if (dark) DarkTk else LightTk
    val scheme = if (dark) {
        darkColorScheme(
            primary = TkFixed.tint, onPrimary = Color.White, background = tk.grouped, onBackground = tk.label,
            surface = tk.cell, onSurface = tk.label, surfaceVariant = tk.highlight, onSurfaceVariant = tk.grey33,
            surfaceContainer = tk.cell, surfaceContainerLow = tk.cell, surfaceContainerHigh = tk.cell,
            surfaceContainerHighest = tk.highlight, outline = tk.grey66, outlineVariant = tk.separator,
            error = TkFixed.red,
        )
    } else {
        lightColorScheme(
            primary = TkFixed.tint, onPrimary = Color.White, background = tk.grouped, onBackground = tk.label,
            surface = tk.cell, onSurface = tk.label, surfaceVariant = tk.highlight, onSurfaceVariant = tk.grey33,
            surfaceContainer = tk.cell, surfaceContainerLow = tk.cell, surfaceContainerHigh = tk.cell,
            surfaceContainerHighest = tk.highlight, outline = tk.grey66, outlineVariant = tk.separator,
            error = TkFixed.red,
        )
    }
    CompositionLocalProvider(
        LocalTk provides tk,
        LocalSubjectColors provides SubjectPalette(tk.radical[1], tk.kanji[1], tk.vocabulary[1]),
        LocalStageColors provides StagePalette(
            TkFixed.apprentice, TkFixed.guru, TkFixed.master, TkFixed.enlightened, tk.stage(StageGroup.BURNED),
        ),
        LocalActionColors provides ActionPalette(TkFixed.tint, TkFixed.tint, TkFixed.locked[0]),
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** Text with an explicit size and matching line height, free of the Material body style's fixed height. */
@Composable
fun TkText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 17.sp,
    color: Color = LocalTk.current.label,
    weight: FontWeight? = null,
    align: TextAlign? = null,
    japanese: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
        style = TextStyle(
            color = color,
            fontSize = size,
            lineHeight = size * 1.28f,
            fontWeight = weight,
            textAlign = align ?: TextAlign.Unspecified,
            localeList = if (japanese) JapaneseLocale else null,
        ),
    )
}

/** Runs [onClick] on a tap and greys the row while it is held, like a table cell. */
@Composable
fun Modifier.tkPressable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val highlight = LocalTk.current.highlight
    return this
        .background(if (pressed) highlight else Color.Transparent)
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

/** An uppercase section title, sitting on the table's background. */
@Composable
fun TkSectionHeader(title: String, modifier: Modifier = Modifier, height: Dp = 56.dp) {
    Box(modifier.fillMaxWidth().height(height).padding(start = 16.dp, end = 16.dp, bottom = 7.dp), Alignment.BottomStart) {
        TkText(title.uppercase(), size = 13.sp, color = LocalTk.current.grey33, maxLines = 1)
    }
}

/**
 * A grouped table section: an optional title, the rows on a cell background with hairlines between
 * them, and an optional footer.
 */
@Composable
fun TkSection(
    title: String? = null,
    footer: String? = null,
    modifier: Modifier = Modifier,
    separatorInset: Dp = 16.dp,
    headerHeight: Dp = 56.dp,
    rows: @Composable () -> Unit,
) {
    val tk = LocalTk.current
    Column(modifier.fillMaxWidth()) {
        if (title != null) TkSectionHeader(title, height = headerHeight) else Spacer(Modifier.height(35.dp))
        val lines = remember { ArrayList<Int>() }
        Layout(
            content = rows,
            modifier = Modifier.fillMaxWidth().background(tk.cell).drawWithContent {
                drawContent()
                val hair = 1.dp.toPx() / 2
                val inset = separatorInset.toPx()
                drawLine(tk.separator, Offset(0f, 0f), Offset(size.width, 0f), hair)
                drawLine(tk.separator, Offset(0f, size.height), Offset(size.width, size.height), hair)
                for (y in lines) drawLine(tk.separator, Offset(inset, y.toFloat()), Offset(size.width, y.toFloat()), hair)
            },
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
            layout(constraints.maxWidth, placeables.sumOf { it.height }) {
                lines.clear()
                var y = 0
                placeables.forEachIndexed { index, placeable ->
                    if (index > 0) lines += y
                    placeable.placeRelative(0, y)
                    y += placeable.height
                }
            }
        }
        if (footer != null) {
            TkText(footer, size = 13.sp, color = tk.grey33, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 7.dp))
        }
    }
}

@Composable
fun TkChevron(modifier: Modifier = Modifier, color: Color = LocalTk.current.chevron) {
    Canvas(modifier.size(width = 8.dp, height = 14.dp)) {
        val stroke = 2.dp.toPx()
        drawLine(color, Offset(1.dp.toPx(), 1.dp.toPx()), Offset(size.width - 1.dp.toPx(), size.height / 2), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width - 1.dp.toPx(), size.height / 2), Offset(1.dp.toPx(), size.height - 1.dp.toPx()), stroke, StrokeCap.Round)
    }
}

/** The left-pointing chevron of a navigation bar. */
@Composable
fun TkBackChevron(modifier: Modifier = Modifier, color: Color = TkFixed.tint) {
    Canvas(modifier.size(width = 12.dp, height = 21.dp)) {
        val stroke = 3.dp.toPx()
        val edge = 1.5.dp.toPx()
        drawLine(color, Offset(size.width - edge, edge), Offset(edge, size.height / 2), stroke, StrokeCap.Round)
        drawLine(color, Offset(edge, size.height / 2), Offset(size.width - edge, size.height - edge), stroke, StrokeCap.Round)
    }
}

/** A table row: title on the left, value and chevron on the right. Tappable when [onClick] is set. */
@Composable
fun TkRow(
    title: String,
    value: String? = null,
    subtitle: String? = null,
    titleColor: Color? = null,
    chevron: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val tk = LocalTk.current
    val alpha = if (enabled) 1f else 0.4f
    val base = Modifier.fillMaxWidth().heightIn(min = 44.dp)
    Row(
        (if (onClick != null) base.tkPressable(enabled, onClick) else base).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            TkText(title, color = (titleColor ?: tk.label).copy(alpha = alpha))
            if (subtitle != null) TkText(subtitle, size = 12.sp, color = tk.grey33.copy(alpha = alpha), modifier = Modifier.padding(top = 2.dp))
        }
        if (value != null) TkText(value, color = (if (chevron) tk.grey33 else tk.label).copy(alpha = alpha), modifier = Modifier.padding(start = 8.dp), maxLines = 1)
        if (trailing != null) trailing()
        if (chevron) {
            Spacer(Modifier.width(10.dp))
            TkChevron()
        }
    }
}

/** An iOS-shaped switch: a 51×31 pill with a sliding white thumb. */
@Composable
fun TkSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val tk = LocalTk.current
    val track by animateColorAsState(if (checked) TkFixed.switchOn else if (tk.isDark) Color(0xFF39393D) else Color(0xFFE9E9EA), label = "switch track")
    val x by animateDpAsState(if (checked) 22.dp else 2.dp, label = "switch thumb")
    Box(modifier.size(width = 51.dp, height = 31.dp).clip(CircleShape).background(track)) {
        Box(Modifier.offset(x = x, y = 2.dp).size(27.dp).clip(CircleShape).background(Color.White))
    }
}

@Composable
fun TkSwitchRow(title: String, subtitle: String? = null, checked: Boolean, onToggle: () -> Unit) {
    TkRow(title, subtitle = subtitle, onClick = onToggle, trailing = { TkSwitch(checked, Modifier.padding(start = 12.dp)) })
}

/** A navigation bar: back chevron on the left, a centred title, optional actions on the right. */
@Composable
fun TkNavBar(
    title: String,
    onBack: (() -> Unit)? = null,
    backLabel: String = "Back",
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val tk = LocalTk.current
    Column(Modifier.fillMaxWidth().background(tk.bar).statusBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(44.dp)) {
            TkText(title, weight = FontWeight.SemiBold, maxLines = 1, align = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(horizontal = 88.dp))
            if (onBack != null) {
                Row(
                    Modifier.align(Alignment.CenterStart).clickable(onClick = onBack).height(44.dp).padding(start = 8.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TkBackChevron()
                    TkText(backLabel, color = TkFixed.tint)
                }
            }
            if (actions != null) {
                Row(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
            }
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(tk.separator))
    }
}

/** A text button in the navigation bar's tint. */
@Composable
fun TkBarButton(label: String, enabled: Boolean = true, bold: Boolean = false, onClick: () -> Unit) {
    TkText(
        label,
        color = TkFixed.tint.copy(alpha = if (enabled) 1f else 0.35f),
        weight = if (bold) FontWeight.SemiBold else null,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick).padding(horizontal = 8.dp, vertical = 10.dp),
    )
}

/** A row of mutually exclusive choices, like a `UISegmentedControl`. */
@Composable
fun <T> TkSegmented(options: List<Pair<T, String>>, selected: T, modifier: Modifier = Modifier, onPick: (T) -> Unit) {
    val tk = LocalTk.current
    val track = if (tk.isDark) Color(0xFF2C2C2E) else Color(0xFFE3E3E8)
    val selectedColor = if (tk.isDark) Color(0xFF636366) else Color.White
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(track).padding(2.dp)) {
        for ((value, label) in options) {
            Box(
                Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(7.dp))
                    .background(if (value == selected) selectedColor else Color.Transparent)
                    .clickable { onPick(value) },
                Alignment.Center,
            ) {
                TkText(label, size = 13.sp, weight = if (value == selected) FontWeight.SemiBold else null, maxLines = 1)
            }
        }
    }
}

/**
 * One subject in a list: a row in the subject's own gradient with the level, the characters and the
 * reading and meaning on the right. A mistake on the reading or meaning shows that line in bold.
 */
@Composable
fun SubjectRow(
    title: String,
    kind: SubjectKind?,
    reading: String?,
    meaning: String?,
    level: Int? = null,
    locked: Boolean = false,
    readingWrong: Boolean = false,
    meaningWrong: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val tk = LocalTk.current
    val base = Modifier.fillMaxWidth().background(Brush.verticalGradient(if (locked) TkFixed.locked else tk.gradient(kind)))
    Row(
        (if (onClick != null) base.clickable(onClick = onClick) else base).heightIn(min = 44.dp).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (level != null) TkText("$level", color = Color.White, modifier = Modifier.width(30.dp))
        TkText(title, size = 28.sp, color = Color.White, japanese = true, maxLines = 1, modifier = Modifier.weight(1f))
        Column(Modifier.padding(start = 8.dp).weight(1f, fill = false), horizontalAlignment = Alignment.End) {
            if (!reading.isNullOrEmpty()) {
                TkText(reading, size = 14.sp, color = Color.White, japanese = true, weight = if (readingWrong) FontWeight.Bold else null, maxLines = 1, align = TextAlign.End)
            }
            if (!meaning.isNullOrEmpty()) {
                TkText(meaning, size = 14.sp, color = Color.White, weight = if (meaningWrong) FontWeight.Bold else null, maxLines = 1, align = TextAlign.End)
            }
        }
    }
}

/** A small rounded chip with a subject's characters on its gradient. */
@Composable
fun SubjectChipBox(
    text: String,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    japanese: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val base = modifier.clip(RoundedCornerShape(6.dp)).background(Brush.verticalGradient(if (dimmed) gradient.map { it.copy(alpha = 0.5f) } else gradient))
    Box((if (onClick != null) base.clickable(onClick = onClick) else base).heightIn(min = 28.dp).padding(horizontal = 6.dp), Alignment.Center) {
        TkText(text, size = 16.sp, color = Color.White, japanese = japanese, maxLines = 1)
    }
}

/**
 * A confirmation that slides up from the bottom, like an iOS action sheet: the title and message
 * with the red action in one card, and Cancel in its own card below.
 */
@Composable
fun TkActionSheet(title: String, message: String, action: String, onAction: () -> Unit, onDismiss: () -> Unit) {
    val tk = LocalTk.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            Alignment.BottomCenter,
        ) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp)) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(tk.cell)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        TkText(title, size = 13.sp, color = tk.grey33, weight = FontWeight.SemiBold, align = TextAlign.Center)
                        TkText(message, size = 13.sp, color = tk.grey33, align = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    }
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(tk.separator))
                    Box(Modifier.fillMaxWidth().height(57.dp).tkPressable(onClick = onAction), Alignment.Center) {
                        TkText(action, size = 20.sp, color = TkFixed.red)
                    }
                }
                Box(
                    Modifier.padding(top = 8.dp).fillMaxWidth().height(57.dp).clip(RoundedCornerShape(14.dp)).background(tk.cell).tkPressable(onClick = onDismiss),
                    Alignment.Center,
                ) {
                    TkText("Cancel", size = 20.sp, color = TkFixed.tint, weight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** The tinted icon of an SRS stage group. */
@Composable
fun StageIcon(group: StageGroup, modifier: Modifier = Modifier) {
    val res = when (group) {
        StageGroup.APPRENTICE, StageGroup.NOT_STARTED -> R.drawable.tk_srs_apprentice
        StageGroup.GURU -> R.drawable.tk_srs_guru
        StageGroup.MASTER -> R.drawable.tk_srs_master
        StageGroup.ENLIGHTENED -> R.drawable.tk_srs_enlightened
        StageGroup.BURNED -> R.drawable.tk_srs_burned
    }
    Image(painterResource(res), null, modifier.size(23.dp), colorFilter = ColorFilter.tint(LocalTk.current.stage(group)))
}
