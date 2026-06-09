package com.example.vinfo.data.remote.gemini

import com.example.vinfo.data.local.dao.GenreRelationCacheDao
import com.example.vinfo.data.local.entity.GenreRelationCacheEntity
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

    @Test
    fun `discoverNearbyGenres returns cached candidates without calling Gemini`() = runBlocking {
        val dao = FakeGenreRelationCacheDao(
            cached = GenreRelationCacheEntity.fromCandidates(
                sourceGenre = "R&B",
                candidates = listOf(
                    com.example.vinfo.domain.model.GenreRelationCandidate(
                        genreName = "Soul",
                        score = 0.84f,
                        relationType = "cached",
                        evidence = "Cached relation"
                    )
                ),
                updatedAtMillis = 0L
            )
        )
        val repository = GeminiGenreRelationDiscoveryRepository(
            cacheDao = dao,
            serviceFactory = {
                throw AssertionError("Gemini should not be called when cached nearby genres exist")
            }
        )

        val result = repository.discoverNearbyGenres("R & B", "test-key")

        assertTrue(result is AppResult.Success)
        val candidates = (result as AppResult.Success).data
        assertEquals(listOf("Soul"), candidates.map { it.genreName })
        assertEquals(0, dao.insertCount)
    }

    @Test
    fun `discoverNearbyGenres stores successful Gemini candidates as pending review cache`() = runBlocking {
        val server = MockWebServer()
        server.start()
        server.enqueue(
            MockResponse().setBody(
                """
                    {
                      "selected_genre": "Jazz",
                      "nearby_genres": [
                        {
                          "genre": "Blues",
                          "relation_strength": 0.82,
                          "relation_type": "root",
                          "evidence": "Jazz and Blues share documented roots."
                        }
                      ],
                      "reliability_notes": []
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
        val dao = FakeGenreRelationCacheDao()
        val repository = GeminiGenreRelationDiscoveryRepository(
            cacheDao = dao,
            serviceFactory = { service }
        )

        val result = repository.discoverNearbyGenres("Jazz", "test-key")

        assertTrue(result is AppResult.Success)
        assertEquals(1, dao.insertCount)
        assertEquals("jazz", dao.inserted?.sourceGenreKey)
        assertEquals("PENDING", dao.inserted?.reviewStatus)
        assertEquals(listOf("Blues"), dao.inserted?.toCandidates()?.map { it.genreName })
        server.shutdown()
    }

    private class FakeGenreRelationCacheDao(
        private val cached: GenreRelationCacheEntity? = null
    ) : GenreRelationCacheDao {
        var inserted: GenreRelationCacheEntity? = null
            private set
        var insertCount = 0
            private set

        override suspend fun getBySourceGenreKey(sourceGenreKey: String): GenreRelationCacheEntity? = cached

        override suspend fun insert(entity: GenreRelationCacheEntity) {
            inserted = entity
            insertCount += 1
        }

        override suspend fun getPendingReview(): List<GenreRelationCacheEntity> = cached?.let(::listOf).orEmpty()

        override suspend fun markReviewed(sourceGenreKey: String, reviewedAtMillis: Long) = Unit
    }
}
