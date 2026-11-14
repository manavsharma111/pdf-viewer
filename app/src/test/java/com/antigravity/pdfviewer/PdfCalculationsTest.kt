package com.antigravity.pdfviewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfCalculationsTest {

    @Test
    fun testParseRangesValid() {
        val input = "1-5, 8, 10-12"
        val ranges = parseRanges(input)

        assertEquals(3, ranges.size)
        assertEquals(1..5, ranges[0])
        assertEquals(8..8, ranges[1])
        assertEquals(10..12, ranges[2])
    }

    @Test
    fun testParseRangesWithWhitespace() {
        val input = " 2 - 4 ,  7  "
        val ranges = parseRanges(input)

        assertEquals(2, ranges.size)
        assertEquals(2..4, ranges[0])
        assertEquals(7..7, ranges[1])
    }

    @Test
    fun testDoublePageRowCount() {
        // Even count: 10 pages -> 5 rows
        assertEquals(5, calculateDoublePageRows(10))

        // Odd count: 11 pages -> 6 rows
        assertEquals(6, calculateDoublePageRows(11))

        // 1 page -> 1 row
        assertEquals(1, calculateDoublePageRows(1))

        // 0 pages -> 0 rows
        assertEquals(0, calculateDoublePageRows(0))
    }

    @Test
    fun testDoublePagePairs() {
        // Row 0 -> Page 0, 1
        val (p0, p1) = getDoublePagePair(rowIndex = 0, totalPages = 5)
        assertEquals(0, p0)
        assertEquals(1, p1)

        // Row 2 (pages 4, 5) when totalPages is 5 -> p1 should be null
        val (p4, p5) = getDoublePagePair(rowIndex = 2, totalPages = 5)
        assertEquals(4, p4)
        assertEquals(null, p5)
    }

    @Test
    fun testSearchQueryRegexCaseInsensitive() {
        val query = "Android"
        val regex = Regex(Regex.escape(query), RegexOption.IGNORE_CASE)

        val text = "Developing android applications with Jetpack Compose."
        val matches = regex.findAll(text).toList()

        assertEquals(1, matches.size)
        assertEquals("android", matches[0].value)
    }

    @Test
    fun testSearchQueryRegexWholeWord() {
        val query = "cat"
        val regex = Regex("\\b${Regex.escape(query)}\\b", RegexOption.IGNORE_CASE)

        val text = "The cat cataloged the category."
        val matches = regex.findAll(text).toList()

        // Only "cat" should match, not "cataloged" or "category"
        assertEquals(1, matches.size)
        assertEquals(4, matches[0].range.first)
    }

    @Test
    fun testZoomClamp() {
        val minScale = 1.0f
        val maxScale = 5.0f

        val zoomedOut = (0.5f).coerceIn(minScale, maxScale)
        assertEquals(1.0f, zoomedOut, 0.001f)

        val zoomedIn = (7.5f).coerceIn(minScale, maxScale)
        assertEquals(5.0f, zoomedIn, 0.001f)

        val normal = (2.2f).coerceIn(minScale, maxScale)
        assertEquals(2.2f, normal, 0.001f)
    }

    private fun parseRanges(input: String): List<IntRange> {
        return input.split(",").mapNotNull { part ->
            val trimmed = part.trim()
            if (trimmed.contains("-")) {
                val tokens = trimmed.split("-")
                val start = tokens.getOrNull(0)?.trim()?.toIntOrNull()
                val end = tokens.getOrNull(1)?.trim()?.toIntOrNull()
                if (start != null && end != null && start <= end) start..end else null
            } else {
                val single = trimmed.toIntOrNull()
                if (single != null) single..single else null
            }
        }
    }

    private fun calculateDoublePageRows(totalPages: Int): Int {
        if (totalPages <= 0) return 0
        return (totalPages + 1) / 2
    }

    private fun getDoublePagePair(rowIndex: Int, totalPages: Int): Pair<Int, Int?> {
        val p1 = rowIndex * 2
        val p2 = p1 + 1
        return p1 to if (p2 < totalPages) p2 else null
    }
}
