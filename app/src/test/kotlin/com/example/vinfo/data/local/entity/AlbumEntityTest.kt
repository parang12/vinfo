package com.example.vinfo.data.local.entity

import com.example.vinfo.domain.model.AlbumGenreCandidate
import com.example.vinfo.domain.model.GenreCandidateTier
import com.example.vinfo.domain.model.GenreCategory
import com.example.vinfo.domain.model.GenreSource
import com.example.vinfo.domain.model.NowPlayingTrack
import com.example.vinfo.domain.model.TrackMetadata
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumEntityTest {

    @Test
    fun `fromTrackSnapshot stores genre candidates and restores detail metadata`() {
        val track = NowPlayingTrack(
            artist = "Migos",
            title = "T-Shirt",
            album = "Culture",
            albumArtUrl = "file:///album-art/culture.jpg",
            sourcePackageName = "com.test.player"
        )
        val metadata = TrackMetadata(
            artist = "Migos",
            title = "T-Shirt",
            album = "Culture",
            primaryGenre = GenreCategory.TRAP,
            secondaryGenre = GenreCategory.HIP_HOP,
            genreCandidates = listOf(
                AlbumGenreCandidate("Trap", 0.94f, GenreCandidateTier.PRIMARY, "album-level style"),
                AlbumGenreCandidate("Southern Hip Hop", 0.76f, GenreCandidateTier.SECONDARY)
            ),
            genreSource = GenreSource.LLM,
            rymRating = 3.72f,
            pitchforkScore = null,
            metacriticScore = 79,
            aotyScore = null,
            criticsSummary = "앨범 기준 평론",
            interviewSummary = null,
            listeningGuide = "앨범 감상 포인트",
            samplesUsed = emptyList(),
            missingSources = listOf("pitchfork"),
            reliabilityNotes = listOf("album only")
        )

        val entity = AlbumEntity.fromTrackSnapshot("track-id", track, metadata, savedAtMillis = 0L)
        val restored = entity.toTrackMetadata()

        assertEquals("Culture", entity.albumTitle)
        assertEquals("Trap", entity.genres.first())
        assertEquals(2, restored.genreCandidates.size)
        assertEquals("Trap", restored.genreCandidates[0].name)
        assertEquals(GenreCandidateTier.PRIMARY, restored.genreCandidates[0].tier)
        assertEquals(0.94f, restored.genreCandidates[0].confidence, 0.001f)
        assertEquals("album-level style", restored.genreCandidates[0].evidenceText)
        assertEquals(GenreSource.LLM, restored.genreSource)
        assertEquals(listOf("pitchfork"), restored.missingSources)
    }
}
