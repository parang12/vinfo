package com.example.vinfo.domain.model

data class NowPlayingTrack(
    val artist: String,
    val title: String,
    val album: String? = null,
    val sourcePackageName: String? = null
)