# FE-BE Sync Checkpoints — 0.0.2v (재편 D1 = 2026-07-01)

> **문서 역할**: BE·FE 두 Claude Code 세션을 **동시에 병렬로 굴릴 때**, 중간중간 사용자가 손으로 확인해서 계약 drift · 시그니처 반전 · 어휘 어긋남을 잡아내는 **체크포인트 정의**.
>
> 이 문서는 코드 산출물이 아니다. **사용자가 하루 1~2회 열어서 명령 실행하고 신호 판정**만 하는 운영 문서.
>
> 배경: 이번 M2는 BE 도메인 리팩토링 + FE UI 대응이 **4개 결합점**에서 촘촘하게 물려 있음 (DTO 스위치 · 4-Port record · ADR023 어휘 · Layer Response). 둘을 그냥 방치하면 D5-D6에 drift가 폭발한다.

---

## 체크포인트 지도

```
D1 (수) ──────── D2 (목) ──────── D3 (금) ──────── D4 (토) ──────── D5 (일) ──────── D6 (월) ──────── D7 (화)
   │                 │                                                  │                 │                 │
   ▼                 ▼                                                  ▼                 ▼                 ▼
 [CP-0]           [CP-1]                                             [CP-2]           [CP-3]           [CP-4]
Kickoff        Foundation                                          Contract          E2E              Freeze
sync          drafts sync                                          shape sync        integration       판정
(오전)         (저녁)                                              (저녁)            (오전)            (오후)
```

**5개 체크포인트**:
- **CP-0 (D1 오전)** — Kickoff. 두 세션이 어떤 브랜치·어떤 커밋에서 시작하는지 정렬.
- **CP-1 (D2 저녁)** — Foundation drafts sync. Zod schema · Flyway 테이블 · ADR023 초안의 필드/어휘 정합.
- **CP-2 (D3 저녁)** — Contract shape sync. BE Entity/DTO 확정 후 FE Zod와 실제 컴파일 정합.
- **CP-3 (D5 저녁)** — E2E integration. BE Controller RUNNING + FE MSW handler를 실제 API로 대체해 로컬 통합.
- **CP-4 (D7 오후)** — Freeze 판정. 종료 신호 8개 셀프 체크 후 0.0.2v 동결 또는 0.0.2.1v 패치 발행.

---

## CP-0 — Kickoff Sync (D1 = 2026-07-01 오전)

**목적**: 두 세션이 같은 base commit에서 시작하도록 정렬.

### BE 세션 확인 명령 (working dir = `C:\study\System_Author\porfolio\third-tool\third-tool`)
```bash
git status
git log --oneline -5
git branch --show-current
```

### FE 세션 확인 명령 (working dir = FE 레포)
```bash
git status
git log --oneline -5
git branch --show-current
```

### 판정
- [ ] BE 현재 브랜치: `fix/deck-single-create-path` 완주됐거나 진입 준비된 상태 (아니면 D1 오전에 완주)
- [ ] FE 현재 브랜치: `main` 또는 `develop` 최신 pull 상태
- [ ] BE·FE 각각 새 feature 브랜치 분기 준비 완료 (BE: `feature/lt-epic1-concepts-multiplicity`, FE: `feature/lt-epic1-concepts-ui`)

### 실패 시 대응
- BE `fix/deck-single-create-path` 미머지 → 우선 완주 후 두 세션 새 브랜치 진입
- 두 레포에 uncommitted 변경 남아 있음 → 각 세션에서 처리 지시 후 재확인

---

## CP-1 — Foundation Drafts Sync (D2 = 2026-07-02 저녁)

**목적**: BE Flyway 테이블 + FE Zod schema + ADR023 초안이 **필드명·어휘 축**에서 정합하는지 확인. 이 시점 이후 어긋남을 발견하면 재작업 비용이 붙는다.

### 이 시점까지 완료돼 있어야 할 것

