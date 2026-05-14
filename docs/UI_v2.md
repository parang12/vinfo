# UI Design Document v2 (UI_v2)

> Note: 이 문서는 유지보수용 레거시 기준이다. 2026-05-14부터 통합 기준 문서는 `UI_v3.md`를 우선 사용한다.

**Project:** vinfo (Vinyl + Information)  
**Target Platform:** Android  
**UI Framework:** Jetpack Compose  
**Design Pattern:** Material 3 기반 선언적 UI  
**Purpose:** Stitch에서 `design.md`를 생성하기 위한 UI 구조 정의 문서

---

## 1. 문서 목적

본 문서는 vinfo 애플리케이션의 화면 구성, 주요 UI 컴포넌트, 사용자 흐름, 상태 처리 방식을 정의한다.  
특히 `Track Detail Screen`은 사용자가 음악 정보를 감상하듯 탐색할 수 있도록 **앨범 중심 Hero UI → 장르 → 점수 → 평론/가이드/가사** 순서로 재구성한다.

vinfo의 UI는 단순한 정보 출력 화면이 아니라, 사용자가 현재 듣고 있는 음악의 맥락을 더 깊게 이해할 수 있도록 돕는 **음악 감상 경험 중심 UI**를 목표로 한다.

2026-05-09 기준 최신 시안은 `stitch_vinfo_apple_blue_edition` 폴더의 캡처를 기준으로 다시 정리했다. 이번 버전은 밝은 배경, 유리질감 카드, 떠 있는 상단바, 둥근 하단 탭, Apple Blue 계열 포인트 색을 핵심 규칙으로 사용한다.

---

## 2. 전체 화면 목록

| 화면명                       | 목적                       | 주요 특징                                      |
| ------------------------- | ------------------------ | ------------------------------------------ |
| **Now Playing Screen**    | 현재 재생 중인 음악 감지 및 분석 요청   | 현재 곡 카드, Catch Now 버튼, 권한 안내               |
| **Track Detail Screen**   | AI 분석 결과와 음악 메타데이터 상세 표시 | 앨범 커버 Hero UI, 장르 칩, 점수 카드, 평론, 가사, **하단 보관함 추가 버튼** |
| **Archive List Screen**   | 저장된 감상 기록 목록 조회          | 검색, 필터, 기록 카드 리스트, **다중 선택 및 삭제 모드**    |
| **Archive Detail Screen** | 저장된 특정 음악 정보 재확인         | Track Detail과 동일한 레이아웃 재사용                 |
| **Genre Stats Screen**    | 사용자 청취 장르 통계 시각화         | 도넛 요약, 기간 필터, 장르 분포, 주간 차트               |
| **Settings Screen**       | 권한, API Key, 앱 설정 관리     | 권한 카드, API 입력, 테마 세그먼트, 데이터 관리          |

---

## 3. 전체 네비게이션 구조

vinfo는 Bottom Navigation을 중심으로 주요 화면을 이동한다.

```text
Root
├── Home / Now Playing
│   └── Track Detail
├── Archive
│   └── Archive Detail
├── Stats
└── Settings
```

### 3.1 Floating Bottom Navigation & Top Actions
- **Bottom Navigation**: 화면 하단에 떠 있는 플로팅 형태로, 선택된 탭은 알약(pill) 형태의 연파랑 배경과 굵은 텍스트로 강조된다. 미선택 탭도 아이콘과 텍스트가 가로로 배치되어 통일감을 준다.
- **Top Actions**: 상단바는 긴 pill 형태 대신, 화면별로 필요한 원형 플로팅 버튼(뒤로가기, 설정)만 개별적으로 띄워 펀치홀 카메라 여백을 안전하게 확보한다. 홈 화면은 상단 1/3 영역에 "Vinfo" 타이틀을 크게 중앙 배치한다.

| 탭           | 연결 화면               | 설명                    |
| ----------- | ------------------- | --------------------- |
| **Home**    | Now Playing Screen  | 현재 재생 중인 곡 확인 및 분석 시작 |
| **Archive** | Archive List Screen | 저장된 감상 기록 목록          |
| **Stats**   | Genre Stats Screen  | 장르 기반 청취 통계           |

