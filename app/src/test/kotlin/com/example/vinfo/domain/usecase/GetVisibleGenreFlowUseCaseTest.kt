package com.example.vinfo.domain.usecase

import com.example.vinfo.domain.model.GenreFlowNodeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetVisibleGenreFlowUseCaseTest {

    private val useCase = GetVisibleGenreFlowUseCase()

    @Test
    fun `returns activated genres and direct one hop candidates only`() {
        val flow = useCase(
            albumGenres = listOf(
                listOf("Trap Rap"),
                listOf("Unknown Internet Post-Genre")
            )
        )

        assertTrue(flow.nodes.any { it.displayName == "Trap" && it.state == GenreFlowNodeState.ACTIVATED })
        assertTrue(flow.nodes.any { it.displayName == "Hip-Hop" && it.state == GenreFlowNodeState.ADJACENT })
        assertTrue(flow.nodes.any { it.displayName == "Pop Rap" && it.state == GenreFlowNodeState.ADJACENT })
        assertFalse(flow.nodes.any { it.displayName == "Unknown Internet Post-Genre" })
        assertFalse(flow.nodes.any { it.displayName == "Jazz" })
    }

    @Test
    fun `uses dictionary aliases so synth pop activates synth pop map node`() {
        val flow = useCase(albumGenres = listOf(listOf("Synth Pop")))

        assertEquals(GenreFlowNodeState.ACTIVATED, flow.nodes.first { it.displayName == "Synth-pop" }.state)
        assertEquals(GenreFlowNodeState.ADJACENT, flow.nodes.first { it.displayName == "Electronic" }.state)
        assertTrue(flow.edges.any { edge ->
            setOf(edge.sourceKey, edge.targetKey) == setOf("synthpop", "electronic")
        })
    }

    @Test
    fun `keeps visible nodes positioned from dictionary`() {
        val flow = useCase(albumGenres = listOf(listOf("Jazz")))

        val jazz = flow.nodes.first { it.displayName == "Jazz" }

        assertEquals(0.25f, jazz.x, 0.001f)
        assertEquals(0.25f, jazz.y, 0.001f)
    }
}
