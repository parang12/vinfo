# Vinfo UI 리디자인 구현 노트

작성일: 2026-05-08

이 문서는 [DESIGN.md](DESIGN.md)와 [UI_v2.md](UI_v2.md)를 기준으로 진행한 Compose UI 리디자인 결과를 구현 관점에서 정리한다.

## 범위

- [MainActivity.kt](../app/src/main/java/com/example/vinfo/MainActivity.kt)
- [CommonComponents.kt](../app/src/main/java/com/example/vinfo/ui/component/CommonComponents.kt)
- [NowPlayingScreen.kt](../app/src/main/java/com/example/vinfo/ui/nowplaying/NowPlayingScreen.kt)
- [DetailScreen.kt](../app/src/main/java/com/example/vinfo/ui/detail/DetailScreen.kt)
- [ArchiveListScreen.kt](../app/src/main/java/com/example/vinfo/ui/archive/ArchiveListScreen.kt)
- [GenreStatsScreen.kt](../app/src/main/java/com/example/vinfo/ui/stats/GenreStatsScreen.kt)
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

- 앨범 히어로, 장르 칩, 3열 점수 영역, 비평 요약, 감상 가이드, 가사 탭 순서로 재구성했다.
- 원문 / 번역 가사를 탭 전환 방식으로 분리했다.
- 하단에는 AI 면책 문구를 유지했다.

#### Archive List

- 검색 입력창을 상단에 크게 두고, 필터 칩을 바로 아래에 배치했다.
- 기록 카드는 제목, 아티스트, 날짜, 장르 칩으로 압축해 스캔성을 높였다.

#### Genre Stats

- 기간 선택 세그먼트를 `30d`, `90d`, `1y`로 정리했다.
- 도넛형 요약, KPI 카드, 막대 차트 카드가 한 화면 안에서 이어지도록 구성했다.

#### Settings

- 권한 상태, API 설정, 테마 설정, 데이터 관리 순서로 분리했다.
- API Key 입력은 카드 내부 폼으로 유지하고, 위험 동작은 빨간 텍스트로 구분했다.

## 구현 메모

- `VinfoCard`는 얕은 그림자, 옅은 경계선, 24dp radius를 기준으로 통일했다.
- `GenreChip`은 선택 상태와 비선택 상태를 시각적으로 분리했다.
- `SectionHeader`를 도입해 화면마다 제목과 보조 액션의 위치를 일관되게 맞췄다.
- 설정, 상세, 통계는 모두 스크롤 기반 카드 레이아웃으로 정리했다.

## 검증

- `./gradlew assembleDebug` 성공
- 화면 관련 Kotlin 파일의 컴파일 오류 없음 확인

## 참고

이 문서는 구현 요약용이다. 상세한 디자인 원칙과 화면 정의는 [DESIGN.md](DESIGN.md)와 [UI_v2.md](UI_v2.md)를 기준으로 유지한다.