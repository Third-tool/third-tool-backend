# Rule: 테스트 컨벤션

> private-docs/test/* 의 테스트 매트릭스 작성·수행 규칙. 테스트 코드 작성 시 적용한다.
> CLAUDE.md "테스트 전략" 섹션을 보강한다.

---

## 1. 프레임워크

- **JUnit 5** + **AssertJ**.
- Hamcrest, JUnit 4, Mockito-only assertion 사용 금지. Mock은 협력자에만 사용.

---

## 2. 계층별 전략 (Classist 기본)

| 계층 | 전략 | 근거 |
| --- | --- | --- |
| VO | **Classist** (실제 객체) | 검증·정규화 로직이 검증 대상. Mock하면 로직 자체가 빠진다 |
| Entity | **Classist** | 팩토리 메서드·상태 전이 검증은 실제 생성으로만 확인 가능 |
| Aggregate Root | **Classist** | 컬렉션 관리·순서 재부여·소유권 확인이 핵심. 실제 객체로만 검증 가능 |
| Domain Service | **Classist** (협력 도메인 객체) + Repository는 **Mock** | 도메인 규칙 응집 검증 + 외부 의존만 격리 |
| Application Service | Repository / 외부 어댑터 **Mock** + 도메인은 실제 | 트랜잭션·조율 로직 검증, 도메인 규칙 재검증은 도메인 단위 테스트가 담당 |
| Repository (Slice) | `@DataJpaTest` + 실제 H2 | 쿼리·매핑 검증. Mock하면 의미 없음 |
| Controller (Slice) | `@WebMvcTest` + Service Mock | 요청·응답 직렬화·HTTP 매핑 검증 |
| 통합 (전 계층) | `@SpringBootTest` + 실제 H2 | 핵심 시나리오·트랜잭션 경계만 |

**원칙**: 도메인 객체는 절대 Mock하지 않는다. Mock은 외부 시스템 경계(Repository 인터페이스, 외부 API 클라이언트)에만 사용한다.

---

## 3. 테스트 케이스 분류

테스트 매트릭스는 **해피 / 엣지 / 예외** 세 구분으로 나눈다.

| 구분 | 의미 | 예시 |
| --- | --- | --- |
| 해피 | 정상 입력 → 정상 결과 | `create_valid`, `addAxis_첫번째_displayOrder_1` |
| 엣지 | 경계값·동등 입력·trim·null 정규화·멱등 | `addTopic_description_빈문자열_null로정규화`, `updateConcept_동일값_unchanged반환`, `archive_이미ARCHIVE_무시` |
| 예외 | 검증 실패·잘못된 id·null 인자 | `create_concept_blank_예외`, `reorderAxes_id집합_불일치_예외` |

각 도메인 메서드마다 **세 구분 모두 한 케이스 이상** 작성하는 것을 기본 목표로 한다 (의미 없으면 생략).

---

## 4. 테스트 메서드 명명 규칙

`{대상행위}_{상황}_{기대결과}` 형식. 한글 단어 사용 가능.

```
create_valid
create_concept_blank_예외
updateConcept_동일값_unchanged반환
addAxis_첫번째_displayOrder_1
addAxis_두번째_displayOrder_마지막플러스1
reorderAxes_id집합_불일치_예외
removeTopic_존재하지않는topicId_예외
```

**원칙**:
- 행위 + 입력 조건 + 기대 결과가 메서드명만 보고 식별 가능해야 한다.
- `_예외`, `_무시`, `_정상` 같은 한글 접미어 허용.
- 영어 변수명(`displayOrder`, `null`, `blank`)은 한글 분리어 사이에 그대로 사용.

---

## 5. 명시 검증 항목

다음은 **반드시 명시적 테스트 케이스로 작성**한다 (도메인 모델 문서가 명시한 경계 규칙).

### 5.1 displayOrder
- 첫 항목 추가 시 `displayOrder == 1` (1-based 기준값)
- 두 번째 추가 시 `displayOrder == 마지막 + 1`
- reorder 후 `1, 2, 3, ...` 연속 부여 검증
- reorder 시 id 집합 불일치 → 예외
- 빈 컬렉션 + 빈 리스트 reorder → 정상

### 5.2 멱등성
- 이미 ARCHIVE인데 `archive()` 호출 → 상태 불변
- 이미 ON_FIELD인데 `returnToField()` 호출 → 상태 불변
- ARCHIVE 카드에 `recordView()` → 무시 (예외 X)
- 동일 컨셉으로 `updateConcept()` → `unchanged` 반환, `updatedAt` 불변

### 5.3 입력 정규화
- trim 후 동일 값이면 `unchanged` 결과 (예: `"  백엔드  "` vs `"백엔드"`)
- 선택적 String 필드의 `""` 입력 → 저장 시 `null` 정규화
- `null` 또는 blank → 예외

### 5.4 권장 한도 경계값
- 권장 한도 정확히일 때 `false`, 한도 + 1일 때 `true` (예: 5개 → false, 6개 → true)
- 0개 입력 시 `false`

### 5.5 이중 방어 — 도메인 검증
- 동일 이름 추가 시도 → 도메인 예외 (DB UNIQUE는 통합 테스트에서 별도 검증)

### 5.6 응답 결과 VO
- `ConceptChangeRecord.isChanged()` / `isDrifted()` 양쪽 명시 (v1에서 동등성, v2에서 분기 예정 → 분기점 보존)
- `OnFieldBudget.resolveReason()` 우선순위 (`MAX_VIEW` > `MAX_DURATION`)

---

## 6. 도메인 검증 vs 테이블 CHECK

테이블 CHECK 제약(`display_order >= 0`)은 **안전망**이다. 도메인 의미는 1-based.

- **단위 테스트는 도메인 의미를 검증**한다 → `displayOrder = 0` 입력 시 도메인 예외.
- 테이블 CHECK 자체의 동작은 Repository Slice 또는 통합 테스트에서 검증.
- 두 검증은 분리해서 작성한다.

---

## 7. 테스트 데이터 구성

- **테스트 픽스처는 정적 팩토리 호출로 만든다** (예: `Card.create(...)`). new 사용 금지 (도메인과 동일한 생성 경로).
- 픽스처 빌더(`CardFixture`, `LearningFacadeFixture`)를 BC별 테스트 패키지에 두고 재사용. **프로덕션 코드에 두지 않는다**.
- 시각 의존 테스트(`recordView()` 후 시간 비교 등)는 **`Clock` 주입** 또는 시각 비교 허용 범위(`isAfter`, `isCloseTo`)로 처리.

---

## 8. 단위 / Slice / 통합 분리

(CLAUDE.md "테스트 전략 매트릭스" 보강)

| 종류 | 위치 | 무엇을 검증하나 |
| --- | --- | --- |
| 단위 (도메인) | `{BC}/domain/` 패키지의 동일 위치 테스트 | 도메인 객체 행위·검증·상태 전이 |
| 단위 (Application) | `{BC}/application/` 패키지의 테스트 | Application Service 조율 로직 (Repository Mock) |
| Slice (Repository) | `@DataJpaTest` | 쿼리·매핑·DB 제약 (UNIQUE, CHECK) |
| Slice (Controller) | `@WebMvcTest` | HTTP 매핑·DTO 직렬화·에러 응답 형식 |
| 통합 | `@SpringBootTest` | 다중 BC 협력 시나리오, 트랜잭션 롤백, Flyway 마이그레이션 결과 |

**원칙**: 같은 규칙을 **여러 계층에서 중복 검증하지 않는다**. 도메인 검증은 도메인 단위 테스트가, DB UNIQUE 위반은 Repository Slice가 한 번씩 검증한다.

---

## 9. 검증 가정 정합성

테스트는 도메인 모델 문서의 결정과 정합해야 한다. 충돌이 보이면 코드보다 **문서가 더 최신인지** 먼저 확인하고, 둘 다 진실이 명확하지 않으면 사용자에게 묻는다.

대표적 정합 항목 (예시):
- `displayOrder` = 1-based (도메인) / `>= 0` (DB CHECK)
- `coverage_status` 초기값 = `NO_MATERIAL`
- `proficiencyLevel` 초기값 = `UNRATED`
- 동일 축 내 주제 `name` 중복 = **도메인은 허용**, FE에서만 안내
- `description` 빈 문자열 = `null`로 정규화

---

## 10. 테스트 추가 트리거

다음 변경이 발생하면 **반드시 테스트를 함께 추가/수정**한다.

- 도메인 객체에 새 행위 추가 → 단위 테스트 (해피 / 엣지 / 예외 세트)
- 새 검증 규칙 추가 → 예외 케이스 테스트
- 새 ErrorCode 등록 → Controller Slice 또는 통합 테스트로 응답 형식 검증
- 새 Flyway 마이그레이션 → 통합 테스트로 마이그레이션 전후 데이터 일치성 검증 (데이터 이관 동반 시 필수)
- 새 Repository 쿼리 메서드 → `@DataJpaTest` Slice 테스트
