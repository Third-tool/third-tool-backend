# Brainstorming — 횡단 관심사 메모 영역 (버전 스냅샷 운영)

> **목적**: `workflow/product/pes/` 의 다른 폴더(`ready/`, `done/`, `hot-fix/`, `references/`, `feedbacks/`)가 다루지 못하는 **개발 / 데이터 / 운영 / 배포 / AI 전 과정의 횡단(cross-cutting) 관심사**를 PES로 승격되기 전 단계에서 적어두는 장소.
>
> **운영 모델**: brainstorming은 그 시점의 PES·코드·living-docs를 **트래킹한 스냅샷**이다. 그래서 한 줄 한 줄이 "지금 무엇이 비어 있는가"를 그 시점 기준으로 정확히 가리킨다. 시점이 바뀌면 후보의 의미도 달라지므로 **버전 폴더로 분리한다**.

---

## 다른 폴더와의 차이

| 폴더 | 다루는 것 | 형식 |
| --- | --- | --- |
| `ready/` | 작업 직전, 명세가 확정된 Product/Epic/Story | 정식 PES 포맷 |
| `done/` | 구현 완료된 PES | 정식 PES 포맷 |
| `hot-fix/` | 즉시 처리할 짧은 패치 메모 | 1~5 줄 단문 |
| `references/` | PES 저자 가이드·원본 레퍼런스 | 메타 문서 |
| `feedbacks/` | 사용자/리뷰어 피드백 누적 | 자유 형식 |
| **`brainstorming/`** | **횡단 관심사 — 그 시점에 트래킹한 문서들에 비해 비어 있는 지점** | **버전 폴더 + 추천 형식** |

브레인스토밍은 **결정 문서가 아니다**. 충분히 익으면 다음 중 하나의 상태로 전이된다:
- **promoted** — 기존 `product-*.md`에 흡수 또는 신규 Product로 분리됨
- **resolved** — 직접 구현되거나 ADR로 정착됨
- **deprecated** — 의도적으로 폐기 (이유 한 줄)
- **merged** — 다른 후보에 통합

---

## 폴더 구조

```
brainstorming/
├── README.md           ← (본 문서) 영역 정의 + 버전 운영 규칙
├── 0.0.1v/             ← 첫 스냅샷 (현재 최신)
│   ├── snapshot.md     ← 이 버전이 트래킹한 BE/FE/docs 상태
│   ├── dev.md          ← 개발 횡단 카탈로그 (후보 N개)
│   ├── data.md
│   ├── ops.md
│   ├── deploy.md
│   └── ai.md
└── 0.0.2v/             ← (예정) 다음 스냅샷
    ├── snapshot.md     ← 0.0.1v 대비 트래킹 대상 변경 + 후보 상태 표
    ├── CHANGELOG.md    ← (선택) 길이 길어지면 분리
    ├── dev.md
    └── ...
```

**핵심 규칙**:
- 각 버전 폴더는 **그 시점의 완성된 카탈로그**다. 이전 버전을 참조하지 않아도 읽힌다 (self-contained).
- 후보 한 개의 진화 흐름은 **여러 버전을 가로질러** `snapshot.md`의 상태표로 추적한다.
- 항상 **새 버전을 만들 때는 이전 버전을 복사한 뒤 갱신**한다 (in-place 수정 X). 그래야 시간 축이 보존된다.

---

## 버전 명명 (SemVer 응용)

`MAJOR.MINOR.PATCH` `v` (예: `0.0.1v`, `0.1.0v`, `1.0.0v`)

### PATCH (`0.0.X`)
**트리거**:
- 후보 1개 새로 추가
- 후보 1개의 1차 권장이 변경됨
- 미해결 질문이 답변됨
- 한 후보의 PES 승격 경로가 구체화됨

**기준 빈도**: 주 단위 또는 의미 있는 의사결정마다.

### MINOR (`0.X.0`)
**트리거**:
- 트래킹 대상 문서가 큰 폭으로 갱신됨 (예: 신규 Product가 `ready/`에 추가됨, 신규 BC 도입, 큰 ADR 결정)
- 신규 카탈로그 파일 추가 (예: `security.md`, `accessibility.md`) — 5 파트 + α
- 후보 다수(≥3개)의 상태가 promoted/resolved로 전이됨

**기준 빈도**: 월 단위 또는 큰 변화 시점마다.

### MAJOR (`X.0.0`)
**트리거**:
- 운영 규칙 자체 변경 — README의 추천 형식 / 폴더 구조 / 상태 taxonomy 변경
- 카탈로그 분류 체계 자체 변경 (5 파트 → 7 파트 등)

**기준 빈도**: 드물게 — 운영 모델 자체가 부적합해질 때.

