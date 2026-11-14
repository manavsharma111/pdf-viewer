package com.antigravity.pdfviewer.data.repository

import android.net.Uri
import com.antigravity.pdfviewer.data.local.dao.RecentDocumentDao
import com.antigravity.pdfviewer.data.local.entity.RecentDocumentEntity
import com.antigravity.pdfviewer.domain.model.PdfDocumentInfo
import com.antigravity.pdfviewer.domain.model.PdfMetadata
import com.antigravity.pdfviewer.domain.model.ReadingMode
import com.antigravity.pdfviewer.domain.repository.RecentDocumentsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecentDocumentsRepositoryImpl(
    private val dao: RecentDocumentDao
) : RecentDocumentsRepository {

    override fun getRecentDocuments(): Flow<List<PdfDocumentInfo>> {
        return dao.getAll().map { list ->
            list.map { entity ->
                PdfDocumentInfo(
                    uri = Uri.parse(entity.uri),
                    fileName = entity.fileName,
                    fileSize = entity.fileSize,
                    pageCount = entity.pageCount,
                    isEncrypted = false,
                    metadata = PdfMetadata(pageCount = entity.pageCount, fileSize = entity.fileSize)
                )
            }
        }
    }

    override suspend fun addOrUpdateDocument(
        info: PdfDocumentInfo,
        lastPage: Int,
        lastZoom: Float,
        readingMode: ReadingMode
    ) {
        val existing = dao.getByUri(info.uri.toString())
        val entity = RecentDocumentEntity(
            id = existing?.id ?: 0,
            uri = info.uri.toString(),
            fileName = info.fileName,
            fileSize = info.fileSize,
            pageCount = info.pageCount,
            lastOpenedTimestamp = System.currentTimeMillis(),
            lastPage = lastPage,
            lastZoom = lastZoom,
            readingMode = readingMode.name
        )
        dao.insertOrUpdate(entity)
        dao.trimExcess(50)
    }

    override suspend fun updateReadingProgress(uri: Uri, page: Int, zoom: Float) {
        dao.updateProgress(uri.toString(), page, zoom, System.currentTimeMillis())
    }

    override suspend fun removeDocument(uri: Uri) {
        dao.deleteByUri(uri.toString())
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }
}