**BE 세션**:
- [ ] Story 1: `V(N+1)__learning_facade_concept_table.sql` 커밋 (facade_id / value / display_order / UNIQUE 포함)
- [ ] Story 6: `V(N+2)__learning_layer_table.sql` 커밋 (facade_id / name / display_order / deleted_at 포함)
- [ ] Story 12: `docs/adr/ADR023-terminology-roadmap-selections.md` 초안 커밋 (roadmap = 헌법 / selection = 판례 확정)
- [ ] Story 11: 4-Port 인터페이스 파일 4개 커밋 (`LayerSuggestionPort.java` 등, record 시그니처 최소 초안)

**FE 세션**:
- [ ] FE-1: `lib/api/schemas/learningFacade.ts` Zod `concepts: z.array(...).min(1).max(5)` 이관 커밋
- [ ] FE-6: `lib/api/schemas/layer.ts` Zod 신설 커밋
- [ ] FE-10: `lib/api/schemas/suggestion/*.ts` 4-Port Zod schema 4종 스켈레톤 커밋
- [ ] FE-11: 어휘 grep 결과 파일화 (아직 수정은 X, grep 결과만 로그)

### 확인 명령

**BE Flyway 컬럼 확인** (BE 세션 또는 사용자):
```bash
# BE 레포에서
grep -A 10 "CREATE TABLE learning_facade_concept" src/main/resources/db/migration/V*.sql
grep -A 15 "CREATE TABLE learning_layer" src/main/resources/db/migration/V*.sql
```

**FE Zod schema 필드 확인**:
```bash
# FE 레포에서
cat src/lib/api/schemas/layer.ts
cat src/lib/api/schemas/learningFacade.ts | grep -A 3 "concepts:"
```

**ADR023 초안 확인**:
```bash
# BE 레포에서
head -30 docs/adr/ADR023-terminology-roadmap-selections.md
```

### Contract check 표

| 축 | BE 확정 | FE 확정 | 정합? |
| --- | --- | --- | --- |
| concept 필드명 (테이블) | `value VARCHAR(?)` | `concepts: z.array(z.string())` value 요소 | 요소 타입 일치하는가 |
| concept 상한 | 도메인 상수(예: 5) + DB 제약 | Zod `.max(5)` | 숫자 일치하는가 |
| layer 필드 | `id, facade_id, name, display_order, deleted_at` | Zod `{ id, name, displayOrder, deletedAt }` | 5필드 이름 일치하는가 (facadeId 노출 여부 결정) |
| ADR023 어휘 (roadmap) | "헌법" | FE 라벨 후보 | 일치하는가 |
| ADR023 어휘 (selection) | "판례" | FE 라벨 후보 | 일치하는가 |
| 4-Port record 필드 | 예: `LayerSuggestion(String name, String description)` | Zod `{ name: z.string(), description: z.string() }` | 필드명·타입 일치하는가 |

### 판정

**GO**: 위 6축 모두 일치 → 두 세션 그대로 진행
**FIX**: 1축 이상 불일치 → 재작업 결정
  - BE가 변경 쉬우면 BE 세션에 수정 지시
  - FE가 변경 쉬우면 FE 세션에 수정 지시
  - 판단 근거: **conventions.md §1.9 (도메인 상수)**는 BE 도메인 코드가 정본. 어휘/상한/필드명 원본은 BE
**WAIT**: BE·FE 어느 한쪽이 CP-1까지 다 못 밀었으면 D3 오전에 다시 CP-1 재수행

### 자주 나오는 어긋남
- **displayOrder vs display_order**: BE Column은 snake_case, FE Zod는 camelCase. Jackson `@JsonProperty` 또는 FE Response DTO 변환 여부 확인. 대개 BE Response DTO에서 camelCase로 노출 → FE는 camelCase 그대로.
- **facadeId 노출 여부**: BE Entity에는 있지만 Response DTO에서 제외할 수 있음. FE Zod에도 optional로 두거나 아예 제외.
- **ADR023 초안 어휘가 사용자 정의와 반대**: BE 세션이 잘못 이해하고 SDD 기존 정의(roadmap=초안)로 문서화했을 가능성. **사용자 정의 = roadmap: 헌법 / selection: 판례** 확인.

