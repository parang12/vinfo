package com.example.vinfo.data.remote.gemini

import com.example.vinfo.domain.model.GenreMapper
import com.example.vinfo.domain.model.TrackMetadata
import com.example.vinfo.domain.model.GenreSource
import com.example.vinfo.domain.model.GenreCategory
import com.example.vinfo.domain.model.AppResult
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class GeminiRepositoryIntegrationTest {

    @Test
    fun `end-to-end parse via Retrofit+MockWebServer`() = runBlocking {
        val server = MockWebServer()
        server.start()

        val json = """
            {
              "artist": "Integration Artist",
              "title": "Integration Title",
              "album": "Integration Album",
              "primary_genre": "Synth-pop",
              "secondary_genre": "Electronic",
              "genre_source": "LLM",
              "rym_rating": 4.5,
              "pitchfork_score": 8.1,
              "metacritic_score": 84,
              "aoty_score": null,
              "critics_summary": "통합 테스트 평론",
              "interview_summary": "인터뷰 요약",
              "listening_guide": "주목할 포인트",
              "samples_used": ["Sample X"],
              "missing_sources": ["aoty"],
              "reliability_notes": ["앨범 기준"]
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

        val service = retrofit.create(GeminiApiService::class.java)

        val requestBody = RequestBody.create("application/json".toMediaType(), "{}")
        val raw = service.generate("gemini-1.0", "test-key", requestBody)

        val parser = GeminiJsonParser()
        val parseResult = parser.parseTrackMetadata(raw)

        assertTrue(parseResult is AppResult.Success)
        val dto = (parseResult as AppResult.Success).data

        // map to domain manually (same logic as repository conversion)
        val domain = TrackMetadata(
            artist = dto.artist,
            title = dto.title,
            album = dto.album,
            primaryGenre = GenreMapper.fromRawGenre(dto.primaryGenre),
            secondaryGenre = dto.secondaryGenre?.let(GenreMapper::fromRawGenre),
            genreSource = GenreMapper.fromRawSource(dto.genreSource),
            rymRating = dto.rymRating,
            pitchforkScore = dto.pitchforkScore,
            metacriticScore = dto.metacriticScore,
            aotyScore = dto.aotyScore,
            criticsSummary = dto.criticsSummary,
            interviewSummary = dto.interviewSummary,
            listeningGuide = dto.listeningGuide,
            samplesUsed = dto.samplesUsed,
            missingSources = dto.missingSources,
            reliabilityNotes = dto.reliabilityNotes
        )

        assertEquals("Integration Artist", domain.artist)
        assertEquals("Integration Title", domain.title)
        assertEquals(GenreCategory.POP, domain.primaryGenre)
        assertEquals(GenreSource.LLM, domain.genreSource)
        assertEquals(4.5f, domain.rymRating!!, 0.001f)
        assertEquals(8.1f, domain.pitchforkScore!!, 0.001f)
        assertEquals(84, domain.metacriticScore)
        assertEquals(null, domain.aotyScore)
        assertEquals(listOf("aoty"), domain.missingSources)
        assertEquals(1, domain.samplesUsed.size)

        server.shutdown()
    }
}
