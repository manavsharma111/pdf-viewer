package com.antigravity.pdfviewer.presentation.reader

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.antigravity.pdfviewer.presentation.reader.components.AnnotationToolbar
import com.antigravity.pdfviewer.presentation.reader.components.BookmarksSheet
import com.antigravity.pdfviewer.presentation.reader.components.MetadataDialog
import com.antigravity.pdfviewer.presentation.reader.components.PasswordPromptDialog
import com.antigravity.pdfviewer.presentation.reader.components.PdfViewer
import com.antigravity.pdfviewer.presentation.reader.components.ReaderBottomBar
import com.antigravity.pdfviewer.presentation.reader.components.ReaderTopBar
import com.antigravity.pdfviewer.presentation.reader.components.SearchBar
import com.antigravity.pdfviewer.presentation.reader.components.ThumbnailsSheet
import kotlinx.coroutines.launch

@Composable
fun PdfReaderScreen(
    uri: Uri,
    initialPage: Int,
    viewModel: PdfReaderViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPageManagement: (Uri) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    var showAutoSaveWarningDialog by remember { mutableStateOf(false) }

    // Screen sleep lock effect
    DisposableEffect(uiState.keepScreenAwake) {
        val activity = context as? Activity
        if (uiState.keepScreenAwake) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Full Screen Mode effect (hide status & navigation bars)
    DisposableEffect(uiState.fullScreenMode, uiState.isControlsVisible) {
        val activity = context as? Activity
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (uiState.fullScreenMode && !uiState.isControlsVisible) {
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            window?.let {
                WindowCompat.getInsetsController(it, it.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Load document on launch
    LaunchedEffect(uri) {
        viewModel.loadDocument(uri, initialPage)
        focusRequester.requestFocus()
    }

    // SAF Save As launcher
    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri ->
        if (destUri != null) {
            context.contentResolver.openOutputStream(destUri)?.use { outStream ->
                viewModel.saveToStream(
                    outputStream = outStream,
                    onSuccess = {
                        Toast.makeText(context, "PDF saved successfully", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        scope.launch { snackbarHostState.showSnackbar(error) }
                    }
                )
            }
        }
    }

    val onTriggerSave: () -> Unit = {
        if (uiState.autoSaveWarning) {
            showAutoSaveWarningDialog = true
        } else {
            saveAsLauncher.launch("Edited_${uiState.documentInfo?.fileName ?: "document.pdf"}")
        }
    }

    if (showAutoSaveWarningDialog) {
        AlertDialog(
            onDismissRequest = { showAutoSaveWarningDialog = false },
            title = { Text("Auto Save Warning") },
            text = { Text("A new copy of the file will be created to preserve the original document without modifying it.") },
            confirmButton = {
                Button(
                    onClick = {
                        showAutoSaveWarningDialog = false
                        saveAsLauncher.launch("Copy_${uiState.documentInfo?.fileName ?: "document.pdf"}")
                    }
                ) {
                    Text("Make Copy & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAutoSaveWarningDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when {
                        // Desktop/Chromebook shortcuts
                        event.key == Key.DirectionRight || event.key == Key.PageDown -> {
                            viewModel.nextPage()
                            true
                        }
                        event.key == Key.DirectionLeft || event.key == Key.PageUp -> {
                            viewModel.previousPage()
                            true
                        }
                        event.key == Key.MoveHome -> {
                            viewModel.firstPage()
                            true
                        }
                        event.key == Key.MoveEnd -> {
                            viewModel.lastPage()
                            true
                        }
                        event.isCtrlPressed && event.key == Key.F -> {
                            viewModel.toggleSearch()
                            true
                        }
                        event.isCtrlPressed && event.key == Key.S -> {
                            onTriggerSave()
                            true
                        }
                        event.isCtrlPressed && event.key == Key.Z -> {
                            viewModel.undo()
                            true
                        }
                        event.isCtrlPressed && event.key == Key.Y -> {
                            viewModel.redo()
                            true
                        }
                        event.key == Key.Plus || event.key == Key.Equals -> {
                            viewModel.zoomIn()
                            true
                        }
                        event.key == Key.Minus -> {
                            viewModel.zoomOut()
                            true
                        }
                        event.key == Key.Escape -> {
                            if (uiState.isSearchVisible) viewModel.toggleSearch()
                            if (uiState.activeTool != null) viewModel.setActiveTool(null)
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.totalPages > 0) {
                // PDF Viewer Main Canvas
                PdfViewer(
                    totalPages = uiState.totalPages,
                    currentPage = uiState.currentPage,
                    readingMode = uiState.readingMode,
                    zoomState = uiState.zoomState,
                    nightReadingMode = uiState.nightReadingMode,
                    annotations = uiState.annotations,
                    activeTool = uiState.activeTool,
                    activeColor = uiState.activeColor,
                    activeStrokeWidth = uiState.activeStrokeWidth,
                    activeAlpha = uiState.activeAlpha,
                    onPageChanged = { viewModel.onPageChanged(it) },
                    onZoomChanged = { scale, x, y -> viewModel.setZoom(scale, x, y) },
                    onRenderPage = { viewModel.renderPage(it) },
                    onGetCachedBitmap = { viewModel.getCachedPageBitmap(it) },
                    onStrokeAdded = { page, stroke -> viewModel.addStroke(page, stroke) },
                    onTextAdded = { page, text -> viewModel.addTextAnnotation(page, text) },
                    onHighlightAdded = { page, hl -> viewModel.addHighlight(page, hl) },
                    onTap = { viewModel.toggleControls() }
                )
            } else if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Text(
                        text = uiState.errorMessage ?: "Failed to open document",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Top Bar
            AnimatedVisibility(
                visible = uiState.isControlsVisible && uiState.activeTool == null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                ReaderTopBar(
                    title = uiState.documentInfo?.fileName ?: "PDF Reader",
                    readingMode = uiState.readingMode,
                    isBookmarked = uiState.isBookmarked,
                    isAnnotationMode = uiState.activeTool != null,
                    useSingleLineToolbar = uiState.useSingleLineToolbar,
                    onBackClick = onNavigateBack,
                    onSearchClick = { viewModel.toggleSearch() },
                    onBookmarkClick = { viewModel.toggleCurrentPageBookmark() },
                    onAnnotationClick = { viewModel.setActiveTool(com.antigravity.pdfviewer.domain.model.AnnotationType.PEN) },
                    onReadingModeSelected = { viewModel.setReadingMode(it) },
                    onRotateClick = { viewModel.rotateCurrentPage(true) },
                    onPageManagementClick = { uiState.documentInfo?.uri?.let { onNavigateToPageManagement(it) } },
                    onMetadataClick = { viewModel.setMetadataVisible(true) },
                    onShareClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
                    },
                    onSaveClick = onTriggerSave
                )
            }

            // Search Bar Overlay
            AnimatedVisibility(
                visible = uiState.isSearchVisible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                SearchBar(
                    searchState = uiState.searchState,
                    onQueryChanged = { q, cs, ww -> viewModel.search(q, cs, ww) },
                    onNextMatch = { viewModel.nextSearchResult() },
                    onPreviousMatch = { viewModel.previousSearchResult() },
                    onClose = { viewModel.toggleSearch() }
                )
            }

            // Annotation Toolbar (replaces bottom bar when editing)
            AnimatedVisibility(
                visible = uiState.activeTool != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                AnnotationToolbar(
                    activeTool = uiState.activeTool,
                    activeColor = uiState.activeColor,
                    activeStrokeWidth = uiState.activeStrokeWidth,
                    canUndo = uiState.canUndo,
                    canRedo = uiState.canRedo,
                    onToolSelected = { viewModel.setActiveTool(it) },
                    onColorSelected = { viewModel.setActiveColor(it) },
                    onStrokeWidthSelected = { viewModel.setActiveStrokeWidth(it) },
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    onAddTextNote = { textNote -> viewModel.addTextAnnotation(uiState.currentPage, textNote) },
                    onClose = { viewModel.setActiveTool(null) }
                )
            }

            // Normal Bottom Bar
            AnimatedVisibility(
                visible = uiState.isControlsVisible && uiState.activeTool == null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ReaderBottomBar(
                    currentPage = uiState.currentPage,
                    totalPages = uiState.totalPages,
                    zoomScale = uiState.zoomState.scale,
                    onPageSelected = { viewModel.onPageChanged(it) },
                    onPreviousPage = { viewModel.previousPage() },
                    onNextPage = { viewModel.nextPage() },
                    onFirstPage = { viewModel.firstPage() },
                    onLastPage = { viewModel.lastPage() },
                    onZoomIn = { viewModel.zoomIn() },
                    onZoomOut = { viewModel.zoomOut() },
                    onResetZoom = { viewModel.resetZoom() },
                    onThumbnailsClick = { viewModel.setThumbnailsVisible(true) },
                    onBookmarksClick = { viewModel.setBookmarksVisible(true) }
                )
            }

            // Long-running progress indicator
            if (uiState.isProcessing) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                        uiState.processingMessage?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 48.dp)
                            )
                        }
                    }
                }
            }

            // Thumbnails Sheet
            if (uiState.isThumbnailsVisible) {
                ThumbnailsSheet(
                    totalPages = uiState.totalPages,
                    currentPage = uiState.currentPage,
                    onPageSelected = { viewModel.onPageChanged(it) },
                    onRenderThumbnail = { viewModel.renderPage(it, 180, 240) },
                    onDismiss = { viewModel.setThumbnailsVisible(false) }
                )
            }

            // Bookmarks Sheet
            if (uiState.isBookmarksVisible) {
                BookmarksSheet(
                    bookmarks = uiState.bookmarks,
                    onBookmarkSelected = { viewModel.onPageChanged(it) },
                    onDeleteBookmark = { viewModel.removeBookmark(it) },
                    onDismiss = { viewModel.setBookmarksVisible(false) }
                )
            }

            // Document Info / Metadata Dialog
            if (uiState.isMetadataVisible && uiState.documentInfo != null) {
                MetadataDialog(
                    documentInfo = uiState.documentInfo!!,
                    onDismiss = { viewModel.setMetadataVisible(false) }
                )
            }

            // Password Unlock Prompt
            if (uiState.isPasswordRequired) {
                PasswordPromptDialog(
                    onUnlock = { password -> viewModel.loadDocument(uri, initialPage, password) },
                    onCancel = onNavigateBack
                )
            }
        }
    }
}
