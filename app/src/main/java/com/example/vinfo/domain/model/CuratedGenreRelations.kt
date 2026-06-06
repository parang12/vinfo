package com.example.vinfo.domain.model

object CuratedGenreRelations {
    private val relations = listOf(
        relation("Blues", "Jazz"),
        relation("Blues", "Soul"),
        relation("Jazz", "Funk"),
        relation("Soul", "Funk"),
        relation("Soul", "R&B"),
        relation("Funk", "Hip-Hop"),
        relation("R&B", "Hip-Hop"),
        relation("Jazz", "Jazz Rap"),
        relation("Hip-Hop", "Boom Bap"),
        relation("Hip-Hop", "Trap"),
        relation("Hip-Hop", "Jazz Rap"),
        relation("Hip-Hop", "Progressive Rap"),
        relation("Jazz Rap", "Neo Soul"),
        relation("Progressive Rap", "Art Pop"),
        relation("Trap", "Pop Rap"),
        relation("Neo Soul", "Art Pop"),
        relation("Pop Rap", "Art Pop"),
        relation("Funk", "Electronic"),
        relation("Synth-pop", "Electronic"),
        relation("Synth-pop", "Art Pop"),
        relation("Electronic", "House"),
        relation("Electronic", "Ambient"),
        relation("Art Pop", "Electronic")
    )

    fun nearbyGenres(sourceGenre: String): List<GenreRelationCandidate> {
        val sourceKey = sourceGenre.normalizedGenreKey()
        return relations
            .filter { it.source.normalizedGenreKey() == sourceKey || it.target.normalizedGenreKey() == sourceKey }
            .map { relation ->
                val target = if (relation.source.normalizedGenreKey() == sourceKey) relation.target else relation.source
                GenreRelationCandidate(
                    genreName = target,
                    score = relation.score,
                    relationType = "curated",
                    evidence = "앱의 장르 맵 사전에 등록된 직접 연결입니다."
                )
            }
            .distinctBy { it.genreName.normalizedGenreKey() }
            .sortedByDescending(GenreRelationCandidate::score)
    }

    private fun relation(
        source: String,
        target: String,
        score: Float = 0.72f
    ): CuratedRelation = CuratedRelation(source, target, score)

    private data class CuratedRelation(
        val source: String,
        val target: String,
        val score: Float
    )

    private fun String.normalizedGenreKey(): String {
        return trim()
            .lowercase()
            .replace(Regex("""[^a-z0-9]+"""), "")
    }
}
