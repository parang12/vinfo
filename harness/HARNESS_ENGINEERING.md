# vinfo Harness Engineering Guide

문서 버전: 1.0  
작성일: 2026-05-14  
적용 범위: vinfo 프로젝트 전반 (SRS/SDD/UI/코드/테스트)

---

## 1. 목적

이 문서는 vinfo 프로젝트에서 하네스 엔지니어링 방식을 일관되게 적용하기 위한 기준 문서다.

하네스 엔지니어링의 목표는 다음과 같다.

- 요구사항부터 결과물까지 재현 가능한 실행 경로를 만든다.
- 변경이 생겨도 같은 입력에서 같은 품질의 출력이 나오도록 통제한다.
- 기능 구현 이전에 검증 가능한 계약(입력/출력/평가)을 먼저 정의한다.
- 문서 중심 개발(SRS -> SDD -> UI -> 코드)을 자동화 친화적으로 운영한다.

---

## 2. 핵심 원칙

### 2.1 Contract First

구현 전에 반드시 계약을 먼저 정의한다.

- 입력 계약: 어떤 데이터가 들어오는가
- 출력 계약: 어떤 형태로 결과를 보장하는가
- 실패 계약: 실패 시 무엇을 반환하고 어떻게 복구하는가

### 2.2 Deterministic Pipeline

가능한 모든 단계는 결정적으로 동작해야 한다.

- 동일 입력 + 동일 버전 = 동일 출력
- 버전이 바뀌면 변경 로그를 강제 기록
- 랜덤 요소는 시드 고정 또는 배제

### 2.3 Observable by Default

모든 실행은 추적 가능해야 한다.

- 실행 ID
- 입력 스냅샷
- 출력 스냅샷
- 품질 지표
- 실패 원인

### 2.4 Document-Driven

문서가 구현을 리드해야 한다.

- SRS는 무엇을 만든다를 정의
- SDD는 어떻게 만든다를 정의
- UI_v3는 무엇을 보여준다를 정의
- 하네스는 어떻게 검증한다를 정의

---

## 3. 디렉토리 구조

권장 하네스 구조:

- harness/
- harness/specs/
- harness/scenarios/
- harness/checklists/
- harness/logs/
- harness/reports/

현재 프로젝트에는 최소 시작점으로 다음 파일을 포함한다.

- harness/HARNESS_ENGINEERING.md

---

## 4. vinfo 적용 대상

하네스는 아래 4개 트랙에 적용한다.

### 4.1 문서 정합성 트랙

대상 문서:

- docs/vinfo_SRS_v2.md
- docs/vinfo_SDD_final.md
- docs/UI_v3.md

검증 포인트:

- 요구사항 추적성 (SRS 항목이 SDD/UI에 반영되었는가)
- 용어 일관성 (예: Taste Map, Insight Hub, Genre Adjacency)
- 정책 일치성 (권한, 실패 처리, AI 고지)

### 4.2 UI/UX 트랙

대상:

- 화면 구조
- 상태 전이
- 카피 톤

검증 포인트:

- 추천 중심 문구가 탐험 중심 문구로 유지되는가
- 빈 상태/오류 상태가 규칙대로 노출되는가
- 접근성 라벨이 주요 컴포넌트에 포함되는가

### 4.3 데이터/도메인 트랙

대상:

- 장르 정규화
- 인접 장르 계산
- 저장 이력 기반 확장 경로

검증 포인트:

- 같은 입력에서 같은 탐험 경로가 도출되는가
- Unknown/Null 데이터 처리 규칙이 유지되는가
- Cold Start 시 탐험 시작점 전략이 동작하는가

### 4.4 릴리즈 품질 트랙

대상:

- 빌드/테스트
- 회귀 리스크
- 문서-코드 드리프트

검증 포인트:

- 핵심 시나리오 스모크 통과
- 문서 변경 후 구현 갭 보고
- 릴리즈 전 체크리스트 완료

---

## 5. 하네스 시나리오 템플릿

각 시나리오는 다음 포맷을 사용한다.

### 5.1 시나리오 메타

- Scenario ID:
- Scope:
- Owner:
- Priority: P0/P1/P2
- Input Version:

### 5.2 Given / When / Then

- Given: 초기 상태
- When: 실행 액션
- Then: 기대 결과

### 5.3 검사 항목

- Functional Check
- State Check
- Copy Check
- Accessibility Check
- Logging Check

### 5.4 판정

- Pass / Fail
- 실패 원인
- 재현 단계
- 후속 액션

---

## 6. Taste Map 전용 하네스

### 6.1 필수 시나리오

1) Cold Start Map

- Given: 저장 이력 0건
- When: Insight Hub > Taste Map 진입
- Then: 빈 지도 + 탐험 시작점 문구 노출

2) First Save Activation

- Given: 저장 이력 0건
- When: Hip-hop 계열 트랙 1건 저장
- Then: 중심 노드 활성화 + 인접 노드 표시

3) Expansion Path Update

- Given: Hip-hop, Jazz Rap 누적 저장
- When: Neo Soul 계열 추가 저장
- Then: 흐름 요약이 Hip-hop -> Jazz Rap -> Neo Soul로 갱신

4) New Area Unlock

- Given: 기존 경로 외 장르 저장
- When: Dream Pop 계열 저장
- Then: New Area Unlocked 이벤트 노출

5) Failure Fallback

- Given: 인접 맵 계산 실패
- When: Taste Map 렌더 요청
- Then: 지도 영역 에러 메시지 + 재시도 액션 제공

### 6.2 품질 지표

- Map Render Success Rate
- Node Activation Accuracy
- Adjacency Consistency
- Copy Compliance (탐험 문구 비율)
- Recovery Success Rate

---

## 7. 문서 하네스 체크리스트

문서 변경 시 아래를 반드시 확인한다.

- SRS 변경이 SDD에 반영되었는가
- SDD 변경이 UI_v3에 반영되었는가
- UI_v3 변경이 구현 노트와 충돌하지 않는가
- 용어 변경 이력이 Change Log에 기록되었는가
- 레거시 문서에 우선 참조 문서 안내가 있는가

---

## 8. 운영 절차

### 8.1 변경 시작

1. 변경 목표 정의
2. 영향 범위 문서 식별
3. 하네스 시나리오 등록

### 8.2 변경 실행

1. 문서/설계 갱신
2. 구현 반영
3. 시나리오 실행
4. 리포트 기록

### 8.3 변경 종료

1. 실패 항목 처리
2. 잔여 리스크 명시
3. 릴리즈 체크리스트 업데이트

---

## 9. 리포트 포맷

리포트 최소 항목:

- Run ID
- 실행 시간
- 시나리오 개수
- Pass/Fail 개수
- 주요 실패 3건
- 리스크 요약
- 권장 다음 액션

---

## 10. vinfo 팀 규칙

- 탐험 중심 프레이밍을 깨는 카피는 차단한다.
- 문서보다 코드가 앞서지 않도록 한다.
- 하네스 없는 신규 핵심 기능은 병합하지 않는다.
- 실패를 숨기지 않고 재현 정보를 남긴다.

---

## 11. 다음 확장 계획

- harness/scenarios/taste_map_scenarios.md 생성
- harness/checklists/release_checklist.md 생성
- harness/reports/report_template.md 생성
- CI 단계에서 문서 정합성 점검 자동화

---

End of Document
