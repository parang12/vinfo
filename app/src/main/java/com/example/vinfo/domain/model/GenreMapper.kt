package com.example.vinfo.domain.model

object GenreMapper {

    fun fromRawGenre(rawGenre: String?): GenreCategory {
        val normalized = rawGenre
            ?.trim()
            ?.lowercase()
            .orEmpty()

        return when {
            normalized.isBlank() -> GenreCategory.UNKNOWN
            normalized.contains("hip hop") || normalized.contains("hip-hop") || normalized == "rap" -> GenreCategory.HIP_HOP
            normalized.contains("r&b") || normalized.contains("rnb") || normalized.contains("neo soul") -> GenreCategory.RNB
            normalized.contains("pop") || normalized.contains("synth") || normalized.contains("electro pop") -> GenreCategory.POP
            normalized.contains("rock") || normalized.contains("punk") || normalized.contains("indie") -> GenreCategory.ROCK
            normalized.contains("electronic") || normalized.contains("house") || normalized.contains("techno") || normalized.contains("ambient") -> GenreCategory.ELECTRONIC
            normalized.contains("jazz") -> GenreCategory.JAZZ
            normalized.contains("classical") || normalized.contains("orchestral") -> GenreCategory.CLASSICAL
            else -> GenreCategory.UNKNOWN
        }
    }

    fun fromRawSource(rawSource: String?): GenreSource {
        return when (rawSource?.trim()?.uppercase()) {
            "RYM" -> GenreSource.RYM
            "LLM" -> GenreSource.LLM
            "MANUAL" -> GenreSource.MANUAL
            else -> GenreSource.UNKNOWN
        }
    }
}