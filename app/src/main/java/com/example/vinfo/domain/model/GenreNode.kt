package com.example.vinfo.domain.model

data class GenreNode(
    val genreKey: String,
    val displayName: String,
    val activated: Boolean = false,
    val lastActivatedAt: Long? = null,
    val saveCount: Int = 0
)
