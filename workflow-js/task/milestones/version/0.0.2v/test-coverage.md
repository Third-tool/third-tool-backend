# 0.0.2v / Test Coverage

> **목적**: 이번 버전에서 새로 작성/변경된 도메인·계층별 **테스트 매트릭스**를 추적하고, 해피/엣지/예외 3구분과 slice/integration 계층 확보 여부를 성과 지표로 남긴다.
>
> **규범**: `.claude/rules/conventions.md` §4 (테스트 컨벤션). 도메인·Application·Repository·Controller 계층별 전략 준수.
>
> **원칙**: "테스트 통과" 는 최소 조건. 이 파일은 **어떤 케이스를 커버했는지·어떤 케이스를 놓쳤는지**의 자기 검증.

---

## 요약 대시보드

> D7 종료 시 채움. milestone.md 테스트 신호 판정 근거.

| 지표 | 목표 | 실측 | 상태 |
| --- | --- | --- | --- |
| `./gradlew test` 결과 | BUILD SUCCESSFUL | ___ | ☐ |
| 신규 테스트 파일 수 | ≥ 12 (Epic 1·2 + Port 1) | ___ | ☐ |
| 신규 테스트 케이스 수 | ≥ 60 (도메인 30 + slice 15 + integration 15) | ___ | ☐ |
| 해피/엣지/예외 3구분 커버 Story 비율 | ≥ 100% (모든 신규 Story) | ___ | ☐ |
| 회귀 실패 (기존 테스트) | 0건 | ___ | ☐ |
| JaCoCo 라인 커버리지 (신규 도메인 클래스) | ≥ 80% | ___ | ☐ |
| JaCoCo 브랜치 커버리지 (신규 도메인 클래스) | ≥ 70% | ___ | ☐ |

---

## 계층별 테스트 전략 (본 버전 적용)

| 계층 | 대상 | 전략 | 위치 |
| --- | --- | --- | --- |
| 도메인 (VO/Entity/Aggregate) | `LearningFacadeConcept` · `Layer` · `LearningFacade` 신규 API | Classist (실 객체) | `src/test/java/.../domain/model/*Test.java` |
| Domain Service | (본 버전 없음, Epic 3 이후) | — | — |
| Application Service | `LearningFacadeCommandService` (concepts) · `LayerCommandService` (신규) | Repository Mock + 도메인 실 객체 | `src/test/java/.../application/service/*Test.java` |
| Repository Slice | `LearningFacadeConceptRepository` · `LayerRepository` · `LearningAxisRepository` (재배선) | `@DataJpaTest` + H2 | `src/test/java/.../infrastructure/persistence/*RepositorySliceTest.java` |
| Controller Slice | 신규 엔드포인트 (`/facades/me/concepts`, `/facades/me/layers/*`) | `@WebMvcTest` + Service Mock | `src/test/java/.../presentation/*ControllerSliceTest.java` |
| 통합 | Layer 백필 · axis 재배선 · concepts 저장 시나리오 | `@SpringBootTest` + H2 | `src/test/java/.../integration/*IntegrationTest.java` |
| Port 계약 | `LayerSuggestionPort` 등 4 Port | stub Adapter 컴파일 실험 | (본 버전은 슬라이스 없음, 컴파일만) |

---

## Story 별 테스트 매트릭스

### LT Epic 1 — concepts[]

#### Story 1-1: `learning_facade_concept` 테이블 + Flyway

| 케이스 | 유형 | 대상 | 위치 | 상태 |
| --- | --- | --- | --- | --- |
| 마이그레이션 성공 → 테이블 생성 | 해피 (통합) | Flyway V | `V{N}MigrationTest` | ☐ |
| UNIQUE `(facade_id, value)` 위반 → DataIntegrityViolationException | 예외 (slice) | Repository | `LearningFacadeConceptRepositorySliceTest` | ☐ |
| `display_order` CHECK ≥ 0 위반 | 예외 (slice) | Repository | 상동 | ☐ |
| Rollback R{N} 실행 → 테이블 제거 후 재적용 성공 | 회귀 (수동) | Flyway | (수동 스크립트) | ☐ |

#### Story 1-2: `LearningFacadeConcept` Entity + 컬렉션 API

