package com.example.vinfo.data.remote.gemini

import com.example.vinfo.domain.model.AppResult
import com.example.vinfo.domain.model.AlbumGenreCandidate
import com.example.vinfo.domain.model.GenreCandidateTier
import org.json.JSONObject

class GeminiJsonParser {

    fun parseTrackMetadata(rawResponse: String): AppResult<com.example.vinfo.data.remote.perplexity.TrackMetadataDto> {
        val payload = extractGeminiText(rawResponse) ?: rawResponse
        val jsonText = extractFirstJsonObject(payload) ?: extractFirstJsonObject(rawResponse)
            ?: return AppResult.Error("LLM 응답에서 JSON 객체를 찾을 수 없습니다.")

        val jsonObject = runCatching { JSONObject(jsonText) }.getOrElse {
            return AppResult.Error("LLM JSON 파싱 실패", it)
        }

        val dto = runCatching {
            // reuse existing DTO structure from perplexity package
            jsonObject.toTrackMetadataDto()
        }.getOrElse {
            return AppResult.Error("LLM 필드 검증 실패", it)
        }

        return if (dto.artist.isBlank() || dto.title.isBlank()) {
            AppResult.Error("LLM 응답 필수 필드 누락")
        } else {
            AppResult.Success(dto)
        }
    }

    private fun extractGeminiText(rawResponse: String): String? {
        val root = runCatching { JSONObject(rawResponse) }.getOrNull() ?: return null
        val candidates = root.optJSONArray("candidates") ?: return null
        val firstCandidate = candidates.optJSONObject(0) ?: return null
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        val firstPart = parts.optJSONObject(0) ?: return null
        return firstPart.optString("text").takeIf { it.isNotBlank() }
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

    private fun JSONObject.toTrackMetadataDto(): com.example.vinfo.data.remote.perplexity.TrackMetadataDto {
        val genreCandidates = buildList {
            addAll(optGenreCandidates("primary_genres", GenreCandidateTier.PRIMARY))
            addAll(optGenreCandidates("secondary_genres", GenreCandidateTier.SECONDARY))
            addAll(optGenreCandidates("microgenres", GenreCandidateTier.MICRO))
        }
        val primaryGenre = genreCandidates.firstOrNull { it.tier == GenreCandidateTier.PRIMARY }?.name
            ?: optString("primary_genre").trim()
        val secondaryGenre = genreCandidates.firstOrNull { it.tier == GenreCandidateTier.SECONDARY }?.name
            ?: optNullableString("secondary_genre")

        return com.example.vinfo.data.remote.perplexity.TrackMetadataDto(
            artist = optString("artist").trim(),
            title = optString("title").trim(),
            album = optNullableString("album"),
            primaryGenre = primaryGenre,
            secondaryGenre = secondaryGenre,
            genreCandidates = genreCandidates,
            genreSource = optNullableString("genre_source"),
            rymRating = optNullableFloat("rym_rating"),
            pitchforkScore = optNullableFloat("pitchfork_score"),
            metacriticScore = optNullableInt("metacritic_score"),
            aotyScore = optNullableInt("aoty_score"),
            criticsSummary = optString("critics_summary").trim(),
            interviewSummary = optNullableString("interview_summary"),
            listeningGuide = optString("listening_guide").trim(),
            samplesUsed = optStringList("samples_used", "samplesUsed"),
            missingSources = optStringList("missing_sources", "missingSources"),
            reliabilityNotes = optStringList("reliability_notes", "reliabilityNotes")
        )
    }

    private fun JSONObject.optGenreCandidates(
        key: String,
        tier: GenreCandidateTier
    ): List<AlbumGenreCandidate> {
        val array = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index)
                val name = when {
                    item != null -> item.optString("name").trim()
                    else -> array.optString(index).trim()
                }
                if (name.isBlank()) continue
                add(
                    AlbumGenreCandidate(
                        name = name,
                        confidence = item?.optDouble("confidence", 1.0)?.toFloat()?.coerceIn(0f, 1f) ?: 1f,
                        tier = tier,
                        evidenceText = item
                            ?.optString("evidence_text")
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).trim().takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    }

    private fun JSONObject.optNullableFloat(key: String): Float? {
        if (!has(key) || isNull(key)) return null
        return when (val value = opt(key)) {
            is Number -> value.toFloat()
            is String -> value.toNumericStringOrNull()?.toFloatOrNull()
            else -> null
        }
    }

    private fun JSONObject.optNullableInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return when (val value = opt(key)) {
            is Number -> value.toInt()
            is String -> value.toNumericStringOrNull()?.toFloatOrNull()?.toInt()
            else -> null
        }
    }

    private fun String.toNumericStringOrNull(): String? {
        val trimmed = trim()
        if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) return null
        return Regex("""[-+]?\d+(?:\.\d+)?""")
            .find(trimmed)
            ?.value
    }

    private fun JSONObject.optStringList(vararg keys: String): List<String> {
        val array = keys.firstNotNullOfOrNull { key ->
            if (has(key)) optJSONArray(key) else null
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
}
