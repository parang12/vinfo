package com.example.vinfo.ui.nowplaying

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinfo.data.nowplaying.NowPlayingEventBus
import com.example.vinfo.data.remote.perplexity.PerplexityTrackMetadataRepository
import com.example.vinfo.data.settings.ApiKeyStore
import com.example.vinfo.domain.model.AppResult
import com.example.vinfo.domain.model.NowPlayingTrack
import com.example.vinfo.domain.model.TrackMetadata
import com.example.vinfo.domain.usecase.GetTrackInformationUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Locale

data class NowPlayingUiState(
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val currentTrack: NowPlayingTrack? = null,
    val trackMetadata: TrackMetadata? = null
)

class NowPlayingViewModel(application: Application) : AndroidViewModel(application) {

    private val apiKeyStore = ApiKeyStore(application.applicationContext)
    private val getTrackInformationUseCase = GetTrackInformationUseCase(PerplexityTrackMetadataRepository())

    private val _uiState = MutableStateFlow(NowPlayingUiState())
    val uiState: StateFlow<NowPlayingUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigationEvents: SharedFlow<String> = _navigationEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            NowPlayingEventBus.currentTrack.collect { track ->
                _uiState.update {
                    it.copy(currentTrack = track)
                }
            }
        }
    }

    fun catchNow() {
        viewModelScope.launch {
            val apiKey = apiKeyStore.getPerplexityApiKey()
            if (apiKey.isBlank()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = "Perplexity API Key가 설정되지 않았습니다. 설정 화면에서 저장해 주세요."
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(isLoading = true, statusMessage = "Perplexity로 음악 정보를 가져오는 중입니다.")
            }

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

            when (val result = getTrackInformationUseCase(currentTrack.artist, currentTrack.title, currentTrack.album, apiKey)) {
                is AppResult.Success -> {
                    val metadata = result.data
                    _uiState.value = NowPlayingUiState(
                        isLoading = false,
                        statusMessage = "정보를 가져왔습니다.",
                        currentTrack = currentTrack,
                        trackMetadata = metadata
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
        }
    }

    private fun buildTrackId(artist: String, title: String): String {
        val normalized = "${artist.trim().lowercase(Locale.US)}|${title.trim().lowercase(Locale.US)}"
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
        return digest.take(16).joinToString("") { byte -> "%02x".format(byte) }
    }

}