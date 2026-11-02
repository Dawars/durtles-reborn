// Based on Tsurukame (ios/SettingsViewController.swift, ReviewSettingsViewController.swift),
// Copyright David Sansome, Apache License 2.0: see assets/TSURUKAME-LICENSE.txt.
// Rewritten in Kotlin and changed for this app.
package com.example.durtlesreborn.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.durtlesreborn.BuildConfig
import com.example.durtlesreborn.answer.CloseEnoughAction
import com.example.durtlesreborn.notify.WidgetUpdater
import com.example.durtlesreborn.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private val reviewOrders = listOf(
    "SHUFFLE", "DUE", "LEVEL", "TYPE", "SRS", "HIGHEST_LEVEL_THEN_TYPE",
    "LEVEL_THEN_TYPE", "LEVEL_THEN_SRS", "TYPE_THEN_LEVEL", "TYPE_THEN_SRS", "SRS_THEN_TYPE", "SRS_THEN_LEVEL",
    "LEVEL_THEN_TYPE_THEN_SRS", "LEVEL_THEN_SRS_THEN_TYPE", "TYPE_THEN_LEVEL_THEN_SRS",
    "TYPE_THEN_SRS_THEN_LEVEL", "SRS_THEN_TYPE_THEN_LEVEL", "SRS_THEN_LEVEL_THEN_TYPE",
)

private val notificationFrequencies = listOf("ONLY_NEW_REVIEWS", "ONCE_PER_HOUR", "CONTINUOUSLY")

private val notificationPriorities = listOf("MIN", "LOW", "DEFAULT", "HIGH", "MAX")

private val notificationCategories = listOf(
    "CALL", "NAVIGATION", "MESSAGE", "EMAIL", "EVENT", "PROMO", "ALARM", "PROGRESS", "SOCIAL", "ERROR",
    "TRANSPORT", "SYSTEM", "SERVICE", "REMINDER", "RECOMMENDATION", "STATUS",
)

private val lessonOrders = listOf(
    "SHUFFLE", "LEVEL_THEN_TYPE", "RADICALS_THEN_LEVEL_THEN_TYPE", "TYPE_THEN_LEVEL", "LEVEL_THEN_SHUFFLE", "TYPE_THEN_SHUFFLE",
)

private val priorities = listOf(
    "NONE", "RADICALS_FIRST", "LEVEL_UP_FIRST", "CURRENT_LEVEL_RADICAL_KANJI_FIRST", "CURRENT_LEVEL_FIRST",
)

/** "LEVEL_THEN_TYPE" becomes "Level, then type". */
private fun pretty(name: String) = name.lowercase().replace("_then_", ", then ").replace('_', ' ').replaceFirstChar { it.uppercase() }

/** Tsurukame's review orders, as the combination of settings each one means here. */
internal val tsurukameReviewOrders: List<Pair<String, (AppSettings) -> AppSettings>> = listOf(
    "Random" to reviewOrder("SHUFFLE"),
    "Ascending SRS stage" to reviewOrder("SRS"),
    "Descending SRS stage" to reviewOrder("SRS", reversed = true),
    "Current level first" to reviewOrder("HIGHEST_LEVEL_THEN_TYPE"),
    "Lowest level first" to reviewOrder("LEVEL_THEN_TYPE"),
    "Newest available first" to reviewOrder("DUE", reversed = true),
    "Oldest available first" to reviewOrder("DUE"),
    "Longest relative wait" to reviewOrder("SHUFFLE", overdueFirst = true),
)

private fun reviewOrder(order: String, reversed: Boolean = false, overdueFirst: Boolean = false): (AppSettings) -> AppSettings = {
    it.copy(reviewOrder = order, reviewOrderReversed = reversed, reviewOrderOverdueFirst = overdueFirst, reviewOrderPriority = "NONE")
}

/** The name of the order [s] is set to, or null if the advanced settings make it something else. */
internal fun tsurukameReviewOrderName(s: AppSettings): String? =
    tsurukameReviewOrders.firstOrNull { (_, apply) -> apply(s) == s }?.first

