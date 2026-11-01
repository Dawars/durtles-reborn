package com.example.durtlesreborn.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

/**
 * The colour themes the user can pick from, in the order the settings list them. The names are
 * those stored in settings. DYNAMIC takes its colours from the wallpaper on Android 12 and later
 * and follows the system's light or dark mode; every other theme looks the same in both modes.
 */
enum class AppTheme(val isDark: Boolean?, val label: String) {
    DYNAMIC(null, "Dynamic (wallpaper colours)"),
    LIGHT(false, "Light"),
    MATCHA(false, "Matcha"),
    SANDSTONE(false, "Sandstone"),
    DARK(true, "Dark"),
    BLACK(true, "Black (OLED)"),
    NORD_POLAR_NIGHT(true, "Nord polar night"),
    FOREST(true, "Forest"),
    CRAB_CROC(false, "Crabcroc"),
    SAKURA(false, "Sakura"),
    ;

    companion object {
        fun fromName(name: String, fallback: AppTheme): AppTheme = entries.firstOrNull { it.name == name } ?: fallback
    }
}

/** Colours for the kinds of subject, chosen for this app. Lighter on dark surfaces so they stay readable. */
data class SubjectPalette(val radical: Color, val kanji: Color, val vocabulary: Color)

private val lightSubjects = SubjectPalette(radical = Color(0xFF1F7A70), kanji = Color(0xFFAD5E0C), vocabulary = Color(0xFF4A5BB8))
private val darkSubjects = SubjectPalette(radical = Color(0xFF4FC3B2), kanji = Color(0xFFF0A04B), vocabulary = Color(0xFF8E9DF0))

/**
 * WaniKani's subject colours for the Crabcroc theme: radical blue, kanji pink, vocabulary purple.
 * Each is a shade darker than the site's own so large text keeps 3:1 on the pale background and
 * white/dark text on the tiles keeps 4.5:1.
 */
private val crabCrocSubjects = SubjectPalette(
    radical = Color(0xFF0090E6),
    kanji = Color(0xFFE1008C),
    vocabulary = Color(0xFF9808F3),
)

val LocalSubjectColors = staticCompositionLocalOf { lightSubjects }

/** Builds a complete scheme from a few colours, filling the container and outline roles consistently. */
private fun scheme(
    dark: Boolean,
    background: Color,
    surfaceVariant: Color,
    onSurface: Color,
    onSurfaceVariant: Color,
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
    outline: Color,
    error: Color,
): ColorScheme {
    // Surface containers step from the background towards the variant colour.
    fun step(amount: Float) = surfaceVariant.copy(alpha = amount).compositeOver(background)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onPrimary,
        secondaryContainer = primaryContainer,
        onSecondaryContainer = onPrimaryContainer,
        tertiary = secondary,
        onTertiary = onPrimary,
        background = background,
        onBackground = onSurface,
        surface = background,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = primary,
        inverseSurface = onSurface,
        inverseOnSurface = background,
        inversePrimary = primaryContainer,
        outline = outline,
        outlineVariant = surfaceVariant,
        error = error,
        onError = background,
        surfaceContainerLowest = background,
        surfaceContainerLow = step(0.35f),
        surfaceContainer = step(0.55f),
        surfaceContainerHigh = step(0.8f),
        surfaceContainerHighest = surfaceVariant,
        surfaceDim = background,
        surfaceBright = surfaceVariant,
    )
}

internal val LightScheme = scheme(
    dark = false,
    background = Color(0xFFFAF8F3), surfaceVariant = Color(0xFFEDE8DC),
    onSurface = Color(0xFF1B1B18), onSurfaceVariant = Color(0xFF4A483F),
    primary = Color(0xFF2F5D8A), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E6F6), onPrimaryContainer = Color(0xFF0B2840),
    secondary = Color(0xFF6B5B3E), outline = Color(0xFF7A776B), error = Color(0xFFB3261E),
)

internal val DarkScheme = scheme(
    dark = true,
    background = Color(0xFF14130F), surfaceVariant = Color(0xFF2A2820),
    onSurface = Color(0xFFE6E2D9), onSurfaceVariant = Color(0xFFC9C5B8),
    primary = Color(0xFF9CC4EC), onPrimary = Color(0xFF0B2840),
    primaryContainer = Color(0xFF1F4568), onPrimaryContainer = Color(0xFFD6E6F6),
    secondary = Color(0xFFD8C79F), outline = Color(0xFF938F83), error = Color(0xFFF2B8B5),
)

