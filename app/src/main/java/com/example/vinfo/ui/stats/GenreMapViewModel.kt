package com.example.vinfo.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinfo.data.local.AppDatabase
import com.example.vinfo.data.remote.gemini.GeminiGenreRelationDiscoveryRepository
import com.example.vinfo.data.settings.ApiKeyStore
import com.example.vinfo.domain.model.AppResult
import com.example.vinfo.domain.model.ConfirmedGenreDiscovery
import com.example.vinfo.domain.model.GenreRelationCandidate
import com.example.vinfo.domain.model.toGenreKey
import com.example.vinfo.domain.repository.GenreRelationDiscoveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GenreMapDiscoveryState(
    val selectedGenre: String? = null,
    val isLoading: Boolean = false,
    val isPopupVisible: Boolean = false,
    val candidates: List<GenreRelationCandidate> = emptyList(),
    val confirmedDiscoveries: List<ConfirmedGenreDiscovery> = emptyList(),
    val errorMessage: String? = null
)

fun GenreMapDiscoveryState.startSearch(genre: String): GenreMapDiscoveryState {
    return copy(
        selectedGenre = genre,
        isLoading = true,
        isPopupVisible = false,
        candidates = emptyList(),
        errorMessage = null
    )
}

fun GenreMapDiscoveryState.showCandidates(
    candidates: List<GenreRelationCandidate>
): GenreMapDiscoveryState {
    return copy(
        isLoading = false,
        isPopupVisible = true,
        candidates = candidates,
        errorMessage = null
    )
}

fun GenreMapDiscoveryState.showError(message: String): GenreMapDiscoveryState {
    return copy(
        isLoading = false,
        isPopupVisible = true,
        candidates = emptyList(),
        errorMessage = message
    )
}

fun GenreMapDiscoveryState.dismissPopup(): GenreMapDiscoveryState {
    return copy(
        isLoading = false,
        isPopupVisible = false,
        candidates = emptyList(),
        errorMessage = null
    )
}

fun GenreMapDiscoveryState.confirmCandidates(
    selectedCandidates: List<GenreRelationCandidate>
): GenreMapDiscoveryState {
    val source = selectedGenre ?: return dismissPopup()
    if (candidates.isEmpty() || selectedCandidates.isEmpty()) return dismissPopup()

    val availableCandidateKeys = candidates
        .map { it.genreName.toGenreKey() }
        .toSet()
    val candidatesToConfirm = selectedCandidates
        .filter { it.genreName.toGenreKey() in availableCandidateKeys }
    if (candidatesToConfirm.isEmpty()) return dismissPopup()

    val existing = confirmedDiscoveries
        .firstOrNull { it.sourceGenre.toGenreKey() == source.toGenreKey() }
        ?.candidates
        .orEmpty()
    val mergedCandidates = (existing + candidatesToConfirm)
        .groupBy { it.genreName.toGenreKey() }
        .values
        .mapNotNull { duplicates -> duplicates.maxByOrNull(GenreRelationCandidate::score) }
        .sortedByDescending(GenreRelationCandidate::score)

    return copy(
        isLoading = false,
        isPopupVisible = false,
        candidates = emptyList(),
        errorMessage = null,
        confirmedDiscoveries = confirmedDiscoveries
            .filterNot { it.sourceGenre.toGenreKey() == source.toGenreKey() } +
            ConfirmedGenreDiscovery(sourceGenre = source, candidates = mergedCandidates)
    )
}

class GenreMapViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: GenreRelationDiscoveryRepository = GeminiGenreRelationDiscoveryRepository(
        cacheDao = AppDatabase.getDatabase(application.applicationContext).genreRelationCacheDao()
    )
) : AndroidViewModel(application) {
    private val apiKeyStore = ApiKeyStore(application.applicationContext)
    private val _discoveryState = MutableStateFlow(GenreMapDiscoveryState())
    val discoveryState: StateFlow<GenreMapDiscoveryState> = _discoveryState.asStateFlow()

    fun findNearbyGenres(selectedGenre: String) {
        if (_discoveryState.value.isLoading) return
        _discoveryState.update { it.startSearch(selectedGenre) }

        viewModelScope.launch {
            when (
                val result = repository.discoverNearbyGenres(
                    selectedGenre = selectedGenre,
                    apiKey = apiKeyStore.getGeminiApiKey()
                )
            ) {
                is AppResult.Success -> _discoveryState.update { it.showCandidates(result.data) }
                is AppResult.Error -> _discoveryState.update { it.showError(result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun dismissDiscoveryPopup() {
        _discoveryState.update(GenreMapDiscoveryState::dismissPopup)
    }

    fun confirmDiscoveryCandidates(selectedCandidates: List<GenreRelationCandidate>) {
        _discoveryState.update { it.confirmCandidates(selectedCandidates) }
    }
}
