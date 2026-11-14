package com.antigravity.pdfviewer.presentation.reader.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.pdfviewer.domain.model.AnnotationType
import com.antigravity.pdfviewer.domain.model.DrawingStroke
import com.antigravity.pdfviewer.domain.model.HighlightAnnotation
import com.antigravity.pdfviewer.domain.model.PageAnnotations
import com.antigravity.pdfviewer.domain.model.PointF
import com.antigravity.pdfviewer.domain.model.TextAnnotation

@Composable
fun AnnotationCanvas(
    pageIndex: Int,
    annotations: PageAnnotations?,
    activeTool: AnnotationType?,
    activeColor: Int,
    activeStrokeWidth: Float,
    activeAlpha: Float,
    onStrokeAdded: (DrawingStroke) -> Unit,
    onTextAdded: (TextAnnotation) -> Unit,
    onHighlightAdded: (HighlightAnnotation) -> Unit,
    modifier: Modifier = Modifier
) {
    var livePoints = remember { mutableStateListOf<PointF>() }
    var highlightStart by remember { mutableStateOf<PointF?>(null) }
    var highlightEnd by remember { mutableStateOf<PointF?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeTool, activeColor, activeStrokeWidth, activeAlpha) {
                    if (activeTool == null) return@pointerInput

                    detectDragGestures(
                        onDragStart = { offset ->
                            val normX = (offset.x / size.width).coerceIn(0f, 1f)
                            val normY = (offset.y / size.height).coerceIn(0f, 1f)

                            when (activeTool) {
                                AnnotationType.PEN,
                                AnnotationType.PENCIL,
                                AnnotationType.MARKER,
                                AnnotationType.ERASER -> {
                                    livePoints.clear()
                                    livePoints.add(PointF(normX, normY))
                                }
                                AnnotationType.HIGHLIGHT,
                                AnnotationType.UNDERLINE,
                                AnnotationType.STRIKETHROUGH -> {
                                    highlightStart = PointF(normX, normY)
                                    highlightEnd = PointF(normX, normY)
                                }
                                AnnotationType.TEXT -> {
                                    // Handled via tap or modal
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val normX = (change.position.x / size.width).coerceIn(0f, 1f)
                            val normY = (change.position.y / size.height).coerceIn(0f, 1f)

                            when (activeTool) {
                                AnnotationType.PEN,
                                AnnotationType.PENCIL,
                                AnnotationType.MARKER,
                                AnnotationType.ERASER -> {
                                    livePoints.add(PointF(normX, normY))
                                }
                                AnnotationType.HIGHLIGHT,
                                AnnotationType.UNDERLINE,
                                AnnotationType.STRIKETHROUGH -> {
                                    highlightEnd = PointF(normX, normY)
                                }
                                else -> Unit
                            }
                        },
                        onDragEnd = {
                            when (activeTool) {
                                AnnotationType.PEN,
                                AnnotationType.PENCIL,
                                AnnotationType.MARKER,
                                AnnotationType.ERASER -> {
                                    if (livePoints.size >= 2) {
                                        val stroke = DrawingStroke(
                                            points = livePoints.toList(),
                                            color = activeColor,
                                            strokeWidth = activeStrokeWidth,
                                            alpha = activeAlpha,
                                            isEraser = activeTool == AnnotationType.ERASER
                                        )
                                        onStrokeAdded(stroke)
                                    }
                                    livePoints.clear()
                                }
                                AnnotationType.HIGHLIGHT,
                                AnnotationType.UNDERLINE,
                                AnnotationType.STRIKETHROUGH -> {
                                    val start = highlightStart
                                    val end = highlightEnd
                                    if (start != null && end != null) {
                                        val hl = HighlightAnnotation(
                                            start = start,
                                            end = end,
                                            color = activeColor,
                                            strokeWidth = activeStrokeWidth,
                                            type = activeTool
                                        )
                                        onHighlightAdded(hl)
                                    }
                                    highlightStart = null
                                    highlightEnd = null
                                }
                                else -> Unit
                            }
                        },
                        onDragCancel = {
                            livePoints.clear()
                            highlightStart = null
                            highlightEnd = null
                        }
                    )
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw Saved Highlights
            annotations?.highlights?.forEach { hl ->
                val start = Offset(hl.start.x * canvasW, hl.start.y * canvasH)
                val end = Offset(hl.end.x * canvasW, hl.end.y * canvasH)
                val color = Color(hl.color).copy(
                    alpha = if (hl.type == AnnotationType.HIGHLIGHT) 0.35f else 1.0f
                )

                drawLine(
                    color = color,
                    start = start,
                    end = end,
                    strokeWidth = hl.strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 2. Draw Live Highlight during gesture
            val lStart = highlightStart
            val lEnd = highlightEnd
            if (lStart != null && lEnd != null) {
                val start = Offset(lStart.x * canvasW, lStart.y * canvasH)
                val end = Offset(lEnd.x * canvasW, lEnd.y * canvasH)
                val color = Color(activeColor).copy(
                    alpha = if (activeTool == AnnotationType.HIGHLIGHT) 0.35f else 1.0f
                )
                drawLine(
                    color = color,
                    start = start,
                    end = end,
                    strokeWidth = activeStrokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 3. Draw Saved Freehand Strokes
            annotations?.strokes?.forEach { stroke ->
                if (stroke.points.size >= 2 && !stroke.isEraser) {
                    val path = Path()
                    val p0 = stroke.points[0]
                    path.moveTo(p0.x * canvasW, p0.y * canvasH)
                    for (i in 1 until stroke.points.size) {
                        val p = stroke.points[i]
                        path.lineTo(p.x * canvasW, p.y * canvasH)
                    }
                    drawPath(
                        path = path,
                        color = Color(stroke.color).copy(alpha = stroke.alpha),
                        style = Stroke(
                            width = stroke.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // 4. Draw Live Stroke during gesture
            if (livePoints.size >= 2 && activeTool != AnnotationType.ERASER) {
                val livePath = Path()
                val p0 = livePoints[0]
                livePath.moveTo(p0.x * canvasW, p0.y * canvasH)
                for (i in 1 until livePoints.size) {
                    val p = livePoints[i]
                    livePath.lineTo(p.x * canvasW, p.y * canvasH)
                }
                drawPath(
                    path = livePath,
                    color = Color(activeColor).copy(alpha = activeAlpha),
                    style = Stroke(
                        width = activeStrokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // 5. Draw Saved Text Annotations
        annotations?.textNotes?.forEach { textNote ->
            Box(
                modifier = Modifier
                    .offset {
                        // Position text relative to page bounds
                        IntOffset(
                            x = (textNote.position.x * 1000).toInt(),
                            y = (textNote.position.y * 1000).toInt()
                        )
                    }
            ) {
                Surface(
                    color = Color.Yellow.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = textNote.text,
                        fontSize = textNote.fontSize.sp,
                        color = Color(textNote.color),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