/** Pure black background for OLED screens. */
internal val BlackScheme = scheme(
    dark = true,
    background = Color(0xFF000000), surfaceVariant = Color(0xFF1C1C1C),
    onSurface = Color(0xFFE8E8E8), onSurfaceVariant = Color(0xFFC4C4C4),
    primary = Color(0xFF9CC4EC), onPrimary = Color(0xFF0B2840),
    primaryContainer = Color(0xFF1F4568), onPrimaryContainer = Color(0xFFD6E6F6),
    secondary = Color(0xFFD8C79F), outline = Color(0xFF8C8C8C), error = Color(0xFFF2B8B5),
)

/** Built on the open Nord palette (nordtheme.com, MIT licensed). */
internal val NordScheme = scheme(
    dark = true,
    background = Color(0xFF2E3440), surfaceVariant = Color(0xFF3B4252),
    onSurface = Color(0xFFECEFF4), onSurfaceVariant = Color(0xFFD8DEE9),
    primary = Color(0xFF88C0D0), onPrimary = Color(0xFF2E3440),
    primaryContainer = Color(0xFF434C5E), onPrimaryContainer = Color(0xFFECEFF4),
    secondary = Color(0xFF81A1C1), outline = Color(0xFF8F9BB3), error = Color(0xFFE8939B),
)

/** Soft sage greens on a pale leaf background. */
internal val MatchaScheme = scheme(
    dark = false,
    background = Color(0xFFF3F7EE), surfaceVariant = Color(0xFFE1EAD6),
    onSurface = Color(0xFF1A2016), onSurfaceVariant = Color(0xFF435039),
    primary = Color(0xFF3F6B2C), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E8C4), onPrimaryContainer = Color(0xFF12300A),
    secondary = Color(0xFF6A5A2A), outline = Color(0xFF6F7A63), error = Color(0xFFB3261E),
)

/** Warm paper with terracotta accents. */
internal val SandstoneScheme = scheme(
    dark = false,
    background = Color(0xFFFBF5EC), surfaceVariant = Color(0xFFEFE3D1),
    onSurface = Color(0xFF231A12), onSurfaceVariant = Color(0xFF55473A),
    primary = Color(0xFF9A4A1F), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF6DCC8), onPrimaryContainer = Color(0xFF3A1805),
    secondary = Color(0xFF6B5B3E), outline = Color(0xFF857360), error = Color(0xFFB3261E),
)

/** Deep pine with mint accents. */
internal val ForestScheme = scheme(
    dark = true,
    background = Color(0xFF0F1A14), surfaceVariant = Color(0xFF1F2E25),
    onSurface = Color(0xFFE3EBE2), onSurfaceVariant = Color(0xFFBFCDBF),
    primary = Color(0xFF8FD0A5), onPrimary = Color(0xFF0A2A16),
    primaryContainer = Color(0xFF214A33), onPrimaryContainer = Color(0xFFD2EEDC),
    secondary = Color(0xFFD3C98A), outline = Color(0xFF8A9C8E), error = Color(0xFFF2B8B5),
)

/** WaniKani's pink and purple on a pale page. The pink is a shade darker so links keep 4.5:1. */
internal val CrabcrocScheme = scheme(
    dark = false,
    background = Color(0xFFFDF3F8), surfaceVariant = Color(0xFFF2E1EC),
    onSurface = Color(0xFF201A1E), onSurfaceVariant = Color(0xFF54444E),
    primary = Color(0xFFC4007B), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF7C9E3), onPrimaryContainer = Color(0xFF3B0024),
    secondary = Color(0xFF7D2893), outline = Color(0xFF8B6B7E), error = Color(0xFFB3261E),
)

/** Cherry blossom pinks with a leaf-green accent on a warm blush page. */
internal val SakuraScheme = scheme(
    dark = false,
    background = Color(0xFFFFF7F5), surfaceVariant = Color(0xFFF7E2DD),
    onSurface = Color(0xFF241A18), onSurfaceVariant = Color(0xFF5C4A45),
    primary = Color(0xFFB03A5B), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF8D7DC), onPrimaryContainer = Color(0xFF3A0A18),
    secondary = Color(0xFF4F6B52), outline = Color(0xFF8C6B70), error = Color(0xFFB3261E),
)

