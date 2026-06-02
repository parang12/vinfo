package com.example.vinfo.data.remote.gemini

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Query
import retrofit2.http.POST

interface GeminiApiService {
    @POST("models/{model}:generateContent")
    suspend fun generate(
        @retrofit2.http.Path("model") model: String,
        @Query("key") apiKey: String,
        @Body requestBody: RequestBody
    ): String
}
