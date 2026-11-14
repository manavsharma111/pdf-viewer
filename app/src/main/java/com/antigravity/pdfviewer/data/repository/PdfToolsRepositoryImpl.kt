package com.antigravity.pdfviewer.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.antigravity.pdfviewer.data.pdf.NativePdfRendererManager
import com.antigravity.pdfviewer.data.pdf.PdfBoxManager
import com.antigravity.pdfviewer.domain.repository.PdfToolsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class PdfToolsRepositoryImpl(
    private val context: Context,
    private val pdfBoxManager: PdfBoxManager,
    private val rendererManager: NativePdfRendererManager
) : PdfToolsRepository {

    override suspend fun mergePdfs(
        uris: List<Uri>,
        outputStream: OutputStream,
        onProgress: (Int, Int) -> Unit
    ) {
        pdfBoxManager.mergeDocuments(uris, outputStream, onProgress)
    }

    override suspend fun splitPdf(
        uri: Uri,
        splitAtPages: Int,
        outputDir: File,
        baseName: String
    ): List<File> {
        return pdfBoxManager.splitDocument(uri, splitAtPages, outputDir, baseName)
    }

    override suspend fun splitPdfByRanges(
        uri: Uri,
        ranges: List<IntRange>,
        outputDir: File,
        baseName: String
    ): List<File> = withContext(Dispatchers.IO) {
        val resultFiles = mutableListOf<File>()
        ranges.forEachIndexed { index, range ->
            val outFile = File(outputDir, "${baseName}_pages_${range.first}_to_${range.last}.pdf")
            FileOutputStream(outFile).use { outStream ->
                val pageIndices = range.map { it - 1 } // 1-based to 0-based
                pdfBoxManager.extractPages(uri, outStream, pageIndices)
            }
            resultFiles.add(outFile)
        }
        resultFiles
    }

    override suspend fun extractSelectedPages(
        uri: Uri,
        pages: List<Int>,
        outputStream: OutputStream
    ) {
        val zeroBasedPages = pages.map { it - 1 }
        pdfBoxManager.extractPages(uri, outputStream, zeroBasedPages)
    }

    override suspend fun compressPdf(
        uri: Uri,
        outputStream: OutputStream,
        qualityLevel: String
    ): Long = withContext(Dispatchers.IO) {
        val (quality, maxDim) = when (qualityLevel.uppercase()) {
            "HIGH" -> 0.35f to 900       // High compression = lower quality
            "MEDIUM" -> 0.60f to 1400    // Medium compression
            "LOW" -> 0.85f to 2000       // Low compression = high quality
            else -> 0.60f to 1400
        }
        pdfBoxManager.compressDocument(uri, outputStream, quality, maxDim)
        0L
    }

    override suspend fun protectPdf(
        uri: Uri,
        outputStream: OutputStream,
        userPass: String,
        ownerPass: String
    ) {
        pdfBoxManager.protectDocument(uri, outputStream, userPass, ownerPass)
    }

    override suspend fun convertPdfToImages(
        uri: Uri,
        outputDir: File,
        format: String, // "PNG" or "JPEG"
        dpi: Int,       // 72, 150, 300
        onProgress: (Int, Int) -> Unit
    ): List<File> = withContext(Dispatchers.IO) {
        val outputFiles = mutableListOf<File>()
        val totalPages = rendererManager.openDocument(uri)

        // Standard density base is 72 dpi (1.0x)
        val densityMultiplier = dpi / 72.0f
        val isPng = format.equals("PNG", ignoreCase = true)
        val ext = if (isPng) "png" else "jpg"
        val compressFormat = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

        try {
            for (p in 0 until totalPages) {
                val pageInfo = rendererManager.getPageInfo(p)
                val targetW = (pageInfo.width * densityMultiplier).toInt()
                val targetH = (pageInfo.height * densityMultiplier).toInt()

                val bitmap = rendererManager.renderPage(p, targetW, targetH)
                val file = File(outputDir, "page_${p + 1}.$ext")
                FileOutputStream(file).use { out ->
                    bitmap.compress(compressFormat, 90, out)
                }
                outputFiles.add(file)
                onProgress(p + 1, totalPages)
            }
        } finally {
            rendererManager.closeDocument()
        }
        outputFiles
    }

    override suspend fun convertImagesToPdf(
        uris: List<Uri>,
        outputStream: OutputStream,
        pageSize: String
    ) {
        pdfBoxManager.createPdfFromImages(uris, outputStream, pageSize)
    }

    override suspend fun extractAllText(uri: Uri, outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val text = pdfBoxManager.extractText(uri)
        outputStream.write(text.toByteArray(Charsets.UTF_8))
    }

    override suspend fun deletePages(
        uri: Uri,
        outputStream: OutputStream,
        pagesToDelete: Set<Int>
    ) {
        pdfBoxManager.deletePages(uri, outputStream, pagesToDelete)
    }

    override suspend fun reorderPages(
        uri: Uri,
        outputStream: OutputStream,
        newOrder: List<Int>
    ) {
        pdfBoxManager.reorderPages(uri, outputStream, newOrder)
    }
}
