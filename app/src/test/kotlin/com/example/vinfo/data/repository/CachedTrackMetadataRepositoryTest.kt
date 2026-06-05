package com.example.vinfo.data.repository

import com.example.vinfo.data.local.dao.AlbumDao
import com.example.vinfo.data.local.entity.AlbumEntity
import com.example.vinfo.domain.model.AppResult
import com.example.vinfo.domain.model.GenreCategory
import com.example.vinfo.domain.model.GenreSource
import com.example.vinfo.domain.model.TrackMetadata
import com.example.vinfo.domain.repository.TrackMetadataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CachedTrackMetadataRepositoryTest {

    @Test
    fun `fetchTrackMetadata returns cached album metadata without calling Gemini`() = runBlocking {
        val cachedEntity = AlbumEntity(
            id = "cached-id",
            albumTitle = "Culture",
            artist = "Migos",
            album = "Culture",
            genres = listOf("Trap"),
            primaryGenre = "Trap",
            genreSource = GenreSource.LLM.name,
            criticsSummary = "캐시된 앨범 평론",
            listeningGuide = "캐시된 감상 가이드",
            date = "2026.06.05"
        )
        val dao = FakeAlbumDao(cachedByAlbum = cachedEntity)
        val remote = CountingTrackMetadataRepository()
        val repository = CachedTrackMetadataRepository(dao, remote)

        val result = repository.fetchTrackMetadata(
            artist = "migos",
            title = "T-Shirt",
            album = "culture",
            apiKey = "test-key"
        )

        assertTrue(result is AppResult.Success<*>)
        val metadata = (result as AppResult.Success).data
        assertEquals("Culture", metadata.album)
        assertEquals(GenreCategory.TRAP, metadata.primaryGenre)
        assertEquals("캐시된 앨범 평론", metadata.criticsSummary)
        assertEquals(0, remote.callCount)
    }

    @Test
    fun `fetchTrackMetadata stores successful Gemini result for later album cache hits`() = runBlocking {
        val dao = FakeAlbumDao()
        val remote = CountingTrackMetadataRepository(
            result = AppResult.Success(
                TrackMetadata(
                    artist = "Migos",
                    title = "T-Shirt",
                    album = "Culture",
                    primaryGenre = GenreCategory.TRAP,
                    secondaryGenre = GenreCategory.HIP_HOP,
                    genreSource = GenreSource.LLM,
                    rymRating = null,
                    pitchforkScore = null,
                    metacriticScore = null,
                    aotyScore = null,
                    criticsSummary = "새로 받은 앨범 평론",
                    interviewSummary = null,
                    listeningGuide = "새로 받은 감상 가이드",
                    samplesUsed = emptyList(),
                    missingSources = emptyList(),
                    reliabilityNotes = emptyList()
                )
            )
        )
        val repository = CachedTrackMetadataRepository(dao, remote)

        val result = repository.fetchTrackMetadata(
            artist = "Migos",
            title = "T-Shirt",
            album = "Culture",
            apiKey = "test-key"
        )

        assertTrue(result is AppResult.Success<*>)
        assertEquals(1, remote.callCount)
        assertEquals("Culture", dao.insertedAlbum?.albumTitle)
        assertEquals("Trap", dao.insertedAlbum?.primaryGenre)
    }

    @Test
    fun `fetchTrackMetadata ignores incomplete cached album and refreshes Gemini`() = runBlocking {
        val incompleteCache = AlbumEntity(
            id = "bad-cache",
            albumTitle = "The Life of Pablo",
            artist = "Kanye West",
            album = "The Life of Pablo",
            genres = listOf("Unknown"),
            primaryGenre = null,
            secondaryGenre = null,
            rymRating = null,
            pitchforkScore = null,
            metacriticScore = null,
            aotyScore = null,
            criticsSummary = null,
            listeningGuide = null,
            date = "2026.06.05"
        )
        val dao = FakeAlbumDao(cachedByAlbum = incompleteCache)
        val remote = CountingTrackMetadataRepository(
            result = AppResult.Success(
                TrackMetadata(
                    artist = "Kanye West",
                    title = "Ultralight Beam",
                    album = "The Life of Pablo",
                    primaryGenre = GenreCategory.HIP_HOP,
                    secondaryGenre = null,
                    genreSource = GenreSource.LLM,
                    rymRating = 3.47f,
                    pitchforkScore = 9.0f,
                    metacriticScore = 75,
                    aotyScore = 78,
                    criticsSummary = "새로 받은 앨범 평론",
                    interviewSummary = null,
                    listeningGuide = "새로 받은 감상 가이드",
                    samplesUsed = emptyList(),
                    missingSources = emptyList(),
                    reliabilityNotes = emptyList()
                )
            )
        )
        val repository = CachedTrackMetadataRepository(dao, remote)

        val result = repository.fetchTrackMetadata(
            artist = "Kanye West",
            title = "Ultralight Beam",
            album = "The Life of Pablo",
            apiKey = "test-key"
        )

        assertTrue(result is AppResult.Success<*>)
        assertEquals(1, remote.callCount)
        assertEquals("Hip Hop", dao.insertedAlbum?.primaryGenre)
        assertEquals(3.47f, dao.insertedAlbum?.rymRating ?: 0f, 0.001f)
    }

    @Test
    fun `fetchTrackMetadata refreshes cached album that has only broad genre without details`() = runBlocking {
        val broadOnlyCache = AlbumEntity(
            id = "broad-cache",
            albumTitle = "My Beautiful Dark Twisted Fantasy",
            artist = "Kanye West",
            album = "My Beautiful Dark Twisted Fantasy",
            genres = listOf("Hip Hop"),
            primaryGenre = "Hip Hop",
            secondaryGenre = null,
            rymRating = null,
            pitchforkScore = null,
            metacriticScore = null,
            aotyScore = null,
            criticsSummary = null,
            listeningGuide = null,
            samplesUsedJson = "[]",
            date = "2026.06.05"
        )
        val dao = FakeAlbumDao(cachedByAlbum = broadOnlyCache)
        val remote = CountingTrackMetadataRepository(
            result = AppResult.Success(
                TrackMetadata(
                    artist = "Kanye West",
                    title = "Dark Fantasy",
                    album = "My Beautiful Dark Twisted Fantasy",
                    primaryGenre = GenreCategory.HIP_HOP,
                    secondaryGenre = null,
                    genreSource = GenreSource.LLM,
                    rymRating = 4.05f,
                    pitchforkScore = 10.0f,
                    metacriticScore = 94,
                    aotyScore = 85,
                    criticsSummary = "새로 받은 앨범 평론",
                    interviewSummary = null,
                    listeningGuide = "새로 받은 감상 가이드",
                    samplesUsed = listOf("King Crimson - 21st Century Schizoid Man"),
                    missingSources = emptyList(),
                    reliabilityNotes = emptyList()
                )
            )
        )
        val repository = CachedTrackMetadataRepository(dao, remote)

        val result = repository.fetchTrackMetadata(
            artist = "Kanye West",
            title = "Dark Fantasy",
            album = "My Beautiful Dark Twisted Fantasy",
            apiKey = "test-key"
        )

        assertTrue(result is AppResult.Success<*>)
        assertEquals(1, remote.callCount)
        assertEquals(4.05f, dao.insertedAlbum?.rymRating ?: 0f, 0.001f)
        assertEquals("새로 받은 앨범 평론", dao.insertedAlbum?.criticsSummary)
    }

    private class CountingTrackMetadataRepository(
        private val result: AppResult<TrackMetadata> = AppResult.Error("remote should not be called")
    ) : TrackMetadataRepository {
        var callCount = 0
            private set

        override suspend fun fetchTrackMetadata(
            artist: String,
            title: String,
            album: String?,
            apiKey: String
        ): AppResult<TrackMetadata> {
            callCount += 1
            return result
        }
    }

    private class FakeAlbumDao(
        private val cachedByAlbum: AlbumEntity? = null,
        private val cachedById: AlbumEntity? = null
    ) : AlbumDao {
        var insertedAlbum: AlbumEntity? = null
            private set

        override fun getAllAlbums(): Flow<List<AlbumEntity>> = flowOf(emptyList())

        override suspend fun insertAlbum(album: AlbumEntity) {
            insertedAlbum = album
        }

        override suspend fun deleteAlbums(ids: List<String>) = Unit

        override suspend fun deleteAllAlbums() = Unit

        override suspend fun getAlbumById(id: String): AlbumEntity? = cachedById

        override suspend fun findAlbumByArtistAndAlbum(
            artist: String,
            albumTitle: String
        ): AlbumEntity? = cachedByAlbum
    }
}
