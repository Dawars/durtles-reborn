// Based on Tsurukame (ios/ReviewViewController.swift, SuccessAnimation.swift),
// Copyright David Sansome, Apache License 2.0: see assets/TSURUKAME-LICENSE.txt.
// Rewritten in Kotlin and changed for this app.
package com.example.durtlesreborn.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.durtlesreborn.R
import com.example.durtlesreborn.data.LessonInfo
import com.example.durtlesreborn.session.QuestionKind
import com.example.durtlesreborn.session.QuizCard
import com.example.durtlesreborn.session.SubjectKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private fun QuestionKind.subjectKind() = when (this) {
    QuestionKind.RADICAL_NAME -> SubjectKind.RADICAL
    QuestionKind.KANJI_MEANING, QuestionKind.KANJI_READING, QuestionKind.KANJI_ONYOMI, QuestionKind.KANJI_KUNYOMI -> SubjectKind.KANJI
    else -> SubjectKind.VOCABULARY
}

private fun QuestionKind.subjectPrompt() = when (subjectKind()) {
    SubjectKind.RADICAL -> "Radical"
    SubjectKind.KANJI -> "Kanji"
    else -> "Vocabulary"
}

private fun QuestionKind.taskPrompt() = when (this) {
    QuestionKind.RADICAL_NAME -> "Name"
    QuestionKind.KANJI_ONYOMI -> "On'yomi"
    QuestionKind.KANJI_KUNYOMI -> "Kun'yomi"
    else -> if (reading) "Reading" else "Meaning"
}

/**
 * What the input field tells the keyboard. Android apps cannot switch to another keyboard the way
 * iOS apps can, but they can say which language a field wants: a keyboard that honours the hint
 * changes mode by itself as the question changes. Without such a keyboard, romaji typed on an
 * English keyboard is still converted to kana by the app.
 */
internal fun answerKeyboardOptions(kana: Boolean, switchKeyboard: Boolean) = KeyboardOptions(
    capitalization = KeyboardCapitalization.None,
    autoCorrectEnabled = false,
    keyboardType = if (switchKeyboard && !kana) KeyboardType.Ascii else KeyboardType.Text,
    imeAction = ImeAction.Done,
    hintLocales = when {
        !switchKeyboard -> null
        kana -> LocaleList("ja-JP")
        else -> LocaleList("en-US")
    },
)

/**
 * Tsurukame's review screen: the item on its subject's gradient filling the screen, a prompt strip
 * saying what to answer, the answer field, and the keyboard right below. A correct answer goes
 * straight to the next question with a burst of sparks. A wrong one turns the answer red and slides
 * the item's details up where the keyboard was.
 */
