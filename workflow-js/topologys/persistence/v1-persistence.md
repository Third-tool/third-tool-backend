# Pinned Topology — `persistence` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 테이블명·컬럼명·JPA 어노테이션 옵션은 여기 없다)

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

1. **DB 스키마 / JPA 매핑 변경 작업 시 이 파일을 먼저 읽는다.** §2의 graph는 본 구간 동안 **고정 제약**.
2. **이 topology 안에서 실제 컬럼·타입·인덱스 어휘만 채운다.** PK 전략·enum 저장 방식·생애주기 표시자의 존재는 건드리지 않는다.
3. **이미 머지된 마이그레이션 파일을 수정해야 할 것 같으면 STOP하고 보고한다.** forward-only 경계 위반이다.
4. **enum을 ordinal로 저장하려 하거나 audit 시각을 외부에서 주입하려는 정황이 보이면 보고**한다.
5. **이 파일에 어휘를 추가하지 않는다.** 실제 마이그레이션과 매핑은 §3 포인터로.

---

## 2. The pinned topology

### Nodes
- `식별자` — 모든 영속 엔티티의 surrogate PK
- `열거 표현` — DB에 enum을 저장하는 형태(이름 기반 + DB 측 허용값 제약)
- `생애주기 표시자` — Soft Delete 대상 도메인의 삭제 marker
- `감사 시각` — 생성/수정 시점 기록 컬럼
- `마이그레이션 단위` — Flyway 버전 파일 1개
- `도메인 매핑` — 도메인 객체와 DB 테이블의 1:1 또는 1:N 매핑
- `중복 방지 제약` — UNIQUE 제약을 통한 도메인 규칙의 DB 측 안전망
- `이중 방어` — 도메인 검증 + DB 제약의 동시 강제 패턴

### Edges
- `도메인 매핑` → `식별자` : 1:1 surrogate
- `도메인 매핑` → `감사 시각` : 자동 주입(외부 주입 경로 없음)
- `도메인 매핑` → `생애주기 표시자` : Soft Delete 적용 대상에 한해
- `마이그레이션 단위` → `도메인 매핑` : 스키마 변경의 단방향 전달
- `도메인 검증` ↔ `중복 방지 제약` : 동일 규칙을 양쪽이 강제 (`이중 방어`)
- 조회 → `생애주기 표시자` : 자동 필터링 (삭제 표시 행은 조회에서 제외)

### Boundaries
- **시간 경계 (forward-only)**: 머지된 `마이그레이션 단위`는 수정 불가. 새 버전 파일로만 추가
- **가시성 경계**: `생애주기 표시자`가 켜진 행은 일반 조회 경로에서 자동 제외
- **주입 경계**: `감사 시각`은 시스템(JPA Auditing 등)이 단일 출처. 외부 주입 금지
- **enum 표현 경계**: 이름 기반 저장만 허용. ordinal 저장 금지
- **수정 동반 경계**: 매핑 테이블(연결 사실만 기록)은 수정 컬럼을 두지 않는다 (연결/해제만)

### Invariants
- `열거 표현`은 ordinal로 저장되지 않는다
  - 감지법: ArchUnit 또는 enum 매핑 grep
- 사용자 노출 자산 도메인은 100% `생애주기 표시자` 적용 대상이다 (구조 편집·매핑·이력 도메인은 제외)
  - 감지법: Soft Delete 적용 분류표를 PR 리뷰에서 점검
- 핵심 도메인 규칙은 도메인 검증 + DB 제약 양쪽에 존재한다 (`이중 방어`)
  - 감지법: 도메인 단위 테스트 + Repository slice 테스트가 같은 규칙을 각각 검증
- 머지된 `마이그레이션 단위` 수정 0건
  - 감지법: PR 리뷰 시 기존 V 파일 변경 여부 확인
- 외부에서 `감사 시각` 컬럼을 set하는 코드 0건
  - 감지법: setter / setField 호출 grep

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 테이블/컬럼명 · 타입 · 길이 → `src/main/resources/db/migration/V*.sql`
- 실제 JPA 매핑 · 어노테이션 옵션 → `{BC}/infrastructure/persistence/` 하위 코드
- 실제 PK 컬럼명 · 인덱스 이름 → 마이그레이션 SQL
- PK 전략 / enum / Soft Delete / 이중 방어의 상세 규칙 → `.claude/rules/conventions.md` §3
- Soft Delete 적용 도메인 분류표 → `docs/adr/` 의 해당 ADR
- 왜 이렇게 박혔는지 → `docs/adr/` (ADR001 PK, ADR002 enum, ADR003 Soft Delete 등)

---

## 4. Re-pin trigger

- PK 전략 변경 (auto-increment → UUID/ULID 등)
- enum 저장 방식 변경 (이름 기반 → 코드값/ordinal)
- Soft Delete 적용 분류표 변경 (새 도메인 분류 편입 등 v1 분류 외 의사결정)
- 멀티 DBMS 도입으로 forward-only 마이그레이션 가정 깨짐
- audit 컬럼 정밀도/소스 변경 (예: 클라이언트 시각 수용)
- 매핑 테이블에 수정 행위가 필요한 새 도메인 등장
