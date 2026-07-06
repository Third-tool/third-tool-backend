# Pinned Topology — `migration-policy` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 V번호·SQL 문·컬럼명은 여기 없다)

**목적**: DB 스키마 변경의 반복 안전 패턴을 pin. `persistence`가 스키마 매핑 규칙을 다룬다면, 본 파일은 **스키마 변경의 3-phase·rollback·soft-deprecate·검증 SQL 원칙**을 다룬다.

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

1. **DB 스키마 변경 시 이 파일을 먼저 읽는다.** §2의 3-phase·R페어·검증 SQL은 본 구간 **고정 제약**.
2. **컬럼 추가 + NOT NULL을 한 마이그레이션에 몰아넣으려는 정황이 보이면 STOP하고 보고한다.** 3-phase 위반이다.
3. **rollback 스크립트(R페어) 없이 데이터 이관 마이그레이션을 만들려는 정황이 보이면 보고**한다.
4. **컬럼 즉시 DROP을 하려는 정황이 보이면 보고**한다. soft-deprecate 우회다.
5. **V번호 사전 할당표 없이 V번호를 배정하려는 정황이 보이면 보고**한다. 병렬 개발 충돌 위험.
6. **이 파일에 어휘를 추가하지 않는다.** 실제 SQL·컬럼명은 §3 포인터로.

---

## 2. The pinned topology

### Nodes
- `마이그레이션 단위` — Flyway V 파일 1개 (`V{n}__{purpose}.sql`)
- `3-phase 변경` — 컬럼 추가 + NOT NULL 승격을 3단계로 분리 (NULL → 백필 → NOT NULL)
- `rollback 스크립트` — 각 V버전에 대응하는 `R{n}__rollback_*.sql`
- `검증 SQL` — 백필·재매핑 마이그레이션에 첨부되는 데이터 일치성 검증 SELECT
- `soft-deprecate` — 컬럼·테이블을 즉시 DROP 대신 사용 중단 표시 후 다음 릴리스에 DROP
- `RENAME 아카이브` — 폐기 테이블을 `_archived_*`로 rename 후 유지
- `V번호 사전 할당표` — 마일스톤 시작 시점에 V번호 범위 사용자 확인
- `데이터 이관` — 기존 데이터 형태 변경 (재매핑·자동 마이그레이션)
- `CHECK 제약 재작성` — Enum 값 변경 시 CHECK 재정의
- `머지된 V 파일` — main·develop에 머지된 V파일 · **수정 금지 대상**
- `dev H2 검증` — 로컬 부팅 시 마이그레이션 착지 정합 확인
- `staging dry-run` — 프로덕션 배포 전 staging RDS에 사전 적용

### Edges
- 스키마 변경 요구 → `V번호 사전 할당표` → `마이그레이션 단위` : V번호 확보 후 파일 생성
- `3-phase 변경` → 3개의 `마이그레이션 단위` : 1단계(NULL) → 2단계(백필) → 3단계(NOT NULL)
- 데이터 이관 마이그레이션 → `rollback 스크립트` : R페어 동반 필수
- 백필·재매핑 → `검증 SQL` : 마이그레이션 주석 또는 R페어 인접에 명시
- 컬럼 폐기 → `soft-deprecate` → 다음 릴리스 DROP : 즉시 DROP 우회
- 테이블 폐기 → `RENAME 아카이브` (`_archived_*`) → 다음 릴리스 DROP
- `머지된 V 파일` → 수정 없이 새 V버전 파일 추가만 : forward-only (persistence topology 계승)
- 프로덕션 배포 전 → `staging dry-run` : sanity check
- 로컬 개발 → `dev H2 검증` : Flyway 착지 로그·CHECK 제약 확인

