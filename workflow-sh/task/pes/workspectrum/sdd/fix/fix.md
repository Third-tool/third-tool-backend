# [SDD · Fix] 정의 — 마일스톤 진행 중 발생하는 계획 변경 흡수 레이어

> `workflow/task/pes/workspectrum/sdd/` 아래의 `fix/`는 SDD Product의 **마일스톤 진행 도중** 발생하는 계획 변경·리팩토링·요구사항 변화를 담아내는 폴더다.
> `backlog → ready → in-progress → done`의 순차 진행 궤도(=마일스톤)와 달리, fix는 그 궤도가 굴러가는 **와중에** 튀어나오는 정정·재설계·범위 조정을 별도 문서로 잡아둔다.
> 본 문서(`fix.md`)는 SDD 레이어에서 fix를 작성할 때의 규범이다. Product 레벨 규범은 [`../sdd.md`](../sdd.md) 참조.

---

## 1. 마일스톤 vs fix — 무엇이 다른가

| 축 | 마일스톤 (`backlog / ready / in-progress / done`) | fix (`fix/`) |
| --- | --- | --- |
| **진행 방식** | 순서대로 (backlog → ready → in-progress → done) | 마일스톤이 굴러가는 도중, 필요할 때마다 삽입 |
| **트리거** | 사전에 합의된 Product/Epic/Story 계획 | 진행 중 발견된 정책 충돌·요구사항 변경·리팩토링 필요 |
| **문서 성격** | 계획 단위 (앞으로 뭘 할지) | 조정 단위 (원본 계획의 어디를 어떻게 뒤집을지) |
| **원본과의 관계** | 원본 그 자체 | 원본을 **참조하고 부분 폐기/승격/변경** |
| **완결 시** | done으로 이동 후 진실 소스로 인용 | 원본 SDD에 반영 완료 시 아카이브 (`done/` 또는 `version/` 스냅샷) |

**요지**: 마일스톤은 "무엇을 만들 것인가"를 순서대로 밀고 나가는 축. fix는 "밀고 나가다 부딪힌 실패·재고를 어떻게 흡수할 것인가"를 정리하는 축.

---

## 2. SDD 레이어에서 fix가 필요한 순간

SDD(풀버전)는 1~2개월 대형 작업이라 진행 중 다음 시나리오가 흔하게 발생한다.

- **여러 Product 사이의 정책 충돌 발견** — 예: LearningFacade Product의 Axis Hard Delete와 Deck Product의 Soft Delete 정책이 어긋남
- **선행 Product 완료 후 후행 Product 착수 직전에 원본 결정을 뒤집어야 함** — 예: "축=덱" 정책으로 통합하면서 3개 생성 경로 중 2개를 폐기
- **로컬 테스트/실사용 중 원본에서 예측 못 한 실패 모드 발견** — 예: "카드 만들 때 축 인식 실패, 화면 이탈 시 사라짐"
- **ADR로 승격되기 전 단계의 결정 재검토** — 원본 SDD의 §설계 결정 중 1~2개를 뒤집는 판단이 필요할 때
- **다중 BC 협력 재설계가 원본 Product 범위를 넘어감** — Fix-Story로 분리해 별도 트래킹

---

## 3. 파일 명명 규칙

```
fix-<핵심주제>.md               # 현재 진행 중인 fix (본 폴더 직속)
version/<X.Y.Zv>/fix-*.md      # 완료된 fix 스냅샷 (선택)
```

- `<핵심주제>`는 kebab-case, 원본 Product들이 얽힌 핵심 축을 짧게 표현
  - 예: `fix-axis-deck-full-integration.md`, `fix-deck-axis-visibility.md`
- 하나의 fix는 여러 원본 Product를 참조할 수 있고, 자신을 **선행 fix / 후행 fix**로 연결해 체인을 만든다.

---

## 4. 섹션 순서 (요약 표)

한 fix 파일 안에 다음 순서로 필수 8섹션(§0~§7) → 선택 6섹션(§8~§13)이 이어진다. 순서 벗어남 자체가 리뷰 지적 대상.

### Fix 문서 레벨

