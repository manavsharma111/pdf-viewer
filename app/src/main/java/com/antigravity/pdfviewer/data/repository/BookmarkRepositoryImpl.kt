package com.antigravity.pdfviewer.data.repository

import com.antigravity.pdfviewer.data.local.dao.BookmarkDao
import com.antigravity.pdfviewer.data.local.entity.BookmarkEntity
import com.antigravity.pdfviewer.domain.model.Bookmark
import com.antigravity.pdfviewer.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookmarkRepositoryImpl(
    private val dao: BookmarkDao
) : BookmarkRepository {

    override fun getBookmarks(documentUri: String): Flow<List<Bookmark>> {
        return dao.getBookmarksForDocument(documentUri).map { list ->
            list.map { entity ->
                Bookmark(
                    id = entity.id,
                    documentUri = entity.documentUri,
                    pageNumber = entity.pageNumber,
                    title = entity.title,
                    timestamp = entity.timestamp
                )
            }
        }
    }

    override suspend fun addBookmark(documentUri: String, pageNumber: Int, title: String): Long {
        val entity = BookmarkEntity(
            documentUri = documentUri,
            pageNumber = pageNumber,
            title = title,
            timestamp = System.currentTimeMillis()
        )
        return dao.insert(entity)
    }

    override suspend fun removeBookmark(id: Long) {
        dao.delete(id)
    }

    override suspend fun removeBookmarkByPage(documentUri: String, pageNumber: Int) {
        dao.deleteByPage(documentUri, pageNumber)
    }
}
