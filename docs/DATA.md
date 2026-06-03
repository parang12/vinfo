# vinfo Data Contract

**Project:** vinfo (Vinyl + Information)  
**Scope:** Gemini/Lyrics API 계약, 앨범 기준 DTO, 저장 구조, 정규화, 캐싱, 오류 처리, 마이그레이션

이 문서는 vinfo의 데이터 계층 계약을 정의한다. SDD는 상위 구조만 유지하고, 세부 데이터 계약은 이 문서를 기준으로 구현한다.

---

## 1. Data Layer Overview

Data Layer는 외부 API와 로컬 저장소를 캡슐화하고, Domain/Presentation이 안정적인 모델만 보도록 만든다.

- Remote: Gemini, lyrics.ovh
- Local: Room DB, DataStore, SharedPreferences(전환 중)
- Mapping: JSON -> DTO -> Domain Model -> Entity
- Strategy: 부분 실패 허용, 캐시 우선, 정규화 우선

---

## 2. Album-based Metadata Contract

vinfo의 메타데이터 파이프라인은 현재 재생 중인 곡의 `artist + title`을 입력으로 받지만, Gemini가 반환해야 하는 장르/평론/평점 정보는 **수록 앨범 기준**이다. `title`은 앨범을 찾기 위한 식별 입력이자 화면에 현재 곡을 표시하기 위한 값이며, RYM/Pitchfork/Metacritic/AOTY 점수와 Primary/Secondary Genre는 곡이 아니라 식별된 앨범에 귀속된다.

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
    val pitchforkScore: Float?,
    val metacriticScore: Int?,
    val aotyScore: Int?,
    val criticsSummary: String,
    val interviewSummary: String?,
    val listeningGuide: String,
    val samplesUsed: List<String>,
    val missingSources: List<String>,
    val reliabilityNotes: List<String>
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

`TrackMetadata`라는 기존 이름은 코드 호환성을 위해 유지할 수 있지만, 의미상으로는 `AlbumMetadataForTrack`에 가깝다. 구현을 확장할 때는 `AlbumMetadata` 또는 `AlbumResearchMetadata`로 이름을 변경하는 것을 권장한다.

### 2.2 Rating Source Contract

```kotlin
data class AlbumRating(
    val source: RatingSource,
    val score: Float?,
    val maxScore: Float?,
    val reviewUrl: String?,
    val note: String?
)

enum class RatingSource {
    RYM,
    PITCHFORK,
    METACRITIC,
    AOTY
}
```

표시 규칙:
- Gemini는 RYM/Pitchfork/Metacritic/AOTY 중 확인 가능한 출처만 값으로 채운다.
- 확인되지 않는 출처는 점수를 추정하지 않고 `null`로 둔다.
- UI는 `null` 점수 카드를 숨긴다.
- `critic_review`와 `listening_guide`는 식별된 앨범 맥락을 기준으로 작성한다.
- 구현 현재 상태는 평점별 URL을 별도 필드로 저장하지 않고, 평탄화된 점수 필드와 `missingSources`/`reliabilityNotes`를 우선 사용한다.

### 2.3 Room Entity

```kotlin
@Entity(tableName = "archive_table")
data class ArchiveEntity(
    @PrimaryKey val trackId: String,
    @ColumnInfo(name = "artist") val artist: String,
  @ColumnInfo(name = "album_title") val albumTitle: String,
    @ColumnInfo(name = "album") val album: String?,
    @ColumnInfo(name = "primary_genre") val primaryGenre: String,
    @ColumnInfo(name = "secondary_genre") val secondaryGenre: String?,
    @ColumnInfo(name = "genre_candidates_json") val genreCandidatesJson: String,
    @ColumnInfo(name = "genre_source") val genreSource: String,
    @ColumnInfo(name = "rym_rating") val rymRating: Float?,
    @ColumnInfo(name = "pitchfork_score") val pitchforkScore: Float?,
    @ColumnInfo(name = "metacritic_score") val metacriticScore: Int?,
    @ColumnInfo(name = "aoty_score") val aotyScore: Int?,
    @ColumnInfo(name = "ratings_json") val ratingsJson: String,
    @ColumnInfo(name = "critics_summary") val criticsSummary: String,
    @ColumnInfo(name = "interview_summary") val interviewSummary: String?,
    @ColumnInfo(name = "listening_guide") val listeningGuide: String,
    @ColumnInfo(name = "samples_used_json") val samplesUsedJson: String,
    @ColumnInfo(name = "original_lyrics") val originalLyrics: String?,
    @ColumnInfo(name = "translated_lyrics") val translatedLyrics: String?,
    @ColumnInfo(name = "timestamp") val timestamp: Long
)
```

