package com.antigravity.pdfviewer.presentation.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigravity.pdfviewer.domain.model.PdfDocumentInfo
import com.antigravity.pdfviewer.domain.repository.RecentDocumentsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val recentDocuments: List<PdfDocumentInfo> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val resumeDocument: PdfDocumentInfo? = null,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val recentRepository: RecentDocumentsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _resumeDocument = MutableStateFlow<PdfDocumentInfo?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        recentRepository.getRecentDocuments(),
        _searchQuery,
        _isLoading,
        _resumeDocument,
        _errorMessage
    ) { recents, query, loading, resumeDoc, error ->
        val filtered = if (query.isBlank()) {
            recents
        } else {
            recents.filter { it.fileName.contains(query, ignoreCase = true) }
        }
        HomeUiState(
            recentDocuments = filtered,
            searchQuery = query,
            isLoading = loading,
            resumeDocument = resumeDoc,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun removeRecentDocument(uri: Uri) {
        viewModelScope.launch {
            recentRepository.removeDocument(uri)
        }
    }

    fun clearAllRecentDocuments() {
        viewModelScope.launch {
            recentRepository.clearAll()
        }
    }

    fun onDocumentSelected(document: PdfDocumentInfo) {
        // If document was previously read past page 0, offer resume
        _resumeDocument.value = document
    }

    fun dismissResumePrompt() {
        _resumeDocument.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(
        private val recentRepository: RecentDocumentsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(recentRepository) as T
        }
    }
}
