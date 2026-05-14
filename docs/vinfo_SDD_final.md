# Software Design Document (SDD)

**Project:** vinfo (Vinyl + Information)  
**Architecture:** Clean Architecture + MVVM + Unidirectional Data Flow (UDF)

---

## 1. System Architecture Overview

vinfo는 유지보수성과 외부 API(LLM, Lyrics) 종속성 격리를 위해 **Clean Architecture**를 도입한다. 시스템은 3개의 독립적인 계층으로 분리된다.

### 1.1 계층별 책임 (Layer Responsibilities)

- **Presentation Layer (`ui`, `viewmodel`)**
  - Jetpack Compose를 이용한 선언적 UI 렌더링.
  - ViewModel은 Intent(사용자/시스템 액션)를 받아 UseCase를 호출하고, 결과를 `StateFlow`를 통해 UI State로 변환한다.

- **Domain Layer (`domain`, `usecase`, `model`)**
  - 가장 안쪽에 위치하며 프레임워크(Android)에 대한 의존성이 없는 순수 Kotlin 모듈.
  - `GetTrackInformationUseCase`, `TranslateLyricsUseCase`, `SaveArchiveUseCase`, `GetGenreStatisticsUseCase` 등 핵심 비즈니스 로직을 캡슐화한다.
  - Repository Interface를 정의하여 의존성 역전 원칙(DIP)을 적용한다.

- **Data Layer (`data`, `repository`, `source`)**
  - Domain 계층의 Repository Interface를 구현한다.
  - Local(Room) 및 Remote(Retrofit) 데이터 소스를 제어하고, DTO를 Domain Model로 매핑한다.

---

## 2. Detailed Component Specification

### 2.1 Core Services

**`ActiveMediaMonitorService` (`NotificationListenerService`)**

- **기능:** 백그라운드에서 미디어 세션의 메타데이터(`MediaMetadata.METADATA_KEY_TITLE`, `MediaMetadata.METADATA_KEY_ARTIST`) 변경을 감지한다.
- **제약:** 서비스 내에서 직접 네트워크 통신을 수행하지 않는다.
- **이벤트 전달 계약:** 현재 재생곡 변경은 프로세스 생존 중 실시간 이벤트이므로 `MutableSharedFlow<NowPlayingEvent>`로 발행한다. 마지막 감지 곡, 권한 안내 확인 여부, 사용자 설정처럼 재시작 후에도 보존해야 하는 값만 `DataStore`에 저장한다.
- **캐시 키 계약:** `DataStore`에는 `last_track_artist`, `last_track_title`, `last_track_updated_at`, `notification_permission_prompted` 키를 사용한다. `Catch Now`는 `SharedFlow` 최신 이벤트를 우선 사용하고 이벤트가 없으면 `DataStore`의 마지막 곡 정보를 폴백한다.
- **트리거 정책:** 앱이 포그라운드이면 `NowPlayingViewModel`이 `SharedFlow`를 구독해 정보 수집 UseCase를 즉시 실행한다. 앱이 백그라운드이면 서비스는 마지막 곡 정보만 `DataStore`에 기록하고, 실제 네트워크 수집은 사용자가 앱으로 돌아왔을 때 실행한다. MVP에서는 백그라운드 자동 수집용 `WorkManager`를 구현하지 않는다.
- **권한 폴백 계약:** Notification Access 권한이 없으면 `NowPlayingScreen` 내 인라인 수동 입력 UI를 노출해 `artist/title`을 직접 입력받는다.

---

### 2.2 Data Models & Schema

#### 2.2.1 Domain Model (비즈니스 객체)

```kotlin
data class TrackArchive(
    val id: String,           // Artist + Title Hash
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
    val samplesUsed: List<String>,
    val originalLyrics: String?,
    val translatedLyrics: String?,
    val timestamp: Long
)

enum class GenreCategory {
    HIP_HOP, POP, ROCK, ELECTRONIC, JAZZ, CLASSICAL, RNB, UNKNOWN
}

enum class GenreSource {
    RYM, LLM, MANUAL, UNKNOWN
}
```

#### 2.2.2 Room Database Entity (data layer)

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

#### 2.2.3 Genre Statistics Projection

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

#### 2.2.4 Genre Normalization Contract

