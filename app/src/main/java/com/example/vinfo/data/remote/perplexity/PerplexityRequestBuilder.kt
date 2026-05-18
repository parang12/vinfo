package com.example.vinfo.data.remote.perplexity

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object PerplexityRequestBuilder {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun build(artist: String, title: String, album: String?): RequestBody {
        val messages = JSONArray()
            .put(
                JSONObject()
                    .put("role", "system")
                    .put("content", PerplexityPromptBuilder.buildSystemPrompt())
            )
            .put(
                JSONObject()
                    .put("role", "user")
                    .put("content", PerplexityPromptBuilder.buildUserPrompt(artist, title, album))
            )

        val body = JSONObject()
            .put("model", "sonar-pro")
            .put("temperature", 0.2)
            .put("messages", messages)
            .put("response_format", JSONObject()
                .put("type", "json_schema")
                .put("json_schema", JSONObject()
                    .put("name", "vinfo_music_research")
                    .put("schema", JSONObject()
                        .put("type", "object")
                        .put("properties", JSONObject()
                            .put("critic_review", JSONObject())
                            .put("genres", JSONObject())
                            .put("rym", JSONObject())
                            .put("interviews", JSONObject())
                            .put("sampling", JSONObject())
                            .put("listening_guide", JSONObject())
                            .put("taste_exploration", JSONObject())
                            .put("reliability", JSONObject())
                        )
                    )
                )
            )

        return body.toString().toRequestBody(jsonMediaType)
    }
}