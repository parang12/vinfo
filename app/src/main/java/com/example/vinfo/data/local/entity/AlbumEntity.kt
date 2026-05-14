package com.example.vinfo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.vinfo.ui.archive.DummyArchive

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val genres: List<String>,
    val date: String
) {
    // Entity -> UI Model 변환
    fun toDomain(): DummyArchive {
        return DummyArchive(id, title, artist, genres, date)
    }

    companion object {
        // UI Model -> Entity 변환
        fun fromDomain(domain: DummyArchive): AlbumEntity {
            return AlbumEntity(domain.id, domain.title, domain.artist, domain.genres, domain.date)
        }
    }
}