| 케이스 | 유형 | 대상 | 위치 | 상태 |
| --- | --- | --- | --- | --- |
| `LearningFacade.addConcept("A")` → concepts.size()=1, displayOrder=1 | 해피 (단위) | Aggregate | `LearningFacadeTest` | ☐ |
| 5건 상태에서 `addConcept("F")` → 예외 SIZE_INVALID | 예외 (단위) | 상동 | 상동 | ☐ |
| 이미 존재하는 값 재입력 → DUPLICATE | 예외 (단위) | 상동 | 상동 | ☐ |
| `"  A  "` trim 후 기존 "A" 와 중복 → DUPLICATE | 엣지 (단위) | 상동 | 상동 | ☐ |
| 빈 문자열 · whitespace only → BLANK | 예외 (단위) | 상동 | 상동 | ☐ |
| `getConcepts()` 반환값 immutable | 엣지 (단위) | 상동 | 상동 | ☐ |
| displayOrder 자동 1-based 부여 (3건 순차 추가) | 해피 (단위) | 상동 | 상동 | ☐ |

#### Story 1-3: 백필 + `updateConcepts()`

| 케이스 | 유형 | 대상 | 위치 | 상태 |
| --- | --- | --- | --- | --- |
| `updateConcepts(["A","B","C"])` on facade with [X,Y] → 기존 삭제, 신규 3건 | 해피 (단위) | Aggregate | `LearningFacadeTest` | ☐ |
| 동일값 재저장 → `ConceptsChangeRecord.changed = false` | 엣지 (단위) | 상동 | 상동 | ☐ |
| 다건 중 1건 blank → 전체 롤백 | 예외 (단위) | 상동 | 상동 | ☐ |
| 다건 중 중복 → 전체 롤백 | 예외 (단위) | 상동 | 상동 | ☐ |
| 백필 마이그레이션: 단일 `concept="X"` → `concepts[0]="X"` | 해피 (통합) | Flyway | `ConceptBackfillIntegrationTest` | ☐ |
| 백필: `concept=null` facade → concepts 추가 없음 | 엣지 (통합) | 상동 | 상동 | ☐ |

#### Story 1-4: API/DTO 이관

| 케이스 | 유형 | 대상 | 위치 | 상태 |
| --- | --- | --- | --- | --- |
| `POST /concepts { concepts:[...] }` → 201 | 해피 (controller slice) | Controller | `LearningFacadeControllerSliceTest` | ☐ |
| `PATCH /concepts` → 200 + `changed:true/false` | 해피 (controller slice) | 상동 | 상동 | ☐ |
| 응답 body 에 `concepts: string[]` 필드 노출 | 해피 (controller slice) | 상동 | 상동 | ☐ |

#### Story 1-5: 검증 + ErrorCode

| 케이스 | 유형 | 대상 | 위치 | 상태 |
| --- | --- | --- | --- | --- |
| `LEARNING_FACADE_CONCEPTS_SIZE_INVALID` → 400 응답 | 예외 (controller slice) | GlobalExceptionHandler | `LearningFacadeControllerSliceTest` | ☐ |
| `LEARNING_FACADE_CONCEPT_DUPLICATE` → 409 | 예외 (controller slice) | 상동 | 상동 | ☐ |
| `LEARNING_FACADE_CONCEPT_BLANK` → 400 | 예외 (controller slice) | 상동 | 상동 | ☐ |
| `LEARNING_FACADE_CONCEPT_TOO_LONG` → 400 | 예외 (controller slice) | 상동 | 상동 | ☐ |
| 응답 body `{code, message, path, timestamp}` 4필드 | 해피 (controller slice) | 상동 | 상동 | ☐ |

### LT Epic 2 — Layer

#### Story 2-1: `learning_layer` 테이블 + Aggregate

| 케이스 | 유형 | 대상 | 위치 | 상태 |
| --- | --- | --- | --- | --- |
| Layer 생성 성공 | 해피 (단위) | `LayerTest` | 상동 | ☐ |
| name blank → 예외 | 예외 (단위) | 상동 | 상동 | ☐ |
| name > 100자 → 예외 | 예외 (단위) | 상동 | 상동 | ☐ |
| `@SQLRestriction` 자동 필터 (deleted_at != null 조회 제외) | 엣지 (slice) | Repository | `LayerRepositorySliceTest` | ☐ |
| UNIQUE composite `(facade_id, name, deleted_at)` softDelete 후 재사용 | 엣지 (slice) | 상동 | 상동 | ☐ |

