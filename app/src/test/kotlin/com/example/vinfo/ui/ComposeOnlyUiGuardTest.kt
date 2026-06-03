package com.example.vinfo.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ComposeOnlyUiGuardTest {

    @Test
    fun `main UI does not use XML layouts or View based screen APIs`() {
        val projectDir = File(requireNotNull(System.getProperty("user.dir")))
        val mainDir = File(projectDir, "src/main")
        val layoutDir = File(mainDir, "res/layout")
        val kotlinFiles = File(mainDir, "java")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

        assertFalse("UI screens must be Jetpack Compose, not XML layouts.", layoutDir.exists())
        assertTrue("Expected Kotlin source files under src/main/java.", kotlinFiles.isNotEmpty())

        val forbiddenPatterns = listOf(
            "setContentView(",
            "LayoutInflater",
            "findViewById",
            "R.layout.",
            "android.widget.",
            "RecyclerView",
            "AppCompatActivity",
            "FragmentActivity",
            "androidx.fragment."
        )

        val offenders = kotlinFiles.flatMap { file ->
            val text = file.readText()
            forbiddenPatterns
                .filter { pattern -> text.contains(pattern) }
                .map { pattern -> "${file.relativeTo(projectDir).path}: $pattern" }
        }

        assertTrue(
            "Found View/XML UI APIs. Keep screens implemented with Jetpack Compose only:\n${offenders.joinToString("\n")}",
            offenders.isEmpty()
        )
    }
}
