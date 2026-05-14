package com.example.vinfo.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinfo.ui.archive.DummyArchive
import com.example.vinfo.ui.component.FloatingSettingsButton
import com.example.vinfo.ui.component.SectionHeader
import com.example.vinfo.ui.component.SkeletonBox
import com.example.vinfo.ui.component.VinfoCard
import com.example.vinfo.ui.theme.VinfoTheme

@Composable
fun NowPlayingScreen(
    onBackClick: () -> Unit,
    onCatchNowClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onViewAllArchiveClick: () -> Unit = {},
    isLoading: Boolean = false // 로딩 상태 추가
) {
    // 보관함에서 가져온 최근 2개 항목 (DummyArchive 재사용)
    val recentArchiveItems = listOf(
        DummyArchive("1", "Midnight City", "M83", listOf("Synth-pop", "Indie"), "2024.03.15"),
        DummyArchive("2", "Instant Crush", "Daft Punk ft. Julian Casablancas", listOf("Electronic", "Pop"), "2024.03.12")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9FF))
    ) {
        // 상단 1/3 영역 — Vinfo 제목 가운데 배치
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.33f)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Vinfo",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF181C23),
                textAlign = TextAlign.Center
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 0.dp,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 상단 1/3만큼 빈 공간 확보
            item { Spacer(modifier = Modifier.fillParentMaxHeight(0.33f)) }

            // 히어로 카드 - 현재 재생 중인 곡
            item {
                if (isLoading) {
                    SkeletonHeroCard()
                } else {
                    VinfoCard {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // 바이닐 아트워크 영역
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF091018),
                                                Color(0xFF141B24),
                                                Color(0xFF05070A)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(248.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    Color(0xFF2E3946),
                                                    Color(0xFF18212B),
                                                    Color(0xFF06090D)
                                                )
                                            )
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(170.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    Color(0xFF5A6572),
                                                    Color(0xFF1D2731),
                                                    Color(0xFF0A0D11)
                                                )
                                            )
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(116.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF26313D))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(86.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .rotate(-32f)
                                        .background(Color.White.copy(alpha = 0.92f))
                                        .align(Alignment.CenterStart)
                                        .offset(x = 44.dp, y = (-42).dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD8DCE4))
                                )
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(16.dp)
                                        .size(48.dp),
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.95f),
                                    shadowElevation = 4.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = Color(0xFF0058BC),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Echoes of Tomorrow",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF181C23)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Neon Synthesizers",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF6B7280)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    listOf(18.dp, 30.dp, 14.dp, 26.dp, 20.dp, 10.dp, 28.dp).forEach { barHeight ->
                                        Box(
                                            modifier = Modifier
                                                .width(6.dp)
                                                .height(barHeight)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(Color(0xFF4EA1FF))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Catch Now CTA 버튼
            item {
                Button(
                    onClick = onCatchNowClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0058BC))
                ) {
                    Icon(
                        Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Catch Now",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 최근 감상 섹션 헤더
            item {
                SectionHeader(
                    title = "최근 감상",
                    actionLabel = "전체보기",
                    onActionClick = onViewAllArchiveClick
                )
            }

            // 보관함 스타일 카드 2개
            if (isLoading) {
                items(2) { SkeletonRecentCard() }
            } else {
                items(recentArchiveItems.size) { index ->
                    val item = recentArchiveItems[index]
                    RecentArchiveCard(item = item)
                }
            }
        }

        // 우측 상단 설정 원형 버튼
        FloatingSettingsButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 20.dp, top = 12.dp)
        )
    }
}

@Composable
fun SkeletonHeroCard() {
    VinfoCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(20.dp)
            )
            SkeletonBox(modifier = Modifier.width(180.dp).height(24.dp))
            SkeletonBox(modifier = Modifier.width(120.dp).height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(7) { SkeletonBox(modifier = Modifier.width(6.dp).height(20.dp)) }
            }
        }
    }
}

@Composable
fun SkeletonRecentCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White
    ) {
        Row(modifier = Modifier.padding(20.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBox(modifier = Modifier.width(140.dp).height(20.dp))
                SkeletonBox(modifier = Modifier.width(100.dp).height(16.dp))
                SkeletonBox(modifier = Modifier.width(60.dp).height(14.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonBox(modifier = Modifier.width(60.dp).height(24.dp), shape = RoundedCornerShape(999.dp))
                SkeletonBox(modifier = Modifier.width(60.dp).height(24.dp), shape = RoundedCornerShape(999.dp))
            }
        }
    }
}

@Composable
fun RecentArchiveCard(item: DummyArchive) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF181C23)
                )
                Text(
                    text = item.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF414755)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.date,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6B7280)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item.genres.take(2).forEach { genre ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFFEEF0F8)
                    ) {
                        Text(
                            text = genre,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF414755)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun NowPlayingScreenPreview() {
    VinfoTheme {
        NowPlayingScreen(
            onBackClick = {},
            onCatchNowClick = {},
            onSettingsClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun NowPlayingScreenLoadingPreview() {
    VinfoTheme {
        NowPlayingScreen(
            onBackClick = {},
            onCatchNowClick = {},
            onSettingsClick = {},
            isLoading = true
        )
    }
}
