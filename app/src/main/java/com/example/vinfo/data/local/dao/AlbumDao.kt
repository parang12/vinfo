package com.example.vinfo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.vinfo.data.local.entity.AlbumEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY date DESC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity)

    @Query("DELETE FROM albums WHERE id IN (:ids)")
    suspend fun deleteAlbums(ids: List<String>)

    @Query("DELETE FROM albums")
    suspend fun deleteAllAlbums()

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getAlbumById(id: String): AlbumEntity?
}
