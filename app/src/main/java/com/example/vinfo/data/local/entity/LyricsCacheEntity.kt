package com.example.vinfo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyrics_cache")
data class LyricsCacheEntity(
    @PrimaryKey val id: String,
    val artist: String,
    val title: String,
    val lyrics: String,
    val updatedAtMillis: Long
)