하단 탭은 둥근 유리질감 컨테이너 안에 배치하고, 선택된 항목은 연한 파란 배경과 진한 파란 아이콘으로 강조한다. 상세 화면에서도 홈 탭이 유지되어 탐색 맥락을 이어준다. 설정 화면은 작업형 페이지로 취급해 별도의 상단 헤더를 사용한다.

### 3.2 Top App Bar

- 현재 화면 제목은 유리질감의 떠 있는 상단바 안에 중앙 정렬로 표시한다.
- Settings 화면은 왼쪽 정렬 제목을 사용하고, 상세 화면은 트랙 제목을 타이틀로 사용한다.
- Detail 계열 화면에서는 Back Button과 Settings Button을 함께 표시한다.
- 상단바와 카드 사이의 간격은 14px 내외를 유지하고, 본문 좌우 패딩은 20px을 기본값으로 둔다.

---

## 4. Now Playing Screen

### 4.1 화면 목적

사용자가 현재 재생 중인 음악을 확인하고, 해당 음악에 대한 분석을 시작할 수 있는 홈 화면이다. 최신 시안에서는 권한 배너를 기본으로 노출하지 않고, 큰 기록 카드와 `Catch Now` CTA를 중심으로 배치한다.

### 4.2 화면 구성

```text
┌──────────────────────────────┐
│ Top App Bar                  │
├──────────────────────────────┤
│ Permission Banner (조건부)    │
├──────────────────────────────┤
│ Active Track Card            │
│ - Album Artwork              │
│ - Track Title                │
│ - Artist Name                │
│ - Source App                 │
├──────────────────────────────┤
│ Catch Now Button             │
├──────────────────────────────┤
│ Recent Catch Preview         │
└──────────────────────────────┘
```

### 4.3 주요 컴포넌트

#### 4.3.1 Permission Banner

`NotificationListenerService` 권한이 없을 때 표시된다. 기본 상태에서는 화면을 압박하지 않도록 숨길 수 있다.

- 알림 접근 권한 필요 문구 표시
- 시스템 설정으로 이동하는 버튼 제공
- 권한이 허용되면 자동으로 숨김 처리

#### 4.3.2 Active Track Card

현재 미디어 알림에서 추출한 음악 정보를 표시한다.

표시 정보:

- 앨범 아트워크
- 곡 제목
- 아티스트명
- 재생 앱 이름
- 마지막 감지 시간

#### 4.3.3 Catch Now Button

현재 감지된 음악을 기준으로 AI 분석을 시작하는 핵심 버튼이다.

동작:

1. 현재 캐싱된 음악 메타데이터 확인
2. Track Detail Screen으로 이동
3. Metadata, Lyrics, Translation 요청 시작
4. 분석 진행 상태를 Detail 화면에서 순차적으로 렌더링

---

## 5. Track Detail Screen

### 5.1 화면 목적

`Track Detail Screen`은 vinfo의 핵심 화면이다.  
사용자는 이 화면에서 현재 음악의 앨범 정보, 장르, 평점, 평론 요약, 인터뷰 맥락, 샘플링 정보, 감상 가이드, 가사 번역을 확인한다.

이 화면은 단순한 데이터 목록이 아니라 **음악을 깊게 감상하기 위한 분석형 상세 페이지**로 설계한다.

최신 시안에서는 상단바에 트랙명을 넣고, 본문 첫 카드에 레코드/앨범 아트를 크게 배치한 뒤 장르 칩, 3열 점수 카드, 비평가 요약, 샘플링 & 감상 가이드, 가사 순으로 이어진다.

---

## 5.2 Track Detail 전체 레이아웃 순서

```text
┌──────────────────────────────┐
│ Top App Bar                  │
├──────────────────────────────┤
│ Hero Section                 │
│ - Album Cover                │
│ - Album Title                │
│ - Artist Name                │
│ - Release Date               │
├──────────────────────────────┤
│ Genre Section                │
│ - Primary Genre Chips        │
│ - Secondary Genre Chips      │
├──────────────────────────────┤
│ Score Section                │
│ - RYM Score                  │
│ - Critics Score              │
│ - Popularity / AI Agreement  │
├──────────────────────────────┤
│ Critics Summary Section      │
├──────────────────────────────┤
│ Artist / Producer Interview  │
├──────────────────────────────┤
│ Sampling Section             │
├──────────────────────────────┤
│ Listening Guide Section      │
├──────────────────────────────┤
│ Lyrics Section               │
│ - Original Lyrics            │
│ - Translated Lyrics          │
├──────────────────────────────┤
│ AI Disclaimer                │
└──────────────────────────────┘
```

