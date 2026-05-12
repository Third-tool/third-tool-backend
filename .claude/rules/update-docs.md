# Rule: update-docs 갱신 프로토콜 (umbrella)

> `update-docs/` 하위는 **Claude Code가 작업 결과를 기록·갱신하는 영역**이다.
> `private-docs/` (개발자가 외부에서 확정해 올리는 read-only 정책 입력)와 명확히 대비된다.
> 본 문서는 update-docs 전체에 적용되는 **공통 규칙**(권한·트리거 분류·기존 양식 미러링·체크리스트·커밋·금지)을 정의하고, 패키지별 상세는 `update-docs/{package}.md` 서브 규칙이 정의한다.

---

## 1. 위치 및 패키지 일람

`update-docs/` 직속 디렉토리는 각각이 **하나의 기록 카테고리**다. 새 카테고리는 사용자 요청 없이는 추가하지 않는다.

| 패키지 | 위치 | 갱신 트리거 | 상세 규칙 |
| --- | --- | --- | --- |
| **adr** | `update-docs/adr/ADR{NNN}.md`, `index.md` | **작업 중 감지 즉시** | [`update-docs/adr.md`](./update-docs/adr.md) |
| **architecture** | `update-docs/architecture/{overview,infra,TechStack,application}.md` | **Story/Epic 종료 시 일괄** | [`update-docs/architecture.md`](./update-docs/architecture.md) |
| **dict** | `update-docs/dict/Ubiquitous-Language.md` | **Story/Epic 종료 시 일괄** | [`update-docs/dict.md`](./update-docs/dict.md) |
| **table-spec** | `update-docs/table-spec/{BC}.md` | **Story/Epic 종료 시 일괄** | [`update-docs/table-spec.md`](./update-docs/table-spec.md) |
| **test** | `update-docs/test/{bc}.md` | **Story/Epic 종료 시 일괄** | [`update-docs/test.md`](./update-docs/test.md) |

새 패키지 추가 시: 디렉토리 + 본 표 행 + `update-docs/{package}.md` 서브 규칙을 사용자 합의 후 동시에 도입한다.

---

## 2. private-docs 와의 분업

| 측면 | `private-docs/` | `update-docs/` |
| --- | --- | --- |
| 성격 | 개발자가 확정해 올리는 **정책 입력** | Claude Code가 작업 결과를 누적하는 **기록 출력** |
| Claude의 Write 권한 | **없음** (read-only). 사용자 명시 요청 시 예외 | **있음**. 본 규칙·서브 규칙이 정의한 트리거에 한해 자동 작성·갱신 |
| Claude의 Read 의무 | Story가 명시한 패키지는 작업 시작 전 전부 읽음 ([`private-docs.md`](./private-docs.md) §3.1) | 작업 중 충돌 확인이 필요할 때 참조 (예: 동일 ADR이 이미 있는지) + 갱신 직전 기존 양식 파악(§4) |
| 코드와 어긋날 때 | 사용자에게 진실을 물음. Claude가 임의 수정 금지 | 트리거가 충족되면 Claude가 즉시·또는 종료 시점에 갱신 |

**원칙**: 같은 사실이 양쪽에 중복되어 보이더라도 (예: 도메인 용어가 `private-docs/domain/용어사전.md` 와 `update-docs/dict/...` 에 모두 등장 가능) **갱신 권한은 update-docs 쪽에만 있다**. private-docs 쪽 정정이 필요해 보이면 사용자에게 보고한다.

---

## 3. 공통 트리거 분류

| 트리거 종류 | 의미 | 해당 패키지 |
| --- | --- | --- |
| **즉시** | 결정/감지 순간 작성. Story 종료를 기다리지 않음 | adr |
| **Story/Epic 종료 일괄** | Story push 직전 후보를 사용자에게 1줄 보고 → 승인 후 갱신 | architecture, dict, table-spec, test |

패키지별 구체 트리거 조건은 각 서브 규칙(§1 표의 "상세 규칙" 열)을 따른다.

---

## 4. 기존 양식 미러링 원칙 (핵심)

update-docs의 각 파일을 갱신할 때, **파일이 비어 있지 않다면 기존 내용의 양식·구조·표기 컨벤션을 최대한 미러링해서 추가한다.** update 행위는 "기존에 관리되어 온 양식에 새 항목을 합류시키는" 행위이지 양식 자체를 재설계하는 행위가 아니다.

### 4.1 적용 절차 (모든 update-docs 패키지 공통)

