# vinfo 🎵

> **Vinyl + Information** — 지금 재생 중인 음악의 모든 정보를 자동으로 수집하는 Android 아카이브 앱

---

## 📌 프로젝트 개요

vinfo는 Android 알림 리스너를 통해 현재 재생 중인 음악의 아티스트명과 곡명을 감지하고,
Gemini API와 lyrics.ovh를 활용해 해당 곡이 수록된 앨범을 식별한 뒤 아래 정보를 자동으로 수집·저장한다.

- 앨범 기준 평론 및 RYM/Pitchfork/Metacritic/AOTY 평점
- 앨범 기준 Primary/Secondary 장르 분류
- 감상 포인트 (Listening Guide)
- 샘플링 출처
- `lyrics.ovh` 원문 가사 조회

수집된 데이터는 로컬 Room DB에 저장되며, 장르별 청취 통계 시각화 기능을 제공한다.

---

## 🛠 Tech Stack

| 분류 | 라이브러리 / 도구 |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | Clean Architecture + MVVM + UDF |
| DI | Hilt |
| Async | Coroutines + Flow |
| Network | Retrofit2 + OkHttp3 |
| Local DB | Room |
| Navigation | Navigation Compose |
| Media 감지 | NotificationListenerService |
| 외부 API | Gemini API, lyrics.ovh |
| 시각화 | Vico |

---

## 🏗 Architecture

```
Presentation (UI + ViewModel)
        ↓ Intent
    Domain (UseCase + Repository Interface)
        ↓ implements
    Data (RepositoryImpl + Remote/Local Source)
```

- **Presentation Layer:** Jetpack Compose UI, ViewModel, UiState
- **Domain Layer:** UseCase, Repository Interface, Domain Model (순수 Kotlin, Android 의존성 없음)
- **Data Layer:** Retrofit API, Room DB, Mapper, Interceptor

자세한 설계는 [`docs/SDD.md`](docs/SDD.md)를 참고한다.

---

## 📁 Project Structure

```
vinfo/
├── di/                   # Hilt DI 모듈 (Network, Database, Repository, Dispatcher)
├── domain/
│   ├── model/            # TrackArchive, GenreCategory, GenreStat
│   ├── repository/       # Repository Interface 정의
│   └── usecase/          # GetTrackInformationUseCase, SaveArchiveUseCase 등
├── data/
│   ├── local/            # Room DB, ArchiveDao, ArchiveEntity
│   ├── remote/           # Gemini / Lyrics API Service + DTO
│   ├── interceptor/      # JsonSanitizerInterceptor
│   ├── mapper/           # DTO ↔ Domain Model 변환
│   └── repository/       # Repository 구현체
├── service/              # ActiveMediaMonitorService
├── ui/
│   ├── navigation/       # Route, NavGraph
│   ├── nowplaying/       # 현재 재생 화면
│   ├── detail/           # 상세 정보 화면
│   ├── archive/          # 아카이브 목록/상세
│   ├── stats/            # 장르 통계 시각화
│   ├── settings/         # 설정
│   └── component/        # 공통 Composable (ShimmerCard, LyricsCard 등)
├── common/               # AppResult, 확장 함수, Dispatcher Qualifier
├── docs/
│   ├── SRS.md
│   └── SDD.md
└── VinfoApplication.kt
```

---

## 🔑 API Keys 설정

`local.properties`에 아래 키를 추가한다. **절대 Git에 커밋하지 않는다.**

```properties
GEMINI_API_KEY=your_gemini_key_here
```

`build.gradle.kts`에서 Gradle Secrets Plugin으로 `BuildConfig`에 주입한다.

```kotlin
// build.gradle.kts (app)
plugins {
    id("com.google.android.libraries.mobileads.secrets-gradle-plugin")
}
```

---

## ⚙️ 권한 설정

앱 최초 실행 시 **알림 접근 권한**이 필요하다.

```xml
<!-- AndroidManifest.xml -->
<service
    android:name=".service.ActiveMediaMonitorService"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService" />
    </intent-filter>
</service>
```

권한이 없을 경우 `NowPlayingScreen`에서 시스템 설정으로 안내하는 UI를 렌더링한다.

---

## 🔄 데이터 수집 파이프라인

```
MediaMonitor 곡 감지
        ↓
DetailViewModel → UI: Loading (Shimmer)
        ↓
[Job A] GeminiAPI.identifyAlbumAndMetadata() ──┐
[Job B] LyricsAPI.getRawLyrics()               ──┤ 병렬 실행 (async)
        ↓ Job A 완료                   │
Album MetadataUiState.Success 즉시 렌더링 │
        ↓ Job B 완료                   │
LyricsUiState.Success 렌더링      ◄───┘
        ↓
ArchiveRepository.save() → Room DB 저장
```

---

## 🧱 핵심 설계 결정

| 결정 | 이유 |
|---|---|
| 앨범 기준 메타데이터 계약 | 곡명/아티스트는 앨범 식별 입력으로만 사용하고 장르/평점/평론은 앨범 기준으로 통일 |
| Gemini JSON 계약 | `responseMimeType = "application/json"` 요청, 앨범 기준 평점 키, `missing_sources`/`reliability_notes` 포함 |
| JSON Parser 방어 로직 | Gemini 래퍼/원시 JSON 대응, nullable 평점 보존, 파싱 안정성 확보 |
| UI State 분리 (`MetadataState` / `LyricsState`) | Compose 불필요한 Recomposition 방지 |
| `AppResult<T>` 공통 래퍼 | 계층 간 에러 전파 표준화, 부분 실패 허용 |
| 원문 가사 nullable 필드 | lyrics.ovh 미수록 곡 대응 |
| DB Migration 전략 초기 수립 | 아카이브 데이터 소실 방지 |

---

## 📋 TODO (Prototype)

- [x] `ActiveMediaMonitorService` 구현 및 미디어 알림 감지 연결
- [x] Gemini 앨범 식별/메타데이터 JSON 계약 정리
- [x] Gemini 앨범 기준 평점 JSON 파싱 및 상세 화면 표시
- [x] lyrics.ovh 원문 가사 조회 연동
- [ ] 필요 시 Gemini 번역 연동
- [ ] Room DB 스키마 + DAO 구현
- [x] `DetailScreen` UI (앨범 메타데이터 + 원문 가사 렌더링)
- [x] `ArchiveListScreen` 구현
- [x] `GenreMapScreen` 전체 화면 pan/zoom 구현
- [ ] 검수된 `GenreDictionary` 리소스 분리 및 Gemini 장르 후보 배열 연동
- [ ] Hilt DI 모듈 전체 연결
- [ ] 알림 권한 온보딩 플로우

---

## 📄 문서

- [Software Design Document (SDD)](docs/SDD.md)

---

## 📝 License

Personal / Non-commercial use only.  
상용화 전환 시 외부 API 이용 약관을 별도로 검토할 것.

## 🧪 Harness (AI 도구 통합)

- 로컬 체크: `flake8 .` 및 `mypy .` 실행으로 스타일과 타입 검사를 수행합니다.
- 도구 설치:

```bash
pip install -r requirements.txt
```

- PR 규칙: `flake8`/`mypy` 실패 시 병합 금지, AI가 생성한 변경은 PR에 `AI-ASSISTED` 표기

자세한 가이드는 [docs/harness/AI_Workflow.md](docs/harness/AI_Workflow.md) 참조.