- `GenreMapper`는 LLM 원문 장르를 표준 `GenreCategory`로 정규화한다.
- 예시 매핑: `Synth-pop`, `Electro Pop`, `Electronic Pop` -> `POP`.
- 매핑 실패 시 `UNKNOWN`으로 저장하고 `genreSource = LLM`을 유지한다.
- 통계 집계는 정규화된 `primaryGenre`만 사용한다.

---

### 2.3 API Interface & Payload (Data Contract)

LLM의 비정형 응답을 시스템이 안정적으로 파싱하기 위해 강한 프롬프트 제약과 DTO 검증이 필요하다.

**Perplexity API 연동 — Mock Payload Example: Kanye West - Runaway**

> 시스템 프롬프트는 아래 JSON 스키마만 반환하도록 강제해야 한다.

```json
{
  "artist": "Kanye West",
  "title": "Runaway",
  "album": "My Beautiful Dark Twisted Fantasy",
  "primary_genre": "HIP_HOP",
    "secondary_genre": "RNB",
    "genre_source": "RYM",
  "rym_rating": 4.09,
  "critics_summary": "Pitchfork 10/10. 자기 성찰과 오만함이 교차하는 걸작...",
    "interview_summary": "Kanye와 프로듀서진은 최소한의 피아노 모티프를 중심으로 감정의 긴장을 설계했다고 밝혔다.",
  "listening_guide": "아웃트로의 보코더 솔로와 단일 피아노 노트의 반복에 집중할 것.",
  "samples_used": ["Rick James - Mary Jane", "Pete Rock & C.L. Smooth - The Basement"]
}
```

**예상 DTO 클래스**

```kotlin
@Serializable
data class TrackMetadataDto(
    @SerialName("artist") val artist: String,
    @SerialName("title") val title: String,
    @SerialName("album") val album: String?,
    @SerialName("primary_genre") val primaryGenre: String,
    @SerialName("secondary_genre") val secondaryGenre: String?,
    @SerialName("genre_source") val genreSource: String?,
    @SerialName("rym_rating") val rymRating: Float?,
    @SerialName("critics_summary") val criticsSummary: String,
    @SerialName("interview_summary") val interviewSummary: String?,
    @SerialName("listening_guide") val listeningGuide: String,
    @SerialName("samples_used") val samplesUsed: List<String>?
)
```

---

## 3. Data Flow & Concurrency Design

음악 정보 수집 파이프라인의 핵심은 **네트워크 레이턴시 은닉(Latency Hiding)** 이다.

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

### 3.1 저장 조건 및 중복 정책

- `artist`와 `title`은 필수값이다. 둘 중 하나라도 비어 있으면 정보 수집과 저장을 수행하지 않고 `NowPlayingUiState.InvalidTrack`을 표시한다.
- Notification Access 권한이 없는 경우 `NowPlayingScreen` 인라인 수동 입력 폼에서 `artist/title`을 직접 입력받아 동일 파이프라인을 실행한다.
- `trackId`는 정규화된 `artist + title` 문자열의 SHA-256 해시 앞 16바이트를 hex로 변환해 생성한다. 정규화는 trim, lowercase, 연속 공백 1칸 치환을 적용한다.
- 메타데이터 API가 실패해도 `artist`, `title`, `timestamp`만으로 아카이브 저장이 가능하다. 이때 `primaryGenre = UNKNOWN`, `secondaryGenre = null`, `genreSource = UNKNOWN`, `criticsSummary = ""`, `interviewSummary = null`, `listeningGuide = ""`, `samplesUsed = emptyList()`, `rymRating = null`로 저장한다.
- 가사 또는 번역 실패는 저장 실패로 간주하지 않는다. `originalLyrics`, `translatedLyrics`는 각각 nullable로 저장한다.
- 동일 `trackId`가 다시 저장되면 Repository가 기존 row를 먼저 조회해 새 응답과 병합한다. 병합된 최종 Entity를 `OnConflictStrategy.REPLACE`로 저장하며, 새 응답이 null인 필드는 기존 non-null 값을 유지한다.

**Coroutine 코드 스케치**

