package com.antigravity.pdfviewer.presentation.reader.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.antigravity.pdfviewer.domain.model.AnnotationType
import com.antigravity.pdfviewer.domain.model.DrawingStroke
import com.antigravity.pdfviewer.domain.model.HighlightAnnotation
import com.antigravity.pdfviewer.domain.model.NightReadingMode
import com.antigravity.pdfviewer.domain.model.PageAnnotations
import com.antigravity.pdfviewer.domain.model.TextAnnotation

@Composable
fun PdfPageItem(
    pageIndex: Int,
    cachedBitmap: Bitmap?,
    onRenderPage: suspend (Int) -> Bitmap,
    nightReadingMode: NightReadingMode,
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
    var bitmap by remember(pageIndex) { mutableStateOf(cachedBitmap) }
    var isLoading by remember(pageIndex) { mutableStateOf(cachedBitmap == null) }

    LaunchedEffect(pageIndex) {
        if (bitmap == null) {
            isLoading = true
            try {
                bitmap = onRenderPage(pageIndex)
            } catch (e: Exception) {
                // ignore
            } finally {
                isLoading = false
            }
        }
    }

    val colorFilter = remember(nightReadingMode) {
        when (nightReadingMode) {
            NightReadingMode.NONE -> null
            NightReadingMode.INVERTED -> {
                // Invert RGB: R' = 255 - R, G' = 255 - G, B' = 255 - B
                ColorFilter.colorMatrix(
                    ColorMatrix(
                        floatArrayOf(
                            -1f,  0f,  0f, 0f, 255f,
                             0f, -1f,  0f, 0f, 255f,
                             0f,  0f, -1f, 0f, 255f,
                             0f,  0f,  0f, 1f,   0f
                        )
                    )
                )
            }
            NightReadingMode.SEPIA -> {
                ColorFilter.colorMatrix(
                    ColorMatrix(
                        floatArrayOf(
                            0.393f, 0.769f, 0.189f, 0f, 0f,
                            0.349f, 0.686f, 0.168f, 0f, 0f,
                            0.272f, 0.534f, 0.131f, 0f, 0f,
                            0f,     0f,     0f,     1f, 0f
                        )
                    )
                )
            }
            NightReadingMode.NIGHT_GRAY -> {
                val matrix = ColorMatrix()
                matrix.setToSaturation(0f)
                ColorFilter.colorMatrix(matrix)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = MaterialTheme.shapes.small)
            .background(Color.White, shape = MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            val bmp = bitmap!!
            val aspectRatio = bmp.width.toFloat() / bmp.height.toFloat()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    contentScale = ContentScale.FillWidth,
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxSize()
                )

                // Vector annotations layer
                AnnotationCanvas(
                    pageIndex = pageIndex,
                    annotations = annotations,
                    activeTool = activeTool,
                    activeColor = activeColor,
                    activeStrokeWidth = activeStrokeWidth,
                    activeAlpha = activeAlpha,
                    onStrokeAdded = onStrokeAdded,
                    onTextAdded = onTextAdded,
                    onHighlightAdded = onHighlightAdded,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Placeholder while rendering
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.707f) // standard A4 ratio
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                } else {
                    Text(
                        text = "Page ${pageIndex + 1}",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