#### Story 2-2: Axis `layer_id` FK 재배선

| 케이스 | 유형 | 대상 | 위치 | 상태 |
| --- | --- | --- | --- | --- |
| 3단계 마이그레이션 (NULL → 백필 → NOT NULL) 성공 | 해피 (통합) | Flyway V | `AxisLayerFkMigrationIntegrationTest` | ☐ |
| 백필 후 기존 axis 조회 정상 | 회귀 (통합) | Repository | 상동 | ☐ |
| 백필 실패 시 롤백 R 스크립트 검증 | 예외 (수동) | Rollback | (수동) | ☐ |
| FK ON DELETE RESTRICT: Layer 삭제 시 FK 위반 → 예외 | 예외 (slice) | Repository | 상동 | ☐ |

#### Story 2-3: default "Uncategorized" 백필

| 케이스 | 유형 | 대상 | 위치 | 상태 |
| --- | --- | --- | --- | --- |
| 기존 facade 별 default Layer 자동 생성 | 해피 (통합) | Flyway V | `DefaultLayerBackfillIntegrationTest` | ☐ |
| 기존 axis 100% default Layer 로 이관 | 해피 (통합) | 상동 | 상동 | ☐ |
| `LearningFacade.create()` 신규 생성 시 default Layer 자동 생성 | 해피 (단위) | Aggregate | `LearningFacadeTest` | ☐ |
| softDeleted facade 는 백필 대상 아님 | 엣지 (통합) | Flyway | 상동 | ☐ |

#### Story 2-4: Layer softDelete

| 케이스 | 유형 | 대상 | 위치 | 상태 |
| --- | --- | --- | --- | --- |
| axis 0건 Layer 삭제 → softDelete 성공 | 해피 (단위) | Aggregate | `LayerTest` | ☐ |
| axis 1건 이상 → `LAYER_HAS_ACTIVE_AXES` 예외 | 예외 (단위) | 상동 | 상동 | ☐ |
| 이미 softDeleted Layer 재삭제 → `LAYER_ALREADY_DELETED` | 예외 (단위) | 상동 | 상동 | ☐ |
| softDeleted Layer 조회 시 `@SQLRestriction` 자동 필터 | 엣지 (slice) | Repository | `LayerRepositorySliceTest` | ☐ |

#### Story 2-5: Layer 상수 · Controller

| 케이스 | 유형 | 대상 | 위치 | 상태 |
| --- | --- | --- | --- | --- |
| Layer 개수 상한 초과 시 `isLayerCountExceedsRecommended: true` | 해피 (단위) | Aggregate | `LearningFacadeTest` | ☐ |
| displayOrder 자동 1-based 부여 (3건 순차 추가) | 해피 (단위) | 상동 | 상동 | ☐ |
| `reorderLayers([3,1,2])` → displayOrder 재부여 | 해피 (단위) | 상동 | 상동 | ☐ |
| `reorderLayers([99])` (존재하지 않는 id) → `LAYER_REORDER_ID_MISMATCH` | 예외 (단위) | 상동 | 상동 | ☐ |
| Controller: `POST /layers` → 201 | 해피 (controller slice) | Controller | `LayerControllerSliceTest` | ☐ |
| Controller: `PATCH /layers/{id}` → 200 | 해피 (controller slice) | 상동 | 상동 | ☐ |
| Controller: `DELETE /layers/{id}` (axis 존재) → 409 | 예외 (controller slice) | 상동 | 상동 | ☐ |
| Controller: `PUT /layers/reorder` → 200 | 해피 (controller slice) | 상동 | 상동 | ☐ |
| `LAYER_NOT_FOUND` → 404 응답 | 예외 (controller slice) | GlobalExceptionHandler | 상동 | ☐ |

### AS Epic 1 — Port 스켈레톤