```kotlin
viewModelScope.launch {
    _metadataState.value = MetadataUiState.Loading
    _lyricsState.value = LyricsUiState.Loading

    supervisorScope {
        val metadataJob = launch {
            metadataRepo.getMetadata(artist, title)
                .onSuccess { metadata ->
                    _metadataState.value = MetadataUiState.Success(metadata)
                    partialArchive.update { it.copy(metadata = metadata) }
                }
                .onFailure { message ->
                    _metadataState.value = MetadataUiState.Error(message)
                }
        }

        val lyricsJob = launch {
            val rawLyrics = lyricsRepo.getRawLyrics(artist, title).getOrNull()
            if (rawLyrics == null) {
                _lyricsState.value = LyricsUiState.Unavailable
                return@launch
            }

            _lyricsState.value = LyricsUiState.LyricsOnly(rawLyrics)
            val translated = translationRepo.translate(rawLyrics).getOrNull()
            _lyricsState.value = LyricsUiState.Success(rawLyrics, translated)
            partialArchive.update { it.copy(originalLyrics = rawLyrics, translatedLyrics = translated) }
        }

        joinAll(metadataJob, lyricsJob)
    }

    archiveRepo.save(partialArchive.value.toTrackArchive())
}
```

---

## 4. 논리적 타당성 및 결함 지적 (Sparring Feedback)

### 4.1 의존성 체인의 취약성 (Lyrics → Translation)

**Fact:** `lyrics.ovh`에서 가사를 찾지 못하면(HTTP 404) 이후 번역 파이프라인이 중단될 수 있다.

**Actionable Fix:** `getRawLyrics()`를 `runCatching` 또는 `Result<T>` 패턴으로 감싸고, 실패 시 `originalLyrics = null`, `translatedLyrics = null` 상태로 정상 반환하도록 설계한다.

### 4.2 LLM 응답 지연과 Compose Recomposition 충돌

**Fact:** Jetpack Compose는 상태가 갱신될 때마다 재구성(Recomposition)을 수행하므로, 세분화되지 않은 단일 상태 객체는 Jank를 유발할 수 있다.

**Actionable Fix:** `MetadataUiState`, `LyricsUiState`, `ArchiveUiState`를 분리하여 각 UI 컴포넌트가 자신에게 필요한 상태 변화만 구독(`collectAsStateWithLifecycle`)하도록 렌더링 스코프를 최소화한다.

### 4.3 데이터 무결성 훼손 (JSON 파싱 실패)

**Fact:** Perplexity가 JSON 형식을 완벽히 지킨다는 보장이 없다. 설명 텍스트를 덧붙이거나 키 이름을 임의로 바꾸면 Retrofit Converter(Gson/Moshi)가 즉시 예외를 던진다.

**Actionable Fix:** LLM API 응답은 Retrofit Converter에 직접 연결하지 않는다. Data Layer는 문자열 응답을 받은 뒤 `LlmJsonParser`에서 JSON 추출, DTO 파싱, 필수 필드 검증을 순서대로 수행한다. 정규표현식 기반 전체 응답 가공 Interceptor는 사용하지 않는다.

```kotlin
class LlmJsonParser(
    private val json: Json
) {
    fun parseTrackMetadata(rawText: String): AppResult<TrackMetadataDto> {
        val jsonText = extractFirstJsonObject(rawText)
            ?: return AppResult.Error("LLM 응답에서 JSON 객체를 찾을 수 없음")

        val dto = runCatching {
            json.decodeFromString<TrackMetadataDto>(jsonText)
        }.getOrElse {
            return AppResult.Error("LLM JSON 파싱 실패", it)
        }

        return if (dto.artist.isBlank() || dto.title.isBlank()) {
            AppResult.Error("LLM 응답 필수 필드 누락")
        } else {
            AppResult.Success(dto)
        }
    }
}
```

`extractFirstJsonObject()`는 중괄호 depth를 계산해 첫 번째 완성된 JSON 객체만 추출한다. 문자열 내부 escape 처리를 고려해야 하며, 단순 정규표현식 `\{[\s\S]*\}`는 사용하지 않는다.

---

## 5. Error Handling & Resilience Strategy

시스템 전반의 에러 처리는 **부분 실패 허용(partial failure tolerant)** 을 기본 원칙으로 한다. 메타데이터, 가사, 번역, 저장 작업은 각각 독립된 실패 가능성을 가지므로 전체 파이프라인을 단일 성공/실패로 처리하지 않는다.

