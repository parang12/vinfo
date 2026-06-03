package com.example.vinfo.data.remote.gemini

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object GeminiRequestBuilder {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    const val DEFAULT_MODEL = "gemini-3.1-flash-lite"

    fun build(artist: String, title: String, album: String?, model: String = DEFAULT_MODEL): RequestBody {
        val body = JSONObject()
            .put(
                "systemInstruction",
                JSONObject().put(
                    "parts",
                    org.json.JSONArray().put(
                        JSONObject().put(
                            "text",
                            "You are a music metadata assistant with Google Search grounding enabled. Use the provided artist and song title only to identify the matching album, then return album-based metadata only. Return valid JSON with keys: artist, title, album, primary_genre, secondary_genre, genre_source, rym_rating, pitchfork_score, metacritic_score, aoty_score, critics_summary, interview_summary, listening_guide, samples_used, missing_sources, reliability_notes. Do not wrap the JSON in markdown. Use null for unavailable album ratings. Include only ratings that belong to the identified album, not the individual song. Search the direct rating source first. If a direct source page is unavailable or blocked, you may use reliable Reddit discussions or HipHople posts as an indirect discovery and corroboration route. Never accept a single community-only score as authoritative. Only use an indirectly corroborated score when multiple independent search results consistently quote the same album-level score and source name; add a reliability_notes entry that the score was indirectly corroborated through community search. Otherwise return null. Use concise but information-rich Korean descriptions for summaries."
                        )
                    )
                )
            )
            .put(
                "tools",
                org.json.JSONArray().put(
                    JSONObject().put("google_search", JSONObject())
                )
            )
            .put(
                "contents",
                org.json.JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put(
                            "parts",
                            org.json.JSONArray().put(
                                JSONObject().put(
                                    "text",
                                    buildString {
                                        appendLine("Use the artist and title to identify the matching album, then analyze that album and return the JSON payload only.")
                                        appendLine("Artist: $artist")
                                        appendLine("Title: $title")
                                        appendLine("Album: ${album.orEmpty()}")
                                        appendLine("Ratings must be album-based: RYM, Pitchfork, Metacritic, and AOTY. If a source is unavailable, set its score to null and include the source name in missing_sources.")
                                        appendLine("Search direct pages first: site:rateyourmusic.com, site:pitchfork.com, site:metacritic.com, and site:albumoftheyear.org.")
                                        appendLine("If a direct page is blocked or missing, use site:reddit.com and site:hiphople.com as indirect discovery routes. Only use a community-corroborated score when multiple independent results quote the same album-level score and named source. Record indirect use in reliability_notes. Otherwise use null.")
                                    }.trim()
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
                    .put("responseMimeType", "application/json")
            )

        return body.toString().toRequestBody(jsonMediaType)
    }
}
