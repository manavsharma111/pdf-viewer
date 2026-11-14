package com.antigravity.pdfviewer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.antigravity.pdfviewer.data.local.entity.AnnotationEntity
import com.antigravity.pdfviewer.data.local.entity.BookmarkEntity
import com.antigravity.pdfviewer.data.local.entity.RecentDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentDocumentDao {
    @Query("SELECT * FROM recent_documents ORDER BY lastOpenedTimestamp DESC")
    fun getAll(): Flow<List<RecentDocumentEntity>>

    @Query("SELECT * FROM recent_documents WHERE uri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): RecentDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(document: RecentDocumentEntity): Long

    @Query("UPDATE recent_documents SET lastPage = :lastPage, lastZoom = :lastZoom, lastOpenedTimestamp = :timestamp WHERE uri = :uri")
    suspend fun updateProgress(uri: String, lastPage: Int, lastZoom: Float, timestamp: Long)

    @Query("DELETE FROM recent_documents WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM recent_documents")
    suspend fun clearAll()

    @Query("DELETE FROM recent_documents WHERE id NOT IN (SELECT id FROM recent_documents ORDER BY lastOpenedTimestamp DESC LIMIT :limit)")
    suspend fun trimExcess(limit: Int)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE documentUri = :documentUri ORDER BY pageNumber ASC")
    fun getBookmarksForDocument(documentUri: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM bookmarks WHERE documentUri = :documentUri AND pageNumber = :pageNumber")
    suspend fun deleteByPage(documentUri: String, pageNumber: Int)

    @Query("DELETE FROM bookmarks WHERE documentUri = :documentUri")
    suspend fun deleteForDocument(documentUri: String)
}

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE documentUri = :documentUri")
    fun getAnnotationsForDocument(documentUri: String): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations WHERE documentUri = :documentUri AND pageIndex = :pageIndex")
    suspend fun getAnnotationsForPage(documentUri: String, pageIndex: Int): List<AnnotationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(annotation: AnnotationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(annotations: List<AnnotationEntity>)

    @Query("DELETE FROM annotations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM annotations WHERE documentUri = :documentUri AND pageIndex = :pageIndex")
    suspend fun deleteForPage(documentUri: String, pageIndex: Int)

    @Query("DELETE FROM annotations WHERE documentUri = :documentUri")
    suspend fun deleteForDocument(documentUri: String)
}
