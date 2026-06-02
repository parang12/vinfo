package com.example.vinfo.data.remote.lyrics

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

object LyricsApiClientFactory {
    private const val BASE_URL = "https://api.lyrics.ovh/"

    fun create(): LyricsApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(LyricsApiService::class.java)
    }
}
