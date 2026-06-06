package com.example.vinfo.data.remote.gemini

import com.example.vinfo.domain.model.AppResult
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class GeminiGenreRelationDiscoveryRepositoryTest {

    @Test
    fun `discoverNearbyGenres returns curated map relations for known genre without api key`() = runBlocking {
        val repository = GeminiGenreRelationDiscoveryRepository()

        val result = repository.discoverNearbyGenres("Funk", "")

        assertTrue(result is AppResult.Success)
        val candidates = (result as AppResult.Success).data
        assertTrue(candidates.any { it.genreName == "Soul" })
        assertTrue(candidates.any { it.genreName == "Hip-Hop" })
    }

    @Test
    fun `discoverNearbyGenres falls back to curated map relations when Gemini returns no candidates`() = runBlocking {
        val server = MockWebServer()
        server.start()
        server.enqueue(
            MockResponse().setBody(
                """
                    {
                      "selected_genre": "R&B",
                      "nearby_genres": [],
                      "reliability_notes": ["No grounded candidates"]
                    }
                """.trimIndent()
            )
        )
        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
        val repository = GeminiGenreRelationDiscoveryRepository(serviceFactory = { service })

        val result = repository.discoverNearbyGenres("R&B", "test-key")

        assertTrue(result is AppResult.Success)
        val candidates = (result as AppResult.Success).data
        assertTrue(candidates.any { it.genreName == "Soul" })
        assertTrue(candidates.any { it.genreName == "Hip-Hop" })
        server.shutdown()
    }
}