### 5.1 공통 결과 래퍼

```kotlin
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val message: String, val throwable: Throwable? = null) : AppResult<Nothing>
    object Loading : AppResult<Nothing>
}

// 확장 함수
inline fun <T> AppResult<T>.onSuccess(block: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) block(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(block: (String) -> Unit): AppResult<T> {
    if (this is AppResult.Error) block(message)
    return this
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

DI 프레임워크는 **Hilt**를 기준으로 설계한다. 각 계층의 생성 책임을 분리해 테스트 가능성과 모듈 교체 용이성을 확보한다.

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

테스트는 Domain 중심으로 시작하여 Data 및 UI 계층으로 확장한다. 단위 테스트와 통합 테스트를 분리해 유지한다.

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
- `Turbine` (`StateFlow` / `Flow` 테스트)
- `kotlinx-coroutines-test`
- `Robolectric` (Android 프레임워크 의존 단위 테스트)

### 7.3 Integration / UI Test

- Room in-memory DB를 이용한 Repository 통합 테스트
- `MockWebServer`를 이용한 Retrofit API 응답 파싱 테스트
- Compose UI Test: `DetailScreen`, `ArchiveListScreen` 렌더링 및 상태 전이 검증

---

## 8. Permissions & Security

### 8.1 Notification Access

앱의 핵심 기능인 음악 감지는 `NotificationListenerService` 권한이 필수다. 최초 실행 시 온보딩 다이얼로그로 권한 목적을 설명한 뒤 시스템 설정 화면으로 이동시킨다. 권한이 없으면 `NowPlayingScreen`에서 권한 유도 UI와 인라인 수동 입력 폼을 함께 렌더링한다.

### 8.2 API Key 관리

- MVP 개발 단계에서는 API Key를 `local.properties`에 선언하고 **Gradle Secrets Plugin**을 통해 `BuildConfig`로 주입한다.
- `.gitignore`에 `local.properties`를 반드시 포함한다.
- Release 빌드에는 개발자 개인 키를 포함하지 않는다. 내부 테스트용 release가 필요하면 quota가 제한된 별도 키를 사용한다.
- 공개 배포 전에는 클라이언트에서 Key를 직접 보유하지 않도록 **서버 프록시 구조**로 전환한다. 서버 프록시 전환은 상용/공개 배포의 blocker로 간주한다.
- `SettingsScreen`의 API Key 입력 UI는 기본 마스킹, 보기/숨기기 토글, 저장 시 유효성 검사(공백/길이), 저장 성공/실패 피드백(Snackbar)을 제공한다.

### 8.3 보안 및 난독화

- Release 빌드에서 **R8/ProGuard**를 활성화한다.
- Retrofit 사용 DTO 클래스에 `@Keep` 또는 proguard-rules에 keep rule을 명시한다.
- 로컬 DB에는 민감한 개인 식별 정보(PII)를 저장하지 않는다.

---

## 9. Database Migration Strategy

Room DB는 스키마 변경 시 마이그레이션 전략이 없으면 기존 아카이브 데이터가 소실된다. 개발 초기부터 버전 관리 정책을 수립해야 한다.

### 9.1 버전 정책

- `@Database(version = 1)`부터 시작한다.
- 컬럼 추가, 테이블 분리, 인덱스 변경 시 버전을 1씩 증가시킨다.
- `fallbackToDestructiveMigration()`은 **디버그 빌드에만** 허용하고, 릴리즈 빌드에서는 반드시 `Migration` 객체를 제공한다.

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

@Database(
    entities = [ArchiveEntity::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        fun build(context: Context) = Room.databaseBuilder(
            context, AppDatabase::class.java, "vinfo_db"
        ).addMigrations(MIGRATION_1_2).build()
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

- **`NowPlayingUiState`:** 감지 상태, 로딩, 에러
- **`MetadataUiState`:** 평론, 1차/2차 장르, 장르 출처(source), 평점, 감상 포인트, AI 고지(disclaimer), 에러
- **`LyricsUiState`:** 원문, 번역문, 번역 로딩, 가사 없음(`Unavailable`)
- **`ArchiveUiState`:** 저장 성공 여부, 저장 진행 여부, 에러

**AI 고지 렌더링 계약:**

- `MetadataUiState`가 성공 상태일 때 상세 화면 하단에 고정 문구를 노출한다.
- 기본 문구: "이 정보는 AI에 의해 생성되었으며 실제와 다를 수 있습니다."
- 메타데이터 조회 실패/부분 성공 시에도 고지 문구는 숨기지 않는다.

---

## 11. Genre Visualization Feature

사용자의 청취 기록을 기반으로 장르 선호도를 시각화하는 기능은 vinfo의 장기적 차별화 포인트다.

### 11.1 기능 정의

- 저장된 아카이브 데이터를 기반으로 장르별 곡 수를 집계한다.
- 기간 필터(전체 / 최근 30일 / 최근 90일)를 제공한다.
- 차트 항목 터치 시 해당 장르의 아카이브 목록으로 이동할 수 있다.
- 집계 기준은 정규화된 `primaryGenre`이며 `secondaryGenre`는 상세 화면 설명 보조 정보로만 사용한다.

### 11.2 구현 방향

- Domain 계층에 `GetGenreStatisticsUseCase(period: StatPeriod)`를 둔다.
- Data 계층에서 `ArchiveDao.getGenreStatistics(sinceTimestamp: Long)`으로 기간 필터 집계를 수행한다.
- Archive 목록은 `searchArchives(query)` 및 `filterArchivesByGenres(genres)` DAO 계약을 사용해 검색/필터를 지원한다.
- UI 계층에서는 **Vico** 라이브러리 기반 바 차트 또는 파이 차트를 렌더링한다.

### 11.3 확장 가능성

- 월별 장르 변화 추이 꺾은선 그래프
- 가장 많이 저장한 아티스트 Top N 통계
- 청취 시간대별 분포 시각화
- 평균 RYM 평점 추이

---

## 12. Design Decisions Log

| 결정 사항 | 이유 |
|---|---|
| 외부 API 종속성을 Data Layer에 격리 | Domain/Presentation 재사용성 확보 및 교체 용이성 |
| UI State를 기능 단위로 분리 | Compose 불필요한 Recomposition 방지 |
| 가사/번역 nullable 필드 | 부분 실패 허용 및 파이프라인 연속성 유지 |
| `AppResult<T>` 공통 래퍼 도입 | 계층 간 에러 전파 표준화 |
| `LlmJsonParser` 추가 | LLM 비정형 응답을 문자열로 받은 뒤 JSON 추출/파싱/검증을 명시적으로 수행 |
| DB 버전 정책 초기 수립 | 사용자 데이터 소실 방지 |
| MVP 한정 클라이언트 중심 API 연동 | 초기 개발 속도 확보. 공개 배포 전 서버 프록시 전환은 blocker |
| `interviewSummary` 독립 nullable 필드 채택 | 인터뷰 데이터의 존재/부재를 criticsSummary와 분리해 UI 카드와 정확히 매핑 |
| 장르 신뢰도 표시는 `genreSource` enum 채택 | 확률 점수 대신 출처 기반 투명성 제공 및 저장/표시 규칙 단순화 |
| 권한 거부 시 NowPlaying 인라인 수동 입력 채택 | 핵심 기능 접근성을 유지하고 별도 화면 이동 비용을 줄임 |
| 장르 통계는 정규화된 `primaryGenre`만 집계 | LLM 장르 변형으로 인한 통계 분산을 방지 |

---

## 13. Project File Structure

Clean Architecture 계층과 기능 단위를 기준으로 패키지를 분리한다.  
`app/src/main/java/com/example/vinfo/` 이하 구조는 다음과 같다.

```
vinfo/
├── di/
│   ├── NetworkModule.kt          # OkHttpClient, Retrofit, API Service
│   ├── DatabaseModule.kt         # Room AppDatabase, ArchiveDao
│   ├── RepositoryModule.kt       # Repository 구현체 @Binds
│   └── DispatcherModule.kt       # @IoDispatcher, @DefaultDispatcher
│
├── domain/
│   ├── model/
│   │   ├── TrackArchive.kt       # 핵심 비즈니스 객체
│   │   ├── GenreCategory.kt      # Enum
│   │   └── GenreStat.kt          # 장르 통계 집계 결과
│   ├── repository/
│   │   ├── TrackMetadataRepository.kt   # Interface
│   │   ├── LyricsRepository.kt          # Interface
│   │   ├── TranslationRepository.kt     # Interface
│   │   └── ArchiveRepository.kt         # Interface
│   └── usecase/
│       ├── GetTrackInformationUseCase.kt
│       ├── TranslateLyricsUseCase.kt
│       ├── SaveArchiveUseCase.kt
│       └── GetGenreStatisticsUseCase.kt
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt                # Room DB, Migration 관리
│   │   ├── dao/
│   │   │   └── ArchiveDao.kt
│   │   └── entity/
│   │       └── ArchiveEntity.kt
│   ├── remote/
│   │   ├── perplexity/
│   │   │   ├── PerplexityApiService.kt   # Retrofit Interface
│   │   │   └── dto/
│   │   │       └── TrackMetadataDto.kt
│   │   ├── lyrics/
│   │   │   ├── LyricsApiService.kt
│   │   │   └── dto/
│   │   │       └── LyricsResponseDto.kt
│   │   └── gemini/
│   │       ├── GeminiApiService.kt
│   │       └── dto/
│   │           └── TranslationResponseDto.kt
│   ├── parser/
│   │   └── LlmJsonParser.kt              # LLM 응답 JSON 추출/파싱/검증
│   ├── mapper/
│   │   ├── TrackMetadataMapper.kt        # DTO → Domain Model
│   │   ├── ArchiveMapper.kt              # Entity ↔ Domain Model
│   │   └── GenreMapper.kt                # String ↔ GenreCategory Enum
│   └── repository/
│       ├── TrackMetadataRepositoryImpl.kt
│       ├── LyricsRepositoryImpl.kt
│       ├── TranslationRepositoryImpl.kt
│       └── ArchiveRepositoryImpl.kt
│
├── service/
│   └── ActiveMediaMonitorService.kt      # NotificationListenerService
│
├── ui/
│   ├── navigation/
│   │   ├── Route.kt
│   │   └── VinfoNavGraph.kt
│   ├── nowplaying/
│   │   ├── NowPlayingScreen.kt
│   │   ├── NowPlayingViewModel.kt
│   │   └── NowPlayingUiState.kt
│   ├── detail/
│   │   ├── DetailScreen.kt
│   │   ├── DetailViewModel.kt
│   │   ├── MetadataUiState.kt
│   │   └── LyricsUiState.kt
│   ├── archive/
│   │   ├── ArchiveListScreen.kt
│   │   ├── ArchiveListViewModel.kt
│   │   ├── ArchiveDetailScreen.kt
│   │   ├── ArchiveDetailViewModel.kt
│   │   └── ArchiveUiState.kt
│   ├── stats/
│   │   ├── GenreStatsScreen.kt
│   │   └── GenreStatsViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   └── component/
│       ├── ShimmerCard.kt                # 스켈레톤 UI 컴포넌트
│       ├── LyricsCard.kt
│       ├── MetadataCard.kt
│       └── GenreChip.kt
│
├── common/
│   ├── AppResult.kt                      # sealed interface 공통 래퍼
│   ├── extension/
│   │   └── AppResultExt.kt               # onSuccess, onFailure 확장 함수
│   └── qualifier/
│       └── DispatcherQualifiers.kt        # @IoDispatcher, @DefaultDispatcher
│
└── VinfoApplication.kt                   # @HiltAndroidApp
```

### 13.1 패키지 설계 원칙

- `domain/` 패키지는 Android 프레임워크 의존성(`android.*`)을 **import하지 않는다.** 순수 Kotlin 코드로만 구성한다.
- `data/mapper/`는 DTO ↔ Domain Model 변환의 **유일한 책임자**다. RepositoryImpl에서 직접 변환하지 않는다.
- `ui/component/`는 화면에 종속되지 않는 **재사용 가능한 Composable**만 배치한다.
- `service/`는 `ui/`와 직접 통신하지 않는다. 현재 재생 이벤트는 `SharedFlow`, 재시작 후 보존해야 하는 값은 `DataStore`를 사용한다.