---

## 5.3 Hero Section

### 5.3.1 목적

사용자가 Detail 화면에 진입했을 때 가장 먼저 현재 음악의 정체성을 인식하도록 돕는다.

### 5.3.2 구성 요소

| 요소               | 설명                      |
| ---------------- | ----------------------- |
| **Album Cover**  | 화면 상단 중앙에 크게 배치되는 앨범 커버 |
| **Album Title**  | 앨범명 또는 트랙이 속한 앨범명       |
| **Artist Name**  | 아티스트명                   |
| **Release Date** | 발매일 또는 발매년도             |

### 5.3.3 UI 방향

- 앨범 커버는 화면 상단의 가장 큰 시각 요소로 배치한다.
- 커버 이미지를 활용한 흐림 배경 또는 그라데이션 배경을 적용할 수 있다.
- 앨범명은 가장 큰 텍스트로 표시한다.
- 아티스트명은 앨범명보다 한 단계 작은 텍스트로 표시한다.
- 발매일은 보조 텍스트로 표시한다.
- 발매일 표기는 `발매일: 2005. 02. 14.`처럼 짧고 직접적인 형식을 사용한다.

### 5.3.4 예시 구조

```text
┌──────────────────────────────┐
│        [ Album Cover ]        │
│                              │
│        Album Title            │
│        Artist Name            │
│        1997.05.21             │
└──────────────────────────────┘
```

---

## 5.4 Genre Section

### 5.4.1 목적

음악의 장르적 성격을 빠르게 파악할 수 있도록 한다.

### 5.4.2 장르 표시 방식

장르는 크게 두 단계로 나누어 표시한다.

| 구분                  | 설명             | UI 강조        |
| ------------------- | -------------- | ------------ |
| **Primary Genre**   | 대표 장르 2~3개     | 강한 색상, 높은 대비 |
| **Secondary Genre** | 세부 장르 또는 관련 장르 | 약한 색상, 낮은 대비 |

### 5.4.3 UI 예시

```text
[Alternative Rock] [Shoegaze] [Dream Pop]
[Noise Pop] [Neo-Psychedelia] [Indie Rock]
```

### 5.4.4 디자인 규칙

- 대표 장르 2~3개는 Material 3 `primaryContainer` 계열 색상을 사용한다.
- 나머지 장르는 `secondaryContainer` 또는 `surfaceVariant` 계열 색상을 사용한다.
- 장르 칩은 한 줄에 모두 넣지 않고, 화면 폭에 따라 자동 줄바꿈되도록 구성한다.
- 장르 데이터가 없을 경우 `"장르 정보를 찾을 수 없음"` 상태를 표시한다.

---

## 5.5 Score Section

### 5.5.1 목적

음악에 대한 외부 평가 및 AI 분석 신뢰도를 한눈에 확인할 수 있도록 한다.

### 5.5.2 표시 점수

| 점수 항목                            | 설명                         |
| -------------------------------- | -------------------------- |
| **RYM Score**                    | Rate Your Music 기반 평점      |
| **Critics Score**                | 평론가 반응을 요약한 점수 또는 등급       |
| **Community / Popularity Score** | 대중적 반응, 언급량, 인지도 등을 요약한 지표 |
| **AI Agreement**                 | AI가 수집한 정보가 얼마나 일치하는지 요약한 지표  |

### 5.5.3 UI 예시

```text
┌────────────┬────────────┬────────────┐
│ RYM        │ Pitchfork  │ AI Agreement │
│ 4.12       │ 8/10       │ High       │
└────────────┴────────────┴────────────┘
```

### 5.5.4 디자인 규칙

- 점수들은 하나의 카드 안에 가로로 배치한다.
- 모바일 폭이 좁은 경우 2열 Grid 또는 세로 리스트로 전환한다.
- 점수 값이 없을 경우 `-` 또는 `"정보 없음"`으로 표시한다.
- LLM 기반 추정 점수는 실제 공식 점수와 혼동되지 않도록 보조 설명을 함께 제공한다.