private enum class SettingsPage(val title: String) {
    ROOT("Settings"),
    APPEARANCE("Appearance & Notifications"),
    LESSONS("Lessons"),
    REVIEWS("Reviews"),
    SUBJECTS("Radicals, Kanji & Vocabulary"),
}

/** A list of options with a checkmark on the current one, opened from a row. */
private class ChoicePage(val title: String, val current: String, val options: List<Pair<String, String>>, val onPick: (String) -> Unit)

/**
 * Settings as grouped tables with pushed sub-pages.
 */
@Composable
fun SettingsScreen(
    state: UiState.Settings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit,
    onExport: suspend () -> String,
    onImport: (String) -> Unit,
    onMessage: (String) -> Unit,
    onSync: () -> Unit,
    onSignOut: () -> Unit,
    audioDownload: AudioDownload,
    onOpenAudio: () -> Unit,
) {
    val tk = LocalTk.current
    var page by rememberSaveable { mutableStateOf(SettingsPage.ROOT) }
    var choice by remember { mutableStateOf<ChoicePage?>(null) }
    val goBack = {
        when {
            choice != null -> choice = null
            page != SettingsPage.ROOT -> page = SettingsPage.ROOT
            else -> onBack()
        }
    }
    BackHandler(enabled = choice != null || page != SettingsPage.ROOT) { goBack() }
    val s = state.settings
    val openChoice = { title: String, current: String, options: List<Pair<String, String>>, onPick: (String) -> Unit ->
        choice = ChoicePage(title, current, options, onPick)
    }
    Column(Modifier.fillMaxSize().background(tk.grouped)) {
        val open = choice
        TkNavBar(
            open?.title ?: page.title,
            onBack = goBack,
            backLabel = if (open != null) page.title else if (page != SettingsPage.ROOT) "Settings" else "Back",
        )
        key(page, open) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
                when {
                    open != null -> ChoiceList(open) { choice = null }
                    page == SettingsPage.ROOT -> RootPage(s, state.message, onChange, onExport, onImport, onMessage, onSync, onSignOut, audioDownload, onOpenAudio) { page = it }
                    page == SettingsPage.APPEARANCE -> AppearancePage(s, onChange, openChoice)
                    page == SettingsPage.LESSONS -> LessonsPage(s, onChange, openChoice)
                    page == SettingsPage.SUBJECTS -> SubjectsPage(s, onChange)
                    else -> ReviewsPage(s, onChange, openChoice)
                }
            }
        }
    }
}

