package com.example.vinfo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CuratedGenreRelationsTest {

    @Test
    fun `returns nearby fallback candidates for rnb and funk map nodes`() {
        val rnbCandidates = CuratedGenreRelations.nearbyGenres("R&B")
        val funkCandidates = CuratedGenreRelations.nearbyGenres("Funk")

        assertTrue(rnbCandidates.any { it.genreName == "Soul" })
        assertTrue(rnbCandidates.any { it.genreName == "Hip-Hop" })
        assertTrue(funkCandidates.any { it.genreName == "Soul" })
        assertTrue(funkCandidates.any { it.genreName == "Hip-Hop" })
    }

    @Test
    fun `normalizes ampersand and spacing variants`() {
        val rnb = CuratedGenreRelations.nearbyGenres("R & B")

        assertEquals(
            CuratedGenreRelations.nearbyGenres("R&B").map { it.genreName },
            rnb.map { it.genreName }
        )
    }
}
