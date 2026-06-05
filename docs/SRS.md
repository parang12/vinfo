# Software Requirements Specification (SRS) for vinfo (Vinyl + Information)

**Project Name:** vinfo (Vinyl + Information)
**Target Platform:** Android (API Level 26 or higher)
**Development Environment:** Android Studio, Jetpack Compose
**Version:** 1.4
**Author:** Kim Tae-jin (김태진)
**Date:** 2026-06-02

---

## 1. Introduction (서론)

### 1.1 Purpose
본 문서는 음악 정보 아카이빙 및 분석 애플리케이션인 'vinfo'의 요구 사양을 상세히 정의한다. 이 문서는 개발의 기준점이 되며, 기술적 구현 가능성과 시스템 아키텍처를 명확히 함을 목적으로 한다.

### 1.2 Scope
vinfo는 사용자가 현재 청취 중인 음악을 실시간으로 식별하고, 감지된 아티스트명과 곡명을 이용해 해당 곡이 수록된 앨범을 먼저 식별한다. 이후 Gemini를 활용하여 앨범 기준의 심층 음악적 맥락(장르 후보, 평론, 앨범 평점, 샘플링 등)을 제공하고 이를 DB에 기록한다. 현재 곡의 원문 가사는 `lyrics.ovh`에서 별도로 조회한다. Perplexity 관련 레거시 구현은 코드베이스에 남아 있으나, 런타임은 `GeminiTrackMetadataRepository`를 사용하도록 전환되어 있다. 또한 장기 청취 기록을 기반으로 장르 간 인접성과 확장 흐름을 시각화하여, 단순 추천이 아닌 "취향 탐험" 경험을 제공하는 모바일 애플리케이션이다.

### 1.3 Definitions and Abbreviations
* **LLM:** Large Language Model (대규모 언어 모델)
* **RYM:** Rate Your Music (음악 평점 및 데이터베이스 사이트; vinfo에서는 곡 단위가 아니라 앨범 단위 평점/장르 근거로만 사용)
* **Album-based Metadata:** 현재 감지된 곡의 `artist + title`로 수록 앨범을 식별한 뒤, 장르/평론/평점은 해당 앨범 기준으로 수집하는 데이터 계약
* **SRS:** Software Requirements Specification (소프트웨어 요구 사양서)
* **MVVM:** Model-View-ViewModel (UI 아키텍처 패턴)

---

## 2. Overall Description (전체 설명)

### 2.1 Product Perspective
vinfo는 단독 실행되는 앱이나, Android 시스템의 알림 서비스와 긴밀하게 연동된다. 외부 API(Gemini, lyrics.ovh)에 의존하며, Perplexity는 레거시/대체 구현으로 저장소에 일부 코드가 남아있다. 데이터 저장소로 로컬 Room DB를 사용한다.

### 2.2 System Functions
1.  **실시간 음악 인식:** 백그라운드 미디어 알림에서 아티스트/곡 제목 파싱.
2.  **앨범 식별 및 심층 정보 생성:** 감지된 아티스트/곡 제목으로 수록 앨범을 식별하고, Gemini API를 이용해 앨범 기준의 장르/평론/평점/인터뷰 데이터를 요약한다.
3.  **가사 자동화:** `lyrics.ovh` 기반 현재 곡 원문 가사 조회. Gemini 번역은 후속 기능으로 분리.
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

### 3.2 [Feature 2] Intelligent Metadata Synthesis (Gemini API)
* **Description:** 검색 기반 LLM인 Gemini를 사용하여 감지된 곡의 수록 앨범을 식별하고, 앨범 기준의 정형화된 음악 정보를 생성한다.
* **Input Data:** `"${Artist} - ${Title}"` (`Album` 값이 MediaSession에서 감지되면 보조 힌트로만 전달)
* **Album-first Rule:** Gemini는 반드시 아티스트명과 곡명으로 해당 곡이 수록된 앨범을 먼저 찾은 뒤, 아래 항목을 모두 그 앨범 기준으로 산출해야 한다. 곡 단위 평점/장르를 임의로 만들지 않는다.
* **Search Grounding Rule:** Gemini 요청에는 Google Search grounding을 활성화한다. RYM, Pitchfork, Metacritic, AOTY의 직접 페이지를 먼저 찾고, 직접 접근이 막히거나 검색되지 않을 때만 Reddit 또는 HipHople 결과를 우회 탐색과 교차검증에 사용한다. 단일 커뮤니티 게시글만으로 평점을 확정하지 않으며, 여러 독립 결과가 같은 앨범 단위 점수와 원출처를 일관되게 인용하지 않으면 `null`로 처리한다.
* **Prompt Engineering Structure:**
    1.  Album identification: album title and reliability note when identification is uncertain.
    2.  Album-level genre classification (Primary & Secondary).
    3.  Album-level ratings/review scores from RYM, Pitchfork, Metacritic, and AOTY when available.
    4.  Critics' reviews summary based on the identified album.
    5.  Artist/Producer interviews related to the identified album.
    6.  Sampling information for the current track only when reliable, otherwise omit.
    7.  Listening Guide (Focus points) explaining the current track in the album context.
