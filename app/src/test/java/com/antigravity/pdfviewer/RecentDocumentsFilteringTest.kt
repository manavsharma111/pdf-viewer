package com.antigravity.pdfviewer

import org.junit.Assert.assertEquals
import org.junit.Test

data class SimpleDocTestItem(val fileName: String, val pageCount: Int)

class RecentDocumentsFilteringTest {

    @Test
    fun testFilterRecentDocuments() {
        val docs = listOf(
            SimpleDocTestItem("DBMS Notes.pdf", 10),
            SimpleDocTestItem("Operating Systems Concepts.pdf", 50),
            SimpleDocTestItem("Antenna and Wave Propagation.pdf", 30),
            SimpleDocTestItem("Computer Networks.pdf", 25)
        )

        // Query "os" -> matches "Operating Systems Concepts.pdf"
        val filteredOs = filterDocs(docs, "os")
        assertEquals(1, filteredOs.size)
        assertEquals("Operating Systems Concepts.pdf", filteredOs[0].fileName)

        // Query "Net" -> matches "Computer Networks.pdf"
        val filteredNet = filterDocs(docs, "Net")
        assertEquals(1, filteredNet.size)
        assertEquals("Computer Networks.pdf", filteredNet[0].fileName)

        // Blank query -> returns all
        val filteredAll = filterDocs(docs, "")
        assertEquals(4, filteredAll.size)
    }

    private fun filterDocs(docs: List<SimpleDocTestItem>, query: String): List<SimpleDocTestItem> {
        if (query.isBlank()) return docs
        return docs.filter { it.fileName.contains(query, ignoreCase = true) }
    }
}
