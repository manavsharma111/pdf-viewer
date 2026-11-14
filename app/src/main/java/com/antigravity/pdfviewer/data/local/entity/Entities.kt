package com.antigravity.pdfviewer.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recent_documents",
    indices = [Index(value = ["uri"], unique = true)]
)
data class RecentDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val fileName: String,
    val fileSize: Long,
    val pageCount: Int,
    val lastOpenedTimestamp: Long,
    val lastPage: Int,
    val lastZoom: Float,
    val readingMode: String
)

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["documentUri", "pageNumber"])]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentUri: String,
    val pageNumber: Int,
    val title: String,
    val timestamp: Long
)

@Entity(
    tableName = "annotations",
    indices = [Index(value = ["documentUri", "pageIndex"])]
)
data class AnnotationEntity(
    @PrimaryKey
    val id: String,
    val documentUri: String,
    val pageIndex: Int,
    val type: String,
    val dataJson: String,
    val timestamp: Long
)