@Composable
private fun RootPage(
    s: AppSettings,
    message: String?,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onExport: suspend () -> String,
    onImport: (String) -> Unit,
    onMessage: (String) -> Unit,
    onSync: () -> Unit,
    onSignOut: () -> Unit,
    audioDownload: AudioDownload,
    onOpenAudio: () -> Unit,
    onOpenPage: (SettingsPage) -> Unit,
) {
    TkSection("Settings") {
        TkRow(SettingsPage.APPEARANCE.title, chevron = true, onClick = { onOpenPage(SettingsPage.APPEARANCE) })
        TkRow(SettingsPage.LESSONS.title, chevron = true, onClick = { onOpenPage(SettingsPage.LESSONS) })
        TkRow(SettingsPage.REVIEWS.title, chevron = true, onClick = { onOpenPage(SettingsPage.REVIEWS) })
        TkRow(SettingsPage.SUBJECTS.title, chevron = true, onClick = { onOpenPage(SettingsPage.SUBJECTS) })
    }
    TkSection(
        "Keyboard",
        footer = "Tells the keyboard to use Japanese on reading questions and English on meaning questions. " +
            "It needs a keyboard that follows what the field asks for. With any other keyboard, romaji is converted to kana for you.",
    ) {
        TkSwitchRow("Switch keyboard automatically", checked = s.autoSwitchKeyboard) { onChange { it.copy(autoSwitchKeyboard = !it.autoSwitchKeyboard) } }
    }
    TkSection(
        "Audio",
        footer = "Each level downloads by itself once you reach it, so pronunciations also play offline.",
    ) {
        TkRow("Offline audio", subtitle = audioSummary(audioDownload), chevron = true, onClick = onOpenAudio)
    }
    TkSection("Syncing") {
        TkSwitchRow("Allow background sync", "Send reviews and fetch changes every hour when online.", s.enableBackgroundSync) {
            onChange { it.copy(enableBackgroundSync = !it.enableBackgroundSync) }
        }
        TkSwitchRow("Sync when the app is opened", checked = s.syncOnOpen) { onChange { it.copy(syncOnOpen = !it.syncOnOpen) } }
        TkRow("Sync now", titleColor = TkFixed.tint, onClick = onSync)
    }
    BackupSection(message, onExport, onImport, onMessage)
    TkSection(
        "Developer",
        footer = "Sessions run as normal, but nothing is saved on this phone or sent to WaniKani. Results already waiting to be sent are still sent.",
    ) {
        TkSwitchRow("Dry run (practice mode)", checked = s.dryRun) { onChange { it.copy(dryRun = !it.dryRun) } }
    }
    TkSection("Diagnostics") {
        TkRow("Version", value = BuildConfig.VERSION_NAME)
    }
    TkSection("Credits", footer = "Tsurukame's Apache 2.0 licence comes with this app. Tsurukame is not affiliated with this app.") {
        TkRow("Design and artwork", subtitle = "Based on Tsurukame by David Sansome and contributors")
    }
    var confirmLogOut by remember { mutableStateOf(false) }
    TkSection {
        TkRow("Log out", titleColor = TkFixed.red, onClick = { confirmLogOut = true })
    }
    if (confirmLogOut) {
        TkActionSheet(
            title = "Log out?",
            message = "This removes your API token from this phone.",
            action = "Log out",
            onAction = {
                confirmLogOut = false
                onSignOut()
            },
            onDismiss = { confirmLogOut = false },
        )
    }
}

