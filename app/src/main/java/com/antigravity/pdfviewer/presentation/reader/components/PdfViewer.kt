package com.antigravity.pdfviewer.presentation.reader.components

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.antigravity.pdfviewer.domain.model.AnnotationType
import com.antigravity.pdfviewer.domain.model.DrawingStroke
import com.antigravity.pdfviewer.domain.model.HighlightAnnotation
import com.antigravity.pdfviewer.domain.model.NightReadingMode
import com.antigravity.pdfviewer.domain.model.PageAnnotations
import com.antigravity.pdfviewer.domain.model.ReadingMode
import com.antigravity.pdfviewer.domain.model.TextAnnotation
import com.antigravity.pdfviewer.domain.model.ZoomState
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PdfViewer(
    totalPages: Int,
    currentPage: Int,
    readingMode: ReadingMode,
    zoomState: ZoomState,
    nightReadingMode: NightReadingMode,
    annotations: Map<Int, PageAnnotations>,
    activeTool: AnnotationType?,
    activeColor: Int,
    activeStrokeWidth: Float,
    activeAlpha: Float,
    onPageChanged: (Int) -> Unit,
    onZoomChanged: (Float, Float, Float) -> Unit,
    onRenderPage: suspend (Int) -> Bitmap,
    onGetCachedBitmap: (Int) -> Bitmap?,
    onStrokeAdded: (Int, DrawingStroke) -> Unit,
    onTextAdded: (Int, TextAnnotation) -> Unit,
    onHighlightAdded: (Int, HighlightAnnotation) -> Unit,
    onTap: () -> Unit,
    onSelectText: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (totalPages <= 0) return

    var scale by remember(zoomState.scale) { mutableFloatStateOf(zoomState.scale) }
    var offsetX by remember(zoomState.offsetX) { mutableFloatStateOf(zoomState.offsetX) }
    var offsetY by remember(zoomState.offsetY) { mutableFloatStateOf(zoomState.offsetY) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(if (nightReadingMode == NightReadingMode.INVERTED) Color(0xFF1A1A1A) else Color(0xFFE2E8F0))
            .clipToBounds()
            .pointerInput(activeTool) {
                // If an annotation tool is active, do not intercept pinch/tap at root level
                if (activeTool == null) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = { tapOffset ->
                            val newScale = if (scale > 1.2f) 1.0f else 2.5f
                            scale = newScale
                            if (newScale == 1.0f) {
                                offsetX = 0f
                                offsetY = 0f
                            }
                            onZoomChanged(scale, offsetX, offsetY)
                        },
                        onLongPress = {
                            onSelectText()
                        }
                    )
                }
            }
            .pointerInput(activeTool) {
                if (activeTool == null) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1.0f, 6.0f)
                        scale = newScale

                        if (newScale > 1.0f) {
                            val maxPanX = (size.width * (newScale - 1f)) / 2f
                            val maxPanY = (size.height * (newScale - 1f)) / 2f
                            offsetX = (offsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                            offsetY = (offsetY + pan.y).coerceIn(-maxPanY, maxPanY)
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                        onZoomChanged(scale, offsetX, offsetY)
                    }
                }
            }
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
        ) {
            when (readingMode) {
                ReadingMode.SINGLE_PAGE -> {
                    val pagerState = rememberPagerState(
                        initialPage = currentPage.coerceIn(0, totalPages - 1),
                        pageCount = { totalPages }
                    )

                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.currentPage }
                            .distinctUntilChanged()
                            .collect { page -> onPageChanged(page) }
                    }

                    LaunchedEffect(currentPage) {
                        if (pagerState.currentPage != currentPage) {
                            pagerState.scrollToPage(currentPage)
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) { pageIndex ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            PdfPageItem(
                                pageIndex = pageIndex,
                                cachedBitmap = onGetCachedBitmap(pageIndex),
                                onRenderPage = onRenderPage,
                                nightReadingMode = nightReadingMode,
                                annotations = annotations[pageIndex],
                                activeTool = activeTool,
                                activeColor = activeColor,
                                activeStrokeWidth = activeStrokeWidth,
                                activeAlpha = activeAlpha,
                                onStrokeAdded = { onStrokeAdded(pageIndex, it) },
                                onTextAdded = { onTextAdded(pageIndex, it) },
                                onHighlightAdded = { onHighlightAdded(pageIndex, it) }
                            )
                        }
                    }
                }

                ReadingMode.CONTINUOUS_VERTICAL -> {
                    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPage)

                    LaunchedEffect(listState) {
                        snapshotFlow { listState.firstVisibleItemIndex }
                            .distinctUntilChanged()
                            .collect { page -> onPageChanged(page) }
                    }

                    LaunchedEffect(currentPage) {
                        if (listState.firstVisibleItemIndex != currentPage) {
                            listState.scrollToItem(currentPage)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(count = totalPages, key = { it }) { pageIndex ->
                            PdfPageItem(
                                pageIndex = pageIndex,
                                cachedBitmap = onGetCachedBitmap(pageIndex),
                                onRenderPage = onRenderPage,
                                nightReadingMode = nightReadingMode,
                                annotations = annotations[pageIndex],
                                activeTool = activeTool,
                                activeColor = activeColor,
                                activeStrokeWidth = activeStrokeWidth,
                                activeAlpha = activeAlpha,
                                onStrokeAdded = { onStrokeAdded(pageIndex, it) },
                                onTextAdded = { onTextAdded(pageIndex, it) },
                                onHighlightAdded = { onHighlightAdded(pageIndex, it) }
                            )
                        }
                    }
                }

                ReadingMode.CONTINUOUS_HORIZONTAL -> {
                    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPage)

                    LaunchedEffect(listState) {
                        snapshotFlow { listState.firstVisibleItemIndex }
                            .distinctUntilChanged()
                            .collect { page -> onPageChanged(page) }
                    }

                    LaunchedEffect(currentPage) {
                        if (listState.firstVisibleItemIndex != currentPage) {
                            listState.scrollToItem(currentPage)
                        }
                    }

                    LazyRow(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(count = totalPages, key = { it }) { pageIndex ->
                            Box(
                                modifier = Modifier
                                    .width(containerWidth * 0.9f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                PdfPageItem(
                                    pageIndex = pageIndex,
                                    cachedBitmap = onGetCachedBitmap(pageIndex),
                                    onRenderPage = onRenderPage,
                                    nightReadingMode = nightReadingMode,
                                    annotations = annotations[pageIndex],
                                    activeTool = activeTool,
                                    activeColor = activeColor,
                                    activeStrokeWidth = activeStrokeWidth,
                                    activeAlpha = activeAlpha,
                                    onStrokeAdded = { onStrokeAdded(pageIndex, it) },
                                    onTextAdded = { onTextAdded(pageIndex, it) },
                                    onHighlightAdded = { onHighlightAdded(pageIndex, it) }
                                )
                            }
                        }
                    }
                }

                ReadingMode.DOUBLE_PAGE -> {
                    // Double page pairs: (0, 1), (2, 3), (4, 5)...
                    val rowCount = (totalPages + 1) / 2
                    val initialRow = currentPage / 2
                    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialRow)

                    LaunchedEffect(listState) {
                        snapshotFlow { listState.firstVisibleItemIndex * 2 }
                            .distinctUntilChanged()
                            .collect { page -> onPageChanged(page) }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(count = rowCount, key = { it }) { rowIndex ->
                            val page1 = rowIndex * 2
                            val page2 = page1 + 1

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    PdfPageItem(
                                        pageIndex = page1,
                                        cachedBitmap = onGetCachedBitmap(page1),
                                        onRenderPage = onRenderPage,
                                        nightReadingMode = nightReadingMode,
                                        annotations = annotations[page1],
                                        activeTool = activeTool,
                                        activeColor = activeColor,
                                        activeStrokeWidth = activeStrokeWidth,
                                        activeAlpha = activeAlpha,
                                        onStrokeAdded = { onStrokeAdded(page1, it) },
                                        onTextAdded = { onTextAdded(page1, it) },
                                        onHighlightAdded = { onHighlightAdded(page1, it) }
                                    )
                                }

                                if (page2 < totalPages) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        PdfPageItem(
                                            pageIndex = page2,
                                            cachedBitmap = onGetCachedBitmap(page2),
                                            onRenderPage = onRenderPage,
                                            nightReadingMode = nightReadingMode,
                                            annotations = annotations[page2],
                                            activeTool = activeTool,
                                            activeColor = activeColor,
                                            activeStrokeWidth = activeStrokeWidth,
                                            activeAlpha = activeAlpha,
                                            onStrokeAdded = { onStrokeAdded(page2, it) },
                                            onTextAdded = { onTextAdded(page2, it) },
                                            onHighlightAdded = { onHighlightAdded(page2, it) }
                                        )
                                    }
                                } else {
                                    // Empty placeholder to balance last odd page
                                    Box(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