---

## CP-2 — Contract Shape Sync (D3 = 2026-07-03 저녁)

**목적**: BE Entity + Response DTO 확정 + FE Zod schema가 **실제 컴파일/파싱 축**에서 정합. MSW handler payload가 실제 BE 응답 shape과 어긋나지 않게.

### 이 시점까지 완료돼 있어야 할 것

**BE 세션**:
- [ ] Story 2: `LearningFacadeConcept` Entity + Repository + 컬렉션 API 커밋. `LearningFacadeResponse` DTO에 `concepts: List<String>` 노출
- [ ] Story 7: `LearningAxis.facade_id → layer_id` FK 재배선 Flyway + 백필 SQL 커밋. Axis Response DTO에 `layerId` 필드 노출 (또는 layer nested 결정)

**FE 세션**:
- [ ] FE-2: `<ConceptsInput>` Chip Input 컴포넌트 커밋 (Vitest 통과)
- [ ] FE-7: `<LayersListPage>` 초안 + MSW handler `GET /facades/me/layers` 커밋
- [ ] FE-5 진행 중: `features/**` `concept` 단일 참조 grep 로그 확보

### 확인 명령

**BE Response DTO 실제 필드 (사용자 또는 BE 세션이 grep)**:
```bash
# BE 레포에서
grep -A 20 "class LearningFacadeResponse" src/main/java/**/*.java
grep -A 20 "class LayerResponse" src/main/java/**/*.java
grep -A 15 "class AxisResponse" src/main/java/**/*.java
```

**FE MSW handler payload 확인**:
```bash
# FE 레포에서
cat src/mocks/handlers/learningFacade.ts
cat src/mocks/handlers/layer.ts
```

**FE Zod parse 시뮬레이션** (FE 세션에 지시):
```
"src/lib/api/schemas/layer.ts의 layerSchema로 
MSW handler payload를 parse해보고, 
어긋난 필드가 있으면 보고해."
```

### Contract check 표

| 검증 | 실제 BE Response 필드 | FE MSW handler payload | FE Zod schema | 통과? |
| --- | --- | --- | --- | --- |
| GET `/facades/me` `.concepts` | 예: `["A","B","C"]` | 동일 shape? | `z.array(z.string())` | |
| GET `/facades/me/layers` | 예: `[{id, name, displayOrder, deletedAt}]` | 동일? | layerSchema? | |
| GET `/layers/{id}/axes` (또는 nested) | axis 응답에 `layerId` 있는가 | | | |
| Timestamp 필드 (`deletedAt`, `createdAt`) | ISO-8601 문자열 | 동일 | `z.string()` 또는 `z.date()` | |

### 판정

**GO**: 실제 BE Response DTO ≡ FE MSW handler payload ≡ FE Zod schema 3중 정합 → 계속
**FIX (BE 원본이 정본)**: 어긋남 있으면 FE 세션에 수정 지시. MSW handler와 Zod를 BE 실제 shape에 맞추기
**FIX (BE 잘못)**: 예: BE가 Response DTO에서 필요 필드 누락 → BE 세션에 수정 지시
**WAIT**: BE Story 2·7 아직 미완이면 D4 오전에 재수행

### 자주 나오는 어긋남
- **nested vs flat**: BE가 `axis.layer.name`을 nested로 노출 vs FE가 `axis.layerName` flat 기대. Response DTO 초안에서 결정.
- **null 처리**: BE `deletedAt` softDelete 후 값 있지만 `@SQLRestriction`으로 조회에서 제외됨. FE Zod에서 `.nullable()` 필요 없을 수도.
- **Enum 문자열**: BE `@Enumerated(EnumType.STRING)` → FE Zod `z.enum([...])`. 값 리스트 일치 확인.

---

## CP-3 — E2E Integration Test (D5 = 2026-07-05 저녁)

