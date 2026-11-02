// Based on Tsurukame (ios/LessonPickerViewController.swift),
// Copyright David Sansome, Apache License 2.0: see assets/TSURUKAME-LICENSE.txt.
// Rewritten in Kotlin and changed for this app.
package com.example.durtlesreborn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Choose which lessons to learn now: tap items in any level, then Begin. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LessonPickerScreen(state: UiState.LessonPicker, onToggle: (Long) -> Unit, onBegin: () -> Unit, onBack: () -> Unit) {
    val tk = LocalTk.current
    val count = state.selected.size
    val levels = remember(state.choices) { state.choices.groupBy { it.level } }
    Column(Modifier.fillMaxSize().background(tk.grouped)) {
        TkNavBar("Lesson Picker", onBack = onBack, actions = {
            TkBarButton(if (count > 0) "Begin ($count)" else "Begin", enabled = count > 0, bold = true, onClick = onBegin)
        })
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding()) {
            item {
                TkText(
                    "Tap the items you want to learn, then tap Begin.",
                    size = 13.sp,
                    color = tk.grey33,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            for ((level, choices) in levels) {
                item(key = "header$level") { TkSectionHeader("Level $level") }
                item(key = "items$level") {
                    FlowRow(
                        Modifier.fillMaxWidth().background(tk.cell).padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (choice in choices) {
                            val selected = choice.id in state.selected
                            SubjectChipBox(
                                choice.title,
                                tk.gradient(choice.kind),
                                modifier = if (selected) Modifier.border(2.dp, Color.White, RoundedCornerShape(6.dp)) else Modifier,
                                dimmed = !selected,
                                onClick = { onToggle(choice.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
