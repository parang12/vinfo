package com.example.vinfo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.vinfo.domain.model.AlbumGenreCandidate
import com.example.vinfo.domain.model.GenreCategory
import com.example.vinfo.domain.model.GenreCandidateTier
import com.example.vinfo.domain.model.GenreMapper
import com.example.vinfo.domain.model.GenreSource
import com.example.vinfo.domain.model.NowPlayingTrack
import com.example.vinfo.domain.model.TrackMetadata
import com.example.vinfo.ui.archive.DummyArchive
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "album_title") val albumTitle: String,
    val artist: String,
    val album: String? = null,
    val genres: List<String>,
    @ColumnInfo(name = "primary_genre") val primaryGenre: String? = null,
    @ColumnInfo(name = "secondary_genre") val secondaryGenre: String? = null,
    @ColumnInfo(name = "genre_candidates_json") val genreCandidatesJson: String = "[]",
    @ColumnInfo(name = "genre_source") val genreSource: String? = null,
    @ColumnInfo(name = "rym_rating") val rymRating: Float? = null,
    @ColumnInfo(name = "pitchfork_score") val pitchforkScore: Float? = null,
    @ColumnInfo(name = "metacritic_score") val metacriticScore: Int? = null,
    @ColumnInfo(name = "aoty_score") val aotyScore: Int? = null,
    @ColumnInfo(name = "ratings_json") val ratingsJson: String = "{}",
    @ColumnInfo(name = "critics_summary") val criticsSummary: String? = null,
    @ColumnInfo(name = "interview_summary") val interviewSummary: String? = null,
    @ColumnInfo(name = "listening_guide") val listeningGuide: String? = null,
    @ColumnInfo(name = "samples_used_json") val samplesUsedJson: String = "[]",
    @ColumnInfo(name = "missing_sources_json") val missingSourcesJson: String = "[]",
    @ColumnInfo(name = "reliability_notes_json") val reliabilityNotesJson: String = "[]",
    val date: String
) {
    // Entity -> UI Model 변환
    fun toDomain(): DummyArchive {
        return DummyArchive(id, albumTitle, artist, resolveGenres(), date)
    }

    companion object {
        // UI Model -> Entity 변환
        fun fromDomain(domain: DummyArchive): AlbumEntity {
            return AlbumEntity(
                id = domain.id,
                albumTitle = domain.title,
                artist = domain.artist,
                genres = domain.genres,
                date = domain.date
            )
        }

        fun fromTrackSnapshot(
            trackId: String,
            track: NowPlayingTrack,
            metadata: TrackMetadata,
            savedAtMillis: Long = System.currentTimeMillis()
        ): AlbumEntity {
            val primaryGenre = metadata.primaryGenre.displayNameOrNull()
            val secondaryGenre = metadata.secondaryGenre?.displayNameOrNull()
            val genres = listOfNotNull(primaryGenre, secondaryGenre)
            val resolvedAlbumTitle = metadata.album ?: track.album ?: track.title

            return AlbumEntity(
                id = trackId,
                albumTitle = resolvedAlbumTitle,
                artist = track.artist,
                album = resolvedAlbumTitle,
                genres = genres.ifEmpty { listOf("Unknown") },
                primaryGenre = primaryGenre,
                secondaryGenre = secondaryGenre,
                genreCandidatesJson = buildGenreCandidatesJson(metadata.genreCandidates),
                genreSource = metadata.genreSource.name,
                rymRating = metadata.rymRating,
                pitchforkScore = metadata.pitchforkScore,
                metacriticScore = metadata.metacriticScore,
                aotyScore = metadata.aotyScore,
                ratingsJson = buildRatingsJson(metadata),
                criticsSummary = metadata.criticsSummary,
                interviewSummary = metadata.interviewSummary,
                listeningGuide = metadata.listeningGuide,
                samplesUsedJson = JSONArray(metadata.samplesUsed).toString(),
                missingSourcesJson = JSONArray(metadata.missingSources).toString(),
                reliabilityNotesJson = JSONArray(metadata.reliabilityNotes).toString(),
                date = formatDate(savedAtMillis)
            )
        }

        private fun buildRatingsJson(metadata: TrackMetadata): String {
            return JSONObject()
                .put("rym_rating", metadata.rymRating)
                .put("pitchfork_score", metadata.pitchforkScore)
                .put("metacritic_score", metadata.metacriticScore)
                .put("aoty_score", metadata.aotyScore)
                .toString()
        }

        private fun buildGenreCandidatesJson(candidates: List<AlbumGenreCandidate>): String {
            return JSONArray(
                candidates.map { candidate ->
                    JSONObject()
                        .put("name", candidate.name)
                        .put("confidence", candidate.confidence)
                        .put("tier", candidate.tier.name)
                        .put("evidence_text", candidate.evidenceText)
                }
            ).toString()
        }

        private fun formatDate(savedAtMillis: Long): String {
            val formatter = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            return formatter.format(Date(savedAtMillis))
        }
    }

    private fun resolveGenres(): List<String> {
        if (genres.isNotEmpty()) return genres

        return listOfNotNull(primaryGenre, secondaryGenre)
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("Unknown") }
    }

    fun toTrackMetadata(): TrackMetadata {
        val resolvedPrimary = GenreMapper.fromRawGenre(primaryGenre ?: genres.firstOrNull())
        val resolvedSecondary = secondaryGenre?.let(GenreMapper::fromRawGenre)
        return TrackMetadata(
            artist = artist,
            title = albumTitle,
            album = album ?: albumTitle,
            primaryGenre = resolvedPrimary,
            secondaryGenre = resolvedSecondary,
            genreCandidates = parseGenreCandidates(),
            genreSource = runCatching { GenreSource.valueOf(genreSource ?: GenreSource.UNKNOWN.name) }
                .getOrDefault(GenreSource.UNKNOWN),
            rymRating = rymRating,
            pitchforkScore = pitchforkScore,
            metacriticScore = metacriticScore,
            aotyScore = aotyScore,
            criticsSummary = criticsSummary.orEmpty(),
            interviewSummary = interviewSummary,
            listeningGuide = listeningGuide.orEmpty(),
            samplesUsed = parseStringArray(samplesUsedJson),
            missingSources = parseStringArray(missingSourcesJson),
            reliabilityNotes = parseStringArray(reliabilityNotesJson)
        )
    }

    fun toNowPlayingTrack(): NowPlayingTrack {
        return NowPlayingTrack(
            artist = artist,
            title = albumTitle,
            album = album ?: albumTitle,
            sourcePackageName = null,
            albumArtUrl = null
        )
    }

    private fun parseStringArray(rawJson: String): List<String> {
        return runCatching {
            val array = JSONArray(rawJson)
            buildList {
                for (index in 0 until array.length()) {
                    array.optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun parseGenreCandidates(): List<AlbumGenreCandidate> {
        return runCatching {
            val array = JSONArray(genreCandidatesJson)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val name = item.optString("name").trim()
                    if (name.isBlank()) continue
                    val tier = runCatching {
                        GenreCandidateTier.valueOf(item.optString("tier").uppercase(Locale.US))
                    }.getOrDefault(GenreCandidateTier.PRIMARY)
                    add(
                        AlbumGenreCandidate(
                            name = name,
                            confidence = item.optDouble("confidence", 0.0).toFloat().coerceIn(0f, 1f),
                            tier = tier,
                            evidenceText = item.optString("evidence_text").trim().takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}

private fun GenreCategory.displayNameOrNull(): String? {
    return when (this) {
        GenreCategory.HIP_HOP -> "Hip Hop"
        GenreCategory.TRAP -> "Trap"
        GenreCategory.POP -> "Pop"
        GenreCategory.ROCK -> "Rock"
        GenreCategory.ELECTRONIC -> "Electronic"
        GenreCategory.JAZZ -> "Jazz"
        GenreCategory.CLASSICAL -> "Classical"
        GenreCategory.RNB -> "R&B"
        GenreCategory.UNKNOWN -> null
    }
}
