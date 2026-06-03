# Vinfo 프로젝트 남은 과제 (Remaining Tasks)

이 문서는 Vinfo 프로젝트의 현재 구현 상태와 앞으로 진행해야 할 핵심 과제들을 정리한 문서입니다.

## 1. 외부 API 연동 및 데이터 처리 (High Priority)
- [x] **Gemini API 연동**: 앨범 기준 AI 음악 분석 데이터 생성
- [x] **lyrics.ovh 원문 조회 연동**: 현재 재생 곡의 원문 가사를 상세 화면에 표시
- [ ] **가사 번역**: 필요 시 Gemini 번역 파이프라인과 번역 탭 추가
- [x] **Gemini 앨범 식별 계약 정리**: 감지된 `artist + title`로 수록 앨범을 찾고, 장르/평론/평점은 앨범 기준으로만 반환
- [x] **출처별 앨범 평점 모델 확장**: RYM, Pitchfork, Metacritic, AOTY 중 확인 가능한 값만 저장/표시
- [x] **LLM Parser 구현**: Gemini 응답(JSON)의 안정적인 파싱 및 앨범 기준 데이터 모델 변환 로직
- [x] **앨범 메타데이터 영속화 확장**: Room Archive 스키마에 출처별 평점, `missing_sources`, `reliability_notes` 저장 컬럼 반영

## 2. 미디어 감지 서비스 (Medium Priority)
- [x] **ActiveMediaMonitorService 구현**: 타 앱(Spotify, YouTube Music 등) 재생 정보 실시간 감지
- [x] **권한 관리**: Notification Listener 권한 요청 및 상태 체크 로직 구현 (배너 및 설정 열기 포함)

## 3. UI/UX 고도화 (Low Priority)
- [x] **로딩 상태 UI(Skeleton/Shimmer) 적용**: 데이터 로딩 중 사용자 경험 개선
- [ ] **애니메이션 효과**: 화면 전환 및 리스트 인터랙션 애니메이션 추가

### 3.1 Taste Exploration / Genre Map
- [x] `GenreMapScreen` 구현 (Compose): 전체 화면 pan/zoom, 노드/엣지 렌더링, 툴팁, 접근성
- [x] 알 수 없는 장르, 사전 미등록 장르, 직접 연결되지 않은 노드를 화면 상태에서 제외
- [x] 사용자 주도 `근처 장르 찾기` 팝업: 왼쪽 장르명, 오른쪽 연관성 표시
- [x] 검색 후보 `지도에 반영` 후 세션 지도에 주변 노드와 강도별 연결선 추가
- [x] 주변 장르 후보 필터/파서/reducer 단위 테스트
- [x] Gemini 장르 응답을 `primary_genres`, `secondary_genres`, `microgenres` 후보 배열 + 신뢰도로 확장
- [ ] 검수된 `GenreDictionary`를 버전 관리되는 JSON/Kotlin 리소스로 분리
- [ ] `NormalizeAlbumGenreCandidatesUseCase`, `GetVisibleGenreFlowUseCase` 구현 및 단위 테스트
- [x] Room Archive에 `genre_candidates_json` 저장 컬럼 추가
- [ ] 주변 장르 검색 결과 Room 캐시 및 검수 큐 설계
- [ ] UI 통합 테스트: 활성 노드 + 직접 연결된 1-hop 후보만 표시, unknown 비노출, pan/zoom 플로우

## 4. 아키텍처 및 품질 개선
- [ ] **Clean Architecture 적용**: Domain(UseCase) 및 Data(Repository) 계층 분리
- [ ] **Unit Test 작성**: 주요 비즈니스 로직(통계 계산, 파서 등) 검증

---

추가 요청 시 우선순위를 재조정하고 작업 항목을 세분화합니다.
