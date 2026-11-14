package com.antigravity.pdfviewer.data.pdf

import android.content.Context
import android.graphics.Color
import android.net.Uri
import com.antigravity.pdfviewer.util.UriHelper
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

enum class OfficeDocumentType {
    PDF,
    DOCX,
    DOC,
    XLSX,
    XLS,
    PPTX,
    PPT,
    UNKNOWN;

    companion object {
        fun fromFileNameOrMime(name: String, mime: String? = null): OfficeDocumentType {
            val lowerName = name.lowercase()
            return when {
                lowerName.endsWith(".pdf") || mime == "application/pdf" -> PDF
                lowerName.endsWith(".docx") || mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> DOCX
                lowerName.endsWith(".doc") || mime == "application/msword" -> DOC
                lowerName.endsWith(".xlsx") || mime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> XLSX
                lowerName.endsWith(".xls") || mime == "application/vnd.ms-excel" -> XLS
                lowerName.endsWith(".pptx") || mime == "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> PPTX
                lowerName.endsWith(".ppt") || mime == "application/vnd.ms-powerpoint" -> PPT
                else -> UNKNOWN
            }
        }
    }
}

class OfficeToPdfConverter(private val context: Context) {

    private val parserFactory = XmlPullParserFactory.newInstance().apply {
        isNamespaceAware = true
    }

    suspend fun convertToPdf(sourceUri: Uri, type: OfficeDocumentType): File = withContext(Dispatchers.IO) {
        val cacheFile = File(
            context.cacheDir,
            "converted_${System.currentTimeMillis()}_${type.name.lowercase()}.pdf"
        )

        when (type) {
            OfficeDocumentType.DOCX, OfficeDocumentType.DOC -> {
                convertDocxToPdf(sourceUri, cacheFile)
            }
            OfficeDocumentType.XLSX, OfficeDocumentType.XLS -> {
                convertXlsxToPdf(sourceUri, cacheFile)
            }
            OfficeDocumentType.PPTX, OfficeDocumentType.PPT -> {
                convertPptxToPdf(sourceUri, cacheFile)
            }
            else -> {
                // If it's already a PDF, copy directly
                UriHelper.openInputStream(context, sourceUri)?.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
        cacheFile
    }

    // --- DOCX CONVERSION ---
    private fun convertDocxToPdf(sourceUri: Uri, outputFile: File) {
        val paragraphs = mutableListOf<String>()

        UriHelper.openInputStream(context, sourceUri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        val xmlContent = zip.bufferedReader(Charsets.UTF_8).readText()
                        paragraphs.addAll(parseDocxXml(xmlContent))
                        break
                    }
                    entry = zip.nextEntry
                }
            }
        }

        if (paragraphs.isEmpty()) {
            paragraphs.add("Empty Document")
        }

        // Render paragraphs into A4 PDF pages with word wrap and pagination
        PDDocument().use { doc ->
            val pageWidth = PDRectangle.A4.width
            val pageHeight = PDRectangle.A4.height
            val margin = 50f
            val printableWidth = pageWidth - (margin * 2)
            val fontSize = 12f
            val leading = 16f

            var currentPage = PDPage(PDRectangle.A4)
            doc.addPage(currentPage)
            var contentStream = PDPageContentStream(doc, currentPage)
            var currentY = pageHeight - margin

            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, fontSize)
            contentStream.setNonStrokingColor(0.1f, 0.1f, 0.1f)
            contentStream.newLineAtOffset(margin, currentY)

            for (paragraph in paragraphs) {
                val lines = wrapText(paragraph, printableWidth, fontSize)
                for (line in lines) {
                    if (currentY - leading < margin) {
                        // Create next page
                        contentStream.endText()
                        contentStream.close()

                        currentPage = PDPage(PDRectangle.A4)
                        doc.addPage(currentPage)
                        contentStream = PDPageContentStream(doc, currentPage)
                        currentY = pageHeight - margin

                        contentStream.beginText()
                        contentStream.setFont(PDType1Font.HELVETICA, fontSize)
                        contentStream.setNonStrokingColor(0.1f, 0.1f, 0.1f)
                        contentStream.newLineAtOffset(margin, currentY)
                    }

                    val sanitizedLine = sanitizeForPdf(line)
                    contentStream.showText(sanitizedLine)
                    contentStream.newLineAtOffset(0f, -leading)
                    currentY -= leading
                }
                // Extra space between paragraphs
                currentY -= leading * 0.5f
                contentStream.newLineAtOffset(0f, -(leading * 0.5f))
            }

