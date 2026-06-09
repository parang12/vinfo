package com.example.vinfo.ui.stats

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinfo.ui.archive.DummyArchive
import com.example.vinfo.ui.component.FloatingBackButton
import com.example.vinfo.ui.component.FloatingSettingsButton
import com.example.vinfo.ui.component.GenreChip
import com.example.vinfo.ui.component.VinfoCard
import com.example.vinfo.ui.theme.VinfoTheme
import com.example.vinfo.domain.model.ConfirmedGenreDiscovery
import com.example.vinfo.domain.model.GenreFlowNodeState
import com.example.vinfo.domain.model.GenreRelationCandidate
import com.example.vinfo.domain.model.RelationStrength
import com.example.vinfo.domain.model.toGenreKey
import com.example.vinfo.domain.usecase.GetVisibleGenreFlowUseCase
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal enum class GenreMapNodeType {
    Activated,
    Adjacent,
    Locked
}

internal enum class TasteFlowSheetState {
    Expanded,
    Peek,
    Hidden
}

internal fun nextTasteFlowSheetStateAfterDrag(
    current: TasteFlowSheetState,
    dragDeltaPx: Float,
    thresholdPx: Float
): TasteFlowSheetState {
    if (dragDeltaPx > thresholdPx) {
        return when (current) {
            TasteFlowSheetState.Expanded -> TasteFlowSheetState.Peek
            TasteFlowSheetState.Peek -> TasteFlowSheetState.Hidden
            TasteFlowSheetState.Hidden -> TasteFlowSheetState.Hidden
        }
    }

    if (dragDeltaPx < -thresholdPx) {
        return when (current) {
            TasteFlowSheetState.Expanded -> TasteFlowSheetState.Expanded
            TasteFlowSheetState.Peek -> TasteFlowSheetState.Expanded
            TasteFlowSheetState.Hidden -> TasteFlowSheetState.Peek
        }
    }

    return current
}

internal fun nextTasteFlowSheetStateOnHandleTap(current: TasteFlowSheetState): TasteFlowSheetState {
    return when (current) {
        TasteFlowSheetState.Expanded -> TasteFlowSheetState.Hidden
        TasteFlowSheetState.Peek,
        TasteFlowSheetState.Hidden -> TasteFlowSheetState.Expanded
    }
}

private fun TasteFlowSheetState.visibleHeight(): Dp {
    return when (this) {
        TasteFlowSheetState.Expanded -> 318.dp
        TasteFlowSheetState.Peek -> 104.dp
        TasteFlowSheetState.Hidden -> 32.dp
    }
}

internal data class GenreMapNodeUi(
    val id: String,
    val genreKey: String,
    val label: String,
    val note: String,
    val saveCount: Int,
    val lastActivatedText: String,
    val type: GenreMapNodeType,
    val position: Offset,
    val accessibilityLabel: String,
)

internal data class GenreMapEdgeUi(
    val fromId: String,
    val toId: String,
    val label: String,
    val evidence: String,
    val unlocked: Boolean,
    val relationScore: Float = 1f,
)

