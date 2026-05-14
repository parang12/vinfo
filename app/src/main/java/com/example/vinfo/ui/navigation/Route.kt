package com.example.vinfo.ui.navigation

sealed class Route(val path: String) {
    object NowPlaying : Route("now_playing")
    object Detail : Route("detail/{trackId}") {
        fun createRoute(trackId: String) = "detail/$trackId"
    }

    object Archive : Route("archive")
    object GenreStats : Route("genre_stats")
    object Settings : Route("settings")
}