**목적**: BE Controller가 실제 로컬 (H2 + `./gradlew bootRun`)에서 RUNNING. FE가 MSW를 끄고 실제 BE API로 붙어 UX가 그림처럼 도는지 확인.

이 시점이 **본 마일스톤의 핵심 검증** — 종료 신호 대부분이 여기에서 판정된다.

### 이 시점까지 완료돼 있어야 할 것

**BE 세션**:
- [ ] Story 3·4·5 완료: `updateConcepts()` 도메인 API + DTO `concepts:string[]` 스위치 + ErrorCode 등록
- [ ] Story 8·9·10 완료: default Layer 백필 + softDelete + Layer Controller 5 엔드포인트
- [ ] Story 11 완료: 4-Port 인터페이스 최종 시그니처
- [ ] `./gradlew build` 통과
- [ ] `./gradlew bootRun` (dev 프로필, H2) 부팅 성공 + Flyway 마이그레이션 오류 없음

**FE 세션**:
- [ ] FE-3·4·5 완료: ConceptsEditPage + ConceptsBar + 단수 concept grep 제거
- [ ] FE-8·9 완료: LayerFormDialog + ConfirmDeleteDialog
- [ ] FE-10 완료: 4-Port Zod + hook 스켈레톤 (실제 사용 미필요)
- [ ] `pnpm build` 통과 + `pnpm test` (Vitest) 통과

### E2E 통합 시나리오 (수동 실행)

**환경 준비**:
```bash
# BE 터미널 1
./gradlew bootRun  # 8080 포트

# FE 터미널 2
# .env.local에 VITE_API_BASE_URL=http://localhost:8080 설정
# MSW 비활성 (env: VITE_ENABLE_MSW=false 또는 dev 모드에서 MSW 조건부 비활성)
pnpm dev  # 5173 or 3000 포트
```

**시나리오 A — concepts[] 다중화 E2E**:
1. 브라우저 → `http://localhost:5173/` 접속
2. 로그인 (기존 시드 사용자)
3. `<LearningFacadePage>` 진입 → `<ConceptsBar>`에 기존 concept 백필된 chip 1개 렌더 확인
4. `<ConceptsEditPage>` 진입 → chip 2개 추가 (`["백엔드", "JPA", "Spring"]`)
5. 저장 → DevTools Network 탭에서 `PUT /facades/me` payload `concepts:["백엔드","JPA","Spring"]` 확인
6. 응답 200 OK + reload 시 3개 chip 순서 유지
7. Edge: 6번째 concept 추가 시도 → 400 + ErrorCode `LEARNING_FACADE_CONCEPTS_LIMIT_EXCEEDED`
8. Edge: blank concept 추가 → 400 + ErrorCode `LEARNING_FACADE_CONCEPTS_BLANK`

**시나리오 B — Layer 도메인 E2E**:
1. `<LayersListPage>` 진입 → "Uncategorized" default Layer + 기존 Axis들 소속 확인
2. `<LayerFormDialog>` → 새 Layer "UI" 생성 → 201
3. `POST /layers/{id}/axes` (또는 UI 진입) → axis "컴포넌트 설계" 생성 → 200
4. `<LayersListPage>` 새 Layer 카드 렌더 + axis 목록 표시
5. Edge: 동일 이름 Layer 생성 시도 → 409 + `LEARNING_LAYER_NAME_DUPLICATE`
6. `<LayerConfirmDeleteDialog>` → 하위 axis 존재 시 정책 문구 노출 + softDelete 후 목록에서 사라짐

**시나리오 C — 4-Port hook 컴파일**:
1. FE 코드 아무 곳에서 `import { useSuggestLayers } from '@/lib/api/hooks/suggestion'` 시도
2. TypeScript 컴파일 성공 + 시그니처가 BE Port record와 일치 확인

### Contract check 표 (실제 API 응답 기준)