---

## 5.6 Critics Summary Section

### 5.6.1 목적

Perplexity API를 통해 수집한 평론가 반응과 음악적 평가를 요약하여 제공한다.

### 5.6.2 구성 요소

- 섹션 제목: `Critics Summary`
- 요약 본문
- 주요 키워드
- 출처 기반 정보 여부 표시

### 5.6.3 UI 예시

```text
Critics Summary

이 앨범은 발매 당시 실험적인 사운드와 감정적인 보컬로 주목받았으며,
장르적으로는 Shoegaze와 Dream Pop의 경계를 확장한 작품으로 평가된다.
```

### 5.6.4 디자인 규칙

- 긴 텍스트는 카드 형태로 묶는다.
- 처음에는 3~5줄만 보여주고, `더보기` 버튼으로 확장할 수 있다.
- 로딩 중에는 Shimmer Text Placeholder를 표시한다.

---

## 5.7 Artist / Producer Interview Section

### 5.7.1 목적

아티스트 또는 프로듀서가 해당 음악에 대해 언급한 배경, 제작 의도, 인터뷰 내용을 요약한다.

### 5.7.2 구성 요소

- 인터뷰 요약
- 제작 배경
- 녹음 또는 프로덕션 관련 맥락
- 확인 가능한 출처가 있을 경우 출처명 표시

### 5.7.3 Empty State

관련 인터뷰 정보를 찾지 못한 경우:

```text
관련 인터뷰 정보를 찾을 수 없습니다.
```

---

## 5.8 Sampling Section

### 5.8.1 목적

해당 곡이 사용한 샘플링 정보 또는 원곡 정보를 제공한다.

### 5.8.2 구성 요소

| 요소                  | 설명                 |
| ------------------- | ------------------ |
| **Sampled From**    | 샘플링된 원곡            |
| **Sample Type**     | 보컬, 드럼, 멜로디, 베이스 등 |
| **Original Artist** | 원곡 아티스트            |
| **Usage Summary**   | 어떤 방식으로 활용되었는지 설명  |

### 5.8.3 UI 예시

```text
Sampling Information

Sampled From
- Artist A - Original Track
- Artist B - Drum Break Source
```

### 5.8.4 Empty State

샘플링 정보가 없거나 확인되지 않을 경우:

```text
확인된 샘플링 정보가 없습니다.
```

---

## 5.9 Listening Guide Section

### 5.9.1 목적

사용자가 음악을 들을 때 집중해서 감상하면 좋은 포인트를 제공한다.

### 5.9.2 구성 요소

- 보컬 감상 포인트
- 악기 또는 사운드 디자인 포인트
- 믹싱 / 프로덕션 포인트
- 가사적 주제
- 곡 전개 구조

### 5.9.3 UI 예시

```text
Listening Guide

• 초반부 기타 톤의 질감 변화에 집중해보세요.
• 보컬이 믹스 뒤쪽에 배치되면서 몽환적인 분위기를 만듭니다.
• 후반부 드럼의 밀도가 증가하며 감정선이 고조됩니다.
```

### 5.9.4 디자인 규칙

- 불렛 포인트 형식으로 표시한다.
- 각 항목은 너무 길지 않게 1~2문장으로 제한한다.
- 사용자가 바로 음악을 들으며 따라갈 수 있도록 간결하게 작성한다.

---

## 5.10 Lyrics Section

### 5.10.1 목적

원문 가사와 Gemini 기반 한국어 의역 번역을 제공한다.

### 5.10.2 구성 방식

- `Original Lyrics`
- `Translated Lyrics`

두 개의 탭 또는 Segmented Button으로 전환한다.

### 5.10.3 UI 예시

```text
[Original] [Translated]

Translated Lyrics

...
```

### 5.10.4 디자인 규칙

- 가사는 긴 텍스트이므로 세로 스크롤을 지원한다.
- 번역 가사는 원문보다 살짝 다른 타이포그래피 스타일을 사용할 수 있다.
- 가사 로딩 실패 시 전체 Detail 화면을 실패 처리하지 않고, 가사 영역만 Empty State로 대체한다.

### 5.10.5 Empty / Error State

```text
가사를 찾을 수 없습니다.
```

또는

