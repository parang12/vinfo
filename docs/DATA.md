# vinfo Data Contract

**Project:** vinfo (Vinyl + Information)  
**Scope:** Perplexity/Lyrics/Gemini API 계약, DTO, 저장 구조, 정규화, 캐싱, 오류 처리, 마이그레이션

이 문서는 vinfo의 데이터 계층 계약을 정의한다. SDD는 상위 구조만 유지하고, 세부 데이터 계약은 이 문서를 기준으로 구현한다.

---

## 1. Data Layer Overview

Data Layer는 외부 API와 로컬 저장소를 캡슐화하고, Domain/Presentation이 안정적인 모델만 보도록 만든다.

- Remote: Perplexity, lyrics.ovh, Gemini
- Local: Room DB, DataStore, SharedPreferences(전환 중)
- Mapping: JSON -> DTO -> Domain Model -> Entity
- Strategy: 부분 실패 허용, 캐시 우선, 정규화 우선

---

## 2. Track Metadata Contract

### 2.1 Domain Model

```kotlin
data class TrackMetadata(
    val artist: String,
    val title: String,
    val album: String?,
    val primaryGenre: GenreCategory,
    val secondaryGenre: GenreCategory?,
    val genreSource: GenreSource,
    val rymRating: Float?,
    val criticsSummary: String,
    val interviewSummary: String?,
    val listeningGuide: String,
    val samplesUsed: List<String>
)
```

```kotlin
enum class GenreCategory {
    HIP_HOP, POP, ROCK, ELECTRONIC, JAZZ, CLASSICAL, RNB, UNKNOWN
}

enum class GenreSource {
    RYM, LLM, MANUAL, UNKNOWN
}
```

### 2.2 Room Entity

```kotlin
@Entity(tableName = "archive_table")
data class ArchiveEntity(
    @PrimaryKey val trackId: String,
    @ColumnInfo(name = "artist") val artist: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "album") val album: String?,
    @ColumnInfo(name = "primary_genre") val primaryGenre: String,
    @ColumnInfo(name = "secondary_genre") val secondaryGenre: String?,
    @ColumnInfo(name = "genre_source") val genreSource: String,
    @ColumnInfo(name = "rym_rating") val rymRating: Float?,
    @ColumnInfo(name = "critics_summary") val criticsSummary: String,
    @ColumnInfo(name = "interview_summary") val interviewSummary: String?,
    @ColumnInfo(name = "listening_guide") val listeningGuide: String,
    @ColumnInfo(name = "samples_used_json") val samplesUsedJson: String,
    @ColumnInfo(name = "original_lyrics") val originalLyrics: String?,
    @ColumnInfo(name = "translated_lyrics") val translatedLyrics: String?,
    @ColumnInfo(name = "timestamp") val timestamp: Long
)
```

### 2.3 Genre Statistics Projection

```kotlin
data class GenreStat(
    val genre: String,
    val count: Int
)
```

```kotlin
@Dao
interface ArchiveDao {
    @Query("SELECT primary_genre as genre, COUNT(*) as count FROM archive_table GROUP BY primary_genre ORDER BY count DESC")
    suspend fun getGenreStatistics(): List<GenreStat>

    @Query("SELECT primary_genre as genre, COUNT(*) as count FROM archive_table WHERE timestamp >= :sinceTimestamp GROUP BY primary_genre ORDER BY count DESC")
    suspend fun getGenreStatistics(sinceTimestamp: Long): List<GenreStat>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchive(entity: ArchiveEntity)

    @Query("SELECT * FROM archive_table ORDER BY timestamp DESC")
    fun getAllArchives(): Flow<List<ArchiveEntity>>

    @Query("SELECT * FROM archive_table WHERE artist LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchArchives(query: String): Flow<List<ArchiveEntity>>

    @Query("SELECT * FROM archive_table WHERE primary_genre IN (:genres) ORDER BY timestamp DESC")
    fun filterArchivesByGenres(genres: List<String>): Flow<List<ArchiveEntity>>

    @Query("SELECT * FROM archive_table WHERE trackId = :id")
    suspend fun getArchiveById(id: String): ArchiveEntity?
}
```

### 2.4 Genre Normalization Contract

