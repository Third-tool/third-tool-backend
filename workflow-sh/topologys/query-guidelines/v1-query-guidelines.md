# Pinned Topology — `query-guidelines` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 Repository 시그니처·QueryDSL Q타입·fetch join 예시는 여기 없다)

**목적**: DB 조회 코드의 반복 원칙을 pin. `index`가 인덱스를 다룬다면, 본 파일은 **쿼리 자체의 성능·정합·트랜잭션 원칙**을 다룬다. 성능 baseline (M7) 부터 실측 반영.

---

## 0. 유효 구간 (Validity)

| 항목 | 값 |
| --- | --- |
| Pinned at | 2026-07-21 |
| Valid for | 본 워크플로우 v1 구간 — re-pin trigger 발생 전까지 |
| Owner | 메인 Claude + 사용자 합의 |
| Re-pin trigger | 아래 §4 |

---

## 1. How Claude Code must use this file — 행동 계약

1. **Repository 쿼리·Query Service 작업 시 이 파일을 먼저 읽는다.** §2의 readOnly·N+1 방지·slow query 로깅은 본 구간 **고정 제약**.
2. **Query Service의 public 메서드에 `readOnly=true` 없이 커밋하려는 정황이 보이면 STOP하고 보고한다.**
3. **컬렉션 조회에 fetch join·`@EntityGraph` 없이 lazy loading에 의존하려는 정황이 보이면 보고**한다. N+1 위험.
4. **slow query 로깅 config 없이 프로덕션 배포 정황이 보이면 보고**한다.
5. **Repository 메서드 시그니처가 인덱스 근거 없이 신설되려는 정황이 보이면 보고**한다 (index topology 계승).
6. **이 파일에 어휘를 추가하지 않는다.** 실제 시그니처·QueryDSL 코드는 §3 포인터로.

---

## 2. The pinned topology

### Nodes
- `Query Service` — `@Transactional(readOnly=true)` 로 조회 유스케이스 조율
- `Command Service` — 기본 `@Transactional` 로 쓰기 조율 (transaction topology 계승)
- `Spring Data Repository` — 명명 규칙 기반 자동 쿼리 · 단순 조회
- `QueryDSL Q타입` — 동적 조건·복잡 조인·페이징 조회 · type-safe
- `fetch join` — 자식 컬렉션 조회 시 즉시 로드로 N+1 방지
- `@EntityGraph` — 자식 로드 전략 명시 (fetch join 대안)
- `N+1 신호` — 반복 SQL 로그 · Hibernate statistics
- `slow query 로깅` — 임계값 초과 SQL 발견 시 로그 (100ms 등 externalize)
- `Hibernate statistics` — dev 활성 · 프로덕션 비활성 (성능 부담)
- `쿼리 시그니처` — Repository 메서드의 조건 조합 (index topology 근거)
- `dev vs prod 쿼리 로깅` — dev = show_sql · prod = slow query 로깅만
- `페이징` — Pageable 표준 · 커서 페이징은 별도 결정
- `SQL 원문 로깅 경계` — 프로덕션에서 SQL 원문 로깅 시 개인정보 파라미터 노출 위험

### Edges
- Controller → `Query Service` (readOnly=true) → Repository 조회
- Controller → `Command Service` (readOnly=false) → Repository 저장·상태 변경
- 자식 컬렉션 조회 요구 → `fetch join` 또는 `@EntityGraph` : lazy 의존 지양
- `N+1 신호` 감지 → `fetch join`·`@EntityGraph` 재검토 : dev `Hibernate statistics` 로 관찰
- 복잡 동적 조건 · 페이징 · type-safety 요구 → `QueryDSL Q타입`
- 단순 조회 (`findBy...`·`existsBy...`) → `Spring Data Repository` 자동 쿼리
- 새 Repository 메서드 → `쿼리 시그니처` → `index` topology 근거 확인
- 프로덕션 SQL 실행 → `slow query 로깅` 임계값 초과 시 로그 (SQL 원문·파라미터 마스킹)
- 페이징 요구 → `페이징` (Pageable) : 커서 페이징 도입 시 별도 검토

