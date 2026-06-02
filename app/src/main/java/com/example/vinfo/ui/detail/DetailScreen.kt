package com.example.vinfo.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.vinfo.domain.model.GenreCategory
import com.example.vinfo.domain.model.NowPlayingTrack
import com.example.vinfo.domain.model.TrackMetadata
import com.example.vinfo.ui.component.FloatingBackButton
import com.example.vinfo.ui.component.FloatingSettingsButton
import com.example.vinfo.ui.component.GenreChip
import com.example.vinfo.ui.component.MetadataCard
import com.example.vinfo.ui.component.SectionHeader
import com.example.vinfo.ui.component.VinfoCard
import com.example.vinfo.ui.theme.VinfoTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    trackId: String?,
    albumArtUrl: String? = null,
    currentTrack: NowPlayingTrack? = null,
    trackMetadata: TrackMetadata? = null,
    originalLyrics: String? = null,
    isLyricsLoading: Boolean = false,
    lyricsErrorMessage: String? = null,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onAddToArchiveClick: () -> Unit = {}
) {
    val rememberedAlbumArtUrl = remember(albumArtUrl, currentTrack?.albumArtUrl) {
        albumArtUrl ?: currentTrack?.albumArtUrl
    }
    val detailTrackTitle = trackMetadata?.title ?: currentTrack?.title
    val detailArtist = trackMetadata?.artist ?: currentTrack?.artist ?: "아티스트 정보 없음"
    val detailAlbum = trackMetadata?.album ?: currentTrack?.album
    val detailTitle = detailAlbum ?: detailTrackTitle ?: "분석된 앨범"
    val canSaveToArchive = !trackId.isNullOrBlank() && currentTrack != null && trackMetadata != null
    val genreLabels = buildGenreLabels(trackMetadata)
    val albumRatings = buildAlbumRatings(trackMetadata)
    val criticsSummary = trackMetadata?.criticsSummary
        ?.takeIf { it.isNotBlank() }
        ?: "Catch Now로 가져온 분석 정보가 아직 없습니다. Gemini 응답을 받으면 이 영역에 평론 요약이 표시됩니다."
    val listeningGuide = trackMetadata?.listeningGuide
        ?.takeIf { it.isNotBlank() }
        ?: "Gemini가 감상 포인트를 가져오면 여기에 표시됩니다."
    val samplesUsed = trackMetadata?.samplesUsed.orEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9FF))
    ) {
        // 콘텐츠
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 96.dp,
                bottom = 110.dp // 하단바(~72dp) + 여백
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 히어로 섹션 - 앨범 아트 + 메타데이터
            item {
                VinfoCard(containerColor = Color(0xFFF1F3FE)) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DetailAlbumArtwork(albumArtUrl = rememberedAlbumArtUrl)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = detailTitle,
                                style = MaterialTheme.typography.titleLarge.copy(lineHeight = 32.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF181C23),
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = detailArtist,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF414755),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!detailTrackTitle.isNullOrBlank() && detailTrackTitle != detailTitle) {
                                Text(
                                    text = detailTrackTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF6B7280),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 장르 섹션
            item {
                MetadataCard(title = "장르") {
                    Text(
                        text = "대표 장르",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (genreLabels.isEmpty()) {
                        Text(
                            text = "확인된 앨범 장르가 아직 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280)
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            genreLabels.forEachIndexed { index, label ->
                                GenreChip(label, isPrimary = index == 0)
                            }
                        }
                    }
                }
            }

            // 점수 섹션
            item {
                VinfoCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader(title = "앨범 평점")
                        if (albumRatings.isEmpty()) {
                            Text(
                                text = "확인된 앨범 평점이 아직 없습니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B7280)
                            )
                        } else {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                albumRatings.forEach { rating ->
                                    DetailMetricCard(
                                        label = rating.label,
                                        value = rating.value,
                                        unit = rating.unit,
                                        valueColor = Color(0xFF0058BC)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 비평가 요약
            item {
                MetadataCard(title = "비평가 요약") {
                    Text(
                        text = criticsSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = Color(0xFF414755)
                    )
                }
            }

            // 샘플링 & 감상 가이드
            item {
                MetadataCard(title = "샘플링 & 감상 가이드") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        GuideItem(
                            icon = Icons.Default.Headphones,
                            title = "감상 포인트",
                            text = listeningGuide
                        )
                        if (samplesUsed.isNotEmpty()) {
                            HorizontalDivider(color = Color(0xFFC1C6D7).copy(alpha = 0.3f))
                            GuideItem(
                                icon = Icons.Default.CenterFocusStrong,
                                title = "샘플 정보",
                                text = samplesUsed.joinToString(separator = "\n")
                            )
                        }
                    }
                }
            }

            // 가사 섹션
            item {
                VinfoCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionHeader(title = "가사")
                        LyricsContent(
                            lyrics = originalLyrics,
                            isLoading = isLyricsLoading,
                            errorMessage = lyricsErrorMessage
                        )
                    }
                }
            }

            // '보관함에 추가' 버튼 — AI 면체 문구 바로 위
            item {
                Button(
                    onClick = onAddToArchiveClick,
                    enabled = canSaveToArchive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF181C23),
                        disabledContainerColor = Color(0xFFBFC6D5)
                    )
                ) {
                    Icon(
                        Icons.Default.BookmarkAdd,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (canSaveToArchive) "보관함에 추가" else "분석 후 저장",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // AI 면체 문구
            item {
                Text(
                    text = "Vinfo AI가 수집한 정보입니다. 데이터는 공개 음악 데이터베이스 및 알고리즘 분석을 통해 제공됩니다.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9CA3AF),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        }

        // 좌측: 뒤로가기 원형 버튼
        FloatingBackButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 20.dp, top = 12.dp)
        )

        // 우측: 설정 원형 버튼
        FloatingSettingsButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 20.dp, top = 12.dp)
        )

    }
}

