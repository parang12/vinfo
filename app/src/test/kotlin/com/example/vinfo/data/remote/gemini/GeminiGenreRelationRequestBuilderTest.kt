package com.example.vinfo.data.remote.gemini

import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiGenreRelationRequestBuilderTest {
    @Test
    fun `build requests grounded nearby genre relationships as JSON`() {
        val body = GeminiGenreRelationRequestBuilder.build("Hyperpop")
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

        assertTrue(systemText.contains("selected genre"))
        assertTrue(systemText.contains("nearby_genres"))
        assertTrue(systemText.contains("relation_strength"))
        assertTrue(systemText.contains("Do not invent"))
        assertTrue(userText.contains("Selected genre: Hyperpop"))
        assertEquals("application/json", generationConfig.getString("responseMimeType"))
        assertEquals(0, googleSearchTool.length())
    }
}