저장 스키마에서는 `album_title`이 보관함 카드와 검색 인덱스의 기준 제목이다. 곡명은 메타데이터 조회/식별 입력으로만 사용하고, 로컬 아카이브의 표시 제목은 앨범명으로 유지한다. `primary_genre`와 `secondary_genre`는 통계 호환성을 위한 정규화 대표값이며, 전체 후보와 신뢰도는 `genre_candidates_json`에 보존한다.

### 2.4 Genre Statistics Projection

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

### 2.5 Genre Normalization Contract

- Gemini는 앨범 기준 장르 후보와 신뢰도를 반환한다. 장르 간 영향선은 반환하지 않는다.
- `GenreMapper`는 후보 장르 alias를 표준 장르 사전의 `genreKey`로 정규화한다.
- 예시 매핑: `Synth-pop`, `Synth Pop` -> `SYNTH_POP`; `Jazz rap`, `Jazz Hip Hop` -> `JAZZ_RAP`.
- 사전에 없는 후보와 `UNKNOWN`은 런타임 그래프에서 제외한다.
- MVP 활성화 기준은 `confidence >= 0.80`이다.
- 통계 집계는 검증된 Primary 장르를 우선 사용한다. Secondary/Microgenre는 지도 확장 후보 및 상세 설명에 사용할 수 있다.

### 2.6 Taste Exploration 데이터 모델

`taste_exploration`은 앨범 저장으로 활성화된 중심 장르와 사용자가 직접 요청한 주변 장르 탐색 결과를 분리한다. Gemini는 앨범 저장 시 장르 관계를 자동 생성하지 않으며, 사용자가 `근처 장르 찾기`를 누른 경우에만 선택 장르 주변 후보를 검색한다.

```kotlin
enum class GenreCandidateTier {
  PRIMARY, SECONDARY, MICRO
}

data class AlbumGenreCandidate(
  val name: String,
  val confidence: Float,
  val tier: GenreCandidateTier,
  val evidenceText: String? = null
)

data class GenreDictionaryEntry(
  val genreKey: String,
  val displayName: String,
  val aliases: List<String>,
  val relations: List<GenreRelation>
)

data class GenreRelation(
  val fromGenreKey: String,
  val toGenreKey: String,
  val relationType: GenreRelationType,
  val curated: Boolean = true
)

enum class GenreRelationType {
  INFLUENCE, ADJACENT, DERIVED
}

data class VisibleGenreFlow(
  val activatedNodes: List<String>,
  val adjacentNodes: List<String>,
  val visibleRelations: List<GenreRelation>
)

enum class RelationStrength {
  STRONG, MEDIUM, WEAK
}

data class GenreRelationCandidate(
  val genreName: String,
  val score: Float,
  val relationType: String,
  val evidence: String
)

data class ConfirmedGenreDiscovery(
  val sourceGenre: String,
  val candidates: List<GenreRelationCandidate>
)
```

정적 장르 사전 JSON 예시:

```json
{
  "genre_key": "JAZZ_RAP",
  "display_name": "Jazz Rap",
  "aliases": ["Jazz rap", "Jazz Hip Hop"],
  "relations": [
    { "to": "HIP_HOP", "type": "DERIVED", "curated": true },
    { "to": "JAZZ", "type": "INFLUENCE", "curated": true },
    { "to": "NEO_SOUL", "type": "ADJACENT", "curated": true }
  ]
}
```

매핑 규칙:
- Gemini 응답의 `primary_genres`, `secondary_genres`, `microgenres`를 `AlbumGenreCandidate`로 파싱한다.
- 후보는 `GenreDictionaryEntry.aliases`를 통해 표준 `genreKey`로 정규화한다.
- `confidence >= 0.80`이며 사전에 존재하는 후보만 활성 장르로 반영한다.
- 화면에는 활성 장르와 검수된 관계로 직접 연결된 1-hop 주변 노드만 포함한다.
- `UNKNOWN`, 사전 미등록 후보, 직접 연결되지 않은 노드는 화면에 렌더링하지 않는다.
- 저장 횟수와 최근성은 노드/선 시각 강도만 보정한다. 신규 영향선을 생성하지 않는다.

