package com.example.vinfo.domain.model

data class Adjacency(
    val fromGenre: String,
    val toGenre: String,
    val weight: Double,
    val unlocked: Boolean = false,
    val evidence: Map<String, Any>? = null
)
