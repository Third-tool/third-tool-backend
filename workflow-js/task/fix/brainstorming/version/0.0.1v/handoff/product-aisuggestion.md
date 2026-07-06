# [Handoff] product-aisuggestion — concept 다중값 진화에 따른 도메인 의존 조율 필요
> issue2b 결론 → `sdd/in-progress/product-aisuggestion.md` 계획 수정 권고 (2026-06-30)

**출처**: `../issue2b-facade-multicontext-deck-arc-report.md` §3 · §7
**대상 product**: `workflow/task/pes/workspectrum/sdd/in-progress/product-aisuggestion.md`

---

## 발견한 불일치

`product-aisuggestion.md`는 **v1 입력 컨텍스트**를 이미 다음으로 설계했다:

```
concept set(2~3개) + composition reason + desired outcome
+ 기존 이름 목록 + optional refresh context
```

그러나 현재 `LearningFacade` 도메인 모델의 concept 필드는 **단일 String**이다.

```java
// 현재 도메인
LearningFacade.concept = "백엔드 개발자"  // 단일 문자열

// product-aisuggestion v1이 전제하는 입력
concept set = ["백엔드 개발자", "기획자"]  // 배열
```

issue2b §3.3 확정 결론:
> "(A) 1 Facade 유지. 장기 진화 경로는 Facade를 늘리는 것이 아니라 **concept 필드를 다중값으로 진화**시키는 것."

issue2b §7 버전 타임라인:

| 버전 | concept 필드 상태 |
|------|------------------|
| **0.0.1v (현재)** | 단일 String |
| **v1.5** | `concepts[]` 다중값으로 진화. ADR 신설 필요 |

---

## product-aisuggestion에 미치는 영향

### 영향 1 — v1 AI 추천 API의 concept 입력 출처

product-aisuggestion v1 설계가 `concept set(2~3개)`를 **요청 body로 받는다면** (클라이언트가 직접 전달):
- 도메인 모델 변경과 독립적 → v1 진행 가능
- 단, FE가 `LearningFacade.concept` 단일값을 배열로 감싸서 전달하는 어댑터 레이어 필요

product-aisuggestion v1 설계가 `LearningFacade.concept`을 **서버에서 직접 읽는다면**:
- 0.0.1v에서는 단일 String밖에 없으므로 `concept set`이 1개짜리 배열로 제한
- v1 API 계약을 `conceptSet: string[]`으로 잡되, 0.0.1v에서는 1개 원소로 동작하고 v1.5에서 실제 다중값으로 채워지는 방식으로 설계하면 비파괴 진화 가능

### 영향 2 — refresh context 재추천 설계와의 정합

issue2b에서 확인된 방향:
> "Facade.concepts 배열 → AI 프롬프트의 페르소나 컨텍스트로 삽입 → 더 다양한 axis 추천"

product-aisuggestion.md §설계결정 마지막 항목:
> "축 추천은 동일 concept context에서 반복 reload 가능하게 둔다"

`concepts[]`가 도메인에 저장되기 전까지는 **클라이언트가 매번 concept set을 명시적으로 전달**하는 것이 유일한 방법. v1.5에서 도메인 저장 후에는 서버가 자동으로 컨텍스트를 구성할 수 있음.

---

## 계획 수정 권고

### 권고 1 — v1 API 계약: `conceptSet: string[]` 명시 (현재 명확히 않으면)

```
POST /api/v1/learning-facade/axes/suggestions
Request:
  conceptSet: string[]      ← 0.0.1v에서는 1개, v1.5에서 2~3개
  compositionReason: string
  desiredOutcome: string
  existingAxisNames: string[]
  refreshContext?: string
```

0.0.1v에서 `conceptSet` 길이를 1로 제한하지 말 것 — v1.5 진화 시 계약 깨짐 없이 배열 확장 가능.

### 권고 2 — v1.5 concept 도메인 진화와 연동 지점 미리 표시

product-aisuggestion.md v1.5 섹션(갭 인지형 개인화)에 다음 전제 조건을 추가:
> "LearningFacade.concepts[] 도메인 진화(v1.5 scope, issue2b §7) 완료 후 서버 사이드 자동 컨텍스트 구성 가능. 그 전까지는 클라이언트가 conceptSet 명시 전달."

---

## 조율 필요 항목

| 항목 | 결정 필요 | 영향 버전 |
|------|-----------|-----------|
| concept 입력 출처: 요청 body vs 서버 도메인 조회 | product-aisuggestion 설계 결정 확인 | v1 |
| `conceptSet: string[]` API 계약 확정 | product-aisuggestion / FE 합의 | v1 |
| LearningFacade.concepts[] ADR 신설 | issue2b §7 per "ADR 신설 필요" | v1.5 |

---

## 참조

- 출처 보고서: `../issue2b-facade-multicontext-deck-arc-report.md` §1·§2·§3·§7
- 대상 product: `workflow/task/pes/workspectrum/sdd/in-progress/product-aisuggestion.md` — 설계결정 §v1 입력 컨텍스트, refresh context 정책
- 도메인 현황: `docs/DOMAIN.md` — LearningFacade.concept 단일 String