```text
번역을 불러오지 못했습니다. 다시 시도해주세요.
```

---

## 5.11 AI Disclaimer

### 5.11.1 목적

LLM이 생성하거나 요약한 정보가 실제와 다를 수 있음을 사용자에게 명확히 안내한다.

### 5.11.2 표시 문구

```text
AI가 생성한 정보로 실제와 다를 수 있습니다. 중요한 정보는 공식 출처를 확인해주세요.
```

### 5.11.3 디자인 규칙

- Detail 화면 하단에 작게 표시한다.
- 너무 강한 경고처럼 보이지 않도록 `onSurfaceVariant` 계열 색상을 사용한다.
- RYM 점수, 평론 요약, 장르 분류처럼 AI가 수집하거나 추정한 정보 근처에도 보조 문구를 표시할 수 있다.

---

## 6. Archive List Screen

### 6.1 화면 목적

사용자가 과거에 분석하고 저장한 음악 기록을 목록으로 확인할 수 있도록 한다.

최신 시안에서는 상단바에 `Vinfo`를 두고, 검색 바는 `보관함 검색...` 플레이스홀더를 사용한다. 필터 칩은 `전체`, `최근`, `재즈`, `일렉트로닉` 같은 빠른 선택 항목을 가로로 배치하고, 리스트 카드는 제목/아티스트/날짜와 오른쪽 장르 칩만 남겨 빠르게 스캔할 수 있게 정리한다.

### 6.2 화면 구성

```text
┌──────────────────────────────┐
│ Top App Bar                  │
├──────────────────────────────┤
│ Search Bar                   │
├──────────────────────────────┤
│ Filter Chips                 │
├──────────────────────────────┤
│ History Card List            │
└──────────────────────────────┘
```

### 6.3 주요 컴포넌트

#### 6.3.1 Archive Search Bar

- 곡명 검색
- 아티스트명 검색
- 앨범명 검색
- Debounce 적용

#### 6.3.2 Filter Chips

- 전체
- 최근 7일
- 최근 30일
- 장르별
- 높은 평점순

#### 6.3.3 History Card

표시 정보:

- 앨범 커버 썸네일
- 곡 제목
- 아티스트명
- 대표 장르 칩
- 저장 시각
- 간단한 점수 정보

---

## 7. Archive Detail Screen

### 7.1 화면 목적

저장된 감상 기록의 상세 정보를 다시 확인하는 화면이다.

### 7.2 설계 방식

`Archive Detail Screen`은 `Track Detail Screen`과 동일한 레이아웃을 재사용한다.

차이점:

| 항목     | Track Detail | Archive Detail |
| ------ | ------------ | -------------- |
| 데이터 출처 | API + DB 저장  | Room DB        |
| 로딩 상태  | API 호출 상태 존재 | 대부분 즉시 표시      |
| 저장 동작  | 분석 완료 후 저장   | 이미 저장된 데이터 표시  |
| 오류 상태  | 네트워크 오류 가능   | DB 데이터 누락 가능   |

---

## 8. Genre Stats Screen

### 8.1 화면 목적

사용자의 음악 감상 기록을 기반으로 장르별 취향을 시각화한다. 최신 시안에서는 상단에 `장르 통계` 제목과 설명을 두고, 그 아래에 전체 폭 세그먼트와 도넛 카드, 세로 요약 카드, 주간 막대 차트 카드를 차례대로 배치한다.

### 8.2 화면 구성

```text
┌──────────────────────────────┐
│ Top App Bar                  │
├──────────────────────────────┤
│ Period Selector              │
├──────────────────────────────┤
│ Genre Distribution Chart     │
├──────────────────────────────┤
│ Weekly Trend Chart           │
├──────────────────────────────┤
│ Summary Cards                │
└──────────────────────────────┘
```

### 8.3 주요 컴포넌트

#### 8.3.1 Period Selector

- 전체
- 30일
- 90일
- 1년

#### 8.3.2 Genre Distribution Chart

- 장르별 감상 비율 표시
- Pie Chart 또는 Donut Chart 사용 가능
- 특정 장르 선택 시 해당 장르로 필터링된 Archive List로 이동 가능

#### 8.3.3 Weekly Trend Chart

- 주간 감상량 변화 표시
- Bar Chart 형태 권장

