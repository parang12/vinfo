# Genre Map Nearby Discovery Popup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 사용자가 Taste Map에서 장르 노드를 선택하고 `근처 장르 찾기`를 누르면 Gemini 검색 기반 연관 장르를 팝업 목록으로 확인한 뒤, 선택한 결과를 화살표 없는 가중치 연결선과 주변 노드로 지도에 추가한다.

**Architecture:** 기존 앨범 저장 흐름과 `GenreMapUiState.fromArchive()`는 중심 활성 노드를 만드는 경로로 유지한다. 새 `GenreRelationDiscoveryRepository`는 선택 장르 하나를 Gemini Search grounding으로 조회하고, 순수 도메인 유스케이스는 응답을 검증하여 최대 6개의 주변 후보로 제한한다. `GenreMapViewModel`은 팝업 상태와 사용자가 지도에 반영한 세션 탐색 결과를 관리하고, Compose 화면은 목록 팝업과 가중치 기반 선 렌더링만 담당한다.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android ViewModel, Kotlin Coroutines StateFlow, Retrofit Scalars, OkHttp, org.json, JUnit4, MockWebServer

---

## Scope

이번 계획은 "사용자가 직접 선택한 장르의 주변을 한 단계 탐색"하는 MVP에 집중한다.

- 기존 앨범 분석 결과의 대표 장르가 지도 중심 노드가 된다.
- 장르 노드를 탭하면 하단 패널에 `근처 장르 찾기` 버튼이 보인다.
- 버튼을 누르면 Gemini가 선택 장르의 영향 관계와 강도를 검색한다.
- 팝업 목록은 왼쪽에 장르명, 오른쪽에 `강함 / 보통 / 약함`을 표시한다.
- 팝업의 `지도에 반영` 버튼을 누르면 검증된 후보 전체가 주변 노드로 추가된다.
- 선은 화살표 없이 그린다. 관계 강도는 선 굵기와 투명도로 표현한다.
- `Unknown`, 빈 장르명, 선택 장르 자기 자신, 중복 후보, 강도 `0.35` 미만 후보는 표시하지 않는다.
- 한 번의 탐색에서 최대 6개만 노출한다.
- 이번 MVP의 발견 결과는 `GenreMapViewModel` 세션 동안 유지한다. Room 영속화와 앨범 응답의 `primary_genres` 배열 전환은 다음 계획으로 분리한다.

## File Structure

### Create

- `app/src/main/java/com/example/vinfo/domain/model/GenreRelationDiscovery.kt`
  - 연관 장르 후보, 강도 단계, 검색 결과 상태를 정의한다.
- `app/src/main/java/com/example/vinfo/domain/repository/GenreRelationDiscoveryRepository.kt`
  - 선택 장르 하나의 주변 후보를 조회하는 인터페이스다.
- `app/src/main/java/com/example/vinfo/domain/usecase/DiscoverNearbyGenresUseCase.kt`
  - 후보 검증, 중복 제거, 임계값 필터, 정렬, 최대 개수 제한을 담당한다.
- `app/src/main/java/com/example/vinfo/data/remote/gemini/GeminiGenreRelationRequestBuilder.kt`
  - Search grounding을 포함한 장르 관계 전용 Gemini 요청을 만든다.
- `app/src/main/java/com/example/vinfo/data/remote/gemini/GeminiGenreRelationJsonParser.kt`
  - Gemini 래퍼 또는 원시 JSON에서 `nearby_genres` 배열을 파싱한다.
- `app/src/main/java/com/example/vinfo/data/remote/gemini/GeminiGenreRelationDiscoveryRepository.kt`
  - Gemini 호출, 파싱, 오류 변환을 담당한다.
- `app/src/main/java/com/example/vinfo/ui/stats/GenreMapViewModel.kt`
  - 팝업 상태와 세션 탐색 결과를 관리한다.