### Boundaries
- **readOnly 경계**: Query Service public 메서드는 **100% `readOnly=true`**. Command Service는 기본 (`readOnly=false`).
- **N+1 방지 경계**: 컬렉션 반환 유스케이스는 **fetch join 또는 `@EntityGraph`**. Application Service 에서 lazy loading 의존 금지 (OSIV=false 이므로 view 계층에서 lazy fetch 불가).
- **QueryDSL vs Spring Data 경계**: 단순 조회 → Spring Data · 동적 조건·복잡 조인·type-safety → QueryDSL. 두 방식 혼재 시 리뷰.
- **slow query 로깅 경계**: 프로덕션에 slow query 로깅 필수 (예: 100ms 초과). SQL 원문 로깅 시 파라미터 개인정보 마스킹.
- **Hibernate statistics 경계**: dev 활성 (`spring.jpa.properties.hibernate.generate_statistics=true`) · 프로덕션 **비활성** (성능 부담).
- **SQL 원문 로깅 경계**: 프로덕션에서 `spring.jpa.show-sql=true` 금지. 대신 slow query 로깅.
- **인덱스 근거 경계**: 새 Repository 메서드는 대응 인덱스 존재 확인 (`index` topology · living-docs/index-catalog 대조).
- **페이징 경계**: 목록 조회는 Pageable 표준. 커서 페이징 도입은 별도 검토 (대량 데이터·최신순 등).

### Invariants
- Query Service public 메서드 중 `readOnly=true` 미지정 사례 0건
  - 감지법: `*QueryService.java` grep · `@Transactional` 옵션 리뷰
- 컬렉션 반환 유스케이스에서 fetch join·`@EntityGraph` 없이 lazy 로딩 의존 사례 0건 (N+1 위험)
  - 감지법: Query Service 코드 리뷰 · Hibernate statistics 관찰
- 프로덕션 config `spring.jpa.show-sql=true` 사례 0건
  - 감지법: `application-prod.yml` 리뷰
- 프로덕션 config에 slow query 로깅 임계값이 미설정된 사례 0건
  - 감지법: `application-prod.yml` slow query 관련 config 확인
- 새 Repository 메서드가 대응 인덱스 없이 신설된 사례 0건 (index topology와 중복 강제)
  - 감지법: Repository 메서드 vs `living-docs/index-catalog` 대조
- Application Service에서 view 계층으로 lazy 프록시 전달 사례 0건 (OSIV=false)
  - 감지법: Response DTO 조립 시점 리뷰 · lazy 필드 접근 흐름
- 프로덕션 SQL 로그에 개인정보 파라미터가 raw 노출된 사례 0건
  - 감지법: slow query 로그 sample · 마스킹 config 확인

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 Repository 메서드 시그니처 → `{BC}/infrastructure/persistence/`
- 실제 QueryDSL Q타입·쿼리 코드 → `{BC}/infrastructure/persistence/query/`
- 실제 fetch join·`@EntityGraph` 예시 → Repository 코드
- 실제 slow query 로깅 config → `application.yml` (Hibernate·datasource 하위)
- 실제 인덱스 카탈로그 → `living-docs/index-catalog/`
- readOnly·QueryDSL 상세 규칙 → `.claude/rules/conventions.md` (query 절 · 미신설이면 향후 확장)
- 페이징·커서 페이징 판단 → 코드 시점 결정 (case-by-case)
- 왜 이렇게 박혔는지 → `docs/adr/` (query 관련 ADR · 미신설)

---

## 4. Re-pin trigger

- QueryDSL 폐기 · 다른 type-safe query 도구 도입 (jOOQ 등)
- Spring Data Repository 폐기 · JPA EntityManager 직접 사용
- OSIV 활성화 (transaction topology 재pin과 연동)
- Hibernate → 다른 ORM (MyBatis 등) 이관
- lazy loading 기본 폐기 (eager 기본)
- slow query 로깅 → APM 도입 (분산 트레이싱으로 대체)
- 커서 페이징 기본 도입 (Pageable 폐기)
- CQRS 도입 (Read model 분리) 로 쿼리 계층 구조 변경
