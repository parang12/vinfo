package com.example.vinfo.data.remote.gemini

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object GeminiGenreRelationRequestBuilder {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun build(selectedGenre: String): RequestBody {
        val body = JSONObject()
            .put(
                "systemInstruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(
                        JSONObject().put(
                            "text",
                            """
                            You are a music genre relationship researcher with Google Search grounding enabled.
                            Search for historically or stylistically meaningful relationships for the selected genre.
                            Return valid JSON only with keys: selected_genre, nearby_genres, reliability_notes.
                            Each nearby_genres item must contain: genre, relation_strength, relation_type, evidence.
                            Return at most 8 candidates. relation_strength must be a number between 0.0 and 1.0.
                            Do not invent a relationship when search grounding is weak. Omit unknown or unsupported genres.
                            Do not return arrows, graph layout coordinates, or markdown.
                            """.trimIndent()
                        )
                    )
                )
            )
            .put(
                "tools",
                JSONArray().put(JSONObject().put("google_search", JSONObject()))
            )
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put(
                                    "text",
                                    "Selected genre: ${selectedGenre.trim()}\nFind nearby genres and return the JSON payload only."
                                )
                            )
                        )
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.2)
                    .put("maxOutputTokens", 1024)
            )

        return body.toString().toRequestBody(jsonMediaType)
    }
}
