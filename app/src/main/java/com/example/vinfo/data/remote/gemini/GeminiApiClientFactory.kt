package com.example.vinfo.data.remote.gemini

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

object GeminiApiClientFactory {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"

    fun create(apiKey: String): GeminiApiService {
        val client = OkHttpClient.Builder()
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }
}
