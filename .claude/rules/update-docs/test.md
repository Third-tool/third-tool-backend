# Rule: update-docs/test 갱신 프로토콜

> **BC 단위 테스트 관리 문서**를 기록한다 — 어떤 테스트가 존재하고 어느 계층에서 무엇을 검증하는지의 인벤토리.
> [`test-conventions.md`](../test-conventions.md)(테스트 작성 컨벤션)와는 다르다 — 본 규칙은 결과물 기록 + Story/Epic 종료 시 테스트 동반 작성 의무.
> 본 규칙은 [`update-docs.md`](../update-docs.md) §1 패키지 일람의 test 패키지 상세이다.

---

## 1. 위치·파일 구성

- **위치**: `update-docs/test/`
- **파일명**: `{bc}.md` — BC 이름 소문자 (현재: `card.md`, `learningfacade.md`, `review.md`).
- 한 파일 = 한 BC의 모든 계층 테스트 인벤토리 (단위 + 슬라이스 + 통합).
- 신규 BC가 도메인 업데이트와 함께 등장하면 해당 BC의 `{bc}.md` 파일을 **함께 신설**한다.

---

## 2. 인벤토리 갱신 트리거

다음이 발생한 Story 종료 시 일괄 갱신.

- 새 테스트 클래스·메서드 추가 (의미 있는 검증 단위가 늘었을 때 — 사소한 케이스 추가는 묶어서 한 줄로).
- 테스트 전략 변경 (Classist ↔ Mockist 전환, Mock 대상 변경).
- 신규 BC 도입 — 첫 테스트와 함께 BC 파일 신설.
- 도메인 행위·VO·Service 추가로 테스트 매트릭스가 확장됨.

---

## 3. 도메인 변경 동반 테스트 작성 의무 (Story/Epic 종료 ceremony)

**원칙**: Story 또는 Epic 안에서 BC의 도메인이 갱신되었으면, 그 Story 안에 **해당 BC의 단위 테스트와 슬라이스 테스트를 동반 작성**한다. Story/Epic 종료 push 직전에 매칭을 점검하고, 누락이 있으면 보강 커밋 후 종료한다. 인벤토리(`update-docs/test/{bc}.md`)도 같은 시점에 갱신한다.

### 3.1 무엇이 "도메인 업데이트"인가

다음이 한 가지라도 발생한 Story는 본 §3의 의무가 적용된다.

- 신규 Aggregate Root / Entity / VO / Domain Service 추가
- 기존 도메인 객체에 새 행위 메서드 추가·수정
- 새 검증 규칙 또는 새 ErrorCode 등록
- Repository Port 신규 또는 쿼리 메서드 추가
- Controller·Request / Response DTO 신규 또는 응답 스키마 변경
- Flyway 마이그레이션 추가 (스키마 변경)

세부 트리거 정의는 [`test-conventions.md`](../test-conventions.md) §10이 단일 진실 소스이며, 본 §3은 그 트리거가 발생한 Story의 종료 ceremony를 정의한다.

### 3.2 BC 단위 동반 작성 매트릭스

각 BC마다, 도메인 변경 유형별로 다음 종류의 테스트를 동반 작성한다.

| 도메인 변경 유형 | 단위 테스트 | 슬라이스 테스트 |
| --- | --- | --- |
| VO / Entity / Aggregate Root 행위 | ✅ Classist | — |
| Domain Service (순수) | ✅ Classist | — |
| Domain Service (Repository 의존) | ✅ Mockist | — |
| Application Service 조율 로직 | ✅ Repository Mock | — |
| Repository 쿼리 / 매핑 | — | ✅ `@DataJpaTest` + 실제 H2 |
| Controller / DTO 직렬화 | — | ✅ `@WebMvcTest` + Service Mock |
| 새 ErrorCode → HTTP 응답 형식 | — | ✅ Controller Slice 또는 통합 |
| Flyway 마이그레이션 매핑 정합성 | — | ✅ Repository Slice |

