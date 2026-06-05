package com.example.vinfo.data.remote.gemini

import com.example.vinfo.domain.model.AppResult
import com.example.vinfo.domain.model.GenreCandidateTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiJsonParserTest {

    @Test
    fun `parseTrackMetadata returns success for valid JSON payload`() {
        val json = """
            {
              "artist": "Test Artist",
              "title": "Test Title",
              "album": "Test Album",
              "primary_genre": "Synth-pop",
              "secondary_genre": "Electronic",
              "genre_source": "LLM",
              "rym_rating": 4.2,
              "pitchfork_score": 8.4,
              "metacritic_score": 86,
              "aoty_score": 82,
              "critics_summary": "간단한 평론 요약",
              "interview_summary": "인터뷰 요약",
              "listening_guide": "들어볼 포인트",
              "samples_used": ["Sample A", "Sample B"],
              "missing_sources": ["metacritic"],
              "reliability_notes": ["앨범 단위로 확인된 평점만 사용"]
            }
        """.trimIndent()

        val parser = GeminiJsonParser()
        val result = parser.parseTrackMetadata(json)

        assertTrue(result is AppResult.Success)
        val dto = (result as AppResult.Success).data
        assertEquals("Test Artist", dto.artist)
        assertEquals("Test Title", dto.title)
        assertEquals("Test Album", dto.album)
        assertEquals("Synth-pop", dto.primaryGenre)
        assertEquals("Electronic", dto.secondaryGenre)
        assertEquals("LLM", dto.genreSource)
        assertEquals(4.2f, dto.rymRating!!, 0.001f)
        assertEquals(8.4f, dto.pitchforkScore!!, 0.001f)
        assertEquals(86, dto.metacriticScore)
        assertEquals(82, dto.aotyScore)
        assertEquals("간단한 평론 요약", dto.criticsSummary)
        assertEquals("인터뷰 요약", dto.interviewSummary)
        assertEquals("들어볼 포인트", dto.listeningGuide)
        assertEquals(2, dto.samplesUsed.size)
        assertEquals(listOf("metacritic"), dto.missingSources)
        assertEquals(listOf("앨범 단위로 확인된 평점만 사용"), dto.reliabilityNotes)
    }

    @Test
    fun `parseTrackMetadata keeps unavailable album ratings as null`() {
        val json = """
            {
              "artist": "Test Artist",
              "title": "Test Title",
              "album": "Test Album",
              "primary_genre": "Rock",
              "secondary_genre": null,
              "genre_source": "LLM",
              "rym_rating": null,
              "pitchfork_score": null,
              "metacritic_score": null,
              "aoty_score": null,
              "critics_summary": "",
              "interview_summary": null,
              "listening_guide": "",
              "samples_used": [],
              "missing_sources": ["rym", "pitchfork", "metacritic", "aoty"],
              "reliability_notes": []
            }
        """.trimIndent()

        val parser = GeminiJsonParser()
        val result = parser.parseTrackMetadata(json)

        assertTrue(result is AppResult.Success)
        val dto = (result as AppResult.Success).data
        assertEquals(null, dto.rymRating)
        assertEquals(null, dto.pitchforkScore)
        assertEquals(null, dto.metacriticScore)
        assertEquals(null, dto.aotyScore)
        assertEquals(null, dto.secondaryGenre)
        assertEquals(null, dto.interviewSummary)
        assertEquals(listOf("rym", "pitchfork", "metacritic", "aoty"), dto.missingSources)
    }

    @Test
    fun `parseTrackMetadata accepts rating strings with score denominators`() {
        val json = """
            {
              "artist": "Kanye West",
              "title": "See Me Now",
              "album": "My Beautiful Dark Twisted Fantasy",
              "primary_genre": "Hip Hop",
              "secondary_genre": "Pop Rap",
              "genre_source": "LLM",
              "rym_rating": "4.05/5",
              "pitchfork_score": "10/10",
              "metacritic_score": "94/100",
              "aoty_score": "85/100",
              "critics_summary": "앨범 기준 평론",
              "interview_summary": null,
              "listening_guide": "앨범 맥락 감상 포인트",
              "samples_used": [],
              "missing_sources": [],
              "reliability_notes": []
            }
        """.trimIndent()

        val parser = GeminiJsonParser()
        val result = parser.parseTrackMetadata(json)

        assertTrue(result is AppResult.Success)
        val dto = (result as AppResult.Success).data
        assertEquals(4.05f, dto.rymRating!!, 0.001f)
        assertEquals(10f, dto.pitchforkScore!!, 0.001f)
        assertEquals(94, dto.metacriticScore)
        assertEquals(85, dto.aotyScore)
    }

    @Test
    fun `parseTrackMetadata reads album genre candidate arrays with confidence`() {
        val json = """
            {
              "artist": "Migos",
              "title": "T-Shirt",
              "album": "Culture",
              "primary_genres": [
                {
                  "name": "Trap",
                  "confidence": 0.94,
                  "evidence_text": "Culture is commonly categorized as trap at album level."
                }
              ],
              "secondary_genres": [
                {
                  "name": "Southern Hip Hop",
                  "confidence": 0.76
                }
              ],
              "microgenres": [
                {
                  "name": "Pop Rap",
                  "confidence": 0.41
                }
              ],
              "genre_source": "LLM",
              "rym_rating": null,
              "pitchfork_score": null,
              "metacritic_score": null,
              "aoty_score": null,
              "critics_summary": "앨범 기준 요약",
              "interview_summary": null,
              "listening_guide": "앨범 감상 포인트",
              "samples_used": [],
              "missing_sources": [],
              "reliability_notes": []
            }
        """.trimIndent()

        val parser = GeminiJsonParser()
        val result = parser.parseTrackMetadata(json)

        assertTrue(result is AppResult.Success)
        val dto = (result as AppResult.Success).data
        assertEquals("Trap", dto.primaryGenre)
        assertEquals("Southern Hip Hop", dto.secondaryGenre)
        assertEquals(3, dto.genreCandidates.size)
        assertEquals(GenreCandidateTier.PRIMARY, dto.genreCandidates[0].tier)
        assertEquals(0.94f, dto.genreCandidates[0].confidence, 0.001f)
        assertEquals("Culture is commonly categorized as trap at album level.", dto.genreCandidates[0].evidenceText)
        assertEquals(GenreCandidateTier.SECONDARY, dto.genreCandidates[1].tier)
        assertEquals(GenreCandidateTier.MICRO, dto.genreCandidates[2].tier)
    }

    @Test
    fun `parseTrackMetadata accepts common Gemini key aliases for artist title and album`() {
        val json = """
            {
              "artist_name": "Kanye West",
              "track_title": "See Me Now",
              "album_title": "My Beautiful Dark Twisted Fantasy",
              "primary_genres": [
                { "name": "Progressive Rap", "confidence": 0.91 }
              ],
              "secondary_genres": [],
              "microgenres": [],
              "genre_source": "LLM",
              "rym_rating": null,
              "pitchfork_score": null,
              "metacritic_score": null,
              "aoty_score": null,
              "review_summary": "앨범 기준 요약",
              "interview_summary": null,
              "listening_guide": "앨범 맥락 감상 포인트",
              "samples_used": [],
              "missing_sources": [],
              "reliability_notes": []
            }
        """.trimIndent()

        val parser = GeminiJsonParser()
        val result = parser.parseTrackMetadata(json)

        assertTrue(result is AppResult.Success)
        val dto = (result as AppResult.Success).data
        assertEquals("Kanye West", dto.artist)
        assertEquals("See Me Now", dto.title)
        assertEquals("My Beautiful Dark Twisted Fantasy", dto.album)
        assertEquals("앨범 기준 요약", dto.criticsSummary)
    }

    @Test
    fun `parseTrackMetadata uses request values when Gemini omits artist and title`() {
        val json = """
            {
              "artist": "Wrong Artist",
              "title": "Wrong Title",
              "album": "My Beautiful Dark Twisted Fantasy",
              "primary_genres": [
                { "name": "Progressive Rap", "confidence": 0.91 }
              ],
              "secondary_genres": [],
              "microgenres": [],
              "genre_source": "LLM",
              "rym_rating": null,
              "pitchfork_score": null,
              "metacritic_score": null,
              "aoty_score": null,
              "critics_summary": "앨범 기준 요약",
              "interview_summary": null,
              "listening_guide": "앨범 맥락 감상 포인트",
              "samples_used": [],
              "missing_sources": [],
              "reliability_notes": []
            }
        """.trimIndent()

        val parser = GeminiJsonParser()
        val result = parser.parseTrackMetadata(
            rawResponse = json,
            fallbackArtist = "Kanye West",
            fallbackTitle = "See Me Now",
            fallbackAlbum = null
        )

        assertTrue(result is AppResult.Success)
        val dto = (result as AppResult.Success).data
        assertEquals("Kanye West", dto.artist)
        assertEquals("See Me Now", dto.title)
        assertEquals("My Beautiful Dark Twisted Fantasy", dto.album)
    }

    @Test
    fun `parseTrackMetadata accepts alternate genre arrays and nested ratings`() {
        val json = """
            {
              "album_title": "The Life of Pablo",
              "genres": [
                { "name": "Experimental Hip Hop", "confidence": 0.89 },
                { "name": "Gospel Rap", "confidence": 0.78 }
              ],
              "ratings": {
                "rym": "3.47/5",
                "pitchfork": "9.0/10",
                "metacritic": "75/100",
                "aoty": "78/100"
              },
              "review_summary": "앨범 기준 요약",
              "listening_guide": "앨범 맥락 감상 포인트"
            }
        """.trimIndent()

        val parser = GeminiJsonParser()
        val result = parser.parseTrackMetadata(
            rawResponse = json,
            fallbackArtist = "Kanye West",
            fallbackTitle = "Ultralight Beam",
            fallbackAlbum = null
        )

        assertTrue(result is AppResult.Success)
        val dto = (result as AppResult.Success).data
        assertEquals("The Life of Pablo", dto.album)
        assertEquals("Experimental Hip Hop", dto.primaryGenre)
        assertEquals("Gospel Rap", dto.secondaryGenre)
        assertEquals(3.47f, dto.rymRating!!, 0.001f)
        assertEquals(9.0f, dto.pitchforkScore!!, 0.001f)
        assertEquals(75, dto.metacriticScore)
        assertEquals(78, dto.aotyScore)
    }

    @Test
    fun `parseTrackMetadata rejects Gemini envelope when candidate text has no metadata JSON`() {
        val json = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "I cannot verify the album ratings from the available sources."
                      }
                    ]
                  },
                  "finishReason": "STOP"
                }
              ],
              "usageMetadata": {
                "promptTokenCount": 100,
                "candidatesTokenCount": 12
              }
            }
        """.trimIndent()

        val result = GeminiJsonParser().parseTrackMetadata(
            json,
            fallbackArtist = "Kanye West",
            fallbackTitle = "Dark Fantasy",
            fallbackAlbum = null
        )

        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `parseTrackMetadata accepts singular and album genre aliases`() {
        val json = """
            {
              "artist": "Kanye West",
              "title": "Through the Wire",
              "album": "The College Dropout",
              "genre": "Chipmunk Soul",
              "album_genres": ["Hip Hop", "Pop Rap"],
              "ratings": {
                "rateYourMusic": "3.68 / 5",
                "pitchforkScore": "8.2 / 10"
              },
              "summary": "소울 샘플과 회고적 랩 서사가 결합된 데뷔 앨범.",
              "guide": "보컬 샘플의 질감과 드럼 배치를 중심으로 감상한다.",
              "samples": ["Chaka Khan - Through the Fire"]
            }
        """.trimIndent()

        val result = GeminiJsonParser().parseTrackMetadata(json)

        assertTrue(result is AppResult.Success)
        val dto = (result as AppResult.Success).data
        assertEquals("Chipmunk Soul", dto.primaryGenre)
        assertEquals("Hip Hop", dto.secondaryGenre)
        assertEquals(3.68f, dto.rymRating ?: 0f, 0.001f)
        assertEquals(8.2f, dto.pitchforkScore ?: 0f, 0.001f)
        assertEquals("소울 샘플과 회고적 랩 서사가 결합된 데뷔 앨범.", dto.criticsSummary)
        assertEquals("보컬 샘플의 질감과 드럼 배치를 중심으로 감상한다.", dto.listeningGuide)
        assertEquals(listOf("Chaka Khan - Through the Fire"), dto.samplesUsed)
    }
}
