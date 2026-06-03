package com.example.vinfo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.example.vinfo.ui.permission.NotificationPermissionBanner
import com.example.vinfo.ui.permission.openNotificationListenerSettings
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vinfo.domain.model.buildTrackId
import com.example.vinfo.ui.archive.ArchiveListScreen
import com.example.vinfo.ui.archive.ArchiveViewModel
import com.example.vinfo.ui.detail.DetailScreen
import com.example.vinfo.ui.navigation.Route
import com.example.vinfo.ui.nowplaying.NowPlayingScreen
import com.example.vinfo.ui.nowplaying.NowPlayingViewModel
import com.example.vinfo.ui.settings.SettingsScreen
import com.example.vinfo.ui.permission.isNotificationListenerEnabled
import com.example.vinfo.ui.permission.openAppNotificationSettings
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import com.example.vinfo.data.settings.ThemeMode
import com.example.vinfo.data.settings.ThemeSettingsStore
import com.example.vinfo.ui.stats.GenreMapScreen
import com.example.vinfo.ui.stats.GenreMapViewModel
import com.example.vinfo.ui.theme.VinfoPrimary
import com.example.vinfo.ui.theme.VinfoTheme
import kotlinx.coroutines.flow.collectLatest
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val themeSettingsStore = remember(context) { ThemeSettingsStore(context.applicationContext) }
            val themeMode by themeSettingsStore.themeMode.collectAsState()
            val systemDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDarkTheme
            }

            VinfoTheme(darkTheme = darkTheme) {
                MainScreen(
                    themeMode = themeMode,
                    onThemeModeChange = themeSettingsStore::saveThemeMode
                )
            }
        }
    }
}

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object NowPlaying : BottomNavItem(Route.NowPlaying.path, "홈", Icons.Default.Home)
    object Archive : BottomNavItem(Route.Archive.path, "보관함", Icons.Default.LibraryMusic)
    object Stats : BottomNavItem(Route.GenreStats.path, "인사이트", Icons.Default.Map)
}

