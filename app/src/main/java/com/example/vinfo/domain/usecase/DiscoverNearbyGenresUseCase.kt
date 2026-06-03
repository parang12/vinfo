package com.example.vinfo.domain.usecase

import com.example.vinfo.domain.model.GenreRelationCandidate

class DiscoverNearbyGenresUseCase {
    operator fun invoke(
        sourceGenre: String,
        candidates: List<GenreRelationCandidate>
    ): List<GenreRelationCandidate> {
        val sourceKey = sourceGenre.normalizedGenreKey()
        return candidates
            .filter { it.genreName.isNotBlank() }
            .filterNot { it.genreName.equals("unknown", ignoreCase = true) }
            .filterNot { it.genreName.normalizedGenreKey() == sourceKey }
            .filter { it.score >= MINIMUM_RELATION_SCORE }
            .groupBy { it.genreName.normalizedGenreKey() }
            .values
            .mapNotNull { duplicates -> duplicates.maxByOrNull(GenreRelationCandidate::score) }
            .sortedByDescending(GenreRelationCandidate::score)
            .take(MAXIMUM_CANDIDATES)
    }

    private fun String.normalizedGenreKey(): String {
        return trim()
            .lowercase()
            .replace(Regex("""[^a-z0-9]+"""), "")
    }

    private companion object {
        const val MINIMUM_RELATION_SCORE = 0.35f
        const val MAXIMUM_CANDIDATES = 6
    }
}