| # | 섹션 | 필수/선택 | 목적 |
|---|---|---|---|
| — | `# [Fix · SDD] {제목} ({YYYY-MM-DD})` | 필수 | 파일당 1개. Fix 식별자 + 작성일 |
| 0 | `## 0. 메타` | 필수 | 원본 SDD / 선행·후행 fix / 작성자 / 영향 Epic·Story / 연계 브레인스토밍 |
| 1 | `## 1. 배경 — 왜 원본을 바꾸나` | 필수 | 현재 상태 / 발견된 문제 / 왜 지금 |
| 2 | `## 2. 원본 대비 delta (뒤집는 지점)` | 필수 | 폐기 / 승격 / 신설 / 유지 4카테고리 |
| 3 | `## 3. 새 목표 (To-Be)` | 필수 | 최종 아키텍처 요약 (ASCII) · 핵심 플로우 · 마이그레이션 단계 · 환경별 설정 분기 |
| 4 | `## 4. 대안 검토 (Alternatives Considered)` | 필수 | 갈림길마다 Option A/B/C, 거부 사유 |
| 5 | `## 5. Fix-Epic / Fix-Story 분할` | 필수 | 원본 Epic·Story에 어떻게 접목되는가 · Fix-Story SDD Story 형식 (§4.1) |
| 6 | `## 6. ADR 승격 후보` | 필수 | 본 fix의 결정 중 ADR로 남길 것 |
| 7 | `## 7. 진실 소스 반영 계획` | 필수 | 코드 / Flyway / DOMAIN.md / PACKAGE.md / Swagger / FE 저장소 매핑 |
| 8 | `## 8. (선택) 실패 모드 / 관측 갱신` | 선택 | 원본 SDD의 실패 시나리오 표에 변경 사항이 있으면 |
| 9 | `## 9. (선택) 검증` | 선택 | 통합 · 단위 · 슬라이스 · 롤백 절차 |
| 10 | `## 10. (선택) Open Questions` | 선택 | 미결 항목 · 다음 의사결정 트리거 |
| 11 | `## 11. (선택) 자가 점검` | 선택 | 작성자용 checklist |
| 12 | `## 12. (선택) 실행 결과 ({YYYY-MM-DD})` | 선택 | fix 완료 후 실제 실행 결과 · 계획 대비 편차 |
| 13 | `## 13. (선택) 명세 변경 이력` | 선택 | 후행 fix가 본 fix의 결정을 뒤집을 때 이력 표 |

**차이점 (SDD Product vs SDD Fix)**:
- Product 골격은 §1~§15 필수 15섹션 위주. Fix 골격은 §0~§7 필수 8섹션 + §8~§13 선택 6섹션.
- Product에는 없는 **§2 원본 대비 delta 4카테고리** 섹션이 fix 전용 핵심.
- Product에는 없는 **§7 진실 소스 반영 계획** 섹션이 fix 전용 (fix는 원본에 흡수되는 성격).
- 완료된 fix는 §12 실행 결과 · §13 명세 변경 이력을 이력 섹션으로 유지.

---

## 4.1 Fix-Story 형식 — SDD Story와 동일 골격

§5 Fix-Epic / Fix-Story 분할의 각 Fix-Story는 **SDD Story 골격과 완전히 동일한 형식**을 사용한다 (`../sdd.md` §양식 골격 → Story 레벨 참조).

```markdown
### Fix-Story {번호}: {제목}

#### User Story
- As a {역할}
- I want {원하는 행위}
- so that {얻는 가치}

#### 설명
{구현 단서 — 메서드 시그니처·정적 팩토리·불변식·ErrorCode·Port명}

**핵심 파일/메서드**:
- `{FullyQualifiedName}` — {역할 한 줄}
- `{Class}.{method}({param types})` — {행위 한 줄}

#### 완료 기준 (AC)
- Given {선행 상태} / When {트리거} / Then {기대 결과}
- *(엣지 - 사유)* Given ... / When ... / Then ...
- *(예외 - 사유)* Given ... / When ... / Then {ErrorCode + HTTP}

#### Definition of Done
- [ ] 구현 (구체 클래스명 · 파일 경로)
- [ ] 단위 테스트 ({N건}, 해피/엣지/예외)
- [ ] 슬라이스 테스트 (해당 시)
- [ ] 통합 테스트 (해당 시)
- [ ] Flyway 마이그레이션 (해당 시 — `V{N}__*.sql` + 롤백 `R{N}__*.sql`)
- [ ] ErrorCode 등록 (해당 시)
- [ ] ADR 작성 (해당 시)
- [ ] **원본 SDD 반영** (Product-*.md 갱신 · [명세 변경 이력] 블록)

#### 스토리 포인트
{0.5d / 1d / 2d / 3d — Estimable 미달이면 분할}

#### 의존성
- 선행: Fix-Story {N-K} (이유)
- 후행: Fix-Story {N-L} (이유)

#### [명세 변경 이력]   ← 선택. 원안 편차 기록.
- YYYY-MM-DD: {원안 → 실제 구현} 차이 + 변경 사유
```

