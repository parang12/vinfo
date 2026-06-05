package com.example.vinfo.data.repository

import com.example.vinfo.data.local.dao.LyricsCacheDao
import com.example.vinfo.data.local.entity.LyricsCacheEntity
import com.example.vinfo.domain.model.AppResult
import com.example.vinfo.domain.model.buildTrackId
import com.example.vinfo.domain.repository.RawLyricsRepository

class CachedLyricsRepository(
    private val lyricsCacheDao: LyricsCacheDao,
    private val remoteRepository: RawLyricsRepository
) : RawLyricsRepository {

    override suspend fun getRawLyrics(artist: String, title: String): AppResult<String> {
        if (artist.isBlank() || title.isBlank()) {
            return AppResult.Error("가사를 조회할 곡 정보가 없습니다.")
        }

        val id = buildTrackId(artist, title)
        lyricsCacheDao.getLyricsById(id)?.let { cached ->
            if (cached.lyrics.isNotBlank()) {
                return AppResult.Success(cached.lyrics)
            }
        }

        val result = remoteRepository.getRawLyrics(artist, title)
        if (result is AppResult.Success) {
            lyricsCacheDao.insertLyrics(
                LyricsCacheEntity(
                    id = id,
                    artist = artist.trim(),
                    title = title.trim(),
                    lyrics = result.data,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
        return result
    }
}
