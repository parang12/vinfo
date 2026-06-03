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

    @Test
    fun `fetchTrackMetadata explains Gemini quota errors in Korean`() = runBlocking {
        val server = MockWebServer()
        server.start()
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody(
                    """
                    {
                      "error": {
                        "code": 429,
                        "message": "You exceeded your current quota, please check your plan and billing details."
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
            assertTrue(message.contains("Gemini API 사용량 한도를 초과했습니다."))
            assertTrue(message.contains("AI Studio"))
        } finally {
            server.shutdown()
        }
    }
}