- `app/src/test/kotlin/com/example/vinfo/domain/usecase/DiscoverNearbyGenresUseCaseTest.kt`
- `app/src/test/kotlin/com/example/vinfo/data/remote/gemini/GeminiGenreRelationRequestBuilderTest.kt`
- `app/src/test/kotlin/com/example/vinfo/data/remote/gemini/GeminiGenreRelationJsonParserTest.kt`
- `app/src/test/kotlin/com/example/vinfo/ui/stats/GenreMapDiscoveryReducerTest.kt`

### Modify

- `app/src/main/java/com/example/vinfo/ui/stats/GenreMapScreen.kt`
  - 선택 노드 하단 버튼, 결과 팝업, 세션 발견 노드 병합, 강도별 선 렌더링을 추가한다.
- `docs/SRS.md`
- `docs/SDD.md`
- `docs/DATA.md`
- `docs/UI.md`
- `docs/REMAINING_TASKS.md`
  - 자동 생성이 아닌 사용자 주도 탐색 MVP로 문서를 맞춘다.

---

### Task 1: Define And Filter Nearby Genre Candidates

**Files:**
- Create: `app/src/main/java/com/example/vinfo/domain/model/GenreRelationDiscovery.kt`
- Create: `app/src/main/java/com/example/vinfo/domain/usecase/DiscoverNearbyGenresUseCase.kt`
- Test: `app/src/test/kotlin/com/example/vinfo/domain/usecase/DiscoverNearbyGenresUseCaseTest.kt`

- [ ] **Step 1: Write the failing candidate-filter tests**

```kotlin
class DiscoverNearbyGenresUseCaseTest {
    private val useCase = DiscoverNearbyGenresUseCase()

    @Test
    fun `filters unknown self duplicates and weak candidates then keeps strongest six`() {
        val result = useCase(
            sourceGenre = "Hyperpop",
            candidates = listOf(
                GenreRelationCandidate("Electropop", 0.92f, "influence", "근거 A"),
                GenreRelationCandidate("Synth-pop", 0.81f, "influence", "근거 B"),
                GenreRelationCandidate("Trap", 0.73f, "adjacent", "근거 C"),
                GenreRelationCandidate("Bubblegum Pop", 0.66f, "influence", "근거 D"),
                GenreRelationCandidate("EDM", 0.58f, "adjacent", "근거 E"),
                GenreRelationCandidate("Hip Hop", 0.51f, "adjacent", "근거 F"),
                GenreRelationCandidate("Dance-pop", 0.49f, "adjacent", "근거 G"),
                GenreRelationCandidate("Unknown", 0.99f, "adjacent", "제외"),
                GenreRelationCandidate("Hyperpop", 0.95f, "adjacent", "제외"),
                GenreRelationCandidate("Electropop", 0.44f, "adjacent", "중복"),
                GenreRelationCandidate("Noise Pop", 0.20f, "adjacent", "약함")
            )
        )

        assertEquals(6, result.size)
        assertEquals("Electropop", result.first().genreName)
        assertFalse(result.any { it.genreName == "Unknown" })
        assertFalse(result.any { it.genreName == "Hyperpop" })
    }

    @Test
    fun `maps numeric strength to strong medium and weak labels`() {
        assertEquals(RelationStrength.STRONG, RelationStrength.fromScore(0.85f))
        assertEquals(RelationStrength.MEDIUM, RelationStrength.fromScore(0.60f))
        assertEquals(RelationStrength.WEAK, RelationStrength.fromScore(0.35f))
    }
}
```

- [ ] **Step 2: Run the tests and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*DiscoverNearbyGenresUseCaseTest"
```

Expected: FAIL because `DiscoverNearbyGenresUseCase`, `GenreRelationCandidate`, and `RelationStrength` do not exist.

- [ ] **Step 3: Add the minimal domain model**

```kotlin
enum class RelationStrength(val koreanLabel: String) {
    STRONG("강함"),
    MEDIUM("보통"),
    WEAK("약함");

    companion object {
        fun fromScore(score: Float): RelationStrength = when {
            score >= 0.75f -> STRONG
            score >= 0.50f -> MEDIUM
            else -> WEAK
        }
    }
}