Validation/Parsing:
- Gemini에서 받은 장르 후보 필드는 DTO로 받아 내부 정규화/검증을 거친 후 반영한다.
- 잘못되거나 비정상적인 장르 문자열은 무시한다.
- 주변 장르 검색 결과는 팝업 미리보기 후보로 먼저 표시하고, 사용자가 `지도에 반영`을 누를 때만 세션 지도에 추가한다.
- 주변 후보의 `relation_strength`는 `0.0..1.0` 범위로 보정한다.
- `Unknown`, 빈 장르명, 선택 장르 자기 자신, 중복 후보, `score < 0.35` 후보는 무시한다.


---

## 3. Gemini Album Research Contract

### 3.0 Genre Relation Discovery Contract

- **Trigger:** 사용자가 지도에서 장르 노드를 선택하고 `근처 장르 찾기`를 누를 때만 실행한다.
- **Runtime Repository:** `GeminiGenreRelationDiscoveryRepository`
- **Search Grounding:** `tools: [{ "google_search": {} }]`
- **Output Contract:**

```json
{
  "selected_genre": "Hyperpop",
  "nearby_genres": [
    {
      "genre": "Electropop",
      "relation_strength": 0.92,
      "relation_type": "influence",
      "evidence": "Search-grounded evidence"
    }
  ],
  "reliability_notes": []
}
```

표시 규칙:
- 팝업 목록 왼쪽에는 `genre`, 오른쪽에는 `relation_strength`를 변환한 `강함`, `보통`, `약함`을 표시한다.
- `지도에 반영` 전에는 노드/엣지를 추가하지 않는다.
- 반영된 관계는 현재 지도 세션에 유지하고, 선은 화살표 없이 굵기와 투명도로만 강도를 표현한다.

### 3.1 Request Contract

- **Method:** `POST`
- **Endpoint:** `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`
- **Runtime Repository:** `GeminiTrackMetadataRepository`
- **Runtime Model:** `gemini-3.1-flash-lite`
- **Search Grounding:** `tools: [{ "google_search": {} }]`를 활성화하여 실시간 웹 검색 결과를 근거로 사용한다.
- **Output Constraint:** `generationConfig.responseMimeType = "application/json"`을 사용하고, 응답 본문은 JSON-only로 제한한다. Gemini 래퍼 응답에서는 `candidates[0].content.parts[0].text` 안의 JSON을 추출한다.
- **Input Variables:** `artist_name`, `track_title`, `album_title`
- **Album-first Rule:** `artist_name + track_title`로 수록 앨범을 먼저 식별한 뒤, 장르/평점/평론은 식별된 앨범 기준으로만 반환한다. `album_title`은 감지된 경우 보조 힌트로만 사용한다.
- **Community Search Fallback:** 직접 평점 페이지를 우선 검색한다. 접근 불가 또는 검색 누락 시 Reddit과 HipHople 검색 결과를 우회 탐색 및 교차검증에 사용할 수 있다. 단일 커뮤니티 게시글의 숫자는 평점으로 채택하지 않는다. 여러 독립 검색 결과가 같은 앨범 단위 점수와 원출처를 일관되게 인용할 때만 간접 확인값으로 사용하고 `reliability_notes`에 남긴다.

```json
{
  "systemInstruction": {
    "parts": [
      {
        "text": "Identify the album that contains the given track. Return only album-based metadata as valid JSON. Do not invent unavailable ratings."
      }
    ]
  },
  "tools": [
    {
      "google_search": {}
    }
  ],
  "contents": [
    {
      "role": "user",
      "parts": [
        {
          "text": "Use the artist and title to identify the matching album, then analyze that album and return the JSON payload only.\nArtist: {{artist_name}}\nTitle: {{track_title}}\nAlbum: {{album_title}}\nRatings must be album-based: RYM, Pitchfork, Metacritic, and AOTY. If a source is unavailable, set its score to null and include the source name in missing_sources."
        }
      ]
    }
  ],
  "generationConfig": {
    "temperature": 0.2,
    "maxOutputTokens": 1024,
    "responseMimeType": "application/json"
  }
}
```

### 3.2 Response Contract

Gemini 응답은 아래 평탄화된 JSON을 우선 계약으로 한다.

