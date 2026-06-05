package com.example.vinfo.ui.detail

import com.example.vinfo.domain.model.AlbumGenreCandidate
import com.example.vinfo.domain.model.GenreCandidateTier
import com.example.vinfo.domain.model.GenreCategory
import com.example.vinfo.domain.model.GenreSource
import com.example.vinfo.domain.model.TrackMetadata
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailGenreLabelsTest {

    @Test
    fun `buildGenreLabels shows specific album genre candidates before broad categories`() {
        val metadata = TrackMetadata(
            artist = "Kanye West",
            title = "Dark Fantasy",
            album = "My Beautiful Dark Twisted Fantasy",
            primaryGenre = GenreCategory.HIP_HOP,
            secondaryGenre = null,
            genreCandidates = listOf(
                AlbumGenreCandidate("Progressive Rap", 0.91f, GenreCandidateTier.PRIMARY),
                AlbumGenreCandidate("Art Pop", 0.72f, GenreCandidateTier.SECONDARY),
                AlbumGenreCandidate("Orchestral", 0.51f, GenreCandidateTier.MICRO)
            ),
            genreSource = GenreSource.LLM,
            rymRating = 4.05f,
            pitchforkScore = 10f,
            metacriticScore = 94,
            aotyScore = 85,
            criticsSummary = "앨범 기준 평론",
            interviewSummary = null,
            listeningGuide = "앨범 감상 포인트",
            samplesUsed = emptyList(),
            missingSources = emptyList(),
            reliabilityNotes = emptyList()
        )

        assertEquals(
            listOf("Progressive Rap", "Art Pop"),
            buildGenreLabels(metadata)
        )
    }
}
