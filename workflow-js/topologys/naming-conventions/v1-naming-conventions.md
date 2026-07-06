# Pinned Topology — `naming-conventions` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 클래스·용어·pivot 이력은 여기 없다)

**목적**: 도메인 용어 명명·pivot·아카이브의 반복 원칙을 pin. `context-engineering`이 문서 배치를 다룬다면, 본 파일은 **용어 pivot 이력·ADR 등록·별도 커밋 트랙** 원칙을 다룬다.

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

1. **도메인 용어 rename·pivot 작업 시 이 파일을 먼저 읽는다.** §2의 ADR·별도 커밋·아카이브 원칙은 본 구간 **고정 제약**.
2. **용어 rename을 코드 커밋 안에 슬쩍 섞으려는 정황이 보이면 STOP하고 보고한다.** 별도 커밋 트랙 위반.
3. **ADR 등록 없이 도메인 용어 pivot을 시도하려는 정황이 보이면 보고**한다.
4. **폐기된 용어를 SUPERSEDED 표기 없이 즉시 삭제하려는 정황이 보이면 보고**한다. 계보 추적 실종.
5. **DOMAIN.md·glossary 갱신 없이 도메인 용어를 변경하려는 정황이 보이면 보고**한다.
6. **이 파일에 실제 용어 pivot 이력을 적지 않는다.** 그건 ADR·glossary에.

---

## 2. The pinned topology

### Nodes
- `도메인 용어` — Aggregate·Entity·VO·enum 값의 명칭
- `용어 pivot` — 기존 용어의 이름·의미 변경 (예: 동사 → 명사구)
- `ADR 등록` — 용어 변경 근거 문서화 (`docs/adr/ADR{NNN}-*.md`)
- `별도 커밋 트랙` — 용어 변경 커밋은 `docs(adr): ...` 또는 `refactor(scope): rename ... [ADR{NNN}]` 로 단독
- `SUPERSEDED 표기` — 폐기된 용어는 문서에서 즉시 삭제 대신 SUPERSEDED 표기 (계보 추적)
- `아카이브 계보` — 이전 용어 → 현재 용어의 이관 이력 (ADR + glossary + DOMAIN.md)
- `DOMAIN.md 갱신` — 용어 변경 시 서사·불변식 반영
- `glossary 갱신` — living-docs 용어 색인 반영
- `깨진 참조 검색` — 코드·문서에서 이전 용어 잔재 (grep) 소진
- `대체 용어 원칙` — 새 용어는 의미·컨텍스트 정합 필수 (사용자 판단 게이트)
- `ubiquitous language` — 코드·문서·UI가 동일 용어 사용 (Domain-Driven 원칙)

### Edges
- 용어 부정합 감지 → 사용자 회의 → `용어 pivot` 결정
- `용어 pivot` 결정 → `ADR 등록` (근거·대안·거부 사유 명시) → `docs/adr/`
- `ADR 등록` → `별도 커밋 트랙` (`docs(adr): ADR{NNN} ...`) : 코드 변경과 분리
- `용어 pivot` 실행 → 코드 rename + `DOMAIN.md 갱신` + `glossary 갱신` : 3축 동시
- 폐기된 용어 → `SUPERSEDED 표기` → 문서에서 계보 유지 (`아카이브 계보`)
- rename 후 → `깨진 참조 검색` → 코드·문서 잔재 소진
- `대체 용어 원칙` : 새 용어가 의미·컨텍스트 정합인지 사용자 판단
- 코드·문서·UI → `ubiquitous language` : 동일 용어 · 언어 불일치 감지 시 리뷰

### Boundaries
- **ADR 경계**: 도메인 용어 pivot 은 **ADR 등록 필수**. ADR 없는 rename 금지 (사소한 rename은 예외 · 도메인 개념 변경 아닐 때).
- **커밋 트랙 경계**: 용어 변경은 **별도 커밋** (`docs(adr): ...` 또는 `refactor(scope): rename ... [ADR{NNN}]`). 코드 기능 변경과 섞지 않음.
- **SUPERSEDED 경계**: 폐기된 용어는 문서에서 즉시 삭제 대신 SUPERSEDED 표기 · 계보 추적 유지 (일정 기간 후 정리).
- **3축 동기화 경계**: 용어 변경 시 코드 rename + DOMAIN.md 갱신 + glossary 갱신 **동시 반영**. 하나라도 누락 시 부정합.
- **참조 잔재 경계**: rename 후 코드·문서에서 이전 용어 잔재 grep · 소진. 잔재 방치 시 혼란 유발.
- **ubiquitous language 경계**: 코드·문서·UI·API 응답이 동일 용어 사용. 언어 (한/영) 병기 시 규칙 문서화.
- **사용자 판단 경계**: 새 용어의 의미·컨텍스트 정합은 **사용자 결정** (도메인 이해도 반영).

### Invariants
- ADR 등록 없이 도메인 용어 pivot한 사례 0건 (사소한 rename은 예외)
  - 감지법: `docs/adr/` vs 도메인 클래스 rename 이력 대조
- 용어 변경 커밋이 기능 변경 커밋과 섞인 사례 0건
  - 감지법: git log · `docs(adr)` · `refactor(scope): rename` 커밋 분리 확인
- 폐기된 용어가 SUPERSEDED 표기 없이 즉시 삭제된 사례 0건
  - 감지법: DOMAIN.md · glossary · ADR index 확인
- 용어 변경 시 코드·DOMAIN.md·glossary 3축 중 하나라도 미갱신 사례 0건
  - 감지법: 3축 diff 매칭
- rename 후 코드·문서에서 이전 용어 잔재가 유지된 사례 0건
  - 감지법: 이전 용어 grep · 잔재 소진 이력 확인
- 코드와 UI·API 응답의 용어 불일치 사례 0건 (ubiquitous language)
  - 감지법: Response DTO 필드명 vs 도메인 용어 · UI 라벨 대조

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 도메인 용어·Aggregate 이름 → `docs/DOMAIN.md`·`living-docs/domain-glossary/`
- 실제 용어 pivot 이력 → `docs/adr/ADR004`·`ADR022`·`ADR023` 등
- 실제 SUPERSEDED 표기 · 폐기 계보 → glossary·SDD 파일
- 실제 rename 커밋 이력 → git log · `refactor(scope): rename ... [ADR{NNN}]`
- ubiquitous language 상세 (한/영 병기 원칙 등) → 향후 확장
- ADR 작성 규칙 → `.claude/rules/adr.md`
- 커밋 컨벤션 → `.claude/rules/git.md` · `.claude/rules/pr-commit.md`
- 왜 이렇게 박혔는지 → `docs/adr/ADR004` (AxisAction→AxisTopic) · `ADR022` (Roadmap/Selection 용어) · `ADR023` (헌법·판례 재정의)

---

## 4. Re-pin trigger

- ADR 등록 원칙 폐기 (도메인 용어 pivot 자유 rename 허용)
- 별도 커밋 트랙 폐기 (기능 변경과 rename 섞기 허용)
- SUPERSEDED 표기 폐기 (즉시 삭제)
- ubiquitous language 원칙 폐기 (코드·UI·API 용어 분리 허용)
- 자동 rename 도구 도입 (LLM 기반 · ADR 자동 생성 등)
- 다국어 도메인 용어 정책 도입 (한국어·영어 병행 관리)
- Ubiquitous Language를 팀 확장으로 이관 (개인 → 팀 boundary 변경)
