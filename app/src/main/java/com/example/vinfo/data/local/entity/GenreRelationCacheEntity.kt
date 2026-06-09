package com.example.vinfo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.vinfo.domain.model.GenreRelationCandidate
import com.example.vinfo.domain.model.toGenreKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "genre_relation_cache")
data class GenreRelationCacheEntity(
    @PrimaryKey @ColumnInfo(name = "source_genre_key") val sourceGenreKey: String,
    @ColumnInfo(name = "source_genre") val sourceGenre: String,
    @ColumnInfo(name = "candidates_json") val candidatesJson: String,
    @ColumnInfo(name = "review_status") val reviewStatus: String = REVIEW_PENDING,
    @ColumnInfo(name = "updated_at_millis") val updatedAtMillis: Long,
    @ColumnInfo(name = "reviewed_at_millis") val reviewedAtMillis: Long? = null
) {
    fun toCandidates(): List<GenreRelationCandidate> {
        return runCatching {
            val array = JSONArray(candidatesJson)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val genreName = item.optString("genre").trim()
                    if (genreName.isBlank()) continue
                    add(
                        GenreRelationCandidate(
                            genreName = genreName,
                            score = item.optDouble("score", 0.0).toFloat().coerceIn(0f, 1f),
                            relationType = item.optString("relation_type").trim(),
                            evidence = item.optString("evidence").trim()
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        const val REVIEW_PENDING = "PENDING"
        const val REVIEW_CONFIRMED = "CONFIRMED"

        fun fromCandidates(
            sourceGenre: String,
            candidates: List<GenreRelationCandidate>,
            updatedAtMillis: Long = System.currentTimeMillis(),
            reviewStatus: String = REVIEW_PENDING,
            reviewedAtMillis: Long? = null
        ): GenreRelationCacheEntity {
            return GenreRelationCacheEntity(
                sourceGenreKey = sourceGenre.toGenreKey(),
                sourceGenre = sourceGenre.trim(),
                candidatesJson = JSONArray(
                    candidates.map { candidate ->
                        JSONObject()
                            .put("genre", candidate.genreName)
                            .put("score", candidate.score)
                            .put("relation_type", candidate.relationType)
                            .put("evidence", candidate.evidence)
                    }
                ).toString(),
                reviewStatus = reviewStatus,
                updatedAtMillis = updatedAtMillis,
                reviewedAtMillis = reviewedAtMillis
            )
        }
    }
}
