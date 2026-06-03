package com.example.vinfo.domain.model

enum class RelationStrength(val koreanLabel: String) {
    STRONG("강함"),
    MEDIUM("보통"),
    WEAK("약함");

    companion object {
        fun fromScore(score: Float): RelationStrength = when {
            score >= 0.75f -> STRONG
            score >= 0.50f -> MEDIUM
            else -> WEAK
        }
    }
}

data class GenreRelationCandidate(
    val genreName: String,
    val score: Float,
    val relationType: String,
    val evidence: String
) {
    val strength: RelationStrength = RelationStrength.fromScore(score)
}

data class ConfirmedGenreDiscovery(
    val sourceGenre: String,
    val candidates: List<GenreRelationCandidate>
)

data class GenreRelationSearchPayload(
    val selectedGenre: String,
    val nearbyGenres: List<GenreRelationCandidate>,
    val reliabilityNotes: List<String>
)
