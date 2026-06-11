package com.example.vinfo.ui.stats

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class GenreMapCanvasLabelGuardTest {

    @Test
    fun `genre map canvas does not render background lane labels`() {
        val projectDir = File(requireNotNull(System.getProperty("user.dir")))
        val source = File(
            projectDir,
            "src/main/java/com/example/vinfo/ui/stats/GenreMapScreen.kt"
        ).readText()

        val removedLabels = listOf("원천 장르", "중심 흐름", "파생 흐름")
        removedLabels.forEach { label ->
            assertFalse(
                "Genre map background lane label should not be rendered: $label",
                source.contains(label)
            )
        }
    }
}