/** The fixed colours of a theme, or null for DYNAMIC. */
internal fun fixedScheme(theme: AppTheme): ColorScheme? = when (theme) {
    AppTheme.LIGHT -> LightScheme
    AppTheme.MATCHA -> MatchaScheme
    AppTheme.SANDSTONE -> SandstoneScheme
    AppTheme.DARK -> DarkScheme
    AppTheme.BLACK -> BlackScheme
    AppTheme.NORD_POLAR_NIGHT -> NordScheme
    AppTheme.FOREST -> ForestScheme
    AppTheme.CRAB_CROC -> CrabcrocScheme
    AppTheme.SAKURA -> SakuraScheme
    AppTheme.DYNAMIC -> null
}

internal fun subjectPalette(darkSurface: Boolean) = if (darkSurface) darkSubjects else lightSubjects

/** Whether this theme, in this system mode, has a dark surface. Only DYNAMIC depends on the mode. */
fun isDarkSurface(theme: AppTheme, systemDark: Boolean) = theme.isDark ?: systemDark

/**
 * Colours for the SRS stage groups. A warm-to-cool ramp of this app's own, deliberately not
 * WaniKani's pink, purple and blue scheme.
 */
data class StagePalette(
    val apprentice: Color,
    val guru: Color,
    val master: Color,
    val enlightened: Color,
    val burned: Color,
)

private val lightStages = StagePalette(
    apprentice = Color(0xFFB4532A), guru = Color(0xFF7C6A0A), master = Color(0xFF1F7A70),
    enlightened = Color(0xFF2F5D8A), burned = Color(0xFF5E5E5E),
)
private val darkStages = StagePalette(
    apprentice = Color(0xFFE58A63), guru = Color(0xFFD0B64A), master = Color(0xFF4FC3B2),
    enlightened = Color(0xFF7FB0E0), burned = Color(0xFF9A9A9A),
)

/** The big Lessons and Reviews cards on the dashboard, and the colour of an item that is not unlocked yet. */
data class ActionPalette(val lessons: Color, val reviews: Color, val locked: Color)

private val lightActions = ActionPalette(lessons = Color(0xFFAD5E0C), reviews = Color(0xFF2F5D8A), locked = Color(0xFF8A877D))
private val darkActions = ActionPalette(lessons = Color(0xFFF0A04B), reviews = Color(0xFF7FB0E0), locked = Color(0xFF5A584F))

val LocalStageColors = staticCompositionLocalOf { lightStages }
val LocalActionColors = staticCompositionLocalOf { lightActions }

internal fun stagePalette(darkSurface: Boolean) = if (darkSurface) darkStages else lightStages
internal fun actionPalette(darkSurface: Boolean) = if (darkSurface) darkActions else lightActions

/** All the tile colours of one theme: the subject kinds, the five SRS stages and the two actions. */
data class ThemePalette(
    val subjects: SubjectPalette,
    val stages: StagePalette,
    val actions: ActionPalette,
)

/**
 * Each fixed theme brings its own tile colours, so the radical, kanji, vocabulary, stage and action
 * tiles move with the theme instead of using one shared palette. Every colour here is checked by
 * `ThemeTest` for 3:1 against its background and 4.5:1 for the text drawn on it. DYNAMIC has no
 * colours of its own and falls back to the light/dark base palettes.
 */
