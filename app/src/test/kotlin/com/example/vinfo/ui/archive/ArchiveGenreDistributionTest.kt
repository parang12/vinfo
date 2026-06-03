package com.example.vinfo.ui.archive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveGenreDistributionTest {
    @Test
    fun `buildArchiveGenreSegments calculates primary genre album percentages`() {
        val segments = buildArchiveGenreSegments(
            listOf(
                DummyArchive("1", "A", "Artist", listOf("Jazz", "Modal Jazz"), "2026.06.03"),
                DummyArchive("2", "B", "Artist", listOf("Jazz"), "2026.06.03"),
                DummyArchive("3", "C", "Artist", listOf("Electronic"), "2026.06.03"),
                DummyArchive("4", "D", "Artist", listOf("Trap"), "2026.06.03")
            )
        )

        assertEquals(
            listOf(
                ArchiveGenreSegment("Jazz", albumCount = 2, percent = 50),
                ArchiveGenreSegment("Electronic", albumCount = 1, percent = 25),
                ArchiveGenreSegment("Trap", albumCount = 1, percent = 25)
            ),
            segments
        )
    }

    @Test
    fun `buildArchiveGenreSegments groups smaller genres into other and keeps percent total at one hundred`() {
        val segments = buildArchiveGenreSegments(
            listOf(
                DummyArchive("1", "A", "Artist", listOf("Jazz"), "2026.06.03"),
                DummyArchive("2", "B", "Artist", listOf("Trap"), "2026.06.03"),
                DummyArchive("3", "C", "Artist", listOf("Electronic"), "2026.06.03"),
                DummyArchive("4", "D", "Artist", listOf("Soul"), "2026.06.03"),
                DummyArchive("5", "E", "Artist", listOf("Funk"), "2026.06.03")
            ),
            maxSegments = 4
        )

        assertEquals(listOf("Jazz", "Trap", "Electronic", "기타"), segments.map { it.genre })
        assertEquals(100, segments.sumOf { it.percent })
        assertEquals(2, segments.last().albumCount)
    }

    @Test
    fun `buildArchiveGenreSegments ignores unknown or blank primary genres`() {
        val segments = buildArchiveGenreSegments(
            listOf(
                DummyArchive("1", "A", "Artist", listOf("Unknown"), "2026.06.03"),
                DummyArchive("2", "B", "Artist", listOf(""), "2026.06.03"),
                DummyArchive("3", "C", "Artist", emptyList(), "2026.06.03")
            )
        )

        assertTrue(segments.isEmpty())
    }
}
