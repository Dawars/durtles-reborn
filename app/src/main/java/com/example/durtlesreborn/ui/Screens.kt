// Based on Tsurukame (ios/ReviewSummaryViewController.swift),
// Copyright David Sansome, Apache License 2.0: see assets/TSURUKAME-LICENSE.txt.
// Rewritten in Kotlin and changed for this app.
package com.example.durtlesreborn.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.durtlesreborn.R
import com.example.durtlesreborn.data.LessonInfo
import com.example.durtlesreborn.session.SessionType

private const val TOKEN_PAGE = "https://www.wanikani.com/settings/personal_access_tokens"

@Composable
fun AppRoot(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val audioDownload by viewModel.audioDownload.collectAsState()
    val tk = LocalTk.current
    // Screens with a gradient under the status bar have white status icons, as on iOS.
    val view = LocalView.current
    val gradientUnderStatusBar = state is UiState.Home || state is UiState.Quiz || state is UiState.Lesson ||
        state is UiState.Detail || state is UiState.NeedsToken
    SideEffect {
        val controller = WindowCompat.getInsetsController((view.context as Activity).window, view)
        controller.isAppearanceLightStatusBars = !tk.isDark && !gradientUnderStatusBar
        controller.isAppearanceLightNavigationBars = !tk.isDark
    }
    // Back from a sub-screen goes to where it was opened from instead of closing the app.
    BackHandler(enabled = state is UiState.Settings || state is UiState.AudioLevels || state is UiState.Search || state is UiState.Detail || state is UiState.StatsView || state is UiState.Levels || state is UiState.StageItems) {
        when (state) {
            is UiState.Settings -> viewModel.closeSettings()
            is UiState.AudioLevels -> viewModel.closeAudioLevels()
            is UiState.Search -> viewModel.closeSearch()
            is UiState.Detail -> viewModel.closeDetail()
            is UiState.StatsView -> viewModel.closeStats()
            is UiState.Levels -> viewModel.closeLevels()
            is UiState.StageItems -> viewModel.closeStage()
            else -> Unit
        }
    }
    Surface(Modifier.fillMaxSize(), color = tk.grouped) {
        when (val s = state) {
            UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            is UiState.NeedsToken -> TokenScreen(s, viewModel::submitToken)
            is UiState.Home -> DashboardScreen(
                s,
                DashboardActions(
                    startReviews = viewModel::startReviews,
                    startLessons = viewModel::startLessons,
                    openSearch = viewModel::openSearch,
                    openLevels = { viewModel.openLevels(it) },
                    openRemaining = { viewModel.openLevels(it, remainingOnly = true) },
                    openSettings = viewModel::openSettings,
                    openStats = viewModel::openStats,
                    openStage = viewModel::openStage,
                    refresh = viewModel::sync,
                    dismissMessage = viewModel::dismissMessage,
                    resumeSession = viewModel::resumeSession,
                    discardSession = viewModel::discardSavedSession,
                ),
            )
            is UiState.StageItems -> StageScreen(s, viewModel::setStageKind, viewModel::openDetail, viewModel::closeStage)
            is UiState.Levels -> LevelsScreen(s, viewModel::ensureLevel, viewModel::openDetail, viewModel::closeLevels)
            is UiState.Lesson -> LessonScreen(s, viewModel::onLessonGoTo, viewModel::onLeaveSession, viewModel::lessonInfo, viewModel::playAudio)
            is UiState.Settings -> SettingsScreen(
                s, viewModel::updateSettings, viewModel::closeSettings, viewModel::settingsBackup, viewModel::restoreSettings, viewModel::showSettingsMessage,
                viewModel::syncNow, viewModel::signOut, audioDownload, viewModel::openAudioLevels,
            )
            is UiState.AudioLevels -> Box(Modifier.safeDrawingPadding()) {
                AudioLevelsScreen(
                    s, audioDownload, viewModel::closeAudioLevels, viewModel::deleteLevelAudio,
                    viewModel::downloadLevelAudio, viewModel::cancelAudioDownload,
                )
            }
            is UiState.Search -> SearchScreen(s, viewModel::onSearchCriteria, viewModel::openDetail, viewModel::startSelfStudy, viewModel::closeSearch)
            is UiState.Detail -> ItemDetailScreen(s, viewModel::closeDetail, viewModel::openDetail, viewModel::playAudio)
            is UiState.StatsView -> Box(Modifier.safeDrawingPadding()) { StatsScreen(s.stats, viewModel::closeStats) }
            is UiState.Quiz -> QuizScreen(
                s, viewModel::onInputChange, viewModel::onSubmit, viewModel::onDontKnow,
                viewModel::onSkip, viewModel::onUndo, viewModel::onIgnoreMistake, viewModel::onWrapUp,
                viewModel::onLeaveSession, viewModel::lessonInfo, viewModel::playAudio,
            )
            is UiState.Summary -> SummaryScreen(s, viewModel::closeSummary, viewModel::lessonInfo, viewModel::playAudio)
        }
    }
}

