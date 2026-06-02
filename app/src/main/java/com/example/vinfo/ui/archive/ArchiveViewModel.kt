package com.example.vinfo.ui.archive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinfo.data.local.AppDatabase
import com.example.vinfo.data.local.entity.AlbumEntity
import com.example.vinfo.domain.model.NowPlayingTrack
import com.example.vinfo.domain.model.TrackMetadata
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * 보관함 데이터를 관리하는 공유 ViewModel.
 * Room DB를 사용하여 데이터를 영구 저장.
 */
class ArchiveViewModel(application: Application) : AndroidViewModel(application) {

    private val albumDao = AppDatabase.getDatabase(application).albumDao()
    private val _saveEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val saveEvents: SharedFlow<String> = _saveEvents.asSharedFlow()

    // DB에서 데이터를 실시간으로 관찰하여 UI 모델로 변환
    val archiveList: StateFlow<List<DummyArchive>> = albumDao.getAllAlbums()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** 초기 데이터 삽입 (최초 1회 실행용) */
    fun initDummyData() {
        viewModelScope.launch {
            val current = archiveList.value
            if (current.isEmpty()) {
                val dummy = listOf(
                    DummyArchive("1", "Midnight Vultures", "Beck", listOf("Funk", "Alt"), "2024.03.15"),
                    DummyArchive("2", "Selected Ambient Works", "Aphex Twin", listOf("Ambient", "Electronic"), "2024.03.12"),
                    DummyArchive("3", "Kind of Blue", "Miles Davis", listOf("Jazz", "Classic"), "2024.03.10"),
                    DummyArchive("4", "Discovery", "Daft Punk", listOf("Electronic", "House"), "2024.03.05")
                )
                dummy.forEach { addItem(it) }
            }
        }
    }

    /** 항목 삭제 */
    fun deleteItems(ids: Set<String>) {
        viewModelScope.launch {
            albumDao.deleteAlbums(ids.toList())
        }
    }

    /** 항목 추가 */
    fun addItem(item: DummyArchive) {
        viewModelScope.launch {
            albumDao.insertAlbum(AlbumEntity.fromDomain(item))
        }
    }

    fun saveCurrentTrack(
        trackId: String,
        currentTrack: NowPlayingTrack?,
        trackMetadata: TrackMetadata?
    ) {
        if (currentTrack == null || trackMetadata == null) return

        viewModelScope.launch {
            albumDao.insertAlbum(
                AlbumEntity.fromTrackSnapshot(
                    trackId = trackId,
                    track = currentTrack,
                    metadata = trackMetadata
                )
            )
            _saveEvents.tryEmit("보관함에 저장했습니다.")
        }
    }

    // ─── 통계 계산 프로퍼티 ───────────────────────────────────────────

    /** 장르별 개수 집계 (내림차순) */
    fun genreDistribution(list: List<DummyArchive>): List<Pair<String, Int>> {
        val counts = mutableMapOf<String, Int>()
        list.forEach { archive ->
            archive.genres.forEach { genre ->
                counts[genre] = (counts[genre] ?: 0) + 1
            }
        }
        val total = counts.values.sum().coerceAtLeast(1)
        return counts.entries
            .sortedByDescending { it.value }
            .map { (genre, count) -> genre to (count * 100 / total) }
    }

    /** 가장 많이 등장하는 장르 */
    fun topGenre(list: List<DummyArchive>): String {
        return genreDistribution(list).firstOrNull()?.first ?: "-"
    }

    /** 총 앨범 수 */
    fun totalAlbums(list: List<DummyArchive>): Int = list.size
}
