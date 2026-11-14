package com.antigravity.pdfviewer.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.antigravity.pdfviewer.domain.model.AnnotationType
import com.antigravity.pdfviewer.domain.model.DrawingStroke
import com.antigravity.pdfviewer.domain.model.HighlightAnnotation
import com.antigravity.pdfviewer.domain.model.PageAnnotations
import com.antigravity.pdfviewer.domain.model.PdfMetadata
import com.antigravity.pdfviewer.domain.model.SearchResult
import com.antigravity.pdfviewer.domain.model.TextAnnotation
import com.antigravity.pdfviewer.util.UriHelper
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class PdfBoxManager(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    suspend fun getDocumentMetadata(uri: Uri, password: String? = null): PdfMetadata = withContext(Dispatchers.IO) {
        openDocument(uri, password).use { doc ->
            val info = doc.documentInformation
            val version = doc.version.toString()
            val fileSize = UriHelper.queryFileSize(context, uri)

            PdfMetadata(
                title = info?.title,
                author = info?.author,
                subject = info?.subject,
                keywords = info?.keywords,
                creator = info?.creator,
                producer = info?.producer,
                creationDate = info?.creationDate?.time?.let { dateFormat.format(it) },
                modificationDate = info?.modificationDate?.time?.let { dateFormat.format(it) },
                version = version,
                pageCount = doc.numberOfPages,
                fileSize = fileSize
            )
        }
    }

    suspend fun isDocumentEncrypted(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            UriHelper.openInputStream(context, uri)?.use { stream ->
                PDDocument.load(stream).use { doc ->
                    doc.isEncrypted
                }
            } ?: false
        } catch (e: Exception) {
            // Password-required exception indicates encryption
            e.message?.contains("password", ignoreCase = true) == true ||
                    e.javaClass.simpleName.contains("InvalidPassword", ignoreCase = true)
        }
    }

    private fun openDocument(uri: Uri, password: String? = null): PDDocument {
        val inputStream: InputStream = UriHelper.openInputStream(context, uri)
            ?: throw IllegalArgumentException("Cannot open stream for URI: $uri")
        return if (!password.isNullOrEmpty()) {
            PDDocument.load(inputStream, password)
        } else {
            PDDocument.load(inputStream)
        }
    }

    suspend fun extractText(
        uri: Uri,
        startPage: Int? = null,
        endPage: Int? = null,
        password: String? = null
    ): String = withContext(Dispatchers.IO) {
        openDocument(uri, password).use { doc ->
            val stripper = PDFTextStripper()
            stripper.sortByPosition = true
            if (startPage != null) stripper.startPage = startPage + 1
            if (endPage != null) stripper.endPage = endPage + 1
            stripper.getText(doc)
        }
    }

    suspend fun searchText(
        uri: Uri,
        query: String,
        isCaseSensitive: Boolean = false,
        isWholeWord: Boolean = false,
        password: String? = null
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val results = mutableListOf<SearchResult>()
        openDocument(uri, password).use { doc ->
            val stripper = PDFTextStripper()
            stripper.sortByPosition = true
            val pageCount = doc.numberOfPages

            val regexPattern = if (isWholeWord) {
                "\\b${Regex.escape(query)}\\b"
            } else {
                Regex.escape(query)
            }
            val regex = if (isCaseSensitive) {
                Regex(regexPattern)
            } else {
                Regex(regexPattern, RegexOption.IGNORE_CASE)
            }

            for (p in 1..pageCount) {
                stripper.startPage = p
                stripper.endPage = p
                val pageText = stripper.getText(doc)

                regex.findAll(pageText).forEach { match ->
                    val startIndex = (match.range.first - 30).coerceAtLeast(0)
                    val endIndex = (match.range.last + 30).coerceAtMost(pageText.length)
                    val snippet = pageText.substring(startIndex, endIndex).replace("\n", " ").trim()

                    results.add(
                        SearchResult(
                            pageIndex = p - 1,
                            matchText = match.value,
                            snippet = "…$snippet…",
                            charOffset = match.range.first
                        )
                    )
                }
            }
        }
        results
    }

    suspend fun rotatePages(
        sourceUri: Uri,
        outputStream: OutputStream,
        pageIndices: Set<Int>?, // null means all pages
        degrees: Int,
        password: String? = null
    ) = withContext(Dispatchers.IO) {
        openDocument(sourceUri, password).use { doc ->
            val totalPages = doc.numberOfPages
            for (i in 0 until totalPages) {
                if (pageIndices == null || pageIndices.contains(i)) {
                    val page = doc.getPage(i)
                    val newRotation = (page.rotation + degrees + 360) % 360
                    page.rotation = newRotation
                }
            }
            doc.save(outputStream)
        }
    }

    suspend fun deletePages(
        sourceUri: Uri,
        outputStream: OutputStream,
        pagesToDelete: Set<Int>,
        password: String? = null
    ) = withContext(Dispatchers.IO) {
        openDocument(sourceUri, password).use { doc ->
            val sortedPages = pagesToDelete.sortedDescending()
            for (p in sortedPages) {
                if (p in 0 until doc.numberOfPages) {
                    doc.removePage(p)
                }
            }
            doc.save(outputStream)
        }
    }

    suspend fun extractPages(
        sourceUri: Uri,
        outputStream: OutputStream,
        pagesToExtract: List<Int>,
        password: String? = null
    ) = withContext(Dispatchers.IO) {
        openDocument(sourceUri, password).use { sourceDoc ->
            PDDocument().use { newDoc ->
                for (pageIndex in pagesToExtract) {
                    if (pageIndex in 0 until sourceDoc.numberOfPages) {
                        newDoc.addPage(sourceDoc.getPage(pageIndex))
                    }
                }
                newDoc.save(outputStream)
            }
        }
    }

    suspend fun reorderPages(
        sourceUri: Uri,
        outputStream: OutputStream,
        newOrder: List<Int>,
        password: String? = null
    ) = withContext(Dispatchers.IO) {
        openDocument(sourceUri, password).use { sourceDoc ->
            PDDocument().use { newDoc ->
                for (pageIndex in newOrder) {
                    if (pageIndex in 0 until sourceDoc.numberOfPages) {
                        newDoc.addPage(sourceDoc.getPage(pageIndex))
                    }
                }
                newDoc.save(outputStream)
            }
        }
    }

    suspend fun mergeDocuments(
        sourceUris: List<Uri>,
        outputStream: OutputStream,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        val merger = PDFMergerUtility()
        merger.destinationStream = outputStream

        val tempFiles = mutableListOf<File>()
        try {
            sourceUris.forEachIndexed { index, uri ->
                val temp = File(context.cacheDir, "merge_src_${System.currentTimeMillis()}_$index.pdf")
                tempFiles.add(temp)
                UriHelper.openInputStream(context, uri)?.use { input ->
                    FileOutputStream(temp).use { output ->
                        input.copyTo(output)
                    }
                }
                merger.addSource(temp)
                onProgress(index + 1, sourceUris.size)
            }
            merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
        } finally {
            tempFiles.forEach { it.delete() }
        }
    }

    suspend fun splitDocument(
        sourceUri: Uri,
        splitAtPages: Int,
        outputDir: File,
        baseName: String,
        password: String? = null
    ): List<File> = withContext(Dispatchers.IO) {
        val outputFiles = mutableListOf<File>()
        openDocument(sourceUri, password).use { doc ->
            val splitter = Splitter()
            splitter.setSplitAtPage(splitAtPages)
            val parts = splitter.split(doc)

            parts.forEachIndexed { index, partDoc ->
                val outFile = File(outputDir, "${baseName}_part_${index + 1}.pdf")
                partDoc.use { part ->
                    part.save(outFile)
                }
                outputFiles.add(outFile)
            }
        }
        outputFiles
    }

    suspend fun protectDocument(
        sourceUri: Uri,
        outputStream: OutputStream,
        userPassword: String,
        ownerPassword: String = userPassword,
        keyLength: Int = 128
    ) = withContext(Dispatchers.IO) {
        openDocument(sourceUri).use { doc ->
            val permission = AccessPermission()
            val policy = StandardProtectionPolicy(ownerPassword, userPassword, permission)
            policy.encryptionKeyLength = keyLength
            doc.protect(policy)
            doc.save(outputStream)
        }
    }

    suspend fun compressDocument(
        sourceUri: Uri,
        outputStream: OutputStream,
        jpegQuality: Float = 0.6f, // 0.3f (high), 0.6f (medium), 0.85f (low)
        maxImageDimension: Int = 1200,
        password: String? = null
    ) = withContext(Dispatchers.IO) {
        openDocument(sourceUri, password).use { doc ->
            for (p in 0 until doc.numberOfPages) {
                val page = doc.getPage(p)
                val resources = page.resources ?: continue

                for (name in resources.xObjectNames) {
                    if (resources.isImageXObject(name)) {
                        val imageXObject = resources.getXObject(name) as? PDImageXObject ?: continue
                        try {
                            val bitmap = imageXObject.image ?: continue
                            val (scaledWidth, scaledHeight) = calculateScaledDimensions(
                                bitmap.width,
                                bitmap.height,
                                maxImageDimension
                            )

                            val scaledBitmap = if (scaledWidth < bitmap.width || scaledHeight < bitmap.height) {
                                Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
                            } else {
                                bitmap
                            }

                            val compressedImage = JPEGFactory.createFromImage(
                                doc,
                                scaledBitmap,
                                jpegQuality
                            )
                            resources.put(name, compressedImage)
                        } catch (e: Exception) {
                            // Skip images that cannot be re-encoded
                        }
                    }
                }
            }
            doc.save(outputStream)
        }
    }

    private fun calculateScaledDimensions(w: Int, h: Int, maxDim: Int): Pair<Int, Int> {
        if (w <= maxDim && h <= maxDim) return w to h
        val ratio = w.toFloat() / h.toFloat()
        return if (ratio > 1f) {
            maxDim to (maxDim / ratio).toInt()
        } else {
            (maxDim * ratio).toInt() to maxDim
        }
    }

    suspend fun createPdfFromImages(
        imageUris: List<Uri>,
        outputStream: OutputStream,
        pageSize: String = "A4", // A4, LETTER, ORIGINAL
        rotation: Int = 0
    ) = withContext(Dispatchers.IO) {
        PDDocument().use { doc ->
            for (uri in imageUris) {
                // Decode bounds first to prevent OutOfMemory on modern 50MP/108MP phone photos
                val bitmap = decodeSampledBitmap(context, uri) ?: continue

                val pageRect = when (pageSize.uppercase()) {
                    "LETTER" -> PDRectangle.LETTER
                    "ORIGINAL" -> PDRectangle(bitmap.width.toFloat(), bitmap.height.toFloat())
                    else -> PDRectangle.A4
                }

                val page = PDPage(pageRect)
                page.rotation = rotation
                doc.addPage(page)

                val pdImage = JPEGFactory.createFromImage(doc, bitmap, 0.85f)
                PDPageContentStream(doc, page).use { contentStream ->
                    val pageWidth = pageRect.width
                    val pageHeight = pageRect.height

                    // Fit within page while keeping aspect ratio
                    val imgRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val pageRatio = pageWidth / pageHeight

                    val (drawWidth, drawHeight) = if (imgRatio > pageRatio) {
                        pageWidth to (pageWidth / imgRatio)
                    } else {
                        (pageHeight * imgRatio) to pageHeight
                    }

                    val x = (pageWidth - drawWidth) / 2f
                    val y = (pageHeight - drawHeight) / 2f

                    contentStream.drawImage(pdImage, x, y, drawWidth, drawHeight)
                }
            }
            doc.save(outputStream)
        }
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, maxDim: Int = 2400): Bitmap? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            UriHelper.openInputStream(context, uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val origW = bounds.outWidth
            val origH = bounds.outHeight
            if (origW <= 0 || origH <= 0) return null

            var sampleSize = 1
            while ((origW / sampleSize) > maxDim || (origH / sampleSize) > maxDim) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            UriHelper.openInputStream(context, uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            }
        }.getOrNull()
    }

    suspend fun burnAnnotationsIntoPdf(
        sourceUri: Uri,
        outputStream: OutputStream,
        annotationsMap: Map<Int, PageAnnotations>,
        password: String? = null
    ) = withContext(Dispatchers.IO) {
        openDocument(sourceUri, password).use { doc ->
            for ((pageIndex, annotations) in annotationsMap) {
                if (pageIndex !in 0 until doc.numberOfPages) continue
                val page = doc.getPage(pageIndex)
                val mediaBox = page.mediaBox
                val pageWidth = mediaBox.width
                val pageHeight = mediaBox.height

                PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                    // 1. Draw Highlights
                    for (hl in annotations.highlights) {
                        val color = hl.color
                        val r = Color.red(color) / 255f
                        val g = Color.green(color) / 255f
                        val b = Color.blue(color) / 255f

                        cs.setNonStrokingColor(r, g, b)
                        cs.setStrokingColor(r, g, b)
                        cs.setLineWidth(hl.strokeWidth)

                        val x1 = hl.start.x * pageWidth
                        val y1 = (1f - hl.start.y) * pageHeight
                        val x2 = hl.end.x * pageWidth
                        val y2 = (1f - hl.end.y) * pageHeight

                        cs.moveTo(x1, y1)
                        cs.lineTo(x2, y2)
                        cs.stroke()
                    }

                    // 2. Draw Freehand Strokes
                    for (stroke in annotations.strokes) {
                        if (stroke.isEraser || stroke.points.size < 2) continue

                        val color = stroke.color
                        val r = Color.red(color) / 255f
                        val g = Color.green(color) / 255f
                        val b = Color.blue(color) / 255f

                        cs.setStrokingColor(r, g, b)
                        cs.setLineWidth(stroke.strokeWidth)

                        val startP = stroke.points.first()
                        cs.moveTo(startP.x * pageWidth, (1f - startP.y) * pageHeight)

                        for (i in 1 until stroke.points.size) {
                            val p = stroke.points[i]
                            cs.lineTo(p.x * pageWidth, (1f - p.y) * pageHeight)
                        }
                        cs.stroke()
                    }

                    // 3. Draw Text Annotations
                    for (textNote in annotations.textNotes) {
                        val color = textNote.color
                        val r = Color.red(color) / 255f
                        val g = Color.green(color) / 255f
                        val b = Color.blue(color) / 255f

                        val x = textNote.position.x * pageWidth
                        val y = (1f - textNote.position.y) * pageHeight

                        cs.beginText()
                        cs.setFont(PDType1Font.HELVETICA_BOLD, textNote.fontSize)
                        cs.setNonStrokingColor(r, g, b)
                        cs.newLineAtOffset(x, y)

                        val lines = textNote.text.split("\n")
                        for ((lineIdx, line) in lines.withIndex()) {
                            if (lineIdx > 0) {
                                cs.newLineAtOffset(0f, -(textNote.fontSize + 4f))
                            }
                            cs.showText(line.replace(Regex("[^\\x20-\\x7E]"), " "))
                        }
                        cs.endText()
                    }
                }
            }
            doc.save(outputStream)
        }
    }
}
