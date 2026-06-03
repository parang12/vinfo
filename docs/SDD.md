# Software Design Document (SDD)

**Project:** vinfo (Vinyl + Information)  
**Architecture:** Clean Architecture + MVVM + Unidirectional Data Flow (UDF)

---

## 1. System Architecture Overview

vinfo는 유지보수성과 외부 API 종속성 격리를 위해 Clean Architecture를 사용한다.

### 1.1 Layer Responsibilities

- Presentation Layer: Jetpack Compose UI, ViewModel, UI State
- Domain Layer: 핵심 비즈니스 규칙, 앨범 기준 메타데이터 UseCase, Repository Interface
- Data Layer: 외부 API, 앨범 식별/평점 JSON 파싱, Room, DTO/Entity 매핑

---

## 2. Related Documents

세부 데이터 계약은 [docs/DATA.md](DATA.md)에 분리했다.

- [docs/DATA.md](DATA.md): Gemini/Lyrics(및 Perplexity 레거시) 요청 계약, DTO, 저장 구조, 정규화, 오류 처리, 마이그레이션, 테스트
- [docs/SRS.md](SRS.md): 요구사항 정의
- [docs/UI.md](UI.md): UI/UX 표현 정의

---

## 3. Core Responsibilities

- Presentation: 화면 렌더링, 사용자 입력 처리, 상태 구독 및 반영
- Domain: 비즈니스 규칙, 유스케이스 조합, 저장/조회 정책
- Data: 외부 API 호출, `artist + title` 기반 앨범 식별, 앨범 기준 JSON 파싱, DTO 변환, Room 저장/조회, 캐시 동기화

---

## 4. Design Notes

- Data 관련 세부 설계는 [docs/DATA.md](DATA.md)로 이동했다.
- SDD는 상위 구조와 책임 분리에 집중한다.
- 새로운 API 계약이나 저장 정책이 생기면 우선 DATA 문서를 갱신한다.
- 음악 정보 분석은 현재 재생 곡을 입력으로 받지만, Gemini가 산출하는 장르/평론/RYM/Pitchfork/Metacritic/AOTY 정보는 식별된 앨범 기준으로만 취급한다.

## 5. Taste Exploration Map (Genre Adjacency Map) 설계

### 5.1 개요
`Taste Exploration Map`은 사용자의 저장된 아카이브를 입력으로 하여 앨범 기준 장르 노드와 장르 간 인접성을 그래프로 표현하는 UI/도메인 기능이다. 목적은 '추천'이 아닌 '탐험'을 돕는 시각화다. 앨범 저장 단계에서는 대표 장르만 활성 노드로 반영하고, 주변 장르 탐색은 사용자가 노드를 선택해 `근처 장르 찾기`를 눌렀을 때만 실행한다.

### 5.2 아키텍처 위치
- Presentation: `GenreMapScreen` (Compose) — 노드 레이아웃, 애니메이션, 터치/툴팁, 주변 장르 팝업 처리
- Presentation State: `GenreMapViewModel`, `GenreMapDiscoveryState` — 검색 로딩, 팝업 후보, 세션 반영 결과 관리
- Domain: `DiscoverNearbyGenresUseCase` — Gemini 주변 장르 후보 검증, 중복 제거, 강도 임계값 필터, 최대 6개 제한
- Data: `GeminiGenreRelationDiscoveryRepository` — 선택 장르 기반 Search grounding 요청과 JSON 파싱

### 5.3 데이터 모델 (요약)
- `AlbumGenreCandidate` (name, confidence, tier: Primary|Secondary|Micro)
- `GenreRelationCandidate` (genreName, score, relationType, evidence)
- `RelationStrength` (STRONG|MEDIUM|WEAK)
- `ConfirmedGenreDiscovery` (sourceGenre, candidates)

세부 스키마와 필드명은 `docs/DATA.md`의 `taste_exploration.*` 항목과 일치시킨다.

### 5.4 장르 정규화 및 화면 그래프 생성(도메인 로직)
1. 입력: 사용자가 선택한 지도 노드의 장르명.
2. 검색: `GeminiGenreRelationRequestBuilder`가 Search grounding 요청으로 주변 장르 후보와 `relation_strength`를 요청한다.
3. 파싱: `GeminiGenreRelationJsonParser`가 `nearby_genres` 배열을 `GenreRelationCandidate`로 변환한다.
4. 검증: `DiscoverNearbyGenresUseCase`가 `Unknown`, 빈 값, 자기 자신, 중복, `score < 0.35` 후보를 제외하고 최대 6개만 유지한다.
5. 팝업: 후보는 바로 지도에 들어가지 않고 팝업 리스트로 표시된다.
6. 반영: 사용자가 `지도에 반영`을 누르면 `ConfirmedGenreDiscovery`로 세션 상태에 저장되고 지도에 주변 노드와 연결선을 추가한다.
7. 시각 강도: `relation_strength`는 선 굵기와 투명도, 팝업의 `강함/보통/약함` 라벨로 표현한다.

### 5.5 Presentation 요구사항
- 노드 유형: `Activated`(앨범 저장으로 확인됨), `Adjacent`(사용자 검색으로 발견되어 반영됨)
- `Unknown`, 사전에 없는 장르, 2-hop 이상 떨어진 장르, `Locked` 노드는 기본 화면에 렌더링하지 않는다.
- 상호작용: 노드 탭→장르 기반 아카이브 목록으로 네비게이션, 롱프레스→노드 카드(설명/저장수/최근 진입) 표시
- 애니메이션: 새 활성 장르가 추가될 때 간단한 확장 토스트 및 배지(성능 저하 최소화)
- 접근성: 색상 외에 심볼/테두리로 상태 구분

### 5.6 Use Cases (도메인 유스케이스)
- `DiscoverNearbyGenresUseCase`: 선택 장르 주변 후보를 검증하고 정렬한다.

### 5.7 API/계약
- Repository: `GenreRelationDiscoveryRepository`
	- `discoverNearbyGenres(selectedGenre, apiKey): AppResult<List<GenreRelationCandidate>>`

### 5.8 비기능 요구 및 제약
- 계산은 배경 스레드(코루틴)에서 실행해야 하며, UI는 비동기 옵저버로 변경을 구독한다.
- 검색 결과는 세션 상태로만 유지한다. 영속화는 후속 작업에서 관계 캐시/검수 목록과 함께 설계한다.
- Explainability: 각 엣지는 Gemini 검색 결과의 `evidence`를 근거 팝업에 노출한다.
- AI가 제안한 후보는 사용자 반영 전까지 런타임 그래프를 변경하지 않는다.

### 5.9 테스트 전략
- 유닛: 주변 후보 필터, 연관성 강도 라벨, Gemini 요청 빌더, JSON 파서, 팝업 reducer 테스트
- 통합: 선택 장르 -> Gemini 응답 -> 후보 필터 -> 팝업 후보 -> 지도 반영 흐름 검증
- UI: Compose 테스트로 팝업 목록, `지도에 반영`, pan/zoom, unknown 비노출 검증

---

이 섹션의 목표 계약은 `docs/DATA.md`와 동기화되어 있다. 현재 Compose 지도는 사용자 주도 1-hop 탐색과 세션 반영을 지원하며, 관계 영속화와 검수 큐는 후속 구현 항목이다.
