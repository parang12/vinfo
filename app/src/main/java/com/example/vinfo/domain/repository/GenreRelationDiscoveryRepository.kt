package com.example.vinfo.domain.repository

import com.example.vinfo.domain.model.AppResult
import com.example.vinfo.domain.model.GenreRelationCandidate

interface GenreRelationDiscoveryRepository {
    suspend fun discoverNearbyGenres(
        selectedGenre: String,
        apiKey: String
    ): AppResult<List<GenreRelationCandidate>>
}