* **Output Format:** JSON 형식으로 응답을 강제하여 앱 내 View에서 구조적으로 표시.
* **Missing Data Rule:** RYM, Pitchfork, Metacritic, AOTY 중 확인 가능한 출처만 표시한다. 확인되지 않는 값은 추정하지 않고 `null` 또는 빈 배열로 반환한다.
* **Genre Candidate Rule:** 앨범 분석 단계에서 Gemini는 앨범의 장르 후보와 신뢰도만 반환한다. 장르 간 영향 관계나 신규 연결선은 앨범 저장 시 자동 생성하지 않는다.
* **Current JSON Keys:** `artist`, `title`, `album`, `primary_genres`, `secondary_genres`, `microgenres`, `genre_source`, `rym_rating`, `pitchfork_score`, `metacritic_score`, `aoty_score`, `critics_summary`, `interview_summary`, `listening_guide`, `samples_used`, `missing_sources`, `reliability_notes`.

### 3.3 [Feature 3] Lyrics & AI Translation (lyrics.ovh + Gemini)
* **Description:** 현재 구현 범위에서는 `lyrics.ovh`로 현재 곡의 원문 가사를 조회한다. Gemini 번역은 후속 기능으로 유지한다.
* **Process:**
    1.  `lyrics.ovh` API 호출.
    2.  원문 가사를 상세 화면에 표시.
    3.  조회 실패 시 앨범 분석과 분리된 섹션 단위 오류 상태를 표시.

### 3.4 [Feature 4] Database & Archiving (Room DB)
* **Schema (ListenHistory):**
    * `id` (Primary Key)
    * `timestamp` (Date/Time)
    * `artist_name`, `album_title`
    * `primary_genre`, `secondary_genre` (정규화된 앨범 기준 대표 장르)
    * `genre_candidates_json` (Gemini의 앨범 기준 장르 후보와 신뢰도 보존)
    * `rym_rating`, `pitchfork_score`, `metacritic_score`, `aoty_score` (있는 출처만 저장)
    * `missing_sources`, `reliability_notes` (표시하지 않은 출처와 신뢰도 주의사항)
    * `review_summary`
    * 원문 가사는 곡 단위 `lyrics_cache`에 `artist + track_title` 기준으로 별도 저장
    * `translated_lyrics`는 후속 번역 기능에서 곡 단위 캐시로 추가 예정
* **Functional Requirements:** '저장' 버튼 클릭 시 현재 조회된 모든 데이터를 Local DB에 커밋.

### 3.5 [Feature 5] Preference Visualization (Charts)
* **Description:** 저장된 장르 데이터를 분석하여 사용자 취향 통계를 생성한다.
* **UI Components:**
    * `Genre Distribution`: 전체 보관함 기준 장르 분포.
    * `Archive KPI`: 총 보관 앨범 수와 최다 장르.
    * 기간별 변화 차트는 실제 timestamp 기반 집계가 추가된 뒤 후속으로 연결한다.

### 3.6 [Feature 6] Taste Exploration Map (Genre Adjacency Map)
* **Description:** 사용자의 저장 기록을 기반으로 장르를 독립 카테고리가 아닌 연결된 공간으로 표현하고, 취향의 인접 영역 및 확장 가능성을 시각화한다.
* **Core Framing Constraint:** 본 기능은 "다음 곡 추천"이 아니라 "취향 탐험 경로 안내"를 목표로 한다.
* **Functional Requirements:**
    * 초기 상태에서 탐험 지도는 비활성(빈 노드) 상태로 시작한다.
    * 곡 저장 시 식별된 앨범의 장르 후보를 표준 장르 사전에 매핑한다.
    * 신뢰도 기준을 통과하고 표준 장르 사전에 존재하는 후보만 활성 장르로 저장한다.
    * 활성 장르와 직접 연결된 1-hop 주변 노드만 화면에 표시한다.
    * 알 수 없는 장르, 사전에 없는 장르, 활성 장르와 직접 연결되지 않은 노드는 화면에 표시하지 않는다.
    * 장르 노드를 선택하고 `근처 장르 찾기`를 누르면 Gemini Search grounding으로 주변 장르와 연관성 강도를 조회한다.
    * 검색 결과는 팝업 리스트로 먼저 표시하며, 사용자가 후보를 선택하고 `선택 반영`을 눌렀을 때만 주변 노드와 연결선을 세션 지도에 추가한다.
    * 팝업 리스트는 체크박스, 왼쪽 장르명, 오른쪽 연관성(`강함`, `보통`, `약함`)을 표시한다.
    * 새 주변 노드는 기존 노드와 겹치지 않도록 선택 장르 주변의 후보 위치를 탐색해 배치한다.
    * 장르 간 연결선은 화살표 없이 표시하고, 연관성 강도에 따라 굵기와 투명도를 조절한다.
    * 반복 저장 횟수는 기존 영향선을 새로 만들지 않고 시각적 강도와 우선순위만 보정한다.