internal data class GenreMapUiState(
    val headline: String,
    val subtitle: String,
    val flowSummary: String,
    val activeGenreCount: Int,
    val candidateGenreCount: Int,
    val recentFlowCount: Int,
    val unlockBanner: String?,
    val unlockBannerDetail: String?,
    val nodes: List<GenreMapNodeUi>,
    val edges: List<GenreMapEdgeUi>,
    val recentAlbums: List<DummyArchive>,
) {
    companion object {
        fun empty() = GenreMapUiState(
            headline = "Taste Flow",
            subtitle = "저장한 앨범이 만든 장르 흐름",
            flowSummary = "첫 저장 후 탐험이 시작됩니다",
            activeGenreCount = 0,
            candidateGenreCount = 0,
            recentFlowCount = 0,
            unlockBanner = null,
            unlockBannerDetail = null,
            nodes = emptyList(),
            edges = emptyList(),
            recentAlbums = emptyList()
        )

        fun sample() = fromArchive(
            listOf(
                DummyArchive("1", "Modal Soul", "Nujabes", listOf("Jazz Rap"), "2026.05.22"),
                DummyArchive("2", "To Pimp a Butterfly", "Kendrick Lamar", listOf("Hip-Hop", "Jazz Rap"), "2026.05.20"),
                DummyArchive("3", "Voodoo", "D'Angelo", listOf("Neo Soul", "R&B"), "2026.05.18"),
                DummyArchive("4", "What's Going On", "Marvin Gaye", listOf("Soul"), "2026.05.15"),
                DummyArchive("5", "In a Silent Way", "Miles Davis", listOf("Jazz"), "2026.05.10")
            )
        )

        fun fromArchive(archiveItems: List<DummyArchive>): GenreMapUiState {
            if (archiveItems.isEmpty()) return empty()

            val visibleFlow = GetVisibleGenreFlowUseCase()(
                albumGenres = archiveItems.map { it.genres }
            )
            if (visibleFlow.nodes.isEmpty()) return empty()

            val nodes = visibleFlow.nodes.map { node ->
                val type = when {
                    node.state == GenreFlowNodeState.ACTIVATED -> GenreMapNodeType.Activated
                    else -> GenreMapNodeType.Adjacent
                }
                GenreMapNodeUi(
                    id = node.key,
                    genreKey = node.displayName.uppercase().replace(" ", "_").replace("-", "_"),
                    label = node.displayName,
                    note = when (type) {
                        GenreMapNodeType.Activated -> "저장 앨범 ${node.saveCount}개"
                        GenreMapNodeType.Adjacent -> "연결 후보"
                        GenreMapNodeType.Locked -> ""
                    },
                    saveCount = node.saveCount,
                    lastActivatedText = if (node.saveCount > 0) "자동 반영" else "대기 중",
                    type = type,
                    position = Offset(node.x, node.y),
                    accessibilityLabel = "${node.displayName}, ${type.koreanLabel()}, 저장 앨범 ${node.saveCount}개"
                )
            }

            val edges = visibleFlow.edges.map { edge ->
                val label = when {
                    edge.active -> "최근 열린 흐름"
                    else -> "연결 후보"
                }
                GenreMapEdgeUi(
                    fromId = edge.sourceKey,
                    toId = edge.targetKey,
                    label = label,
                    evidence = if (edge.active) {
                        "보관함에 두 장르의 앨범이 함께 저장되어 연결이 활성화되었습니다."
                    } else {
                        edge.evidence
                    },
                    unlocked = true,
                    relationScore = edge.score
                )
            }

            val activeCount = nodes.count { it.type == GenreMapNodeType.Activated }
            val candidateCount = nodes.count { it.type == GenreMapNodeType.Adjacent }
            val recentCount = visibleFlow.edges.count { it.active }
            val topFlow = nodes
                .filter { it.type == GenreMapNodeType.Activated }
                .map { it.label }
                .take(3)
                .joinToString(" -> ")
                .ifBlank {
                "저장 앨범 기반으로 자동 구성 중"
            }

            return GenreMapUiState(
                headline = "Taste Flow",
                subtitle = "저장한 앨범이 만든 장르 흐름",
                flowSummary = topFlow,
                activeGenreCount = activeCount,
                candidateGenreCount = candidateCount,
                recentFlowCount = recentCount,
                unlockBanner = "지도 데이터 반영 완료",
                unlockBannerDetail = "보관함의 앨범 장르를 기준으로 활성 장르와 연결 후보를 계산했습니다.",
                nodes = nodes,
                edges = edges,
                recentAlbums = archiveItems.take(5)
            )
        }
    }
}

private fun String.toNodeId(): String = toGenreKey()

