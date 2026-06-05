package com.example.vinfo.ui.nowplaying

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinfo.data.nowplaying.NowPlayingEventBus
import com.example.vinfo.data.local.AppDatabase
import com.example.vinfo.data.repository.CachedLyricsRepository
import com.example.vinfo.data.repository.CachedTrackMetadataRepository
import com.example.vinfo.data.remote.gemini.GeminiTrackMetadataRepository
import com.example.vinfo.data.remote.lyrics.LyricsRepository
import com.example.vinfo.data.settings.ApiKeyStore
import com.example.vinfo.domain.model.AppResult
import com.example.vinfo.domain.model.NowPlayingTrack
import com.example.vinfo.domain.model.TrackMetadata
import com.example.vinfo.domain.model.buildTrackId
import com.example.vinfo.domain.usecase.GetTrackInformationUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class NowPlayingUiState(
    val isLoading: Boolean = false,
    val isLyricsLoading: Boolean = false,
    val statusMessage: String? = null,
    val currentTrack: NowPlayingTrack? = null,
    val trackMetadata: TrackMetadata? = null,
    val originalLyrics: String? = null,
    val lyricsErrorMessage: String? = null
)

class NowPlayingViewModel(application: Application) : AndroidViewModel(application) {

    private val apiKeyStore = ApiKeyStore(application.applicationContext)
    private val database = AppDatabase.getDatabase(application.applicationContext)
    private val getTrackInformationUseCase = GetTrackInformationUseCase(
        CachedTrackMetadataRepository(
            albumDao = database.albumDao(),
            remoteRepository = GeminiTrackMetadataRepository()
        )
    )
    private val lyricsRepository = CachedLyricsRepository(
        lyricsCacheDao = database.lyricsCacheDao(),
        remoteRepository = LyricsRepository()
    )
    private val catchNowRequestGate = CatchNowRequestGate()

    private val _uiState = MutableStateFlow(NowPlayingUiState())
    val uiState: StateFlow<NowPlayingUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigationEvents: SharedFlow<String> = _navigationEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            NowPlayingEventBus.currentTrack.collect { track ->
                // 디버그: NowPlayingEventBus로부터 전달된 albumArtUrl 확인
                try {
                    android.util.Log.d("NowPlayingViewModel", "EventBus track received: ${track?.artist} - ${track?.title} | albumArtUrl=${track?.albumArtUrl}")
                } catch (_: Exception) {}
                _uiState.update { state ->
                    state.copy(currentTrack = state.currentTrack.mergeAlbumArtIfSameTrack(track))
                }
            }
        }
    }

    fun catchNow() {
        if (!catchNowRequestGate.tryStart()) {
            return
        }

        viewModelScope.launch {
            var lyricsJob: Job? = null
            try {
                val currentTrack = _uiState.value.currentTrack
                if (currentTrack == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            statusMessage = "현재 재생 곡을 아직 감지하지 못했습니다. 알림 접근 권한을 확인해 주세요."
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isLyricsLoading = true,
                        originalLyrics = null,
                        lyricsErrorMessage = null
                    )
                }
                lyricsJob = launch { fetchLyrics(currentTrack) }

                val apiKey = apiKeyStore.getGeminiApiKey()
                if (apiKey.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            statusMessage = "원문 가사를 조회 중입니다. 앨범 분석을 사용하려면 Gemini API Key를 설정해 주세요."
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(isLoading = true, statusMessage = "Gemini로 음악 정보를 가져오는 중입니다.")
                }

                when (val result = getTrackInformationUseCase(currentTrack.artist, currentTrack.title, currentTrack.album, apiKey)) {
                    is AppResult.Success -> {
                        val metadata = result.data
                        _uiState.value = NowPlayingUiState(
                            isLoading = false,
                            isLyricsLoading = _uiState.value.isLyricsLoading,
                            statusMessage = "정보를 가져왔습니다.",
                            currentTrack = currentTrack,
                            trackMetadata = metadata,
                            originalLyrics = _uiState.value.originalLyrics,
                            lyricsErrorMessage = _uiState.value.lyricsErrorMessage
                        )
                        _navigationEvents.tryEmit(buildTrackId(metadata.artist, metadata.title))
                    }

                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                statusMessage = result.message,
                                currentTrack = currentTrack,
                                trackMetadata = null
                            )
                        }
                    }

                    AppResult.Loading -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                statusMessage = "요청 상태를 확인할 수 없습니다.",
                                currentTrack = currentTrack,
                                trackMetadata = null
                            )
                        }
                    }
                }
            } finally {
                lyricsJob?.join()
                catchNowRequestGate.finish()
            }
        }
    }

    private suspend fun fetchLyrics(currentTrack: NowPlayingTrack) {
        when (val result = lyricsRepository.getRawLyrics(currentTrack.artist, currentTrack.title)) {
            is AppResult.Success -> {
                _uiState.update {
                    it.copy(
                        isLyricsLoading = false,
                        originalLyrics = result.data,
                        lyricsErrorMessage = null
                    )
                }
            }
            is AppResult.Error -> {
                _uiState.update {
                    it.copy(
                        isLyricsLoading = false,
                        originalLyrics = null,
                        lyricsErrorMessage = result.message
                    )
                }
            }
            AppResult.Loading -> Unit
        }
    }

    private fun NowPlayingTrack?.mergeAlbumArtIfSameTrack(incoming: NowPlayingTrack?): NowPlayingTrack? {
        if (incoming == null) return null
        if (!incoming.albumArtUrl.isNullOrBlank()) return incoming

        val previous = this ?: return incoming
        val isSameTrack = previous.artist.equals(incoming.artist, ignoreCase = true) &&
            previous.title.equals(incoming.title, ignoreCase = true)

        return if (isSameTrack && !previous.albumArtUrl.isNullOrBlank()) {
            incoming.copy(albumArtUrl = previous.albumArtUrl)
        } else {
            incoming
        }
    }

}