**차이점 (SDD Story vs Fix-Story)**:
- Fix-Story는 `## [Story N-M]` 대신 `### Fix-Story {번호}:` 헤딩 사용 — H2 소진 방지 (Fix 문서 H2는 §0~§13 골격 예약)
- DoD 마지막에 **"원본 SDD 반영"** 필수 (fix는 원본에 흡수되는 성격)
- Fix-Story는 하나의 Fix-Epic 아래에 위치 — §5는 "단일 Fix-Epic + 3~5개 Fix-Story" 구성이 일반적

---

## 4.2 [명세 변경 이력] 블록 사용법

Fix가 여러 회차로 이어지거나, 진행 도중 원안이 다시 흔들릴 때 다음 두 위치에 이력을 남긴다:

### 4.2.1 Fix-Story 내부 이력

각 Fix-Story 마지막에 다음 블록 추가:

```markdown
#### [명세 변경 이력]
- YYYY-MM-DD: {원안 → 실제 구현} 차이 + 변경 사유
- YYYY-MM-DD: {두 번째 변경} + 사유
```

### 4.2.2 Fix 문서 §13 명세 변경 이력 표

후행 fix가 본 fix의 결정을 뒤집을 때, 본 fix에 §13 섹션을 추가하고 표로 이력을 남긴다:

```markdown
## 13. (선택) 명세 변경 이력

| 후속 fix | 날짜 | 폐기된 결정 |
| --- | --- | --- |
| fix-{후속 이름} — [ADR{NNN}] | YYYY-MM-DD | **§{원본 섹션} — {폐기된 결정 요지}** — {폐기 사유 1줄} |
```

### 4.2.3 §0 메타의 fix 체인

**여러 fix가 참조 체인**을 이루는 경우 (`fix A → fix B → fix C`), 각 fix의 §0 메타에 다음을 명시:
- **선행 fix**: `fix A` (경로) — 어떤 결정을 이어받았는가
- **후행 fix**: `fix C` (경로 · 알려진 경우) — 어떤 결정을 이어받았는가

---

## 4.3 §2 원본 대비 delta 4카테고리 상세

§2는 fix 전용 핵심 섹션. 다음 4카테고리로 원본 대비 변화를 정확히 명시:

| 카테고리 | 의미 | 예시 |
|---|---|---|
| **폐기 (Deprecate)** | 원본에서 살아있던 결정 중 본 fix로 없어지는 것 | 엔드포인트 제거, 팩토리 제거, 정책 번복 |
| **승격 (Promote)** | 원본에서 임시/부분/nullable이던 것을 정식화·강제화 | 컬럼 NOT NULL 승격, Product 스켈레톤 → 정착 |
| **신설 (Introduce)** | 원본에 없던 것을 새로 추가 | 새 도메인 메서드, 새 Flyway 마이그레이션, 새 ADR |
| **유지 (Keep)** | 원본 그대로 유지 (혼동 방지용 명시) | FK ON DELETE 규칙, orphanRemoval, 기존 정책 |

**원칙**: 4카테고리는 항상 4개 다 나타난다. "유지"가 없어 보여도 명시적으로 "유지" 소섹션에 나열해 원본 대비 무변경 지점을 드러낸다.

---

## 5. fix의 수명 (Lifecycle)

