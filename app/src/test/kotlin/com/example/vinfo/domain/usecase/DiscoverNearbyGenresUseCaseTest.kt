package com.example.vinfo.domain.usecase

import com.example.vinfo.domain.model.GenreRelationCandidate
import com.example.vinfo.domain.model.RelationStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DiscoverNearbyGenresUseCaseTest {
    private val useCase = DiscoverNearbyGenresUseCase()

    @Test
    fun `filters unknown self duplicates and weak candidates then keeps strongest six`() {
        val result = useCase(
            sourceGenre = "Hyperpop",
            candidates = listOf(
                GenreRelationCandidate("Electropop", 0.92f, "influence", "근거 A"),
                GenreRelationCandidate("Synth-pop", 0.81f, "influence", "근거 B"),
                GenreRelationCandidate("Trap", 0.73f, "adjacent", "근거 C"),
                GenreRelationCandidate("Bubblegum Pop", 0.66f, "influence", "근거 D"),
                GenreRelationCandidate("EDM", 0.58f, "adjacent", "근거 E"),
                GenreRelationCandidate("Hip Hop", 0.51f, "adjacent", "근거 F"),
                GenreRelationCandidate("Dance-pop", 0.49f, "adjacent", "근거 G"),
                GenreRelationCandidate("Unknown", 0.99f, "adjacent", "제외"),
                GenreRelationCandidate("Hyperpop", 0.95f, "adjacent", "제외"),
                GenreRelationCandidate("Electropop", 0.44f, "adjacent", "중복"),
                GenreRelationCandidate("Noise Pop", 0.20f, "adjacent", "약함")
            )
        )

        assertEquals(6, result.size)
        assertEquals("Electropop", result.first().genreName)
        assertFalse(result.any { it.genreName == "Unknown" })
        assertFalse(result.any { it.genreName == "Hyperpop" })
    }

    @Test
    fun `maps numeric strength to strong medium and weak labels`() {
        assertEquals(RelationStrength.STRONG, RelationStrength.fromScore(0.85f))
        assertEquals(RelationStrength.MEDIUM, RelationStrength.fromScore(0.60f))
        assertEquals(RelationStrength.WEAK, RelationStrength.fromScore(0.35f))
    }
}
