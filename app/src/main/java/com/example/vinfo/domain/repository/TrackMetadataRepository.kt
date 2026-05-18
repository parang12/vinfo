package com.example.vinfo.domain.repository

import com.example.vinfo.domain.model.AppResult
import com.example.vinfo.domain.model.TrackMetadata

interface TrackMetadataRepository {
    suspend fun fetchTrackMetadata(
        artist: String,
        title: String,
        album: String?,
        apiKey: String
    ): AppResult<TrackMetadata>
}