| 확인 | 시나리오 | 통과 조건 | 결과 |
| --- | --- | --- | --- |
| concepts[] Zod parse | A-5 | 응답 `concepts:string[]` Zod parse 무오류 | |
| ErrorCode 매핑 | A-7·A-8·B-5 | 400/409 응답 `{code, message}` + FE 인터셉터가 code로 UX 분기 | |
| default Layer 존재 | B-1 | 기존 facade에 "Uncategorized" 1건 발행됨 (마이그레이션 신호와 결합) | |
| 4-Port hook 임포트 가능 | C | TS 컴파일 통과 | |

### 판정

**GO**: 시나리오 A·B·C 모두 그림처럼 통과 → CP-4 진입 준비
**FIX**: 
  - BE 응답 shape 어긋남 → BE 세션에 수정 지시 (Response DTO 조정)
  - FE Zod parse 실패 → FE 세션에 수정 지시 (Zod 필드 정합)
  - ErrorCode 미매핑 → BE에 코드 등록 지시 or FE 인터셉터 확장
**WAIT**: BE bootRun 실패 (Flyway 오류 · Bean 부팅 실패) → BE 세션에 우선 해결 지시. FE는 MSW 유지하며 대기

### 자주 나오는 문제
- **CORS**: `./gradlew bootRun`은 8080, FE dev server는 5173. CORS 설정 확인. dev 프로필의 `application-dev.yml`에 `http://localhost:5173` 허용.
- **로그인 시드**: 로컬 H2에 시드 사용자 있는지 확인. 없으면 `POST /user` 회원가입 먼저.
- **쿠키 SameSite**: dev는 `Lax`, prod는 `Strict`. dev에서 로그인 후 쿠키 안 붙으면 로그인 응답 `Set-Cookie` 헤더 존재 확인.

---

## CP-4 — Freeze 판정 (D7 = 2026-07-07 오후)

**목적**: 종료 신호 8개 셀프 체크 후 0.0.2v 동결 또는 0.0.2.1v 패치 발행 결정.

### BE 종료 신호 체크 (BE `milestone.md` 참조)
- [ ] 머지 신호: Tier 1 12 Story 중 10+ 머지
- [ ] concepts[] 신호: POST /facades/me/concepts 로컬 시나리오 통과
- [ ] Layer 도메인 신호: POST /facades/me/layers 로컬 시나리오 통과
- [ ] 마이그레이션 신호: `./gradlew clean bootRun` Flyway 통과 + 백필 검증
- [ ] 테스트 신호: `./gradlew test` 해피/엣지/예외 3구분 + BUILD SUCCESSFUL
- [ ] Port 골격 신호: 4-Port 컴파일 통과 + 시그니처 확정
- [ ] ADR 신호: ADR023 머지 + index.md 반영
- [ ] 문서 정합 신호: DOMAIN.md 반영 + fix issue 링크

### FE 종료 신호 체크 (FE `milestone.md` 참조)
- [ ] 머지 신호: Tier 1 11 Story 중 9+ 머지
- [ ] concepts[] 신호: dev server에서 ConceptsBar + ConceptsEditPage flow 정상
- [ ] Layer UI 신호: LayersListPage + LayerFormDialog flow 정상
- [ ] MSW 신호: MSW handler Vitest에서 1회 이상 히트 + BUILD SUCCESSFUL
- [ ] 계약 정합 신호: `concept` 단일 grep 0건 + Zod ≡ BE Response DTO
- [ ] AI 계약 신호: 4-Port hook Zod parse 성공 + 컴포넌트에서 임포트 가능
- [ ] 어휘 정합 신호: roadmap/selection 어휘 grep 통과
- [ ] 테스트 신호: `pnpm test` 해피/엣지/예외 통과

### 판정

**동결 (BE 6+/8 AND FE 6+/8)**:
- 두 milestone.md 링크된 산출물 5종(`infra.md`/`performance.md`/`outcome.md`/`cost.md`/`review.md`) 마무리
- `0.0.2v` 동결 태그 또는 PR merge
- `0.0.3v` brainstorming 폴더 신설 트리거

**패치 (BE 또는 FE가 6개 미만)**:
- 0.0.2.1v 브랜치 유지 + 다음 주 초까지 미달 신호 완주
- 두 세션에 명확한 남은 작업 목록 지시
- CP-3 시나리오 재수행 후 재판정

