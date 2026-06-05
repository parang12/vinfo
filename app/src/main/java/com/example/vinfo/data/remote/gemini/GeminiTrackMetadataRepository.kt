package com.example.vinfo.data.remote.gemini

import com.example.vinfo.domain.model.AppResult
import com.example.vinfo.domain.model.GenreMapper
import com.example.vinfo.domain.model.TrackMetadata
import com.example.vinfo.domain.repository.TrackMetadataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

class GeminiTrackMetadataRepository(
    private val jsonParser: GeminiJsonParser = GeminiJsonParser(),
    private val serviceFactory: (String) -> GeminiApiService = GeminiApiClientFactory::create
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

        val service = serviceFactory(apiKey)

        val rawResponse = retryNetworkCall {
            service.generate(GeminiRequestBuilder.DEFAULT_MODEL, apiKey, GeminiRequestBuilder.build(artist, title, album))
        }.getOrElse { throwable ->
            return@withContext AppResult.Error(throwable.toGeminiErrorMessage(), throwable)
        }

        when (val parsed = jsonParser.parseTrackMetadata(rawResponse, artist, title, album)) {
            is AppResult.Success -> AppResult.Success(parsed.data.toDomainMetadata())
            is AppResult.Error -> parsed
            AppResult.Loading -> AppResult.Error("LLM 상태를 해석할 수 없습니다.")
        }
    }

    private suspend fun <T> retryNetworkCall(block: suspend () -> T): Result<T> {
        val maxAttempts = 3
        var lastError: Throwable? = null

        repeat(maxAttempts) { attempt ->
            try {
                return Result.success(block())
            } catch (throwable: Throwable) {
                lastError = throwable
                if (!shouldRetry(throwable) || attempt == maxAttempts - 1) {
                    return Result.failure(throwable)
                }
                delay(backoffMillis(attempt))
            }
        }

        return Result.failure(lastError ?: IllegalStateException("Gemini API 호출 실패"))
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

    private fun Throwable.toGeminiErrorMessage(): String {
        if (this is HttpException) {
            if (code() == 429) {
                return "Gemini API 사용량 한도를 초과했습니다. AI Studio에서 현재 사용량과 결제/요금제 설정을 확인해 주세요."
            }

            val body = response()?.errorBody()?.string()
            val detail = body?.extractGeminiErrorMessage()
                ?: message()
                ?: "HTTP 오류"
            return "Gemini API 오류 ${code()}: $detail"
        }

        if (this is IOException) {
            return "Gemini API 네트워크 오류: ${message ?: "연결을 확인해 주세요."}"
        }

        return "Gemini API 호출 오류: ${message ?: "알 수 없는 오류"}"
    }

    private fun String.extractGeminiErrorMessage(): String? {
        return runCatching {
            JSONObject(this)
                .optJSONObject("error")
                ?.optString("message")
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun com.example.vinfo.data.remote.perplexity.TrackMetadataDto.toDomainMetadata(): TrackMetadata {
        return TrackMetadata(
            artist = artist,
            title = title,
            album = album,
            primaryGenre = GenreMapper.fromRawGenre(primaryGenre),
            secondaryGenre = secondaryGenre?.let(GenreMapper::fromRawGenre),
            genreCandidates = genreCandidates,
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
