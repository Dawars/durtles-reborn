package com.example.durtlesreborn.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** The two colours a quiz feedback box is drawn with. */
data class FeedbackColors(val container: Color, val content: Color)

/** The green a correct answer starts from, before it is nudged towards the theme. */
private const val GOOD_HUE = 145f

/** How far the green may lean towards the theme's primary hue, in degrees. */
private const val HARMONY_SHIFT = 12f

/**
 * A green shaded box for a correct answer that still belongs to the theme: the green hue is rotated
 * at most [HARMONY_SHIFT] degrees towards the theme's primary, a pale tint is used on light
 * surfaces and a deep one on dark surfaces, so it follows the theme rather than fighting it.
 */
fun correctFeedbackColors(scheme: ColorScheme): FeedbackColors {
    val dark = scheme.background.luminance() < 0.5f
    val hue = harmonize(GOOD_HUE, hueOf(scheme.primary), HARMONY_SHIFT)
    return if (dark) {
        FeedbackColors(container = hsl(hue, 0.42f, 0.24f), content = hsl(hue, 0.55f, 0.86f))
    } else {
        FeedbackColors(container = hsl(hue, 0.52f, 0.89f), content = hsl(hue, 0.62f, 0.17f))
    }
}

/**
 * The theme's own error container for a wrong answer. Every scheme keeps its error role red, so a
 * wrong answer always reads as red while the shade still matches the theme's surface.
 */
fun wrongFeedbackColors(scheme: ColorScheme): FeedbackColors =
    FeedbackColors(container = scheme.errorContainer, content = scheme.onErrorContainer)

/** The two colours of an answer-type chip (Meaning or Reading). */
data class AnswerTypeColors(val background: Color, val content: Color)

/** Blue for "Meaning", amber for "Reading", each nudged towards the theme's primary. */
private const val MEANING_HUE = 210f
private const val READING_HUE = 32f

/**
 * The chip beside the item that says what has to be typed: a cool blue for an English meaning and a
 * warm amber for a kana reading. Both hues are nudged towards the theme's primary (at most 18°), and
 * the tone follows the surface, so the two stay distinct from each other, from the subject chip, and
 * still belong to the theme.
 */
fun answerTypeColors(scheme: ColorScheme, reading: Boolean): AnswerTypeColors {
    val dark = scheme.background.luminance() < 0.5f
    val base = if (reading) READING_HUE else MEANING_HUE
    val hue = harmonize(base, hueOf(scheme.primary), 18f)
    return if (dark) {
        AnswerTypeColors(background = hsl(hue, 0.38f, 0.30f), content = hsl(hue, 0.50f, 0.90f))
    } else {
        AnswerTypeColors(background = hsl(hue, 0.58f, 0.88f), content = hsl(hue, 0.62f, 0.20f))
    }
}

/** [source] hue in degrees moved towards [target] by at most [maxShift], taking the short way round. */
internal fun harmonize(source: Float, target: Float, maxShift: Float): Float {
    // 0..360, then folded into -180..180; an exact 180 stays positive so the green leans teal.
    var delta = ((target - source) % 360f + 360f) % 360f
    if (delta > 180f) delta -= 360f
    return (source + delta.coerceIn(-maxShift, maxShift) + 360f) % 360f
}

/** Hue in degrees (0–360) of [color]. */
internal fun hueOf(color: Color): Float = hslOf(color).first

/** [color] as hue (0–360), saturation and lightness, each 0–1. */
internal fun hslOf(color: Color): Triple<Float, Float, Float> {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    if (max == min) return Triple(0f, 0f, l)
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        r -> 60f * (((g - b) / d) % 6f)
        g -> 60f * (((b - r) / d) + 2f)
        else -> 60f * (((r - g) / d) + 4f)
    }
    return Triple((h + 360f) % 360f, s, l)
}

/** Builds an opaque [Color] from hue (degrees), saturation and lightness (0–1 each). */
internal fun hsl(h: Float, s: Float, l: Float): Color {
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}