- `GenreMapper`는 LLM 원문 장르를 표준 `GenreCategory`로 정규화한다.
- 예시 매핑: `Synth-pop`, `Electro Pop`, `Electronic Pop` -> `POP`.
- 매핑 실패 시 `UNKNOWN`으로 저장하고 `genreSource = LLM`을 유지한다.
- 통계 집계는 정규화된 `primaryGenre`만 사용한다.

---

## 3. Perplexity API Contract

### 3.1 Request Contract

- **Method:** `POST`
- **Endpoint:** `https://api.perplexity.ai/v1/sonar`
- **Model:** `sonar-pro`
- **Output Constraint:** `response_format.type = "json_schema"`
- **Input Variables:** `artist_name`, `track_title`, `album_title`

```json
{
  "model": "sonar-pro",
  "messages": [
    {
      "role": "system",
      "content": "vinfo 음악 분석 assistant 역할 정의"
    },
    {
      "role": "user",
      "content": "Artist: {{artist_name}}\nTitle: {{track_title}}\nAlbum: {{album_title}}"
    }
  ],
  "response_format": {
    "type": "json_schema",
    "json_schema": {
      "name": "vinfo_music_research",
      "schema": {
        "type": "object",
        "required": [
          "critic_review",
          "genres",
          "rym",
          "interviews",
          "sampling",
          "listening_guide",
          "taste_exploration",
          "reliability"
        ],
        "properties": {
          "critic_review": {
            "type": "object",
            "required": ["summary"],
            "properties": {
              "summary": { "type": "string" },
              "highlights": {
                "type": "array",
                "items": { "type": "string" }
              },
              "sources": {
                "type": "array",
                "items": { "type": "string" }
              }
            }
          },
          "genres": {
            "type": "object",
            "required": ["primary"],
            "properties": {
              "primary": { "type": "string" },
              "secondary": {
                "type": "array",
                "items": { "type": "string" }
              },
              "normalized_primary": { "type": "string" },
              "genre_source": { "type": "string" }
            }
          },
          "rym": {
            "type": "object",
            "properties": {
              "score": { "type": "number" },
              "estimated": { "type": "boolean" },
              "notes": { "type": "string" }
            }
          },
          "interviews": {
            "type": "array",
            "items": {
              "type": "object",
              "required": ["summary"],
              "properties": {
                "summary": { "type": "string" },
                "speaker": { "type": "string" },
                "source": { "type": "string" }
              }
            }
          },
          "sampling": {
            "type": "object",
            "properties": {
              "used": {
                "type": "array",
                "items": { "type": "string" }
              },
              "notes": { "type": "string" }
            }
          },
          "listening_guide": {
            "type": "object",
            "properties": {
              "summary": { "type": "string" },
              "focus_points": {
                "type": "array",
                "items": { "type": "string" }
              }
            }
          },
          "taste_exploration": {
            "type": "object",
            "properties": {
              "center_genre": { "type": "string" },
              "adjacent_genres": {
                "type": "array",
                "items": { "type": "string" }
              },
              "expansion_notes": { "type": "string" }
            }
          },
          "reliability": {
            "type": "object",
            "properties": {
              "confidence": { "type": "number" },
              "source_count": { "type": "integer" },
              "warnings": {
                "type": "array",
                "items": { "type": "string" }
              }
            }
          }
        }
      }
    }
  }
}
```

### 3.2 Response Contract

- `critic_review.summary`: 평론 요약
- `genres.primary`: 원문 장르
- `genres.normalized_primary`: 앱 내부 표준 장르
- `rym.score`: RYM 점수 또는 추정 점수
- `interviews[*].summary`: 인터뷰 요약 목록
- `sampling.used`: 샘플링 원곡 목록
- `listening_guide.focus_points`: 감상 포인트 목록
- `taste_exploration.adjacent_genres`: 취향 탐험 지도용 인접 장르 후보
- `reliability`: 정보 신뢰도 및 주의사항

### 3.3 Kotlin DTO Mapping Contract