internal fun GenreMapUiState.withDiscoveries(
    discoveries: List<ConfirmedGenreDiscovery>
): GenreMapUiState {
    if (discoveries.isEmpty()) return this

    val updatedNodes = nodes.toMutableList()
    val updatedEdges = edges.toMutableList()

    discoveries.forEach { discovery ->
        val sourceNode = updatedNodes.firstOrNull {
            it.label.toGenreKey() == discovery.sourceGenre.toGenreKey()
        } ?: return@forEach

        discovery.candidates.forEachIndexed { index, candidate ->
            val candidateId = candidate.genreName.toNodeId()
            val existingNodeIndex = updatedNodes.indexOfFirst { it.id == candidateId }
            if (existingNodeIndex < 0) {
                val candidatePosition = findOpenGenreNodePosition(
                    source = sourceNode.position,
                    candidateIndex = index,
                    candidateCount = discovery.candidates.size,
                    occupiedPositions = updatedNodes.map { it.position }
                )
                updatedNodes += GenreMapNodeUi(
                    id = candidateId,
                    genreKey = candidate.genreName.uppercase().replace(" ", "_").replace("-", "_"),
                    label = candidate.genreName,
                    note = "탐색한 연결 후보",
                    saveCount = 0,
                    lastActivatedText = "검색으로 발견",
                    type = GenreMapNodeType.Adjacent,
                    position = candidatePosition,
                    accessibilityLabel = "${candidate.genreName}, 연결 후보, 연관성 ${candidate.strength.koreanLabel}"
                )
            } else {
                val existingNode = updatedNodes[existingNodeIndex]
                if (existingNode.type == GenreMapNodeType.Adjacent) {
                    updatedNodes[existingNodeIndex] = existingNode.copy(
                        note = "탐색한 연결 후보",
                        lastActivatedText = "검색으로 확인",
                        accessibilityLabel = "${existingNode.label}, 연결 후보, 연관성 ${candidate.strength.koreanLabel}"
                    )
                }
            }

            val existingIndex = updatedEdges.indexOfFirst {
                setOf(it.fromId, it.toId) == setOf(sourceNode.id, candidateId)
            }
            val discoveredEdge = GenreMapEdgeUi(
                fromId = sourceNode.id,
                toId = candidateId,
                label = "연관성 ${candidate.strength.koreanLabel}",
                evidence = candidate.evidence.ifBlank { "Gemini 검색으로 확인한 장르 관계입니다." },
                unlocked = true,
                relationScore = candidate.score
            )
            if (existingIndex >= 0) {
                val existingEdge = updatedEdges[existingIndex]
                if (existingEdge.label == "연결 후보" || existingEdge.relationScore < candidate.score) {
                    updatedEdges[existingIndex] = discoveredEdge
                }
            } else {
                updatedEdges += discoveredEdge
            }
        }
    }

    return copy(
        candidateGenreCount = updatedNodes.count { it.type == GenreMapNodeType.Adjacent },
        nodes = updatedNodes,
        edges = updatedEdges
    )
}

private fun findOpenGenreNodePosition(
    source: Offset,
    candidateIndex: Int,
    candidateCount: Int,
    occupiedPositions: List<Offset>
): Offset {
    val slots = candidateCount.coerceAtLeast(7)
    val startAngle = -PI / 2.0
    val radii = listOf(0.23f, 0.31f, 0.39f, 0.47f)
    val minDistance = 0.17f

    radii.forEachIndexed { radiusIndex, radius ->
        repeat(slots) { slotOffset ->
            val slot = candidateIndex + slotOffset + (radiusIndex * 2)
            val angle = startAngle + (2.0 * PI * slot / slots)
            val position = Offset(
                x = (source.x + cos(angle).toFloat() * radius).coerceIn(0.06f, 0.94f),
                y = (source.y + sin(angle).toFloat() * radius).coerceIn(0.08f, 0.94f)
            )
            if (occupiedPositions.none { it.distanceTo(position) < minDistance }) {
                return position
            }
        }
    }

    val fallbackRadius = 0.28f + (candidateIndex % 4) * 0.08f
    val fallbackAngle = startAngle + (candidateIndex * 2.399963229728653)
    return Offset(
        x = (source.x + cos(fallbackAngle).toFloat() * fallbackRadius).coerceIn(0.06f, 0.94f),
        y = (source.y + sin(fallbackAngle).toFloat() * fallbackRadius).coerceIn(0.08f, 0.94f)
    )
}

private fun Offset.distanceTo(other: Offset): Float {
    val dx = x - other.x
    val dy = y - other.y
    return sqrt(dx * dx + dy * dy)
}

private fun GenreMapNodeType.koreanLabel(): String = when (this) {
    GenreMapNodeType.Activated -> "활성 장르"
    GenreMapNodeType.Adjacent -> "연결 후보"
    GenreMapNodeType.Locked -> "미탐험"
}

internal fun relatedAlbumsForNode(
    selectedNode: GenreMapNodeUi?,
    albums: List<DummyArchive>
): List<DummyArchive> {
    val selectedGenreKey = selectedNode?.label?.toGenreKey() ?: return emptyList()
    return albums.filter { album ->
        album.genres.any { genre -> genre.toGenreKey() == selectedGenreKey }
    }
}

