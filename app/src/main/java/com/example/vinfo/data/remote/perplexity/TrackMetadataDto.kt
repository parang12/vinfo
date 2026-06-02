package com.example.vinfo.data.remote.perplexity

data class TrackMetadataDto(
    val artist: String,
    val title: String,
    val album: String?,
    val primaryGenre: String,
    val secondaryGenre: String?,
    val genreSource: String?,
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
