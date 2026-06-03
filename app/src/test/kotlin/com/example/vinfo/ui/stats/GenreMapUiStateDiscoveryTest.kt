package com.example.vinfo.ui.stats

import com.example.vinfo.domain.model.ConfirmedGenreDiscovery
import com.example.vinfo.domain.model.GenreRelationCandidate
import com.example.vinfo.ui.archive.DummyArchive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

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

    @Test
    fun `fromArchive activates blues genre from blues album tags`() {
        val state = GenreMapUiState.fromArchive(
            listOf(
                DummyArchive(
                    id = "album-1",
                    title = "Album",
                    artist = "Artist",
                    genres = listOf("Delta Blues"),
                    date = "2026.06.03"
                )
            )
        )

        val bluesNode = state.nodes.firstOrNull { it.label == "Blues" }

        assertNotNull(bluesNode)
        assertEquals(GenreMapNodeType.Activated, bluesNode!!.type)
    }

    @Test
    fun `fromArchive keeps trap rap tags as trap instead of folding into hip hop`() {
        val state = GenreMapUiState.fromArchive(
            listOf(
                DummyArchive(
                    id = "album-1",
                    title = "Culture",
                    artist = "Migos",
                    genres = listOf("Trap Rap"),
                    date = "2026.06.03"
                )
            )
        )

        val trapNode = state.nodes.firstOrNull { it.label == "Trap" }
        val hipHopNode = state.nodes.firstOrNull { it.label == "Hip-Hop" }

        assertNotNull(trapNode)
        assertEquals(GenreMapNodeType.Activated, trapNode!!.type)
        assertTrue(hipHopNode == null || hipHopNode.type != GenreMapNodeType.Activated)
    }

    @Test
    fun `withDiscoveries marks already visible nearby nodes with searched relation evidence`() {
        val baseState = GenreMapUiState.fromArchive(
            listOf(
                DummyArchive(
                    id = "album-1",
                    title = "Album",
                    artist = "Artist",
                    genres = listOf("Jazz"),
                    date = "2026.06.03"
                )
            )
        )

        val updatedState = baseState.withDiscoveries(
            listOf(
                ConfirmedGenreDiscovery(
                    sourceGenre = "Jazz",
                    candidates = listOf(
                        GenreRelationCandidate(
                            genreName = "Blues",
                            score = 0.82f,
                            relationType = "influence",
                            evidence = "Jazz and blues share historically documented roots."
                        )
                    )
                )
            )
        )

        val bluesNode = updatedState.nodes.first { it.label == "Blues" }
        val bluesEdge = updatedState.edges.first {
            setOf(it.fromId, it.toId) == setOf("jazz", "blues")
        }

        assertEquals("탐색한 연결 후보", bluesNode.note)
        assertEquals("검색으로 확인", bluesNode.lastActivatedText)
        assertEquals(0.82f, bluesEdge.relationScore, 0.001f)
        assertEquals("Jazz and blues share historically documented roots.", bluesEdge.evidence)
        assertTrue(bluesEdge.label.contains("강함"))
    }

    @Test
    fun `withDiscoveries spreads discovered nearby nodes away from existing map nodes`() {
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
        val candidateNames = listOf("Drill", "Cloud Rap", "Grime", "Phonk", "Crunk")

        val updatedState = baseState.withDiscoveries(
            listOf(
                ConfirmedGenreDiscovery(
                    sourceGenre = "Hip-Hop",
                    candidates = candidateNames.mapIndexed { index, genre ->
                        GenreRelationCandidate(
                            genreName = genre,
                            score = 0.85f - (index * 0.05f),
                            relationType = "adjacent",
                            evidence = "$genre relation"
                        )
                    }
                )
            )
        )

        val discoveredNodes = updatedState.nodes.filter { it.label in candidateNames }
        val closestDistance = discoveredNodes
            .flatMapIndexed { index, first ->
                discoveredNodes.drop(index + 1).map { second ->
                    first.position.distanceTo(second.position)
                }
            }
            .minOrNull()

        assertEquals(candidateNames.size, discoveredNodes.size)
        assertTrue(closestDistance != null && closestDistance > 0.16f)
    }

    private fun androidx.compose.ui.geometry.Offset.distanceTo(
        other: androidx.compose.ui.geometry.Offset
    ): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }
}
