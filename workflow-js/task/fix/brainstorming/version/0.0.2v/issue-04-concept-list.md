# Issue: LearningFacade.concept → concepts[] 다중화

## 배경
사용자 지시:
> "learningFacade를 만드는 과정도 ai를 통해서 같이 concept들을 받아 나갈것이고, 그 concept을 받으면서, learningFacade 예시 백엔드 개발자, 기획자 이런 컨셉을 조합을 같이 고민하고 만들어진 조합을 통해서 어울리는 layer를 만들고"

현재 `LearningFacade`의 정체성은 **단일 String** (`concept`)이다. 신 모델은 concept **조합**(예: `["백엔드 개발자", "기획자"]`)으로 정의되며, 이 조합 컨텍스트에서 AI가 layer 후보를 추천한다. 단일 concept 필드는 조합 표현이 불가능하고, AI 프롬프트에도 컨텍스트로 들어갈 수 없다.

## 조사 결과 — 현재 도메인 상태

| 항목 | 현재 |
|---|---|
| BE 필드 | `learning_facade.concept VARCHAR(...)` 단일 |
| FE 사용처 | MapPage에서 `identity`로 로드, 편집 UI는 단일 텍스트 필드 |
| API | `PATCH /api/v1/learning-facade` 에서 concept 단일 필드 수정 |
| AI 활용 | 현재 AxisSuggestionPort 프롬프트에 concept 1개만 삽입 |

## 옵션 비교

**Option A — `learning_facade_concept` 자식 테이블 신설 (1:N) (채택)**
- `learning_facade.concept` 컬럼 폐기, `learning_facade_concept { id, facade_id, value, display_order }` 신설.
- 조합 순서 유지·개별 편집·중복 방지 유리.
- 마이그레이션: 기존 단일 concept 값을 자식 행 1건으로 이관.

**Option B — JSON 컬럼(`concepts JSON`)**
- 단순 구현. FK/정렬/유니크 제약 걸기 어려움.
- MySQL JSON은 인덱싱·검증 부담 있음. 이번 리팩토링은 조합 자체가 도메인 개념이므로 JSON은 과소 표현.

**Option C — 콤마 구분 String 유지**
- 마이그레이션 최소지만 도메인 표현이 문자열 파싱에 의존. 후속 규칙(중복 금지, 정렬) 강제 불가.

## 선택: Option A

## 부속 결정

### concepts 개수 제한
- 하한 1개 (최소 1 concept 필수), 상한은 초기 5개 권장 한도 (도메인 상수). 초과 시 도메인 예외 대신 `isConceptCountExceedsRecommended` 플래그로 안내.

### 중복 정책
- 동일 facade 내 concept value 중복 금지. `UNIQUE(facade_id, value)` + 도메인 검증 이중 방어.

### display_order
- 1-based, `addConcept`가 자동 부여. `reorderConcepts(orderedIds)`로만 재배치 (id 집합 불일치 시 예외 — conventions.md §1.4 규칙 따름).

## 이관 산출물

- **BE-Story #4-1**: `learning_facade_concept` 테이블 신설 + `LearningFacade.concepts` OneToMany 도메인. `addConcept`/`updateConceptValue`/`removeConcept`/`reorderConcepts` 행위 추가.
- **BE-Story #4-2**: Flyway `V{N}__learning_facade_concepts.sql` 마이그레이션 (기존 concept 값 자식 행 이관, 이후 `learning_facade.concept` DROP).
- **BE-Story #4-3**: `PATCH /api/v1/learning-facade` 개편 — concepts 리스트 CRUD 엔드포인트 추가 (`POST /facades/{id}/concepts`, `PATCH .../concepts/{conceptId}`, `DELETE .../concepts/{conceptId}`, `PUT .../concepts/order`).
- **FE-Story #4-4**: identity 편집 UI를 concepts 리스트 편집(추가·삭제·순서 변경)으로 전환. `useLearningFacade` 훅에 concepts 필드 반영.
- **SDD 개정 필요분** (이슈 #8에서 함께 처리): `product-learningFacade.md` concept → concepts 명시, `product-aisuggestion.md` 프롬프트 입력 concept → concepts[] 명시.

## 관련 이슈 / 문서

- 다음 이슈: [#5 Layer 서버 도메인 승격](./issue-05-layer-server-domain.md) — concepts[] 조합이 layer 추천의 입력.
- 후속: [#9 AI 추천 3층 확장](./issue-09-ai-suggestion-3layer.md) — LayerSuggestionPort가 concepts[]를 컨텍스트로 사용.
- SDD 개정: [#8 용어 재정의](./issue-08-terminology-redefinition-sdd.md)에 개정 항목 목록화.
