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

class GeminiTrackMetadataRepositoryTest {
    @Test
    fun `fetchTrackMetadata exposes Gemini HTTP error body instead of generic failure`() = runBlocking {
        val server = MockWebServer()
        server.start()
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody(
                    """
                    {
                      "error": {
                        "code": 404,
                        "message": "models/gemini-test is not found for API version v1beta"
                      }
                    }
                    """.trimIndent()
                )
        )

        try {
            val service = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(OkHttpClient())
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create(GeminiApiService::class.java)
            val repository = GeminiTrackMetadataRepository(serviceFactory = { service })

            val result = repository.fetchTrackMetadata(
                artist = "Artist",
                title = "Title",
                album = null,
                apiKey = "test-key"
            )

            assertTrue(result is AppResult.Error)
            val message = (result as AppResult.Error).message
            assertTrue(message.contains("Gemini API 오류 404"))
            assertTrue(message.contains("models/gemini-test is not found"))
        } finally {
            server.shutdown()
        }
    }
}
