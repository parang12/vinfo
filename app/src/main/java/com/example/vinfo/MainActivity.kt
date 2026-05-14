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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vinfo.ui.archive.ArchiveListScreen
import com.example.vinfo.ui.archive.ArchiveViewModel
import com.example.vinfo.ui.detail.DetailScreen
import com.example.vinfo.ui.navigation.Route
import com.example.vinfo.ui.nowplaying.NowPlayingScreen
import com.example.vinfo.ui.settings.SettingsScreen
import com.example.vinfo.ui.stats.GenreStatsScreen
import com.example.vinfo.ui.theme.VinfoPrimary
import com.example.vinfo.ui.theme.VinfoSurface
import com.example.vinfo.ui.theme.VinfoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VinfoTheme {
                MainScreen()
            }
        }
    }
}

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object NowPlaying : BottomNavItem(Route.NowPlaying.path, "홈", Icons.Default.Home)
    object Archive : BottomNavItem(Route.Archive.path, "보관함", Icons.Default.LibraryMusic)
    object Stats : BottomNavItem(Route.GenreStats.path, "통계", Icons.Default.BarChart)
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val bottomNavItems = listOf(
        BottomNavItem.NowPlaying,
        BottomNavItem.Archive,
        BottomNavItem.Stats
    )

    // Activity 레벨에서 공유 ViewModel 생성 — 보관함과 통계가 동일 인스턴스 사용
    val archiveViewModel: ArchiveViewModel = viewModel()
    
    // 최초 실행 시 더미 데이터 삽입
    androidx.compose.runtime.LaunchedEffect(Unit) {
        archiveViewModel.initDummyData()
    }

    // 보관함 선택 모드 상태 (하단바 투명 처리용)
    var isArchiveSelectionMode by remember { mutableStateOf(false) }

    // Box 오버레이 구조: 하단바가 콘텐츠 위에 떠 있음
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VinfoSurface)
    ) {
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
                        navController.navigate(Route.Detail.createRoute("runaway_kanye_west"))
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
                    }
                )
            }
            composable(Route.Detail.path) { backStackEntry ->
                val trackId = backStackEntry.arguments?.getString("trackId") ?: ""
                DetailScreen(
                    trackId = trackId,
                    onBackClick = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(Route.Settings.path) }
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
                GenreStatsScreen(
                    onBackClick = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(Route.Settings.path) },
                    archiveViewModel = archiveViewModel
                )
            }
            composable(Route.Settings.path) {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        // 플로팅 하단바 — 콘텐츠 위에 오버레이
        val showBottomBar = currentRoute in setOf(
            Route.NowPlaying.path,
            Route.Archive.path,
            Route.GenreStats.path,
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
        color = Color.White.copy(alpha = 0.94f),
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, Color(0xFFE4E7F0).copy(alpha = 0.7f))
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
                                .background(Color(0xFFDCEBFF))
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
                                tint = Color(0xFF9CA3AF)
                            )
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF9CA3AF)
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