```json
{
  "artist": "Artist Name",
  "title": "Track Title",
  "album": "Identified Album Title",
  "primary_genres": [
    { "name": "Hip Hop", "confidence": 0.96, "evidence_text": "Album-level genre evidence" }
  ],
  "secondary_genres": [
    { "name": "Jazz Rap", "confidence": 0.88, "evidence_text": "Album-level subgenre evidence" }
  ],
  "microgenres": [
    { "name": "Conscious Hip Hop", "confidence": 0.73, "evidence_text": "Optional microgenre evidence" }
  ],
  "genre_source": "LLM",
  "rym_rating": 4.12,
  "pitchfork_score": 8.4,
  "metacritic_score": 86,
  "aoty_score": 82,
  "critics_summary": "Album-level critical summary in Korean",
  "interview_summary": "Album-related interview summary in Korean or null",
  "listening_guide": "How to listen to the current track in the album context",
  "samples_used": ["Reliable sample information for the current track only"],
  "missing_sources": ["pitchfork"],
  "reliability_notes": ["Only include ratings found from recognizable album pages or review summaries."]
}
```

- `artist`: 감지된 아티스트 또는 Gemini가 정규화한 표기.
- `title`: 현재 재생 중인 곡명. 앨범 식별 입력으로 유지한다.
- `album`: `artist + title`로 식별한 수록 앨범명. 메타데이터의 중심 엔티티다.
- `primary_genres`, `secondary_genres`, `microgenres`: 앨범 기준 장르 후보 배열. 각 후보는 `name`, `confidence`, 선택적 `evidence_text`를 포함한다.
- `rym_rating`: 앨범 기준 RYM 점수. 확인 불가 시 `null`.
- `pitchfork_score`: 앨범 기준 Pitchfork 리뷰 점수. 확인 불가 시 `null`.
- `metacritic_score`: 앨범 기준 Metacritic metascore. 확인 불가 시 `null`.
- `aoty_score`: 앨범 기준 Album of the Year 점수. 확인 불가 시 `null`.
- `critics_summary`: 앨범 기준 주요 비평 요약.
- `interview_summary`: 앨범 또는 앨범 제작 맥락과 관련된 인터뷰 요약.
- `listening_guide`: 현재 곡을 식별된 앨범 맥락에서 듣는 포인트.
- `samples_used`: 현재 곡에 대해 신뢰 가능한 샘플 정보만 포함.
- `missing_sources`: 확인되지 않아 표시하지 않을 평점 출처 목록.
- `reliability_notes`: 앨범 식별/평점 출처 관련 주의사항.

### 3.3 Kotlin DTO Mapping Contract

```kotlin
@Serializable
data class GeminiAlbumResearchResponse(
    @SerialName("artist") val artist: String,
    @SerialName("title") val title: String,
    @SerialName("album") val album: String?,
    @SerialName("primary_genres") val primaryGenres: List<GenreCandidateDto>,
    @SerialName("secondary_genres") val secondaryGenres: List<GenreCandidateDto> = emptyList(),
    @SerialName("microgenres") val microgenres: List<GenreCandidateDto> = emptyList(),
    @SerialName("genre_source") val genreSource: String? = "LLM",
    @SerialName("rym_rating") val rymRating: Float? = null,
    @SerialName("pitchfork_score") val pitchforkScore: Float? = null,
    @SerialName("metacritic_score") val metacriticScore: Int? = null,
    @SerialName("aoty_score") val aotyScore: Int? = null,
    @SerialName("critics_summary") val criticsSummary: String,
    @SerialName("interview_summary") val interviewSummary: String? = null,
    @SerialName("listening_guide") val listeningGuide: String,
    @SerialName("samples_used") val samplesUsed: List<String> = emptyList(),
    @SerialName("missing_sources") val missingSources: List<String> = emptyList(),
    @SerialName("reliability_notes") val reliabilityNotes: List<String> = emptyList()
)

@Serializable
data class GenreCandidateDto(
    @SerialName("name") val name: String,
    @SerialName("confidence") val confidence: Float,
    @SerialName("evidence_text") val evidenceText: String? = null
)
```

### 3.4 Validation Rules

