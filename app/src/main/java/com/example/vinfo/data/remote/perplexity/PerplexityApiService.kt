package com.example.vinfo.data.remote.perplexity

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST

interface PerplexityApiService {
    @POST("chat/completions")
    suspend fun createChatCompletion(@Body requestBody: RequestBody): String
}