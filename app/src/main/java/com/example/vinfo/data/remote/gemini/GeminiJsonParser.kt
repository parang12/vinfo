package com.example.vinfo.data.remote.gemini

import com.example.vinfo.domain.model.AppResult
import com.example.vinfo.domain.model.AlbumGenreCandidate
import com.example.vinfo.domain.model.GenreCandidateTier
import org.json.JSONObject

class GeminiJsonParser {

    fun parseTrackMetadata(
        rawResponse: String,
        fallbackArtist: String? = null,
        fallbackTitle: String? = null,
        fallbackAlbum: String? = null
    ): AppResult<TrackMetadataDto> {
        val payload = extractGeminiText(rawResponse)
        val jsonText = if (payload != null) {
            extractFirstJsonObject(payload)
                ?: return AppResult.Error("LLM 응답에서 앨범 메타데이터 JSON을 찾을 수 없습니다.")
        } else {
            extractFirstJsonObject(rawResponse)
                ?: return AppResult.Error("LLM 응답에서 JSON 객체를 찾을 수 없습니다.")
        }

        val jsonObject = runCatching { JSONObject(jsonText) }.getOrElse {
            return AppResult.Error("LLM JSON 파싱 실패", it)
        }

        val dto = runCatching {
            jsonObject.toTrackMetadataDto(fallbackArtist, fallbackTitle, fallbackAlbum)
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

    private fun JSONObject.toTrackMetadataDto(
        fallbackArtist: String?,
        fallbackTitle: String?,
        fallbackAlbum: String?
    ): TrackMetadataDto {
        val fallbackGenreCandidates = optGenreCandidates(
            GenreCandidateTier.PRIMARY,
            "genres",
            "album_genres",
            "albumGenres",
            "genre_tags",
            "genreTags",
            "tags",
            "styles"
        )
        val explicitPrimaryCandidates = optGenreCandidates(
            GenreCandidateTier.PRIMARY,
            "primary_genres",
            "primaryGenres",
            "primary_genre",
            "primaryGenre",
            "main_genre",
            "mainGenre",
            "genre"
        )
        val primaryCandidates = explicitPrimaryCandidates.ifEmpty {
            fallbackGenreCandidates.take(1)
        }
        val explicitSecondaryCandidates = optGenreCandidates(
            GenreCandidateTier.SECONDARY,
            "secondary_genres",
            "secondaryGenres",
            "secondary_genre",
            "secondaryGenre",
            "subgenres",
            "sub_genres",
            "subGenres"
        )
        val secondaryCandidates = explicitSecondaryCandidates.ifEmpty {
            val existingPrimaryNames = primaryCandidates.map { it.name.normalizedKey() }.toSet()
            fallbackGenreCandidates
                .drop(if (explicitPrimaryCandidates.isEmpty()) 1 else 0)
                .filterNot { it.name.normalizedKey() in existingPrimaryNames }
                .map { it.copy(tier = GenreCandidateTier.SECONDARY) }
        }
        val microCandidates = optGenreCandidates(
            GenreCandidateTier.MICRO,
            "microgenres",
            "microGenres",
            "micro_genres",
            "microgenre",
            "microGenre"
        )
        val genreCandidates = buildList {
            addAll(primaryCandidates)
            addAll(secondaryCandidates)
            addAll(microCandidates)
        }
        val primaryGenre = genreCandidates.firstOrNull { it.tier == GenreCandidateTier.PRIMARY }?.name
            ?: optString("primary_genre").trim()
        val secondaryGenre = genreCandidates.firstOrNull { it.tier == GenreCandidateTier.SECONDARY }?.name
            ?: optNullableString("secondary_genre")

        return TrackMetadataDto(
            artist = fallbackArtist?.trim()?.takeIf { it.isNotBlank() }
                ?: optFirstString("artist", "artist_name", "artistName").orEmpty(),
            title = fallbackTitle?.trim()?.takeIf { it.isNotBlank() }
                ?: optFirstString("title", "track_title", "trackTitle", "song_title", "songTitle").orEmpty(),
            album = optFirstString("album", "album_title", "albumTitle")
                ?: fallbackAlbum?.trim()?.takeIf { it.isNotBlank() },
            primaryGenre = primaryGenre,
            secondaryGenre = secondaryGenre,
            genreCandidates = genreCandidates,
            genreSource = optFirstString("genre_source", "genreSource", "source"),
            rymRating = optFirstNullableFloat("rym_rating", "rymRating", "rym", "rate_your_music", "rateYourMusic")
                ?: optNestedNullableFloat("ratings", "rym", "rate_your_music", "rateYourMusic")
                ?: optNestedNullableFloat("album_ratings", "rym", "rate_your_music", "rateYourMusic")
                ?: optNestedNullableFloat("albumRatings", "rym", "rate_your_music", "rateYourMusic"),
            pitchforkScore = optFirstNullableFloat("pitchfork_score", "pitchforkScore", "pitchfork")
                ?: optNestedNullableFloat("ratings", "pitchfork", "pitchfork_score", "pitchforkScore")
                ?: optNestedNullableFloat("album_ratings", "pitchfork", "pitchfork_score", "pitchforkScore")
                ?: optNestedNullableFloat("albumRatings", "pitchfork", "pitchfork_score", "pitchforkScore"),
            metacriticScore = optFirstNullableInt("metacritic_score", "metacriticScore", "metacritic")
                ?: optNestedNullableInt("ratings", "metacritic", "metacritic_score", "metacriticScore")
                ?: optNestedNullableInt("album_ratings", "metacritic", "metacritic_score", "metacriticScore")
                ?: optNestedNullableInt("albumRatings", "metacritic", "metacritic_score", "metacriticScore"),
            aotyScore = optFirstNullableInt("aoty_score", "aotyScore", "aoty", "album_of_the_year", "albumOfTheYear")
                ?: optNestedNullableInt("ratings", "aoty", "album_of_the_year", "albumOfTheYear")
                ?: optNestedNullableInt("album_ratings", "aoty", "album_of_the_year", "albumOfTheYear")
                ?: optNestedNullableInt("albumRatings", "aoty", "album_of_the_year", "albumOfTheYear"),
            criticsSummary = optFirstString(
                "critics_summary",
                "criticsSummary",
                "review_summary",
                "reviewSummary",
                "critical_summary",
                "criticalSummary",
                "summary"
            ).orEmpty(),
            interviewSummary = optFirstString("interview_summary", "interviewSummary"),
            listeningGuide = optFirstString(
                "listening_guide",
                "listeningGuide",
                "listening_notes",
                "listeningNotes",
                "guide"
            )?.takeIf { it.hasHangul() }.orEmpty(),
            samplesUsed = optStringList("samples_used", "samplesUsed", "samples"),
            missingSources = optStringList("missing_sources", "missingSources"),
            reliabilityNotes = optStringList("reliability_notes", "reliabilityNotes")
        )
    }

    private fun JSONObject.optGenreCandidates(
        tier: GenreCandidateTier,
        vararg keys: String
    ): List<AlbumGenreCandidate> {
        return keys.firstNotNullOfOrNull { key ->
            if (!has(key) || isNull(key)) {
                null
            } else {
                opt(key).toGenreCandidates(tier)
            }
        } ?: emptyList()
    }

    private fun Any?.toGenreCandidates(tier: GenreCandidateTier): List<AlbumGenreCandidate> {
        return when (this) {
            is org.json.JSONArray -> toGenreCandidates(tier)
            is JSONObject -> toSingleGenreCandidate(tier)?.let(::listOf).orEmpty()
            is String -> splitGenreNames().map { name ->
                AlbumGenreCandidate(
                    name = name,
                    confidence = 1f,
                    tier = tier
                )
            }
            else -> emptyList()
        }
    }

    private fun org.json.JSONArray.toGenreCandidates(tier: GenreCandidateTier): List<AlbumGenreCandidate> {
        return buildList {
            for (index in 0 until length()) {
                when (val item = opt(index)) {
                    is JSONObject -> item.toSingleGenreCandidate(tier)?.let(::add)
                    is String -> item.splitGenreNames().forEach { name ->
                        add(
                            AlbumGenreCandidate(
                                name = name,
                                confidence = 1f,
                                tier = tier
                            )
                        )
                    }
                }
            }
        }
    }

    private fun JSONObject.toSingleGenreCandidate(tier: GenreCandidateTier): AlbumGenreCandidate? {
        val name = optFirstString("name", "genre", "label") ?: return null
        return AlbumGenreCandidate(
            name = name,
            confidence = optDouble("confidence", 1.0).toFloat().coerceIn(0f, 1f),
            tier = tier,
            evidenceText = optFirstString("evidence_text", "evidenceText", "evidence")
        )
    }

    private fun String.splitGenreNames(): List<String> {
        return split(',', ';')
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).trim().takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    }

    private fun JSONObject.optFirstString(vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key -> optNullableString(key) }
    }

