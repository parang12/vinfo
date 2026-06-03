package com.example.vinfo.ui.stats

import com.example.vinfo.domain.model.GenreRelationCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenreMapDiscoveryReducerTest {
    @Test
    fun `loading success and confirm update popup state and confirmed discoveries`() {
        val loading = GenreMapDiscoveryState().startSearch("Hyperpop")
        assertTrue(loading.isLoading)

        val loaded = loading.showCandidates(
            listOf(GenreRelationCandidate("Electropop", 0.9f, "influence", "근거"))
        )
        assertTrue(loaded.isPopupVisible)
        assertEquals("Electropop", loaded.candidates.single().genreName)

        val confirmed = loaded.confirmCandidates(loaded.candidates)
        assertFalse(confirmed.isPopupVisible)
        assertEquals("Hyperpop", confirmed.confirmedDiscoveries.single().sourceGenre)
    }

    @Test
    fun `confirming selected candidates only adds the selected nearby genres`() {
        val loaded = GenreMapDiscoveryState()
            .startSearch("Hyperpop")
            .showCandidates(
                listOf(
                    GenreRelationCandidate("Electropop", 0.9f, "influence", "강한 근거"),
                    GenreRelationCandidate("Bubblegum Bass", 0.7f, "adjacent", "보통 근거")
                )
            )

        val confirmed = loaded.confirmCandidates(listOf(loaded.candidates.first()))

        assertEquals(
            listOf("Electropop"),
            confirmed.confirmedDiscoveries.single().candidates.map { it.genreName }
        )
    }

    @Test
    fun `confirming the same discovered genre twice keeps the stronger relation`() {
        val state = GenreMapDiscoveryState()
            .startSearch("Hyperpop")
            .showCandidates(listOf(GenreRelationCandidate("Electropop", 0.6f, "adjacent", "약한 근거")))
            .let { it.confirmCandidates(it.candidates) }
            .startSearch("Hyperpop")
            .showCandidates(listOf(GenreRelationCandidate("Electropop", 0.9f, "influence", "강한 근거")))
            .let { it.confirmCandidates(it.candidates) }

        val electropop = state.confirmedDiscoveries.single().candidates.single()
        assertEquals(0.9f, electropop.score, 0.001f)
        assertEquals("강한 근거", electropop.evidence)
    }
}
