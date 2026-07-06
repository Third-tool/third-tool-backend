# [Fix · FE] Axis↔Deck 완전 통합 대응 — Deck 생성 진입점 폐쇄 & axisId non-null 반영

> 0.0.2v · FE 저장소 대응 (third-tool-fe, 별도 git) · BE 짝: `workflow/task/fix/sdd/version/0.0.2v/fix-axis-deck-full-integration.md`
> **전제**: BE-Story 1·2 머지 완료 후 착수. 그 전에 착수하면 배포 순간 FE→BE 400/404 발생.

---

## 배경

BE fix `fix-axis-deck-full-integration.md`가 Deck 생성 경로를 축 생성 이벤트 자동 유일로 축소:
- 삭제되는 BE 엔드포인트: `POST /api/v1/decks`, `POST /api/v1/learning-facade/axes/{axisId}/decks`
- 승격되는 스키마: `deck.axis_id` NULL → NOT NULL (응답 DTO의 `axisId`·`axisName`이 항상 non-null)
- Axis Soft Delete 도입: 축 삭제 시 소속 Deck 연쇄 soft delete

FE는 (a) 삭제될 엔드포인트를 호출하는 코드를 정리하고, (b) axisId non-null 전제를 스키마·타입에 반영하고, (c) 축 삭제 UX가 소속 Deck의 소멸을 사용자에게 인지시킨다.

**BE fix 결정 근거는 짝 SDD § 3~4 참조. 본 파일은 FE 변경의 구체 지점만 담는다.**

---

## 수정 지점 5건

### 수정 1 — `createDeck` (POST /decks) 클라이언트 함수 제거

**대상**: `features/deck/api/*.ts` (또는 `lib/api/endpoints/deck.ts`) 중 `POST /api/v1/decks` 호출부.

**변경**:
- endpoint 함수 삭제
- 관련 mutation hook 삭제 (`useCreateDeck` 등)
- import 참조 grep 후 dead code 정리

**호출부**: 카드 에디터의 "새 덱 만들기" 흐름에서 사용되었을 가능성 높음 — 수정 3에서 함께 정리.

**검증**: 프로젝트 grep에서 `/api/v1/decks` 문자열·`createDeck` 심볼 모두 미검출.

---

### 수정 2 — `createAxisDeck` (POST /axes/{axisId}/decks) 클라이언트 함수 제거

**대상**: 선행 fix(`fix-deck-axis-visibility.md` § 14)에서 신설했다고 기록된 `createAxisDeck` 함수. 아마 `features/learning-facade/api/*.ts` 또는 `features/deck/api/*.ts`.

**변경**:
- endpoint 함수 삭제
- mutation hook 삭제
- 호출부 (카드 에디터의 "축에 새 덱 추가") 삭제 — 수정 3에서 함께 정리

**BE 이력 인용**: 선행 fix가 신설한 것을 본 fix가 되돌리는 형태. 선행 fix § 11 "FE 저장소" 항목이 본 fix로 무효화됨을 이력에 남긴다 (backend-boundary 문서 있으면 갱신).

---

### 수정 3 — 카드 에디터 "새 덱 만들기" UI 진입점 제거

**대상**: 카드 생성/편집 화면에서 Deck 선택 컴포넌트.

**변경 전 (예상 구조)**:
```
[Deck 선택 드롭다운]
  ├ 기존 Deck 목록
  └ + 새 덱 만들기 ▼
      ├ 자유 덱 (parentDeckId만 지정)     ─ createDeck 호출
      └ 축에 덱 추가 (axisId 지정)         ─ createAxisDeck 호출
```

**변경 후**:
```
[Deck 선택 드롭다운]
  └ 기존 Deck 목록 (축별 그룹핑, axisName 표시)
    ※ 새 Deck이 필요하면 학습 지도에서 축을 먼저 만드세요.
```

- "새 덱 만들기" 버튼·모달·submit 핸들러 전체 제거
- "덱이 없다면 축을 먼저 생성" 안내 문구를 dropdown 빈 상태에 표시
- Deck 목록은 이미 axis별 그룹핑 가능 (선행 fix가 응답에 `axisId`/`axisName` 노출)

**의존**: 
- FE가 사용하던 useCreateDeck / useCreateAxisDeck mutation → 수정 1·2에서 삭제됨. 컴파일러가 미사용 컴포넌트를 잡아줌.
- 학습 지도 화면의 "축 만들기" 흐름은 그대로 (BE Axis 생성 이벤트가 Deck을 자동 생성).

---

### 수정 4 — Deck 스키마 `axisId` non-null 반영

**대상**: `lib/api/schemas/deck.ts` (또는 `features/deck/schemas/*.ts`)의 zod 스키마.

**변경 전 (선행 fix 반영 상태)**:
```ts
export const DeckSummarySchema = z.object({
  deckId: z.string(),
  name: z.string(),
  // ...
  axisId: z.string().nullable(),      // 선행 fix가 nullable로 도입
  axisName: z.string().nullable(),
});
```

