package com.example.vinfo.data.settings

import android.content.Context

class ApiKeyStore(context: Context) {

    private val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun savePerplexityApiKey(apiKey: String) {
        sharedPreferences.edit().putString(KEY_PERPLEXITY, apiKey.trim()).apply()
    }

    fun saveGeminiApiKey(apiKey: String) {
        sharedPreferences.edit().putString(KEY_GEMINI, apiKey.trim()).apply()
    }

    fun getPerplexityApiKey(): String {
        return sharedPreferences.getString(KEY_PERPLEXITY, "") ?: ""
    }

    fun getGeminiApiKey(): String {
        return sharedPreferences.getString(KEY_GEMINI, "") ?: ""
    }

    companion object {
        private const val PREF_NAME = "vinfo_api_keys"
        private const val KEY_PERPLEXITY = "perplexity_api_key"
        private const val KEY_GEMINI = "gemini_api_key"
    }
}