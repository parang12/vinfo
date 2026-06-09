# Vinfo UI 리디자인 구현 노트

작성일: 2026-06-02

이 문서는 [DESIGN.md](DESIGN.md)와 [UI_v2.md](UI_v2.md)를 기준으로 진행한 Compose UI 리디자인 결과를 구현 관점에서 정리한다.

## 범위

- [MainActivity.kt](../app/src/main/java/com/example/vinfo/MainActivity.kt)
- [CommonComponents.kt](../app/src/main/java/com/example/vinfo/ui/component/CommonComponents.kt)
- [NowPlayingScreen.kt](../app/src/main/java/com/example/vinfo/ui/nowplaying/NowPlayingScreen.kt)
- [DetailScreen.kt](../app/src/main/java/com/example/vinfo/ui/detail/DetailScreen.kt)
- [ArchiveListScreen.kt](../app/src/main/java/com/example/vinfo/ui/archive/ArchiveListScreen.kt)
- [GenreStatsScreen.kt](../app/src/main/java/com/example/vinfo/ui/stats/GenreStatsScreen.kt)
- [GenreMapScreen.kt](../app/src/main/java/com/example/vinfo/ui/stats/GenreMapScreen.kt)
- [SettingsScreen.kt](../app/src/main/java/com/example/vinfo/ui/settings/SettingsScreen.kt)
- [Color.kt](../app/src/main/java/com/example/vinfo/ui/theme/Color.kt)
- [Theme.kt](../app/src/main/java/com/example/vinfo/ui/theme/Theme.kt)
- [Type.kt](../app/src/main/java/com/example/vinfo/ui/theme/Type.kt)

## 핵심 변경 사항

### 1. 공통 디자인 토큰 정리

- 배경, 표면, 텍스트, 경계선 색상을 `VinfoPrimary`, `VinfoSurface`, `VinfoOnSurface` 계열로 정리했다.
- 타이포그래피를 `displayLarge`, `titleLarge`, `titleMedium`, `bodyLarge`, `bodyMedium`, `labelMedium`, `labelSmall` 중심으로 재정의했다.
- 카드, 칩, 섹션 헤더를 화면 공통으로 재사용하도록 정리했다.

### 2. 내비게이션 구조 정리

- 하단 탭은 `홈`, `보관함`, `통계` 3개로 통일했다.
- 설정과 상세 화면은 탭 셸 바깥의 작업형 화면으로 동작하도록 분리했다.
- `NavHost` 기준의 단일 진입점으로 정리해 화면 전환 구조를 단순화했다.

### 3. 화면별 재구성

#### Home / Now Playing

- 권한 안내 배너를 상단에 배치했다.
- 큰 히어로 카드, `Catch Now` 버튼, 최근 감상 리스트 순서로 재배치했다.
- 최근 감상 항목은 소스, 시각, 더보기 메뉴까지 포함하도록 확장했다.

#### Track Detail

- 앨범 히어로, 앨범 기준 장르 칩, 출처별 점수 영역, 비평 요약, 감상 가이드, 가사 탭 순서로 재구성했다.
- `artist + title`은 앨범 식별 입력으로 사용하고, 장르/평론/평점은 식별된 앨범 기준으로 표시한다.
- 출처별 점수 영역은 `rymRating`, `pitchforkScore`, `metacriticScore`, `aotyScore` 중 실제 값이 있는 항목만 표시한다.
- `albumArtUrl`이 전달되면 MediaSession/알림에서 추출한 앨범 커버를 히어로 이미지로 표시하고, 없으면 기존 바이닐형 fallback 아트를 표시한다.
- `lyrics.ovh`에서 현재 재생 곡의 원문 가사를 조회하여 상세 화면에 표시한다.
- 가사 번역은 아직 호출하지 않는다. 번역 UI와 Gemini 번역 파이프라인은 후속 작업으로 유지한다.
- 하단에는 AI 면책 문구를 유지했다.

#### Archive List

- 검색 입력창을 상단에 크게 두고, 필터 칩을 바로 아래에 배치했다.
- 기록 카드는 제목, 아티스트, 날짜, 장르 칩으로 압축해 스캔성을 높였다.

#### Genre Stats