### Boundaries
- **3-phase 경계**: 컬럼 추가와 NOT NULL 승격을 **동일 V버전에 몰지 않는다**. `ADD COLUMN NULL` → `UPDATE ... 백필` → `MODIFY COLUMN NOT NULL` 3단계 필수.
- **rollback 경계**: 데이터 이관 (`UPDATE`·`DELETE`·재매핑) 을 동반하는 마이그레이션은 **R페어 스크립트 사전 준비 필수**. 순수 DDL 추가만 하는 마이그레이션은 R페어 선택.
- **검증 경계**: 백필·재매핑 마이그레이션은 **성공 후 데이터 일치성 검증 SQL** 을 주석 또는 인접 파일로 두어야 한다. 백필 실패를 감지할 수 없으면 프로덕션 위험.
- **DROP 경계**: 컬럼·테이블 즉시 DROP 금지. `soft-deprecate` → 다음 릴리스에 별도 마이그레이션으로 DROP. 컬럼은 유지·`RENAME 아카이브` 로 테이블 보존.
- **수정 경계**: 머지된 V 파일은 **절대 수정 금지**. 새 V버전 파일 추가로만 변경 (persistence topology forward-only 원칙 계승).
- **V번호 경계**: 병렬 마일스톤·PR에서 V번호 충돌 방지를 위해 마일스톤 시작 시점에 사전 할당표 사용자 확인.
- **staging dry-run 경계**: 프로덕션 첫 착지 시점 (M7) 이전에 staging RDS에서 dry-run 완료 후 프로덕션 적용.

### Invariants
- `ADD COLUMN NOT NULL` 을 백필 없이 실행하는 V파일 0건 (기존 행이 없는 신설 테이블 제외)
  - 감지법: V파일 grep `ADD COLUMN.*NOT NULL` · 백필 UPDATE 부재 확인
- 데이터 이관·재매핑 V파일에 대응하는 R페어 없는 사례 0건
  - 감지법: V파일별 R파일 존재 확인 · 데이터 이관 여부 grep
- 백필·재매핑 V파일에 검증 SQL 주석·인접 파일 없는 사례 0건
  - 감지법: V파일 주석 · `V{n}_verify.sql` 등 인접 파일 존재 확인
- 컬럼·테이블 즉시 DROP (soft-deprecate 미경유) 사례 0건
  - 감지법: `DROP COLUMN`·`DROP TABLE` V파일 리뷰 · 사전 soft-deprecate 이력 확인
- 머지된 V파일 수정 사례 0건
  - 감지법: git diff on merged V files · PR 리뷰 시 기존 V파일 변경 여부
- V번호 사전 할당표 없이 V번호 배정한 사례 0건
  - 감지법: 마일스톤 milestone.md §Flyway V 버전 순서 관리 표 존재 확인
- 프로덕션 첫 착지 (M7) 전 staging dry-run 없이 프로덕션 배포한 사례 0건
  - 감지법: 배포 파이프라인 로그 · staging Flyway 이력 확인

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 V번호·V파일명·SQL 내용 → `src/main/resources/db/migration/V*.sql`
- 실제 R페어 rollback 스크립트 → `src/main/resources/db/migration/R*.sql`
- 실제 백필 SQL·검증 SELECT → V파일 본문 및 주석
- 실제 CHECK 제약 재정의 SQL → V파일
- V번호 사전 할당표 → 각 `workflow/task/milestones/version/{Nv}/milestone.md` §Flyway 순서 관리
- Flyway 사용 규칙 상세·컬럼 길이 가이드 → `.claude/rules/conventions.md` §3.8·§3.9
- 3-phase 마이그레이션 실 예시 → `CARD E3 S3-1` (V25/V26/V27) · `LT E4 S4-1~S4-3` (V28/V29/V30)
- 왜 이렇게 박혔는지 → M4 review.md R5 (rollback 검증 지연) · persistence topology 계승

---

## 4. Re-pin trigger

- Flyway → 다른 마이그레이션 도구 이관 (Liquibase 등)
- forward-only 원칙 폐기 (기존 V파일 수정 허용)
- soft-deprecate 정책 폐기 (즉시 DROP 허용)
- 3-phase 패턴 자동화 도구 도입 (예: `gh-ost`·`pt-online-schema-change`) 로 원칙 자동화
- 다중 DBMS 도입으로 R페어 표준 SQL 방언 문제 등장
- V번호 사전 할당 필요성 소멸 (예: Flyway가 자동 collision 해결)
- 프로덕션 첫 착지 (M7) 이후 staging dry-run 요구 재조정
