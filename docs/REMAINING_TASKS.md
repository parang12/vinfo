# Vinfo 프로젝트 남은 과제 (Remaining Tasks)

이 문서는 Vinfo 프로젝트의 현재 구현 상태와 앞으로 진행해야 할 핵심 과제들을 정리한 문서입니다.

## 1. 외부 API 연동 및 데이터 처리 (High Priority)
- [x] **Perplexity API 연동**: 실시간 아티스트/앨범 정보 검색 및 비평가 요약 데이터 확보
- [ ] **Gemini API 연동**: 가사 번역 및 AI 기반 음악 분석 데이터 생성
- [ ] **LLM Parser 구현**: AI 응답(JSON)의 안정적인 파싱 및 데이터 모델 변환 로직

## 2. 미디어 감지 서비스 (Medium Priority)
- [x] **ActiveMediaMonitorService 구현**: 타 앱(Spotify, YouTube Music 등) 재생 정보 실시간 감지
- [ ] **권한 관리**: Notification Listener 권한 요청 및 상태 체크 로직 구현

## 3. UI/UX 고도화 (Low Priority)
- [x] **로딩 상태 UI(Skeleton/Shimmer) 적용**: 데이터 로딩 중 사용자 경험 개선
- [ ] **애니메이션 효과**: 화면 전환 및 리스트 인터랙션 애니메이션 추가

### 3.1 Taste Exploration / Genre Map
- [ ] `GenreMapScreen` 구현 (Compose): 노드/엣지 렌더링, 툴팁, 접근성
- [ ] `GenreExplorationRepository` 인터페이스 및 Room DAO 구현
- [ ] `ComputeGenreAdjacencyUseCase` 구현 및 단위 테스트
- [ ] UI 통합 테스트: 노드 활성화/잠금 해제 플로우

## 4. 아키텍처 및 품질 개선
- [ ] **Clean Architecture 적용**: Domain(UseCase) 및 Data(Repository) 계층 분리
- [ ] **Unit Test 작성**: 주요 비즈니스 로직(통계 계산, 파서 등) 검증

---

추가 요청 시 우선순위를 재조정하고 작업 항목을 세분화합니다.
