package com.example.vinfo.domain.usecase

import com.example.vinfo.domain.model.AppResult
import com.example.vinfo.domain.model.TrackMetadata
import com.example.vinfo.domain.repository.TrackMetadataRepository

class GetTrackInformationUseCase(
    private val repository: TrackMetadataRepository
) {
    suspend operator fun invoke(
        artist: String,
        title: String,
        album: String?,
        apiKey: String
    ): AppResult<TrackMetadata> {
        return repository.fetchTrackMetadata(artist, title, album, apiKey)
    }
}