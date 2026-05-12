# Rule: private-docs 패키지 분류 및 검색·매칭 프로토콜

> `private-docs/` 하위는 카테고리별로 **패키징된 운영 reference 문서**의 모음이다.
> **개발자가 외부(Claude Desktop 등)에서 확정해 올리는 정책 입력**이며, Claude Code 입장에서는 **read-only**다 (자세한 권한 모델은 §3.2).
> Story가 자신이 필요한 패키지를 선언하면, Claude Code는 검색으로 매칭하여 읽고 작업한다.
> Claude Code가 기록·갱신하는 영역(`update-docs/`)은 본 규칙의 대칭쌍 [`update-docs.md`](./update-docs.md)가 따로 정의한다 — ADR도 그 쪽으로 이관되어 있다.
> workflow 자체에 대한 규칙은 [`workflow.md`](./workflow.md) 참조.

---

## 1. 패키지 분류

`private-docs/` 직속 디렉토리는 각각이 **하나의 reference 카테고리**다. 새 카테고리는 사용자 요청 없이는 추가하지 않는다.

| 패키지 | 위치 | 한 파일이 다루는 단위 | 용도 |
| --- | --- | --- | --- |
| **api** | `private-docs/api/{bc}.md` | BC 단위 API 명세 | Controller/DTO 작성 시 엔드포인트 형태·에러 코드·요청·응답 스키마 근거 |
| **domain** | `private-docs/domain/{bc}.md` + `용어사전.md`, `도메인모델.md` | BC 단위 도메인 모델 | Aggregate · Entity · VO · 도메인 규칙. 도메인 코드 작성 시 단일 진실 소스 |
| **table** | `private-docs/table/{bc}.md` | BC 단위 RDB 스키마 매핑 | JPA 엔티티 매핑, Flyway 마이그레이션 작성 근거 |
| **test** | `private-docs/test/{bc}.md` | BC 단위 테스트 매트릭스 | 단위·슬라이스·통합 테스트 작성 시 케이스 명세 |

> ADR은 더 이상 `private-docs/`에 두지 않는다. `update-docs/adr/`로 이관되었으며 [`update-docs/adr.md`](./update-docs/adr.md)·[`update-docs.md`](./update-docs.md) 가 정의한다.

**파일명 규칙**: BC 단위 파일은 BC 이름 소문자 (예: `card.md`, `learningfacade.md`, `userschedule.md`). 도메인 모델 폴더는 일부 한글명 파일(`용어사전.md`, `도메인모델.md`)이 BC 횡단 자료로 존재한다.

---

## 2. Story → 패키지 매칭 프로토콜

Story 문서는 자신이 의존하는 reference를 명시한다. Claude Code의 작업 순서는 다음과 같다.

### 2.1 Story가 reference를 **명시한** 경우

Story 본문에 다음과 같이 적혀 있으면:

```markdown
## Reference
- private-docs/domain/learning-facade.md
- private-docs/table/learningFacade.md
- private-docs/test/learningfacade.md
```

→ **명시된 파일을 전부 Read한 뒤** 작업을 시작한다. 일부만 읽고 시작하지 않는다.

### 2.2 Story가 reference를 **명시하지 않은** 경우

Story 본문에서 다루는 BC와 작업 종류를 식별한 뒤, 아래 매칭 표로 **후보 패키지**를 추정한다. 추정한 후보는 **Read 전에 사용자에게 확인을 받는다.** Claude가 임의로 범위를 확장하지 않는다.

| 작업 종류 (Story 본문 단서) | 우선 Read 대상 |
| --- | --- |
| 도메인 모델 변경 (Entity/VO/AR 추가·수정) | `domain/{bc}.md`, `domain/용어사전.md` |
| 새 API 엔드포인트 / Controller / DTO | `api/{bc}.md`, `domain/{bc}.md` |
| DB 스키마 변경 / Flyway 추가 | `table/{bc}.md`, `domain/{bc}.md` |
| 테스트 추가·보강 | `test/{bc}.md`, 대응되는 `domain/{bc}.md` |
| 아키텍처 차원 결정 (BC 분할, 의존 역전 등) | `update-docs/adr/`, `domain/도메인모델.md` |
| BC 간 협력 추가 | 양쪽 BC의 `domain/{bc}.md`, `update-docs/adr/` |

### 2.3 검색 기술 활용

위 매칭 표로 식별이 어려울 때, Claude는 다음 도구를 사용한다.

- **Glob**: `private-docs/**/*{keyword}*.md` 로 후보 파일 탐색
- **Grep**: 도메인 용어(예: `AxisTopic`, `LearningFacade`)로 어느 패키지가 다루는지 역추적
- **`domain/용어사전.md`**: BC 식별이 모호할 때 먼저 참조

검색으로 후보를 좁힌 뒤에도 **2.2의 사용자 확인 원칙**은 동일하게 적용된다.

---

## 3. 읽기·쓰기 규칙

### 3.1 Read 의무

- Story가 명시한 reference, 또는 사용자가 승인한 후보 패키지는 **작업 시작 전 전부 Read**해야 한다.
- 빈 파일(0 byte)을 만나면 그 사실을 사용자에게 보고하고 진행 가능 여부를 확인한다.
- 패키지 문서와 코드 사이에 **모순**이 있으면 임의로 한쪽을 선택하지 않는다 — 사용자에게 어느 쪽이 진실인지 묻는다.

### 3.2 Write 권한 — private-docs는 전면 read-only

- `private-docs/` 하위 문서는 **Claude Desktop 등 외부에서 관리되는 확정 정책 입력**으로 간주한다. Claude Code는 **명시적 사용자 요청이 있기 전까지 절대 수정·생성·삭제하지 않는다.**
- 도메인/테이블/API 명세가 코드 변경에 따라 갱신되어야 해 보일 때도, **Claude Code는 코드만 바꾸고 문서 갱신은 사용자에게 위임**한다 (코드와 문서가 어긋난 상태로 PR을 내는 것은 허용 — 본 규칙에 따른 의도된 분업).
- 사용자가 "이 부분 private-docs도 같이 갱신해줘"처럼 **명시 요청**하면 그때만 Write 가능. 추측·선의로 갱신하지 않는다.
- ADR을 비롯한 Claude Code의 기록 산출물은 본 규칙이 다루지 않는다 — [`update-docs.md`](./update-docs.md) (그리고 ADR은 [`update-docs/adr.md`](./update-docs/adr.md)) 가 그 영역의 갱신 규칙을 정의한다.

### 3.3 인용 규칙

코드를 작성하면서 reference의 특정 규칙을 근거로 할 때는, 커밋 메시지나 PR 본문에 출처를 적는다 (예: `근거: domain/learning-facade.md §AxisTopic 불변식`).

---

## 4. 금지 사항

- Story가 명시하지 않은 패키지를 사용자 동의 없이 작업 근거로 끌어들여 Story 범위를 확장하지 않는다.
- `private-docs/` 하위 디렉토리 구조를 임의로 재편(폴더 이동/삭제/리네이밍)하지 않는다.
- 패키지 문서가 비어 있다고 해서 임의의 내용을 채워 넣지 않는다 — 외부 관리 영역이다.
