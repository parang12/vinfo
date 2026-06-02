package com.example.vinfo.data.remote.lyrics

import retrofit2.http.GET
import retrofit2.http.Path

interface LyricsApiService {
    @GET("v1/{artist}/{title}")
    suspend fun getLyrics(
        @Path("artist") artist: String,
        @Path("title") title: String
    ): String
}