@Composable
fun QuizScreen(
    state: UiState.Quiz,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDontKnow: () -> Unit,
    onSkip: () -> Unit,
    onUndo: () -> Unit,
    onIgnoreMistake: () -> Unit,
    onWrapUp: () -> Unit,
    onLeave: () -> Unit,
    loadInfo: suspend (Long) -> LessonInfo?,
    onPlayAudio: (Long) -> Unit,
) {
    val tk = LocalTk.current
    val card = state.card
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var openSubject by remember { mutableStateOf<Long?>(null) }
    val wrong = state.answered && state.feedback?.correct == false
    var revealed by remember(card.itemId, card.kind, state.answered) { mutableStateOf(false) }
    val showDetails = wrong && (state.showAnswerOnWrong || revealed)
    var confirmEnd by rememberSaveable { mutableStateOf(false) }
    // Leaving asks first once something has been answered, from Back as from the menu. Undo is in the menu.
    val endSession = { if (state.progress.anyAnswered) confirmEnd = true else onLeave() }
    BackHandler(enabled = openSubject == null) { if (menuOpen) menuOpen = false else endSession() }

    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(card.itemId, card.kind, showDetails, state.answered, menuOpen) {
        when {
            showDetails || menuOpen -> {
                focusManager.clearFocus()
                keyboard?.hide()
            }
            !state.answered && state.keepKeyboardOpen -> {
                runCatching { focus.requestFocus() }
                keyboard?.show()
            }
        }
    }
    // The keyboard already vibrated for its own Enter key, so only the on-screen tick adds one.
    var submittedByKeyboard by remember { mutableStateOf(false) }
    LaunchedEffect(state.success?.id) {
        if (state.success != null && !submittedByKeyboard) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    // The text keeps the keyboard's selection and composition unless the romaji conversion changed it.
    var field by remember(card.itemId, card.kind) { mutableStateOf(TextFieldValue("")) }
    if (field.text != state.input) field = TextFieldValue(state.input, TextRange(state.input.length))

    val shake = remember { Animatable(0f) }
    val density = LocalDensity.current
    LaunchedEffect(state.feedback) {
        if (state.feedback?.retry == true) {
            val distance = with(density) { 10.dp.toPx() }
            repeat(3) {
                shake.animateTo(distance, tween(50, easing = LinearEasing))
                shake.animateTo(-distance, tween(100, easing = LinearEasing))
            }
            shake.animateTo(0f, tween(50, easing = LinearEasing))
        }
    }

    val detailsShown by animateFloatAsState(if (showDetails) 1f else 0f, tween(250), label = "details")
    val gradient = tk.gradient(card.kind.subjectKind())
    val top by animateColorAsState(gradient[0], tween(250), label = "gradient top")
    val bottom by animateColorAsState(gradient[1], tween(250), label = "gradient bottom")
    val promptGradient = if (card.kind.reading) TkFixed.reading else tk.meaning
    val promptTop by animateColorAsState(promptGradient[0], tween(250), label = "prompt top")
    val promptBottom by animateColorAsState(promptGradient[1], tween(250), label = "prompt bottom")
    val promptText by animateColorAsState(if (card.kind.reading) TkFixed.readingText else TkFixed.meaningText, tween(250), label = "prompt text")
    var answerTop by remember { mutableStateOf(Offset.Zero) }
    var answerWidth by remember { mutableStateOf(0) }
    val canSubmit = state.answered || state.input.isNotBlank()

    Box(Modifier.fillMaxSize().background(tk.background)) {
        Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
            Box(
                Modifier.fillMaxWidth().weight(1f).background(Brush.verticalGradient(listOf(top, bottom)))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = state.answered && card.kind.hasAudio,
                    ) { onPlayAudio(card.itemId) },
            ) {
                QuizStatusBar(state, onMenu = { menuOpen = true })
                val scale = 1f - 0.3f * detailsShown
                BasicText(
                    card.characters,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 16.dp, vertical = 60.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .semantics { contentDescription = "Japanese ${card.kind.subjectPrompt()}. Question" },
                    style = TextStyle(
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        localeList = JapaneseLocale,
                        shadow = Shadow(Color.Black.copy(alpha = 0.2f), Offset(0f, with(density) { 1.dp.toPx() }), with(density) { 4.dp.toPx() }),
                    ),
                    maxLines = 2,
                    autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 60.sp, stepSize = 2.sp),
                )
                QuizNotice(state, Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
            }
            Box(Modifier.fillMaxWidth().height(40.dp).background(Brush.verticalGradient(listOf(promptTop, promptBottom)))) {
                Text(
                    buildAnnotatedString {
                        append(card.kind.subjectPrompt())
                        append(' ')
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(card.kind.taskPrompt()) }
                    },
                    modifier = Modifier.align(Alignment.Center),
                    style = TextStyle(color = promptText, fontSize = 15.sp),
                )
                if (state.showSrsIndicator) SrsDots(card.stage, Modifier.align(Alignment.CenterEnd).padding(end = 12.dp))
            }
            Box(
                Modifier.fillMaxWidth().height(64.dp).background(tk.background).onGloballyPositioned {
                    answerTop = it.positionInRoot()
                    answerWidth = it.size.width
                },
            ) {
                BasicTextField(
                    value = field,
                    onValueChange = { value ->
                        if (!state.answered) {
                            field = value
                            onInputChange(value.text)
                        } else if (state.canUndo && value.text.length < state.input.length) {
                            onUndo()
                        }
                    },
                    readOnly = showDetails,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = if (wrong) TkFixed.red else tk.label,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                        localeList = if (card.kanaInput) JapaneseLocale else null,
                    ),
                    cursorBrush = SolidColor(TkFixed.tint),
                    keyboardOptions = answerKeyboardOptions(card.kanaInput, state.switchKeyboard),
                    keyboardActions = KeyboardActions(onDone = {
                        if (canSubmit) {
                            submittedByKeyboard = true
                            onSubmit()
                        }
                    }),
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 62.dp)
                        .offset { IntOffset(shake.value.roundToInt(), 0) }
                        .focusRequester(focus)
                        .onPreviewKeyEvent { event ->
                            val delete = event.key == Key.Backspace || event.key == Key.Delete
                            if (delete && state.answered && state.canUndo && event.type == KeyEventType.KeyDown) {
                                onUndo()
                                true
                            } else {
                                false
                            }
                        },
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth(), Alignment.Center) {
                            if (field.text.isEmpty()) {
                                TkText(
                                    if (card.kanaInput) "答え" else "Your Response",
                                    size = 24.sp,
                                    color = tk.placeholder,
                                    japanese = card.kanaInput,
                                    maxLines = 1,
                                )
                            }
                            inner()
                        }
                    },
                )
                if (wrong) {
                    AnswerActions(
                        canReveal = !showDetails,
                        canUndo = state.canUndo,
                        onReveal = { revealed = true },
                        onUndo = onUndo,
                        onMarkCorrect = onIgnoreMistake,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                }
                Box(
                    Modifier.align(Alignment.CenterEnd).size(width = 53.dp, height = 58.dp)
                        .clickable(enabled = canSubmit) {
                            submittedByKeyboard = false
                            onSubmit()
                        }
                        .semantics { contentDescription = if (state.answered) "Next question" else "Submit answer" },
                    Alignment.Center,
                ) {
                    if (state.answered) {
                        Image(painterResource(R.drawable.tk_forward), null, Modifier.size(28.dp), colorFilter = ColorFilter.tint(TkFixed.tint))
                    } else {
                        TickCircle(if (canSubmit) TkFixed.tint else tk.grey80)
                    }
                }
            }
            if (detailsShown > 0f) {
                Box(Modifier.fillMaxWidth().weight(1.8f * detailsShown).background(tk.grouped)) {
                    val info by produceState<LessonInfo?>(null, card.itemId) { value = loadInfo(card.itemId) }
                    val loaded = info
                    if (loaded == null) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                    } else {
                        SubjectDetailsTable(
                            loaded,
                            onOpen = { openSubject = it },
                            onPlayAudio = onPlayAudio,
                            hideMeaning = !card.kind.meaning && !card.meaningAttempted,
                            hideReading = !card.kind.reading && !card.readingAttempted,
                        )
                    }
                }
            }
        }

        state.success?.let { success ->
            SparkBurst(success.id, answerTop + Offset(answerWidth / 2f, with(density) { 32.dp.toPx() }))
            if (success.itemFinished) PlusOne(success.id, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 16.dp, end = 76.dp))
            success.move?.takeIf { it.to > it.from }?.let { move ->
                StagePopup(success.id, move.to, answerTop.y, Modifier.align(Alignment.TopCenter))
            }
        }

        AnimatedVisibility(menuOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { menuOpen = false },
            )
        }
        AnimatedVisibility(menuOpen, enter = slideInHorizontally { -it }, exit = slideOutHorizontally { -it }) {
            QuizMenu(
                state,
                onClose = { menuOpen = false },
                onWrapUp = onWrapUp,
                onLeave = endSession,
                onSkip = onSkip,
                onDontKnow = onDontKnow,
                onUndo = onUndo,
            )
        }
        openSubject?.let { id -> SubjectOverlay(id, loadInfo, onDismiss = { openSubject = null }, onPlayAudio = onPlayAudio) }
        if (confirmEnd) {
            TkActionSheet(
                title = "End review session?",
                message = "Your answers are saved, and you can carry on later from the home screen.",
                action = "End review session",
                onAction = {
                    confirmEnd = false
                    onLeave()
                },
                onDismiss = { confirmEnd = false },
            )
        }
    }
}

