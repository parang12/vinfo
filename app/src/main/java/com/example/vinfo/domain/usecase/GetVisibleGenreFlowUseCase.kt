package com.example.vinfo.domain.usecase

import com.example.vinfo.domain.model.CuratedGenreRelations
import com.example.vinfo.domain.model.GenreDictionary
import com.example.vinfo.domain.model.GenreFlowEdge
import com.example.vinfo.domain.model.GenreFlowNode
import com.example.vinfo.domain.model.GenreFlowNodeState
import com.example.vinfo.domain.model.VisibleGenreFlow

class GetVisibleGenreFlowUseCase {
    operator fun invoke(albumGenres: List<List<String>>): VisibleGenreFlow {
        val activeCounts = albumGenres
            .flatten()
            .mapNotNull { rawGenre -> GenreDictionary.find(rawGenre) }
            .filter { it.mapPosition != null }
            .groupingBy { it.key }
            .eachCount()

        if (activeCounts.isEmpty()) return VisibleGenreFlow(nodes = emptyList(), edges = emptyList())

        val relations = CuratedGenreRelations.directRelations()
        val activeKeys = activeCounts.keys
        val adjacentKeys = relations
            .filter { relation -> relation.sourceKey in activeKeys || relation.targetKey in activeKeys }
            .flatMap { relation -> listOf(relation.sourceKey, relation.targetKey) }
            .filterNot { it in activeKeys }
            .toSet()

        val visibleKeys = activeKeys + adjacentKeys
        val entriesByKey = GenreDictionary.mapEntries().associateBy { it.key }
        val nodes = visibleKeys
            .mapNotNull { key -> entriesByKey[key] }
            .map { entry ->
                val position = requireNotNull(entry.mapPosition)
                val saveCount = activeCounts[entry.key] ?: 0
                GenreFlowNode(
                    key = entry.key,
                    displayName = entry.displayName,
                    state = if (saveCount > 0) GenreFlowNodeState.ACTIVATED else GenreFlowNodeState.ADJACENT,
                    saveCount = saveCount,
                    x = position.x,
                    y = position.y
                )
            }
            .sortedWith(
                compareByDescending<GenreFlowNode> { it.state == GenreFlowNodeState.ACTIVATED }
                    .thenBy { it.displayName }
            )

        val visibleNodeKeys = nodes.map { it.key }.toSet()
        val edges = relations
            .filter { relation ->
                relation.sourceKey in visibleNodeKeys &&
                    relation.targetKey in visibleNodeKeys &&
                    (relation.sourceKey in activeKeys || relation.targetKey in activeKeys)
            }
            .map { relation ->
                GenreFlowEdge(
                    sourceKey = relation.sourceKey,
                    targetKey = relation.targetKey,
                    score = relation.score,
                    active = relation.sourceKey in activeKeys && relation.targetKey in activeKeys,
                    evidence = relation.evidence
                )
            }

        return VisibleGenreFlow(nodes = nodes, edges = edges)
    }
}
