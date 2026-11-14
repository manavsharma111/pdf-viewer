package com.antigravity.pdfviewer.presentation.tools

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigravity.pdfviewer.domain.repository.PdfToolsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream

data class ToolExecutionState(
    val isRunning: Boolean = false,
    val progress: Float = 0f,
    val currentStep: String = "",
    val isSuccess: Boolean = false,
    val successMessage: String? = null,
    val outputFiles: List<File> = emptyList(),
    val errorMessage: String? = null
)

class PdfToolsViewModel(
    private val toolsRepository: PdfToolsRepository
) : ViewModel() {

    private val _toolState = MutableStateFlow(ToolExecutionState())
    val toolState: StateFlow<ToolExecutionState> = _toolState.asStateFlow()

    fun resetState() {
        _toolState.value = ToolExecutionState()
    }

    fun mergePdfs(uris: List<Uri>, outputStream: OutputStream) {
        _toolState.update {
            it.copy(
                isRunning = true,
                progress = 0f,
                currentStep = "Merging ${uris.size} PDF files...",
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                toolsRepository.mergePdfs(uris, outputStream) { current, total ->
                    _toolState.update {
                        it.copy(
                            progress = current.toFloat() / total.toFloat(),
                            currentStep = "Merging document $current of $total..."
                        )
                    }
                }
                _toolState.update {
                    it.copy(
                        isRunning = false,
                        isSuccess = true,
                        successMessage = "Successfully merged ${uris.size} PDFs!"
                    )
                }
            } catch (e: Exception) {
                _toolState.update {
                    it.copy(
                        isRunning = false,
                        errorMessage = "Failed to merge: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            } finally {
                runCatching { outputStream.close() }
            }
        }
    }

    fun splitPdfByEveryNPages(uri: Uri, splitAt: Int, outputDir: File, baseName: String) {
        _toolState.update {
            it.copy(
                isRunning = true,
                progress = 0f,
                currentStep = "Splitting PDF every $splitAt pages...",
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                val files = toolsRepository.splitPdf(uri, splitAt, outputDir, baseName)
                _toolState.update {
                    it.copy(
                        isRunning = false,
                        isSuccess = true,
                        outputFiles = files,
                        successMessage = "PDF split into ${files.size} parts successfully!"
                    )
                }
            } catch (e: Exception) {
                _toolState.update {
                    it.copy(
                        isRunning = false,
                        errorMessage = "Failed to split: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun splitPdfByRanges(uri: Uri, ranges: List<IntRange>, outputDir: File, baseName: String) {
        _toolState.update {
            it.copy(
                isRunning = true,
                progress = 0f,
                currentStep = "Splitting PDF into ${ranges.size} ranges...",
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                val files = toolsRepository.splitPdfByRanges(uri, ranges, outputDir, baseName)
                _toolState.update {
                    it.copy(
                        isRunning = false,
                        isSuccess = true,
                        outputFiles = files,
                        successMessage = "Extracted ${files.size} documents successfully!"
                    )
                }
            } catch (e: Exception) {
                _toolState.update {
                    it.copy(
                        isRunning = false,
                        errorMessage = "Failed to split: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun compressPdf(uri: Uri, outputStream: OutputStream, level: String) {
        _toolState.update {
            it.copy(
                isRunning = true,
                progress = 0.5f,
                currentStep = "Compressing embedded images and optimizing PDF...",
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                toolsRepository.compressPdf(uri, outputStream, level)
                _toolState.update {
                    it.copy(
                        isRunning = false,
                        isSuccess = true,
                        successMessage = "PDF compression finished successfully!"
                    )
                }
            } catch (e: Exception) {
                _toolState.update {
                    it.copy(
                        isRunning = false,
                        errorMessage = "Compression failed: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            } finally {
                runCatching { outputStream.close() }
            }
        }
    }

    fun protectPdf(uri: Uri, outputStream: OutputStream, userPass: String, ownerPass: String) {
        _toolState.update {
            it.copy(
                isRunning = true,
                progress = 0.5f,
                currentStep = "Encrypting PDF with 128-bit AES...",
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                toolsRepository.protectPdf(uri, outputStream, userPass, ownerPass)
                _toolState.update {
                    it.copy(
                        isRunning = false,
                        isSuccess = true,
                        successMessage = "PDF encrypted and protected successfully!"
                    )
                }
            } catch (e: Exception) {
                _toolState.update {
                    it.copy(
                        isRunning = false,
                        errorMessage = "Encryption failed: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            } finally {
                runCatching { outputStream.close() }
            }
        }
    }

    fun convertPdfToImages(uri: Uri, outputDir: File, format: String, dpi: Int) {
        _toolState.update {
            it.copy(
                isRunning = true,
                progress = 0f,
                currentStep = "Rendering pages to $format at $dpi DPI...",
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                val files = toolsRepository.convertPdfToImages(uri, outputDir, format, dpi) { current, total ->
                    _toolState.update {
                        it.copy(
                            progress = current.toFloat() / total.toFloat(),
                            currentStep = "Rendering page $current / $total (${((current.toFloat()/total)*100).toInt()}%)"
                        )
                    }
                }
                _toolState.update {
                    it.copy(
                        isRunning = false,
                        isSuccess = true,
                        outputFiles = files,
                        successMessage = "Rendered ${files.size} pages to images successfully!"
                    )
                }
            } catch (e: Exception) {
                _toolState.update {
                    it.copy(
                        isRunning = false,
                        errorMessage = "Conversion failed: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun convertImagesToPdf(uris: List<Uri>, outputStream: OutputStream, pageSize: String) {
        _toolState.update {
            it.copy(
                isRunning = true,
                progress = 0.5f,
                currentStep = "Compiling ${uris.size} images into PDF...",
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                toolsRepository.convertImagesToPdf(uris, outputStream, pageSize)
                _toolState.update {
                    it.copy(
                        isRunning = false,
                        isSuccess = true,
                        successMessage = "Created PDF from ${uris.size} images successfully!"
                    )
                }
            } catch (e: Exception) {
                _toolState.update {
                    it.copy(
                        isRunning = false,
                        errorMessage = "Failed to convert images: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            } finally {
                runCatching { outputStream.close() }
            }
        }
    }

    fun extractText(uri: Uri, outputStream: OutputStream) {
        _toolState.update {
            it.copy(
                isRunning = true,
                progress = 0.5f,
                currentStep = "Extracting text from PDF...",
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                toolsRepository.extractAllText(uri, outputStream)
                _toolState.update {
                    it.copy(
                        isRunning = false,
                        isSuccess = true,
                        successMessage = "Extracted text to file successfully!"
                    )
                }
            } catch (e: Exception) {
                _toolState.update {
                    it.copy(
                        isRunning = false,
                        errorMessage = "Text extraction failed: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            } finally {
                runCatching { outputStream.close() }
            }
        }
    }

    class Factory(
        private val toolsRepository: PdfToolsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PdfToolsViewModel(toolsRepository) as T
        }
    }
}