/** Sign-in over the turtle engraving. WaniKani only allows personal access tokens. */
@Composable
private fun TokenScreen(state: UiState.NeedsToken, onSubmit: (String) -> Unit) {
    var token by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    val tk = LocalTk.current
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Image(painterResource(R.drawable.tk_launch_screen), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.65f)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TkText("Sign in to your WaniKani account", color = Color.White, weight = FontWeight.SemiBold, align = TextAlign.Center)
            BasicTextField(
                value = token,
                onValueChange = { token = it },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                textStyle = TextStyle(color = tk.label, fontSize = 14.sp),
                cursorBrush = SolidColor(TkFixed.tint),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit(token) }),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(
                        Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(6.dp)).background(tk.cell).padding(horizontal = 10.dp),
                        Alignment.CenterStart,
                    ) {
                        if (token.isEmpty()) TkText("API Token", size = 14.sp, color = tk.placeholder)
                        inner()
                    }
                },
            )
            state.error?.let { TkText(it, size = 13.sp, color = Color.White, weight = FontWeight.SemiBold, align = TextAlign.Center) }
            val enabled = token.isNotBlank() && !state.checking
            Box(
                Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(6.dp)).background(TkFixed.tint.copy(alpha = if (enabled) 1f else 0.5f))
                    .clickable(enabled = enabled) { onSubmit(token) },
                Alignment.Center,
            ) {
                TkText(if (state.checking) "Checking\u2026" else "Sign in", color = Color.White, weight = FontWeight.SemiBold)
            }
            TkText(
                "Create an API token",
                size = 15.sp,
                color = Color.White,
                modifier = Modifier.clickable { uriHandler.openUri(TOKEN_PAGE) }.padding(8.dp),
            )
            TkText(
                "An unofficial, third-party app for WaniKani, not made or endorsed by WaniKani. Give the token the permissions to start assignments and create reviews. The app never asks for your password.",
                size = 13.sp,
                color = Color.White,
                align = TextAlign.Center,
            )
        }
    }
}

/** How the session went, then the items that were missed, by level. */
@Composable
private fun SummaryScreen(
    state: UiState.Summary,
    onDone: () -> Unit,
    loadInfo: suspend (Long) -> LessonInfo?,
    onPlayAudio: (Long) -> Unit,
) {
    val tk = LocalTk.current
    var openId by remember { mutableStateOf<Long?>(null) }
    BackHandler(enabled = openId == null, onBack = onDone)
    val title = when (state.type) {
        SessionType.LESSON -> "Lesson Summary"
        SessionType.SELF_STUDY -> "Self-study Summary"
        SessionType.REVIEW -> "Review Summary"
    }
    Box(Modifier.fillMaxSize().background(tk.grouped)) {
        Column(Modifier.fillMaxSize()) {
            TkNavBar(title, actions = { TkBarButton("Done", bold = true, onClick = onDone) })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
                val footer = when {
                    state.type == SessionType.SELF_STUDY -> "Self-study results stay on your phone. They do not change your progress or reach WaniKani."
                    state.dryRun -> "Dry run: nothing was saved or sent to WaniKani."
                    else -> null
                }
                TkSection("Summary", footer = footer) {
                    val percent = if (state.totalItems == 0) 0 else state.perfectItems * 100 / state.totalItems
                    TkRow("Correct answers", value = "$percent% (${state.perfectItems}/${state.totalItems})")
                }
                for ((level, items) in state.mistakes.groupBy { it.level }.toSortedMap(compareByDescending { it })) {
                    TkSection(if (level == state.currentLevel) "Current level ($level)" else "Level $level") {
                        for (item in items) {
                            SubjectRow(
                                item.title, item.kind, item.reading, item.meaning,
                                readingWrong = item.readingWrong,
                                meaningWrong = item.meaningWrong,
                                onClick = { openId = item.id },
                            )
                        }
                    }
                }
            }
        }
        openId?.let { SubjectOverlay(it, loadInfo, onDismiss = { openId = null }, onPlayAudio = onPlayAudio) }
    }
}