#### 8.3.4 Summary Cards

예시:

- 가장 많이 들은 장르
- 가장 높은 평점
- 평균 RYM 점수
- 최근 가장 자주 저장한 장르

---

## 9. Settings Screen

### 9.1 화면 목적

앱 동작에 필요한 권한, API Key, 테마, 데이터 관리 기능을 제공한다. 설정은 작업형 페이지로 취급하며, 왼쪽 정렬 제목과 세로 카드 그룹, 그리고 하단의 버전/정책 링크를 둔다.

### 9.2 화면 구성

```text
┌──────────────────────────────┐
│ Top App Bar                  │
├──────────────────────────────┤
│ Permission Status Section    │
├──────────────────────────────┤
│ API Key Section              │
├──────────────────────────────┤
│ Theme Section                │
├──────────────────────────────┤
│ Data Management Section      │
└──────────────────────────────┘
```

### 9.3 주요 컴포넌트

#### 9.3.1 Permission Status Section

- Notification Access 권한 상태 표시
- 권한 미허용 시 설정 이동 버튼 표시

#### 9.3.2 API Key Section

- Perplexity API Key 입력
- Gemini API Key 입력
- 입력 시 공백 제거
- 저장 전 간단한 형식 검증

#### 9.3.3 Theme Section

- 라이트 모드
- 다크 모드
- 시스템 기본값

#### 9.3.4 Data Management Section

- 캐시 삭제
- 저장 기록 초기화
- DB 백업 또는 내보내기 기능은 추후 확장 가능

---

## 10. UiState 설계

### 10.1 공통 상태

| 상태                  | 설명                | UI 대응                       |
| ------------------- | ----------------- | --------------------------- |
| **Idle**            | 아직 요청이 시작되지 않은 상태 | 기본 화면 표시                    |
| **Loading**         | API 또는 DB 요청 진행 중 | Shimmer, Progress Indicator |
| **Partial Success** | 일부 데이터만 먼저 도착한 상태 | 도착한 영역부터 순차 렌더링             |
| **Success**         | 전체 데이터 로드 완료      | 전체 UI 표시                    |
| **Empty**           | 표시할 데이터가 없음       | Empty State 문구와 안내 버튼       |
| **Error**           | 필수 데이터 로드 실패      | Snackbar, Retry Button      |

### 10.2 Track Detail 상태

Track Detail은 여러 API 결과를 순차적으로 표시하기 때문에 세분화된 상태가 필요하다.

| 상태                      | 설명                         | UI 대응                  |
| ----------------------- | -------------------------- | ---------------------- |
| **Metadata Loading**    | 앨범/곡 기본 정보 로딩              | Hero Section Shimmer   |
| **Metadata Loaded**     | 앨범명, 아티스트, 발매일 로드 완료       | Hero Section 표시        |
| **Genre Loading**       | 장르 정보 로딩                   | Genre Chip Placeholder |
| **Genre Loaded**        | Primary/Secondary 장르 로드 완료 | Genre Chip 표시          |
| **Score Loading**       | RYM/평론 점수 로딩               | Score Card Shimmer     |
| **Score Loaded**        | 점수 데이터 로드 완료               | Score Section 표시       |
| **Lyrics Loading**      | 가사 요청 중                    | Lyrics Placeholder     |
| **Translation Loading** | Gemini 번역 중                | 번역 영역 로딩 표시            |
| **Fully Loaded**        | 전체 분석 결과 로드 완료             | 저장 및 전체 표시             |
| **Partial Failed**      | 일부 선택 데이터 실패               | 해당 영역만 Empty State 처리  |
| **Critical Failed**     | 기본 메타데이터 실패                | 전체 재시도 UI 표시           |

---

## 11. 로딩 및 에러 처리

### 11.1 Shimmer Loading

다음 영역에서 Shimmer를 사용한다.

- Album Cover
- Album Title
- Genre Chips
- Score Cards
- Critics Summary
- Lyrics Section

### 11.2 Partial Rendering

API 응답은 한 번에 모두 도착하지 않을 수 있으므로, 먼저 도착한 데이터부터 화면에 표시한다.

예시:

```text
1. Hero Section 먼저 표시
2. Genre Section 표시
3. Score Section 표시
4. Critics Summary 표시
5. Lyrics / Translation 표시
```

