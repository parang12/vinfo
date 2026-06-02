package com.example.vinfo.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteTest {

    @Test
    fun detailRouteEncodesAlbumArtUrlWhenPresent() {
        val route = Route.Detail.createRoute(
            trackId = "track-1",
            albumArtUrl = "file:///data/user/0/com.example.vinfo/cache/album art.png"
        )

        assertEquals(
            "detail/track-1?albumArtUrl=file%3A%2F%2F%2Fdata%2Fuser%2F0%2Fcom.example.vinfo%2Fcache%2Falbum+art.png",
            route
        )
    }

    @Test
    fun detailRouteOmitsAlbumArtUrlWhenAbsent() {
        assertEquals("detail/track-1", Route.Detail.createRoute("track-1"))
    }
}