- `artist_name`, `track_title`는 필수이다.
- `album_title`은 비어 있어도 요청은 가능하지만, 응답의 `album`은 Gemini가 식별한 앨범명으로 채워야 한다.
- `artist + title`로 앨범 식별 신뢰도가 낮으면 `reliability_notes`에 사유를 남긴다.
- `primary_genres`, `critics_summary`, `listening_guide`는 최소 필수 필드로 간주한다.
- RYM/Pitchfork/Metacritic/AOTY는 앨범 기준으로만 파싱한다. 출처가 없으면 값을 만들지 않고 `null`로 유지한다.
- 앨범 분석 응답은 장르 간 영향선 또는 `adjacent_genres`를 생성하지 않는다.

---

## 4. Data Flow & Concurrency Design

1. **Event Emit:** `ActiveMediaMonitorService`가 새로운 곡을 감지하고 `SharedFlow<NowPlayingEvent>`로 발행한다.
2. **State Loading:** `DetailViewModel`은 UI 상태를 `Loading`으로 전환하고 스켈레톤(Shimmer) UI를 렌더링한다.
3. **Parallel Execution:**
   - `supervisorScope` 내부에서 메타데이터 수집과 가사 수집을 독립 코루틴으로 동시에 실행한다.
   - **Job A:** `GeminiTrackMetadataRepository.fetchTrackMetadata()` 호출. 내부적으로 `artist + title`로 앨범을 식별하고 앨범 기준 메타데이터를 반환한다.
   - **Job B:** `LyricsRepository.getRawLyrics()` 호출 (수백 ms 소요).
4. **Current Lyrics Scope:**
   - Job B가 성공하면 원문 가사를 즉시 `LyricsUiState.LyricsLoaded`로 표시한다.
   - 현재 범위에서는 가사를 Gemini에 전달하거나 번역 작업을 실행하지 않는다.
   - 번역이 필요해지면 별도 후속 파이프라인으로 추가하며, 원문 가사 조회와 앨범 메타데이터 표시를 막지 않아야 한다.
5. **Partial UI Update:**
   - Job A와 Job B는 서로를 기다리지 않는다.
   - Job A가 끝나면 앨범 메타데이터 영역(`MetadataUiState`)을 즉시 렌더링한다.
   - 가사 조회 진행 중에도 상단 앨범 평론/장르/평점 영역은 사용 가능해야 한다.
6. **Final Commit:** 최소 저장 조건을 만족하면 `ArchiveRepository.save()`를 호출해 로컬 DB에 저장한다.

### 4.1 저장 조건 및 중복 정책

- `artist`와 `title`은 필수값이다. 둘 중 하나라도 비어 있으면 정보 수집과 저장을 수행하지 않고 `NowPlayingUiState.InvalidTrack`을 표시한다.
- Notification Access 권한이 없는 경우 `NowPlayingScreen` 인라인 수동 입력 폼에서 `artist/title`을 직접 입력받아 동일 파이프라인을 실행한다.
- `trackId`는 정규화된 `artist + title` 문자열의 SHA-256 해시 앞 16바이트를 hex로 변환해 생성한다. 정규화는 trim, lowercase, 연속 공백 1칸 치환을 적용한다.
- `albumId`가 필요한 저장소에서는 정규화된 `artist + album` 문자열을 별도 키로 사용해 앨범 기준 메타데이터 캐싱에 활용한다.
- 메타데이터 API가 실패해도 `artist`, `title`, `timestamp`만으로 아카이브 저장이 가능하다. 이때 `album = null`, `primaryGenre = UNKNOWN`, `secondaryGenre = null`, `genreSource = UNKNOWN`, `criticsSummary = ""`, `interviewSummary = null`, `listeningGuide = ""`, `samplesUsed = emptyList()`, `rymRating = null`, `pitchforkScore = null`, `metacriticScore = null`, `aotyScore = null`, `missingSources = emptyList()`, `reliabilityNotes = emptyList()`로 저장한다.
- 가사 조회 실패는 저장 실패로 간주하지 않는다. `originalLyrics`는 nullable로 저장한다.
- 현재 구현 범위는 `lyrics.ovh` 원문 조회까지다. Gemini 번역 호출과 `translatedLyrics` 저장은 후속 작업으로 유지한다.
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
| 메타데이터 조회 실패 | 기본 곡 정보(artist/title)만 유지, 앨범 기준 정보는 숨김, 에러 배너 표시 |
| 가사 조회 실패 | 가사 카드 숨김 또는 "가사를 찾을 수 없음" 렌더링 |
| DB 저장 실패 | Snackbar로 재시도 액션 제공, 메모리 캐시 유지 |
| Gemini 429 quota 초과 | 재시도하지 않고 AI Studio 사용량/결제/요금제 확인 안내 표시 |

