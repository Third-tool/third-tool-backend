# [비즈니스 로직 보고서] 다중 컨셉 Facade + Axis→Deck 연동 아키텍처
> Issue 2 연장 분석 — 장기 방향성 기반 BC 경계 및 연동 구조 보고서 (2026-06-30)

**목적**: 오늘 대화에서 확인된 장기 방향성("concept 다중값 + AI 페르소나 + Axis→Deck 연동")이  
Issue 2(Facade 1개 vs 다수) 결정 및 향후 BC 설계에 미치는 영향을 정리.  
**선행 보고서**: `./issue2-facade-count-report.md` — A/B/C 결정 분석  
**연계 파일**: `./be.md` Issue 2 · `./fe.md` Issue 2

---

## 1. 신규 장기 방향성 요약 (오늘 대화 입력)

| 항목 | 내용 |
| --- | --- |
| **Facade concept 진화** | 현재 단일 문자열 → 2~3개 다중값 컬렉션으로 진화 예정 |
| **AI 페르소나 연동** | Facade.concepts 배열이 AI 프롬프트의 페르소나 컨텍스트로 삽입됨 |
| **axis 추천 다양성** | 복수 페르소나 컨텍스트 → AI가 더 다양한 axis를 추천 가능 |
| **0.0.1v 종료 기준** | Facade 생성 → Axis 생성 → 로드맵 수동 구성(AxisTopic + LearningMaterial) |
| **Axis → Deck 연동** | Axis 생성이 Learning BC의 Deck 생성을 트리거 |
| **Roadmap → Card 흐름** | 로드맵 콘텐츠(AxisTopic/LearningMaterial) 기반으로 Card를 만들어 해당 Deck에 배치 |

---

## 2. 다중 컨셉 Facade — 의미와 구조

### 2.1 concept 다중화의 실제 의미

```
현재 (단일):
  LearningFacade.concept = "백엔드 개발자"

장기 방향 (다중):
  LearningFacade.concepts = ["백엔드 개발자", "기획자"]
```

단순한 문자열 배열이 아니라 **AI 컨텍스트의 복합 페르소나**다.

- "백엔드 개발자"만 있을 때: AI가 Java·DB·인프라 중심 axis를 추천
- "백엔드 개발자 + 기획자" 조합: AI가 기술 축 외에 PRD 작성·유저 리서치·데이터 분석 같은 cross-domain axis도 자연스럽게 추천
- 유저는 "나는 개발자이면서 동시에 기획도 배운다"는 정체성을 하나의 Facade에 표현 가능

### 2.2 AI 페르소나 컨텍스트로 작동하는 원리

```
[Facade.concepts 배열] → [AI Prompt 시스템 메시지]

예시 시스템 메시지:
"사용자는 [백엔드 개발자, 기획자] 두 가지 역할을 동시에 개발 중인 학습자입니다.
 학습 축(Axis)을 추천할 때 이 두 역할 관점에서 모두 의미 있는 방향을 제안하세요."
```

현재 axis 추천 ai-suggestion product는 이 contexts 배열을 받아서
백엔드 관점의 axis와 기획 관점의 axis를 동시에 제안하는 방향으로 진화한다.

---

## 3. Issue 2 재검토 — 장기 방향이 A/B/C 결정에 주는 영향

### 3.1 핵심 발견

선행 보고서(issue2-facade-count-report.md)에서 B 옵션(다수 Facade 허용)을 논거한  
핵심 시나리오는 이것이었다:

> "백엔드 개발자 + 기획자를 동시에 배운다면, 두 커리어 목적이 독립된 컨텍스트여야 하지 않나?"

**이 시나리오가 오늘 방향성으로 해소된다.**

```
[기존 B 필요 논거]
  "백엔드 개발자" Facade (별도)
  "기획자" Facade (별도)
  → 두 Facade를 동시 운영

[신규 방향으로 해소]
  "백엔드 개발자 + 기획자" Facade (1개)
  concepts = ["백엔드 개발자", "기획자"]
  → 한 Facade 안에서 다중 정체성 표현
```

Facade를 분리하는 것이 아니라 **하나의 Facade 안에 복수 컨셉을 수용**함으로써
B의 "목적별 분리" 요구를 흡수한다.

### 3.2 수정된 결정 구조

| 기존 옵션 | 장기 방향 반영 후 |
| --- | --- |
| (A) 1 Facade 유지 (concept 단일) | **지속 유지.** 단기(v1)에는 이 상태 그대로 |
| (B) 다수 Facade 허용 | **장기 방향으로 불필요.** concept 다중화가 B의 요구를 흡수 |
| (C) active=1 하이브리드 | 커리어 전환 히스토리 보존 관점은 여전히 유효. v1.5 검토 항목 유지 |
| **(신)** concept 다중화 | **v1.5~v2 진화 경로.** ADR 신설 필요 |