---

## 세션 간 정보 공유 방법 (자동화 없이, 수동 sync)

두 Claude Code 세션은 서로의 대화 컨텍스트를 볼 수 없다. **git commit + push가 유일한 명확한 공유 채널**.

### 원칙
- **한 세션의 진행 상태는 그 세션의 git log가 정본**. 세션에 물어보지 말고 git log를 사용자가 직접 열거나 다른 세션에 grep 지시.
- **BE record 시그니처 · Response DTO 필드 · ErrorCode 코드값**은 BE 레포 정본. FE는 항상 BE 레포를 grep해서 확인.
- **어휘 · ADR 결정**은 ADR 파일이 정본. BE 세션이 ADR을 최신으로 유지.

### 실전 명령

**FE 세션에게 BE Response DTO 확인 지시 예시**:
```
"BE 레포는 C:\study\System_Author\porfolio\third-tool\third-tool 에 있어.
grep으로 src/main/java/**/LayerResponse.java 를 찾아서 
필드 이름·타입 목록 보고해줘. 
FE Zod와 정합 판정도 같이."
```

**BE 세션에게 FE Zod 확인 지시 예시**:
```
"FE 레포는 <FE_REPO_PATH> 에 있어.
src/lib/api/schemas/layer.ts 의 Zod schema 필드 목록을
BE LayerResponse DTO와 대조해서 어긋나는 축 있으면 지적해줘.
어긋난 경우 BE 쪽 조정이 자연스러운지 FE 쪽 조정이 자연스러운지 판단 근거도."
```

**두 레포 모두 접근하는 세션 지시 방법**:
- BE 세션의 CLAUDE.md는 BE 프로젝트만 대상. FE 레포 grep은 명시적 절대경로 필요.
- 반대로 FE 세션이 BE 레포 grep도 명시적 절대경로 필요.
- 도메인 ownership 룰이 있다면(`git.md` §10) — 정보 조회 목적의 read-only는 OK.

---

## Rollback 조건

CP-1/2/3 어디에서든 **재작업 규모가 너무 크면 스코프 축소 결정**:

| 시점 | 스코프 축소 옵션 |
| --- | --- |
| CP-1에서 ADR023 어휘 반전 필요 발견 | BE/FE 모두 어휘 grep 재수행. 반나절 소요. 지속 |
| CP-2에서 Layer Response DTO 재설계 필요 | Tier 1 Layer 계열 (BE 6~10, FE 6~9) 5-6 SP 슬립. Tier 2 완전 취소 → Tier 1 재집중 |
| CP-3에서 BE Controller 안 뜸 | BE Story 10 다음 주 이관 → FE는 MSW 유지 상태로 종료 신호 재정의 |
| CP-3에서 CORS/쿠키 문제로 E2E 안 됨 | 종료 신호 중 "concepts[] 신호"·"Layer UI 신호"를 dev server 단독 UI 렌더로 완화 (E2E 대신) |

**결정 원칙**: **Tier 1 12(BE)+11(FE) 중 각 6+ 머지가 M2 합격**. E2E 통합이 안 되어도 각자 로컬 검증(BE는 test + 통합테스트, FE는 dev server + Vitest)만 통과하면 합격 처리 가능.

---

## 참고

- BE milestone: `workflow/task/milestones/version/0.0.2v/milestone.md`
- FE milestone: `workflow/task/pes/fe/fe-milestones/version/0.0.2v/milestone.md`
- BE fix 이슈: `workflow/task/fix/brainstorming/version/0.0.2v/issue-04 ~ 14`
- BE 세션 룰: `CLAUDE.md`·`.claude/rules/workflow.md`·`.claude/rules/conventions.md`
- FE 세션 룰: FE 레포의 CLAUDE.md (분리)
- 병렬 실행 시 세션 분리·정보 공유·계약 drift 방지에 대한 대화 기록: 사용자 요청 기반 본 문서