```kotlin
@Serializable
data class PerplexityRequest(
    @SerialName("model") val model: String = "sonar-pro",
    @SerialName("messages") val messages: List<PerplexityMessage>,
    @SerialName("response_format") val responseFormat: ResponseFormat
)

@Serializable
data class PerplexityMessage(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String
)

@Serializable
data class ResponseFormat(
    @SerialName("type") val type: String = "json_schema",
    @SerialName("json_schema") val jsonSchema: JsonSchemaEnvelope
)

@Serializable
data class JsonSchemaEnvelope(
    @SerialName("name") val name: String,
    @SerialName("schema") val schema: JsonElement
)

@Serializable
data class VinfoMusicResearchResponse(
    @SerialName("critic_review") val criticReview: CriticReview,
    @SerialName("genres") val genres: GenreInfo,
    @SerialName("rym") val rym: RymInfo,
    @SerialName("interviews") val interviews: List<InterviewInfo> = emptyList(),
    @SerialName("sampling") val sampling: SamplingInfo,
    @SerialName("listening_guide") val listeningGuide: ListeningGuide,
    @SerialName("taste_exploration") val tasteExploration: TasteExplorationInfo,
    @SerialName("reliability") val reliability: ReliabilityInfo
)
```

### 3.4 Validation Rules

- `artist_name`, `track_title`는 필수이다.
- `album_title`은 비어 있어도 요청은 가능하다.
- 응답 파싱은 `json_schema` 결과를 1차 검증하고, DTO 파싱을 2차 검증으로 수행한다.
- `critic_review.summary`, `genres.primary`, `listening_guide.summary`는 최소 필수 필드로 간주한다.
- `taste_exploration.adjacent_genres`는 취향 탐험 지도 계산의 입력 후보로 사용하되, 앱 내부 장르 정규화와 별개로 보관한다.

---

## 4. Data Flow & Concurrency Design

1. **Event Emit:** `ActiveMediaMonitorService`가 새로운 곡을 감지하고 `SharedFlow<NowPlayingEvent>`로 발행한다.
2. **State Loading:** `DetailViewModel`은 UI 상태를 `Loading`으로 전환하고 스켈레톤(Shimmer) UI를 렌더링한다.
3. **Parallel Execution:**
   - `supervisorScope` 내부에서 메타데이터 수집과 가사 수집을 독립 코루틴으로 동시에 실행한다.
   - **Job A:** `PerplexityRepository.getMetadata()` 호출 (수 초 소요).
   - **Job B:** `LyricsRepository.getRawLyrics()` 호출 (수백 ms 소요).
4. **Sequential Execution:**
   - Job B가 성공(`AppResult.Success`)한 경우에만 **Job C** `GeminiRepository.translate(rawLyrics)`를 실행한다.
   - 실패 시 번역 없이 원문 가사 상태(`LyricsUiState.LyricsOnly`)로 진행한다.
5. **Partial UI Update:**
   - Job A와 Job B/C는 서로를 기다리지 않는다.
   - Job A가 끝나면 메타데이터 영역(`MetadataUiState`)을 즉시 렌더링한다.
   - 번역(Job C) 진행 중에도 상단 평론/장르/평점 영역은 사용 가능해야 한다.
6. **Final Commit:** 최소 저장 조건을 만족하면 `ArchiveRepository.save()`를 호출해 로컬 DB에 저장한다.

### 4.1 저장 조건 및 중복 정책

- `artist`와 `title`은 필수값이다. 둘 중 하나라도 비어 있으면 정보 수집과 저장을 수행하지 않고 `NowPlayingUiState.InvalidTrack`을 표시한다.
- Notification Access 권한이 없는 경우 `NowPlayingScreen` 인라인 수동 입력 폼에서 `artist/title`을 직접 입력받아 동일 파이프라인을 실행한다.
- `trackId`는 정규화된 `artist + title` 문자열의 SHA-256 해시 앞 16바이트를 hex로 변환해 생성한다. 정규화는 trim, lowercase, 연속 공백 1칸 치환을 적용한다.
- 메타데이터 API가 실패해도 `artist`, `title`, `timestamp`만으로 아카이브 저장이 가능하다. 이때 `primaryGenre = UNKNOWN`, `secondaryGenre = null`, `genreSource = UNKNOWN`, `criticsSummary = ""`, `interviewSummary = null`, `listeningGuide = ""`, `samplesUsed = emptyList()`, `rymRating = null`로 저장한다.
- 가사 또는 번역 실패는 저장 실패로 간주하지 않는다. `originalLyrics`, `translatedLyrics`는 각각 nullable로 저장한다.
- 동일 `trackId`가 다시 저장되면 Repository가 기존 row를 먼저 조회해 새 응답과 병합한다. 병합된 최종 Entity를 `OnConflictStrategy.REPLACE`로 저장하며, 새 응답이 null인 필드는 기존 non-null 값을 유지한다.