### 3.3 단기 결론 (Issue 2 최종)

> **(A) 1 Facade 유지 확정.**  
> 장기 진화 경로는 Facade를 늘리는 것이 아니라 **concept 필드를 다중값으로 진화**시키는 것.  
> 따라서 B(다수 Facade) 방향 전환 비용(DB UNIQUE 폐기, API 전반 수정, FE 복잡도)은 감수할 필요 없음.

---

## 4. BC 경계 분석 — Roadmap BC vs Learning BC

### 4.1 두 BC의 역할 정의

```
┌─────────────────────────────────────────────────┐
│  Roadmap BC (현재 구현됨)                        │
│  "무엇을 배울 것인가" — 로드맵 정의              │
│                                                  │
│  LearningFacade                                  │
│    └── LearningAxis  ← "학습 차원"               │
│          └── AxisTopic  ← "세부 주제"            │
│                └── LearningMaterial  ← "자료"   │
└────────────────────┬────────────────────────────┘
                     │
          [경계 이벤트: Axis 생성]
                     │
                     ↓
┌─────────────────────────────────────────────────┐
│  Learning BC (학습 실행 측)                      │
│  "어떻게 학습할 것인가" — 카드 기반 리뷰         │
│                                                  │
│  Deck  ← Axis 1개 ↔ Deck 1개 대응               │
│    └── Card  ← 플래시카드 (keyword + MainNote)  │
└─────────────────────────────────────────────────┘
```

### 4.2 BC 경계의 의미

Roadmap BC는 **"뭘 배울지"를 설계**하는 공간이다.  
Learning BC는 **"그걸 어떻게 익힐지"를 실행**하는 공간이다.

- 로드맵을 아무리 정교하게 만들어도, 학습 실행(카드 리뷰·복습 스케줄)과는 분리된다.
- Axis가 생성된다는 것은 "이 학습 차원을 실행하겠다"는 의사 표시 → Deck이라는 실행 공간이 열린다.
- Card는 Deck 안에서 생성되며, 그 내용의 재료는 Roadmap BC의 AxisTopic·LearningMaterial에서 온다.

---

## 5. Axis → Deck 연동 설계 분석

### 5.1 연동 시점 옵션

| 항목 | 옵션 1: Axis 생성 즉시 자동 Deck 생성 | 옵션 2: 사용자가 명시적으로 "학습 시작" |
| --- | --- | --- |
| 트리거 | `POST /axes` 성공 → 동일 트랜잭션에서 Deck 생성 | 별도 `POST /axes/{axisId}/deck` or UI 버튼 |
| 1:1 보장 | 항상 보장 | Deck 없는 Axis 존재 가능 (의도된 상태) |
| 노이즈 리스크 | 탐색용으로 만든 Axis에도 Deck 생성 | 없음 |
| 0.0.1v 적합성 | 로드맵 완성 전에 Deck이 먼저 생기는 것이 어색할 수 있음 | 수동 흐름에 적합. 유저가 준비됐을 때 연동 |
| 장기 자동화 | AI가 axis 추천 후 바로 Deck까지 생성하는 흐름에 유리 | 추가 트리거 단계 필요 |

**0.0.1v 권장**: 옵션 2 (수동/명시적 연동)  
로드맵을 완성한 후 "학습 시작" 시점에 Deck이 생기는 것이 사용자 경험상 자연스럽다.  
Axis를 자유롭게 추가·수정하는 로드맵 구성 단계와 실제 학습 실행 단계를 분리.

**장기(v1.5+) 권장**: 옵션 1로 전환  
AI가 axis를 추천하고 바로 Deck을 열어 Card를 생성하는 자동 흐름에는 즉시 생성이 맞다.

### 5.2 Deck 명칭 정책

```
LearningAxis.name = "Java 심화"
   → Deck.name = "Java 심화"  (Axis.name 그대로 사용)

LearningAxis.name = "PRD 작성"
   → Deck.name = "PRD 작성"
```

- 0.0.1v에서는 Axis.name을 그대로 Deck.name으로 사용 (단순성 우선)
- 나중에 사용자가 Deck 이름을 별도로 커스텀하고 싶은 경우 → 별도 `PATCH /decks/{deckId}/name` 엔드포인트로 처리

### 5.3 Deck 소유권 및 구조

```
User
  └── LearningFacade (1개)
        └── LearningAxis (여러 개)
              └── [연동]
              
User
  └── Deck (Axis와 동일한 의미 단위, 여러 개)
        └── Card (여러 개)
```

- Deck의 직접 소유자는 User (LearningFacade를 통해 간접 소속)
- Axis 삭제 시 대응 Deck 처리 정책 결정 필요 (Hard delete? Soft delete? 연결만 끊기?)
- 0.0.1v: Deck은 독립 보존. Axis 삭제해도 Deck과 Card는 남음.

---

