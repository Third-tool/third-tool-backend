# Pinned Topology — `bounded-context` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 BC 이름·실제 패키지 경로·실제 도메인 클래스는 여기 없다)

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

1. **BC 구조에 영향을 주는 작업 시 이 파일을 먼저 읽는다.** §2의 의존 방향은 본 구간 동안 **고정 제약**.
2. **BC 안에서 클래스·메서드·이벤트 어휘만 자유롭게 채운다.** 레이어 구조·BC↔BC 매개 방식은 건드리지 않는다.
3. **BC가 다른 BC의 도메인 타입을 직접 import해야 할 것 같으면 STOP하고 보고한다.** 그건 엣지 위반이다.
4. **Aggregate가 Repository를 직접 호출하려 하거나 application이 presentation을 import하려는 정황을 발견하면 보고**한다.
5. **이 파일에 BC 이름·도메인 클래스명을 추가하지 않는다.** 그건 §3 포인터로.

---

## 2. The pinned topology

### Nodes
- `Bounded Context` — 응집된 도메인 의미 단위. 시스템의 1차 분할 축
- `presentation 레이어` — HTTP 진입 / DTO 직렬화
- `application 레이어` — 트랜잭션 경계 / 유스케이스 조율 / Command·Query 분리
- `domain 레이어` — Aggregate / Entity / VO / Domain Service / BC 전용 예외
- `infrastructure 레이어` — Repository 어댑터 / JPA / QueryDSL / 외부 어댑터
- `Aggregate Root` — 일관성 경계의 단위
- `도메인 이벤트` — BC ↔ BC 협력의 매개체
- `공통 모듈` — 비-BC 가로 모듈 (참조: common-core topology)

### Edges
- `presentation` → `application` (depend)
- `application` → `domain` (depend)
- `infrastructure` → `domain` (구현, 의존 역전)
- `presentation` ↛ `domain` 직접 의존 **금지** (application 경유)
- `application` ↛ `presentation` **금지** (양방향 금지)
- `domain` ↛ `infrastructure` **금지** (의존 역전 유지)
- `Aggregate Root` ↛ `Repository` 직접 호출 **금지** (Application Service만)
- `Bounded Context A` ↛ `Bounded Context B` 직접 import **금지**
- `Bounded Context A` → (`도메인 이벤트`) → `Bounded Context B` : 이벤트로만 협력
- `Bounded Context` → `공통 모듈` : 참조 허용
- `공통 모듈` ↛ `Bounded Context` **금지** (참조: common-core topology)

### Boundaries
- **레이어 경계**: 동일 BC 내에서 application ↔ presentation 양방향 의존 금지
- **BC 경계**: BC 내부 도메인 타입은 BC 밖에 노출되지 않음 (DTO / `도메인 이벤트`로만)
- **일관성 경계**: 트랜잭션은 `application 레이어`가 소유. `Aggregate Root` 한 개의 일관성만 한 트랜잭션에서 보장
- **소유권 경계**: `Aggregate Root`의 자식 엔티티는 Aggregate를 통해서만 생성/변경/삭제

### Invariants
- 모든 BC는 동일한 4-레이어 구조를 가진다
  - 감지법: 패키지 트리 점검 + ArchUnit
- `presentation` 타입이 `domain`에서 import되는 일 0건
  - 감지법: ArchUnit 또는 import 그래프 검사
- BC가 다른 BC의 `domain` 타입을 import하는 일 0건
  - 감지법: ArchUnit 또는 import grep
- `Aggregate Root`가 Repository를 호출하는 코드 0건
  - 감지법: domain 패키지에서 Repository 타입 참조 grep
- BC ↔ BC 협력은 `도메인 이벤트`를 통해서만 일어난다
  - 감지법: BC 간 cross-import 차단 + 이벤트 발행 지점 리뷰

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 BC 목록 · 실제 BC별 책임 → `docs/PACKAGE.md` · `docs/DOMAIN.md`
- 실제 도메인 클래스 · Aggregate 식별 → `{BC}/domain/` 하위 코드
- 실제 Application Service 시그니처 · Command/Query record → `{BC}/application/`
- 실제 Repository 인터페이스 · JPA 매핑 → `{BC}/infrastructure/persistence/`
- 실제 도메인 이벤트 타입 → `{BC}/domain/event/` (또는 이벤트 발행 지점)
- 4-레이어 구조 / Command·Query 분리 / 의존 방향 상세 규칙 → `docs/PACKAGE.md`
- 왜 이렇게 박혔는지 → `docs/adr/` 의 BC·레이어 관련 ADR

---

## 4. Re-pin trigger

- 새 BC 추가 또는 기존 BC 분할/병합 (구조 변경)
- 4-레이어 구조의 변형(예: application 폐기, hexagonal로 재편)
- BC ↔ BC 직접 호출 허용으로 전환 (이벤트 외 수단 도입)
- Command·Query 분리 원칙의 변경
- 모듈러 모놀리스 → 마이크로서비스 분리 결정
- ArchUnit 등 의존 강제 도구 도입/철회
