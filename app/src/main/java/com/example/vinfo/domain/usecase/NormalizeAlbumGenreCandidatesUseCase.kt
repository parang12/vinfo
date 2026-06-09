package com.example.vinfo.domain.usecase

import com.example.vinfo.domain.model.AlbumGenreCandidate
import com.example.vinfo.domain.model.GenreCandidateTier
import com.example.vinfo.domain.model.GenreDictionary
import com.example.vinfo.domain.model.GenreRoot
import com.example.vinfo.domain.model.NormalizedAlbumGenres
import com.example.vinfo.domain.model.NormalizedGenreCandidate
import com.example.vinfo.domain.model.RejectedGenreCandidate

class NormalizeAlbumGenreCandidatesUseCase {
    operator fun invoke(
        candidates: List<AlbumGenreCandidate>,
        maxRepresentativeGenres: Int = 3
    ): NormalizedAlbumGenres {
        val accepted = mutableListOf<NormalizedGenreCandidate>()
        val rejected = mutableListOf<RejectedGenreCandidate>()

        candidates.forEach { candidate ->
            val rawName = candidate.name.trim()
            if (rawName.isBlank() || rawName.equals("unknown", ignoreCase = true)) {
                rejected += RejectedGenreCandidate(rawName = rawName.ifBlank { candidate.name }, reason = "unknown")
                return@forEach
            }

            val entry = GenreDictionary.find(rawName)
            if (entry == null) {
                rejected += RejectedGenreCandidate(rawName = rawName, reason = "not_in_dictionary")
                return@forEach
            }

            accepted += NormalizedGenreCandidate(
                rawName = rawName,
                displayName = entry.displayName,
                key = entry.key,
                root = entry.root,
                confidence = candidate.confidence.coerceIn(0f, 1f),
                tier = candidate.tier,
                status = entry.status,
                evidenceText = candidate.evidenceText
            )
        }

        val deduped = accepted
            .groupBy { it.key }
            .values
            .map { duplicates ->
                duplicates.maxWith(
                    compareBy<NormalizedGenreCandidate> { it.confidence }
                        .thenBy { it.tier.priority }
                )
            }
            .sortedWith(
                compareByDescending<NormalizedGenreCandidate> { it.tier.priority }
                    .thenByDescending { it.confidence }
            )

        val filteredAccepted = deduped
            .filterNot { candidate -> candidate.isBroadRootShadowedBy(deduped) }

        val representatives = filteredAccepted
            .take(maxRepresentativeGenres)
            .map { it.displayName }

        return NormalizedAlbumGenres(
            representativeGenres = representatives,
            accepted = filteredAccepted,
            rejected = rejected
        )
    }

    private fun NormalizedGenreCandidate.isBroadRootShadowedBy(
        allAccepted: List<NormalizedGenreCandidate>
    ): Boolean {
        val isBroadRoot = when (displayName) {
            "Hip-Hop" -> root == GenreRoot.HIP_HOP
            "Pop" -> root == GenreRoot.POP
            "Rock" -> root == GenreRoot.ROCK
            "Electronic" -> root == GenreRoot.ELECTRONIC
            "Jazz" -> root == GenreRoot.JAZZ
            "Blues" -> root == GenreRoot.BLUES
            "Soul", "R&B" -> root == GenreRoot.SOUL_RNB
            "Classical" -> root == GenreRoot.CLASSICAL
            else -> false
        }
        if (!isBroadRoot) return false

        return allAccepted.any { other ->
            other.key != key && other.root == root && other.tier.priority >= tier.priority
        }
    }

    private val GenreCandidateTier.priority: Int
        get() = when (this) {
            GenreCandidateTier.PRIMARY -> 3
            GenreCandidateTier.SECONDARY -> 2
            GenreCandidateTier.MICRO -> 1
        }
}
