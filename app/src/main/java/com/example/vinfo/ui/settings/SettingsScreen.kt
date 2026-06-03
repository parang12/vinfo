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
import com.example.vinfo.data.settings.ThemeMode
import com.example.vinfo.data.settings.ThemeSettingsStore
import com.example.vinfo.ui.component.FloatingBackButton
import com.example.vinfo.ui.theme.VinfoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Boolean = { true },
    onClearAlbumArtCache: () -> String = { "앨범 커버 캐시를 삭제했습니다." },
    onClearArchive: () -> Unit = {}
) {
    val context = LocalContext.current
    val apiKeyStore = remember(context) { ApiKeyStore(context.applicationContext) }
    var geminiKey by remember { mutableStateOf(apiKeyStore.getGeminiApiKey()) }
    var apiKeySaveMessage by remember { mutableStateOf<String?>(null) }
    var apiKeySaveMessageColor by remember { mutableStateOf(Color(0xFF007A3D)) }
    var themeSaveMessage by remember { mutableStateOf<String?>(null) }
    var themeSaveMessageColor by remember { mutableStateOf(Color(0xFF007A3D)) }
    var dataActionMessage by remember { mutableStateOf<String?>(null) }
    var dataActionMessageColor by remember { mutableStateOf(Color(0xFF007A3D)) }

    val themeOptions = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "재생 중인 음악 알림을 읽어 아티스트와 곡 정보를 감지합니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                val trimmedKey = geminiKey.trim()
                                if (trimmedKey.isBlank()) {
                                    apiKeySaveMessage = "Gemini API Key를 입력해 주세요."
                                    apiKeySaveMessageColor = Color(0xFFBA1A1A)
                                    return@Button
                                }

                                val saved = apiKeyStore.saveGeminiApiKey(trimmedKey)
                                val persistedKey = apiKeyStore.getGeminiApiKey()
                                val verified = saved && persistedKey == trimmedKey
                                geminiKey = persistedKey
                                apiKeySaveMessage = if (verified) {
                                    "Gemini API Key가 저장되었습니다."
                                } else {
                                    "Gemini API Key 저장을 확인하지 못했습니다. 다시 시도해 주세요."
                                }
                                apiKeySaveMessageColor = if (verified) Color(0xFF007A3D) else Color(0xFFBA1A1A)
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

                    apiKeySaveMessage?.let { message ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.labelMedium,
                            color = apiKeySaveMessageColor,
                            modifier = Modifier.fillMaxWidth()
                        )
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
                            val selected = themeMode == title
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.surface else Color.Transparent
                                    )
                                    .clickable {
                                        val saved = onThemeModeChange(title)
                                        if (saved) {
                                            themeSaveMessage = "${title.label}가 적용되었습니다."
                                            themeSaveMessageColor = Color(0xFF007A3D)
                                        } else {
                                            themeSaveMessage = "테마 설정을 저장하지 못했습니다."
                                            themeSaveMessageColor = Color(0xFFBA1A1A)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }

                    themeSaveMessage?.let { message ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.labelMedium,
                            color = themeSaveMessageColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                SettingsSectionTitle("데이터 관리")
            }
            item {
                SettingsCard {
                    SettingsActionRow(
                        title = "앨범 커버 캐시 삭제",
                        onClick = {
                            dataActionMessage = onClearAlbumArtCache()
                            dataActionMessageColor = Color(0xFF007A3D)
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = Color(0xFFC1C6D7).copy(alpha = 0.4f)
                    )
                    SettingsActionRow(
                        title = "저장 기록 초기화",
                        titleColor = Color(0xFFBA1A1A),
                        onClick = {
                            onClearArchive()
                            dataActionMessage = "보관함 기록을 초기화했습니다."
                            dataActionMessageColor = Color(0xFFBA1A1A)
                        }
                    )

                    dataActionMessage?.let { message ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.labelMedium,
                            color = dataActionMessageColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 12.dp, top = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
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
            color = MaterialTheme.colorScheme.onSurface,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
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
            color = if (titleColor == Color(0xFF181C23)) MaterialTheme.colorScheme.onSurface else titleColor
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
