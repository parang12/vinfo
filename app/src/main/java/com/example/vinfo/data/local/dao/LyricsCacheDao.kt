package com.example.vinfo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.vinfo.data.local.entity.LyricsCacheEntity

@Dao
interface LyricsCacheDao {
    @Query("SELECT * FROM lyrics_cache WHERE id = :id")
    suspend fun getLyricsById(id: String): LyricsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(entity: LyricsCacheEntity)

    @Query("DELETE FROM lyrics_cache")
    suspend fun deleteAllLyrics()
}
