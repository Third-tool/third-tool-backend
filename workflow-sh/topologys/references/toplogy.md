# Pinned Topology — `<영역명>` (Phase: `<phase>`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님 (현재 상태를 따라 변하는 문서가 아니다)
> - runbook 아님 (운영 절차가 아니다)
> - vocabulary 아님 (구체 컬럼·필드·시그니처는 여기 없다 — 그건 수직축, 코드의 몫)
>
> 이 구간 동안 graph(노드·엣지·경계·불변식)는 **변하지 않는 제약**이다.
> 어휘는 이 제약 **안에서** 자유롭게 채운다.

---

## 0. 유효 구간 (Validity)

| 항목 | 값 |
| --- | --- |
| Pinned at | `<날짜 / 커밋 해시>` |
| Valid for | `<이 Epic / 이 마일스톤 / 이 분기>` |
| Owner | `<누가 이 박음을 책임지는가>` |
| Re-pin trigger | 아래 §4 조건 중 하나 발생 시 |

**이 구간이 끝나거나 trigger가 걸리면 이 파일은 무효다.** 재고정(Desktop에서 합의) 전까지 새 topology 가정 금지. "잠정 영구"가 아니라 "이 구간만큼"의 가설로 박힌 것이다.

---

## 1. How Claude Code must use this file — 행동 계약

작업할 때 이 파일을 이렇게 다룬다:

1. **작업 시작 시 이 파일을 먼저 읽는다.** §2의 graph / boundary / invariant는 이번 구간 동안 **고정 제약**이다. 의심하지 말고 전제로 깐다.
2. **이 topology 안에서 vocabulary만 생성한다.** 코드·이름·타입·메서드 시그니처·쿼리·마이그레이션은 자유롭게 만들되, 노드·엣지·경계는 건드리지 않는다.
3. **topology를 바꿔야 할 것 같으면 바꾸지 말고 STOP하고 보고한다.** L2 결정(경계 변경, 새 노드, 의존 방향 변경)을 코드 PR 안에 슬쩍 끼워 넣지 않는다 (architectural smuggling 금지). "이 작업을 깔끔히 하려면 §2의 X를 바꿔야 합니다 — 진행할까요?"로 멈춘다.
4. **코드와 이 파일이 충돌하면 임의로 맞추지 않는다.** 둘 중 하나가 틀린 게 아니라 "결정이 필요한 지점"이라는 신호다. 어느 쪽으로 맞출지 보고하고 지시를 기다린다.
5. **이 파일에 어휘를 추가하지 않는다.** 구체 컬럼·필드가 필요하면 §3의 코드 경로를 본다. 이 파일이 어휘로 오염되면 죽은 문서가 된다.

---

## 2. The pinned topology

### Nodes (어휘 없이, 개념만)
- `<NodeA>` — 한 줄 설명
- `<NodeB>` — 한 줄 설명

### Edges (방향 / 카디널리티)
- `<NodeA>` → `<NodeB>` : `<관계 / 1:N / 호출 방향 / 데이터 흐름>`

### Boundaries (어디서 끊는가)
- 트랜잭션 경계: `<...>`
- 컨텍스트 경계: `<...>`
- 동기 / 비동기 경계: `<...>`

### Invariants (이 구간 동안 절대 안 깨짐)
- `<불변식 1>` — 위반 감지법: `<테스트 / ArchUnit / 리뷰 포인트>`
- `<불변식 2>` — 위반 감지법: `<...>`

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 구체 vocabulary (컬럼·필드·시그니처) → `<코드 경로, 예: src/payment/entity/>`
- 현재 상태(살아있는 문서) → `<living doc 경로>` ← **이 파일과 다름**
- 운영 절차 → `<runbook 경로>` ← **이 파일과 다름**
- 이 위상이 왜 이렇게 박혔는지 → `<ADR 링크>`

---

## 4. Re-pin trigger — 이 topology를 다시 박아야 하는 신호

다음 중 하나라도 발생하면 **작업을 멈추고 Desktop에서 재고정**한다. Code 세션에서 즉흥적으로 바꾸지 않는다:

- 새 bounded context / 새 노드가 필요해진다
- 엣지(방향·카디널리티) 또는 경계를 바꿔야 한다
- invariant가 현실 코드와 반복적으로 안 맞는다 (= 위상이 어휘 차원에서 침식되고 있다는 진단 신호)
- 외부 의존성(PG, 메시지 브로커 등) 교체
- 비기능 요구(정합성 수준, 성능 목표)가 바뀐다

> 트리거가 안 걸리는 한, 이 파일은 이번 구간 동안 **질문 없이 따르는 단단한 제약**이다. 그게 이 파일의 목적이다.

---

<details>
<summary>채운 예시 — Payment Phase (모양 참고용)</summary>

```
# Pinned Topology — payment (Phase: M2 결제 도입)

## 0. Validity
- Pinned at: 2026-06-05 / <commit>
- Valid for: M2 마일스톤 (결제 1차)
- Re-pin trigger: §4

## 2. Topology
### Nodes
- Payment — 결제 집계, PG와의 정합성 소유
- Refund — 환불 집계 (Payment와 분리)
- Learning — 결제 결과를 소비하는 다운스트림

### Edges
- Learning → Payment : 동기 요청 (결제 시작), 1:N
- Payment → Learning : 이벤트 (PaymentCompleted/Failed/Refunded), at-least-once

### Boundaries
- 트랜잭션: PG cancel 호출은 트랜잭션 경계 "밖" (보상 트랜잭션)
- 동기/비동기: 결제 시작은 동기 202, 결과는 이벤트로 비동기
- 멱등성: 웹훅은 INSERT-first + DB UNIQUE

### Invariants
- 결제 완료 이벤트는 Payment 집계 외부에서 발행되지 않는다
  - 감지법: ArchUnit + 이벤트 발행 지점 테스트
- 서버가 금액을 재검증하지 않은 confirm은 통과하지 않는다
  - 감지법: confirm 핸들러 슬라이스 테스트

## 3. 어휘는
- 컬럼·필드 → src/payment/**
- 현재 상태 → docs/architecture/payment.living.md
- 왜 → docs/adrs/0001-payment-idempotency.md
```

</details>