**변경 후**:
```ts
export const DeckSummarySchema = z.object({
  deckId: z.string(),
  name: z.string(),
  // ...
  axisId: z.string(),                 // ◀ non-null 승격 (BE가 NOT NULL 승격)
  axisName: z.string(),               // ◀ non-null 승격
});
```

- `Detail`, `Create` 스키마도 동일하게 `.nullable()` 제거
- 축별 그룹핑 로직에서 `axisId ?? ...` fallback 코드 삭제 가능

**검증**: 스키마 갱신 후 타입 컴파일 → `?.` / `?? "..."` null 방어 코드 발견 지점 정리.

**주의**: BE-Story 2 배포 시점 이전에 배포되면, 아직 axisId=null인 응답에서 zod validation 실패. **BE-Story 2 배포 완료 확인 후 FE 배포**.

---

### 수정 5 — 축 삭제 UX: 연쇄 소프트 삭제 안내

**대상**: 학습 지도 화면의 축 삭제 확인 다이얼로그.

**변경 전**:
```
[다이얼로그]
  "이 축을 삭제하시겠습니까?"
  [취소] [삭제]
```

**변경 후**:
```
[다이얼로그]
  "이 축을 삭제하시겠습니까?"
  ※ 이 축에 속한 덱과 카드도 함께 아카이브됩니다.
     (설정에서 복원할 수 있어요.)
  [취소] [삭제]
```

- BE는 축 softDelete 시 소속 Deck을 연쇄 softDelete
- 사용자에게 "카드가 사라진 것 처럼 보이는" 이유를 사전 안내
- (선택) 삭제 전에 `GET /learning-facade/axes/{axisId}/cards` 카드 수를 fetch해 "N개의 카드 포함" 표시 — YAGNI 판단 후 결정

---

## 수정 순서 및 배포 순서

**BE 배포 완료가 선행 조건**. FE 배포 순서는 다음:

1. BE-Story 1 (Axis Soft Delete) 배포 완료 확인
2. BE-Story 2 (Deck 경로 단일화 + axis_id NOT NULL) 배포 완료 확인
3. FE 브랜치 분기: `fix/axis-deck-full-integration`
4. 수정 4(스키마 non-null) → 수정 1·2(엔드포인트 함수 제거) → 수정 3(UI 진입점 제거) → 수정 5(다이얼로그 안내) 순차 커밋
5. FE 배포 (사용자 M1 단계 — 다운타임 무관)

**롤백**: BE 롤백 발생 시 FE도 이전 버전으로 되돌림. 스키마 관대(`.nullable()`) 상태의 이전 FE와 non-null 응답 BE 조합은 안전(zod가 non-null도 nullable 스키마에 통과).

---

## 검증

- **수동 시나리오 A**: 학습 지도에서 축 A 생성 → 카드 에디터 진입 → Deck dropdown에 축 A 이름이 붙은 자동 Deck 표시 → 카드 저장 → today 노출
- **수동 시나리오 B**: 학습 지도에서 축 A 삭제 다이얼로그 → 안내 문구 확인 → 삭제 → 카드/덱 사라짐 확인
- **회귀**: 카드 에디터에서 "새 덱 만들기" UI 요소 없음 확인
- **타입 체크**: `axisId ?? ...` 관련 null 방어 코드가 컴파일 시 warning으로 잡히면 정리

---

## 관련 문서

- **BE 짝 SDD**: `workflow/task/fix/sdd/version/0.0.2v/fix-axis-deck-full-integration.md`
- **선행 fix (FE 기구현 완료)**: `workflow/task/fix/sdd/version/0.0.2v/fix-deck-axis-visibility.md` § 14
- **FE Product SDD (영향)**: `workflow/task/pes/fe/fe-workspectrum/sdd/done/product-card.md`, `done/product-learningFacade.md` — [명세 변경 이력] 블록 추가 대상
- **연계 브레인스토밍**:
  - `workflow/task/fix/brainstorming/version/0.0.2v/issue-deck-axis-integration.md`
  - `workflow/task/fix/brainstorming/version/0.0.2v/issue-card-axis-recognition.md`

---

## Open Questions

1. **`createDeck` / `createAxisDeck` 심볼명 실제 확인** — 위 이름은 fix-deck-axis-visibility.md § 14 및 관례 기준. FE 저장소 grep으로 실제 파일 경로·심볼명 확인 후 착수.
2. **카드 에디터 "새 덱 만들기" 실제 UI 컴포넌트 이름** — 착수 시 코드 스캔.
3. **backend-boundary 문서 존재 여부** — 존재하면 스키마·엔드포인트 변경 기록 대상.
4. **삭제 다이얼로그에 카드 수 표시 여부 (수정 5 선택)** — UX 판단, 별도 story 분리 가능.

---

*작성일: 2026-07-01 | 기반: BE `fix-axis-deck-full-integration.md` | 상태: **미시작 (BE-Story 2 배포 후 착수)***
