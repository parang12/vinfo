package com.example.vinfo.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsApiKeyInputPrivacyTest {

    @Test
    fun `api key input starts blank even when a key is already saved`() {
        val savedApiKey = "AIzaSecretKeyThatMustNotAppear"

        val inputValue = initialGeminiApiKeyInputValue(savedApiKey)

        assertEquals("", inputValue)
    }

    @Test
    fun `api key placeholder asks user to enter an api key without showing key prefix`() {
        assertEquals("Gemini API Key 입력", geminiApiKeyPlaceholder())
    }
}