전략·명명·픽스처 규칙은 [`test-conventions.md`](../test-conventions.md) §2(계층별 전략)·§4(명명)·§5(명시 검증 항목)·§7(픽스처)를 그대로 따른다.

통합 테스트(`@SpringBootTest`)는 본 의무 범위 밖이다 — 다중 BC 협력 시나리오로 사용자가 명시 요청하거나 Story가 요구할 때만 작성.

### 3.3 테스트 파일 생성·확장 절차

1. 변경 대상 BC의 테스트 디렉토리(`src/test/java/com/example/thirdtool/{BC}/{layer}/`)를 확인.
2. 기존 테스트 클래스에 케이스 추가가 자연스러우면 추가, 새 검증 대상이면 **새 파일 생성**.
   - 단위 테스트 위치: `{BC}/domain/`, `{BC}/application/` 패키지의 동일 위치.
   - Repository Slice: `{BC}/infrastructure/persistence/` (`@DataJpaTest`).
   - Controller Slice: `{BC}/presentation/` (`@WebMvcTest`).
3. 파일명·메서드명은 `test-conventions.md` §4를 따른다 (`{대상행위}_{상황}_{기대결과}` 한글 허용).
4. `./gradlew test --tests "..."`로 통과 확인 후 의미 단위 커밋 ([`pr-commit.md`](../pr-commit.md) §2).
5. **단위 테스트 커밋**과 **슬라이스 테스트 커밋**은 분리한다 ([`pr-commit.md`](../pr-commit.md) §2 표).

### 3.4 Story/Epic 종료 시 매칭 점검

Story 종료 push **직전**, Claude Code는 다음을 자체 점검 후 사용자에게 1줄씩 보고한다.

1. **단위 테스트 매칭** — 본 Story의 도메인 행위·VO·Service 변경 각 항목에 대응되는 단위 테스트가 있는가.
2. **슬라이스 테스트 매칭** — Repository 쿼리·Controller·DTO·ErrorCode·Flyway 변경 각 항목에 대응되는 슬라이스 테스트가 있는가.
3. **누락 보강** — 매칭 미충족 항목이 있으면 즉시 보강 커밋 후 종료. 사용자가 "의도적 미작성"을 명시한 경우만 제외 (사유는 PR 본문 "트레이드오프"에 1줄 기재).
4. **인벤토리 동반 갱신** — 영향받은 BC의 `update-docs/test/{bc}.md`에 추가된 테스트 클래스·메서드·전략 변경을 §4(파일 골격)에 맞춰 반영. **별도 커밋(`docs(test): ...`)** 으로 분리.
5. **BC 파일 신설** — 신규 BC가 도메인 업데이트와 함께 도입되었으면 해당 BC의 `{bc}.md` 파일을 첫 작성한다 (§4 골격 따름).

Epic 마지막 Story 종료 시점에는 위에 더해:

6. **Epic 누적 일관성** — Epic 안의 모든 도메인 변경이 테스트·인벤토리에 반영되었는지 종합 점검. 누락 발견 시 보강 또는 사용자 보고.

### 3.5 누락 허용 예외

다음은 §3.4의 누락 보강 의무에서 면제된다 (사유를 PR 본문 "트레이드오프" 또는 "참고 사항"에 1줄 기재).

- 사용자 또는 Story 본문이 명시적으로 "테스트는 다음 Story에서 작성"이라고 분리한 경우.
- 동작·시그니처 변경이 전혀 없는 순수 리팩토링이며 기존 테스트가 그대로 통과하는 경우.
- 외부 의존(외부 API 클라이언트 등)이 아직 구현 중이라 슬라이스 테스트가 무의미한 경우.

위 외의 누락은 보강을 원칙으로 한다.

---

## 4. 인벤토리 파일 골격 (권장)

