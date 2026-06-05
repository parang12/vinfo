package com.example.vinfo.data.repository

import com.example.vinfo.data.local.dao.AlbumDao
import com.example.vinfo.data.local.entity.AlbumEntity
import com.example.vinfo.domain.model.AppResult
import com.example.vinfo.domain.model.NowPlayingTrack
import com.example.vinfo.domain.model.TrackMetadata
import com.example.vinfo.domain.model.buildTrackId
import com.example.vinfo.domain.repository.TrackMetadataRepository

class CachedTrackMetadataRepository(
    private val albumDao: AlbumDao,
    private val remoteRepository: TrackMetadataRepository
) : TrackMetadataRepository {

    override suspend fun fetchTrackMetadata(
        artist: String,
        title: String,
        album: String?,
        apiKey: String
    ): AppResult<TrackMetadata> {
        findCachedMetadata(artist, title, album)?.let { cached ->
            return AppResult.Success(cached.toTrackMetadata())
        }

        val result = remoteRepository.fetchTrackMetadata(artist, title, album, apiKey)
        if (result is AppResult.Success) {
            cacheMetadata(artist, title, album, result.data)
        }
        return result
    }

    private suspend fun findCachedMetadata(
        artist: String,
        title: String,
        album: String?
    ): AlbumEntity? {
        val albumTitle = album?.trim()?.takeIf { it.isNotBlank() }
        if (albumTitle != null) {
            albumDao.findAlbumByArtistAndAlbum(artist.trim(), albumTitle)?.let { return it }
        }

        return albumDao.getAlbumById(buildTrackId(artist, title))
    }

    private suspend fun cacheMetadata(
        artist: String,
        title: String,
        album: String?,
        metadata: TrackMetadata
    ) {
        val track = NowPlayingTrack(
            artist = artist,
            title = title,
            album = metadata.album ?: album
        )
        albumDao.insertAlbum(
            AlbumEntity.fromTrackSnapshot(
                trackId = buildTrackId(metadata.artist, metadata.title),
                track = track,
                metadata = metadata
            )
        )
    }
}
