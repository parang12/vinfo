# Software Requirements Specification (SRS) for vinfo (Vinyl + Information)

**Project Name:** vinfo (Vinyl + Information)
**Target Platform:** Android (API Level 26 or higher)
**Development Environment:** Android Studio, Jetpack Compose
**Version:** 1.3
**Author:** Kim Tae-jin (김태진)
**Date:** 2026-05-14

---

## 1. Introduction (서론)

### 1.1 Purpose
본 문서는 음악 정보 아카이빙 및 분석 애플리케이션인 'vinfo'의 요구 사양을 상세히 정의한다. 이 문서는 개발의 기준점이 되며, 기술적 구현 가능성과 시스템 아키텍처를 명확히 함을 목적으로 한다.

### 1.2 Scope
vinfo는 사용자가 현재 청취 중인 음악을 실시간으로 식별하고, LLM(Perplexity, Gemini)을 활용하여 심층적인 음악적 맥락(평론, 샘플링, 가사 번역 등)을 제공하며, 이를 DB에 기록한다. 또한 장기 청취 기록을 기반으로 장르 간 인접성과 확장 흐름을 시각화하여, 단순 추천이 아닌 "취향 탐험" 경험을 제공하는 모바일 애플리케이션이다.

### 1.3 Definitions and Abbreviations
* **LLM:** Large Language Model (대규모 언어 모델)
* **RYM:** Rate Your Music (음악 평점 및 데이터베이스 사이트)
* **SRS:** Software Requirements Specification (소프트웨어 요구 사양서)
* **MVVM:** Model-View-ViewModel (UI 아키텍처 패턴)

---

## 2. Overall Description (전체 설명)

### 2.1 Product Perspective
vinfo는 단독 실행되는 앱이나, Android 시스템의 알림 서비스와 긴밀하게 연동된다. 외부 API(Perplexity, Gemini, lyrics.ovh)에 의존하며, 데이터 저장소로 로컬 Room DB를 사용한다.

### 2.2 System Functions
1.  **실시간 음악 인식:** 백그라운드 미디어 알림에서 아티스트/곡 제목 파싱.
2.  **심층 정보 생성:** Perplexity API를 이용한 전문 평론 및 인터뷰 데이터 요약.
3.  **가사 자동화:** 가사 추출 및 Gemini API 기반 고품질 의역 번역.
4.  **아카이빙:** 감상 기록 저장 및 장르/날짜 데이터 관리.
5.  **취향 탐험 지도:** 장르 인접 그래프(Genre Adjacency Map) 기반 취향 확장 흐름 시각화.
6.  **통계 시각화:** 아카이브 데이터를 활용한 장르 비율 및 기간별 변화 차트 제공.

### 2.3 User Classes and Characteristics
* **음악 매니아:** 단순 감상을 넘어 앨범의 배경, 샘플링, 평론가의 시각을 중시하는 사용자.
* **데이터 수집가:** 자신의 감상 기록을 장르별로 정리하고 시각화하여 보고 싶어 하는 사용자.

---

## 3. System Features (시스템 기능 요구 사항)

### 3.1 [Feature 1] Real-time Music Detection (Notification Listener)
* **Description:** `NotificationListenerService`를 통해 활성화된 미디어 플레이어(Spotify, YouTube Music, Apple Music 등)의 알림을 감시한다.
* **Functional Requirements:**
    * `onNotificationPosted` 이벤트 발생 시 미디어 메타데이터(Title, Artist) 추출.
    * 앱 UI 내 "Catch Now" 버튼 클릭 시 최신 메타데이터를 캐싱.
* **Logical Constraint:** 권한 획득 실패 시 수동 입력 모드로 전환되어야 함.

### 3.2 [Feature 2] Intelligent Metadata Synthesis (Perplexity API)
* **Description:** 검색 기반 LLM인 Perplexity를 사용하여 정형화된 음악 정보를 생성한다.
* **Input Data:** `"${Artist} - ${Title}"`
* **Prompt Engineering Structure:**
    1.  Critics' reviews summary.
    2.  Genre classification (Primary & Secondary).
    3.  RYM (Rate Your Music) score estimation/search.
    4.  Artist/Producer interviews related to the track/album.
    5.  Sampling information (Original tracks used).
    6.  Listening Guide (Focus points).
* **Output Format:** JSON 형식으로 응답을 강제하여 앱 내 View에서 구조적으로 표시.

### 3.3 [Feature 3] Lyrics & AI Translation (lyrics.ovh + Gemini)
* **Description:** 가사 데이터 획득 후 감성적인 맥락을 유지하며 번역을 수행한다.
* **Process:**
    1.  `lyrics.ovh` API 호출.
    2.  텍스트를 Gemini 1.5 Flash/Pro 모델에 전송.
    3.  번역 지침: "직역이 아닌 한국 대중음악 감성에 맞게 문학적으로 의역할 것."

### 3.4 [Feature 4] Database & Archiving (Room DB)
* **Schema (ListenHistory):**
    * `id` (Primary Key)
    * `timestamp` (Date/Time)
    * `artist_name`, `track_title`, `genre`
    * `review_summary`, `translated_lyrics`
* **Functional Requirements:** '저장' 버튼 클릭 시 현재 조회된 모든 데이터를 Local DB에 커밋.

### 3.5 [Feature 5] Preference Visualization (Charts)
* **Description:** 저장된 장르 데이터를 분석하여 사용자 취향 통계를 생성한다.
* **UI Components:**
    * `Genre Pie Chart`: 전체 장르 분포.
    * `Weekly Trend Bar Chart`: 주간 음악 소비량 추이.

