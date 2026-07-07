# Pinned Topology — `domain-modeling` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 Aggregate 이름·필드명·팩토리 시그니처는 여기 없다)

**목적**: Aggregate·Entity·VO 도메인 객체 설계의 반복 원칙을 pin. `bounded-context`가 BC 경계·의존 방향을 다룬다면, 본 파일은 **BC 내부 도메인 객체의 캡슐화·생성·상태·컬렉션 관리 원칙**을 다룬다.

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

1. **새 Aggregate/Entity/VO 신설·수정 시 이 파일을 먼저 읽는다.** §2의 graph는 본 구간 **고정 제약**.
2. **팩토리·컬렉션 캡슐화·displayOrder 규칙은 어휘 수준에서 자유롭게 채우되, 원칙은 건드리지 않는다.**
3. **Aggregate 외부에서 `new`로 자식 Entity를 생성하려는 정황이 보이면 STOP하고 보고한다.** 그건 팩토리 강제 위반이다.
4. **Application Service에서 도메인 객체의 필드를 setter로 조작하거나 도메인 검증을 우회하는 정황이 보이면 보고**한다.
5. **다건 입력 처리에서 부분 성공을 허용하려는 정황이 보이면 보고**한다. 트랜잭션 원자성 위반이다.
6. **이 파일에 어휘를 추가하지 않는다.** 그건 §3 포인터로.

---

## 2. The pinned topology

### Nodes
- `Aggregate Root` — 일관성 경계의 단위. 자식 Entity·VO의 유일한 진입점
- `자식 Entity` — Aggregate 내부에 소속. 외부 생성 금지
- `Value Object` — 불변 자료. 정적 팩토리로만 생성. 동등성은 값 비교
- `정적 팩토리` — `of(...)`·`create(...)` 등 이름 있는 생성 진입점. 생성자는 protected/package-private
- `자식 컬렉션` — Aggregate가 소유한 자식 리스트. 외부 노출은 `unmodifiableList`
- `자식 관리 행위` — `addX` / `removeX` / `reorderX` / `replaceX` (Aggregate 메서드)
- `displayOrder` — 1-based 순서 필드. Aggregate 내부에서만 부여·재부여
- `상태 전환` — Aggregate/Entity의 status 필드 변경 행위
- `멱등 전환` — 같은 상태로의 재호출 시 no-op (예외 X)
- `입력 정규화` — String의 trim·빈 문자열 null 정규화 (도메인 메서드 내부)
- `결과 VO` — 상태 변경 결과를 명시 표현하는 불변 record (`changed/unchanged`)
- `다건 입력` — 리스트 형태의 도메인 행위 (`addTopics(List<...>)` 등)
- `소유권 검증` — `isOwnedBy(userId)` 도메인 메서드
- `이중 방어` — 도메인 검증 + DB 제약의 동시 강제
- `권장 한도` — 상수 (`RECOMMENDED_AXIS_COUNT_LIMIT` 등) · 도메인 내부 · 외부 재정의 금지

### Edges
- 외부 → `Aggregate Root` → `자식 Entity` · `Value Object` : 외부 접근은 Aggregate 통해서만
- Application Service → `자식 관리 행위` → `자식 컬렉션` : 자식 조작은 Aggregate 메서드로만
- `자식 관리 행위` → `displayOrder` : 신규 추가 시 `현재 max + 1` 자동 · reorder 시 1부터 재부여
- `자식 컬렉션` → 외부 노출 : `Collections.unmodifiableList` 로만
- 외부 → `정적 팩토리` → `Aggregate Root`·`Value Object` : `new` 경로 없음
- `상태 전환` → `멱등 전환` : 같은 상태로의 호출은 no-op
- 도메인 메서드 내부 → `입력 정규화` : String 필드는 저장 전 trim
- `상태 변경 행위` → `결과 VO` : 변경 발생 여부를 명시 반환 (`isChanged() = false`면 저장 쿼리 생략)
- `다건 입력` → 전체 롤백 : 한 건 실패 시 전체 롤백. 부분 성공 없음
- Application Service → `소유권 검증` → Aggregate : 요청 진입 후 즉시 검증
- 도메인 검증 ↔ DB 제약 : 동일 규칙을 양쪽이 강제 (`이중 방어`)

