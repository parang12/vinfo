package com.example.vinfo.domain.model

enum class GenreRoot {
    HIP_HOP,
    POP,
    ROCK,
    ELECTRONIC,
    JAZZ,
    BLUES,
    SOUL_RNB,
    CLASSICAL
}

enum class NormalizedGenreStatus {
    VERIFIED,
    EMERGING,
    NEEDS_REVIEW
}

data class GenreDictionaryEntry(
    val displayName: String,
    val root: GenreRoot,
    val aliases: Set<String>,
    val status: NormalizedGenreStatus = NormalizedGenreStatus.VERIFIED,
    val mapPosition: GenreMapPosition? = null
) {
    val key: String = displayName.toGenreKey()
}

data class GenreMapPosition(
    val x: Float,
    val y: Float
)

data class NormalizedGenreCandidate(
    val rawName: String,
    val displayName: String,
    val key: String,
    val root: GenreRoot,
    val confidence: Float,
    val tier: GenreCandidateTier,
    val status: NormalizedGenreStatus,
    val evidenceText: String?
)

data class RejectedGenreCandidate(
    val rawName: String,
    val reason: String
)

data class NormalizedAlbumGenres(
    val representativeGenres: List<String>,
    val accepted: List<NormalizedGenreCandidate>,
    val rejected: List<RejectedGenreCandidate>
)

object GenreDictionary {
    private val entries = listOf(
        entry("Blues", GenreRoot.BLUES, "delta blues", "electric blues", position = GenreMapPosition(0.10f, 0.62f)),
        entry("Jazz", GenreRoot.JAZZ, "modal jazz", "spiritual jazz", position = GenreMapPosition(0.25f, 0.25f)),
        entry("Soul", GenreRoot.SOUL_RNB, "southern soul", "psychedelic soul", position = GenreMapPosition(0.25f, 0.62f)),
        entry("Funk", GenreRoot.SOUL_RNB, "p-funk", "funk music", position = GenreMapPosition(0.40f, 0.38f)),
        entry("R&B", GenreRoot.SOUL_RNB, "rnb", "r & b", "rhythm and blues", "contemporary r&b", position = GenreMapPosition(0.40f, 0.68f)),
        entry("Chipmunk Soul", GenreRoot.SOUL_RNB, "chipmunk-soul"),
        entry("Hip-Hop", GenreRoot.HIP_HOP, "hip hop", "hip-hop", "rap", "hip hop music", position = GenreMapPosition(0.54f, 0.48f)),
        entry("Boom Bap", GenreRoot.HIP_HOP, "boombap", position = GenreMapPosition(0.69f, 0.22f)),
        entry("Trap", GenreRoot.HIP_HOP, "trap rap", "southern trap", "atlanta trap", position = GenreMapPosition(0.69f, 0.72f)),
        entry("Jazz Rap", GenreRoot.HIP_HOP, "jazz-rap", position = GenreMapPosition(0.72f, 0.48f)),
        entry("Progressive Rap", GenreRoot.HIP_HOP, "experimental hip hop", "art rap", position = GenreMapPosition(0.80f, 0.58f)),
        entry("Neo Soul", GenreRoot.SOUL_RNB, "neosoul", "neo-soul", position = GenreMapPosition(0.86f, 0.34f)),
        entry("Pop Rap", GenreRoot.HIP_HOP, "pop-rap", position = GenreMapPosition(0.86f, 0.76f)),
        entry("Pop", GenreRoot.POP, "popular music"),
        entry("Art Pop", GenreRoot.POP, "art-pop", position = GenreMapPosition(0.92f, 0.55f)),
        entry("Neo-Psychedelia", GenreRoot.POP, "neo psychedelia", "neo psychedelic"),
        entry("Electronic", GenreRoot.ELECTRONIC, "electronica", position = GenreMapPosition(0.58f, 0.86f)),
        entry("Synth-pop", GenreRoot.POP, "synth pop", "synthpop", position = GenreMapPosition(0.74f, 0.82f)),
        entry("House", GenreRoot.ELECTRONIC, "deep house", "house music", position = GenreMapPosition(0.74f, 0.92f)),
        entry("Ambient", GenreRoot.ELECTRONIC, "ambient music", position = GenreMapPosition(0.90f, 0.92f)),
        entry("Rock", GenreRoot.ROCK, "indie rock", "punk rock"),
        entry("Classical", GenreRoot.CLASSICAL, "orchestral", "classical music")
    )

    private val entriesByKey: Map<String, GenreDictionaryEntry> = entries.associateBy { it.key }
    private val entriesByAlias: Map<String, GenreDictionaryEntry> = entries.flatMap { entry ->
        (entry.aliases + entry.displayName).map { alias -> alias.toGenreKey() to entry }
    }.toMap()

    fun all(): List<GenreDictionaryEntry> = entries

    fun find(rawName: String): GenreDictionaryEntry? {
        val key = rawName.toGenreKey()
        return entriesByAlias[key] ?: entriesByKey[key]
    }

    fun mapEntries(): List<GenreDictionaryEntry> = entries.filter { it.mapPosition != null }

    private fun entry(
        displayName: String,
        root: GenreRoot,
        vararg aliases: String,
        status: NormalizedGenreStatus = NormalizedGenreStatus.VERIFIED,
        position: GenreMapPosition? = null
    ): GenreDictionaryEntry {
        return GenreDictionaryEntry(
            displayName = displayName,
            root = root,
            aliases = aliases.toSet(),
            status = status,
            mapPosition = position
        )
    }
}

fun String.toGenreKey(): String {
    return trim()
        .lowercase()
        .replace("&", "and")
        .replace(Regex("""[^a-z0-9]+"""), "")
}
