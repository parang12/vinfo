package com.example.vinfo.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vinfo.ui.archive.ArchiveViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.vinfo.ui.component.FloatingSettingsButton
import com.example.vinfo.ui.component.VinfoCard
import com.example.vinfo.ui.theme.VinfoTheme
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.entryModelOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreStatsScreen(
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    archiveViewModel: ArchiveViewModel = viewModel()
) {
    var selectedPeriod by remember { mutableIntStateOf(2) }
    val periods = listOf("30d", "90d", "1y")

    // 보관함 데이터 구독
    val archiveList by archiveViewModel.archiveList.collectAsState()
    val genreDistribution = archiveViewModel.genreDistribution(archiveList)
    val topGenre = archiveViewModel.topGenre(archiveList)
    val totalAlbums = archiveViewModel.totalAlbums(archiveList)
    val topGenrePercent = genreDistribution.firstOrNull()?.second ?: 0

    // Box 오버레이 구조: TopBar가 콘텐츠 위에 떠 있음
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
                top = 96.dp,    // statusBar + 설정 버튼 영역 확보
                bottom = 110.dp // 하단바(~72dp) + 여백(38dp)
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 화면 제목
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "장르 통계",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF181C23)
                    )
                    Text(
                        text = "나의 컬렉션 분석 결과입니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            // 기간 선택 세그먼트
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(Color(0xFFE8EAF6), RoundedCornerShape(22.dp))
                        .padding(4.dp)
                ) {
                    periods.forEachIndexed { index, title ->
                        val selected = selectedPeriod == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (selected) Color(0xFF0058BC) else Color.Transparent
                                )
                                .clickable { selectedPeriod = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }

            // 도넛 차트 + 장르 분포 카드
            item {
                VinfoCard {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.size(172.dp),
                            strokeWidth = 24.dp,
                            color = Color(0xFFE8EAF6),
                            trackColor = Color.Transparent,
                            strokeCap = StrokeCap.Round
                        )
                        androidx.compose.material3.CircularProgressIndicator(
                            progress = { topGenrePercent / 100f },
                            modifier = Modifier.size(172.dp),
                            strokeWidth = 24.dp,
                            color = Color(0xFF0058BC),
                            trackColor = Color.Transparent,
                            strokeCap = StrokeCap.Round
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalAlbums",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF181C23),
                                lineHeight = 44.sp
                            )
                            Text(
                                text = "보관함 앨범",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = Color(0xFFC1C6D7).copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "장르 분포",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF181C23)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val genreColors = listOf(
                        Color(0xFF0058BC), Color(0xFF3B82F6), Color(0xFF60A5FA),
                        Color(0xFFBFDBFE), Color(0xFFE5E7EB)
                    )
                    val distribution = genreDistribution.take(5).mapIndexed { i, (name, percent) ->
                        Triple(name, percent, genreColors.getOrElse(i) { Color(0xFFE5E7EB) })
                    }

                    distribution.forEach { (name, percent, color) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                                color = Color(0xFF374151)
                            )
                            Text(
                                text = "$percent%",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF181C23)
                            )
                        }
                    }
                }
            }

            // KPI 카드 - 가장 많이 들은 장르
            item {
                VinfoCard {
                    Text(
                        text = "가장 많이 들은 장르",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6B7280)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = topGenre,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0058BC)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF0058BC)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "컬렉션과 일관됨",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            Icons.Default.Headphones,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .padding(top = 4.dp),
                            tint = Color(0xFF0058BC).copy(alpha = 0.15f)
                        )
                    }
                }
            }

            // KPI 카드 - 전체 앙범 수
            item {
                VinfoCard {
                    Text(
                        text = "전체 보관 앙범",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6B7280)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$totalAlbums",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF181C23)
                        )
                        Text(
                            text = " / 5.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(2.dp)
                                .background(Color(0xFF6B7280))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "컬렉션과 일관됨",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .padding(top = 4.dp),
                            tint = Color(0xFFD97706).copy(alpha = 0.18f)
                        )
                    }
                }
            }

            // 주간 막대 차트 카드
            item {
                VinfoCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "장르 분포",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF181C23)
                        )
                        Surface(
                            color = Color(0xFFEEF4FF),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = "청취 시간",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0058BC)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Chart(
                        chart = columnChart(),
                        model = entryModelOf(4, 5, 3, 6, 7, 5, 8),
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                        color = Color(0xFFC1C6D7).copy(alpha = 0.35f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("월", "화", "수", "목", "금", "토", "일").forEach { day ->
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }
        }

        // 우측 상단 설정 원형 버튼 (펀치홀 카메라 여백 포함)
        FloatingSettingsButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 20.dp, top = 12.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun GenreStatsScreenPreview() {
    VinfoTheme {
        GenreStatsScreen(
            onBackClick = {},
            onSettingsClick = {}
        )
    }
}
