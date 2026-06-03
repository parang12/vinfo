package com.example.vinfo.data.remote.gemini

import com.example.vinfo.domain.model.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiGenreRelationJsonParserTest {
    @Test
    fun `parse returns nearby genre candidates from valid payload`() {
        val json = """
            {
              "selected_genre": "Hyperpop",
              "nearby_genres": [
                {
                  "genre": "Electropop",
                  "relation_strength": 0.92,
                  "relation_type": "influence",
                  "evidence": "Search-grounded evidence"
                },
                {
                  "genre": "Synth-pop",
                  "relation_strength": 0.81,
                  "relation_type": "adjacent",
                  "evidence": "Search-grounded evidence"
                }
              ],
              "reliability_notes": []
            }
        """.trimIndent()

        val result = GeminiGenreRelationJsonParser().parse(json)

        assertTrue(result is AppResult.Success)
        val payload = (result as AppResult.Success).data
        assertEquals("Hyperpop", payload.selectedGenre)
        assertEquals(2, payload.nearbyGenres.size)
        assertEquals("Electropop", payload.nearbyGenres.first().genreName)
        assertEquals(0.92f, payload.nearbyGenres.first().score, 0.001f)
    }

    @Test
    fun `parse skips malformed candidates and clamps score`() {
        val json = """
            {
              "selected_genre": "Hyperpop",
              "nearby_genres": [
                {"genre": "", "relation_strength": 0.9},
                {"genre": "Electropop", "relation_strength": 3.0},
                {"genre": "Trap", "relation_strength": "invalid"}
              ]
            }
        """.trimIndent()

        val result = GeminiGenreRelationJsonParser().parse(json)

        assertTrue(result is AppResult.Success)
        val candidates = (result as AppResult.Success).data.nearbyGenres
        assertEquals(1, candidates.size)
        assertEquals("Electropop", candidates.single().genreName)
        assertEquals(1f, candidates.single().score, 0.001f)
    }
}
