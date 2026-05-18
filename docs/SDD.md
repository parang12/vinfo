# Software Design Document (SDD)

**Project:** vinfo (Vinyl + Information)  
**Architecture:** Clean Architecture + MVVM + Unidirectional Data Flow (UDF)

---

## 1. System Architecture Overview

vinfo는 유지보수성과 외부 API 종속성 격리를 위해 Clean Architecture를 사용한다.

### 1.1 Layer Responsibilities

- Presentation Layer: Jetpack Compose UI, ViewModel, UI State
- Domain Layer: 핵심 비즈니스 규칙, UseCase, Repository Interface
- Data Layer: 외부 API, Room, DTO/Entity 매핑

---

## 2. Related Documents

세부 데이터 계약은 [docs/DATA.md](DATA.md)에 분리했다.

- [docs/DATA.md](DATA.md): Perplexity/Lyrics/Gemini 요청 계약, DTO, 저장 구조, 정규화, 오류 처리, 마이그레이션, 테스트
- [docs/SRS.md](SRS.md): 요구사항 정의
- [docs/UI.md](UI.md): UI/UX 표현 정의

---

## 3. Core Responsibilities

- Presentation: 화면 렌더링, 사용자 입력 처리, 상태 구독 및 반영
- Domain: 비즈니스 규칙, 유스케이스 조합, 저장/조회 정책
- Data: 외부 API 호출, JSON 파싱, DTO 변환, Room 저장/조회, 캐시 동기화

---

## 4. Design Notes

- Data 관련 세부 설계는 [docs/DATA.md](DATA.md)로 이동했다.
- SDD는 상위 구조와 책임 분리에 집중한다.
- 새로운 API 계약이나 저장 정책이 생기면 우선 DATA 문서를 갱신한다.

## 5. Taste Exploration Map (Genre Adjacency Map) 설계

### 5.1 개요
`Taste Exploration Map`은 사용자의 저장된 아카이브를 입력으로 하여 장르 노드와 장르 간 인접성을 그래프로 표현하는 UI/도메인 기능이다. 목적은 '추천'이 아닌 '탐험'을 돕는 시각화이며, 사용자의 장기 기록(저장 빈도, 최근성, 이동 패턴 등)을 반영하여 노드 활성화·우선순위·잠금 해제 로직을 결정한다.

### 5.2 아키텍처 위치
- Presentation: `GenreMapScreen` (Compose) — 노드 레이아웃, 애니메이션, 터치/툴팁 처리
- Domain: `ComputeGenreAdjacencyUseCase`, `UpdateExplorationStateUseCase` — 가중치 계산, 상태 전이
- Data: Room 엔터티(`GenreNodeEntity`, `AdjacencyEntity`, `ExplorationState`), `GenreMapper` 정규화 결과 사용

### 5.3 데이터 모델 (요약)
- `GenreNode` (id, name, normalizedKey, activated: Boolean, lastActivatedAt: Timestamp, saveCount: Int)
- `Adjacency` (fromGenreKey, toGenreKey, weight: Float, unlocked: Boolean)
- `ExplorationState` (userId, version, diversityWeight: Float, lastUpdated)

세부 스키마와 필드명은 `docs/DATA.md`의 `taste_exploration.*` 항목과 일치시킨다.

### 5.4 인접성 계산(도메인 로직)
1. 입력: 저장된 아카이브 목록(각 항목에 primary/secondary 장르), 글로벌 장르 맵(정규화 매핑), 과거 탐험 이력
2. 기본 가중치 산출: `weight = base + log(1 + saveCount) + recencyFactor - explorationPenalty`
	 - `base`: 장르 간 기본 유사도(정적 맵에서 유래)
	 - `recencyFactor`: 최근 저장 시점에 따른 가중치 보정
	 - `explorationPenalty`: 이미 많이 탐험된 경로에 대한 감소 항목(다양성 확보)
3. 잠금/해제 결정: 가중치 임계값 또는 특정 규칙(예: adjacent_genres 리스트 포함 여부)으로 `unlocked` 설정
4. 우선순위 업데이트: 정렬 가능한 우선순위 값 생성(탐험 UI에서 시각적 강조에 사용)

### 5.5 Presentation 요구사항
- 노드 유형: `Activated`(이미 저장됨), `Adjacent`(연결 가능), `Locked`(미탐험)
- 상호작용: 노드 탭→장르 기반 아카이브 목록으로 네비게이션, 롱프레스→노드 카드(설명/저장수/최근 진입) 표시
- 애니메이션: 노드 활성화/잠금 해제 시 간단한 확장 토스트 및 배지(성능 저하 최소화)
- 접근성: 색상 외에 심볼/테두리로 상태 구분

### 5.6 Use Cases (도메인 유스케이스)
- `ComputeGenreAdjacencyUseCase`: 저장 이벤트 또는 주기적 배치에서 인접성 테이블 갱신
- `ActivateGenreUseCase`: 특정 곡 저장 시 해당 장르 노드 활성화 및 인접 노드 우선순위 재계산
- `UnlockAdjacentUseCase`: 사용자 행동 또는 가중치 임계치를 만족할 때 잠금 해제

### 5.7 API/계약
- Repository: `GenreExplorationRepository`
	- `getExplorationState(userId): ExplorationState`
	- `computeAndPersistAdjacency(userId, archives): List<Adjacency>`
	- `activateNode(userId, genreKey): GenreNode`

### 5.8 비기능 요구 및 제약
- 계산은 배경 스레드(코루틴)에서 실행해야 하며, UI는 비동기 옵저버로 변경을 구독한다.
- 맵 데이터는 증분 업데이트(Delta)를 저장하여 불필요한 전체 재계산을 피한다.
- Explainability: 각 엣지의 근거(예: 저장 횟수, 공통 아티스트, LLM 기반 유사도)를 `Adjacency` 메타로 보관하여 툴팁에 노출할 수 있어야 한다.

### 5.9 테스트 전략
- 유닛: 가중치 계산 함수에 대해 다양한 입력(빈 이력, 고빈도 이력, 최근성 편향) 테스트
- 통합: 샘플 아카이브 데이터를 사용해 `computeAndPersistAdjacency`가 예상된 노드/엣지 집합을 생성하는지 검증
- UI: Compose 테스트로 노드 활성화/잠금 해제 플로우와 네비게이션 동작 검증

---

이 섹션은 SDD의 확장 설계로, 구현 전 `docs/DATA.md`의 스키마 부분과 동기화가 필요합니다.

