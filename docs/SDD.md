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

