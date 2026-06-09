package com.example.vinfo.domain.model

enum class GenreFlowNodeState {
    ACTIVATED,
    ADJACENT
}

data class GenreFlowNode(
    val key: String,
    val displayName: String,
    val state: GenreFlowNodeState,
    val saveCount: Int,
    val x: Float,
    val y: Float
)

data class GenreFlowEdge(
    val sourceKey: String,
    val targetKey: String,
    val score: Float,
    val active: Boolean,
    val evidence: String
)

data class VisibleGenreFlow(
    val nodes: List<GenreFlowNode>,
    val edges: List<GenreFlowEdge>
)

data class CuratedGenreRelation(
    val source: String,
    val target: String,
    val score: Float,
    val evidence: String
) {
    val sourceKey: String = source.toGenreKey()
    val targetKey: String = target.toGenreKey()
}