@Composable
private fun AppearancePage(
    s: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    openChoice: (String, String, List<Pair<String, String>>, (String) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    TkSection("Appearance") {
        ChoiceRow("Dark/light mode", s.interfaceStyle, listOf("SYSTEM", "LIGHT", "DARK"), openChoice) { name -> onChange { it.copy(interfaceStyle = name) } }
    }
    TkSection("Notifications", footer = "Shows how many reviews and lessons are waiting. Needs the notification permission.") {
        TkSwitchRow("Notify about new reviews", checked = s.enableNotifications) { onChange { it.copy(enableNotifications = !it.enableNotifications) } }
        ChoiceRow("Update frequency", s.notificationFrequency, notificationFrequencies, openChoice) { name -> onChange { it.copy(notificationFrequency = name) } }
        ChoiceRow("Priority", s.notificationPriority, notificationPriorities, openChoice) { name -> onChange { it.copy(notificationPriority = name) } }
        ChoiceRow("Category", s.notificationCategory, notificationCategories, openChoice) { name -> onChange { it.copy(notificationCategory = name) } }
    }
    TkSection {
        TkRow("Add widget to home screen", titleColor = TkFixed.tint, onClick = { WidgetUpdater.requestPin(context) })
    }
}

@Composable
private fun SubjectsPage(s: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit) {
    TkSection {
        TkSwitchRow("Blur context sentences", checked = s.blurContextSentences) { onChange { it.copy(blurContextSentences = !it.blurContextSentences) } }
    }
}

@Composable
private fun LessonsPage(
    s: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    openChoice: (String, String, List<Pair<String, String>>, (String) -> Unit) -> Unit,
) {
    TkSection {
        StepperRow("Lessons per session", s.maxLessonSessionSize, 1, AppSettings.MIN_SESSION_SIZE, AppSettings.MAX_SESSION_SIZE) { value ->
            onChange { it.copy(maxLessonSessionSize = value) }
        }
        ChoiceRow("Order", s.lessonOrder, lessonOrders, openChoice) { name -> onChange { it.copy(lessonOrder = name) } }
    }
}

@Composable
private fun ReviewsPage(
    s: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    openChoice: (String, String, List<Pair<String, String>>, (String) -> Unit) -> Unit,
) {
    TkSection("Reviews", footer = "With results sent at the end, you can leave a session and carry on later, online or offline.") {
        TkSwitchRow("Send results when the session ends", checked = s.delayResultUpload) { onChange { it.copy(delayResultUpload = !it.delayResultUpload) } }
        StepperRow("Reviews per session", s.maxReviewSessionSize, 10, AppSettings.MIN_SESSION_SIZE, AppSettings.MAX_SESSION_SIZE) { value ->
            onChange { it.copy(maxReviewSessionSize = value) }
        }
    }
    TkSection("Order") {
        val orderName = tsurukameReviewOrderName(s)
        TkRow(
            "Order",
            value = orderName ?: "Custom",
            chevron = true,
            onClick = {
                openChoice("Order", orderName.orEmpty(), tsurukameReviewOrders.map { it.first to it.first }) { name ->
                    tsurukameReviewOrders.first { it.first == name }.second.let { apply -> onChange(apply) }
                }
            },
        )
        TkSwitchRow("Back-to-back", "Group meaning and reading together", s.reviewBackToBack) { onChange { it.copy(reviewBackToBack = !it.reviewBackToBack) } }
        // One choice instead of the original's two switches; both falsy means random.
        val questionOrder = when {
            s.reviewMeaningFirst -> "MEANING_FIRST"
            s.reviewReadingFirst -> "READING_FIRST"
            else -> "RANDOM"
        }
        ChoiceRow("Question order", questionOrder, listOf("RANDOM", "MEANING_FIRST", "READING_FIRST"), openChoice) { name ->
            onChange { it.copy(reviewMeaningFirst = name == "MEANING_FIRST", reviewReadingFirst = name == "READING_FIRST") }
        }
    }
    TkSection("Advanced order", footer = "These combine freely. Changing them can make the order above show as Custom.") {
        ChoiceRow("Sort by", s.reviewOrder, reviewOrders, openChoice) { name -> onChange { it.copy(reviewOrder = name) } }
        TkSwitchRow("Reverse order", checked = s.reviewOrderReversed) { onChange { it.copy(reviewOrderReversed = !it.reviewOrderReversed) } }
        TkSwitchRow("Overdue items first", checked = s.reviewOrderOverdueFirst) { onChange { it.copy(reviewOrderOverdueFirst = !it.reviewOrderOverdueFirst) } }
        ChoiceRow("Priority", s.reviewOrderPriority, priorities, openChoice) { name -> onChange { it.copy(reviewOrderPriority = name) } }
    }
    TkSection("Answers & marking") {
        TkSwitchRow("Show answer immediately", "After a wrong answer. Off: tap the eye to reveal it.", s.showAnswerOnWrong) {
            onChange { it.copy(showAnswerOnWrong = !it.showAnswerOnWrong) }
        }
        TkSwitchRow("Reveal full answer", "Instead of hiding it behind a \"Show all information\" button", s.revealFullAnswer) {
            onChange { it.copy(revealFullAnswer = !it.revealFullAnswer) }
        }
        TkSwitchRow("Keep the keyboard open between answers", checked = s.keepKeyboardOpen) { onChange { it.copy(keepKeyboardOpen = !it.keepKeyboardOpen) } }
        TkRow(
            "Answer that is close enough",
            value = closeEnoughLabel(s.closeEnoughAction),
            chevron = true,
            onClick = {
                openChoice("Close enough", s.closeEnoughAction.name, CloseEnoughAction.entries.map { it.name to closeEnoughLabel(it) }) { name ->
                    onChange { it.copy(closeEnoughAction = CloseEnoughAction.valueOf(name)) }
                }
            },
        )
        TkSwitchRow("Require on\u2019yomi answers in katakana", checked = s.requireOnYomiInKatakana) {
            onChange { it.copy(requireOnYomiInKatakana = !it.requireOnYomiInKatakana) }
        }
        TkSwitchRow("Show SRS level indicator", checked = s.showSrsIndicator) { onChange { it.copy(showSrsIndicator = !it.showSrsIndicator) } }
    }
}

/** A row that opens a checkmark list of [options]; the stored value is shown in plain words. */
@Composable
private fun ChoiceRow(
    title: String,
    current: String,
    options: List<String>,
    openChoice: (String, String, List<Pair<String, String>>, (String) -> Unit) -> Unit,
    onPick: (String) -> Unit,
) {
    TkRow(title, value = pretty(current), chevron = true, onClick = { openChoice(title, current, options.map { it to pretty(it) }, onPick) })
}

@Composable
private fun ChoiceList(page: ChoicePage, onClose: () -> Unit) {
    TkSection {
        for ((name, label) in page.options) {
            TkRow(
                label,
                onClick = {
                    page.onPick(name)
                    onClose()
                },
                trailing = { if (name == page.current) TkText("\u2713", color = TkFixed.tint, modifier = Modifier.padding(start = 8.dp)) },
            )
        }
    }
}

@Composable
private fun StepperRow(title: String, value: Int, step: Int, min: Int, max: Int, onValue: (Int) -> Unit) {
    val tk = LocalTk.current
    TkRow(
        title,
        value = "$value",
        trailing = {
            Spacer(Modifier.width(10.dp))
            Row(Modifier.clip(RoundedCornerShape(8.dp)).background(if (tk.isDark) Color(0xFF2C2C2E) else Color(0xFFE3E3E8)), verticalAlignment = Alignment.CenterVertically) {
                StepperButton("\u2212", value > min) { onValue((value - step).coerceAtLeast(min)) }
                Box(Modifier.size(width = 1.dp, height = 18.dp).background(tk.separator))
                StepperButton("+", value < max) { onValue((value + step).coerceAtMost(max)) }
            }
        },
    )
}

@Composable
private fun StepperButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(width = 46.dp, height = 32.dp).clickable(enabled = enabled, onClick = onClick), Alignment.Center) {
        TkText(label, size = 20.sp, color = LocalTk.current.label.copy(alpha = if (enabled) 1f else 0.3f))
    }
}

