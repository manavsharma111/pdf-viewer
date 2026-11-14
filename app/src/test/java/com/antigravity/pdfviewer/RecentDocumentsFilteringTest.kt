package com.antigravity.pdfviewer

import android.net.Uri
import com.antigravity.pdfviewer.domain.model.PdfDocumentInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentDocumentsFilteringTest {

    @Test
    fun testFilterRecentDocuments() {
        // Mock list of documents
        val docs = listOf(
            createDoc("DBMS Notes.pdf"),
            createDoc("Operating Systems Concepts.pdf"),
            createDoc("Antenna and Wave Propagation.pdf"),
            createDoc("Computer Networks.pdf")
        )

        // Query "os" -> should match "Operating Systems Concepts.pdf"
        val filteredOs = filterDocs(docs, "os")
        assertEquals(1, filteredOs.size)
        assertEquals("Operating Systems Concepts.pdf", filteredOs[0].fileName)

        // Query "Net" -> should match "Computer Networks.pdf"
        val filteredNet = filterDocs(docs, "Net")
        assertEquals(1, filteredNet.size)
        assertEquals("Computer Networks.pdf", filteredNet[0].fileName)

        // Blank query -> returns all
        val filteredAll = filterDocs(docs, "")
        assertEquals(4, filteredAll.size)
    }

    private fun filterDocs(docs: List<PdfDocumentInfo>, query: String): List<PdfDocumentInfo> {
        if (query.isBlank()) return docs
        return docs.filter { it.fileName.contains(query, ignoreCase = true) }
    }

    private fun createDoc(name: String): PdfDocumentInfo {
        return PdfDocumentInfo(
            uri = Uri.parse("content://docs/$name"),
            fileName = name,
            fileSize = 1024L,
            pageCount = 10
        )
    }
}
