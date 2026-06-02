package com.example.vinfo.domain.model

data class TrackMetadata(
    val artist: String,
    val title: String,
    val album: String?,
    val primaryGenre: GenreCategory,
    val secondaryGenre: GenreCategory?,
    val genreSource: GenreSource,
    val rymRating: Float?,
    val pitchforkScore: Float?,
    val metacriticScore: Int?,
    val aotyScore: Int?,
    val criticsSummary: String,
    val interviewSummary: String?,
    val listeningGuide: String,
    val samplesUsed: List<String>,
    val missingSources: List<String>,
    val reliabilityNotes: List<String>
)

enum class GenreCategory {
    HIP_HOP,
    POP,
    ROCK,
    ELECTRONIC,
    JAZZ,
    CLASSICAL,
    RNB,
    UNKNOWN
}

enum class GenreSource {
    RYM,
    LLM,
    MANUAL,
    UNKNOWN
}
