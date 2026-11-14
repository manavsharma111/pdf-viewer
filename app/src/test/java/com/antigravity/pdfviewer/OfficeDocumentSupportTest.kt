package com.antigravity.pdfviewer

import com.antigravity.pdfviewer.data.pdf.OfficeDocumentType
import org.junit.Assert.assertEquals
import org.junit.Test

class OfficeDocumentSupportTest {

    @Test
    fun testOfficeDocumentTypeDetectionByExtension() {
        assertEquals(OfficeDocumentType.PDF, OfficeDocumentType.fromFileNameOrMime("report.pdf"))
        assertEquals(OfficeDocumentType.DOCX, OfficeDocumentType.fromFileNameOrMime("Assignment.docx"))
        assertEquals(OfficeDocumentType.DOC, OfficeDocumentType.fromFileNameOrMime("legacy_resume.doc"))
        assertEquals(OfficeDocumentType.XLSX, OfficeDocumentType.fromFileNameOrMime("financial_budget_2026.xlsx"))
        assertEquals(OfficeDocumentType.XLS, OfficeDocumentType.fromFileNameOrMime("old_grades.xls"))
        assertEquals(OfficeDocumentType.PPTX, OfficeDocumentType.fromFileNameOrMime("pitch_deck.pptx"))
        assertEquals(OfficeDocumentType.PPT, OfficeDocumentType.fromFileNameOrMime("lecture1.ppt"))
    }

    @Test
    fun testOfficeDocumentTypeDetectionByMime() {
        assertEquals(
            OfficeDocumentType.DOCX,
            OfficeDocumentType.fromFileNameOrMime("file", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        )
        assertEquals(
            OfficeDocumentType.XLSX,
            OfficeDocumentType.fromFileNameOrMime("file", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        )
        assertEquals(
            OfficeDocumentType.PPTX,
            OfficeDocumentType.fromFileNameOrMime("file", "application/vnd.openxmlformats-officedocument.presentationml.presentation")
        )
    }

    @Test
    fun testCaseInsensitiveExtension() {
        assertEquals(OfficeDocumentType.XLSX, OfficeDocumentType.fromFileNameOrMime("DATA.XLSX"))
        assertEquals(OfficeDocumentType.DOCX, OfficeDocumentType.fromFileNameOrMime("Notes.DOCX"))
        assertEquals(OfficeDocumentType.PPTX, OfficeDocumentType.fromFileNameOrMime("SLIDES.PPTX"))
        assertEquals(OfficeDocumentType.PDF, OfficeDocumentType.fromFileNameOrMime("BOOK.PDF"))
    }
}
