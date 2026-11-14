package com.antigravity.pdfviewer.domain.model

import java.util.UUID

enum class AnnotationType {
    PEN,
    PENCIL,
    MARKER,
    ERASER,
    HIGHLIGHT,
    UNDERLINE,
    STRIKETHROUGH,
    TEXT
}

data class PointF(
    val x: Float,
    val y: Float
)

data class DrawingStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<PointF>,
    val color: Int,
    val strokeWidth: Float,
    val alpha: Float = 1.0f,
    val isEraser: Boolean = false
)

data class TextAnnotation(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val position: PointF,
    val color: Int,
    val fontSize: Float = 16f
)

data class HighlightAnnotation(
    val id: String = UUID.randomUUID().toString(),
    val start: PointF,
    val end: PointF,
    val color: Int,
    val strokeWidth: Float = 14f,
    val type: AnnotationType = AnnotationType.HIGHLIGHT
)

data class PageAnnotations(
    val pageIndex: Int,
    val strokes: List<DrawingStroke> = emptyList(),
    val textNotes: List<TextAnnotation> = emptyList(),
    val highlights: List<HighlightAnnotation> = emptyList()
)

data class Bookmark(
    val id: Long = 0,
    val documentUri: String,
    val pageNumber: Int,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)