private fun GenreCategory.displayName(): String {
    return when (this) {
        GenreCategory.HIP_HOP -> "Hip Hop"
        GenreCategory.POP -> "Pop"
        GenreCategory.ROCK -> "Rock"
        GenreCategory.ELECTRONIC -> "Electronic"
        GenreCategory.JAZZ -> "Jazz"
        GenreCategory.CLASSICAL -> "Classical"
        GenreCategory.RNB -> "R&B"
        GenreCategory.UNKNOWN -> "Unknown"
    }
}

private fun buildGenreLabels(trackMetadata: TrackMetadata?): List<String> {
    if (trackMetadata == null) return emptyList()
    return listOfNotNull(
        trackMetadata.primaryGenre.takeUnless { it == GenreCategory.UNKNOWN }?.displayName(),
        trackMetadata.secondaryGenre?.takeUnless { it == GenreCategory.UNKNOWN }?.displayName()
    ).distinct()
}

private data class AlbumRatingUi(
    val label: String,
    val value: String,
    val unit: String
)

private fun buildAlbumRatings(trackMetadata: TrackMetadata?): List<AlbumRatingUi> {
    if (trackMetadata == null) return emptyList()
    return listOfNotNull(
        trackMetadata.rymRating?.let { AlbumRatingUi("RYM", "%.2f".format(it), "/5") },
        trackMetadata.pitchforkScore?.let { AlbumRatingUi("Pitchfork", "%.1f".format(it), "/10") },
        trackMetadata.metacriticScore?.let { AlbumRatingUi("Metacritic", it.toString(), "/100") },
        trackMetadata.aotyScore?.let { AlbumRatingUi("AOTY", it.toString(), "/100") }
    )
}

@Composable
private fun DetailAlbumArtwork(albumArtUrl: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFEEF2F8),
                        Color(0xFFC3CFDB),
                        Color(0xFFEEF2F8)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        DetailAlbumArtworkFallback()

        if (!albumArtUrl.isNullOrBlank()) {
            AsyncImage(
                model = albumArtUrl,
                contentDescription = "앨범 커버",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
            )
        }
    }
}

@Composable
private fun DetailAlbumArtworkFallback() {
    Box(
        modifier = Modifier
            .size(262.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        Color(0xFF5B6673),
                        Color(0xFF2C3641),
                        Color(0xFF0C1116)
                    )
                )
            )
    )
    Box(
        modifier = Modifier
            .size(172.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        Color(0xFF38424E),
                        Color(0xFF141A20),
                        Color(0xFF080B0E)
                    )
                )
            )
    )
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E2630))
    )
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .rotate(-32f)
            .offset(x = (-88).dp, y = (-70).dp)
    )
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(Color(0xFFD8DCE4))
    )
}

@Composable
fun DetailMetricCard(
    label: String,
    value: String,
    unit: String = "",
    valueColor: Color = Color(0xFF181C23)
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF6B7280)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

@Composable
fun VerticalScoreDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(44.dp)
            .background(Color(0xFFC1C6D7).copy(alpha = 0.4f))
    )
}

@Composable
fun GuideItem(icon: ImageVector, title: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFEEF4FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color(0xFF0058BC)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF181C23)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF414755),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun LyricsContent(
    lyrics: String?,
    isLoading: Boolean,
    errorMessage: String?
) {
    when {
        isLoading -> Text(
            text = "가사를 가져오는 중입니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280)
        )
        !lyrics.isNullOrBlank() -> SelectionContainer {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                lyrics.lines().forEach { line ->
                    if (line.isBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFF181C23),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }
        else -> Text(
            text = errorMessage ?: "가사를 찾을 수 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280)
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun DetailScreenPreview() {
    VinfoTheme {
        DetailScreen(
            trackId = "preview",
            onBackClick = {},
            onSettingsClick = {}
        )
    }
}
