package com.example.vinfo.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Route(val path: String) {
    object NowPlaying : Route("now_playing")
    object Detail : Route("detail/{trackId}?albumArtUrl={albumArtUrl}") {
        fun createRoute(trackId: String, albumArtUrl: String? = null): String {
            if (albumArtUrl.isNullOrBlank()) {
                return "detail/$trackId"
            }

            val encodedAlbumArtUrl = URLEncoder.encode(albumArtUrl, StandardCharsets.UTF_8.name())
            return "detail/$trackId?albumArtUrl=$encodedAlbumArtUrl"
        }
    }

    object Archive : Route("archive")
    object GenreStats : Route("genre_stats")
    object Settings : Route("settings")
}