private fun closeEnoughLabel(action: CloseEnoughAction) = when (action) {
    CloseEnoughAction.SILENTLY_ACCEPT -> "Accept silently"
    CloseEnoughAction.ACCEPT_WITH_TOAST -> "Accept and show the match"
    CloseEnoughAction.ACCEPT_WITH_TOAST_NO_LM -> "Accept and show the match (no lenience on short words)"
    CloseEnoughAction.SHAKE_AND_RETRY -> "Let me try again"
    CloseEnoughAction.REJECT -> "Count as wrong"
}

private fun audioSummary(state: AudioDownload) = when {
    state.running && state.total > 0 -> "Downloading level ${state.level}: ${state.done} of ${state.total} clips${if (state.failed > 0) ", ${state.failed} failed" else ""}"
    state.running -> "Preparing level ${state.level}\u2026"
    state.storedCount == 0 -> "No clips on this phone yet."
    else -> "${state.storedCount} clips on this phone (${formatSize(state.storedBytes)})."
}

internal fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.0f kB".format(bytes / 1_000.0)
    else -> "$bytes B"
}

/** Largest backup file accepted; real backups are a few kilobytes. */
private const val MAX_BACKUP_BYTES = 1_000_000

/** Saves and restores the settings as a JSON file chosen with Android's file picker. The token is never included. */
@Composable
private fun BackupSection(message: String?, onExport: suspend () -> String, onImport: (String) -> Unit, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching {
                val text = onExport()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                }
            }.isSuccess
            onMessage(if (ok) "Settings saved. The file does not contain your API token." else "Could not write the file.")
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.use { it.readNBytes(MAX_BACKUP_BYTES + 1).toString(Charsets.UTF_8) }
                }
            }.getOrNull()
            when {
                text == null -> onMessage("Could not read the file.")
                text.length > MAX_BACKUP_BYTES -> onMessage("That file is too large to be a settings backup.")
                else -> onImport(text)
            }
        }
    }

    TkSection(
        "Backup",
        footer = message ?: "Save your settings to a file, or restore them from one. Your API token is never saved or restored.",
    ) {
        TkRow("Back up settings", titleColor = TkFixed.tint, onClick = { save.launch("urashima_settings_${LocalDate.now()}.json") })
        TkRow("Restore settings", titleColor = TkFixed.tint, onClick = { open.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) })
    }
}