/** The progress bar, the menu button, and the success rate, done and remaining counts. */
@Composable
private fun QuizStatusBar(state: UiState.Quiz, onMenu: () -> Unit) {
    val progress = state.progress
    val done = progress.answeredItems.toFloat() / progress.totalItems.coerceAtLeast(1)
    Column(Modifier.fillMaxWidth().statusBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(5.dp).background(Color.White.copy(alpha = 0.25f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(done).background(Color.White.copy(alpha = 0.6f)))
        }
        Row(Modifier.fillMaxWidth().height(56.dp).padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clickable(onClick = onMenu).semantics { contentDescription = "Menu" }, Alignment.Center) {
                Image(painterResource(R.drawable.tk_menu), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(Color.White))
            }
            Spacer(Modifier.weight(1f))
            if (progress.wrappingUp) QuizStat(R.drawable.tk_time, "Wrap up", "Wrapping up")
            QuizStat(R.drawable.tk_thumb, "${state.successRate}%", "${state.successRate} percent correct so far")
            QuizStat(R.drawable.tk_tick, "${progress.answeredItems}", "${progress.answeredItems} done")
            QuizStat(R.drawable.tk_inbox, "${(progress.totalItems - progress.answeredItems).coerceAtLeast(0)}", "${progress.totalItems - progress.answeredItems} remaining")
        }
    }
}

@Composable
private fun QuizStat(icon: Int, text: String, description: String) {
    Row(
        Modifier.padding(start = 12.dp).semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Image(painterResource(icon), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Color.White))
        TkText(text, size = 14.sp, color = Color.White, maxLines = 1)
    }
}

