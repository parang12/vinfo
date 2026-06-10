package com.example.vinfo.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinfo.data.local.AppDatabase
import com.example.vinfo.data.local.dao.GenreRelationCacheDao
import com.example.vinfo.data.local.entity.GenreRelationCacheEntity
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

data class GenreRelationReviewItem(
    val sourceGenre: String,
    val candidates: List<GenreRelationCandidate>
)

data class GenreMapDiscoveryState(
    val selectedGenre: String? = null,
    val isLoading: Boolean = false,
    val isPopupVisible: Boolean = false,
    val candidates: List<GenreRelationCandidate> = emptyList(),
    val confirmedDiscoveries: List<ConfirmedGenreDiscovery> = emptyList(),
    val pendingReviews: List<GenreRelationReviewItem> = emptyList(),
    val errorMessage: String? = null
) {
    val pendingReviewCount: Int = pendingReviews.size
}

fun GenreMapDiscoveryState.confirmedRelationCount(): Int {
    return confirmedDiscoveries.sumOf { discovery -> discovery.candidates.size }
}

fun expansionFeedbackMessage(
    previous: GenreMapDiscoveryState,
    next: GenreMapDiscoveryState
): String? {
    val addedCount = next.confirmedRelationCount() - previous.confirmedRelationCount()
    return if (addedCount > 0) {
        "장르 관계 ${addedCount}개가 지도에 반영되었습니다."
    } else {
        null
    }
}

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

fun GenreMapDiscoveryState.showPendingReviews(
    pendingReviews: List<GenreRelationReviewItem>
): GenreMapDiscoveryState {
    return copy(pendingReviews = pendingReviews)
}

fun GenreMapDiscoveryState.confirmPendingReview(
    sourceGenre: String
): GenreMapDiscoveryState {
    val reviewItem = pendingReviews.firstOrNull {
        it.sourceGenre.toGenreKey() == sourceGenre.toGenreKey()
    } ?: return this
    val updated = copy(selectedGenre = reviewItem.sourceGenre)
        .showCandidates(reviewItem.candidates)
        .confirmCandidates(reviewItem.candidates)

    return updated.copy(
        pendingReviews = pendingReviews.filterNot {
            it.sourceGenre.toGenreKey() == sourceGenre.toGenreKey()
        }
    )
}

class GenreMapViewModel @JvmOverloads constructor(
    application: Application,
    private val cacheDao: GenreRelationCacheDao = AppDatabase
        .getDatabase(application.applicationContext)
        .genreRelationCacheDao(),
    private val repository: GenreRelationDiscoveryRepository = GeminiGenreRelationDiscoveryRepository(
        cacheDao = cacheDao
    )
) : AndroidViewModel(application) {
    private val apiKeyStore = ApiKeyStore(application.applicationContext)
    private val _discoveryState = MutableStateFlow(GenreMapDiscoveryState())
    val discoveryState: StateFlow<GenreMapDiscoveryState> = _discoveryState.asStateFlow()

    init {
        loadPendingReviews()
    }

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
        loadPendingReviews()
    }

    fun confirmPendingReview(sourceGenre: String) {
        _discoveryState.update { it.confirmPendingReview(sourceGenre) }
        viewModelScope.launch {
            cacheDao.markReviewed(sourceGenre.toGenreKey(), System.currentTimeMillis())
            loadPendingReviews()
        }
    }

    fun loadPendingReviews() {
        viewModelScope.launch {
            val reviews = cacheDao.getPendingReview().mapNotNull(GenreRelationCacheEntity::toReviewItem)
            _discoveryState.update { it.showPendingReviews(reviews) }
        }
    }
}

private fun GenreRelationCacheEntity.toReviewItem(): GenreRelationReviewItem? {
    val candidates = toCandidates()
    if (candidates.isEmpty()) return null
    return GenreRelationReviewItem(
        sourceGenre = sourceGenre,
        candidates = candidates
    )
}
