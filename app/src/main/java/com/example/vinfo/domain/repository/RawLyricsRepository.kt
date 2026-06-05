package com.example.vinfo.domain.repository

import com.example.vinfo.domain.model.AppResult

interface RawLyricsRepository {
    suspend fun getRawLyrics(artist: String, title: String): AppResult<String>
}