| 케이스 | 유형 | 대상 | 위치 | 상태 |
| --- | --- | --- | --- | --- |
| stub `LayerSuggestionPort` 구현 컴파일 통과 | 계약 (컴파일) | Port | `sandbox/StubLayerAdapterTest` (임시) | ☐ |
| stub `AxisSuggestionPort` 구현 컴파일 통과 | 계약 (컴파일) | 상동 | 상동 | ☐ |
| stub `RoadmapSuggestionPort` 구현 컴파일 통과 | 계약 (컴파일) | 상동 | 상동 | ☐ |
| stub `SelectionsSuggestionPort` 구현 컴파일 통과 | 계약 (컴파일) | 상동 | 상동 | ☐ |
| record 시그니처 mismatch 없음 (필드 명·타입) | 계약 (컴파일) | DTO | 상동 | ☐ |

---

## 3구분 매트릭스 (해피/엣지/예외) — Epic 별 요약

| Epic | 해피 케이스 수 | 엣지 케이스 수 | 예외 케이스 수 | 합계 |
| --- | --- | --- | --- | --- |
| LT Epic 1 (concepts) | ___ | ___ | ___ | ___ |
| LT Epic 2 (Layer) | ___ | ___ | ___ | ___ |
| AS Epic 1 (Port) | ___ | (해당 없음, 계약만) | ___ | ___ |
| **합계 (신규)** | **___** | **___** | **___** | **___** |

> 각 Epic 별 3구분 최소 1건 이상이어야 통과. 0 인 구분이 있으면 미달.

---

## JaCoCo 커버리지 목표 (신규 도메인)

> 신규 도메인 클래스만 대상. 기존 코드 커버리지는 별도 트래킹.

| 클래스 | 라인 커버리지 (실측) | 브랜치 커버리지 (실측) | 목표 | 상태 |
| --- | --- | --- | --- | --- |
| `LearningFacade` (concepts 변경) | ___% | ___% | 80% / 70% | ☐ |
| `LearningFacadeConcept` | ___% | ___% | 80% / 70% | ☐ |
| `Layer` | ___% | ___% | 80% / 70% | ☐ |
| `LearningAxis` (재배선 변경분) | ___% | ___% | 80% / 70% | ☐ |
| `LayerCommandService` | ___% | ___% | 80% / 70% | ☐ |
| `LayerQueryService` | ___% | ___% | 80% / 70% | ☐ |

**측정 명령**: `./gradlew test jacocoTestReport` → `build/reports/jacoco/test/html/index.html`

---

## 테스트 실행 결과 (D7 종료 시)

```bash
./gradlew clean test
```

| 항목 | 실측 |
| --- | --- |
| 테스트 총 개수 | ___ |
| Passed | ___ |
| Failed | ___ |
| Skipped | ___ |
| 실행 시간 | ___초 |
| 신규 테스트 파일 수 | ___ |
| 삭제된 테스트 (Story 이관·폐기) | ___ |

**실패 테스트 상세** (있는 경우):

| 파일 | 메서드 | 실패 사유 | 조치 |
| --- | --- | --- | --- |
| | | | |

---

## 놓친 커버리지 (자기 검증)

> 이번 버전에서 커버 못 한 케이스. 다음 버전 후보.

| 대상 | 놓친 케이스 | 후속 조치 |
| --- | --- | --- |
| | | |

---

## 테스트 부재 리스크

> 커버리지 부족으로 회귀 위험이 큰 영역.

| 영역 | 리스크 | 다음 버전 대응 |
| --- | --- | --- |
| Layer softDelete → 하위 axis 자동 이관 (v1.5 후보) | 정책 미확정, 테스트 부재 | 열린 질문 확정 후 |
| Layer 간 axis 이동 (v1 out of scope) | 이동 UX 미정 | Product SDD 결정 후 |

---

## 참고

- 테스트 컨벤션: `.claude/rules/conventions.md` §4
- 테스트 명명: `{대상행위}_{상황}_{기대결과}` (한글 가능)
- 3구분: 해피 / 엣지 (경계값·trim·null·멱등) / 예외 (검증 실패·id 오류·null 인자)
- Repository Slice: `@DataJpaTest`
- Controller Slice: `@WebMvcTest`
- 통합: `@SpringBootTest`
- 픽스처 위치: BC 별 test 패키지 (프로덕션 코드에 두지 않음)

*작성일: 2026-07-01 | 갱신 주기: Story 별 머지 시점 | 완료 판정: D7 저녁*
