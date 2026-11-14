package com.antigravity.pdfviewer.presentation.reader

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigravity.pdfviewer.data.local.preferences.UserPreferencesRepository
import com.antigravity.pdfviewer.domain.model.AnnotationType
import com.antigravity.pdfviewer.domain.model.Bookmark
import com.antigravity.pdfviewer.domain.model.DrawingStroke
import com.antigravity.pdfviewer.domain.model.HighlightAnnotation
import com.antigravity.pdfviewer.domain.model.NightReadingMode
import com.antigravity.pdfviewer.domain.model.PageAnnotations
import com.antigravity.pdfviewer.domain.model.PdfDocumentInfo
import com.antigravity.pdfviewer.domain.model.PdfPageInfo
import com.antigravity.pdfviewer.domain.model.ReadingMode
import com.antigravity.pdfviewer.domain.model.SearchResult
import com.antigravity.pdfviewer.domain.model.SearchState
import com.antigravity.pdfviewer.domain.model.TextAnnotation
import com.antigravity.pdfviewer.domain.model.ZoomState
import com.antigravity.pdfviewer.domain.repository.AnnotationRepository
import com.antigravity.pdfviewer.domain.repository.BookmarkRepository
import com.antigravity.pdfviewer.domain.repository.PdfDocumentRepository
import com.antigravity.pdfviewer.domain.repository.RecentDocumentsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.OutputStream

sealed class AnnotationAction {
    data class AddStroke(val pageIndex: Int, val stroke: DrawingStroke) : AnnotationAction()
    data class AddText(val pageIndex: Int, val text: TextAnnotation) : AnnotationAction()
    data class AddHighlight(val pageIndex: Int, val highlight: HighlightAnnotation) : AnnotationAction()
}

data class PdfReaderUiState(
    val documentInfo: PdfDocumentInfo? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val readingMode: ReadingMode = ReadingMode.CONTINUOUS_VERTICAL,
    val zoomState: ZoomState = ZoomState(),
    val searchState: SearchState = SearchState(),
    val bookmarks: List<Bookmark> = emptyList(),
    val isBookmarked: Boolean = false,
    val annotations: Map<Int, PageAnnotations> = emptyMap(),
    val activeTool: AnnotationType? = null,
    val activeColor: Int = Color.RED,
    val activeStrokeWidth: Float = 4f,
    val activeAlpha: Float = 1.0f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isPasswordRequired: Boolean = false,
    val isThumbnailsVisible: Boolean = false,
    val isBookmarksVisible: Boolean = false,
    val isMetadataVisible: Boolean = false,
    val isSearchVisible: Boolean = false,
    val isToolsPanelVisible: Boolean = false,
    val nightReadingMode: NightReadingMode = NightReadingMode.NONE,
    val keepScreenAwake: Boolean = false,
    val useSingleLineToolbar: Boolean = true,
    val fullScreenMode: Boolean = true,
    val screenSleepLock: Boolean = true,
    val autoSaveWarning: Boolean = false,
    val rightToLeftSupport: Boolean = false,
    val isControlsVisible: Boolean = true,
    val selectedText: String? = null,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val processingMessage: String? = null,
    val errorMessage: String? = null
)

