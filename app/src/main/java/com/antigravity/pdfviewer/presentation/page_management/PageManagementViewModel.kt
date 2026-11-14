package com.antigravity.pdfviewer.presentation.page_management

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigravity.pdfviewer.domain.repository.PdfDocumentRepository
import com.antigravity.pdfviewer.domain.repository.PdfToolsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.OutputStream

data class PageItem(
    val originalIndex: Int,
    val displayIndex: Int,
    var rotation: Int = 0
)

data class PageManagementUiState(
    val uri: Uri? = null,
    val pages: List<PageItem> = emptyList(),
    val selectedIndices: Set<Int> = emptySet(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class PageManagementViewModel(
    private val pdfRepository: PdfDocumentRepository,
    private val toolsRepository: PdfToolsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PageManagementUiState())
    val uiState: StateFlow<PageManagementUiState> = _uiState.asStateFlow()

    fun loadPages(uri: Uri) {
        _uiState.update { it.copy(uri = uri, isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val doc = pdfRepository.openDocument(uri)
                val list = (0 until doc.pageCount).map { i ->
                    PageItem(originalIndex = i, displayIndex = i + 1)
                }
                _uiState.update { it.copy(pages = list, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load pages: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    suspend fun renderThumbnail(pageIndex: Int): Bitmap {
        return pdfRepository.renderPage(pageIndex, targetWidth = 240, targetHeight = 320)
    }

    fun togglePageSelection(index: Int) {
        _uiState.update {
            val selected = it.selectedIndices.toMutableSet()
            if (selected.contains(index)) selected.remove(index) else selected.add(index)
            it.copy(selectedIndices = selected)
        }
    }

    fun selectAll() {
        _uiState.update {
            it.copy(selectedIndices = it.pages.indices.toSet())
        }
    }

    fun deselectAll() {
        _uiState.update {
            it.copy(selectedIndices = emptySet())
        }
    }

    fun deleteSelectedPages() {
        val selected = _uiState.value.selectedIndices
        if (selected.isEmpty()) return

        _uiState.update {
            val updated = it.pages.filterIndexed { index, _ -> !selected.contains(index) }
                .mapIndexed { idx, item -> item.copy(displayIndex = idx + 1) }
            it.copy(pages = updated, selectedIndices = emptySet())
        }
    }

    fun rotateSelectedPages(clockwise: Boolean) {
        val selected = _uiState.value.selectedIndices
        if (selected.isEmpty()) return
        val delta = if (clockwise) 90 else -90

        _uiState.update {
            val updated = it.pages.mapIndexed { index, item ->
                if (selected.contains(index)) {
                    item.copy(rotation = (item.rotation + delta + 360) % 360)
                } else {
                    item
                }
            }
            it.copy(pages = updated)
        }
    }

    fun moveSelectedUp() {
        val selected = _uiState.value.selectedIndices
        if (selected.isEmpty() || selected.contains(0)) return

        val currentList = _uiState.value.pages.toMutableList()
        val newSelected = mutableSetOf<Int>()

        for (i in 1 until currentList.size) {
            if (selected.contains(i)) {
                val temp = currentList[i]
                currentList[i] = currentList[i - 1]
                currentList[i - 1] = temp
                newSelected.add(i - 1)
            }
        }

        _uiState.update {
            it.copy(
                pages = currentList.mapIndexed { idx, item -> item.copy(displayIndex = idx + 1) },
                selectedIndices = newSelected
            )
        }
    }

    fun moveSelectedDown() {
        val selected = _uiState.value.selectedIndices
        val size = _uiState.value.pages.size
        if (selected.isEmpty() || selected.contains(size - 1)) return

        val currentList = _uiState.value.pages.toMutableList()
        val newSelected = mutableSetOf<Int>()

        for (i in (size - 2) downTo 0) {
            if (selected.contains(i)) {
                val temp = currentList[i]
                currentList[i] = currentList[i + 1]
                currentList[i + 1] = temp
                newSelected.add(i + 1)
            }
        }

        _uiState.update {
            it.copy(
                pages = currentList.mapIndexed { idx, item -> item.copy(displayIndex = idx + 1) },
                selectedIndices = newSelected
            )
        }
    }

    fun duplicateSelectedPages() {
        val selected = _uiState.value.selectedIndices
        if (selected.isEmpty()) return

        val currentList = _uiState.value.pages.toMutableList()
        val toInsert = currentList.filterIndexed { index, _ -> selected.contains(index) }
        currentList.addAll(toInsert)

        _uiState.update {
            it.copy(
                pages = currentList.mapIndexed { idx, item -> item.copy(displayIndex = idx + 1) },
                selectedIndices = emptySet()
            )
        }
    }

    fun saveChanges(outputStream: OutputStream) {
        val uri = _uiState.value.uri ?: return
        val currentPages = _uiState.value.pages
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val order = currentPages.map { it.originalIndex }
                toolsRepository.reorderPages(uri, outputStream, order)
                _uiState.update { it.copy(isSaving = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Failed to save: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun extractSelectedToStream(outputStream: OutputStream) {
        val uri = _uiState.value.uri ?: return
        val selected = _uiState.value.selectedIndices
        if (selected.isEmpty()) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val pagesToExtract = selected.sorted().map { _uiState.value.pages[it].originalIndex }
                toolsRepository.extractSelectedPages(uri, pagesToExtract.map { it + 1 }, outputStream)
                _uiState.update { it.copy(isSaving = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Extraction failed: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    class Factory(
        private val pdfRepository: PdfDocumentRepository,
        private val toolsRepository: PdfToolsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PageManagementViewModel(pdfRepository, toolsRepository) as T
        }
    }
}