@Composable
internal fun GenreMapScreen(
    modifier: Modifier = Modifier,
    archiveItems: List<DummyArchive> = emptyList(),
    uiState: GenreMapUiState? = null,
    discoveryState: GenreMapDiscoveryState = GenreMapDiscoveryState(),
    onFindNearbyGenres: (String) -> Unit = {},
    onDismissDiscoveryPopup: () -> Unit = {},
    onConfirmDiscoveryCandidates: (List<GenreRelationCandidate>) -> Unit = {},
    onConfirmPendingReview: (String) -> Unit = {},
    onGenreClick: (String) -> Unit = {},
    onEdgeClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val mapState = remember(archiveItems, uiState, discoveryState.confirmedDiscoveries) {
        (uiState ?: GenreMapUiState.fromArchive(archiveItems))
            .withDiscoveries(discoveryState.confirmedDiscoveries)
    }
    var selectedNodeId by rememberSaveable(mapState.nodes) { mutableStateOf(mapState.nodes.firstOrNull()?.id) }
    var selectedNode by remember(mapState.nodes) { mutableStateOf(mapState.nodes.firstOrNull()) }
    var selectedEdge by remember { mutableStateOf<GenreMapEdgeUi?>(null) }
    var bottomSheetState by rememberSaveable { mutableStateOf(TasteFlowSheetState.Expanded) }
    var isReviewQueueVisible by rememberSaveable { mutableStateOf(false) }
    var scale by rememberSaveable { mutableStateOf(0.92f) }
    var panX by rememberSaveable { mutableStateOf(0f) }
    var panY by rememberSaveable { mutableStateOf(12f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        FullFlowMapCanvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(mapState.nodes) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.58f, 2.35f)
                        panX += pan.x
                        panY += pan.y
                    }
                },
            nodes = mapState.nodes,
            edges = mapState.edges,
            selectedNodeId = selectedNodeId,
            scale = scale,
            pan = Offset(panX, panY),
            onNodeSelected = { node ->
                selectedNodeId = node.id
                selectedNode = node
                bottomSheetState = TasteFlowSheetState.Expanded
                onGenreClick(node.genreKey)
            },
            onEdgeSelected = { edge ->
                selectedEdge = edge
                onEdgeClick(edge.label)
            }
        )

        MapTopPanel(
            mapState = mapState,
            pendingReviewCount = discoveryState.pendingReviewCount,
            onReviewQueueClick = { isReviewQueueVisible = true },
            onBackClick = onBackClick,
            onSettingsClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        )

        MapFloatingControls(
            scale = scale,
            onZoomIn = { scale = (scale * 1.18f).coerceAtMost(2.35f) },
            onZoomOut = { scale = (scale / 1.18f).coerceAtLeast(0.58f) },
            onRecenter = {
                scale = 0.92f
                panX = 0f
                panY = 12f
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp)
        )

        TasteFlowBottomSheet(
            selectedNode = selectedNode,
            relatedAlbums = relatedAlbumsForNode(selectedNode, mapState.recentAlbums),
            isDiscoveryLoading = discoveryState.isLoading,
            sheetState = bottomSheetState,
            onSheetStateChange = { bottomSheetState = it },
            onFindNearbyGenres = onFindNearbyGenres,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    selectedEdge?.let { edge ->
        AlertDialog(
            onDismissRequest = { selectedEdge = null },
            title = {
                Text(
                    text = edge.label,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = edge.evidence,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "연관성 ${RelationStrength.fromScore(edge.relationScore).koreanLabel}. 노드를 탭해 주변 흐름을 탐색할 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEdge = null }) {
                    Text("닫기")
                }
            }
        )
    }

    if (discoveryState.isPopupVisible) {
        NearbyGenreDiscoveryDialog(
            state = discoveryState,
            onDismiss = onDismissDiscoveryPopup,
            onConfirm = onConfirmDiscoveryCandidates
        )
    }

    if (isReviewQueueVisible) {
        GenreRelationReviewQueueDialog(
            pendingReviews = discoveryState.pendingReviews,
            onDismiss = { isReviewQueueVisible = false },
            onConfirm = { sourceGenre ->
                onConfirmPendingReview(sourceGenre)
                isReviewQueueVisible = false
            }
        )
    }
}

@Composable
private fun MapTopPanel(
    mapState: GenreMapUiState,
    pendingReviewCount: Int,
    onReviewQueueClick: () -> Unit,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingBackButton(onClick = onBackClick)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0058BC))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = mapState.headline,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0058BC)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mapState.subtitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                FloatingSettingsButton(onClick = onSettingsClick)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapMetric(label = "활성 장르", value = "${mapState.activeGenreCount}개", modifier = Modifier.weight(1f))
                MapMetric(label = "연결 후보", value = "${mapState.candidateGenreCount}개", modifier = Modifier.weight(1f))
                MapMetric(
                    label = "검수 대기",
                    value = "${pendingReviewCount}개",
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = pendingReviewCount > 0, onClick = onReviewQueueClick)
                        .padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun MapFloatingControls(
    scale: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${(scale * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "+",
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onZoomIn)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0058BC)
            )
            Text(
                text = "-",
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onZoomOut)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0058BC)
            )
            Text(
                text = "중앙",
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable(onClick = onRecenter)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0058BC)
            )
        }
    }
}

