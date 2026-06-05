package com.example.vinfo.data.repository

import com.example.vinfo.data.local.dao.LyricsCacheDao
import com.example.vinfo.data.local.entity.LyricsCacheEntity
import com.example.vinfo.domain.model.AppResult
import com.example.vinfo.domain.repository.RawLyricsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CachedLyricsRepositoryTest {

    @Test
    fun `getRawLyrics returns cached lyrics without calling remote lyrics api`() = runBlocking {
        val dao = FakeLyricsCacheDao(
            cached = LyricsCacheEntity(
                id = "cached-id",
                artist = "Kanye West",
                title = "Runaway",
                lyrics = "Cached lyrics",
                updatedAtMillis = 0L
            )
        )
        val remote = CountingRawLyricsRepository()
        val repository = CachedLyricsRepository(dao, remote)

        val result = repository.getRawLyrics("kanye west", "runaway")

        assertTrue(result is AppResult.Success<*>)
        assertEquals("Cached lyrics", (result as AppResult.Success).data)
        assertEquals(0, remote.callCount)
    }

    @Test
    fun `getRawLyrics stores successful remote lyrics for later cache hits`() = runBlocking {
        val dao = FakeLyricsCacheDao()
        val remote = CountingRawLyricsRepository(AppResult.Success("Fresh lyrics"))
        val repository = CachedLyricsRepository(dao, remote)

        val result = repository.getRawLyrics("Kanye West", "Runaway")

        assertTrue(result is AppResult.Success<*>)
        assertEquals(1, remote.callCount)
        assertEquals("Kanye West", dao.insertedLyrics?.artist)
        assertEquals("Runaway", dao.insertedLyrics?.title)
        assertEquals("Fresh lyrics", dao.insertedLyrics?.lyrics)
    }

    private class CountingRawLyricsRepository(
        private val result: AppResult<String> = AppResult.Error("remote should not be called")
    ) : RawLyricsRepository {
        var callCount = 0
            private set

        override suspend fun getRawLyrics(artist: String, title: String): AppResult<String> {
            callCount += 1
            return result
        }
    }

    private class FakeLyricsCacheDao(
        private val cached: LyricsCacheEntity? = null
    ) : LyricsCacheDao {
        var insertedLyrics: LyricsCacheEntity? = null
            private set

        override suspend fun getLyricsById(id: String): LyricsCacheEntity? = cached

        override suspend fun insertLyrics(entity: LyricsCacheEntity) {
            insertedLyrics = entity
        }

        override suspend fun deleteAllLyrics() = Unit
    }
}
