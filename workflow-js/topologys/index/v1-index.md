# Pinned Topology — `index` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 인덱스 이름·컬럼 순서는 여기 없음)

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

1. **새 인덱스 추가·기존 인덱스 수정 시 이 파일을 먼저 읽는다.** §2의 원칙은 본 구간 **고정 제약**.
2. **인덱스는 Repository 쿼리 시그니처가 1차 근거.** 쓰지 않는 인덱스 신설 금지.
3. **커버링 인덱스는 명시적으로 계획.** `EXPLAIN` 근거 없이 감으로 추가 금지.
4. **composite 인덱스의 컬럼 순서는 등가 필터 → 범위 → 정렬 원칙.**
5. **인덱스 변경은 Flyway 새 V 파일로만.** 기존 V 파일 수정 금지 (forward-only).
6. **인덱스 삭제 시 쿼리 회귀 테스트 필수.** slow query 발생 리스크.

---

## 2. The pinned topology

### Nodes
- `단일 컬럼 인덱스` — FK · 자주 필터되는 enum · unique 후보 컬럼
- `composite 인덱스` — 다중 컬럼 (등가 필터 + 정렬)
- `UNIQUE 제약` — 중복 방지 (인덱스 자동 생성)
- `커버링 인덱스` — 인덱스만으로 SELECT 완결 (테이블 접근 없음)
- `partial 인덱스` — MySQL 미지원. 대체는 `@SQLRestriction` + 일반 인덱스
- `soft delete 필터` — `deleted_at IS NULL` 조회 시 인덱스 후보
- `PK 인덱스` — surrogate PK 자동
- `쿼리 시그니처` — Repository 메서드의 조건 조합 (인덱스 근거)
- `EXPLAIN` — 인덱스 사용 여부·비용 검증 도구

### Edges
- `쿼리 시그니처` → `단일 컬럼 인덱스` : `findByX` 1개 조건일 때
- `쿼리 시그니처` → `composite 인덱스` : `WHERE parent_id = ? ORDER BY display_order` 등
- `쿼리 시그니처` → `커버링 인덱스` : SELECT 컬럼이 인덱스에 포함될 때 명시 계획
- `UNIQUE 제약` → `composite 인덱스` : `(facade_id, name, deleted_at)` 3-col composite로 활성 UNIQUE
- `soft delete 필터` → `일반 인덱스` : MySQL partial 미지원, `@SQLRestriction`이 자동 조건 부여
- `EXPLAIN` → `인덱스 조정` : 근거 있는 신설·삭제 결정

### Boundaries
- **컬럼 순서 경계**: composite 인덱스는 **등가 필터 컬럼 → 범위 필터 컬럼 → 정렬 컬럼** 순.
- **커버링 경계**: SELECT 컬럼 전부 커버할 때만 "커버링"이라 부름. 부분 커버는 커버링 아님.
- **FK 인덱스 경계**: 모든 FK는 인덱스 대상 (JOIN 성능). MySQL은 FK 자동 인덱스 X → 명시 CREATE 필요.
- **partial 인덱스 경계**: MySQL은 partial 인덱스 미지원. `deleted_at IS NULL` 필터는 일반 인덱스 + `@SQLRestriction` 조합.
- **DDL 경계**: 인덱스 CREATE/DROP은 Flyway V 파일로만. 앱 코드 · JPA `@Index` 어노테이션도 마이그레이션이 진실 소스.
- **삭제 경계**: 사용되지 않는 인덱스도 함부로 삭제 X. slow query 회귀 리스크 → EXPLAIN + 트래픽 관찰 후 결정.

### Invariants
- 모든 FK 컬럼에 인덱스 존재 (단독 또는 composite 선두)
  - 감지법: `SHOW INDEX FROM {table}` vs FK 컬럼 diff
- composite 인덱스 컬럼 순서 규칙 위반 0건 (등가 → 범위 → 정렬)
  - 감지법: 인덱스 정의 리뷰
- Repository 메서드가 참조하지 않는 인덱스 0건 (신설 시)
  - 감지법: 신설 인덱스에 대해 Repository 메서드 grep
- `@Index` JPA 어노테이션과 Flyway V 파일 인덱스 정의 일치 (drift 0)
  - 감지법: PR 리뷰
- soft delete 대상 테이블은 조회 대상 컬럼에 인덱스 존재
  - 감지법: soft delete 도메인 목록 vs 인덱스 존재 여부

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 인덱스 이름 · 컬럼 조합 → `living-docs/index-catalog/` (현재 목록)
- 실제 인덱스 CREATE SQL → `src/main/resources/db/migration/V*.sql`
- 실제 JPA `@Index` 어노테이션 → `{BC}/domain/model/` 또는 `{BC}/infrastructure/persistence/`
- 왜 이 인덱스가 존재하는지 → Repository 메서드 시그니처 + Flyway V 파일 주석
- Repository 조회 시그니처 → `living-docs/api-spec/` 및 `{BC}/infrastructure/persistence/`

---

## 4. Re-pin trigger

- MySQL → PostgreSQL 등 partial 인덱스 지원 DB 이관 (필터 인덱스 도입 가능)
- 인덱스 튜닝 도구 도입 (pt-online-schema-change, gh-ost 등)
- 쿼리 시그니처를 인덱스 근거로 삼는 원칙에서 벗어남 (예: 캐시 우선 전략)
- 데이터 볼륨 급증으로 인덱스 전략 재설계 (샤딩, 파티셔닝 등)
- composite 컬럼 순서 원칙 예외 필요 (오프라인 배치 등 특수 케이스)
- FK 자동 인덱스 옵션 도입 (다른 RDBMS)