1. **읽기 우선** — 갱신하려는 파일이 비어 있지 않다면, 갱신 작성 전 **반드시 전체를 Read** 한다. 일부만 읽고 추가하지 않는다.
2. **양식 추출** — 기존 헤딩 깊이, 표 컬럼 구성, 불릿 스타일, 한/영 표기, 코드블록 언어 태그, 항목 인덱스 패턴(예: `## {N}. {name}`)을 파악한다.
3. **그대로 추가** — 새 항목은 추출한 양식을 그대로 따른다. 새 헤딩 레벨, 새 표 컬럼, 새 섹션 순서를 임의로 도입하지 않는다.
4. **양식 한계 발견 시** — 기존 양식으로 표현이 곤란한 새 정보가 있다면, **임의로 양식을 확장하지 말고** 사용자에게 1줄 보고한다. 사용자가 양식 변경을 명시 승인한 경우에만 변경. 그 외에는 기존 양식으로 표현 가능한 범위에서만 적고, 표현 못 한 부분은 보고에서 명시한다.

### 4.2 빈 파일 / 신규 파일 예외

- 0 바이트 파일이거나 헤딩이 전혀 없는 placeholder는 본 미러링 의무에서 면제된다 — 첫 항목부터 해당 패키지 서브 규칙의 **권장 골격**(§3 또는 §4 "파일 골격")을 적용한다.
- 신규 BC 파일을 신설할 때는 같은 패키지의 **다른 BC 파일을 모범으로 미러링**한다 (예: `update-docs/test/learningfacade.md` 신설 시 기존 `card.md`의 양식을 따른다). 같은 패키지에 다른 파일조차 없는 첫 BC라면 서브 규칙의 권장 골격을 적용한다.

### 4.3 양식 진화의 책임 분리

| 행위 | 주체 |
| --- | --- |
| 기존 양식 안에서 항목 추가·갱신 | **Claude Code** (§3 트리거에 따라) |
| 양식 자체의 낡음 판단 및 재설계·교체 | **개발자** (외부 관리 사이클로 주기적 수정) |
| 양식 개선 아이디어가 작업 중 떠오름 | Claude는 작업 중 메모로 두고, Story 종료 보고 시 1줄로 공유 — 자동 반영 금지 |

근거: 양식은 개발자가 외부(Claude Desktop 등)에서 관리하는 reference 입력의 연장선이며, 갱신 사이클이 다르다. Claude Code가 같은 작업 안에서 양식까지 흔들면 개발자의 외부 관리 흐름과 충돌한다.

### 4.4 금지

- 기존 파일의 헤딩 레벨·표 컬럼·정렬 기준을 **사용자 동의 없이** 변경.
- "더 좋은 양식이라고 판단했다"는 이유로 일부 항목만 새 양식으로 작성해 한 파일 안에 양식이 혼재되게 만드는 행위.
- 양식 미러링이 부담스럽다는 이유로 기존 내용을 무시하고 파일 끝에 다른 양식의 새 섹션을 덧붙이는 행위.
- 양식 정리·리팩토링 목적의 일괄 갱신 (이는 개발자 영역).

---

## 5. Story/Epic/Product 종료 시 update-docs 갱신 ceremony (분리 진행)

본 ceremony는 Story 작업 자체(코드·테스트·로컬 검증)와 **분리되어 별도 단계로 진행**한다.
Story 작업 도중에는 update-docs를 건드리지 않는다 — 후보는 메모로만 두고, 본 ceremony에서 일괄·디테일하게 반영한다.

### 5.1 갱신 단위 (3계층)

| 단위 | 트리거 시점 | 다루는 항목 |
| --- | --- | --- |
| **Story** | Story 작업이 끝난 직후 | 이번 Story가 직접 변경·신설한 도메인·테스트·테이블 등의 인벤토리 |
| **Epic** | Epic 마지막 Story 종료 시 **추가로** | 누적 항목의 일관성 점검, BC 단위 종합 정리, architecture 영향 |
| **Product** | 제품 방향·범위에 영향을 준 변경이 있을 때 (드물게) | `architecture/overview.md` 등 상위 자료 |

ADR(즉시 트리거)은 본 ceremony와 별개로 작업 중 이미 반영되어 있다 — 누락이 있으면 §5.3 step 1에서 보강.

### 5.2 절차 — 목차 제시 → 사용자 선택 → 디테일 작성

1. **목차 제시** — Story 작업이 끝나면 Claude는 갱신 가능한 항목을 **단위별로 묶어 번호 매긴 목록**으로 제시한다:

   ```
   [Story 단위]
    1. update-docs/test/{bc}.md — {추가된 테스트 클래스 N건} 인벤토리 반영
    2. update-docs/table-spec/{BC}.md — {스키마 변경 요약}
    3. update-docs/dict/Ubiquitous-Language.md — {신규 용어 N개}
   [Epic 단위] (Epic 마지막 Story인 경우만)
    4. update-docs/architecture/application.md — {BC·레이어 영향}
    5. ...
   [Product 단위] (해당 시만)
    6. update-docs/architecture/overview.md — {제품 범위 변동}
   ```

   각 항목에는 **반영할 변경 요약**을 1줄로 명시한다 (사용자가 선택 판단을 할 수 있도록).

