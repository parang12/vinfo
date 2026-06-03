package com.example.vinfo.ui.stats

import com.example.vinfo.domain.model.ConfirmedGenreDiscovery
import com.example.vinfo.domain.model.GenreRelationCandidate
import com.example.vinfo.ui.archive.DummyArchive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenreMapUiStateDiscoveryTest {
    @Test
    fun `withDiscoveries adds nearby node and weighted edge after user confirmation`() {
        val baseState = GenreMapUiState.fromArchive(
            listOf(
                DummyArchive(
                    id = "album-1",
                    title = "Album",
                    artist = "Artist",
                    genres = listOf("Hip-Hop"),
                    date = "2026.06.03"
                )
            )
        )

        val updatedState = baseState.withDiscoveries(
            listOf(
                ConfirmedGenreDiscovery(
                    sourceGenre = "Hip-Hop",
                    candidates = listOf(
                        GenreRelationCandidate(
                            genreName = "Drill",
                            score = 0.82f,
                            relationType = "adjacent",
                            evidence = "Search-grounded relation"
                        )
                    )
                )
            )
        )

        val drillNode = updatedState.nodes.firstOrNull { it.label == "Drill" }
        val drillEdge = updatedState.edges.firstOrNull {
            setOf(it.fromId, it.toId) == setOf("hiphop", "drill")
        }

        assertNotNull(drillNode)
        assertEquals(GenreMapNodeType.Adjacent, drillNode!!.type)
        assertNotNull(drillEdge)
        assertEquals(0.82f, drillEdge!!.relationScore, 0.001f)
        assertEquals("Search-grounded relation", drillEdge.evidence)
        assertFalse(updatedState.nodes.any { it.type == GenreMapNodeType.Locked })
    }

    @Test
    fun `withDiscoveries keeps stronger edge when same nearby genre is applied again`() {
        val baseState = GenreMapUiState.fromArchive(
            listOf(
                DummyArchive(
                    id = "album-1",
                    title = "Album",
                    artist = "Artist",
                    genres = listOf("Hip-Hop"),
                    date = "2026.06.03"
                )
            )
        )

        val updatedState = baseState.withDiscoveries(
            listOf(
                ConfirmedGenreDiscovery(
                    sourceGenre = "Hip-Hop",
                    candidates = listOf(
                        GenreRelationCandidate("Drill", 0.40f, "adjacent", "Weak relation"),
                        GenreRelationCandidate("Drill", 0.90f, "influence", "Strong relation")
                    )
                )
            )
        )

        val drillEdges = updatedState.edges.filter {
            setOf(it.fromId, it.toId) == setOf("hiphop", "drill")
        }

        assertEquals(1, drillEdges.size)
        assertEquals(0.90f, drillEdges.single().relationScore, 0.001f)
        assertTrue(drillEdges.single().label.contains("강함"))
    }
}