data class GenreRelationCandidate(
    val genreName: String,
    val score: Float,
    val relationType: String,
    val evidence: String
) {
    val strength: RelationStrength = RelationStrength.fromScore(score)
}

data class ConfirmedGenreDiscovery(
    val sourceGenre: String,
    val candidates: List<GenreRelationCandidate>
)
```

- [ ] **Step 4: Implement the pure filtering use case**

```kotlin
class DiscoverNearbyGenresUseCase {
    operator fun invoke(
        sourceGenre: String,
        candidates: List<GenreRelationCandidate>
    ): List<GenreRelationCandidate> {
        val sourceKey = sourceGenre.normalizedGenreKey()
        return candidates
            .filter { it.genreName.isNotBlank() }
            .filterNot { it.genreName.equals("unknown", ignoreCase = true) }
            .filterNot { it.genreName.normalizedGenreKey() == sourceKey }
            .filter { it.score >= 0.35f }
            .groupBy { it.genreName.normalizedGenreKey() }
            .values
            .mapNotNull { duplicates -> duplicates.maxByOrNull(GenreRelationCandidate::score) }
            .sortedByDescending(GenreRelationCandidate::score)
            .take(6)
    }
}

private fun String.normalizedGenreKey(): String {
    return trim()
        .lowercase()
        .replace(Regex("""[^a-z0-9]+"""), "")
}
```

- [ ] **Step 5: Run the tests and verify GREEN**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*DiscoverNearbyGenresUseCaseTest"
```

Expected: PASS.

---

### Task 2: Add Gemini Search Request And JSON Parser

**Files:**
- Create: `app/src/main/java/com/example/vinfo/data/remote/gemini/GeminiGenreRelationRequestBuilder.kt`
- Create: `app/src/main/java/com/example/vinfo/data/remote/gemini/GeminiGenreRelationJsonParser.kt`
- Test: `app/src/test/kotlin/com/example/vinfo/data/remote/gemini/GeminiGenreRelationRequestBuilderTest.kt`
- Test: `app/src/test/kotlin/com/example/vinfo/data/remote/gemini/GeminiGenreRelationJsonParserTest.kt`

- [ ] **Step 1: Write failing request-builder tests**

Assert that the request:

```kotlin
assertTrue(systemText.contains("selected genre"))
assertTrue(systemText.contains("nearby_genres"))
assertTrue(systemText.contains("relation_strength"))
assertTrue(systemText.contains("Do not invent"))
assertTrue(userText.contains("Selected genre: Hyperpop"))
assertEquals("application/json", generationConfig.getString("responseMimeType"))
assertEquals(0, googleSearchTool.length())
```

- [ ] **Step 2: Run the builder test and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*GeminiGenreRelationRequestBuilderTest"
```

Expected: FAIL because the relation request builder does not exist.

- [ ] **Step 3: Implement a dedicated grounded request**

The request must require this JSON contract:

```json
{
  "selected_genre": "Hyperpop",
  "nearby_genres": [
    {
      "genre": "Electropop",
      "relation_strength": 0.92,
      "relation_type": "influence",
      "evidence": "Short evidence grounded in search results"
    }
  ],
  "reliability_notes": []
}
```

Prompt rules:

```text
- Search for historically or stylistically meaningful relationships for the selected genre.
- Return at most 8 candidates so the app can validate and trim the result.
- relation_strength must be a number between 0.0 and 1.0.
- Do not invent a relationship when search grounding is weak.
- Omit unknown or unsupported genres.
- Do not return arrows or graph layout coordinates.
```

- [ ] **Step 4: Write failing parser tests**

Cover:

```kotlin
assertEquals("Hyperpop", payload.selectedGenre)
assertEquals(2, payload.nearbyGenres.size)
assertEquals("Electropop", payload.nearbyGenres.first().genreName)
assertEquals(0.92f, payload.nearbyGenres.first().score, 0.001f)
```

Also verify that malformed array items, blank names, and out-of-range scores are skipped or clamped without crashing the entire response.

- [ ] **Step 5: Run the parser tests and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*GeminiGenreRelationJsonParserTest"
```

