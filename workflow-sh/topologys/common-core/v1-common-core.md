# Pinned Topology — `common-core` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (구체 클래스명·패키지 경로는 여기 없다)

---

## 0. 유효 구간 (Validity)

| 항목 | 값 |
| --- | --- |
| Pinned at | 2026-06-15 |
| Valid for | 본 워크플로우 v1 구간 — re-pin trigger 발생 전까지 |
| Owner | 메인 Claude + 사용자 합의 |
| Re-pin trigger | 아래 §4 |

---

## 1. How Claude Code must use this file — 행동 계약

1. **공통 모듈 변경 작업 시 이 파일을 먼저 읽는다.** §2의 graph는 본 구간 동안 **고정 제약**.
2. **이 topology 안에서 어휘만 채운다.** 클래스명·메서드·예외 타입은 자유롭게 만들되, 공통 모듈의 역할 / BC와의 의존 방향은 건드리지 않는다.
3. **공통 모듈에 BC를 import해야 할 것 같으면 STOP하고 보고한다.** 그건 노드·엣지의 위반이다.
4. **공통 모듈이 비즈니스 규칙을 품기 시작하는 정황을 발견하면 보고**한다.
5. **이 파일에 어휘를 추가하지 않는다.** 공통 모듈의 실제 구성은 §3 포인터로.

---

## 2. The pinned topology

### Nodes
- `공통 예외 기반` — 비즈니스 오류의 공통 상위 타입
- `ErrorCode 레지스트리` — 시스템 전역 사용자 노출 오류 코드의 단일 진실 소스
- `전역 예외 변환점` — 모든 비즈니스/검증 예외를 HTTP 응답으로 변환하는 단일 진입점
- `Audit 설정` — 생성/수정 시각 자동 주입 메커니즘
- `공통 인프라 설정` — 트랜잭션·JPA·Auditing 등 cross-BC 인프라 구성
- `Bounded Context` — 공통 모듈의 클라이언트(소비자)

### Edges
- `Bounded Context` → `공통 예외 기반` : 상속/구현 (의존)
- `Bounded Context` → `ErrorCode 레지스트리` : 코드 참조
- `Bounded Context` (throw) ↪ `전역 예외 변환점` (catch) → `Error envelope` (참조: api-spec topology)
- `Bounded Context` → `Audit 설정` · `공통 인프라 설정` : 적용 받음
- `공통 모듈` → `Bounded Context` : **금지** (역방향 의존 없음)

### Boundaries
- **의존 방향 경계**: 공통 모듈은 BC를 import하지 않는다. 일방향 의존만 존재
- **책임 경계**: HTTP 변환 / 시간 주입 / 트랜잭션 설정은 공통. 비즈니스 규칙은 BC
- **에러 출구 경계**: 사용자 노출 오류는 단일 `전역 예외 변환점`을 거치지 않은 경로가 없다
- **레지스트리 경계**: 등록되지 않은 비즈니스 오류 코드를 throw하지 않는다

### Invariants
- 공통 모듈 → BC 의존 0건
  - 감지법: ArchUnit 또는 import 패키지 grep
- 새로 도입되는 사용자 노출 비즈니스 오류는 `ErrorCode 레지스트리`에 먼저 등록된다
  - 감지법: PR 리뷰 체크리스트 + 새 throw 추가 시 레지스트리 변경 동반 여부 확인
- Controller에서 비즈니스 예외를 직접 try-catch하지 않는다 (전역 변환점만 사용)
  - 감지법: 컨트롤러 slice 테스트 + 코드 리뷰
- 외부에서 audit 시각을 주입하지 않는다 (`Audit 설정`이 단일 출처)
  - 감지법: 도메인/Application Service의 setter 호출 grep

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 공통 모듈 패키지 구조 · 클래스 이름 → `docs/PACKAGE.md`
- 실제 `공통 예외 기반` · `전역 예외 변환점` · `ErrorCode 레지스트리` 구현 → `Common/Exception/` 하위 코드
- 실제 `Audit 설정` · `공통 인프라 설정` → `Common/Config/` 하위 코드
- 코드 작성 규칙(상속·등록 절차) → `.claude/rules/conventions.md` §2.3 ~ §2.5
- 왜 이렇게 박혔는지 → 향후 ADR 작성 시 `docs/adr/`

---

## 4. Re-pin trigger

- 공통 모듈에 BC-specific 책임이 침투해야만 풀 수 있는 문제가 발생
- `전역 예외 변환점` 단일성을 깨야 하는 흐름 도입 (예: 비동기 이벤트 핸들러의 별도 변환)
- `ErrorCode 레지스트리` 단일성이 깨질 필요 (예: BC별 레지스트리 분할)
- audit 시간 주입 책임의 외부화 (예: 클라이언트 시각 신뢰)
- 인프라 설정의 BC별 분기 필요
