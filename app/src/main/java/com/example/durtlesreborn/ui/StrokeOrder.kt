package com.example.durtlesreborn.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.durtlesreborn.data.LessonInfo
import com.example.durtlesreborn.session.SubjectKind
import com.example.durtlesreborn.strokes.StrokeRepository

/** Where the stroke data comes from; null in previews and tests, which then show no chart. */
val LocalStrokeRepository = staticCompositionLocalOf<StrokeRepository?> { null }

private const val BOX = 109f

/** Whether a subject gets a stroke order chart: radicals and single kanji, never vocabulary. */
fun hasStrokeOrder(info: LessonInfo) = info.kind == SubjectKind.RADICAL || info.kind == SubjectKind.KANJI

/** The stroke order chart for a radical or kanji, or nothing if there is no data for it (image radicals). */
@Composable
fun StrokeOrderSection(info: LessonInfo) {
    if (!hasStrokeOrder(info)) return
    val repository = LocalStrokeRepository.current ?: return
    val strokes by produceState<List<String>?>(null, info.title) { value = repository.strokesFor(info.title) }
    val list = strokes ?: return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Stroke order · ${list.size} ${if (list.size == 1) "stroke" else "strokes"}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        StrokeOrderChart(list, subjectColor(info.kind))
        Text("Stroke data: KanjiVG, CC BY-SA 3.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** One small picture per stroke: the strokes so far in grey, the newest in colour with a dot where it starts. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StrokeOrderChart(strokes: List<String>, highlight: Color, cell: androidx.compose.ui.unit.Dp = 56.dp) {
    val paths = remember(strokes) { strokes.map { PathParser().parsePathString(it).toPath() } }
    val starts = remember(paths) { paths.map { startOf(it) } }
    val past = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val guide = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val frame = MaterialTheme.colorScheme.outlineVariant
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (index in paths.indices) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(
                    Modifier.size(cell).border(1.dp, frame, RoundedCornerShape(6.dp))
                        .semantics { contentDescription = "Stroke ${index + 1} of ${paths.size}" },
                ) {
                    val scale = size.minDimension / BOX
                    withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
                        drawLine(guide, Offset(BOX / 2, 4f), Offset(BOX / 2, BOX - 4f), strokeWidth = 1f)
                        drawLine(guide, Offset(4f, BOX / 2), Offset(BOX - 4f, BOX / 2), strokeWidth = 1f)
                        val line = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        for (earlier in 0 until index) drawPath(paths[earlier], past, style = line)
                        drawPath(paths[index], highlight, style = line)
                        starts[index]?.let { drawCircle(highlight, radius = 5f, center = it) }
                    }
                }
                Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun startOf(path: Path): Offset? {
    val measure = PathMeasure().apply { setPath(path, false) }
    return if (measure.length > 0f) measure.getPosition(0f) else null
}