```markdown
# {BC} BC — 테스트 관리 문서

> **전략**: {Classist / Mockist 혼합 설명}
> **프레임워크**: JUnit 5 + AssertJ + Mockito (등)
> **원칙**: 도메인 로직은 실제 인스턴스로, Repository 협력은 Mock으로

## 테스트 전략 요약

| 계층 | 전략 | 근거 |
| --- | --- | --- |
| VO ... | Classist | ... |
| Aggregate Root ... | Classist | ... |
| Domain Service (Repository 의존) | Mockist | ... |
| Repository Slice (`@DataJpaTest`) | 실제 H2 | 쿼리·매핑 검증 |
| Controller Slice (`@WebMvcTest`) | Service Mock | HTTP 매핑·직렬화 검증 |

---

## {N}. {대상 클래스/모듈} ({계층 표시})

**전략**: Classist | **검증 대상**: ...

### {시나리오 그룹}
- {테스트 메서드명} — 의미 한 줄
- ...
```

- `test-conventions.md` §4(명명)·§5(명시 검증 항목)을 따른 케이스만 인벤토리에 포함.
- 실제 작성된 테스트 메서드명을 그대로 적는다 — 가상의 케이스 금지.
- 단위 / Repository Slice / Controller Slice / 통합을 한 파일 안에서 섹션으로 구분.

---

## 5. 인벤토리 갱신 절차

1. Story 작업 중 새 테스트를 추가하거나 기존 전략이 변경되면 메모.
2. Story 종료 push **직전**, §3.4 매칭 점검과 함께 영향받은 BC 파일·변경 요약(추가된 테스트 N개 / 전략 변경)을 사용자에게 보고.
3. 사용자 승인 → `{bc}.md` 갱신.
4. 별도 커밋: `docs(test): {BC}에 {요약} 반영 [Story-{NNN}-{N}]`.

---

## 6. test-conventions.md 와의 분업

| 측면 | `.claude/rules/test-conventions.md` | `update-docs/test/{bc}.md` |
| --- | --- | --- |
| 성격 | **작성 컨벤션** — 어떻게 쓰는지 (전략·명명·픽스처·트리거 정의) | **결과 인벤토리** — 무엇이 쓰여 있는지 |
| 갱신 주기 | 거의 불변. 컨벤션 변경 시에만 | Story 종료마다 (해당 BC에 변경 있을 때) |
| 본 규칙(`update-docs/test.md`) 과의 관계 | 트리거 정의의 단일 진실 소스 (§10) | 그 트리거 발생 시 ceremony·인벤토리 갱신을 정의 (본 §3·§5) |

테스트 컨벤션과 본 BC 테스트 인벤토리가 충돌하면 **컨벤션이 우선** — 인벤토리는 사실 기록이지 컨벤션 재정의가 아니다.

---

## 7. 커밋 규칙

- **테스트 코드 커밋**: scope = BC 이름 (`feat(card): ...` 가 아닌 `test(card): ...` — `pr-commit.md` §4 type). 단위·슬라이스는 별도 커밋 ([`pr-commit.md`](../pr-commit.md) §2).
- **인벤토리 커밋**: scope = `test`. 메시지 예: `docs(test): card.md에 CardStatusHistoryAppender Mockist 테스트 추가 반영 [Story-{NNN}-{N}]`.
- 한 Story에서 여러 BC 인벤토리가 영향받으면 **BC별 커밋 분리**.
- 인벤토리 커밋은 항상 테스트 코드 커밋 **이후**에 배치 (사실이 먼저, 기록이 그 다음).

---

## 8. 금지

- 작성하지 않은 테스트를 "TODO"로 인벤토리에 적지 않는다 — 인벤토리는 사실.
- 본 파일을 테스트 매트릭스(planning)로 사용하지 않는다 — planning은 Story 문서 / 사용자 영역.
- 한 BC의 테스트 인벤토리를 다른 BC 파일에 적지 않는다.
- §3 의무를 우회하기 위해 도메인 변경을 "리팩토링"으로 위장하지 않는다 — 시그니처·동작 변경이 있으면 리팩토링이 아니다.