```
발견/작성            in-progress                  done
   │                      │                        │
   ▼                      ▼                        ▼
fix/fix-*.md  →  마일스톤 Story 재정렬  →  원본 SDD·ADR·코드에 반영 완료
                                                    │
                                                    ▼
                                        아카이브 → done/ 또는 version/
```

- **작성 직후**: `fix/fix-*.md`에 두고, 영향 받는 Epic·Story를 원본 SDD `in-progress/`에 재분배.
- **진행 중**: fix 안의 Fix-Story를 하나씩 처리하며 원본 SDD의 done 상태를 갱신.
- **완료 시**: fix가 참조하던 원본 SDD의 §설계 결정에 실제로 반영되면, fix는 `done/` 또는 `version/{X.Y.Zv}/`로 이동. §12 실행 결과 · §13 명세 변경 이력 섹션을 이력으로 유지.
- **원칙**: fix가 원본 SDD를 대체하지 않는다. 원본은 원본대로 최종본으로 유지되고, fix는 그 유지를 위한 조정 이력으로 남는다.

---

## 6. fix를 만들지 말아야 할 경우

- **원본 SDD 안의 오탈자·문법 정정** → 원본 직접 수정
- **원본 SDD의 §설계 결정을 뒤집지 않는 단순 코드 리팩토링** → Story 하나로 처리, fix 불요
- **아직 확정되지 않은 아이디어·재검토 초안** → `workflow/기능의 구현/brainstorming/` 또는 별도 초안 문서에 두고, 결정된 후 fix로 승격
- **hot-fix 트랙과 겹치는 결정** → hot-fix는 별도 트랙(`fix/one-line-spec/` 등). SDD Fix는 정책 결정을 담는 규모.

---

## 7. 참고 사례 (gold standard)

본 폴더의 fix 파일들이 실제 gold standard이다. 새 fix 작성 시 다음 사례에서 가장 가까운 패턴을 골라 §0~§7 골격을 그대로 복사한 뒤 내용만 채우는 것을 권장.

| 파일 | 특징 |
| --- | --- |
| `fix-axis-deck-full-integration.md` | LearningFacade·Card·Deck 3개 Product를 가로지르는 "축=덱" 통합 fix. §2 delta 4카테고리 · §4 Option A/B/C 4갈림길 · §5 Fix-Story 3개(순차 의존) · §6 ADR021 발행 · §7 코드/Flyway/문서/FE 매핑. **미시작 상태 사례**. |
| `fix-deck-axis-visibility.md` | Deck 응답 axis 가시화 · 축 스코프 카드 조회 · read-model 단일화 fix. Fix-Story 5개(§5) · ADR020 발행(§6) · §12 실행 결과 · §13 명세 변경 이력(후행 fix가 §4.2 부분 유지 조항 폐기 표 4행). **완료 · 명세 변경 이력 있는 사례**. |

---

## 8. 워크플로우 위치

```
Story 진행 중 발견
   ↓
CLAUDE.md → 원본 SDD의 §설계 결정 뒤집을 필요?
   ↓ Yes
   │        {정책 충돌 · 예측 못 한 실패 모드 · 사용자 지시 변경}
   ↓
   fix/fix-<핵심주제>.md 신설
   ↓
   §0~§7 필수 8섹션 채움 (본 fix.md §4 골격 준수)
   ↓
   Fix-Story 각각 진행 (§5 Fix-Epic 아래 Fix-Story N개)
   ↓
   실행 결과 §12에 기록, 원본 SDD [명세 변경 이력] 블록 갱신
   ↓
   완료 시 → done/ 또는 version/{X.Y.Zv}/ 이동
```

---

## 9. 참조

- SDD Product 레벨 규범: [`../sdd.md`](../sdd.md)
- SDD 예시 파일: [`../in-progress/`](../in-progress/)
- ADR 인덱스: `../../../../../../docs/adr/index.md`
- 도메인 의도 / 패키지 규칙: `../../../../../../docs/DOMAIN.md`, `../../../../../../docs/PACKAGE.md`
- 양식 진화: 본 양식은 SemVer로 진화. 변경 시 `version/{X.Y.Zv}/`에 새 버전을 두고 본 버전은 보존.