---

## 5. Error Handling & Resilience Strategy

시스템 전반의 에러 처리는 **부분 실패 허용(partial failure tolerant)** 을 기본 원칙으로 한다.

### 5.1 공통 결과 래퍼

```kotlin
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val message: String, val throwable: Throwable? = null) : AppResult<Nothing>
    object Loading : AppResult<Nothing>
}
```

### 5.2 실패 처리 원칙

| 실패 지점 | 처리 방식 |
|---|---|
| 메타데이터 조회 실패 | 기본 곡 정보(artist/title)만 유지, 에러 배너 표시 |
| 가사 조회 실패 | 가사 카드 숨김 또는 "가사를 찾을 수 없음" 렌더링 |
| 번역 실패 | 원문 가사만 표시, 번역 버튼 재시도 제공 |
| DB 저장 실패 | Snackbar로 재시도 액션 제공, 메모리 캐시 유지 |

### 5.3 Retry 정책

- 네트워크 API는 최대 **2회** 재시도한다.
- 재시도 간격은 지수 백오프(`500ms → 1000ms`)를 사용한다.
- HTTP **4xx**는 재시도하지 않고, **5xx** 및 타임아웃만 재시도 대상으로 본다.

---

## 6. Dependency Injection Structure

### 6.1 모듈 구성

- **`NetworkModule`:** `OkHttpClient`, `Retrofit`, API Service, `kotlinx.serialization.Json` 제공.
- **`ParserModule`:** `LlmJsonParser` 제공.
- **`DatabaseModule`:** Room `AppDatabase`, `ArchiveDao` 제공.
- **`RepositoryModule`:** `TrackRepository`, `LyricsRepository`, `ArchiveRepository` 구현체 바인딩 (`@Binds`).
- **`DispatcherModule`:** `@IoDispatcher`, `@DefaultDispatcher` 한정자(Qualifier)로 주입.

### 6.2 주입 규칙

- ViewModel은 `@HiltViewModel`로 선언한다.
- UseCase는 생성자 주입(`@Inject constructor`)을 사용한다.
- Repository 구현체는 인터페이스 기반으로 `@Binds`로 연결한다.
- `Dispatcher`는 `@Qualifier` 어노테이션으로 명시적으로 구분한다.

---

## 7. Testing Strategy

### 7.1 Unit Test 대상

- UseCase 입력/출력 검증
- DTO ↔ Domain Mapper 변환 검증
- ViewModel 상태 전이 검증 (`Turbine` 활용)
- `LlmJsonParser` JSON 추출, DTO 파싱, 필수 필드 검증 로직 검증
- `GenreMapper` 정규화 맵 검증 (동의어 장르 -> 표준 Enum)
- 수동 입력 폴백 시나리오 검증 (권한 없음 -> 인라인 입력 -> 저장 가능)
- `genreSource` 분기 검증 (`RYM`, `LLM`, `MANUAL`, `UNKNOWN`)

### 7.2 도구

- `JUnit5`
- `MockK`
- `Turbine`
- `kotlinx-coroutines-test`
- `Robolectric`

### 7.3 Integration / UI Test

- Room in-memory DB를 이용한 Repository 통합 테스트
- `MockWebServer`를 이용한 Retrofit API 응답 파싱 테스트
- Compose UI Test: `DetailScreen`, `ArchiveListScreen` 렌더링 및 상태 전이 검증

---

## 8. Permissions & Security

### 8.1 Notification Access

- 앱의 핵심 기능인 음악 감지는 `NotificationListenerService` 권한이 필수다.
- 최초 실행 시 온보딩 다이얼로그로 권한 목적을 설명한 뒤 시스템 설정 화면으로 이동시킨다.
- 권한이 없으면 `NowPlayingScreen`에서 권한 유도 UI와 인라인 수동 입력 폼을 함께 렌더링한다.

### 8.2 API Key 관리

- MVP 개발 단계에서는 API Key를 `local.properties`에 선언하고 Gradle Secrets Plugin을 통해 `BuildConfig`로 주입한다.
- `.gitignore`에 `local.properties`를 반드시 포함한다.
- Release 빌드에는 개발자 개인 키를 포함하지 않는다.
- 공개 배포 전에는 클라이언트에서 Key를 직접 보유하지 않도록 서버 프록시 구조로 전환한다.
- `SettingsScreen`의 API Key 입력 UI는 기본 마스킹, 보기/숨기기 토글, 저장 시 유효성 검사, 저장 성공/실패 피드백을 제공한다.