> 항상 시각순으로 정렬되는 폴더명을 위해 `v` 접미사 사용. `0.0.1v < 0.0.2v < 0.1.0v < 1.0.0v`.

---

## 새 버전 만드는 절차

1. **이전 버전 폴더 통째로 복사** → 새 버전명 폴더로
   ```
   cp -r brainstorming/0.0.1v brainstorming/0.0.2v
   ```

2. **`snapshot.md` 갱신**:
   - 기준 시점(commit / 날짜)을 새로 기록
   - 트래킹 대상 문서 목록 갱신 (추가/변경/삭제)
   - **"이전 버전 대비 변경" 섹션 추가** — 각 후보의 상태 변화 표 (아래 양식)

3. **각 카탈로그 파일 갱신**:
   - **promoted / resolved / deprecated / merged** 된 후보는 본문 그대로 두되 제목 옆에 상태 뱃지 + 한 줄 설명 + 링크 (PES 경로 / commit / 사유) 추가
   - **pending** 후보는 그대로 또는 정련
   - **신규 후보**는 다음 번호로 추가

4. **`brainstorming/latest` 심볼릭 링크 갱신 (선택)** — `latest/`가 최신 버전을 가리키도록.

5. **본 README.md의 "현재 최신 버전" 표시 갱신** (다음 섹션 참조)

---

## 후보 상태 표시 양식

각 후보 제목 옆에 상태 뱃지를 단다. 이전 버전과 비교해 변화가 있을 때만 표기.

```markdown
## [후보 3] 로컬 개발 환경 표준화 [promoted → product-infra-deploy.md Epic 3, 2026-07-12]

> 신규 입사자 onboard 시간 단축 + dev/prod 환경 차이 최소화.

... (본문은 보존 — 회상용)
```

### 상태 taxonomy

| 상태 | 의미 | 표기 형식 |
| --- | --- | --- |
| `pending` | (기본) 아직 결정 안 됨 | 뱃지 없음 |
| `promoted` | PES product/Epic으로 승격 | `[promoted → {경로}, {날짜}]` |
| `resolved` | 직접 구현 또는 ADR로 정착 | `[resolved → ADR{NNN} 또는 PR #{N}, {날짜}]` |
| `deprecated` | 의도적 폐기 | `[deprecated — {사유 한 줄}, {날짜}]` |
| `merged` | 다른 후보에 통합 | `[merged → {대상 후보}, {날짜}]` |

후보 본문은 **삭제하지 않는다**. 미래의 누군가가 같은 아이디어를 다시 꺼낼 때 거부 사유·승격 경로를 곧장 볼 수 있도록 보존.

---

## `snapshot.md` 의 "이전 버전 대비 변경" 표 양식

```markdown
## 0.0.1v → 0.0.2v 변경

### 트래킹 대상 변경
- 추가: `ready/product-quality-gate.md` 신설 (테스트 게이트 + ArchUnit 묶음)
- 변경: `ready/product-aisuggestion.md` → LLM 어댑터 Epic 추가
- 이동: `ready/product-auth.md` → `done/`

### 후보 상태 전이
| 카탈로그 | 후보 | 0.0.1v | 0.0.2v | 비고 |
| --- | --- | --- | --- | --- |
| dev.md | 1 테스트 게이트 | pending | promoted | → `product-quality-gate.md` Epic 1 |
| dev.md | 6 ArchUnit | pending | merged | → 후보 1로 통합 |
| ai.md | 1 LLM 어댑터 | pending | promoted | → `product-aisuggestion.md` Epic 5 |
| ops.md | 2 인시던트 runbook | pending | resolved | → `docs/runbooks/db-down.md` 작성 |
| dev.md | 5 FE Storybook | pending | deprecated | 1인 운영 규모에 과함 |

### 신규 후보
- ai.md [후보 10] — RAG 도입 검토 (사용자 자료 정합성 강화)
- security.md (신규 카탈로그) — 5 후보 추가
```

---

## 어떤 주제가 본 영역에 오나

다음 5 파트의 **횡단 관심사**가 본 영역의 대상이다 — 한 BC 안에 가둘 수 없는 시스템 전반의 고민들.

| 파트 | 다루는 주제 |
| --- | --- |
| 개발 (dev) | 코드 품질·테스트 게이트·API 컨트랙트·로컬 환경·디자인 시스템·아키텍처 테스트 |
| 데이터 (data) | 백업·GDPR·Soft delete·분석 웨어하우스·audit·실시간 vs 배치 |
| 운영 (ops) | SLO/SLI·인시던트·헬스체크·비용·용량·보안·chaos |
| 배포 (deploy) | Blue-Green/Canary·feature flag·DB 마이그레이션·롤백·릴리스 노트 |
| AI (ai) | LLM 어댑터·프롬프트 버전·품질 평가·환각·비용·피드백·세션 |