### 5.3 Retry 정책

- 네트워크 API는 최대 **3회** 시도한다.
- 재시도 간격은 지수 백오프(`500ms → 1000ms → 2000ms`)를 사용한다.
- HTTP **4xx**는 재시도하지 않고, **5xx** 및 타임아웃만 재시도 대상으로 본다.
- HTTP **429**는 quota/rate-limit 초과로 분리해 "Gemini API 사용량 한도 초과" 메시지를 표시한다.

---

## 6. Dependency Injection Structure

### 6.1 모듈 구성

- **`NetworkModule`:** `OkHttpClient`, `Retrofit`, API Service, `kotlinx.serialization.Json` 제공.
- **`ParserModule`:** `GeminiJsonParser` 제공.
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
- `GeminiJsonParser` JSON 추출, DTO 파싱, 필수 필드 검증 로직 검증
- `GenreMapper` 정규화 맵 검증 (동의어 장르 -> 표준 Enum)
- Gemini 장르 후보 배열 파싱 검증 (`primary_genres`, `secondary_genres`, `microgenres`, confidence)
- 사용자 주도 주변 장르 검색 검증 (`nearby_genres` 파싱, 강도 라벨, 팝업 반영 reducer)
- 수동 입력 폴백 시나리오 검증 (권한 없음 -> 인라인 입력 -> 저장 가능)
- `genreSource` 분기 검증 (`RYM`, `LLM`, `MANUAL`, `UNKNOWN`)
- 앨범 기준 평점 파싱 검증 (`rymRating`, `pitchforkScore`, `metacriticScore`, `aotyScore` nullable 처리)
- 요청 빌더 검증: `GeminiRequestBuilder`가 앨범 식별 지시, 출처별 평점 키, `missing_sources`, `responseMimeType = "application/json"`을 포함해야 한다.

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
- Compose UI Test: `GenreMapScreen` unknown 비노출, 1-hop 후보 노출, pan/zoom 상태 검증

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
- `SettingsScreen`의 API Key 입력 UI는 현재 런타임에서 사용하는 Gemini API Key만 표시한다. Perplexity 키 입력은 레거시 저장소 함수가 남아 있어도 설정 화면에 노출하지 않는다.
- Gemini API Key 저장은 `SharedPreferences.commit()`으로 즉시 반영 여부를 확인하고, 저장 직후 같은 저장소에서 다시 읽어 UI에 성공/실패 메시지를 표시한다.

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
        db.execSQL("ALTER TABLE archive_table ADD COLUMN genre_candidates_json TEXT NOT NULL DEFAULT '[]'")
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
| `DetailScreen` | 앨범 기준 평론, 장르, 출처별 평점, 원문 가사, 샘플 정보 |
| `ArchiveListScreen` | 저장된 곡 목록 조회, 검색, 장르 필터 |
| `ArchiveDetailScreen` | 저장된 단일 아카이브 상세 조회 |
| `GenreStatsScreen` | 장르별 청취 통계 시각화 |
| `SettingsScreen` | 권한 상태, Gemini API 설정, 앱 정보 |

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
- `LyricsUiState`: 원문 로딩, 원문 표시, 가사 없음
- `ArchiveUiState`: 저장 성공 여부, 저장 진행 여부, 에러

---

## 11. Genre Visualization Feature

### 11.1 기능 정의

- 저장된 아카이브 데이터를 기반으로 앨범 기준 장르별 감상 수를 집계한다.
- 기간 필터(전체 / 최근 30일 / 최근 90일)를 제공한다.
- 차트 항목 터치 시 해당 장르의 아카이브 목록으로 이동할 수 있다.
- 집계 기준은 식별된 앨범의 정규화된 `primaryGenre`이며 `secondaryGenre`는 상세 화면 설명 보조 정보로만 사용한다.

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
| 원문 가사 nullable 필드 | 부분 실패 허용 및 파이프라인 연속성 유지 |
| 번역 필드 예약 | 현재는 원문만 조회하고, 번역은 후속 기능으로 분리 |
| 앨범 장르와 주변 장르 탐색 분리 | 앨범 저장 시에는 대표 장르만 반영하고, 주변 관계는 사용자가 요청한 Gemini 검색 결과를 팝업 검토 후 세션 지도에 반영 |
