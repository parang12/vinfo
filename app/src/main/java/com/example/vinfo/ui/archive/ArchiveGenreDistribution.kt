package com.example.vinfo.ui.archive

import kotlin.math.floor

data class ArchiveGenreSegment(
    val genre: String,
    val albumCount: Int,
    val percent: Int
)

fun buildArchiveGenreSegments(
    archiveItems: List<DummyArchive>,
    maxSegments: Int = 4
): List<ArchiveGenreSegment> {
    val primaryGenres = archiveItems.mapNotNull { item ->
        item.genres
                .firstOrNull()
                ?.trim()
                ?.takeIf { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) }
    }
    val firstIndexes = primaryGenres
        .mapIndexed { index, genre -> genre to index }
        .distinctBy { it.first }
        .toMap()
    val genreCounts = primaryGenres
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }
                .thenBy { firstIndexes[it.key] ?: Int.MAX_VALUE }
        )

    if (genreCounts.isEmpty()) return emptyList()

    val visibleCount = maxSegments.coerceAtLeast(2)
    val groupedCounts = if (genreCounts.size > visibleCount) {
        val head = genreCounts.take(visibleCount - 1).map { it.key to it.value }
        val otherCount = genreCounts.drop(visibleCount - 1).sumOf { it.value }
        head + ("기타" to otherCount)
    } else {
        genreCounts.map { it.key to it.value }
    }

    return assignPercentages(groupedCounts)
}

private fun assignPercentages(counts: List<Pair<String, Int>>): List<ArchiveGenreSegment> {
    val total = counts.sumOf { it.second }
    if (total <= 0) return emptyList()

    val raw = counts.map { (genre, count) ->
        val exact = count * 100.0 / total
        val base = floor(exact).toInt()
        PercentDraft(
            genre = genre,
            albumCount = count,
            basePercent = base,
            remainder = exact - base
        )
    }
    val missingPercent = 100 - raw.sumOf { it.basePercent }
    val bonusGenres = raw
        .sortedWith(compareByDescending<PercentDraft> { it.remainder }.thenBy { it.genre })
        .take(missingPercent)
        .map { it.genre }
        .toSet()

    return raw.map { draft ->
        ArchiveGenreSegment(
            genre = draft.genre,
            albumCount = draft.albumCount,
            percent = draft.basePercent + if (draft.genre in bonusGenres) 1 else 0
        )
    }
}

private data class PercentDraft(
    val genre: String,
    val albumCount: Int,
    val basePercent: Int,
    val remainder: Double
)
