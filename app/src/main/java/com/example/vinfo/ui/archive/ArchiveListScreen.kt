package com.example.vinfo.ui.archive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vinfo.ui.component.FloatingSettingsButton
import com.example.vinfo.ui.component.GenreChip
import com.example.vinfo.ui.theme.VinfoPrimary
import com.example.vinfo.ui.theme.VinfoTheme

// ─── 데이터 클래스 ───────────────────────────────────────────────────────────
data class DummyArchive(
    val id: String,
    val title: String,
    val artist: String,
    val genres: List<String>,
    val date: String
)

// ─── 화면 ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArchiveListScreen(
    onTrackClick: (String) -> Unit,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSelectionModeChange: (Boolean) -> Unit = {},
    archiveViewModel: ArchiveViewModel = viewModel()
) {
    val archiveList by archiveViewModel.archiveList.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedGenreSegment by rememberSaveable { mutableStateOf<String?>(null) }

    // 선택 모드 상태
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    val filteredData = archiveList.filter { item ->
        item.title.contains(searchQuery, ignoreCase = true) ||
            item.artist.contains(searchQuery, ignoreCase = true)
    }
    val genreSegments = remember(archiveList) { buildArchiveGenreSegments(archiveList) }
    val selectedSegment = genreSegments.firstOrNull { it.genre == selectedGenreSegment }
        ?: genreSegments.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 콘텐츠
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 96.dp,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 검색바
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "보관함 검색...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF9CA3AF)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                ArchiveGenreDistributionBar(
                    segments = genreSegments,
                    selectedSegment = selectedSegment,
                    onSegmentClick = { segment -> selectedGenreSegment = segment.genre }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 앨범 카드 목록
            items(filteredData, key = { it.id }) { track ->
                val isSelected = selectedIds.contains(track.id)
                ArchiveItem(
                    track = track,
                    isSelectionMode = isSelectionMode,
                    isSelected = isSelected,
                    onClick = {
                        if (isSelectionMode) {
                            selectedIds = if (isSelected) {
                                selectedIds - track.id
                            } else {
                                selectedIds + track.id
                            }
                        } else {
                            onTrackClick(track.id)
                        }
                    },
                    onLongClick = {
                        if (!isSelectionMode) {
                            isSelectionMode = true
                            selectedIds = setOf(track.id)
                            onSelectionModeChange(true)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // 선택 모드가 아닐 때: 설정 버튼
        if (!isSelectionMode) {
            FloatingSettingsButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 20.dp, top = 12.dp)
            )
        }

        // 선택 모드일 때: 하단 액션바 — 하단바와 동일한 위치/스타일
        AnimatedVisibility(
            visible = isSelectionMode,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                shadowElevation = 14.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 취소 버튼
                    Button(
                        onClick = {
                            isSelectionMode = false
                            selectedIds = emptySet()
                            onSelectionModeChange(false)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "취소",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 삭제 버튼
                    Button(
                        onClick = {
                            archiveViewModel.deleteItems(selectedIds)
                            isSelectionMode = false
                            selectedIds = emptySet()
                            onSelectionModeChange(false)
                        },
                        enabled = selectedIds.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFBA1A1A),
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "삭제 (${selectedIds.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveGenreDistributionBar(
    segments: List<ArchiveGenreSegment>,
    selectedSegment: ArchiveGenreSegment?,
    onSegmentClick: (ArchiveGenreSegment) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "장르 비율",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = selectedSegment?.let {
                            "${it.genre} ${it.percent}% · 앨범 ${it.albumCount}장"
                        } ?: "저장된 장르 데이터가 없습니다.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (segments.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFE8ECF5))
                ) {
                    segments.forEachIndexed { index, segment ->
                        Box(
                            modifier = Modifier
                                .weight(segment.percent.coerceAtLeast(1).toFloat())
                                .height(14.dp)
                                .background(archiveGenreColor(index))
                                .clickable { onSegmentClick(segment) }
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    segments.forEachIndexed { index, segment ->
                        val selected = segment.genre == selectedSegment?.genre
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) Color(0xFFEAF2FF) else Color.Transparent)
                                .clickable { onSegmentClick(segment) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(archiveGenreColor(index))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${segment.genre} ${segment.percent}%",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) VinfoPrimary else Color(0xFF4B5563)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun archiveGenreColor(index: Int): Color {
    return listOf(
        Color(0xFF0058BC),
        Color(0xFF22C55E),
        Color(0xFFF59E0B),
        Color(0xFF8B5CF6)
    )[index % 4]
}

// ─── 아이템 컴포넌트 ─────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArchiveItem(
    track: DummyArchive,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(
                if (isSelected) Modifier.border(2.dp, VinfoPrimary, RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        shadowElevation = if (isSelected) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 선택 체크박스
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) VinfoPrimary else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            // 텍스트 정보
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9CA3AF)
                )
            }

            // 장르 칩 (우측)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                track.genres.take(1).forEach { genreLabel ->
                    GenreChip(genre = genreLabel)
                }
            }
        }
    }
}

// ─── Preview ─────────────────────────────────────────────────────────────────
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun ArchiveListScreenPreview() {
    VinfoTheme {
        ArchiveListScreen(onTrackClick = {})
    }
}