### 8.3 보안 및 난독화

- Release 빌드에서 R8/ProGuard를 활성화한다.
- Retrofit 사용 DTO 클래스에 `@Keep` 또는 keep rule을 명시한다.
- 로컬 DB에는 민감한 개인 식별 정보(PII)를 저장하지 않는다.

---

## 9. Database Migration Strategy

### 9.1 버전 정책

- `@Database(version = 1)`부터 시작한다.
- 컬럼 추가, 테이블 분리, 인덱스 변경 시 버전을 1씩 증가시킨다.
- `fallbackToDestructiveMigration()`은 디버그 빌드에만 허용하고, 릴리즈 빌드에서는 반드시 `Migration` 객체를 제공한다.

### 9.2 예시 마이그레이션

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE archive_table ADD COLUMN secondary_genre TEXT")
        db.execSQL("ALTER TABLE archive_table ADD COLUMN genre_source TEXT NOT NULL DEFAULT 'UNKNOWN'")
        db.execSQL("ALTER TABLE archive_table ADD COLUMN interview_summary TEXT")
        db.execSQL("ALTER TABLE archive_table ADD COLUMN samples_used_json TEXT NOT NULL DEFAULT '[]'")
    }
}
```

---

## 10. UI Flow Specification

### 10.1 Screen 목록

| 화면 | 역할 |
|---|---|
| `NowPlayingScreen` | 현재 감지된 곡 표시, 정보 수집 로딩 상태, 권한 미허용 시 인라인 수동 입력 |
| `DetailScreen` | 평론, 장르, 평점, 가사, 번역, 샘플 정보 |
| `ArchiveListScreen` | 저장된 곡 목록 조회, 검색, 장르 필터 |
| `ArchiveDetailScreen` | 저장된 단일 아카이브 상세 조회 |
| `GenreStatsScreen` | 장르별 청취 통계 시각화 |
| `SettingsScreen` | 권한 상태, API 설정, 앱 정보 |

### 10.2 Navigation Graph

```kotlin
sealed class Route(val path: String) {
    object NowPlaying    : Route("now_playing")
    object Detail        : Route("detail/{trackId}")
    object Archive       : Route("archive")
    object ArchiveDetail : Route("archive_detail/{trackId}")
    object GenreStats    : Route("genre_stats")
    object Settings      : Route("settings")
}
```

### 10.3 UI State 분리

- `NowPlayingUiState`: 감지 상태, 로딩, 에러
- `MetadataUiState`: 평론, 장르, 평점, 감상 포인트, AI 고지, 에러
- `LyricsUiState`: 원문, 번역문, 번역 로딩, 가사 없음
- `ArchiveUiState`: 저장 성공 여부, 저장 진행 여부, 에러

---

## 11. Genre Visualization Feature

### 11.1 기능 정의

- 저장된 아카이브 데이터를 기반으로 장르별 곡 수를 집계한다.
- 기간 필터(전체 / 최근 30일 / 최근 90일)를 제공한다.
- 차트 항목 터치 시 해당 장르의 아카이브 목록으로 이동할 수 있다.
- 집계 기준은 정규화된 `primaryGenre`이며 `secondaryGenre`는 상세 화면 설명 보조 정보로만 사용한다.

### 11.2 구현 방향

- Domain 계층에 `GetGenreStatisticsUseCase(period: StatPeriod)`를 둔다.
- Data 계층에서 `ArchiveDao.getGenreStatistics(sinceTimestamp: Long)`으로 기간 필터 집계를 수행한다.
- Archive 목록은 `searchArchives(query)` 및 `filterArchivesByGenres(genres)` DAO 계약을 사용해 검색/필터를 지원한다.
- UI 계층에서는 Vico 라이브러리 기반 바 차트 또는 파이 차트를 렌더링한다.

---

## 12. Design Decisions Log

| 결정 사항 | 이유 |
|---|---|
| 외부 API 종속성을 Data Layer에 격리 | Domain/Presentation 재사용성 확보 및 교체 용이성 |
| UI State를 기능 단위로 분리 | Compose 불필요한 Recomposition 방지 |
| 가사/번역 nullable 필드 | 부분 실패 허용 및 파이프라인 연속성 유지 |