@Composable
private fun TasteFlowBottomSheet(
    selectedNode: GenreMapNodeUi?,
    relatedAlbums: List<DummyArchive>,
    isDiscoveryLoading: Boolean,
    sheetState: TasteFlowSheetState,
    onSheetStateChange: (TasteFlowSheetState) -> Unit,
    onFindNearbyGenres: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expandedHeight = 318.dp
    val visibleHeight = sheetState.visibleHeight()
    val sheetOffsetY by animateDpAsState(
        targetValue = expandedHeight - visibleHeight,
        label = "tasteFlowBottomSheetOffset"
    )
    val dragThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }
    var dragDeltaPx by remember { mutableStateOf(0f) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = sheetOffsetY)
            .pointerInput(sheetState) {
                detectVerticalDragGestures(
                    onDragStart = { dragDeltaPx = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        dragDeltaPx += dragAmount
                    },
                    onDragEnd = {
                        onSheetStateChange(
                            nextTasteFlowSheetStateAfterDrag(
                                current = sheetState,
                                dragDeltaPx = dragDeltaPx,
                                thresholdPx = dragThresholdPx
                            )
                        )
                    },
                    onDragCancel = { dragDeltaPx = 0f }
                )
            }
            .padding(horizontal = 0.dp),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .height(expandedHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
                    .height(24.dp)
                    .clickable {
                        onSheetStateChange(nextTasteFlowSheetStateOnHandleTap(sheetState))
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedNode?.label ?: "분석할 장르를 지도에서 터치하세요",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                    text = selectedNode?.note
                        ?: "저장한 앨범을 바탕으로 장르가 어떻게 이어지는지 확인할 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                selectedNode?.let {
                    GenreChip(genre = it.type.koreanLabel())
                }
            }

            selectedNode?.let { node ->
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onFindNearbyGenres(node.label) },
                    enabled = !isDiscoveryLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0058BC))
                ) {
                    Text(
                        text = if (isDiscoveryLoading) "찾는 중..." else "근처 장르 찾기",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedNode?.let { "${it.label} 저장 앨범" } ?: "저장 앨범",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "자동 반영됨",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0058BC)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (relatedAlbums.isEmpty()) {
                Text(
                    text = selectedNode?.let { "이 장르로 저장된 앨범이 아직 없습니다." }
                        ?: "지도에서 장르를 선택하면 해당 장르의 저장 앨범을 보여줍니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                relatedAlbums.forEach { album ->
                    RecentAlbumRow(album = album)
                }
            }
        }
    }
}

@Composable
private fun NearbyGenreDiscoveryDialog(
    state: GenreMapDiscoveryState,
    onDismiss: () -> Unit,
    onConfirm: (List<GenreRelationCandidate>) -> Unit,
) {
    var selectedCandidateKeys by remember(state.selectedGenre, state.candidates) {
        mutableStateOf(state.candidates.map { it.genreName.toGenreKey() }.toSet())
    }
    val selectedCandidates = remember(state.candidates, selectedCandidateKeys) {
        state.candidates.filter { it.genreName.toGenreKey() in selectedCandidateKeys }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${state.selectedGenre.orEmpty()} 주변 장르",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "추가할 장르만 선택해서 지도에 반영할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFBA1A1A)
                    )
                }
                if (state.errorMessage == null && state.candidates.isEmpty()) {
                    Text(
                        text = "확인 가능한 주변 장르를 찾지 못했습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.candidates.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "장르",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "연관성",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    state.candidates.forEach { candidate ->
                        val candidateKey = candidate.genreName.toGenreKey()
                        val isSelected = candidateKey in selectedCandidateKeys
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedCandidateKeys = if (isSelected) {
                                        selectedCandidateKeys - candidateKey
                                    } else {
                                        selectedCandidateKeys + candidateKey
                                    }
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedCandidateKeys = if (checked) {
                                        selectedCandidateKeys + candidateKey
                                    } else {
                                        selectedCandidateKeys - candidateKey
                                    }
                                }
                            )
                            Text(
                                text = candidate.genreName,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = candidate.strength.koreanLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (candidate.strength) {
                                    RelationStrength.STRONG -> Color(0xFF0058BC)
                                    RelationStrength.MEDIUM -> Color(0xFF3B82F6)
                                    RelationStrength.WEAK -> Color(0xFF64748B)
                                }
                            )
                        }
                    }
                    Text(
                        text = "선택한 장르 ${selectedCandidates.size}개",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedCandidates) },
                enabled = selectedCandidates.isNotEmpty()
            ) {
                Text("선택 반영")
            }
        }
    )
}