@Composable
fun MainScreen(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Boolean = { true }
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    val bottomNavItems = listOf(
        BottomNavItem.NowPlaying,
        BottomNavItem.Archive,
        BottomNavItem.Stats
    )

    // Activity 레벨에서 공유 ViewModel 생성 — 보관함과 통계가 동일 인스턴스 사용
    val archiveViewModel: ArchiveViewModel = viewModel()
    val nowPlayingViewModel: NowPlayingViewModel = viewModel()
    val nowPlayingState by nowPlayingViewModel.uiState.collectAsState()
    val archiveList by archiveViewModel.archiveList.collectAsState()

    androidx.compose.runtime.LaunchedEffect(nowPlayingViewModel) {
        nowPlayingViewModel.navigationEvents.collect { trackId ->
            navController.navigate(
                Route.Detail.createRoute(
                    trackId = trackId,
                    albumArtUrl = nowPlayingViewModel.uiState.value.currentTrack?.albumArtUrl
                )
            )
        }
    }

    androidx.compose.runtime.LaunchedEffect(archiveViewModel) {
        archiveViewModel.saveEvents.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // 보관함 선택 모드 상태 (하단바 투명 처리용)
    var isArchiveSelectionMode by remember { mutableStateOf(false) }

    // Box 오버레이 구조: 하단바가 콘텐츠 위에 떠 있음
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 알림 리스너 권한 배너 (상단 오버레이)
        val ctx = LocalContext.current
        NotificationPermissionBanner(
            modifier = Modifier.align(Alignment.TopCenter),
            onOpenSettings = {
                openNotificationListenerSettings(ctx)
            }
        )

        // Catch Now 클릭 시 권한이 없으면 하단 안내 시트 표시
        var showPermissionSheet by remember { mutableStateOf(false) }

        // 콘텐츠 (전체 화면 사용)
        NavHost(
            navController = navController,
            startDestination = Route.NowPlaying.path,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Route.NowPlaying.path) {
                NowPlayingScreen(
                    onBackClick = { navController.popBackStack() },
                    onCatchNowClick = {
                        // 권한 확인 후 동작
                        if (!isNotificationListenerEnabled(ctx)) {
                            showPermissionSheet = true
                        } else {
                            nowPlayingViewModel.catchNow()
                        }
                    },
                    onSettingsClick = {
                        navController.navigate(Route.Settings.path)
                    },
                    onViewAllArchiveClick = {
                        navController.navigate(Route.Archive.path) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenMapClick = {
                        navController.navigate(Route.GenreStats.path) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    isLoading = nowPlayingState.isLoading,
                    isCatchNowEnabled = !nowPlayingState.isLoading && !nowPlayingState.isLyricsLoading,
                    statusMessage = nowPlayingState.statusMessage,
                    currentTrack = nowPlayingState.currentTrack,
                    recentArchiveItems = archiveList.take(2)
                )
            }
            composable(
                route = Route.Detail.path,
                arguments = listOf(
                    navArgument("albumArtUrl") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val trackId = backStackEntry.arguments?.getString("trackId") ?: ""
                val albumArtUrl = backStackEntry.arguments?.getString("albumArtUrl")
                var archivedAlbum by remember(trackId) { mutableStateOf<com.example.vinfo.data.local.entity.AlbumEntity?>(null) }
                val currentTrackId = nowPlayingState.trackMetadata?.let { buildTrackId(it.artist, it.title) }
                val isCurrentCatchNowDetail = currentTrackId == trackId

                androidx.compose.runtime.LaunchedEffect(trackId, currentTrackId) {
                    archivedAlbum = if (trackId.isNotBlank() && !isCurrentCatchNowDetail) {
                        archiveViewModel.getAlbumById(trackId)
                    } else {
                        null
                    }
                }

                val detailTrack = if (isCurrentCatchNowDetail) {
                    nowPlayingState.currentTrack
                } else {
                    archivedAlbum?.toNowPlayingTrack()
                }
                val detailMetadata = if (isCurrentCatchNowDetail) {
                    nowPlayingState.trackMetadata
                } else {
                    archivedAlbum?.toTrackMetadata()
                }

                DetailScreen(
                    trackId = trackId,
                    albumArtUrl = albumArtUrl,
                    currentTrack = detailTrack,
                    trackMetadata = detailMetadata,
                    originalLyrics = if (isCurrentCatchNowDetail) nowPlayingState.originalLyrics else null,
                    isLyricsLoading = if (isCurrentCatchNowDetail) nowPlayingState.isLyricsLoading else false,
                    lyricsErrorMessage = if (isCurrentCatchNowDetail) nowPlayingState.lyricsErrorMessage else null,
                    onBackClick = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(Route.Settings.path) },
                    onAddToArchiveClick = {
                        archiveViewModel.saveCurrentTrack(
                            trackId = trackId,
                            currentTrack = detailTrack,
                            trackMetadata = detailMetadata
                        )
                    }
                )
            }
            composable(Route.Archive.path) {
                ArchiveListScreen(
                    onTrackClick = { id ->
                        navController.navigate(Route.Detail.createRoute(id))
                    },
                    onBackClick = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(Route.Settings.path) },
                    onSelectionModeChange = { isSelectionMode ->
                        isArchiveSelectionMode = isSelectionMode
                    },
                    archiveViewModel = archiveViewModel
                )
            }
            composable(Route.GenreStats.path) {
                val genreMapViewModel: GenreMapViewModel = viewModel()
                val discoveryState by genreMapViewModel.discoveryState.collectAsState()
                GenreMapScreen(
                    archiveItems = archiveList,
                    discoveryState = discoveryState,
                    onFindNearbyGenres = genreMapViewModel::findNearbyGenres,
                    onDismissDiscoveryPopup = genreMapViewModel::dismissDiscoveryPopup,
                    onConfirmDiscoveryCandidates = genreMapViewModel::confirmDiscoveryCandidates,
                    onBackClick = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(Route.Settings.path) },
                )
            }
            composable(Route.Settings.path) {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() },
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    onClearAlbumArtCache = {
                        val cacheDir = File(ctx.filesDir, "album_art")
                        if (!cacheDir.exists() || cacheDir.deleteRecursively()) {
                            "앨범 커버 캐시를 삭제했습니다."
                        } else {
                            "앨범 커버 캐시를 완전히 삭제하지 못했습니다."
                        }
                    },
                    onClearArchive = archiveViewModel::clearArchive
                )
            }
        }

        // 플로팅 하단바 — 콘텐츠 위에 오버레이
        val showBottomBar = currentRoute in setOf(
            Route.NowPlaying.path,
            Route.Archive.path,
            Route.Detail.path
        )

        // 보관함 선택 모드에서는 하단바를 완전히 숨김 (터치 이벤트 차단 방지)
        if (showBottomBar && !isArchiveSelectionMode) {
            val activeRoute = if (currentRoute == Route.Detail.path) {
                Route.NowPlaying.path
            } else {
                currentRoute
            }

            FloatingBottomNavigation(
                items = bottomNavItems,
                activeRoute = activeRoute,
                onDestinationClick = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 112.dp)
        )

        // 간단한 하단 권한 안내 시트
        if (showPermissionSheet) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                shadowElevation = 12.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "알림 접근 권한이 필요합니다.", style = MaterialTheme.typography.titleMedium)
                    Text(text = "다른 앱의 재생 상태를 감지하려면 알림 접근 권한을 허용해주세요.", modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            openNotificationListenerSettings(ctx)
                            showPermissionSheet = false
                        }) {
                            Text(text = "권한 설정 열기")
                        }
                        TextButton(onClick = {
                            openAppNotificationSettings(ctx)
                            showPermissionSheet = false
                        }) {
                            Text(text = "앱 설정")
                        }
                        TextButton(onClick = { showPermissionSheet = false }) {
                            Text(text = "취소")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingBottomNavigation(
    items: List<BottomNavItem>,
    activeRoute: String?,
    onDestinationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 16.dp),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = activeRoute == item.route
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onDestinationClick(item.route) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        // 선택 상태: 아이콘 + 텍스트를 함께 감싸는 알약형 배경
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(18.dp),
                                tint = VinfoPrimary
                            )
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = VinfoPrimary
                            )
                        }
                    } else {
                        // 미선택 상태: 선택과 동일한 Row 구조, 배경만 없음
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun MainScreenPreview() {
    VinfoTheme {
        MainScreen()
    }
}
