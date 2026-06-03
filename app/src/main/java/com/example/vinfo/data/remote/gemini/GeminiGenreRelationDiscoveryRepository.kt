package com.example.vinfo.data.remote.gemini

import com.example.vinfo.domain.model.AppResult
import com.example.vinfo.domain.model.GenreRelationCandidate
import com.example.vinfo.domain.repository.GenreRelationDiscoveryRepository
import com.example.vinfo.domain.usecase.DiscoverNearbyGenresUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiGenreRelationDiscoveryRepository(
    private val parser: GeminiGenreRelationJsonParser = GeminiGenreRelationJsonParser(),
    private val filterCandidates: DiscoverNearbyGenresUseCase = DiscoverNearbyGenresUseCase()
) : GenreRelationDiscoveryRepository {
    override suspend fun discoverNearbyGenres(
        selectedGenre: String,
        apiKey: String
    ): AppResult<List<GenreRelationCandidate>> = withContext(Dispatchers.IO) {
        if (selectedGenre.isBlank()) {
            return@withContext AppResult.Error("선택한 장르를 확인할 수 없습니다.")
        }
        if (apiKey.isBlank()) {
            return@withContext AppResult.Error("설정 화면에서 Gemini API Key를 먼저 등록해 주세요.")
        }

        val rawResponse = runCatching {
            GeminiApiClientFactory
                .create(apiKey)
                .generate(
                    GeminiRequestBuilder.DEFAULT_MODEL,
                    apiKey,
                    GeminiGenreRelationRequestBuilder.build(selectedGenre)
                )
        }.getOrElse {
            return@withContext AppResult.Error("근처 장르를 검색하지 못했습니다.", it)
        }

        when (val parsed = parser.parse(rawResponse)) {
            is AppResult.Success -> AppResult.Success(
                filterCandidates(
                    sourceGenre = selectedGenre,
                    candidates = parsed.data.nearbyGenres
                )
            )
            is AppResult.Error -> parsed
            AppResult.Loading -> AppResult.Error("근처 장르 검색 상태를 확인할 수 없습니다.")
        }
    }
}