새 주제가 위 5 파트에 안 들어가면 **새 카탈로그 파일 생성을 허용** (예: `security.md`, `accessibility.md`). MINOR 버전 bump 트리거.

---

## 한 후보의 추천 형식

각 후보당 다음 4 섹션 + 선택 1 섹션을 채운다.

```markdown
## [후보 N] {제목 — 무엇을 추가하고 싶은가} [상태]

> 한 줄 가치 명제 — 무엇을 더 좋게 만드나

### 배경 (왜 지금 고민하나)
- 현재 코드 / PES / 운영 상황에서 비어 있는 지점
- 그대로 두면 발생하는 위험 또는 기회 비용

### 후보 (A / B / C)
- **A안**: 요지 — 장점 · 비용 한 줄씩
- **B안**: 요지 — 장점 · 비용 한 줄씩
- **C안**: (선택)

### 1차 권장
- 어느 안이 끌리는지 + 사유 1~2줄
- *(확정 아님 — PES 승격 단계에서 재검토)*

### PES 승격 경로
- 어떤 기존 Product에 흡수 가능, 또는 신규 Product 후보
- 의존 Story / ADR 후보

### 미해결 질문 *(선택)*
- 결정에 필요한 추가 정보 / 측정값
```

**원칙**:
- **결정문서 아님** — 1차 권장은 참고. 거부된 안에도 합리적 근거가 있었음을 드러낸다.
- **A/B/C 대안 + 트레이드오프**는 [`references/001.md`](../references/001.md)의 PES 컨벤션과 정합.
- **PES 승격 경로**를 반드시 명시 — brainstorming이 영원히 brainstorming으로 남지 않도록.
- 한 후보당 **12~25 줄** 권장. 길어지면 별도 파일(`{part}-{NNN}-{slug}.md`)로 분리.

---

## 깊은 토론 분리 규칙

한 후보가 1차 권장 합의 후 **세부 설계 토론이 필요해지면** 다음 파일명으로 분리한다:

```
brainstorming/{version}/{part}-{NNN}-{kebab-slug}.md
```

예시:
- `brainstorming/0.0.2v/ai-001-llm-evaluation.md` — AI 카탈로그의 "출력 품질 평가 파이프라인" 후보가 분리된 경우
- `brainstorming/0.0.2v/data-002-gdpr-export.md` — 데이터 카탈로그의 "GDPR 데이터 내보내기" 후보가 분리된 경우

원래 카탈로그(`ai.md`, `data.md`)에는 **포인터 한 줄만 남긴다** (예: `→ ai-001-llm-evaluation.md 로 분리`).

---

## 자가 점검 (새 후보 추가 시)

- [ ] **비어있는 지점**이 코드/PES 기준으로 구체적으로 명시되었는가 (추상적 "잘 안 됨" X)
- [ ] **A/B 대안**이 1개 이상 비교되었는가 (단일 안만 적혀있으면 brainstorming의 의의 약화)
- [ ] **PES 승격 경로**가 명시되었는가 — 영구 brainstorming 방지
- [ ] 기존 `product-*.md`와 **중복되는 주제는 아닌가** — 중복이면 기존 PES 보강 항목으로 분류 표시
- [ ] (새 버전을 만드는 경우) `snapshot.md`에 트래킹 대상 변경 + 후보 상태 전이 표가 갱신되었는가

---

## 자가 점검 (새 버전 만들 때)

- [ ] 이전 버전 폴더를 **복사 후 갱신**했는가 (in-place 수정 X)
- [ ] `snapshot.md`의 기준 commit / 날짜 / 트래킹 문서 목록 갱신
- [ ] 이전 버전 대비 변경 표 작성 (트래킹 대상 변경 + 후보 상태 전이)
- [ ] 카탈로그 본문은 promoted/resolved/deprecated/merged 후보를 **삭제하지 않고 뱃지만 추가**했는가
- [ ] README의 "현재 최신 버전" 표시 갱신

---

## 현재 최신 버전

| 버전 | 작성일 | 트래킹한 BE commit | 비고 |
| --- | --- | --- | --- |
| **[0.0.2v/](./0.0.2v/)** | 2026-06-27 | `6b06166` | generic 도메인 추천 카탈로그 추가 — 7개 후보 |
| [0.0.1v/](./0.0.1v/) | 2026-06-22 | `07301c6` | 초기 스냅샷 — 39개 후보 (모두 pending) |

> 새 버전이 생기면 본 표 상단에 추가하고 이전 행은 그대로 둔다.
