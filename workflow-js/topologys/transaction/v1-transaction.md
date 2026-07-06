# Pinned Topology — `transaction` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 `@Transactional` 위치·isolation 상수는 여기 없음)

---

## 0. 유효 구간 (Validity)

| 항목 | 값 |
| --- | --- |
| Pinned at | 2026-07-03 |
| Valid for | 본 워크플로우 v1 구간 — re-pin trigger 발생 전까지 |
| Owner | 메인 Claude + 사용자 합의 |
| Re-pin trigger | 아래 §4 |

---

## 1. How Claude Code must use this file — 행동 계약

1. **Application Service 또는 트랜잭션 관련 코드 작성 시 이 파일을 먼저 읽는다.** §2의 graph는 본 구간 **고정 제약**.
2. **트랜잭션 경계는 항상 Application Service 층에만 둔다.** Domain·Presentation·Infrastructure에 `@Transactional` 없음.
3. **동기 도메인 이벤트 (`ApplicationEventPublisher`)는 발행자 트랜잭션 안에서 실행되어야 한다.** `@Async`, `@TransactionalEventListener` 금지.
4. **BC 간 협력은 트랜잭션 원자성 유지가 원칙.** 부분성공 불허.
5. **isolation 명시 변경은 이 파일 § re-pin 사유.** 기본값 READ_COMMITTED 유지, 예외는 명시 논의.
6. **OSIV=false 는 고정 제약.** application.yml 변경 시 STOP + 보고.

---

## 2. The pinned topology

### Nodes
- `트랜잭션 경계` — Application Service의 `@Transactional` 어노테이션 위치
- `읽기 경계` — `@Transactional(readOnly = true)`로 명시된 조회 트랜잭션
- `쓰기 경계` — 기본 `@Transactional` (readOnly 미지정)
- `전파 정책` — Propagation.REQUIRED 기본. 예외적 `REQUIRES_NEW`는 이력·감사 append 용도로만
- `isolation 레벨` — READ_COMMITTED 기본. 필요 시 명시적 변경 (근거 문서화)
- `동기 이벤트 발행` — `ApplicationEventPublisher.publishEvent(...)` 발행자 트랜잭션 안에서
- `동기 이벤트 수신` — `@EventListener` 발행자 트랜잭션에 참여, 실패 시 rollback 전파
- `롤백 정책` — 검사 예외·검증 실패 모두 롤백. `noRollbackFor` 사용 금지
- `OSIV` — false 고정 (Open Session In View 비활성)
- `DDL·마이그레이션 트랜잭션` — Flyway가 관리 (앱 코드와 분리)

### Edges
- `트랜잭션 경계` → `쓰기 경계` : Command Service default
- `트랜잭션 경계` → `읽기 경계` : Query Service default (`readOnly=true`)
- `쓰기 경계` → `동기 이벤트 발행` : 상태 변경 후 즉시 발행
- `동기 이벤트 발행` → `동기 이벤트 수신` : 같은 트랜잭션 참여 (`@EventListener`, `@Async` 금지)
- `동기 이벤트 수신` 실패 → 발행자 `쓰기 경계` rollback
- `Repository 호출` → `트랜잭션 경계` 안에서만 (Aggregate → Repository 금지)
- `DDL·마이그레이션 트랜잭션` ⊥ `트랜잭션 경계` : 앱 코드와 분리

### Boundaries
- **레이어 경계**: `@Transactional`은 Application Service에만. Domain·Presentation·Infrastructure에 X.
- **전파 경계**: 기본 REQUIRED. `REQUIRES_NEW`는 이력/감사 append 용도로만 (예: history repository 호출).
- **읽기 경계**: Query Service는 항상 `@Transactional(readOnly = true)`. QueryDSL/JPA 최적화 · Hibernate flush skip.
- **BC 간 경계**: 동기 이벤트로만 협력. 발행자 트랜잭션 원자성 유지. cross-BC repository 직접 호출은 예외 (이벤트 핸들러 안의 조회는 허용).
- **OSIV 경계**: false 고정. Presentation 층에서 lazy loading 금지 (DTO 팩토리로 미리 변환).
- **롤백 경계**: 모든 예외에서 rollback. `noRollbackFor`, `Exception 흡수 후 흐름 계속` 금지.

### Invariants
- Application Service의 public 메서드는 `@Transactional` 또는 `@Transactional(readOnly = true)` 중 하나를 갖는다
  - 감지법: Application Service `@Service` + public method grep, `@Transactional` 부재 시 리뷰
- Domain / Presentation 클래스에 `@Transactional` 0건
  - 감지법: 패키지 grep
- `@Async`, `@TransactionalEventListener` 0건
  - 감지법: grep
- Query Service의 public 메서드는 `readOnly = true` 100%
  - 감지법: `*QueryService.java` grep
- `noRollbackFor` 사용 0건
  - 감지법: grep
- application.yml `spring.jpa.open-in-view: false` 유지
  - 감지법: config diff
- 동기 이벤트 핸들러는 `@Async` 없음, `@TransactionalEventListener` 없음 (오직 `@EventListener`)
  - 감지법: `application/event/*Handler.java` grep

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 `@Transactional` 위치 목록 · isolation 상수 값 → `living-docs/transaction-map/`
- 실제 이벤트 핸들러 목록 → `living-docs/event-catalog/`
- 트랜잭션 · 이벤트 협력 실제 흐름 → `living-docs/boundary-trace/`
- 왜 이렇게 박혔는지 → `docs/adr/ADR007.md` (동기 도메인 이벤트)

---

## 4. Re-pin trigger

- 비동기 이벤트 도입 (`@Async`, 메시지 큐 등) — 원자성 계약 변경
- `@TransactionalEventListener` 도입 — 커밋 후 실행 필요 시나리오 등장
- isolation 기본값 변경 (READ_COMMITTED → REPEATABLE_READ 등)
- OSIV 활성화 요청 (성능·UX 상충 시)
- 분산 트랜잭션 도입 (2PC · Saga)
- BC 간 협력에 REST · 외부 API 통합 등장
- `noRollbackFor` 예외 도입 필요성
