package com.example.vinfo.data.remote.gemini

import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiRequestBuilderTest {

    @Test
    fun `build asks Gemini to identify the album and return album-based metadata JSON`() {
        assertEquals("gemini-2.5-flash-lite", GeminiRequestBuilder.DEFAULT_MODEL)

        val body = GeminiRequestBuilder.build(
            artist = "Test Artist",
            title = "Test Title",
            album = null
        )

        val buffer = Buffer()
        body.writeTo(buffer)
        val requestJson = JSONObject(buffer.readUtf8())
        val systemText = requestJson
            .getJSONObject("systemInstruction")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
        val userText = requestJson
            .getJSONArray("contents")
            .getJSONObject(0)
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
        val generationConfig = requestJson.getJSONObject("generationConfig")
        val googleSearchTool = requestJson
            .getJSONArray("tools")
            .getJSONObject(0)
            .getJSONObject("google_search")

        assertTrue(systemText.contains("album"))
        assertTrue(systemText.contains("pitchfork_score"))
        assertTrue(systemText.contains("metacritic_score"))
        assertTrue(systemText.contains("aoty_score"))
        assertTrue(systemText.contains("missing_sources"))
        assertTrue(systemText.contains("primary_genres"))
        assertTrue(systemText.contains("secondary_genres"))
        assertTrue(systemText.contains("microgenres"))
        assertTrue(systemText.contains("confidence"))
        assertTrue(systemText.contains("most specific"))
        assertTrue(systemText.contains("Trap"))
        assertTrue(systemText.contains("Reddit"))
        assertTrue(systemText.contains("HipHople"))
        assertTrue(systemText.contains("community-only score"))
        assertTrue(systemText.contains("Step 1"))
        assertTrue(systemText.contains("canonical album"))
        assertTrue(systemText.contains("Step 2"))
        assertTrue(systemText.contains("identified album"))
        assertTrue(systemText.contains("listening_guide must be written in Korean"))
        assertTrue(systemText.contains("samples_used must be an array of short strings"))
        assertEquals(0, googleSearchTool.length())
        assertTrue(userText.contains("Artist: Test Artist"))
        assertTrue(userText.contains("Title: Test Title"))
        assertTrue(userText.contains("identify the matching album"))
        assertTrue(userText.contains("Do not analyze the song as a standalone unit"))
        assertTrue(userText.contains("Return album metadata for the resolved album"))
        assertTrue(userText.contains("primary_genres"))
        assertTrue(userText.contains("site:reddit.com"))
        assertTrue(userText.contains("site:hiphople.com"))
        assertFalse(generationConfig.has("responseMimeType"))
        assertEquals(2048, generationConfig.getInt("maxOutputTokens"))
    }
}
