package com.example.vinfo.data.remote.gemini

import com.example.vinfo.domain.model.AppResult
import com.example.vinfo.domain.model.GenreMapper
import com.example.vinfo.domain.model.TrackMetadata
import com.example.vinfo.domain.repository.TrackMetadataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class GeminiTrackMetadataRepository(
    private val jsonParser: GeminiJsonParser = GeminiJsonParser()
) : TrackMetadataRepository {

    override suspend fun fetchTrackMetadata(
        artist: String,
        title: String,
        album: String?,
        apiKey: String
    ): AppResult<TrackMetadata> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext AppResult.Error("Gemini API Key가 비어 있습니다.")
        }

        val service = GeminiApiClientFactory.create(apiKey)

        val rawResponse = retryNetworkCall {
            service.generate(GeminiRequestBuilder.DEFAULT_MODEL, apiKey, GeminiRequestBuilder.build(artist, title, album))
        } ?: return@withContext AppResult.Error("Gemini API 응답을 가져오지 못했습니다.")

        when (val parsed = jsonParser.parseTrackMetadata(rawResponse)) {
            is AppResult.Success -> AppResult.Success(parsed.data.toDomainMetadata())
            is AppResult.Error -> parsed
            AppResult.Loading -> AppResult.Error("LLM 상태를 해석할 수 없습니다.")
        }
    }

    private suspend fun <T> retryNetworkCall(block: suspend () -> T): T? {
        val maxAttempts = 3
        var lastError: Throwable? = null

        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (throwable: Throwable) {
                lastError = throwable
                if (!shouldRetry(throwable) || attempt == maxAttempts - 1) {
                    return null
                }
                delay(backoffMillis(attempt))
            }
        }

        return null
    }

    private fun shouldRetry(throwable: Throwable): Boolean {
        return when (throwable) {
            is HttpException -> throwable.code() >= 500
            is IOException -> true
            else -> false
        }
    }

    private fun backoffMillis(attempt: Int): Long {
        return 500L shl attempt
    }

    private fun com.example.vinfo.data.remote.perplexity.TrackMetadataDto.toDomainMetadata(): TrackMetadata {
        return TrackMetadata(
            artist = artist,
            title = title,
            album = album,
            primaryGenre = GenreMapper.fromRawGenre(primaryGenre),
            secondaryGenre = secondaryGenre?.let(GenreMapper::fromRawGenre),
            genreSource = GenreMapper.fromRawSource(genreSource),
            rymRating = rymRating,
            pitchforkScore = pitchforkScore,
            metacriticScore = metacriticScore,
            aotyScore = aotyScore,
            criticsSummary = criticsSummary,
            interviewSummary = interviewSummary,
            listeningGuide = listeningGuide,
            samplesUsed = samplesUsed,
            missingSources = missingSources,
            reliabilityNotes = reliabilityNotes
        )
    }
}