            contentStream.endText()
            contentStream.close()
            doc.save(outputFile)
        }
    }

    private fun parseDocxXml(xml: String): List<String> {
        val result = mutableListOf<String>()
        val parser = parserFactory.newPullParser()
        parser.setInput(StringReader(xml))

        val currentParagraph = StringBuilder()
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "p") {
                        currentParagraph.setLength(0)
                    }
                }
                XmlPullParser.TEXT -> {
                    currentParagraph.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "p") {
                        val text = currentParagraph.toString().trim()
                        if (text.isNotEmpty()) {
                            result.add(text)
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return result
    }

    // --- XLSX CONVERSION ---
    private fun convertXlsxToPdf(sourceUri: Uri, outputFile: File) {
        val sharedStrings = mutableListOf<String>()
        val sheetRows = mutableListOf<List<String>>()

        // Step 1: Extract shared strings & first sheet
        UriHelper.openInputStream(context, sourceUri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                var sharedStringsXml: String? = null
                var sheetXml: String? = null

                while (entry != null) {
                    when (entry.name) {
                        "xl/sharedStrings.xml" -> {
                            sharedStringsXml = zip.bufferedReader(Charsets.UTF_8).readText()
                        }
                        "xl/worksheets/sheet1.xml" -> {
                            sheetXml = zip.bufferedReader(Charsets.UTF_8).readText()
                        }
                    }
                    entry = zip.nextEntry
                }

                if (sharedStringsXml != null) {
                    sharedStrings.addAll(parseXlsxSharedStrings(sharedStringsXml))
                }
                if (sheetXml != null) {
                    sheetRows.addAll(parseXlsxSheet(sheetXml, sharedStrings))
                }
            }
        }

        // Render sheet table in landscape A4
        PDDocument().use { doc ->
            val landscapeA4 = PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width)
            val pageWidth = landscapeA4.width
            val pageHeight = landscapeA4.height
            val margin = 40f
            val rowHeight = 24f
            val maxRowsPerPage = ((pageHeight - (margin * 2)) / rowHeight).toInt()

            val maxCols = sheetRows.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1
            val colWidth = ((pageWidth - (margin * 2)) / maxCols.coerceAtMost(10)).coerceAtLeast(60f)

            var rowIndex = 0
            while (rowIndex < sheetRows.size || rowIndex == 0) {
                val page = PDPage(landscapeA4)
                doc.addPage(page)

                PDPageContentStream(doc, page).use { cs ->
                    var y = pageHeight - margin
                    val rowsOnThisPage = (sheetRows.size - rowIndex).coerceAtMost(maxRowsPerPage)

                    for (r in 0 until rowsOnThisPage) {
                        val actualRowIdx = rowIndex + r
                        val rowData = if (actualRowIdx < sheetRows.size) sheetRows[actualRowIdx] else emptyList()

                        // Draw row border
                        cs.setStrokingColor(0.8f, 0.8f, 0.8f)
                        cs.setLineWidth(0.5f)
                        cs.moveTo(margin, y)
                        cs.lineTo(margin + (maxCols.coerceAtMost(10) * colWidth), y)
                        cs.stroke()

                        // Draw cells
                        for (c in 0 until maxCols.coerceAtMost(10)) {
                            val text = rowData.getOrNull(c) ?: ""
                            val cellX = margin + (c * colWidth)

                            cs.beginText()
                            cs.setFont(if (actualRowIdx == 0) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA, 10f)
                            cs.setNonStrokingColor(0.1f, 0.1f, 0.1f)
                            cs.newLineAtOffset(cellX + 4f, y - 16f)

                            val truncated = text.take(15)
                            cs.showText(sanitizeForPdf(truncated))
                            cs.endText()
                        }
                        y -= rowHeight
                    }
                }
                rowIndex += maxRowsPerPage
                if (sheetRows.isEmpty()) break
            }
            doc.save(outputFile)
        }
    }

    private fun parseXlsxSharedStrings(xml: String): List<String> {
        val list = mutableListOf<String>()
        val parser = parserFactory.newPullParser()
        parser.setInput(StringReader(xml))

        var currentText = StringBuilder()
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "t") {
                        currentText.setLength(0)
                    }
                }
                XmlPullParser.TEXT -> {
                    currentText.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "t") {
                        list.add(currentText.toString())
                    }
                }
            }
            eventType = parser.next()
        }
        return list
    }

    private fun parseXlsxSheet(xml: String, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val parser = parserFactory.newPullParser()
        parser.setInput(StringReader(xml))

        val currentRow = mutableListOf<String>()
        var cellType = ""
        var cellValue = ""
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> currentRow.clear()
                        "c" -> {
                            cellType = parser.getAttributeValue(null, "t") ?: ""
                            cellValue = ""
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    cellValue = parser.text
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> {
                            val resolved = if (cellType == "s") {
                                val sIndex = cellValue.toIntOrNull()
                                if (sIndex != null && sIndex in sharedStrings.indices) {
                                    sharedStrings[sIndex]
                                } else cellValue
                            } else {
                                cellValue
                            }
                            currentRow.add(resolved)
                        }
                        "row" -> {
                            if (currentRow.isNotEmpty()) {
                                rows.add(currentRow.toList())
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }

    // --- PPTX CONVERSION ---
    private fun convertPptxToPdf(sourceUri: Uri, outputFile: File) {
        val slidesText = mutableListOf<List<String>>()

        UriHelper.openInputStream(context, sourceUri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    if (entry.name.startsWith("ppt/slides/slide") && entry.name.endsWith(".xml")) {
                        val xml = zip.bufferedReader(Charsets.UTF_8).readText()
                        slidesText.add(parsePptxSlide(xml))
                    }
                    entry = zip.nextEntry
                }
            }
        }

        if (slidesText.isEmpty()) {
            slidesText.add(listOf("Slide 1", "Empty Presentation"))
        }

        // Render 16:9 Presentation Slides
        PDDocument().use { doc ->
            val slideWidth = 842f
            val slideHeight = 595f
            val slideRect = PDRectangle(slideWidth, slideHeight)

            for ((index, slideLines) in slidesText.withIndex()) {
                val page = PDPage(slideRect)
                doc.addPage(page)

                PDPageContentStream(doc, page).use { cs ->
                    // Slide header background banner
                    cs.setNonStrokingColor(0.12f, 0.44f, 0.78f) // Royal blue accent
                    cs.addRect(0f, slideHeight - 70f, slideWidth, 70f)
                    cs.fill()

                    // Slide Title
                    val title = slideLines.firstOrNull() ?: "Slide ${index + 1}"
                    cs.beginText()
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 22f)
                    cs.setNonStrokingColor(1f, 1f, 1f)
                    cs.newLineAtOffset(40f, slideHeight - 45f)
                    cs.showText(sanitizeForPdf(title.take(50)))
                    cs.endText()

                    // Slide Body Bullets
                    var y = slideHeight - 120f
                    val bodyLines = if (slideLines.size > 1) slideLines.drop(1) else listOf("(No text content on this slide)")

                    for (bodyLine in bodyLines) {
                        cs.beginText()
                        cs.setFont(PDType1Font.HELVETICA, 16f)
                        cs.setNonStrokingColor(0.2f, 0.2f, 0.2f)
                        cs.newLineAtOffset(60f, y)
                        cs.showText("• " + sanitizeForPdf(bodyLine.take(75)))
                        cs.endText()
                        y -= 30f
                        if (y < 60f) break
                    }

                    // Slide number footer
                    cs.beginText()
                    cs.setFont(PDType1Font.HELVETICA, 12f)
                    cs.setNonStrokingColor(0.5f, 0.5f, 0.5f)
                    cs.newLineAtOffset(slideWidth - 80f, 30f)
                    cs.showText("${index + 1} / ${slidesText.size}")
                    cs.endText()
                }
            }
            doc.save(outputFile)
        }
    }

    private fun parsePptxSlide(xml: String): List<String> {
        val lines = mutableListOf<String>()
        val parser = parserFactory.newPullParser()
        parser.setInput(StringReader(xml))

        val currentLine = StringBuilder()
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "p") {
                        currentLine.setLength(0)
                    }
                }
                XmlPullParser.TEXT -> {
                    currentLine.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "p") {
                        val text = currentLine.toString().trim()
                        if (text.isNotEmpty()) {
                            lines.add(text)
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return lines
    }

    private fun wrapText(text: String, maxWidth: Float, fontSize: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()
        val approxCharWidth = fontSize * 0.5f

        for (word in words) {
            val prospectiveLength = (currentLine.length + word.length + 1) * approxCharWidth
            if (prospectiveLength > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            } else {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    private fun sanitizeForPdf(text: String): String {
        return text.replace(Regex("[^\\x20-\\x7E]"), " ")
    }
}