    private fun JSONObject.optFirstNullableFloat(vararg keys: String): Float? {
        return keys.firstNotNullOfOrNull { key -> optNullableFloat(key) }
    }

    private fun JSONObject.optFirstNullableInt(vararg keys: String): Int? {
        return keys.firstNotNullOfOrNull { key -> optNullableInt(key) }
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

    private fun JSONObject.optNestedNullableFloat(parentKey: String, vararg childKeys: String): Float? {
        val parent = optJSONObject(parentKey) ?: return null
        return childKeys.firstNotNullOfOrNull { key -> parent.optNullableFloat(key) }
    }

    private fun JSONObject.optNestedNullableInt(parentKey: String, vararg childKeys: String): Int? {
        val parent = optJSONObject(parentKey) ?: return null
        return childKeys.firstNotNullOfOrNull { key -> parent.optNullableInt(key) }
    }

    private fun String.toNumericStringOrNull(): String? {
        val trimmed = trim()
        if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) return null
        return Regex("""[-+]?\d+(?:\.\d+)?""")
            .find(trimmed)
            ?.value
    }

    private fun String.normalizedKey(): String = trim().lowercase()

    private fun JSONObject.optStringList(vararg keys: String): List<String> {
        val array = keys.firstNotNullOfOrNull { key ->
            if (has(key)) optJSONArray(key) else null
        } ?: return emptyList()

        return buildList {
            for (index in 0 until array.length()) {
                val value = when (val item = array.opt(index)) {
                    is JSONObject -> item.toSampleLabel()
                    else -> array.optString(index).trim()
                }
                if (value.isNotBlank()) {
                    add(value)
                }
            }
        }
    }

    private fun JSONObject.toSampleLabel(): String {
        val artist = optNullableString("artist")
        val title = optNullableString("title")
        val sampleType = optFirstString("sample_type", "sampleType", "type")

        val base = listOfNotNull(artist, title)
            .joinToString(" - ")
            .ifBlank { optFirstString("name", "label", "evidence_text", "evidenceText").orEmpty() }

        return if (!sampleType.isNullOrBlank() && base.isNotBlank()) {
            "$base ($sampleType)"
        } else {
            base
        }
    }

    private fun String.hasHangul(): Boolean {
        return any { it in '\uAC00'..'\uD7A3' }
    }
}
