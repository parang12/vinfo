package com.example.vinfo.data.remote.gemini

import com.example.vinfo.domain.model.AppResult
import com.example.vinfo.domain.model.GenreRelationCandidate
import com.example.vinfo.domain.model.GenreRelationSearchPayload
import org.json.JSONObject

class GeminiGenreRelationJsonParser {
    fun parse(rawResponse: String): AppResult<GenreRelationSearchPayload> {
        val payload = extractGeminiText(rawResponse) ?: rawResponse
        val jsonText = extractFirstJsonObject(payload) ?: extractFirstJsonObject(rawResponse)
            ?: return AppResult.Error("장르 관계 응답에서 JSON 객체를 찾을 수 없습니다.")
        val root = runCatching { JSONObject(jsonText) }.getOrElse {
            return AppResult.Error("장르 관계 JSON 파싱 실패", it)
        }

        val selectedGenre = root.optString("selected_genre").trim()
        if (selectedGenre.isBlank()) {
            return AppResult.Error("선택 장르가 응답에 없습니다.")
        }

        val nearbyGenres = buildList {
            val array = root.optJSONArray("nearby_genres") ?: return@buildList
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val genreName = item.optString("genre").trim()
                val score = item.optFloatOrNull("relation_strength") ?: continue
                if (genreName.isBlank()) continue
                add(
                    GenreRelationCandidate(
                        genreName = genreName,
                        score = score.coerceIn(0f, 1f),
                        relationType = item.optString("relation_type").trim(),
                        evidence = item.optString("evidence").trim()
                    )
                )
            }
        }

        return AppResult.Success(
            GenreRelationSearchPayload(
                selectedGenre = selectedGenre,
                nearbyGenres = nearbyGenres,
                reliabilityNotes = root.optStringList("reliability_notes")
            )
        )
    }

    private fun extractGeminiText(rawResponse: String): String? {
        val root = runCatching { JSONObject(rawResponse) }.getOrNull() ?: return null
        val candidates = root.optJSONArray("candidates") ?: return null
        val firstCandidate = candidates.optJSONObject(0) ?: return null
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        return parts.optJSONObject(0)?.optString("text")?.takeIf { it.isNotBlank() }
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
                '{' -> if (!inString) {
                    if (depth == 0) startIndex = index
                    depth++
                }
                '}' -> if (!inString && depth > 0) {
                    depth--
                    if (depth == 0 && startIndex >= 0) {
                        return text.substring(startIndex, index + 1)
                    }
                }
            }
        }
        return null
    }

    private fun JSONObject.optFloatOrNull(key: String): Float? {
        if (!has(key) || isNull(key)) return null
        return when (val value = opt(key)) {
            is Number -> value.toFloat()
            is String -> value.trim().toFloatOrNull()
            else -> null
        }
    }

    private fun JSONObject.optStringList(key: String): List<String> {
        val array = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
            }
        }
    }
}
