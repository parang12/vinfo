# UI Design Document v3 (UI_v3)

Project: vinfo (Vinyl + Information)
Target Platform: Android (API 26+)
UI Framework: Jetpack Compose + Material 3
Document Status: Unified Source of Truth
Updated: 2026-05-14

---

## 1. Purpose

이 문서는 기존 UI 관련 문서들을 하나로 통합한 v3 기준 문서다.

통합 출처:
- UI_v2.md
- DESIGN.md
- UI_IMPLEMENTATION_NOTES.md
- UI_IMPROVEMENTS_V1.1.md
- UI_IMPROVEMENTS_V1.2.md
- vinfo_SRS_v2.md (탐험 지도 요구사항)

핵심 방향:
- 추천 중심 UI가 아니라 취향 탐험 중심 UI
- 시각 스타일은 Quietly Premium + Apple Blue
- 구현 가능한 Compose 구조를 우선으로 명세

---

## 2. Product Framing

vinfo UI는 "다음 곡 추천"보다 "취향의 연결성과 확장 경로 탐험"을 보여주는 데 집중한다.

핵심 문장:
- vinfo는 음악 추천 앱이 아니라, 장기 청취 기록 기반의 취향 인접 지도 시스템이다.

UX 원칙:
- 추천 문구보다 탐험 문구 사용: 연결, 확장, 탐험, 해금
- 현재 취향 중심 노드 + 인접 노드 + 미탐험 노드 구분
- 저장 행동이 곧 지도 확장으로 연결되도록 피드백 제공

---

## 3. Information Architecture

```text
Root
├── Home (Now Playing)
│   └── Track Detail
├── Archive
│   └── Archive Detail
├── Insight Hub
│   ├── Taste Map (신규 핵심)
│   └── Genre Stats

## Genre Map (Taste Exploration Map) UI 상세

### 목적
사용자 저장 이력을 기반으로 장르를 노드-엣지 그래프로 표현하여 '탐험' 경험을 제공한다. 추천 대신 연결·확장 어휘를 사용한다.

### 노드 유형
- Activated: 사용자가 저장하여 활성화된 장르(시각적 강조)
- Adjacent: 활성 장르와 연결 가능한 장르(탐험 후보)
- Locked: 아직 잠금 상태인 미탐험 영역

### 상호작용
- 탭: 해당 장르의 아카이브 목록으로 이동
- 롱프레스트/스와이프: 노드 카드(설명, 저장 수, 최근 진입, 근거) 표시
- 엣지 탭: 연결 근거 팝업(why 연결되었는지 요약)

### 시각화/애니메이션
- 활성화/잠금 해제 시 간단한 확장 + 배지 토스트(성능 고려, 낮은 해상도에서는 애니메이션 축소)
- 색상 외 표시(심볼/테두리/패턴)로 접근성 확보

### 툴팁 및 설명
- 각 엣지/노드의 근거(evidence)는 `Adjacency.evidenceJson`을 기반으로 툴팁에 표시(예: 저장횟수, LLM 유사도)

### 접근성
- 색약 모드, 키보드 포커스, 콘텐츠 설명(AccessibilityLabel) 제공

### 성능/비동기
- 맵 데이터는 백그라운드에서 계산되어 Flow로 UI에 발행
- 증분(Delta) 업데이트로 전체 레이아웃 재계산 최소화

└── Settings
```

Bottom Navigation:
- Home
- Archive
- Insight

설명:
- 기존 Stats 탭을 Insight Hub로 확장한다.
- Insight Hub 내부에서 Segmented Control로 Taste Map / Genre Stats를 전환한다.

---

## 4. Screen Catalog

