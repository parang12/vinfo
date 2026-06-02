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
`Taste Exploration Map`은 사용자의 저장된 아카이브를 입력으로 하여 앨범 기준 장르 노드와 장르 간 인접성을 그래프로 표현하는 UI/도메인 기능이다. 목적은 '추천'이 아닌 '탐험'을 돕는 시각화다. Gemini는 앨범의 장르 후보만 추출하고, 장르 간 영향선은 사람이 검수한 정적 `GenreDictionary`만 관리한다.

### 5.2 아키텍처 위치
- Presentation: `GenreMapScreen` (Compose) — 노드 레이아웃, 애니메이션, 터치/툴팁 처리
- Domain: `NormalizeAlbumGenreCandidatesUseCase`, `GetVisibleGenreFlowUseCase` — 후보 검증, 사전 조회, 1-hop 화면 모델 생성
- Data: Gemini 앨범 장르 후보, `GenreMapper`, 정적 `GenreDictionary`, Room의 앨범 저장 기록 사용

### 5.3 데이터 모델 (요약)
- `AlbumGenreCandidate` (name, confidence, tier: Primary|Secondary|Micro)
- `GenreDictionaryEntry` (genreKey, displayName, aliases, relations)
- `GenreRelation` (fromGenreKey, toGenreKey, relationType, curated: Boolean)
- `VisibleGenreFlow` (activatedNodes, adjacentNodes, visibleRelations)

세부 스키마와 필드명은 `docs/DATA.md`의 `taste_exploration.*` 항목과 일치시킨다.

### 5.4 장르 정규화 및 화면 그래프 생성(도메인 로직)
1. 입력: 저장된 앨범의 AI 장르 후보 목록, 후보별 신뢰도, 정적 `GenreDictionary`.
2. 정규화: alias를 표준 `genreKey`로 매핑한다. 사전에 없는 후보는 저장 후보 로그에는 남길 수 있으나 그래프에는 삽입하지 않는다.
3. 활성화: MVP 기본 임계값 `confidence >= 0.80`을 통과한 Primary/Secondary 후보만 활성 장르로 반영한다. Microgenre는 사전에 존재하고 임계값을 통과할 때만 반영한다.
4. 인접 조회: 활성 장르마다 검수된 정적 관계를 조회한다. AI가 반환한 관계나 자유 텍스트 관계를 사용하지 않는다.
5. 화면 모델: 활성 장르와 직접 연결된 1-hop 주변 후보만 `VisibleGenreFlow`에 포함한다. 나머지 미탐험 노드는 화면 모델에서 제외한다.
6. 시각 강도: 저장 횟수와 최근성은 검수된 선의 굵기와 정렬 우선순위만 보정하며 신규 선을 만들지 않는다.

### 5.5 Presentation 요구사항
- 노드 유형: `Activated`(이미 저장됨), `Adjacent`(활성 장르와 직접 연결된 1-hop 후보)
- `Unknown`, 사전에 없는 장르, 2-hop 이상 떨어진 장르, `Locked` 노드는 기본 화면에 렌더링하지 않는다.
- 상호작용: 노드 탭→장르 기반 아카이브 목록으로 네비게이션, 롱프레스→노드 카드(설명/저장수/최근 진입) 표시
- 애니메이션: 새 활성 장르가 추가될 때 간단한 확장 토스트 및 배지(성능 저하 최소화)
- 접근성: 색상 외에 심볼/테두리로 상태 구분

### 5.6 Use Cases (도메인 유스케이스)
- `NormalizeAlbumGenreCandidatesUseCase`: Gemini 장르 후보를 정적 사전의 표준 키로 검증 및 정규화
- `ActivateAlbumGenresUseCase`: 저장된 앨범의 검증된 후보를 활성 장르 집합에 반영
- `GetVisibleGenreFlowUseCase`: 활성 장르와 정적 사전을 입력받아 1-hop 화면 그래프 생성

### 5.7 API/계약
- Repository: `GenreExplorationRepository`
	- `getGenreDictionary(): GenreDictionary`
	- `normalizeCandidates(candidates): List<NormalizedGenreCandidate>`
	- `getVisibleGenreFlow(activatedGenreKeys): VisibleGenreFlow`

### 5.8 비기능 요구 및 제약
- 계산은 배경 스레드(코루틴)에서 실행해야 하며, UI는 비동기 옵저버로 변경을 구독한다.
- 장르 사전은 버전 관리 가능한 JSON 또는 Kotlin 정적 리소스로 관리한다.
- Explainability: 각 엣지는 `GenreDictionary`에 등록된 관계 유형과 검수 여부를 근거로 노출한다. 저장 횟수는 시각 강도 설명에만 사용한다.
- AI가 제안한 사전 미등록 후보는 운영 로그 또는 향후 관리자 검수 목록에 보관할 수 있으나 런타임 그래프를 자동 변경하지 않는다.

### 5.9 테스트 전략
- 유닛: alias 정규화, 신뢰도 임계값, 사전 미등록 후보 제외, 1-hop 제한 테스트
- 통합: 샘플 앨범 후보와 정적 장르 사전을 사용해 예상된 활성 노드/주변 후보/검수된 엣지만 생성되는지 검증
- UI: Compose 테스트로 미탐험 노드 비노출, 새 활성 장르 반영, pan/zoom, 네비게이션 동작 검증

---

이 섹션의 목표 계약은 `docs/DATA.md`와 동기화되어 있다. 현재 Compose 지도는 1-hop 표시 규칙을 적용하며, Gemini 후보 배열 파싱과 버전 관리형 `GenreDictionary` 리소스 분리는 후속 구현 항목이다.