* **User-visible Outputs:**
    * 현재 취향 흐름 예시(예: Hip-hop -> Jazz Rap -> Neo Soul)
    * 새 장르 활성화 알림
    * 활성 장르 / 직접 연결된 주변 후보 구분 시각화
* **Logical Constraints:**
    * 추천 어휘("추천", "다음 곡")보다 탐험 어휘("연결", "확장", "탐험")를 우선 사용해야 한다.
    * 앨범 저장 시에는 AI가 반환한 자유 텍스트 장르 또는 영향 관계를 검증 없이 그래프에 삽입하지 않는다.
    * 사용자 주도 검색 결과는 팝업 미리보기 상태로 유지하고, 사용자가 명시적으로 반영하기 전까지 지도에 추가하지 않는다.
    * `Unknown`, 빈 장르명, 선택 장르 자기 자신, 중복 후보, 연관성 기준 미달 후보는 표시하지 않는다.
    * MVP 지도는 1-hop만 표시한다. 2-hop 확장은 후속 검토 대상으로 둔다.

---

## 4. Technical Architecture (기술 설계)

### 4.1 Android Stack
* **UI:** Jetpack Compose (Declarative UI)
* **Architecture:** MVVM + Clean Architecture (Domain, Data, UI Layers)
* **Dependency Injection:** Hilt
* **Networking:** Retrofit2, OkHttp3
* **Local DB:** Room Persistence Library

### 4.2 API Integration Flow
[Catch Music] -> [Extract Artist/Title] -> [Identify Album via Gemini] -> [Fetch Album-based Metadata + Genre Candidates] -> [lyrics.ovh Raw Lyrics] -> [Archive Commit] -> [Map Active Genre Display] -> [User Selects Genre] -> [Find Nearby Genres via Gemini] -> [Popup Preview] -> [Apply To Session Map]

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
    사용자가 매 곡마다 '정보 가져오기'를 누르면 Gemini 비용이 커질 수 있습니다. 특히 같은 앨범의 여러 곡을 반복 조회하면 앨범 평점/장르 정보가 중복 요청될 수 있습니다.
    * *Countermeasure:* 구현된 캐시 계층은 앨범 기준 메타데이터를 기존 Archive row에서 `artist + album_title` 우선으로 조회하고, 원문 가사는 `lyrics_cache`에서 `artist + track_title` 키로 재사용한다. 후속 번역 기능을 추가할 경우 번역 결과도 곡 단위로 별도 캐싱합니다.

3.  **Genre Ambiguity (장르의 모호성):**
    Gemini가 반환하는 앨범 장르는 매번 조금씩 다를 수 있습니다 (예: 'Synth-pop' vs 'Electronic Pop'). 이를 그대로 통계에 넣으면 그래프가 지저분해집니다.
    * *Countermeasure:* 앱 내부적으로 사람이 검수한 '표준 장르 사전'을 정의한다. Gemini는 장르 후보와 신뢰도만 반환하고, 사전에 없는 후보는 그래프에서 제외한다.

4.  **Background Limitation (OS 제약):**
    Android 12 이후 백그라운드 서비스 제약이 강화되었습니다. `NotificationListenerService`가 시스템에 의해 킬(Kill)당할 경우를 대비해, 포그라운드 서비스(Foreground Service) 활용 및 권한 안내 가이드를 철저히 설계해야 합니다.

5.  **Information Hallucination (정보 환각):**
    RYM, Pitchfork, Metacritic, AOTY 평점은 앨범 기준으로만 취급한다. Gemini가 확인되지 않은 점수를 만들어낼 위험이 있으므로, 출처별 값은 실제로 확인 가능한 경우에만 표시하고 없으면 `null`로 둔다. '이 정보는 AI에 의해 생성되었으며 실제와 다를 수 있음'이라는 면책 조항은 디자인적으로 필수입니다.

6.  **Exploration Graph Integrity (탐험 그래프 무결성):**
    AI가 장르 간 영향선을 자동으로 누적하면 그럴듯하지만 검증되지 않은 계보가 쌓일 수 있습니다.
    * *Countermeasure:* 앨범 저장 시 자동 확장을 금지한다. 사용자가 `근처 장르 찾기`를 눌렀을 때만 Gemini 검색을 실행하고, 결과를 팝업 후보로 보여준 뒤 명시적 반영 액션을 요구한다.

7.  **Cold Start Map (초기 지도 공백):**
    신규 사용자는 저장 이력이 적어 지도가 과도하게 비어 보일 수 있습니다.
    * *Countermeasure:* 첫 활성 장르가 생기기 전에는 빈 상태를 유지한다. 활성 장르가 생긴 뒤에만 사전에서 직접 연결된 1-hop 주변 후보를 표시한다.

---

**[End of Document]**
