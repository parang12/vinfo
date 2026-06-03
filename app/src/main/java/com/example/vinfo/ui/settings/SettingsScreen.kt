package com.example.vinfo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.vinfo.data.settings.ApiKeyStore
import com.example.vinfo.ui.component.FloatingBackButton
import com.example.vinfo.ui.theme.VinfoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val apiKeyStore = remember(context) { ApiKeyStore(context.applicationContext) }
    var geminiKey by remember { mutableStateOf(apiKeyStore.getGeminiApiKey()) }
    var selectedTheme by remember { mutableIntStateOf(0) }

    val themeOptions = listOf("라이트 모드", "다크 모드", "시스템 기본값")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9FF))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 96.dp,    // statusBar + 뒤로가기 버튼 영역 확보
                bottom = 110.dp // 하단바(~72dp) + 여백(38dp)
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                SettingsSectionTitle("권한 상태")
            }
            item {
                SettingsCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "알림 접근 권한",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF181C23)
                        )
                        Text(
                            text = "재생 중인 음악 알림을 읽어 아티스트와 곡 정보를 감지합니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF414755)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                SettingsSectionTitle("API 설정")
            }
            item {
                SettingsCard {
                    SettingsInputItem(
                        label = "Gemini API Key",
                        value = geminiKey,
                        onValueChange = { geminiKey = it },
                        placeholder = "AIza..."
                    )
                    Text(
                        text = "리뷰 요약 및 생성에 사용됩니다.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                apiKeyStore.saveGeminiApiKey(geminiKey)
                            },
                            modifier = Modifier.height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0058BC))
                        ) {
                            Text(
                                text = "저장",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                SettingsSectionTitle("테마 설정")
            }
            item {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .background(Color(0xFFF1F3FE), RoundedCornerShape(20.dp))
                            .padding(4.dp)
                    ) {
                        themeOptions.forEachIndexed { index, title ->
                            val selected = selectedTheme == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (selected) Color.White else Color.Transparent
                                    )
                                    .clickable { selectedTheme = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color(0xFF181C23) else Color(0xFF6B7280)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                SettingsSectionTitle("데이터 관리")
            }
            item {
                SettingsCard {
                    SettingsActionRow(title = "캐시 삭제", onClick = {})
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = Color(0xFFC1C6D7).copy(alpha = 0.4f)
                    )
                    SettingsActionRow(
                        title = "저장 기록 초기화",
                        titleColor = Color(0xFFBA1A1A),
                        onClick = {}
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Vinfo v1.0.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9CA3AF)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "이용약관",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0058BC)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(10.dp)
                                .background(Color(0xFFD1D5DB))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "개인정보 처리방침",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0058BC)
                        )
                    }
                }
            }
        }

        // 좌측 상단 뒤로가기 원형 버튼 (펀치홀 카메라 여백 포함)
        FloatingBackButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 20.dp, top = 12.dp)
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF181C23),
        modifier = Modifier.padding(bottom = 12.dp, top = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
fun SettingsInputItem(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF374151),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFBEC5D0)
                )
            },
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFEEF0F8),
                unfocusedContainerColor = Color(0xFFEEF0F8),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    titleColor: Color = Color(0xFF181C23),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = titleColor
        )
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFD1D5DB)
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun SettingsScreenPreview() {
    VinfoTheme {
        SettingsScreen(
            onBackClick = {}
        )
    }
}
