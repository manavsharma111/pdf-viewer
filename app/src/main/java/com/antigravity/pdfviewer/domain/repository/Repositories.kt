package com.antigravity.pdfviewer.domain.repository

import android.graphics.Bitmap
import android.net.Uri
import com.antigravity.pdfviewer.domain.model.Bookmark
import com.antigravity.pdfviewer.domain.model.PageAnnotations
import com.antigravity.pdfviewer.domain.model.PdfDocumentInfo
import com.antigravity.pdfviewer.domain.model.PdfMetadata
import com.antigravity.pdfviewer.domain.model.PdfPageInfo
import com.antigravity.pdfviewer.domain.model.ReadingMode
import com.antigravity.pdfviewer.domain.model.SearchResult
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.OutputStream

interface PdfDocumentRepository {
    suspend fun openDocument(uri: Uri, password: String? = null): PdfDocumentInfo
    suspend fun renderPage(pageIndex: Int, targetWidth: Int? = null, targetHeight: Int? = null): Bitmap
    fun getCachedPageBitmap(pageIndex: Int): Bitmap?
    suspend fun getPageInfo(pageIndex: Int): PdfPageInfo
    fun prefetchPages(currentPage: Int, count: Int, targetWidth: Int? = null)
    fun cancelDistantRenders(currentPage: Int)
    suspend fun searchText(query: String, isCaseSensitive: Boolean, isWholeWord: Boolean): List<SearchResult>
    suspend fun extractText(pageIndex: Int? = null): String
    suspend fun rotatePages(outputStream: OutputStream, pages: Set<Int>?, degrees: Int)
    suspend fun saveWithAnnotations(outputStream: OutputStream, annotations: Map<Int, PageAnnotations>)
    fun closeDocument()
}

interface RecentDocumentsRepository {
    fun getRecentDocuments(): Flow<List<PdfDocumentInfo>>
    suspend fun addOrUpdateDocument(info: PdfDocumentInfo, lastPage: Int = 0, lastZoom: Float = 1.0f, readingMode: ReadingMode = ReadingMode.CONTINUOUS_VERTICAL)
    suspend fun updateReadingProgress(uri: Uri, page: Int, zoom: Float)
    suspend fun removeDocument(uri: Uri)
    suspend fun clearAll()
}

interface BookmarkRepository {
    fun getBookmarks(documentUri: String): Flow<List<Bookmark>>
    suspend fun addBookmark(documentUri: String, pageNumber: Int, title: String): Long
    suspend fun removeBookmark(id: Long)
    suspend fun removeBookmarkByPage(documentUri: String, pageNumber: Int)
}

interface AnnotationRepository {
    fun getAnnotations(documentUri: String): Flow<Map<Int, PageAnnotations>>
    suspend fun savePageAnnotations(documentUri: String, pageIndex: Int, annotations: PageAnnotations)
    suspend fun clearPageAnnotations(documentUri: String, pageIndex: Int)
    suspend fun clearDocumentAnnotations(documentUri: String)
}

interface PdfToolsRepository {
    suspend fun mergePdfs(uris: List<Uri>, outputStream: OutputStream, onProgress: (Int, Int) -> Unit)
    suspend fun splitPdf(uri: Uri, splitAtPages: Int, outputDir: File, baseName: String): List<File>
    suspend fun splitPdfByRanges(uri: Uri, ranges: List<IntRange>, outputDir: File, baseName: String): List<File>
    suspend fun extractSelectedPages(uri: Uri, pages: List<Int>, outputStream: OutputStream)
    suspend fun compressPdf(uri: Uri, outputStream: OutputStream, qualityLevel: String): Long
    suspend fun protectPdf(uri: Uri, outputStream: OutputStream, userPass: String, ownerPass: String)
    suspend fun convertPdfToImages(uri: Uri, outputDir: File, format: String, dpi: Int, onProgress: (Int, Int) -> Unit): List<File>
    suspend fun convertImagesToPdf(uris: List<Uri>, outputStream: OutputStream, pageSize: String)
    suspend fun extractAllText(uri: Uri, outputStream: OutputStream)
    suspend fun deletePages(uri: Uri, outputStream: OutputStream, pagesToDelete: Set<Int>)
    suspend fun reorderPages(uri: Uri, outputStream: OutputStream, newOrder: List<Int>)
}