### 11.3 Error Handling

| 실패 영역          | 처리 방식                        |
| -------------- | ---------------------------- |
| Metadata 실패    | 전체 분석 실패로 보고 Retry Button 표시 |
| Genre 실패       | `"장르 정보를 찾을 수 없음"` 표시        |
| Score 실패       | 점수 영역에 `"정보 없음"` 표시          |
| Lyrics 실패      | `"가사를 찾을 수 없음"` 표시           |
| Translation 실패 | 원문 가사는 유지하고 번역 영역만 실패 처리     |
| Network 실패     | Snackbar + Retry Button 표시   |

---

## 12. 입력값 및 검증

### 12.1 API Key

- 앞뒤 공백 제거
- 비어 있는 값 저장 방지
- 최소 길이 검증
- 저장 후 간단한 연결 테스트 가능

### 12.2 Archive Search

- 입력 즉시 검색하지 않고 Debounce 적용
- 곡명, 아티스트명, 앨범명을 대상으로 필터링
- 검색 결과가 없을 경우 Empty State 표시

### 12.3 Prompt Sanitizer 상태 표시

LLM 응답 파싱 과정에서 다음 로딩 메시지를 사용할 수 있다.

```text
음악 정보를 수집하는 중...
평론 데이터를 정리하는 중...
장르 정보를 정규화하는 중...
가사를 번역하는 중...
화면에 표시할 정보를 준비하는 중...
```

---

## 13. 접근성 및 가독성

### 13.1 Contrast

- Material 3 다크/라이트 테마 지원
- 텍스트와 배경 간 명확한 대비 확보
- 점수나 장르 색상은 색상만으로 의미를 전달하지 않음

### 13.2 Touch Target

- 모든 버튼, 칩, 탭 요소는 최소 48dp 크기 확보
- Lyrics 탭과 Filter Chip은 한 손 조작이 가능하도록 충분한 간격 유지

### 13.3 TalkBack

- 앨범 커버에는 앨범명 기반 Content Description 제공
- 장르 칩은 `"대표 장르: Shoegaze"`처럼 의미가 포함된 라벨 제공
- 점수는 `"RYM 점수 4.12점"`처럼 읽히도록 구성
- 긴 가사/평론은 읽기 순서를 명확히 정의

---

## 14. 디자인 톤

### 14.1 전체 분위기

vinfo는 음악 감상 앱이므로 지나치게 기술적인 UI보다 감성적이고 몰입감 있는 UI를 지향한다.

권장 톤:

- 어두운 배경에 앨범 커버가 돋보이는 구조
- 카드 기반 정보 구획
- 둥근 모서리
- 부드러운 그라데이션
- 음악 아카이브 느낌의 차분한 색상

### 14.2 Material 3 적용

- `Surface`
- `Card`
- `ElevatedCard`
- `AssistChip`
- `FilterChip`
- `SegmentedButton`
- `NavigationBar`
- `CenterAlignedTopAppBar`

사용을 권장한다.

---

## 15. 기타 가정 사항

1. 앨범 아트워크는 미디어 알림 또는 외부 API 결과에서 가져온다고 가정한다.
2. 장르 데이터는 LLM 응답을 그대로 사용하지 않고 앱 내부 표준 장르 맵을 통해 정규화한다.
3. 점수 데이터는 실제 외부 사이트 값과 AI 추정값이 섞일 수 있으므로 사용자에게 명확히 안내한다.
4. 네트워크가 오프라인일 경우 새 분석은 제한되고, Archive 조회만 가능하다.
5. 같은 곡을 다시 분석하는 경우 DB 캐시를 먼저 확인하여 API 비용을 줄인다.

---

## 16. Track Detail 요약 구조

Stitch에서 디자인을 생성할 때 가장 중요하게 반영해야 하는 Track Detail 구조는 다음과 같다.

```text
Track Detail Screen
1. Album Cover
2. Album Title
3. Artist Name
4. Release Date
5. Primary / Secondary Genre Chips
6. Score Cards
7. Critics Summary
8. Artist / Producer Interview Summary
9. Sampling Information
10. Listening Guide
11. Lyrics Original / Translated Tabs
12. AI Disclaimer
```

---

**[End of Document]**
