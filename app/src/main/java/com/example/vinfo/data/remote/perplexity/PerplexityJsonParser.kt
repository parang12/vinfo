package com.example.vinfo.data.remote.perplexity

import com.example.vinfo.domain.model.AppResult
import org.json.JSONArray
import org.json.JSONObject

class PerplexityJsonParser {

    fun parseTrackMetadata(rawResponse: String): AppResult<TrackMetadataDto> {
        val payload = extractMessageContent(rawResponse) ?: rawResponse
        val jsonText = extractFirstJsonObject(payload) ?: extractFirstJsonObject(rawResponse)
            ?: return AppResult.Error("LLM 응답에서 JSON 객체를 찾을 수 없습니다.")

        val jsonObject = runCatching { JSONObject(jsonText) }.getOrElse {
            return AppResult.Error("LLM JSON 파싱 실패", it)
        }

        val dto = runCatching { jsonObject.toTrackMetadataDto() }.getOrElse {
            return AppResult.Error("LLM 필드 검증 실패", it)
        }

        return if (dto.artist.isBlank() || dto.title.isBlank()) {
            AppResult.Error("LLM 응답 필수 필드 누락")
        } else {
            AppResult.Success(dto)
        }
    }

    private fun extractMessageContent(rawResponse: String): String? {
        val root = runCatching { JSONObject(rawResponse) }.getOrNull() ?: return null
        val choices = root.optJSONArray("choices") ?: return null
        val firstChoice = choices.optJSONObject(0) ?: return null
        val message = firstChoice.optJSONObject("message") ?: return null
        return message.optString("content").takeIf { it.isNotBlank() }
    }

    private fun JSONObject.toTrackMetadataDto(): TrackMetadataDto {
        return TrackMetadataDto(
            artist = optString("artist").trim(),
            title = optString("title").trim(),
            album = optString("album").takeIf { it.isNotBlank() },
            primaryGenre = optString("primary_genre").trim(),
            secondaryGenre = optString("secondary_genre").takeIf { it.isNotBlank() },
            genreSource = optString("genre_source").takeIf { it.isNotBlank() },
            rymRating = when (val value = opt("rym_rating")) {
                is Number -> value.toFloat()
                is String -> value.toFloatOrNull()
                else -> null
            },
            criticsSummary = optString("critics_summary").trim(),
            interviewSummary = optString("interview_summary").takeIf { it.isNotBlank() },
            listeningGuide = optString("listening_guide").trim(),
            samplesUsed = optSamplesUsed()
        )
    }

    private fun JSONObject.optSamplesUsed(): List<String> {
        val array = when {
            has("samples_used") -> optJSONArray("samples_used")
            has("samplesUsed") -> optJSONArray("samplesUsed")
            else -> null
        } ?: return emptyList()

        return buildList {
            for (index in 0 until array.length()) {
                val value = array.optString(index).trim()
                if (value.isNotBlank()) {
                    add(value)
                }
            }
        }
    }

    private fun extractFirstJsonObject(text: String): String? {
        var depth = 0
        var inString = false
        var escaping = false
        var startIndex = -1

        text.forEachIndexed { index, character ->
            if (escaping) {
                escaping = false
                return@forEachIndexed
            }

            when (character) {
                '\\' -> if (inString) escaping = true
                '"' -> inString = !inString
                '{' -> {
                    if (!inString) {
                        if (depth == 0) startIndex = index
                        depth++
                    }
                }
                '}' -> {
                    if (!inString && depth > 0) {
                        depth--
                        if (depth == 0 && startIndex >= 0) {
                            return text.substring(startIndex, index + 1)
                        }
                    }
                }
            }
        }

        return null
    }
}