Expected: FAIL because the parser does not exist.

- [ ] **Step 6: Implement parser extraction**

Reuse the existing `GeminiJsonParser` behavior:

```kotlin
val payload = extractGeminiText(rawResponse) ?: rawResponse
val jsonText = extractFirstJsonObject(payload)
    ?: return AppResult.Error("장르 관계 응답에서 JSON 객체를 찾을 수 없습니다.")
```

Parse `nearby_genres` into `GenreRelationCandidate`, trim strings, clamp strength to `0f..1f`, and preserve `evidence`.

- [ ] **Step 7: Run builder and parser tests and verify GREEN**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*GeminiGenreRelation*"
```

Expected: PASS.

---

### Task 3: Add Repository And ViewModel State

**Files:**
- Create: `app/src/main/java/com/example/vinfo/domain/repository/GenreRelationDiscoveryRepository.kt`
- Create: `app/src/main/java/com/example/vinfo/data/remote/gemini/GeminiGenreRelationDiscoveryRepository.kt`
- Create: `app/src/main/java/com/example/vinfo/ui/stats/GenreMapViewModel.kt`
- Test: `app/src/test/kotlin/com/example/vinfo/ui/stats/GenreMapDiscoveryReducerTest.kt`

- [ ] **Step 1: Write failing reducer tests**

Use a pure reducer so popup behavior can be tested without Android lifecycle setup:

```kotlin
@Test
fun `loading success and confirm update popup state and confirmed discoveries`() {
    val loading = GenreMapDiscoveryState().startSearch("Hyperpop")
    assertTrue(loading.isLoading)

    val loaded = loading.showCandidates(
        listOf(GenreRelationCandidate("Electropop", 0.9f, "influence", "근거"))
    )
    assertTrue(loaded.isPopupVisible)
    assertEquals("Electropop", loaded.candidates.single().genreName)

    val confirmed = loaded.confirmCandidates()
    assertFalse(confirmed.isPopupVisible)
    assertEquals("Hyperpop", confirmed.confirmedDiscoveries.single().sourceGenre)
}
```

- [ ] **Step 2: Run reducer tests and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*GenreMapDiscoveryReducerTest"
```

Expected: FAIL because `GenreMapDiscoveryState` does not exist.

- [ ] **Step 3: Implement repository interface**

```kotlin
interface GenreRelationDiscoveryRepository {
    suspend fun discoverNearbyGenres(
        selectedGenre: String,
        apiKey: String
    ): AppResult<List<GenreRelationCandidate>>
}
```

The Gemini implementation must:

1. Reject blank API keys.
2. Call `GeminiApiClientFactory.create(apiKey)`.
3. Use `GeminiGenreRelationRequestBuilder.build(selectedGenre)`.
4. Parse with `GeminiGenreRelationJsonParser`.
5. Filter using `DiscoverNearbyGenresUseCase`.

- [ ] **Step 4: Implement immutable popup reducer state**

```kotlin
data class GenreMapDiscoveryState(
    val selectedGenre: String? = null,
    val isLoading: Boolean = false,
    val isPopupVisible: Boolean = false,
    val candidates: List<GenreRelationCandidate> = emptyList(),
    val confirmedDiscoveries: List<ConfirmedGenreDiscovery> = emptyList(),
    val errorMessage: String? = null
)
```

Reducer functions:

```kotlin
fun startSearch(genre: String): GenreMapDiscoveryState
fun showCandidates(candidates: List<GenreRelationCandidate>): GenreMapDiscoveryState
fun showError(message: String): GenreMapDiscoveryState
fun dismissPopup(): GenreMapDiscoveryState
fun confirmCandidates(): GenreMapDiscoveryState
```

- [ ] **Step 5: Implement `GenreMapViewModel`**

The ViewModel reads the existing Gemini key with:

```kotlin
private val apiKeyStore = ApiKeyStore(application.applicationContext)
```

Public actions:

```kotlin
fun findNearbyGenres(selectedGenre: String)
fun dismissDiscoveryPopup()
fun confirmDiscoveryCandidates()
```