2. **사용자 선택 대기** — 사용자가 항목 번호를 선택할 때까지 **update-docs 작성을 시작하지 않는다** (예: "1, 3 ㄱㄱ", "전부", "1만").

3. **선택 항목 디테일 작성** — 선택된 항목만 §4 미러링 원칙을 따라 **디테일하게** 작성한다:
   - 빈 파일이 아니면 전체 Read → 양식 추출 → 그대로 추가.
   - 새 BC 파일 신설 시 같은 패키지의 다른 BC 파일을 모범으로 미러링.
   - 한 패키지 = 한 커밋 (scope=패키지명). gitignored 영역이면 working tree만 갱신.

4. **종료 보고** — 갱신한 파일 경로 목록과 (커밋된 경우) hash를 보고한다.

### 5.3 목차 제시 직전 자체 점검

목차를 만들기 전 Claude는 다음을 자체 점검한다:

1. **즉시 트리거 패키지** (adr): 작업 중 누락된 트리거가 있는지 마지막으로 확인. 누락 있으면 ADR을 추가 작성 후 별도 커밋 (사용자 선택과 무관, 즉시).
2. **종료 트리거 후보** (architecture, dict, table-spec, test): 이번 Story가 만진 코드/스키마/테스트와 매칭해 후보 추출 → §5.2 step 1 목차에 포함.
3. **인덱스 정합성**: 각 패키지에 `index.md`가 있으면 본 Story 추가/변경분이 반영되었는지 확인.
4. **양식 개선 메모** (선택): §4.3에 따라 작업 중 떠오른 양식 개선 아이디어가 있으면 목차 출력 시 1줄로 공유 (반영은 개발자 결정).

### 5.4 push 판단과의 독립성

본 ceremony는 [`pr-commit.md`](./pr-commit.md) §6 push 판단과 **독립**적으로 진행한다. 사용자는 update-docs 갱신을 push 전·후 어느 시점에든 묶을 수 있다.

### 5.5 금지

- Story 작업 도중 update-docs를 미리·부분 반영하지 않는다 (양식이 흐트러지고 점검 누락 발생).
- 목차 제시 없이 사용자 추가 확인 없이 update-docs를 갱신하지 않는다 (즉시 트리거 adr 제외).
- 사용자가 선택하지 않은 항목을 "권장이라고 판단해" 임의로 함께 갱신하지 않는다.

---

## 6. 커밋 규칙 (공통)

(자세한 컨벤션은 [`pr-commit.md`](./pr-commit.md))

- 커밋 메시지: `docs({package}): {요약} [Story-{NNN}-{N}]`
  - scope는 패키지명 (`adr`, `architecture`, `dict`, `table-spec`, `test`). BC 이름이 아니다.
- update-docs 갱신은 **코드 변경과 별도 커밋으로 분리**.
- 한 커밋에 **여러 update-docs 패키지를 묶지 않는다** — 패키지별로 커밋 분리.
- 같은 패키지 안에서 여러 파일이 영향받았을 때 묶을지 분리할지는 각 서브 규칙을 따른다 (architecture/dict는 묶음 허용, table-spec·test는 BC별 분리).
- PR 본문 "해결 방법" / "참고 사항"에 새로 추가된 항목을 1줄 요약으로 적는다.

---

## 7. 인용 규칙

코드 작성 시 update-docs의 특정 항목을 근거로 할 때 커밋 메시지·PR 본문에 출처를 적는다 (예: `근거: update-docs/adr/ADR003.md`).

---

## 8. 금지 사항

- `update-docs/` 하위 디렉토리 구조를 임의로 재편(폴더 이동/삭제/리네이밍)하지 않는다 — 새 패키지·새 파일 도입은 사용자 요청 시에만.
- 트리거 충족 없이 추측·예상으로 항목을 채워 넣지 않는다 — update-docs는 사실의 기록이지 가설의 저장소가 아니다.
- `private-docs/` 의 내용을 update-docs로 단순 복사하지 않는다 — 두 영역의 권한·성격이 다르며, 동기화는 사용자가 외부에서 관리한다.
- 이미 작성된 update-docs 항목의 **본질적 의미를 사용자 동의 없이 수정·삭제**하지 않는다 (오타·표현 다듬기는 허용; 결정·정의 자체의 변경은 새 항목으로 — adr Supersede 패턴 참조).
- 기존 양식을 사용자 동의 없이 재설계·교체하지 않는다 (§4 미러링 원칙).
