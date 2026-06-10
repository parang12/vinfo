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

    @Test
    fun `loading pending review items exposes review queue count`() {
        val state = GenreMapDiscoveryState().showPendingReviews(
            listOf(
                GenreRelationReviewItem(
                    sourceGenre = "Jazz",
                    candidates = listOf(GenreRelationCandidate("Blues", 0.82f, "root", "근거"))
                )
            )
        )

        assertEquals(1, state.pendingReviewCount)
        assertEquals("Jazz", state.pendingReviews.single().sourceGenre)
    }

    @Test
    fun `confirming pending review moves it into confirmed discoveries and removes queue item`() {
        val state = GenreMapDiscoveryState()
            .showPendingReviews(
                listOf(
                    GenreRelationReviewItem(
                        sourceGenre = "Jazz",
                        candidates = listOf(GenreRelationCandidate("Blues", 0.82f, "root", "근거"))
                    )
                )
            )
            .confirmPendingReview("Jazz")

        assertEquals(0, state.pendingReviewCount)
        assertEquals("Jazz", state.confirmedDiscoveries.single().sourceGenre)
        assertEquals("Blues", state.confirmedDiscoveries.single().candidates.single().genreName)
    }

    @Test
    fun `expansion feedback reports newly confirmed relation count`() {
        val previous = GenreMapDiscoveryState()
        val next = GenreMapDiscoveryState(
            confirmedDiscoveries = listOf(
                com.example.vinfo.domain.model.ConfirmedGenreDiscovery(
                    sourceGenre = "Synth-pop",
                    candidates = listOf(
                        GenreRelationCandidate("Dream Pop", 0.84f, "adjacent", "근거"),
                        GenreRelationCandidate("Art Pop", 0.72f, "adjacent", "근거")
                    )
                )
            )
        )

        assertEquals(
            "장르 관계 2개가 지도에 반영되었습니다.",
            expansionFeedbackMessage(previous, next)
        )
    }

    @Test
    fun `expansion feedback is empty when confirmed relation count does not increase`() {
        val previous = GenreMapDiscoveryState(
            confirmedDiscoveries = listOf(
                com.example.vinfo.domain.model.ConfirmedGenreDiscovery(
                    sourceGenre = "Synth-pop",
                    candidates = listOf(GenreRelationCandidate("Dream Pop", 0.84f, "adjacent", "근거"))
                )
            )
        )
        val next = previous.copy(
            pendingReviews = listOf(
                GenreRelationReviewItem(
                    sourceGenre = "Jazz",
                    candidates = listOf(GenreRelationCandidate("Blues", 0.82f, "root", "근거"))
                )
            )
        )

        assertEquals(null, expansionFeedbackMessage(previous, next))
    }
}
