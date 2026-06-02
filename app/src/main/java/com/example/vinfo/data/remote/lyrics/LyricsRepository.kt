package com.example.vinfo.data.remote.lyrics

import com.example.vinfo.domain.model.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LyricsRepository(
    private val service: LyricsApiService = LyricsApiClientFactory.create()
) {
    suspend fun getRawLyrics(artist: String, title: String): AppResult<String> = withContext(Dispatchers.IO) {
        if (artist.isBlank() || title.isBlank()) {
            return@withContext AppResult.Error("가사를 조회할 곡 정보가 없습니다.")
        }

        runCatching {
            val rawResponse = service.getLyrics(artist.trim(), title.trim())
            JSONObject(rawResponse)
                .optString("lyrics")
                .trim()
                .takeIf { it.isNotBlank() }
                ?: error("가사 응답이 비어 있습니다.")
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Error("가사를 찾을 수 없습니다.", it) }
        )
    }
}