Expose:

```kotlin
val discoveryState: StateFlow<GenreMapDiscoveryState>
```

- [ ] **Step 6: Run reducer tests and verify GREEN**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*GenreMapDiscoveryReducerTest"
```

Expected: PASS.

---

### Task 4: Merge Confirmed Nodes Into The Full-Screen Map

**Files:**
- Modify: `app/src/main/java/com/example/vinfo/ui/stats/GenreMapScreen.kt`
- Test: `app/src/test/kotlin/com/example/vinfo/ui/stats/GenreMapDiscoveryReducerTest.kt`

- [ ] **Step 1: Extend the failing reducer test for duplicate confirmation**

```kotlin
@Test
fun `confirming the same discovered genre twice keeps the stronger relation`() {
    val state = GenreMapDiscoveryState()
        .startSearch("Hyperpop")
        .showCandidates(listOf(GenreRelationCandidate("Electropop", 0.6f, "adjacent", "약한 근거")))
        .confirmCandidates()
        .startSearch("Hyperpop")
        .showCandidates(listOf(GenreRelationCandidate("Electropop", 0.9f, "influence", "강한 근거")))
        .confirmCandidates()

    val electropop = state.confirmedDiscoveries.single().candidates.single()
    assertEquals(0.9f, electropop.score, 0.001f)
}
```

- [ ] **Step 2: Run the test and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*GenreMapDiscoveryReducerTest"
```

Expected: FAIL until confirmation merges duplicate source-target relations.

- [ ] **Step 3: Add discovery overlay state to `GenreMapUiState`**

Change edges to carry a score:

```kotlin
internal data class GenreMapEdgeUi(
    val fromId: String,
    val toId: String,
    val label: String,
    val evidence: String,
    val relationScore: Float,
)
```

Add a pure merge function:

```kotlin
fun GenreMapUiState.withDiscoveries(
    discoveries: List<ConfirmedGenreDiscovery>
): GenreMapUiState
```

Rules:

- Existing archive-driven nodes remain `Activated`.
- Existing archive-driven edges receive `relationScore = 1f` until they are replaced by a searched relationship score.
- Confirmed search results become `Adjacent`.
- Existing nodes are reused by normalized genre key.
- New nodes are arranged radially around the selected source node.
- A repeated relation keeps the stronger score and its evidence.
- No `Locked` node is emitted.

- [ ] **Step 4: Render line strength without arrows**

Replace fixed edge widths with:

```kotlin
val normalizedScore = edge.relationScore.coerceIn(0f, 1f)
val strokeWidth = 1.8f + normalizedScore * 4.2f
val alpha = 0.35f + normalizedScore * 0.55f

drawLine(
    color = lineColor.copy(alpha = alpha),
    start = start,
    end = end,
    strokeWidth = strokeWidth,
    cap = StrokeCap.Round
)
```

Do not draw arrowheads or dashed locked lines.

- [ ] **Step 5: Run unit tests and compile**

Run:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat compileDebugKotlin
```

Expected: PASS.

---

### Task 5: Add The Nearby Genre Popup UI

**Files:**
- Modify: `app/src/main/java/com/example/vinfo/ui/stats/GenreMapScreen.kt`
- Modify: `app/src/main/java/com/example/vinfo/MainActivity.kt`

- [ ] **Step 1: Wire `GenreMapViewModel` into the map route**

In `MainActivity.kt`, keep `archiveItems` and pass the ViewModel-backed actions through `GenreMapScreen`.

Screen parameters:

```kotlin
discoveryState: GenreMapDiscoveryState,
onFindNearbyGenres: (String) -> Unit,
onDismissDiscoveryPopup: () -> Unit,
onConfirmDiscoveryCandidates: () -> Unit,
```

- [ ] **Step 2: Add a button to the selected-node bottom sheet**

Place the button below the selected genre title:

```kotlin
Button(
    onClick = { onFindNearbyGenres(selectedNode.label) },
    enabled = !isDiscoveryLoading
) {
    Text(if (isDiscoveryLoading) "찾는 중..." else "근처 장르 찾기")
}
```

- [ ] **Step 3: Add the popup list**

Use `AlertDialog` with:

```text
Title: {선택 장르} 주변 장르
Subtitle: 검색 결과를 지도에 반영할 수 있습니다.

