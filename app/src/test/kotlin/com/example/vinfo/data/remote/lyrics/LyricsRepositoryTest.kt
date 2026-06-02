package com.example.vinfo.data.remote.lyrics

import com.example.vinfo.domain.model.AppResult
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class LyricsRepositoryTest {

    @Test
    fun `getRawLyrics returns lyrics from lyrics ovh response`() = runBlocking {
        val server = MockWebServer()
        server.start()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"lyrics":"First line\nSecond line"}""")
        )

        val repository = LyricsRepository(createService(server))
        val result = repository.getRawLyrics("Test Artist", "Test Title")

        assertTrue(result is AppResult.Success)
        assertEquals("First line\nSecond line", (result as AppResult.Success).data)
        assertEquals("/v1/Test%20Artist/Test%20Title", server.takeRequest().path)
        server.shutdown()
    }

    @Test
    fun `getRawLyrics returns section-level error when lyrics are unavailable`() = runBlocking {
        val server = MockWebServer()
        server.start()
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"No lyrics found"}"""))

        val repository = LyricsRepository(createService(server))
        val result = repository.getRawLyrics("Unknown Artist", "Unknown Title")

        assertTrue(result is AppResult.Error)
        assertEquals("가사를 찾을 수 없습니다.", (result as AppResult.Error).message)
        server.shutdown()
    }

    private fun createService(server: MockWebServer): LyricsApiService {
        return Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(LyricsApiService::class.java)
    }
}
