package com.example.durtlesreborn.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.durtlesreborn.session.StageMove

/**
 * The animated host for the stage bubble: it fades and scales in with a small upward slide when
 * [move] appears, and reverses when it clears, so it reads like an Android toast.
 */
@Composable
fun StageBubbleToast(move: StageMove?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = move != null,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(140)) +
            scaleIn(initialScale = 0.82f, animationSpec = tween(180)) +
            slideInVertically(animationSpec = tween(180)) { it / 3 },
        exit = fadeOut(animationSpec = tween(180)) +
            scaleOut(targetScale = 0.9f, animationSpec = tween(180)) +
            slideOutVertically(animationSpec = tween(180)) { -it / 3 },
    ) {
        move?.let { StageBubble(it) }
    }
}

/**
 * The full name of an SRS stage index, as WaniKani shows it: Apprentice 1–4, Guru 1–2, Master,
 * Enlightened, Burned. Indices follow [com.example.durtlesreborn.session.SrsSystem].
 */
internal fun stageName(stage: Int): String = when {
    stage <= 4 -> "Apprentice ${stage.coerceAtLeast(1)}"
    stage <= 6 -> "Guru ${stage - 4}"
    stage == 7 -> "Master"
    stage == 8 -> "Enlightened"
    else -> "Burned"
}

/**
 * A small toast-like bubble above the item showing where its review just moved it, e.g.
 * "↑ Guru 1 → Guru 2". It uses the theme's surface and outline, with the arrow in the theme's
 * primary for a rise and its error colour for a fall.
 */
@Composable
fun StageBubble(move: StageMove, modifier: Modifier = Modifier) {
    val up = move.to > move.from
    val accent = if (up) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                if (up) "↑" else "↓",
                color = accent,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${stageName(move.from)} → ${stageName(move.to)}",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}
