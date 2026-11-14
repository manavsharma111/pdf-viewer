package com.antigravity.pdfviewer.domain.model

import android.net.Uri

enum class ReadingMode {
    SINGLE_PAGE,
    CONTINUOUS_VERTICAL,
    CONTINUOUS_HORIZONTAL,
    DOUBLE_PAGE
}

enum class AppTheme(val displayName: String, val isPremium: Boolean = false) {
    SYSTEM("System Default"),
    LIGHT("Xodo Light"),
    DARK("Xodo Dark"),
    OLED_ULTRA_DARK("OLED Ultra Dark", isPremium = true),
    PREMIUM_DARK("Premium Dark", isPremium = true),
    NIGHT_SEA("Night Sea", isPremium = true)
}

enum class NightReadingMode {
    NONE,
    INVERTED,
    SEPIA,
    NIGHT_GRAY
}

enum class CachePreset {
    LOW,
    MEDIUM,
    HIGH
}

data class ZoomState(
    val scale: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val minScale: Float = 1.0f,
    val maxScale: Float = 5.0f
)

data class PdfMetadata(
    val title: String? = null,
    val author: String? = null,
    val subject: String? = null,
    val keywords: String? = null,
    val creator: String? = null,
    val producer: String? = null,
    val creationDate: String? = null,
    val modificationDate: String? = null,
    val version: String? = null,
    val pageCount: Int = 0,
    val fileSize: Long = 0L
)

data class PdfDocumentInfo(
    val uri: Uri,
    val fileName: String,
    val fileSize: Long,
    val pageCount: Int,
    val isEncrypted: Boolean = false,
    val metadata: PdfMetadata = PdfMetadata()
)

data class PdfPageInfo(
    val pageIndex: Int,
    val width: Int,
    val height: Int,
    val rotation: Int = 0
)

data class SearchResult(
    val pageIndex: Int,
    val matchText: String,
    val snippet: String,
    val charOffset: Int
)

data class SearchState(
    val query: String = "",
    val isCaseSensitive: Boolean = false,
    val isWholeWord: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val currentMatchIndex: Int = -1,
    val isSearching: Boolean = false
) {
    val totalMatches: Int get() = results.size
    val currentMatch: SearchResult?
        get() = if (currentMatchIndex in results.indices) results[currentMatchIndex] else null
}

data class UserSettings(
    val theme: AppTheme = AppTheme.DARK,
    val useSingleLineToolbar: Boolean = true,
    val fullScreenMode: Boolean = true,
    val screenSleepLock: Boolean = true,
    val rememberLastPage: Boolean = true,
    val autoSaveWarning: Boolean = false,
    val rightToLeftSupport: Boolean = false,
    val enableJavaScript: Boolean = true,
    val nightReadingMode: NightReadingMode = NightReadingMode.NONE,
    val keepScreenAwake: Boolean = false,
    val defaultReadingMode: ReadingMode = ReadingMode.CONTINUOUS_VERTICAL,
    val defaultZoom: Float = 1.0f,
    val rememberPosition: Boolean = true,
    val cacheSizePreset: CachePreset = CachePreset.MEDIUM,
    val prefetchPages: Int = 2,
    val hardwareAcceleration: Boolean = true,
    val maxRecentDocuments: Int = 20
)
