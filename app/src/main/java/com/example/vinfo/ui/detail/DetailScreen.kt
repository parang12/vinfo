package com.example.vinfo.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onAddToArchiveClick: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

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
                                    .align(Alignment.TopStart)
                                    .offset(x = 50.dp, y = 70.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD8DCE4))
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Silent Alarm",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF181C23),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Bloc Party",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF414755)
                            )
                            Text(
                                text = "발매일: 2005. 02. 14.",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF6B7280)
                            )
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
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GenreChip("Indie Rock", isPrimary = true)
                        GenreChip("Post-Punk Revival", isPrimary = true)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "세부 장르",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GenreChip("Alternative")
                        GenreChip("British Indie")
                    }
                }
            }

            // 점수 섹션
            item {
                VinfoCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DetailMetricCard(label = "RYM 평점", value = "3.82")
                        VerticalScoreDivider()
                        DetailMetricCard(label = "비평가 점수", value = "88", unit = "/100")
                        VerticalScoreDivider()
                        DetailMetricCard(
                            label = "AI 일치도",
                            value = "94",
                            unit = "%",
                            valueColor = Color(0xFF0058BC)
                        )
                    }
                }
            }

            // 비평가 요약
            item {
                MetadataCard(title = "비평가 요약") {
                    Text(
                        text = "2000년대 초반 포스트 펑크 리바이벌의 불안한 에너지를 단단한 리듬과 날카로운 기타로 밀어붙이는 트랙입니다. 비평과 감상의 언어가 겹치는 순간을 보여주며, 장르의 긴장감과 구조적인 정확성을 동시에 전달합니다.",
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
                            title = "하이햇 연주에 주목하세요",
                            text = "드러머의 싱코페이티드 16분 음표가 전체 편곡을 이끌어갑니다."
                        )
                        HorizontalDivider(color = Color(0xFFC1C6D7).copy(alpha = 0.3f))
                        GuideItem(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            title = "2:15의 기타 상호작용",
                            text = "두 기타가 믹스를 흐리지 않으면서 어떻게 서로 상반된 멜로디를 엮어내는지 들어보세요."
                        )
                        HorizontalDivider(color = Color(0xFFC1C6D7).copy(alpha = 0.3f))
                        GuideItem(
                            icon = Icons.Default.CenterFocusStrong,
                            title = "아날로그의 따뜻함",
                            text = "테이프로 녹음되어 거친 주파수를 잡아주는 자연스러운 압축감을 유지합니다."
                        )
                    }
                }
            }

            // 가사 섹션
            item {
                VinfoCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionHeader(title = "가사")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFFEEF0F8))
                                .padding(4.dp)
                        ) {
                            listOf("원문", "번역").forEachIndexed { index, tabTitle ->
                                val selected = selectedTabIndex == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            if (selected) Color.White else Color.Transparent
                                        )
                                        .clickable { selectedTabIndex = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tabTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color(0xFF0058BC) else Color(0xFF6B7280)
                                    )
                                }
                            }
                        }
                        SelectionContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val lyrics = if (selectedTabIndex == 0) {
                                    listOf(
                                        "Are you hoping for a miracle?",
                                        "Are you hoping for a miracle?",
                                        "Are you hoping for a miracle?",
                                        "",
                                        "It's just a matter of time",
                                        "It's just a matter of time",
                                        "",
                                        "We're going to win this",
                                        "We're going to win this"
                                    )
                                } else {
                                    listOf(
                                        "기적을 바라고 있나요?",
                                        "기적을 바라고 있나요?",
                                        "기적을 바라고 있나요?",
                                        "",
                                        "단지 시간 문제일 뿐이에요",
                                        "단지 시간 문제일 뿐이에요",
                                        "",
                                        "우리는 이겨낼 거예요",
                                        "우리는 이겨낼 거예요"
                                    )
                                }
                                lyrics.forEach { line ->
                                    if (line.isEmpty()) {
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
                    }
                }
            }

            // '보관함에 추가' 버튼 — AI 면체 문구 바로 위
            item {
                Button(
                    onClick = onAddToArchiveClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF181C23)
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
                        text = "보관함에 추가",
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
