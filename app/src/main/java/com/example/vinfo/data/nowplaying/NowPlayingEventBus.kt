package com.example.vinfo.data.nowplaying

import com.example.vinfo.domain.model.NowPlayingTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NowPlayingEventBus {

    private val _currentTrack = MutableStateFlow<NowPlayingTrack?>(null)
    val currentTrack: StateFlow<NowPlayingTrack?> = _currentTrack.asStateFlow()

    fun publish(track: NowPlayingTrack) {
        _currentTrack.value = track
    }
}