## 6. Roadmap → Card 흐름

### 6.1 0.0.1v — 수동 흐름

```
사용자가 Roadmap BC에서:
  AxisTopic "Spring Security 인증 흐름" 을 만들고
  LearningMaterial "공식 문서 링크" 를 붙인다

  → 이 내용을 보면서 Learning BC에서:
  
  Card 직접 작성:
    keyword (앞면): "Spring Security 필터 체인"
    MainNote (뒷면): "...직접 설명 작성..."
    
  → 해당 Deck ("Spring Security" or "Java 심화")에 배치
```

핵심: **Roadmap BC와 Learning BC 사이에 0.0.1v에서는 자동 연결 없다.**  
사용자가 두 공간을 오가며 직접 Card를 만든다.

### 6.2 장기 — AI 자동 흐름 (v2+)

```
[Roadmap BC 입력]
  LearningAxis: "Spring Security"
  AxisTopic:    "인증 필터 체인"
  LearningMaterial: "공식 문서 URL"
  
        ↓ [AI 호출]
  
[Learning BC 출력]
  Card 초안 생성:
    keyword: "필터 체인 진입점"
    MainNote: "[공식 문서 요약 + AI 설명]"
    
  → Deck("Spring Security")에 자동 배치
```

Card 앞면/뒷면의 재료:
- `keyword` 소스: AxisTopic.name (주제명) → AI가 핵심 개념어로 변환
- `MainNote` 소스: LearningMaterial.url 요약 + AxisTopic 설명 → AI가 학습 설명 생성

---

## 7. 0.0.1v 범위 확정 — 현재와 장기의 경계

| 버전 | 범위 | BC 상태 |
| --- | --- | --- |
| **0.0.1v (현재)** | Facade 생성 → Axis 생성 → 로드맵 수동 구성 (AxisTopic + LearningMaterial) | Roadmap BC 완성, Learning BC 독립 운영 |
| **0.0.1v 종료 기준** | 위 수동 흐름이 E2E로 동작 | |
| **v1.5** | Axis 생성 → Deck 자동(or 수동) 연동 | BC 간 연동 이벤트 추가 |
| **v1.5+** | concept 필드 다중값으로 진화 + AI 페르소나 구성 | ADR 필요 |
| **v2+** | AxisTopic → AI Card 자동 생성 + Deck 배치 | Roadmap BC가 Learning BC의 Card 소스가 됨 |

**핵심**: 지금 당장 Deck-Card 연동을 설계할 필요는 없다.  
0.0.1v에서 Roadmap BC의 수동 흐름을 완성하는 것이 우선.  
단, **이 아키텍처를 이해하고 설계하면** 나중에 연동을 붙일 때 BC 경계를 침범하지 않는 설계가 된다.

---

## 8. 결정 필요 항목 (우선순위 순)

| 우선순위 | 결정 항목 | 버전 | fix tier |
| --- | --- | --- | --- |
| 1 | **Axis → Deck 연동 트리거**: 즉시 자동 생성 vs 명시적 연동 | v1.5 첫 fix | `pes` |
| 2 | **concept 필드 진화 설계**: 단일 String → List<String> or 별도 엔티티 | v1.5~v2 | `sdd` + ADR |
| 3 | **Deck 자동 생성 시 명칭 정책**: Axis.name 그대로 vs 사용자 커스텀 허용 | v1.5 연동 시 | `feature-story` |
| 4 | **Axis 삭제 시 Deck 처리**: 독립 보존 vs cascade soft delete | v1.5 연동 시 | `feature-story` |

---

## 9. 선행 보고서 A/B/C 결정 업데이트

`issue2-facade-count-report.md` §7 권장 결정을 이 보고서 결과로 갱신:

| 기존 권장 | 갱신 후 |
| --- | --- |
| (C) 하이브리드 장기 권장 | 장기 방향은 **(A) + concept 다중화**. C(active=1)는 커리어 전환 히스토리 목적에 여전히 유효하지만 우선순위 하락 |
| (B) sdd 수준 재설계 가능성 | **제거.** concept 다중화가 B의 요구를 흡수하므로 Facade 다수 허용 재설계 불필요 |
| 즉시 권장: (A) + FE LF002 폴백 | **유지.** 이 방향이 장기 경로와도 일치함이 확인됨 |

---

## 10. 참조

- 선행 보고서: `./issue2-facade-count-report.md`
- BE 브레인스토밍 원문: `./be.md` Issue 2
- FE 브레인스토밍 원문: `./fe.md` Issue 2
- fix tier 정의: `workflow/task/fix/{tier}/version/0.0.1v/{tier}.md`
- 도메인 의도: `docs/DOMAIN.md` — LearningFacade, Deck, Card BC
- AI 추천 product: `workflow/task/pes/workspectrum/sdd/in-progress/product-aisuggestion.md`
- 작성일: 2026-06-30