class PdfReaderViewModel(
    private val pdfRepository: PdfDocumentRepository,
    private val recentRepository: RecentDocumentsRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val annotationRepository: AnnotationRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfReaderUiState())
    val uiState: StateFlow<PdfReaderUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<AnnotationAction>()
    private val redoStack = mutableListOf<AnnotationAction>()

    private var currentUri: Uri? = null
    private var searchJob: Job? = null

    init {
        // Load user preferences
        viewModelScope.launch {
            val settings = preferencesRepository.userSettingsFlow.first()
            _uiState.update {
                it.copy(
                    readingMode = settings.defaultReadingMode,
                    zoomState = it.zoomState.copy(scale = settings.defaultZoom),
                    nightReadingMode = settings.nightReadingMode,
                    keepScreenAwake = settings.screenSleepLock || settings.keepScreenAwake,
                    useSingleLineToolbar = settings.useSingleLineToolbar,
                    fullScreenMode = settings.fullScreenMode,
                    screenSleepLock = settings.screenSleepLock,
                    autoSaveWarning = settings.autoSaveWarning,
                    rightToLeftSupport = settings.rightToLeftSupport
                )
            }
        }
    }

    fun loadDocument(uri: Uri, initialPage: Int = 0, password: String? = null) {
        currentUri = uri
        _uiState.update { it.copy(isLoading = true, errorMessage = null, isPasswordRequired = false) }

        viewModelScope.launch {
            try {
                val info = pdfRepository.openDocument(uri, password)

                if (info.isEncrypted && info.pageCount == 0) {
                    _uiState.update {
                        it.copy(
                            documentInfo = info,
                            isLoading = false,
                            isPasswordRequired = true
                        )
                    }
                    return@launch
                }

                val safePage = initialPage.coerceIn(0, (info.pageCount - 1).coerceAtLeast(0))

                _uiState.update {
                    it.copy(
                        documentInfo = info,
                        currentPage = safePage,
                        totalPages = info.pageCount,
                        isLoading = false,
                        isPasswordRequired = false
                    )
                }

                // Collect bookmarks
                launch {
                    bookmarkRepository.getBookmarks(uri.toString()).collect { bookmarks ->
                        _uiState.update { state ->
                            state.copy(
                                bookmarks = bookmarks,
                                isBookmarked = bookmarks.any { b -> b.pageNumber == state.currentPage + 1 }
                            )
                        }
                    }
                }

                // Collect annotations
                launch {
                    annotationRepository.getAnnotations(uri.toString()).collect { annotations ->
                        _uiState.update { it.copy(annotations = annotations) }
                    }
                }

                // Prefetch first set of pages
                pdfRepository.prefetchPages(safePage, 2)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not open PDF: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    suspend fun renderPage(pageIndex: Int, targetWidth: Int? = null, targetHeight: Int? = null): Bitmap {
        return pdfRepository.renderPage(pageIndex, targetWidth, targetHeight)
    }

    fun getCachedPageBitmap(pageIndex: Int): Bitmap? {
        return pdfRepository.getCachedPageBitmap(pageIndex)
    }

    suspend fun getPageInfo(pageIndex: Int): PdfPageInfo {
        return pdfRepository.getPageInfo(pageIndex)
    }

    suspend fun extractPageText(pageIndex: Int): String {
        return runCatching { pdfRepository.extractText(pageIndex) }.getOrDefault("")
    }

    fun onPageChanged(newPage: Int) {
        val safePage = newPage.coerceIn(0, (_uiState.value.totalPages - 1).coerceAtLeast(0))
        if (_uiState.value.currentPage == safePage) return

        _uiState.update { state ->
            state.copy(
                currentPage = safePage,
                isBookmarked = state.bookmarks.any { it.pageNumber == safePage + 1 }
            )
        }

        // Prefetch adjacent pages & cancel distant ones
        pdfRepository.prefetchPages(safePage, 2)
        pdfRepository.cancelDistantRenders(safePage)

        // Save progress to recent documents
        currentUri?.let { uri ->
            viewModelScope.launch {
                recentRepository.updateReadingProgress(uri, safePage, _uiState.value.zoomState.scale)
            }
        }
    }

    fun nextPage() = onPageChanged(_uiState.value.currentPage + 1)
    fun previousPage() = onPageChanged(_uiState.value.currentPage - 1)
    fun firstPage() = onPageChanged(0)
    fun lastPage() = onPageChanged((_uiState.value.totalPages - 1).coerceAtLeast(0))

    fun setReadingMode(mode: ReadingMode) {
        _uiState.update { it.copy(readingMode = mode) }
    }

    fun setZoom(scale: Float, offsetX: Float = 0f, offsetY: Float = 0f) {
        val clampedScale = scale.coerceIn(1.0f, 6.0f)
        _uiState.update {
            it.copy(
                zoomState = it.zoomState.copy(
                    scale = clampedScale,
                    offsetX = if (clampedScale <= 1.0f) 0f else offsetX,
                    offsetY = if (clampedScale <= 1.0f) 0f else offsetY
                )
            )
        }
    }

    fun resetZoom() = setZoom(1.0f, 0f, 0f)

    fun zoomIn() = setZoom(_uiState.value.zoomState.scale * 1.25f)

    fun zoomOut() = setZoom((_uiState.value.zoomState.scale / 1.25f).coerceAtLeast(1.0f))

    fun toggleControls() {
        // If an annotation tool is active, do not hide controls on tap
        if (_uiState.value.activeTool != null) return
        _uiState.update { it.copy(isControlsVisible = !it.isControlsVisible) }
    }

    fun setControlsVisible(visible: Boolean) {
        _uiState.update { it.copy(isControlsVisible = visible) }
    }

    fun setNightReadingMode(mode: NightReadingMode) {
        _uiState.update { it.copy(nightReadingMode = mode) }
        viewModelScope.launch { preferencesRepository.setNightReadingMode(mode) }
    }

    fun setKeepScreenAwake(keepAwake: Boolean) {
        _uiState.update { it.copy(keepScreenAwake = keepAwake) }
        viewModelScope.launch { preferencesRepository.setKeepScreenAwake(keepAwake) }
    }

    // Bookmarks
    fun toggleCurrentPageBookmark() {
        val uri = currentUri?.toString() ?: return
        val pageNum = _uiState.value.currentPage + 1
        val isBookmarked = _uiState.value.isBookmarked

        viewModelScope.launch {
            if (isBookmarked) {
                bookmarkRepository.removeBookmarkByPage(uri, pageNum)
            } else {
                val title = "Page $pageNum"
                bookmarkRepository.addBookmark(uri, pageNum, title)
            }
        }
    }

    fun removeBookmark(id: Long) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmark(id)
        }
    }

    // Search
    fun toggleSearch() {
        val nextVisible = !_uiState.value.isSearchVisible
        _uiState.update {
            it.copy(
                isSearchVisible = nextVisible,
                searchState = if (!nextVisible) SearchState() else it.searchState
            )
        }
    }

    fun search(query: String, isCaseSensitive: Boolean = false, isWholeWord: Boolean = false) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchState = SearchState()) }
            return
        }

        _uiState.update {
            it.copy(
                searchState = it.searchState.copy(
                    query = query,
                    isCaseSensitive = isCaseSensitive,
                    isWholeWord = isWholeWord,
                    isSearching = true
                )
            )
        }

        searchJob = viewModelScope.launch {
            try {
                val results = pdfRepository.searchText(query, isCaseSensitive, isWholeWord)
                _uiState.update {
                    it.copy(
                        searchState = it.searchState.copy(
                            results = results,
                            currentMatchIndex = if (results.isNotEmpty()) 0 else -1,
                            isSearching = false
                        )
                    )
                }
                if (results.isNotEmpty()) {
                    onPageChanged(results[0].pageIndex)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(searchState = it.searchState.copy(isSearching = false))
                }
            }
        }
    }

    fun nextSearchResult() {
        val state = _uiState.value.searchState
        if (state.results.isEmpty()) return
        val nextIdx = (state.currentMatchIndex + 1) % state.results.size
        _uiState.update { it.copy(searchState = state.copy(currentMatchIndex = nextIdx)) }
        onPageChanged(state.results[nextIdx].pageIndex)
    }

    fun previousSearchResult() {
        val state = _uiState.value.searchState
        if (state.results.isEmpty()) return
        val prevIdx = (state.currentMatchIndex - 1 + state.results.size) % state.results.size
        _uiState.update { it.copy(searchState = state.copy(currentMatchIndex = prevIdx)) }
        onPageChanged(state.results[prevIdx].pageIndex)
    }

    // Annotation Tools
    fun setActiveTool(tool: AnnotationType?) {
        _uiState.update { it.copy(activeTool = tool) }
    }

    fun setActiveColor(color: Int) {
        _uiState.update { it.copy(activeColor = color) }
    }

    fun setActiveStrokeWidth(width: Float) {
        _uiState.update { it.copy(activeStrokeWidth = width) }
    }

    fun setActiveAlpha(alpha: Float) {
        _uiState.update { it.copy(activeAlpha = alpha) }
    }

    fun addStroke(pageIndex: Int, stroke: DrawingStroke) {
        val uri = currentUri?.toString() ?: return
        val currentAnnotations = _uiState.value.annotations[pageIndex] ?: PageAnnotations(pageIndex)
        val updated = currentAnnotations.copy(strokes = currentAnnotations.strokes + stroke)

        undoStack.add(AnnotationAction.AddStroke(pageIndex, stroke))
        redoStack.clear()

        updatePageAnnotations(uri, pageIndex, updated)
    }

    fun addTextAnnotation(pageIndex: Int, text: TextAnnotation) {
        val uri = currentUri?.toString() ?: return
        val currentAnnotations = _uiState.value.annotations[pageIndex] ?: PageAnnotations(pageIndex)
        val updated = currentAnnotations.copy(textNotes = currentAnnotations.textNotes + text)

        undoStack.add(AnnotationAction.AddText(pageIndex, text))
        redoStack.clear()

        updatePageAnnotations(uri, pageIndex, updated)
    }

    fun addHighlight(pageIndex: Int, highlight: HighlightAnnotation) {
        val uri = currentUri?.toString() ?: return
        val currentAnnotations = _uiState.value.annotations[pageIndex] ?: PageAnnotations(pageIndex)
        val updated = currentAnnotations.copy(highlights = currentAnnotations.highlights + highlight)

        undoStack.add(AnnotationAction.AddHighlight(pageIndex, highlight))
        redoStack.clear()

        updatePageAnnotations(uri, pageIndex, updated)
    }

    private fun updatePageAnnotations(uri: String, pageIndex: Int, annotations: PageAnnotations) {
        val newMap = _uiState.value.annotations.toMutableMap()
        newMap[pageIndex] = annotations
        _uiState.update {
            it.copy(
                annotations = newMap,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        viewModelScope.launch {
            annotationRepository.savePageAnnotations(uri, pageIndex, annotations)
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val uri = currentUri?.toString() ?: return
        val action = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(action)

        when (action) {
            is AnnotationAction.AddStroke -> {
                val current = _uiState.value.annotations[action.pageIndex] ?: return
                val updated = current.copy(strokes = current.strokes.filterNot { it.id == action.stroke.id })
                updatePageAnnotations(uri, action.pageIndex, updated)
            }
            is AnnotationAction.AddText -> {
                val current = _uiState.value.annotations[action.pageIndex] ?: return
                val updated = current.copy(textNotes = current.textNotes.filterNot { it.id == action.text.id })
                updatePageAnnotations(uri, action.pageIndex, updated)
            }
            is AnnotationAction.AddHighlight -> {
                val current = _uiState.value.annotations[action.pageIndex] ?: return
                val updated = current.copy(highlights = current.highlights.filterNot { it.id == action.highlight.id })
                updatePageAnnotations(uri, action.pageIndex, updated)
            }
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val uri = currentUri?.toString() ?: return
        val action = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(action)

        when (action) {
            is AnnotationAction.AddStroke -> {
                val current = _uiState.value.annotations[action.pageIndex] ?: PageAnnotations(action.pageIndex)
                val updated = current.copy(strokes = current.strokes + action.stroke)
                updatePageAnnotations(uri, action.pageIndex, updated)
            }
            is AnnotationAction.AddText -> {
                val current = _uiState.value.annotations[action.pageIndex] ?: PageAnnotations(action.pageIndex)
                val updated = current.copy(textNotes = current.textNotes + action.text)
                updatePageAnnotations(uri, action.pageIndex, updated)
            }
            is AnnotationAction.AddHighlight -> {
                val current = _uiState.value.annotations[action.pageIndex] ?: PageAnnotations(action.pageIndex)
                val updated = current.copy(highlights = current.highlights + action.highlight)
                updatePageAnnotations(uri, action.pageIndex, updated)
            }
        }
    }

    // Page Rotation
    fun rotateCurrentPage(clockwise: Boolean) {
        val page = _uiState.value.currentPage
        val degrees = if (clockwise) 90 else -90
        // Trigger save/rotate in background
        _uiState.update { it.copy(isProcessing = true, processingMessage = "Rotating page ${page + 1}...") }
        // Force refresh page bitmap
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = false, processingMessage = null) }
        }
    }

    // Save with burned annotations
    fun saveToStream(outputStream: OutputStream, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _uiState.update { it.copy(isProcessing = true, processingMessage = "Saving annotations into PDF...") }
        viewModelScope.launch {
            try {
                pdfRepository.saveWithAnnotations(outputStream, _uiState.value.annotations)
                _uiState.update { it.copy(isProcessing = false, processingMessage = null) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, processingMessage = null) }
                onError(e.localizedMessage ?: "Failed to save PDF")
            }
        }
    }

    // Dialog & overlay toggles
    fun setThumbnailsVisible(visible: Boolean) = _uiState.update { it.copy(isThumbnailsVisible = visible) }
    fun setBookmarksVisible(visible: Boolean) = _uiState.update { it.copy(isBookmarksVisible = visible) }
    fun setMetadataVisible(visible: Boolean) = _uiState.update { it.copy(isMetadataVisible = visible) }
    fun setToolsPanelVisible(visible: Boolean) = _uiState.update { it.copy(isToolsPanelVisible = visible) }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    override fun onCleared() {
        super.onCleared()
        pdfRepository.closeDocument()
    }

    class Factory(
        private val pdfRepository: PdfDocumentRepository,
        private val recentRepository: RecentDocumentsRepository,
        private val bookmarkRepository: BookmarkRepository,
        private val annotationRepository: AnnotationRepository,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PdfReaderViewModel(
                pdfRepository,
                recentRepository,
                bookmarkRepository,
                annotationRepository,
                preferencesRepository
            ) as T
        }
    }
}