/** A short message over the bottom of the question: why an answer was not accepted, or a near miss. */
@Composable
private fun QuizNotice(state: UiState.Quiz, modifier: Modifier) {
    val feedback = state.feedback
    val digraph = feedback?.verdict?.digraphMatch
    var note by remember(state.success?.id) { mutableStateOf(state.success?.note) }
    LaunchedEffect(state.success?.id) {
        if (note != null) {
            kotlinx.coroutines.delay(2000)
            note = null
        }
    }
    val text = when {
        digraph != null -> "Watch the small kana: ${digraph.regularKana} and ${digraph.smallKana} are different."
        feedback?.retry == true -> "That is not an answer to this question. Try again."
        else -> note
    } ?: return
    TkText(
        text,
        size = 14.sp,
        color = Color.White,
        align = TextAlign.Center,
        modifier = modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** Filled dots for the steps done within an SRS group, hollow for the next one, in the group's colour. */
@Composable
private fun SrsDots(stage: Int, modifier: Modifier) {
    val text = buildAnnotatedString {
        fun dots(text: String, color: Color) = withStyle(SpanStyle(color = color)) { append(text) }
        when (stage) {
            1 -> dots("•◦◦◦", TkFixed.apprentice)
            2 -> dots("••◦◦", TkFixed.apprentice)
            3 -> dots("•••◦", TkFixed.apprentice)
            4 -> {
                dots("••••", TkFixed.apprentice)
                dots("◦", TkFixed.guru)
            }
            5 -> dots("•◦", TkFixed.guru)
            6 -> {
                dots("••", TkFixed.guru)
                dots("◦", TkFixed.master)
            }
            7 -> {
                dots("•", TkFixed.master)
                dots("◦", TkFixed.enlightened)
            }
            8 -> {
                dots("•", TkFixed.enlightened)
                dots("◦", TkFixed.burned)
            }
            9 -> dots("•", TkFixed.burned)
        }
    }
    if (text.isNotEmpty()) {
        Text(
            text,
            modifier = modifier.semantics { contentDescription = "SRS stage $stage" },
            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun TickCircle(color: Color) {
    Canvas(Modifier.size(28.dp)) {
        val stroke = 2.dp.toPx()
        drawCircle(color, style = Stroke(stroke))
        val w = size.width
        drawLine(color, Offset(w * 0.28f, w * 0.52f), Offset(w * 0.44f, w * 0.68f), stroke)
        drawLine(color, Offset(w * 0.44f, w * 0.68f), Offset(w * 0.72f, w * 0.34f), stroke)
    }
}

/** Left of the field after a wrong answer: reveal the details, or correct the verdict. */
@Composable
private fun AnswerActions(
    canReveal: Boolean,
    canUndo: Boolean,
    onReveal: () -> Unit,
    onUndo: () -> Unit,
    onMarkCorrect: () -> Unit,
    modifier: Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Row(modifier) {
        if (canReveal) {
            Box(Modifier.size(width = 44.dp, height = 62.dp).clickable(onClick = onReveal).semantics { contentDescription = "Reveal the answer" }, Alignment.Center) {
                Icon(AppIcons.Visibility, null, tint = TkFixed.tint)
            }
        }
        Box(Modifier.size(width = 44.dp, height = 62.dp).clickable { open = true }.semantics { contentDescription = "Change this result" }, Alignment.Center) {
            Image(painterResource(R.drawable.tk_add), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(TkFixed.tint))
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(text = { Text("Mark correct") }, onClick = { open = false; onMarkCorrect() })
                DropdownMenuItem(text = { Text("Undo and try again") }, enabled = canUndo, onClick = { open = false; onUndo() })
            }
        }
    }
}

/** The slide-out menu behind the menu button. */
@Composable
private fun QuizMenu(
    state: UiState.Quiz,
    onClose: () -> Unit,
    onWrapUp: () -> Unit,
    onLeave: () -> Unit,
    onSkip: () -> Unit,
    onDontKnow: () -> Unit,
    onUndo: () -> Unit,
) {
    val tk = LocalTk.current
    // The menu is pale in light mode, so the status bar needs dark icons while it is open.
    val view = LocalView.current
    DisposableEffect(tk.isDark) {
        val controller = WindowCompat.getInsetsController((view.context as Activity).window, view)
        controller.isAppearanceLightStatusBars = !tk.isDark
        onDispose { controller.isAppearanceLightStatusBars = false }
    }
    Column(Modifier.fillMaxHeight().width(300.dp).background(tk.grouped).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())) {
        TkSection("This question") {
            TkRow("Skip for now", enabled = !state.answered, onClick = { onClose(); onSkip() })
            TkRow("I don't know", enabled = !state.answered, onClick = { onClose(); onDontKnow() })
            TkRow("Undo last answer", enabled = state.canUndo, onClick = { onClose(); onUndo() })
        }
        TkSection("End review session") {
            TkRow(
                "End review session",
                onClick = { onClose(); onLeave() },
                leading = { MenuIcon(R.drawable.tk_cancel) },
            )
            val remaining = state.progress.totalItems - state.progress.answeredItems
            TkRow(
                if (state.progress.wrappingUp) "Wrap up ($remaining to go)" else "Wrap up",
                enabled = !state.answered && !state.progress.wrappingUp,
                onClick = { onClose(); onWrapUp() },
                leading = { MenuIcon(R.drawable.tk_time) },
            )
        }
    }
}

@Composable
private fun MenuIcon(icon: Int) {
    Image(painterResource(icon), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(LocalTk.current.grey33))
}

private class Spark(val angle: Float, val distance: Float, val radius: Float, val duration: Float, val color: Color)

/** Sparks flying up from the answer field after a correct answer. */
@Composable
private fun SparkBurst(id: Long, origin: Offset) {
    val time = remember(id) { Animatable(0f) }
    LaunchedEffect(id) { time.animateTo(1f, tween(700, easing = LinearEasing)) }
    val sparks = remember(id) {
        List(36) {
            Spark(
                angle = Random.nextFloat() * PI.toFloat() * 2f,
                distance = 40f + Random.nextFloat() * 120f,
                radius = 2f + Random.nextFloat() * 5f,
                duration = 0.7f + Random.nextFloat() * 0.3f,
                color = lerp(TkFixed.explosion1, TkFixed.explosion2, Random.nextFloat()),
            )
        }
    }
    if (time.value >= 1f) return
    Canvas(Modifier.fillMaxSize()) {
        for (spark in sparks) {
            val t = (time.value / spark.duration).coerceAtMost(1f)
            val travel = (1f - (1f - t) * (1f - t)) * spark.distance * density
            val center = origin + Offset(sin(spark.angle) * travel, -cos(spark.angle) * travel)
            val shrink = if (t < 0.2f) 1f else 1f - (t - 0.2f) / 0.8f
            val alpha = (t / 0.2f).coerceAtMost(1f)
            if (shrink > 0f) drawCircle(spark.color.copy(alpha = alpha), spark.radius * density * shrink, center)
        }
    }
}

/** "+1" rising into the done counter when an item is finished. */
@Composable
private fun PlusOne(id: Long, modifier: Modifier) {
    val time = remember(id) { Animatable(0f) }
    LaunchedEffect(id) { time.animateTo(1f, tween(1500, easing = LinearEasing)) }
    val t = time.value
    if (t >= 1f) return
    val leaving = ((t - 0.7f) / 0.3f).coerceIn(0f, 1f)
    val appear = (t / 0.2f).coerceAtMost(1f)
    TkText(
        "+1",
        size = 20.sp,
        color = Color.White,
        weight = FontWeight.Bold,
        modifier = modifier.graphicsLayer {
            alpha = appear * (1f - 0.9f * leaving)
            val scale = (0.1f + 0.9f * appear) * (1f - 0.9f * leaving)
            scaleX = scale
            scaleY = scale
            translationY = 30.dp.toPx() * (1f - leaving)
        },
    )
}

/** The new SRS group springing out of the answer field when an item reaches its first stage. */
@Composable
private fun StagePopup(id: Long, stage: Int, answerY: Float, modifier: Modifier) {
    val (label, color) = when (stage) {
        5 -> "Guru" to TkFixed.guru
        7 -> "Master" to TkFixed.master
        8 -> "Enlightened" to TkFixed.enlightened
        9 -> "Burned" to TkFixed.burned
        else -> return
    }
    val rise = remember(id) { Animatable(0f) }
    val fade = remember(id) { Animatable(1f) }
    LaunchedEffect(id) { rise.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)) }
    LaunchedEffect(id) {
        kotlinx.coroutines.delay(2550)
        fade.animateTo(0f, tween(300))
    }
    if (fade.value <= 0f) return
    val density = LocalDensity.current
    TkText(
        label,
        size = 16.sp,
        color = Color.White,
        modifier = modifier
            .offset { IntOffset(0, (answerY - with(density) { 100.dp.toPx() } * rise.value).roundToInt()) }
            .graphicsLayer { alpha = fade.value }
            .clip(RoundedCornerShape(5.dp)).background(color).padding(6.dp),
    )
}