장르                         연관성
Electropop                   강함
Synth-pop                    강함
Bubblegum Pop                보통
Trap                         약함

[닫기]                [지도에 반영]
```

Compose structure:

```kotlin
Column {
    RelationListHeader()
    discoveryState.candidates.forEach { candidate ->
        NearbyGenreRow(
            genreName = candidate.genreName,
            strengthLabel = candidate.strength.koreanLabel
        )
    }
}
```

Behavior:

- 로딩 중에는 버튼 텍스트를 `찾는 중...`으로 바꾸고 중복 요청을 막는다.
- 결과가 비어 있으면 `"확인 가능한 주변 장르를 찾지 못했습니다."`를 표시한다.
- API 키가 없으면 팝업 안에 설정 화면에서 Gemini 키를 등록하라는 오류를 표시한다.
- `지도에 반영`은 후보가 있을 때만 활성화한다.

- [ ] **Step 4: Apply confirmed discoveries to map rendering**

```kotlin
val mapState = remember(archiveItems, discoveryState.confirmedDiscoveries) {
    GenreMapUiState
        .fromArchive(archiveItems)
        .withDiscoveries(discoveryState.confirmedDiscoveries)
}
```

- [ ] **Step 5: Compile the app**

Run:

```powershell
.\gradlew.bat compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

---

### Task 6: Update Documentation For User-Driven Discovery

**Files:**
- Modify: `docs/SRS.md`
- Modify: `docs/SDD.md`
- Modify: `docs/DATA.md`
- Modify: `docs/UI.md`
- Modify: `docs/REMAINING_TASKS.md`

- [ ] **Step 1: Correct the relationship ownership wording**

Document the staged policy precisely:

```text
Gemini may search and propose nearby genre relationships only after an explicit user action.
The app does not automatically expand the graph during album save.
Search results remain preview candidates until the user presses "지도에 반영".
```

- [ ] **Step 2: Add the popup JSON contract**

Add:

```json
{
  "selected_genre": "Hyperpop",
  "nearby_genres": [
    {
      "genre": "Electropop",
      "relation_strength": 0.92,
      "relation_type": "influence",
      "evidence": "..."
    }
  ]
}
```

- [ ] **Step 3: Mark MVP and follow-up boundaries**

Current MVP:

- User-triggered Gemini relationship search
- Popup preview list
- Session-level map reflection
- Weight-based solid lines without arrows

Follow-up:

- Room persistence for confirmed discoveries
- Curated review queue
- Album metadata candidate arrays (`primary_genres`, `secondary_genres`, `microgenres`)
- Relation cache with expiry and dictionary versioning

---

### Task 7: Full Verification

**Files:**
- Verify only

- [ ] **Step 1: Run focused unit tests**

```powershell
.\gradlew.bat testDebugUnitTest --tests "*DiscoverNearbyGenresUseCaseTest" --tests "*GeminiGenreRelation*" --tests "*GenreMapDiscoveryReducerTest"
```

Expected: PASS.

- [ ] **Step 2: Run the full unit suite**

```powershell
.\gradlew.bat testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 3: Compile the debug build**

```powershell
.\gradlew.bat compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run whitespace validation**

```powershell
git diff --check
```

Expected: no whitespace errors.

- [ ] **Step 5: Device smoke test**

On the Galaxy device:

1. Open the Taste Map.
2. Tap an active genre node.
3. Tap `근처 장르 찾기`.
4. Confirm the popup rows show genre on the left and relation strength on the right.
5. Tap `지도에 반영`.
6. Confirm new nodes appear around the selected node.
7. Confirm stronger relations draw thicker, clearer solid lines.
8. Confirm no arrows, unknown nodes, or locked placeholders appear.
9. Leave the map and return during the same navigation session; confirm the newly reflected nodes remain visible.