| Screen | 목적 | 핵심 요소 |
| --- | --- | --- |
| Now Playing | 현재 재생 감지 및 분석 시작 | Hero 카드, Catch Now CTA, 최근 감상 |
| Track Detail | AI 분석 상세 확인 및 저장 | 장르/점수/비평/가사/면책 + 저장 버튼 |
| Archive List | 기록 검색/필터/정리 | 검색, 칩 필터, 다중 선택 삭제 |
| Archive Detail | 저장된 상세 재확인 | Track Detail 레이아웃 재사용 |
| Taste Map (New) | 취향 인접 지도 탐험 | 노드 그래프, 연결선, 해금 상태 |
| Genre Stats | 장르 비율/변화 분석 | 도넛, KPI, 기간별 차트 |
| Settings | 권한/키/테마/데이터 관리 | 카드형 섹션, 위험 액션 구분 |

---

## 5. Navigation and Layout Rules

### 5.1 Top Actions
- 긴 앱바보다 화면별 원형 플로팅 버튼 우선
- 홈: 우측 설정 버튼 + 상단 1/3 타이틀
- 상세: 좌측 뒤로가기 + 우측 설정
- 보관함/통계/지도: 우측 설정
- 설정: 좌측 뒤로가기

### 5.2 Floating Bottom Navigation
- 캡슐형 흰색 컨테이너 + 얕은 그림자
- 선택 탭: 연파랑 pill + 진한 파랑 아이콘/텍스트
- 미선택 탭: 회색 아이콘/텍스트
- 콘텐츠 하단 여백 기본 110dp, 상세 160dp

### 5.3 Safe Area
- statusBarsPadding 필수
- 펀치홀/제스처 영역과 겹침 금지

---

## 6. Taste Map Screen (New Core Feature)

### 6.1 Goal
사용자가 자신의 장기 청취 기록을 기반으로 취향의 인접 장르와 확장 가능성을 시각적으로 탐험하게 한다.

### 6.2 Visual Model
노드 유형:
- Activated Node: 이미 저장/강화된 장르
- Adjacent Node: 연결 가능 장르
- Unexplored Node: 아직 미탐험 영역

엣지 유형:
- Solid: 이미 이동한 경로
- Dashed: 잠재 확장 경로

### 6.3 User Journey
1. 초기: 빈 지도 또는 최소 힌트 노드
2. 곡 저장: 중심 장르 활성화
3. 인접 장르 노출: 연결선/노드 열림
4. 반복 저장: 경로 강도 증가
5. 신규 장르 진입: New Area Unlocked 토스트/배지

### 6.4 Core Components
- Map Canvas (pan/zoom optional)
- Node Card Tooltip (장르 설명, 저장 수, 최근 진입 시점)
- Flow Summary Bar
- Unlock Event Banner
- Legend (활성/인접/미탐험)

### 6.5 Copy Tone
권장:
- "이 방향으로 확장할 수 있습니다"
- "새 영역이 열렸습니다"
- "현재 취향 흐름"

금지:
- "다음 곡 추천"
- "당신을 위한 추천"

### 6.6 Empty and Cold Start
- 상태 문구: "첫 저장 후 탐험 지도가 시작됩니다"
- 추천 대신 탐험 시작점 힌트 제공

---

## 7. Existing Screens Consolidated Spec

### 7.1 Now Playing
- 권한 배너는 조건부 노출
- 큰 Hero 카드 + Catch Now 버튼 우선
- 최근 감상은 텍스트 중심 카드

### 7.2 Track Detail
- Hero -> Genre -> 3열 Score -> Critics -> Interview -> Sampling -> Guide -> Lyrics -> AI Disclaimer
- 하단 고정 "보관함에 추가" 버튼 유지
- 점수 컬럼: RYM / Critics / AI Agreement

### 7.3 Archive List
- 검색바 + 빠른 필터 칩
- 롱프레스로 선택 모드 진입
- 취소/삭제 하단 액션바

### 7.4 Genre Stats
- 기간 세그먼트: 30d / 90d / 1y
- 도넛 + KPI + 주간 막대 차트
- Insight Hub 내 보조 탭으로 운영

