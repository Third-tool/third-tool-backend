# Pinned Topology — `testing` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 테스트 클래스·실제 fixture 헬퍼는 여기 없다)

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

1. **테스트 작성/수정 작업 시 이 파일을 먼저 읽는다.** §2의 graph는 본 구간 동안 **고정 제약**.
2. **이 topology 안에서 fixture·테스트 메서드 어휘만 자유롭게 채운다.** 계층별 mock 전략·검증 위치 분배는 건드리지 않는다.
3. **도메인 객체를 mock해야 할 것 같으면 STOP하고 보고한다.** 그건 위반이다.
4. **같은 규칙을 여러 계층에서 중복 검증하려는 정황이 보이면 보고**한다.
5. **이 파일에 fixture 코드를 두지 않는다.** 그건 §3 포인터로.

---

## 2. The pinned topology

### Nodes
- `도메인 단위 테스트` — Aggregate / Entity / VO / Domain Service 행위 검증
- `애플리케이션 서비스 단위 테스트` — 유스케이스 조율 검증 (Repository는 mock, 도메인은 실제)
- `Repository slice 테스트` — 쿼리·매핑·DB 제약 검증 (실제 DB, in-memory)
- `Controller slice 테스트` — HTTP 매핑·DTO 직렬화·에러 응답 형식 검증 (Service는 mock)
- `통합 테스트` — 다중 BC 시나리오·트랜잭션·마이그레이션 검증
- `테스트 케이스 분류` — 해피 / 엣지 / 예외 의 3구분
- `외부 시스템 경계` — Repository 인터페이스, 외부 API 클라이언트 등 mock 대상

### Edges
- `도메인 행위` → `도메인 단위 테스트` : 실제 객체로만 (mock 불가)
- `애플리케이션 서비스` → `애플리케이션 서비스 단위 테스트` : `외부 시스템 경계`만 mock, 도메인은 실제
- `Repository` → `Repository slice 테스트` : 1:1
- `Controller` → `Controller slice 테스트` : 1:1, 서비스 계층은 mock
- 다중 BC 협력 시나리오 → `통합 테스트` : 핵심 시나리오에만
- 모든 행위 → `테스트 케이스 분류` 3구분 : 해피·엣지·예외 각 최소 1건

### Boundaries
- **mock 경계**: mock은 `외부 시스템 경계`에서만. 도메인 객체는 어떤 테스트에서도 mock하지 않는다
- **검증 분배 경계**: 동일 규칙은 단일 계층에서만 검증 (중복 검증 금지). 도메인 검증은 도메인 단위 테스트가, DB UNIQUE 위반은 Repository slice가
- **fixture 위치 경계**: 테스트 fixture는 테스트 패키지에만. 프로덕션 코드에 두지 않는다
- **시각 의존 경계**: 시각 의존 테스트는 Clock 주입 또는 허용 범위 비교로 처리

### Invariants
- 도메인 객체를 mock한 테스트 0건
  - 감지법: 테스트 코드에서 Mockito mock 호출 대상 grep
- 모든 도메인 행위는 해피/엣지/예외 3구분이 각 최소 1건 존재
  - 감지법: 도메인 행위 추가 PR은 3구분 테스트 동반 (체크리스트)
- 같은 규칙을 도메인 + slice + 통합 세 계층에서 동시 검증하는 사례 0건
  - 감지법: 리뷰 + Test Reviewer 관점에서 중복 지적
- 테스트 메서드명이 `{대상행위}_{상황}_{기대결과}` 형식을 따른다
  - 감지법: 명명 규칙 lint 또는 리뷰

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 테스트 클래스 · 메서드 · fixture 헬퍼 → `src/test/java/` 하위 코드
- 실제 mock 설정 · slice 어노테이션 옵션 → 테스트 코드
- 테스트 명명 규칙 · 케이스 분류 상세 · 계층별 전략 → `.claude/rules/conventions.md` §4
- 어떤 도메인 행위가 어떤 3구분을 가지는지 → 테스트 코드의 메서드명
- 왜 Classist 기본인지 → `docs/adr/` 의 테스트 전략 ADR (있을 경우)

---

## 4. Re-pin trigger

- Classist 기본 → Mockist 기본 전환
- 도메인 객체 mock 허용으로 정책 변경
- 통합 테스트 비중 변경 (예: e2e 우선으로 이동)
- Slice 테스트 폐기 (예: 통합으로 모두 흡수)
- 시각/난수 의존 처리 방식 변경
- 새 테스트 종류 추가 (성능·계약 테스트 등) 로 §2 노드가 확장 필요
