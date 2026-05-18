# AI 하네스 워크플로우

목표: AI 보조 코드 리뷰, 테스트 생성, 프롬프트 기반 개선을 통해 개발 생산성과 코드 품질을 유지한다.

- 로컬 개발자 워크플로우
  - `pip install -r requirements.txt` (또는 `requirements-dev.txt`)로 도구 설치
  - 코드 변경 전 `flake8 .`로 스타일/문제 검사
  - 타입 체크: `mypy .`
  - 자동화: 필요시 `pre-commit` 훅에 `flake8`/`mypy` 추가

- CI 파이프라인
  - PR에서 `flake8`과 `mypy`를 실행하여 실패 시 병합 차단
  - 선택: `ruff`/`isort`로 포맷 정리

- AI 통합 지점
  - PR 설명에 AI가 생성한 요약 또는 테스트 케이스를 붙임
  - AI가 생성한 코드/패치에 대해 반드시 사람이 리뷰
  - 민감한 키/시크릿을 AI에게 제공하지 않음

파일 위치
- flake8 설정: `.flake8`
- mypy 설정: `mypy.ini`
- 하네스 가이드: `docs/harness/*`

명령 예시

```bash
# 스타일 검사
flake8 .

# 타입 검사
mypy .
```