### 7.5 Settings
- 순서: 권한 -> API 키 -> 테마 -> 데이터 관리
- API 키: 마스킹/유효성/저장 피드백
- 위험 동작은 에러 컬러로 분리

---

## 8. Design Tokens (Unified)

### 8.1 Color
- primary: #0058BC
- primaryContainer: #0070EB
- surface: #F9F9FF
- surfaceLowest: #FFFFFF
- surfaceLow: #F1F3FE
- surfaceVariant: #E0E2ED
- onSurface: #181C23
- onSurfaceVariant: #414755
- outlineVariant: #C1C6D7
- error: #BA1A1A

### 8.2 Typography
실제 앱 기준 기본 폰트는 Pretendard를 사용한다.

타입 스케일:
- displayLarge 34/41
- titleLarge 28/34
- titleMedium 22/28
- bodyLarge 17/24
- bodyMedium 15/20
- labelMedium 13/18
- labelSmall 11/13

### 8.3 Spacing and Shape
- base spacing: 8dp
- horizontal padding: 20dp
- card radius: 16~24dp
- chip radius: 999dp
- min touch target: 48dp

---

## 9. Component Standards

- VinfoCard: 흰색 표면 + 얕은 그림자 + 옅은 경계선
- GenreChip: Primary/Secondary 시각 위계 지원
- Segmented Control: 회색 트랙 + 선택 캡슐
- FloatingCircleButton: 상단 액션 공통
- Bottom Navigation Pill: 선택 상태 명확화
- Skeleton UI: Hero/Score/Text 블록 단위

---

## 10. UI State Model

공통:
- Idle / Loading / PartialSuccess / Success / Empty / Error

탐험 지도 추가 상태:
- MapEmpty
- MapBootstrapping
- MapReady
- NodeUnlocked
- MapUpdateFailed

Track Detail 추가 상태:
- MetadataLoading / Loaded
- GenreLoading / Loaded
- LyricsLoading / TranslationLoading
- PartialFailed / CriticalFailed

---

## 11. Validation and Error UX

- API Key: trim, empty check, min length, save feedback
- Search: debounce, no-result empty state
- Lyrics fail: 섹션 단위 실패 처리
- Taste Map fail: 차트/지도만 fallback 메시지, 앱 전체 실패로 전파 금지

권장 메시지:
- "탐험 지도를 불러오지 못했습니다. 다시 시도해주세요."
- "연결 근거 데이터를 준비 중입니다."

---

## 12. Accessibility

- 색상만으로 상태를 전달하지 않는다 (shape/icon/label 병행)
- 노드 상태는 TalkBack 라벨 제공
  - 예: "Hip-hop, 활성화됨, 인접 장르 3개"
- 아이콘 버튼은 contentDescription 필수
- 차트/지도는 텍스트 요약 블록 동시 제공

---

## 13. Content Style Guide

문체:
- 짧고 직접적인 한국어
- 과장형 마케팅 문구 지양
- 근거 기반 설명

표현 규칙:
- 탐험: 연결, 확장, 흐름, 해금
- 금지: 추천, 맞춤 추천, 다음 곡 자동 제안

---

## 14. Implementation Handoff Notes

우선 구현 순서:
1. Insight Hub 라우팅 구조
2. Taste Map 기본 캔버스 + 노드 상태
3. 저장 이벤트와 지도 업데이트 연결
4. 탐험 카피/배지/요약 바 적용

기존 화면은 v2 구현을 유지하되, 탐험 지도와 통합 내비게이션 우선으로 확장한다.

---

## 15. Change Log

v3 (2026-05-14)
- UI_v2 + DESIGN + V1.1/V1.2 개선 이력 + 구현 노트를 단일 문서로 통합
- Stats를 Insight Hub 구조로 확장
- Taste Map(Genre Adjacency Map) 화면 및 상태 모델 신규 추가
- 추천 중심 문맥 제거, 탐험 중심 카피 규칙 명시

---

End of Document