- 기간 선택 세그먼트를 `30d`, `90d`, `1y`로 정리했다.
- 도넛형 요약, KPI 카드, 막대 차트 카드가 한 화면 안에서 이어지도록 구성했다.

#### Taste Map

- 홈의 `맵 열기` 액션에서 전체 화면 장르 지도로 이동한다.
- 캔버스는 드래그 이동, 핀치 줌, 확대/축소, 중앙 복귀를 지원한다.
- 현재 구현은 아카이브의 정규화 장르, `GenreDictionary`, `CuratedGenreRelations`, `GetVisibleGenreFlowUseCase`를 사용하며, 활성 노드와 직접 연결된 1-hop 주변 후보만 렌더링한다.
- 알 수 없는 장르, 사전 미등록 장르, 직접 연결되지 않은 노드는 화면 모델에서 제외한다.
- `GenreDictionary`는 Kotlin 리소스로 버전 관리하며, [GENRE_TAXONOMY.md](GENRE_TAXONOMY.md)의 Root Genre 정책에 따라 alias 매칭과 사전 미등록 후보 제외를 수행한다.
- 신생 장르는 Root Genre 검증을 통과한 경우에만 `EMERGING`/`NEEDS_REVIEW` 상태로 모델링하며, 기본 지도/통계에는 검증된 장르와 직접 연결 후보만 반영한다.
- Gemini는 앨범 장르 후보를 제안하지만, 장르 간 영향선은 생성하지 않는다.

#### Settings

- 권한 상태, API 설정, 테마 설정, 데이터 관리 순서로 분리했다.
- API 설정은 현재 런타임에서 사용하는 Gemini API Key만 입력받는다.
- Gemini API Key 저장 후 즉시 다시 읽어 저장 확인 메시지를 표시한다.
- 테마 설정은 라이트/다크/시스템 기본값을 SharedPreferences에 저장하고 앱 루트의 `VinfoTheme`에 즉시 반영한다.
- 권한 상태에는 실제 동작과 연결되지 않은 알림 토글을 두지 않고, 음악 알림 감지 목적만 안내한다.
- 위험 동작은 빨간 텍스트로 구분했다.

#### API Cache

- `CachedTrackMetadataRepository`는 Gemini 호출 전에 Room Archive의 기존 앨범 row를 조회한다.
- `currentTrack.album`이 있으면 `artist + album_title/album`을 우선 사용하고, 없으면 `artist + title` 기반 `trackId` 조회로 fallback한다.
- Gemini 응답 성공 시 즉시 Archive row로 저장하여, 사용자가 보관함 저장 버튼을 누르지 않아도 다음 Catch Now에서 같은 앨범 분석을 재사용할 수 있다.
- `CachedLyricsRepository`는 `lyrics_cache` 테이블에서 `artist + track_title` 기준 원문 가사를 먼저 찾고, 없을 때만 `lyrics.ovh`를 호출한다.

## 구현 메모

- `VinfoCard`는 얕은 그림자, 옅은 경계선, 24dp radius를 기준으로 통일했다.
- `GenreChip`은 선택 상태와 비선택 상태를 시각적으로 분리했다.
- `SectionHeader`를 도입해 화면마다 제목과 보조 액션의 위치를 일관되게 맞췄다.
- 설정, 상세, 통계는 모두 스크롤 기반 카드 레이아웃으로 정리했다.
- `GeminiRequestBuilder`는 `responseMimeType = "application/json"`을 포함하고, 아티스트/곡명으로 앨범을 식별한 뒤 앨범 기준 JSON만 반환하도록 요청한다.
- `GeminiJsonParser`는 Gemini 래퍼 응답 또는 원시 JSON에서 첫 JSON 객체를 추출하며, `null` 평점/문자열을 그대로 nullable 필드로 보존한다.

## 검증

- `./gradlew assembleDebug` 성공
- 화면 관련 Kotlin 파일의 컴파일 오류 없음 확인
- `./gradlew compileDebugKotlin` 성공
- `./gradlew testDebugUnitTest` 성공

## 참고

이 문서는 구현 요약용이다. 상세한 디자인 원칙과 화면 정의는 [DESIGN.md](DESIGN.md)와 [UI_v2.md](UI_v2.md)를 기준으로 유지한다.
