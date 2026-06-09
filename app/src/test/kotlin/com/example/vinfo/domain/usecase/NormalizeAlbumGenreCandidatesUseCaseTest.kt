package com.example.vinfo.domain.usecase

import com.example.vinfo.domain.model.AlbumGenreCandidate
import com.example.vinfo.domain.model.GenreCandidateTier
import com.example.vinfo.domain.model.GenreRoot
import com.example.vinfo.domain.model.NormalizedGenreStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NormalizeAlbumGenreCandidatesUseCaseTest {

    private val useCase = NormalizeAlbumGenreCandidatesUseCase()

    @Test
    fun `normalizes specific album genres and keeps at most three representative genres`() {
        val result = useCase(
            candidates = listOf(
                AlbumGenreCandidate("Synth Pop", 0.92f, GenreCandidateTier.PRIMARY),
                AlbumGenreCandidate("Neo-Psychedelia", 0.88f, GenreCandidateTier.PRIMARY),
                AlbumGenreCandidate("Art Pop", 0.74f, GenreCandidateTier.SECONDARY),
                AlbumGenreCandidate("Pop", 0.71f, GenreCandidateTier.SECONDARY)
            )
        )

        assertEquals(listOf("Synth-pop", "Neo-Psychedelia", "Art Pop"), result.representativeGenres)
        assertEquals(GenreRoot.POP, result.accepted.first { it.displayName == "Synth-pop" }.root)
        assertEquals(GenreRoot.POP, result.accepted.first { it.displayName == "Art Pop" }.root)
        assertFalse(result.accepted.any { it.displayName == "Pop" })
    }

    @Test
    fun `rejects unknown and unrooted emerging genres from visible map genres`() {
        val result = useCase(
            candidates = listOf(
                AlbumGenreCandidate("Unknown", 0.99f, GenreCandidateTier.PRIMARY),
                AlbumGenreCandidate("Unknown Internet Post-Genre", 0.97f, GenreCandidateTier.PRIMARY),
                AlbumGenreCandidate("Trap Rap", 0.90f, GenreCandidateTier.PRIMARY)
            )
        )

        assertEquals(listOf("Trap"), result.representativeGenres)
        assertTrue(result.accepted.any { it.displayName == "Trap" && it.root == GenreRoot.HIP_HOP })
        assertTrue(result.rejected.any { it.rawName == "Unknown" })
        assertTrue(result.rejected.any { it.rawName == "Unknown Internet Post-Genre" })
    }

    @Test
    fun `deduplicates aliases by strongest normalized genre`() {
        val result = useCase(
            candidates = listOf(
                AlbumGenreCandidate("R & B", 0.62f, GenreCandidateTier.SECONDARY),
                AlbumGenreCandidate("Contemporary R&B", 0.86f, GenreCandidateTier.PRIMARY),
                AlbumGenreCandidate("Neo Soul", 0.77f, GenreCandidateTier.SECONDARY)
            )
        )

        assertEquals(listOf("R&B", "Neo Soul"), result.representativeGenres)
        assertEquals(0.86f, result.accepted.first { it.displayName == "R&B" }.confidence, 0.001f)
        assertEquals(NormalizedGenreStatus.VERIFIED, result.accepted.first { it.displayName == "Neo Soul" }.status)
    }
}
