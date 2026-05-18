package com.example.vinfo.domain.repository

import com.example.vinfo.domain.model.Adjacency
import com.example.vinfo.domain.model.ExplorationState
import com.example.vinfo.domain.model.GenreNode
import com.example.vinfo.domain.model.TrackMetadata

interface GenreExplorationRepository {
    suspend fun getExplorationState(): ExplorationState

    suspend fun computeAndPersistAdjacency(archives: List<TrackMetadata>): List<Adjacency>

    suspend fun activateNode(genreKey: String): GenreNode
}
