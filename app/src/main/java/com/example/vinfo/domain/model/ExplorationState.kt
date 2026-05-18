package com.example.vinfo.domain.model

data class ExplorationState(
    val diversityWeight: Double = 0.5,
    val lastUpdated: Long = System.currentTimeMillis()
)