### Boundaries
- **생성 경계**: Aggregate/Entity/VO는 `정적 팩토리`로만 외부 진입. 생성자는 package-private 이하.
- **자식 소유권 경계**: 자식 Entity는 **부모 Aggregate의 행위를 통해서만** 생성·삭제.
- **컬렉션 노출 경계**: 자식 컬렉션은 `unmodifiableList` 로만 노출. 직접 반환 금지.
- **displayOrder 주입 경계**: 외부 (Application Service·Controller) 가 `displayOrder`를 직접 설정하지 않는다.
- **상태 전환 경계**: 상태 전환 결과는 `결과 VO` 또는 상태 자체로만 표현. 사이드 채널 (`throws`) 로 결과 통신 금지.
- **다건 처리 경계**: 다건 입력은 전체 성공 or 전체 롤백. 부분 성공 응답 없음.
- **setter 경계**: 도메인 객체에 setter (public setField) 노출 금지. 상태 변경은 이름 있는 행위 메서드로.
- **상수 재정의 경계**: 권장 한도·임계값은 도메인 상수. Application Service·Controller·Frontend가 재정의하지 않음.
- **이중 방어 경계**: 도메인 검증만 있고 DB 제약 없는 규칙 · DB 제약만 있고 도메인 검증 없는 규칙 → 둘 다 두어야 함.

### Invariants
- Aggregate·Entity·VO의 public 생성자 (default 포함) 0건
  - 감지법: 도메인 패키지에서 public constructor grep
- Application Service·Controller에서 자식 Entity를 `new`로 생성한 사례 0건
  - 감지법: `new {ChildEntity}(` grep 스코프 = 도메인 밖
- 자식 컬렉션 필드가 `unmodifiableList`로 감싸지 않고 노출된 사례 0건
  - 감지법: getter 리턴 값이 `Collections.unmodifiableList(...)` 인지 리뷰
- `displayOrder`를 외부에서 직접 set하는 사례 0건
  - 감지법: `.setDisplayOrder(` grep 스코프 = 도메인 밖
- 상태 전환 메서드가 같은 상태 호출에 예외를 던지는 사례 0건
  - 감지법: 상태 전환 메서드 도메인 단위 테스트 (`archive_이미ARCHIVE_무시` 등)
- Aggregate가 Repository를 필드로 소유·호출한 사례 0건
  - 감지법: 도메인 패키지에서 Repository 타입 참조 grep (bounded-context topology와 중복 강제)
- 다건 입력 도메인 행위가 부분 성공을 반환하는 사례 0건
  - 감지법: 다건 메서드의 return type이 성공/실패 분리 리스트인지 리뷰
- 결과 VO 없이 상태 변경 결과를 사이드 채널로 통신한 사례 0건 (해당하는 도메인 메서드에 한해)
  - 감지법: `changed/unchanged` 판정이 필요한 메서드는 결과 VO 반환

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 Aggregate·Entity·VO 이름 · 필드 · 팩토리 시그니처 → `{BC}/domain/model/` 하위 코드
- 실제 자식 컬렉션 필드명 · getter · addX/removeX 시그니처 → 도메인 코드
- 실제 displayOrder 컬럼명 · 재부여 로직 → 도메인 메서드
- 실제 결과 VO 타입 (`ConceptChangeRecord` 등) → `{BC}/domain/model/` (VO 파일)
- 실제 도메인 상수 값 (권장 한도 등) → 도메인 클래스 상수
- 도메인 객체 설계 규칙 상세 → `.claude/rules/conventions.md` §1 (도메인 객체 컨벤션)
- 도메인 서사·불변식·의도 → `docs/DOMAIN.md`
- 왜 이렇게 박혔는지 → `docs/adr/` 도메인 관련 ADR

---

## 4. Re-pin trigger

- 도메인 객체를 public 생성자 노출 정책으로 전환
- Aggregate가 Repository를 직접 호출하는 패턴 허용 (bounded-context와 함께 재pin)
- 자식 컬렉션 노출 방식 변경 (mutable list 반환 허용)
- displayOrder 외부 주입 허용 (Application Service에서 직접 부여)
- 상태 전환 멱등성 정책 변경 (예외 던지기로 전환)
- 다건 입력의 부분 성공 허용 (트랜잭션 원자성 원칙 변경)
- 결과 VO 패턴 폐기 (사이드 채널 통신으로 전환)
- 도메인 상수의 외부 재정의 허용 (Application Service·Frontend에서 override)
