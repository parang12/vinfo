package com.example.vinfo.ui.permission

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// 간단한 권한 유틸: 알림 리스너 활성화 여부 확인
fun isNotificationListenerEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (flat.isNullOrEmpty()) return false
    // 패키지명이 포함되어 있으면 우선 허용된 것으로 판단
    return flat.contains(context.packageName)
}

fun openNotificationListenerSettings(context: Context) {
    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

fun openAppNotificationSettings(context: Context) {
    // Open app-specific settings page (so user can also enable system "알림" toggles)
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.data = Uri.parse("package:" + context.packageName)
    context.startActivity(intent)
}

@Composable
fun NotificationPermissionBanner(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val visible = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible.value = !isNotificationListenerEnabled(context)
    }

    if (visible.value) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                    Text(
                        text = "알림 권한이 필요합니다. 재생 상태를 감지하려면 허용해주세요.",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Row {
                    Button(onClick = { onOpenSettings() }) {
                        Text(text = "권한 설정")
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(6.dp))
                    Button(onClick = { openAppNotificationSettings(context) }) {
                        Text(text = "앱 설정")
                    }
                }
            }
        }
    }
}