@Composable
private fun GenreRelationReviewQueueDialog(
    pendingReviews: List<GenreRelationReviewItem>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "장르 관계 검수 큐",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (pendingReviews.isEmpty()) {
                    Text(
                        text = "검수할 장르 관계가 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    pendingReviews.forEach { review ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = review.sourceGenre,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${review.candidates.size}개 후보",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0058BC)
                                    )
                                }
                                review.candidates.take(3).forEach { candidate ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = candidate.genreName,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = candidate.strength.koreanLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                TextButton(
                                    modifier = Modifier.align(Alignment.End),
                                    onClick = { onConfirm(review.sourceGenre) }
                                ) {
                                    Text("지도에 확정")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}

@Composable
private fun FullFlowMapCanvas(
    nodes: List<GenreMapNodeUi>,
    edges: List<GenreMapEdgeUi>,
    selectedNodeId: String?,
    scale: Float,
    pan: Offset,
    onNodeSelected: (GenreMapNodeUi) -> Unit,
    onEdgeSelected: (GenreMapEdgeUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.background(MaterialTheme.colorScheme.background)
    ) {
        val canvasBackground = MaterialTheme.colorScheme.background
        val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val center = Offset(widthPx / 2f, heightPx / 2f)
        val nodeDiameter = 48.dp
        val nodeDiameterPx = with(density) { nodeDiameter.toPx() }
        val mapWidth = 980f
        val mapHeight = 640f

        fun worldPosition(node: GenreMapNodeUi): Offset {
            return Offset(
                x = (node.position.x - 0.5f) * mapWidth,
                y = (node.position.y - 0.5f) * mapHeight
            )
        }

        fun screenPosition(node: GenreMapNodeUi): Offset {
            val world = worldPosition(node)
            return center + pan + (world * scale)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(canvasBackground)

            val gridStep = 72f * scale
            val gridOffsetX = (pan.x % gridStep)
            val gridOffsetY = (pan.y % gridStep)
            var x = gridOffsetX - gridStep
            while (x < size.width + gridStep) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += gridStep
            }
            var y = gridOffsetY - gridStep
            while (y < size.height + gridStep) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += gridStep
            }

            val lookup = nodes.associateBy { it.id }
            edges.forEach { edge ->
                val from = lookup[edge.fromId] ?: return@forEach
                val to = lookup[edge.toId] ?: return@forEach
                val start = screenPosition(from)
                val end = screenPosition(to)
                val isActive = from.type == GenreMapNodeType.Activated && to.type == GenreMapNodeType.Activated
                val isCandidate = !isActive
                val lineColor = when {
                    isActive -> Color(0xFF0058BC)
                    isCandidate -> Color(0xFF60A5FA)
                    else -> Color(0xFFCBD5E1)
                }
                drawLine(
                    color = lineColor.copy(alpha = 0.35f + edge.relationScore.coerceIn(0f, 1f) * 0.55f),
                    start = start,
                    end = end,
                    strokeWidth = 1.8f + edge.relationScore.coerceIn(0f, 1f) * 4.2f,
                    cap = StrokeCap.Round
                )
            }
        }

        MapLaneLabel(
            text = "원천 장르",
            modifier = Modifier.offset {
                IntOffset(
                    x = (center.x + pan.x - 390f * scale).roundToInt(),
                    y = (center.y + pan.y - 270f * scale).roundToInt()
                )
            }
        )
        MapLaneLabel(
            text = "중심 흐름",
            modifier = Modifier.offset {
                IntOffset(
                    x = (center.x + pan.x - 65f * scale).roundToInt(),
                    y = (center.y + pan.y - 270f * scale).roundToInt()
                )
            }
        )
        MapLaneLabel(
            text = "파생 흐름",
            modifier = Modifier.offset {
                IntOffset(
                    x = (center.x + pan.x + 255f * scale).roundToInt(),
                    y = (center.y + pan.y - 270f * scale).roundToInt()
                )
            }
        )

        nodes.forEach { node ->
            val position = screenPosition(node)
            FlowNodeBubble(
                node = node,
                selected = selectedNodeId == node.id,
                modifier = Modifier.offset {
                    IntOffset(
                        x = (position.x - nodeDiameterPx / 2f).roundToInt(),
                        y = (position.y - nodeDiameterPx / 2f).roundToInt()
                    )
                },
                onClick = { onNodeSelected(node) }
            )
        }
    }
}

