package com.antigravity.pdfviewer.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import com.antigravity.pdfviewer.data.local.dao.BookmarkDao
import com.antigravity.pdfviewer.data.local.dao.RecentDocumentDao
import com.antigravity.pdfviewer.data.local.entity.RecentDocumentEntity
import com.antigravity.pdfviewer.data.pdf.NativePdfRendererManager
import com.antigravity.pdfviewer.data.pdf.PdfBoxManager
import com.antigravity.pdfviewer.domain.model.PageAnnotations
import com.antigravity.pdfviewer.domain.model.PdfDocumentInfo
import com.antigravity.pdfviewer.domain.model.PdfMetadata
import com.antigravity.pdfviewer.domain.model.PdfPageInfo
import com.antigravity.pdfviewer.domain.model.ReadingMode
import com.antigravity.pdfviewer.domain.model.SearchResult
import com.antigravity.pdfviewer.domain.repository.PdfDocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

class PdfDocumentRepositoryImpl(
    private val context: Context,
    private val rendererManager: NativePdfRendererManager,
    private val boxManager: PdfBoxManager,
    private val officeConverter: com.antigravity.pdfviewer.data.pdf.OfficeToPdfConverter,
    private val recentDao: RecentDocumentDao,
    private val bookmarkDao: BookmarkDao
) : PdfDocumentRepository {

    private var currentUri: Uri? = null
    private var currentPassword: String? = null
    private var convertedPdfFile: java.io.File? = null

    override suspend fun openDocument(uri: Uri, password: String?): PdfDocumentInfo = withContext(Dispatchers.IO) {
        currentUri = uri
        currentPassword = password

        val fileName = com.antigravity.pdfviewer.util.UriHelper.queryFileName(context, uri)
        val fileSize = com.antigravity.pdfviewer.util.UriHelper.queryFileSize(context, uri)
        val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        val officeType = com.antigravity.pdfviewer.data.pdf.OfficeDocumentType.fromFileNameOrMime(fileName, mimeType)

        val workingUri: Uri = if (officeType != com.antigravity.pdfviewer.data.pdf.OfficeDocumentType.PDF &&
            officeType != com.antigravity.pdfviewer.data.pdf.OfficeDocumentType.UNKNOWN
        ) {
            // Convert Office Document (Word, Excel, PowerPoint) to PDF
            val pdfFile = officeConverter.convertToPdf(uri, officeType)
            convertedPdfFile = pdfFile
            Uri.fromFile(pdfFile)
        } else {
            uri
        }

        val isEncrypted = runCatching { boxManager.isDocumentEncrypted(workingUri) }.getOrDefault(false)
        if (isEncrypted && password.isNullOrEmpty()) {
            // Cannot open without password
            return@withContext PdfDocumentInfo(
                uri = uri,
                fileName = fileName,
                fileSize = fileSize,
                pageCount = 0,
                isEncrypted = true
            )
        }

        val pageCount = if (workingUri.scheme == "file") {
            val path = workingUri.path ?: workingUri.toString().removePrefix("file://")
            rendererManager.openFile(java.io.File(path))
        } else {
            rendererManager.openDocument(workingUri)
        }

        val metadata = runCatching {
            boxManager.getDocumentMetadata(workingUri, password)
        }.getOrDefault(PdfMetadata(pageCount = pageCount, fileSize = fileSize))

        val info = PdfDocumentInfo(
            uri = uri,
            fileName = fileName,
            fileSize = fileSize,
            pageCount = pageCount,
            isEncrypted = isEncrypted,
            metadata = metadata
        )

        // Save to recent
        val existing = recentDao.getByUri(uri.toString())
        recentDao.insertOrUpdate(
            RecentDocumentEntity(
                id = existing?.id ?: 0,
                uri = uri.toString(),
                fileName = fileName,
                fileSize = fileSize,
                pageCount = pageCount,
                lastOpenedTimestamp = System.currentTimeMillis(),
                lastPage = existing?.lastPage ?: 0,
                lastZoom = existing?.lastZoom ?: 1.0f,
                readingMode = existing?.readingMode ?: ReadingMode.CONTINUOUS_VERTICAL.name
            )
        )

        info
    }

    override suspend fun renderPage(
        pageIndex: Int,
        targetWidth: Int?,
        targetHeight: Int?
    ): Bitmap {
        return rendererManager.renderPage(pageIndex, targetWidth, targetHeight)
    }

    override fun getCachedPageBitmap(pageIndex: Int): Bitmap? {
        return rendererManager.getCachedBitmap(pageIndex)
    }

    override suspend fun getPageInfo(pageIndex: Int): PdfPageInfo {
        return rendererManager.getPageInfo(pageIndex)
    }

    override fun prefetchPages(currentPage: Int, count: Int, targetWidth: Int?) {
        rendererManager.prefetchPages(currentPage, count, targetWidth)
    }

    override fun cancelDistantRenders(currentPage: Int) {
        rendererManager.cancelDistantJobs(currentPage)
    }

    override suspend fun searchText(
        query: String,
        isCaseSensitive: Boolean,
        isWholeWord: Boolean
    ): List<SearchResult> {
        val uri = currentUri ?: return emptyList()
        return boxManager.searchText(uri, query, isCaseSensitive, isWholeWord, currentPassword)
    }

    override suspend fun extractText(pageIndex: Int?): String {
        val uri = currentUri ?: return ""
        return boxManager.extractText(uri, pageIndex, pageIndex, currentPassword)
    }

    override suspend fun rotatePages(outputStream: OutputStream, pages: Set<Int>?, degrees: Int) {
        val uri = currentUri ?: throw IllegalStateException("No document opened")
        boxManager.rotatePages(uri, outputStream, pages, degrees, currentPassword)
    }

    override suspend fun saveWithAnnotations(
        outputStream: OutputStream,
        annotations: Map<Int, PageAnnotations>
    ) {
        val uri = currentUri ?: throw IllegalStateException("No document opened")
        boxManager.burnAnnotationsIntoPdf(uri, outputStream, annotations, currentPassword)
    }

    override fun closeDocument() {
        rendererManager.closeDocument()
        convertedPdfFile?.let { runCatching { it.delete() } }
        convertedPdfFile = null
        currentUri = null
        currentPassword = null
    }

    private fun queryFileName(uri: Uri): String {
        val queried = runCatching {
            var name: String? = null
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            }
            name
        }.getOrNull()

        return queried
            ?: uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
            ?: "Document.pdf"
    }

    private fun queryFileSize(uri: Uri): Long {
        return runCatching {
            var size = 0L
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && cursor.moveToFirst()) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
            size
        }.getOrDefault(0L)
    }
}
