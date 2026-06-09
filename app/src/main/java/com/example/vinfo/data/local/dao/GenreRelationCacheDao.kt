package com.example.vinfo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.vinfo.data.local.entity.GenreRelationCacheEntity

@Dao
interface GenreRelationCacheDao {
    @Query("SELECT * FROM genre_relation_cache WHERE source_genre_key = :sourceGenreKey")
    suspend fun getBySourceGenreKey(sourceGenreKey: String): GenreRelationCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GenreRelationCacheEntity)

    @Query(
        """
        SELECT * FROM genre_relation_cache
        WHERE review_status = 'PENDING'
        ORDER BY updated_at_millis DESC
        """
    )
    suspend fun getPendingReview(): List<GenreRelationCacheEntity>

    @Query(
        """
        UPDATE genre_relation_cache
        SET review_status = 'CONFIRMED',
            reviewed_at_millis = :reviewedAtMillis
        WHERE source_genre_key = :sourceGenreKey
        """
    )
    suspend fun markReviewed(sourceGenreKey: String, reviewedAtMillis: Long)
}
