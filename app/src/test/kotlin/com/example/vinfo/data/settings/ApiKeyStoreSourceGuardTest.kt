package com.example.vinfo.data.settings

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class ApiKeyStoreSourceGuardTest {

    @Test
    fun `api key store exposes only Gemini runtime key storage`() {
        val projectDir = File(requireNotNull(System.getProperty("user.dir")))
        val source = File(projectDir, "src/main/java/com/example/vinfo/data/settings/ApiKeyStore.kt")
            .readText()

        assertFalse(source.contains("Perplexity"))
        assertFalse(source.contains("perplexity_api_key"))
    }
}