@Composable
private fun MapLaneLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFF94A3B8)
    )
}

@Composable
private fun FlowNodeBubble(
    node: GenreMapNodeUi,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        label = "fullFlowNodeScale"
    )
    Column(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onClick)
                .semantics {
                    role = Role.Button
                    contentDescription = node.accessibilityLabel
                },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = if (selected) 8.dp else 3.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = when (node.type) {
                    GenreMapNodeType.Activated -> Color(0xFF0058BC)
                    GenreMapNodeType.Adjacent -> Color(0xFF9CC3FF)
                    GenreMapNodeType.Locked -> Color(0xFFCBD5E1)
                }
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (node.type == GenreMapNodeType.Locked) "?" else node.label.initials(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (node.type == GenreMapNodeType.Activated) {
                        Color(0xFF0058BC)
                    } else {
                        Color(0xFF64748B)
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = node.label,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (node.type == GenreMapNodeType.Activated) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun String.initials(): String {
    return split(" ", "-")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { take(2).uppercase() }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MapCanvas(
    nodes: List<GenreMapNodeUi>,
    edges: List<GenreMapEdgeUi>,
    selectedNodeId: String?,
    selectedEdge: GenreMapEdgeUi?,
    onNodeSelected: (GenreMapNodeUi) -> Unit,
    onEdgeSelected: (GenreMapEdgeUi) -> Unit,
) {
    val density = LocalDensity.current
    val nodeDiameter = 74.dp
    val edgeChipWidth = 102.dp
    val edgeChipHeight = 30.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(430.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFFF4F7FF))
            .border(1.dp, Color(0xFFC1C6D7).copy(alpha = 0.30f), RoundedCornerShape(28.dp))
            .padding(16.dp)
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val nodeSizePx = with(density) { nodeDiameter.toPx() }
        val edgeChipWidthPx = with(density) { edgeChipWidth.toPx() }
        val edgeChipHeightPx = with(density) { edgeChipHeight.toPx() }

        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val lookup = nodes.associateBy { it.id }
            edges.forEach { edge ->
                val from = lookup[edge.fromId] ?: return@forEach
                val to = lookup[edge.toId] ?: return@forEach
                val start = Offset(from.position.x * widthPx, from.position.y * heightPx)
                val end = Offset(to.position.x * widthPx, to.position.y * heightPx)
                val lineColor = if (edge.unlocked) Color(0xFF0058BC) else Color(0xFF9CA3AF)
                drawLine(
                    color = lineColor.copy(alpha = if (edge == selectedEdge) 0.95f else if (edge.unlocked) 0.72f else 0.40f),
                    start = start,
                    end = end,
                    strokeWidth = if (edge.unlocked) 7f else 5f,
                    cap = StrokeCap.Round,
                    pathEffect = if (edge.unlocked) null else PathEffect.dashPathEffect(floatArrayOf(18f, 14f), 0f)
                )
                drawCircle(
                    color = lineColor.copy(alpha = if (edge.unlocked) 0.18f else 0.12f),
                    radius = if (edge.unlocked) 16f else 12f,
                    center = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
                )
            }
        }

        edges.forEach { edge ->
            val from = nodes.firstOrNull { it.id == edge.fromId } ?: return@forEach
            val to = nodes.firstOrNull { it.id == edge.toId } ?: return@forEach
            val midpoint = Offset(
                x = (from.position.x + to.position.x) / 2f * widthPx,
                y = (from.position.y + to.position.y) / 2f * heightPx
            )
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (midpoint.x - edgeChipWidthPx / 2f).roundToInt(),
                            y = (midpoint.y - edgeChipHeightPx / 2f).roundToInt()
                        )
                    }
                    .size(edgeChipWidth, edgeChipHeight)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (edge.unlocked) Color(0xFFDCEBFF) else Color(0xFFE8EAF6))
                    .border(
                        width = 1.dp,
                        color = if (edge.unlocked) Color(0xFF0058BC) else Color(0xFFC1C6D7),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .combinedClickable(
                        onClick = { onEdgeSelected(edge) },
                        onLongClick = { onEdgeSelected(edge) },
                        role = Role.Button,
                        onClickLabel = "연결 근거 확인"
                    )
                    .semantics {
                        contentDescription = "${edge.label}, ${edge.evidence}"
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = if (edge.unlocked) Color(0xFF0058BC) else Color(0xFF6B7280)
                    )
                    Text(
                        text = edge.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (edge.unlocked) Color(0xFF0058BC) else Color(0xFF4B5563)
                    )
                }
            }
        }

        nodes.forEach { node ->
            val isSelected = selectedNodeId == node.id
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.08f else 1f,
                label = "genreNodeScale"
            )
            val x = node.position.x * widthPx - nodeSizePx / 2f
            val y = node.position.y * heightPx - nodeSizePx / 2f
            val backgroundColor = when (node.type) {
                GenreMapNodeType.Activated -> Color(0xFF0058BC)
                GenreMapNodeType.Adjacent -> Color.White
                GenreMapNodeType.Locked -> Color(0xFFF1F3FE)
            }
            val borderColor = when (node.type) {
                GenreMapNodeType.Activated -> Color(0xFF0058BC)
                GenreMapNodeType.Adjacent -> Color(0xFF9CC3FF)
                GenreMapNodeType.Locked -> Color(0xFFC1C6D7)
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .size(nodeDiameter)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = borderColor,
                        shape = CircleShape
                    )
                    .combinedClickable(
                        onClick = { onNodeSelected(node) },
                        onLongClick = { onNodeSelected(node) },
                        role = Role.Button,
                        onClickLabel = "${node.label} 열기",
                        onLongClickLabel = "${node.label} 세부 정보"
                    )
                    .semantics {
                        contentDescription = node.accessibilityLabel
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = when (node.type) {
                            GenreMapNodeType.Activated -> Icons.Default.CheckCircle
                            GenreMapNodeType.Adjacent -> Icons.Default.Link
                            GenreMapNodeType.Locked -> Icons.Default.Lock
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = when (node.type) {
                            GenreMapNodeType.Activated -> Color.White
                            GenreMapNodeType.Adjacent -> Color(0xFF0058BC)
                            GenreMapNodeType.Locked -> Color(0xFF6B7280)
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = node.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (node.type) {
                            GenreMapNodeType.Activated -> Color.White
                            GenreMapNodeType.Adjacent -> Color(0xFF181C23)
                            GenreMapNodeType.Locked -> Color(0xFF414755)
                        }
                    )
                }
            }

            val badgeY = y + nodeSizePx + with(density) { 8.dp.toPx() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (node.position.x * widthPx - 32).roundToInt(),
                            y = badgeY.roundToInt()
                        )
                    }
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        when (node.type) {
                            GenreMapNodeType.Activated -> Color(0xFFDCEBFF)
                            GenreMapNodeType.Adjacent -> Color(0xFFEEF4FF)
                            GenreMapNodeType.Locked -> Color(0xFFE8EAF6)
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = node.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (node.type) {
                        GenreMapNodeType.Activated -> Color(0xFF0058BC)
                        GenreMapNodeType.Adjacent -> Color(0xFF1D4ED8)
                        GenreMapNodeType.Locked -> Color(0xFF6B7280)
                    }
                )
            }
        }
    }
}

@Composable
private fun LegendRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GenreChip(genre = "활성", isPrimary = true)
        GenreChip(genre = "인접", isSelected = true)
        GenreChip(genre = "잠금")
    }
}

@Composable
private fun MapMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RecentAlbumRow(album: DummyArchive) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${album.artist} · ${album.date}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        album.genres.firstOrNull { it.isNotBlank() }?.let { genre ->
            GenreChip(genre = genre)
        }
    }
}

@Composable
private fun NodeFacts(
    saveCount: Int,
    lastActivatedText: String,
    typeLabel: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MiniFactChip(label = "저장", value = "${saveCount}회")
        MiniFactChip(label = "최근 진입", value = lastActivatedText)
        MiniFactChip(label = "상태", value = typeLabel)
    }
}

@Composable
private fun MiniFactChip(
    label: String,
    value: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GenreMapScreenPreview() {
    VinfoTheme {
        GenreMapScreen()
    }
}
