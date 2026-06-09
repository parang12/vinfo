package com.example.vinfo.data.local.entity

import com.example.vinfo.domain.model.GenreRelationCandidate
import org.junit.Assert.assertEquals
import org.junit.Test

class GenreRelationCacheEntityTest {

    @Test
    fun `fromCandidates stores normalized source key and restores candidates`() {
        val entity = GenreRelationCacheEntity.fromCandidates(
            sourceGenre = "R & B",
            candidates = listOf(
                GenreRelationCandidate(
                    genreName = "Soul",
                    score = 0.84f,
                    relationType = "root",
                    evidence = "Shared roots"
                )
            ),
            updatedAtMillis = 10L
        )

        val restored = entity.toCandidates()

        assertEquals("randb", entity.sourceGenreKey)
        assertEquals(GenreRelationCacheEntity.REVIEW_PENDING, entity.reviewStatus)
        assertEquals(10L, entity.updatedAtMillis)
        assertEquals("Soul", restored.single().genreName)
        assertEquals(0.84f, restored.single().score, 0.001f)
        assertEquals("root", restored.single().relationType)
        assertEquals("Shared roots", restored.single().evidence)
    }
}
