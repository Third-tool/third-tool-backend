# Rule: update-docs/table-spec 갱신 프로토콜

> **코드 기준 실제 RDB 스키마**를 BC 단위로 기록한다.
> `private-docs/table/{bc}.md` (개발자가 확정해 올리는 목표 스펙)와 대비되는 **실측 사실 기록**이다.
> 두 영역이 불일치하면 사실을 기재하고 불일치를 비고에 명시 — 사용자에게 정정 권한 위임.
> 본 규칙은 [`update-docs.md`](../update-docs.md) §1 패키지 일람의 table-spec 패키지 상세이다.

---

## 1. 위치·파일 구성

- **위치**: `update-docs/table-spec/`
- **파일명**: `{BC}.md` — BC 이름 PascalCase 유지 (현재: `Card.md`, `Deck.md`).
- 새 BC 도입 시 BC 단위로 새 파일 추가.
- 한 파일 = 한 BC의 모든 테이블 (cards, keyword_cues, card_tag, ... 같은 BC 내부 테이블 전부).

---

## 2. 갱신 트리거

다음이 발생한 Story 종료 시 일괄 갱신.

- 새 Flyway 마이그레이션(`V*__*.sql`) 추가.
- JPA 매핑 변경 (컬럼 추가/제거/타입 변경, 인덱스 변경, FK 변경).
- 새 엔티티 추가 (BC 내부).
- `private-docs/table/{bc}.md` 명세와 코드가 **새로 불일치**하기 시작했거나, 기존 불일치가 해소됨.

---

## 3. 파일 골격

각 BC 파일은 BC 내부 테이블을 번호 순으로 나열한다.

```markdown
## {N}. {table_name}

{테이블이 무엇을 저장하는지 1-2문장}

| No. | 컬럼 한글명 | 컬럼 영문명 | 데이터 타입 | 제약 조건 | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | ... | ... | ... | ... | ... | ... |

**인덱스 / 제약조건**

| 종류 | 이름 | 대상 컬럼 | 비고 |
| --- | --- | --- | --- |
| FK | ... | ... | ... |
| INDEX | ... | ... | ... |
| UNIQUE | ... | ... | ... |
```

- "비고" 컬럼에 `private-docs/table/{bc}.md` 명세와 불일치하면 `※ 코드 기준 ... — 명세와 불일치` 명시 (현재 `Card.md` 사례 참조).
- Soft Delete 컬럼·Audit 컬럼은 모든 테이블에서 동일 표기로 반복 기재.
- 코드의 사실을 적는다 — 추측/희망/계획은 제외.

---

## 4. 갱신 절차

1. Story 작업 중 스키마 변경이 발생하면 메모(대화 컨텍스트).
2. Story 종료 push **직전**, 영향받은 BC 파일과 변경 요약을 사용자에게 보고.
3. 사용자 승인 → 해당 `{BC}.md` 갱신.
4. 별도 커밋: `docs(table-spec): {BC}에 {요약} 반영 [Story-{NNN}-{N}]`.

---

## 5. private-docs/table/{bc}.md 와의 분업

| 측면 | `private-docs/table/{bc}.md` | `update-docs/table-spec/{BC}.md` |
| --- | --- | --- |
| 성격 | 개발자가 확정해 올리는 **목표 스펙** | Claude가 코드에서 추출한 **실측 사실** |
| Write 권한 | 사용자만 (Claude는 read-only) | Claude가 본 규칙에 따라 자동 갱신 |
| 두 문서 불일치 시 | 사용자에게 정정 권한 — Claude는 사실만 적고 불일치를 비고에 명시 |

---

## 6. 커밋 규칙

- scope: `table-spec`
- 메시지 예: `docs(table-spec): Card에 card_status_history 인덱스 변경 반영 [Story-{NNN}-{N}]`
- 한 Story에서 여러 BC가 영향받으면 **BC별로 커밋 분리** (각 BC = 1 커밋).

---

## 7. 금지

- 코드에 없는 컬럼·인덱스를 "계획 중"으로 추가 금지.
- `private-docs/table/{bc}.md`를 본 파일로 덮어쓰지 않는다 (두 영역의 권한이 다르다).
- BC 외부 테이블(다른 BC 소유)은 자기 BC 파일에 적지 않는다 — 해당 BC 파일에 가서 적는다.