private val themePalettes: Map<AppTheme, ThemePalette> = mapOf(
    AppTheme.LIGHT to ThemePalette(lightSubjects, lightStages, lightActions),
    AppTheme.MATCHA to ThemePalette(
        subjects = SubjectPalette(radical = Color(0xFF3F6B2C), kanji = Color(0xFF8A6414), vocabulary = Color(0xFF2F5D8A)),
        stages = StagePalette(Color(0xFFA34A26), Color(0xFF7C6A0A), Color(0xFF2F6B4A), Color(0xFF2F5D8A), Color(0xFF5E5E5E)),
        actions = ActionPalette(lessons = Color(0xFFA34A26), reviews = Color(0xFF2F5D8A), locked = Color(0xFF7A8273)),
    ),
    AppTheme.SANDSTONE to ThemePalette(
        subjects = SubjectPalette(radical = Color(0xFF8A5A2B), kanji = Color(0xFF9A4A1F), vocabulary = Color(0xFF5B4A7A)),
        stages = StagePalette(Color(0xFF9A4A1F), Color(0xFF7C6A0A), Color(0xFF2F6B63), Color(0xFF3F5D8A), Color(0xFF5E5E5E)),
        actions = ActionPalette(lessons = Color(0xFF9A4A1F), reviews = Color(0xFF3F5D8A), locked = Color(0xFF948474)),
    ),
    AppTheme.DARK to ThemePalette(darkSubjects, darkStages, darkActions),
    AppTheme.BLACK to ThemePalette(
        subjects = SubjectPalette(radical = Color(0xFF5FD3C0), kanji = Color(0xFFFFB05C), vocabulary = Color(0xFF9FADFF)),
        stages = StagePalette(Color(0xFFF09A73), Color(0xFFE0C85A), Color(0xFF5FD3C0), Color(0xFF8FBCE8), Color(0xFFA8A8A8)),
        actions = ActionPalette(lessons = Color(0xFFFFB05C), reviews = Color(0xFF8FBCE8), locked = Color(0xFF6E6A60)),
    ),
    AppTheme.NORD_POLAR_NIGHT to ThemePalette(
        subjects = SubjectPalette(radical = Color(0xFF88C0D0), kanji = Color(0xFFEBCB8B), vocabulary = Color(0xFFB48EAD)),
        stages = StagePalette(Color(0xFFBF616A), Color(0xFFD08770), Color(0xFFA3BE8C), Color(0xFF5E81AC), Color(0xFF8F9BB3)),
        actions = ActionPalette(lessons = Color(0xFFD08770), reviews = Color(0xFF5E81AC), locked = Color(0xFF8F9BB3)),
    ),
    AppTheme.FOREST to ThemePalette(
        subjects = SubjectPalette(radical = Color(0xFF8FD0A5), kanji = Color(0xFFE0C77A), vocabulary = Color(0xFF7FB7E0)),
        stages = StagePalette(Color(0xFFC97A4F), Color(0xFFC9B45A), Color(0xFF5FB98B), Color(0xFF7FB0E0), Color(0xFF8A9A8F)),
        actions = ActionPalette(lessons = Color(0xFFC97A4F), reviews = Color(0xFF6FA8D8), locked = Color(0xFF4A5A4E)),
    ),
    AppTheme.CRAB_CROC to ThemePalette(
        subjects = crabCrocSubjects,
        stages = StagePalette(Color(0xFFB83F6C), Color(0xFFA84B9E), Color(0xFF7D4FB8), Color(0xFF4F6FB8), Color(0xFF6E6A78)),
        actions = ActionPalette(lessons = Color(0xFFC4007B), reviews = Color(0xFF7D2893), locked = Color(0xFF8B6B7E)),
    ),
    AppTheme.SAKURA to ThemePalette(
        subjects = SubjectPalette(radical = Color(0xFF4F6B52), kanji = Color(0xFFB03A5B), vocabulary = Color(0xFF7A4A8A)),
        stages = StagePalette(Color(0xFFB4562F), Color(0xFF9A7A1F), Color(0xFF4F8A6B), Color(0xFF4F6B9A), Color(0xFF8A7A76)),
        actions = ActionPalette(lessons = Color(0xFFB03A5B), reviews = Color(0xFF4F6B52), locked = Color(0xFF8C6B70)),
    ),
)

/** The tile colours of [theme]; DYNAMIC (or an unknown one) uses the light/dark base palettes. */
internal fun paletteFor(theme: AppTheme, darkSurface: Boolean): ThemePalette =
    themePalettes[theme] ?: ThemePalette(subjectPalette(darkSurface), stagePalette(darkSurface), actionPalette(darkSurface))

/** White or near-black, whichever reads better on [background]. */
fun contentColorOn(background: Color): Color {
    val dark = Color(0xFF14130F)
    return if (contrastRatio(Color.White, background) >= contrastRatio(dark, background)) Color.White else dark
}

/** WCAG contrast ratio between two colours. */
fun contrastRatio(a: Color, b: Color): Double {
    val l1 = a.luminance().toDouble()
    val l2 = b.luminance().toDouble()
    return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
}
