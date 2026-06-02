package com.example.vinfo.data.repository

import com.example.vinfo.domain.model.Adjacency
import com.example.vinfo.domain.model.ExplorationState
import com.example.vinfo.domain.model.GenreNode
import com.example.vinfo.domain.model.TrackMetadata
import com.example.vinfo.domain.repository.GenreExplorationRepository

/**
 * 임시 스텁 구현체. 실제 DB/DAO 연동은 이후 구현.
 */
class GenreExplorationRepositoryImpl : GenreExplorationRepository {
    override suspend fun getExplorationState(): ExplorationState {
        return ExplorationState(diversityWeight = 0.5, lastUpdated = System.currentTimeMillis())
    }

    override suspend fun computeAndPersistAdjacency(archives: List<TrackMetadata>): List<Adjacency> {
        // TODO: 실제 계산 로직 및 DB 반영 구현
        return emptyList()
    }

    override suspend fun activateNode(genreKey: String): GenreNode {
        // TODO: DB에서 노드 조회/갱신 후 반환
        return GenreNode(
            genreKey = genreKey,
            displayName = genreKey,
            activated = true,
            lastActivatedAt = System.currentTimeMillis(),
            saveCount = 1
        )
    }
}