### 3.6 [Feature 6] Taste Exploration Map (Genre Adjacency Map)
* **Description:** 사용자의 저장 기록을 기반으로 장르를 독립 카테고리가 아닌 연결된 공간으로 표현하고, 취향의 인접 영역 및 확장 가능성을 시각화한다.
* **Core Framing Constraint:** 본 기능은 "다음 곡 추천"이 아니라 "취향 탐험 경로 안내"를 목표로 한다.
* **Functional Requirements:**
    * 초기 상태에서 탐험 지도는 비활성(빈 노드) 상태로 시작한다.
    * 곡/앨범 저장 시 해당 곡의 Primary/Secondary Genre를 기준으로 중심 노드를 활성화한다.
    * 중심 노드와 인접 장르 노드를 연결선으로 표시한다.
    * 미탐험 인접 장르는 잠금 해제 가능 영역으로 표시한다.
    * 반복 저장/장기 기록/장르 비율/이동 패턴을 반영해 확장 경로 우선순위를 갱신한다.
* **User-visible Outputs:**
    * 현재 취향 흐름 예시(예: Hip-hop -> Jazz Rap -> Neo Soul)
    * 새 영역 활성화 알림(예: New Area Unlocked: Dream Pop)
    * 중심 장르 / 인접 장르 / 미탐험 영역 구분 시각화
* **Logical Constraints:**
    * 추천 어휘("추천", "다음 곡")보다 탐험 어휘("연결", "확장", "탐험")를 우선 사용해야 한다.
    * 인접 장르 계산은 내부 장르 인접 맵(표준 장르 맵 + 연결 규칙)을 사용한다.

---

## 4. Technical Architecture (기술 설계)

### 4.1 Android Stack
* **UI:** Jetpack Compose (Declarative UI)
* **Architecture:** MVVM + Clean Architecture (Domain, Data, UI Layers)
* **Dependency Injection:** Hilt
* **Networking:** Retrofit2, OkHttp3
* **Local DB:** Room Persistence Library

### 4.2 API Integration Flow
[Catch Music] -> [Extract Strings] -> [Parallel Call: Perplexity & lyrics.ovh] -> [lyrics.ovh Result -> Gemini Translation] -> [Archive Commit] -> [Genre Normalization] -> [Adjacency Map Update] -> [Combine & Display]

---

## 5. Non-functional Requirements (비기능적 요구 사항)

* **Usability:** 정보 로딩 중 사용자 이탈 방지를 위한 Skeleton Screen 적용.
* **Performance:** API 호출의 비동기 처리(Coroutines)를 통해 UI 프리징 방지.
* **Maintainability:** API Key 노출 방지를 위한 Secret Gradle Plugin 사용.
* **Connectivity:** 네트워크 오프라인 시 DB에 저장된 과거 기록만 조회 가능하도록 설계.
* **Explainability:** 탐험 지도의 연결 근거(저장 이력/장르 인접성)를 사용자에게 이해 가능한 형태로 제공.
* **Visual Continuity:** 지도 상 노드/엣지 애니메이션은 60fps 체감을 목표로 하며, 저사양 기기에서는 단순화 모드 제공.

---

## 6. Intellectual Sparring: Critical Analysis & Technical Risks
*(지적 스파링 파트너로서의 냉정한 평가)*

1.  **The API Fragility (API의 취약성):**
    `lyrics.ovh`는 오픈소스 수준의 프로젝트로, 서버 가동률이 불안정하고 데이터 누락이 잦습니다. 상용 앱 수준의 안정성을 원한다면 Genius API나 Musixmatch API로의 전환을 고려해야 합니다.

2.  **Token Economy (비용 문제):**
    사용자가 매 곡마다 '정보 가져오기'를 누르면 Perplexity와 Gemini 비용이 기하급수적으로 발생합니다. 특히 가사 전체 번역은 토큰 소모가 큽니다.
    * *Countermeasure:* 번역 결과물은 반드시 DB에 캐싱하여 같은 곡 재조회 시 API 호출을 차단해야 합니다.

3.  **Genre Ambiguity (장르의 모호성):**
    Perplexity가 반환하는 장르는 매번 조금씩 다를 수 있습니다 (예: 'Synth-pop' vs 'Electronic Pop'). 이를 그대로 통계에 넣으면 그래프가 지저분해집니다.
    * *Countermeasure:* 앱 내부적으로 '표준 장르 맵'을 정의하고 LLM의 결과값을 매핑(Mapping)하는 정규화 로직이 필요합니다.

4.  **Background Limitation (OS 제약):**
    Android 12 이후 백그라운드 서비스 제약이 강화되었습니다. `NotificationListenerService`가 시스템에 의해 킬(Kill)당할 경우를 대비해, 포그라운드 서비스(Foreground Service) 활용 및 권한 안내 가이드를 철저히 설계해야 합니다.

5.  **Information Hallucination (정보 환각):**
    RYM 평점은 LLM이 검색을 통해 가져오지만, 없는 평점을 지어낼 확률이 0이 아닙니다. '이 정보는 AI에 의해 생성되었으며 실제와 다를 수 있음'이라는 면책 조항은 디자인적으로 필수입니다.

6.  **Exploration Bias (탐험 편향):**
    인접 장르 계산 규칙이 단순하면 특정 장르 축으로만 경로가 집중되어 "탐험"이 아닌 "편향된 반복"이 될 수 있습니다.
    * *Countermeasure:* 장기 기록 기반 다양성 보정(Exploration Diversity Weight)과 미탐험 영역 가중치를 함께 적용한다.

7.  **Cold Start Map (초기 지도 공백):**
    신규 사용자는 저장 이력이 적어 지도가 과도하게 비어 보일 수 있습니다.
    * *Countermeasure:* 초기에 최소 인접 노드 힌트를 제공하되, 추천 문구 대신 "탐험 시작점" 문구를 사용한다.

---

**[End of Document]**
