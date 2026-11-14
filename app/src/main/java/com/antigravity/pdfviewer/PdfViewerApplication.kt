package com.antigravity.pdfviewer

import android.app.Application
import com.antigravity.pdfviewer.data.local.database.AppDatabase
import com.antigravity.pdfviewer.data.local.preferences.UserPreferencesRepository
import com.antigravity.pdfviewer.data.pdf.NativePdfRendererManager
import com.antigravity.pdfviewer.data.pdf.PdfBoxManager
import com.antigravity.pdfviewer.data.repository.AnnotationRepositoryImpl
import com.antigravity.pdfviewer.data.repository.BookmarkRepositoryImpl
import com.antigravity.pdfviewer.data.repository.PdfDocumentRepositoryImpl
import com.antigravity.pdfviewer.data.repository.PdfToolsRepositoryImpl
import com.antigravity.pdfviewer.data.repository.RecentDocumentsRepositoryImpl
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class PdfViewerApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var pdfRendererManager: NativePdfRendererManager
        private set

    lateinit var pdfBoxManager: PdfBoxManager
        private set

    lateinit var officeConverter: com.antigravity.pdfviewer.data.pdf.OfficeToPdfConverter
        private set

    lateinit var pdfDocumentRepository: PdfDocumentRepositoryImpl
        private set

    lateinit var recentDocumentsRepository: RecentDocumentsRepositoryImpl
        private set

    lateinit var bookmarkRepository: BookmarkRepositoryImpl
        private set

    lateinit var annotationRepository: AnnotationRepositoryImpl
        private set

    lateinit var pdfToolsRepository: PdfToolsRepositoryImpl
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Apache PDFBox Android resource loader safely
        runCatching {
            PDFBoxResourceLoader.init(applicationContext)
        }.onFailure {
            it.printStackTrace()
        }

        // Local storage & preferences
        database = AppDatabase.getInstance(this)
        preferencesRepository = UserPreferencesRepository(this)

        // PDF & Office engines
        pdfRendererManager = NativePdfRendererManager(this)
        pdfBoxManager = PdfBoxManager(this)
        officeConverter = com.antigravity.pdfviewer.data.pdf.OfficeToPdfConverter(this)

        // Repositories
        recentDocumentsRepository = RecentDocumentsRepositoryImpl(database.recentDocumentDao())
        bookmarkRepository = BookmarkRepositoryImpl(database.bookmarkDao())
        annotationRepository = AnnotationRepositoryImpl(database.annotationDao())
        pdfDocumentRepository = PdfDocumentRepositoryImpl(
            context = this,
            rendererManager = pdfRendererManager,
            boxManager = pdfBoxManager,
            officeConverter = officeConverter,
            recentDao = database.recentDocumentDao(),
            bookmarkDao = database.bookmarkDao()
        )
        pdfToolsRepository = PdfToolsRepositoryImpl(
            context = this,
            pdfBoxManager = pdfBoxManager,
            rendererManager = pdfRendererManager
        )
    }

    companion object {
        lateinit var instance: PdfViewerApplication
            private set
    }
}
