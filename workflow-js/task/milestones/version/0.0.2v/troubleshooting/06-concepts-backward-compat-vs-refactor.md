# Deep-dive · Part 06 — concepts[] · facade.axes · legacy compat vs 전면 refactor

> **본 편의 성격**: 0.0.2v/Tier 1에서 반복적으로 마주친 결정 — **"완전히 새 형상으로 이관 vs 레거시 병존"** 중 어느 쪽을 선택할지. 세 가지 실측 사례 (`learning_facade.concept` 컬럼 유지 · `LearningFacade.axes` 유지 · `LearningAxis.layer` nullable) · 각각의 트레이드오프 · 미래 정리 로드맵.

---

## 0. 세 개의 레거시 잔재 (실측)

### 잔재 1 — `learning_facade.concept` NOT NULL 컬럼

- V16 이전: `concept: String NOT NULL`이 유일한 컨셉 저장 경로
- V16 이후: `learning_facade_concept` 자식 테이블 신설 · 진짜 진실 소스
- 그러나 `learning_facade.concept` NOT NULL 컬럼은 **유지**
- 첫 concept 값과 자동 동기화 (`LearningFacade.create()` · `updateConcepts()`가 legacy 필드 세팅)

### 잔재 2 — `LearningFacade.axes` 컬렉션

- 원래: `LearningFacade.axes = List<LearningAxis>` (@OneToMany mappedBy="facade")
- SDD의 "Layer 도입" 결정 원안 AC: `"facade.axes 직접 접근 컴파일 오류"` — Layer 경유 강제
- 실제 착지: **유지**. `facade.getAxes()`는 여전히 활성 axis 전체 반환
- `LearningLayer.axes`도 병존 (@OneToMany mappedBy="layer")

### 잔재 3 — `LearningAxis.layer` nullable

- V19가 3-phase 마이그레이션으로 `learning_axis.learning_layer_id BIGINT NOT NULL` 승격
- 그러나 Java 도메인 `LearningAxis.layer` 필드는 nullable (`@JoinColumn(nullable = true)`)
- 이유: 3-phase 전이 기간 동안 안전 · legacy 팩토리 `LearningAxis.create(facade, name, order)`가 layer 없이 생성 가능

---

## 1. 왜 유지 결정을 했나 (각 잔재별 회고)

### 잔재 1 — concept 컬럼 유지

**즉시 이유**: 컬럼 DROP은 한 마이그레이션에서 못함. NOT NULL 해제 → NULL 백필 → DROP 3-phase 필요. 이건 별도 릴리스.

**깊은 이유**: 
- 프로덕션 DB에 존재하는 데이터. 마이그레이션 실패 시 손실 위험.
- 컬럼이 있음에도 "concepts 자식 테이블만 진실 소스"라는 도메인 규칙을 명확히 하면 유지비용 낮음.
- 별도 릴리스에서 안전하게 3-phase로 정리.

### 잔재 2 — facade.axes 유지

**즉시 이유**: 40+ 파일 refactor 회피. 기존 테스트 · Controller · QueryService · Card BC · Deck BC · Review BC 등이 `facade.getAxes()` 다수 호출.

**깊은 이유**:
- SDD의 "컴파일 오류" AC는 이상론. 실전 refactor 비용이 압도적.
- `facade.getAxes()`는 여전히 유의미 (Layer 경유하지 않고 전체 axis 조회 요구가 있음)
- backward compat 유지가 도메인 의미를 훼손하지 않음 (facade.axes = 모든 layer의 axes 합)

### 잔재 3 — LearningAxis.layer nullable

**즉시 이유**: 3-phase 전이 안전. V19가 DB에서 NOT NULL을 강제하지만 Java 도메인이 nullable이면 legacy 팩토리가 통과.

**깊은 이유**:
- Java 도메인의 nullable과 DB의 NOT NULL이 어긋나도 실제 저장 시점에는 layer 존재 (default Uncategorized 자동 발행 정책 · `LearningFacade.create()` 자동 layer)
- Test에서 legacy 팩토리로 생성한 axis는 layer 없어도 컴파일 통과 · 테스트 fixture 재활용 이득
- 미래 nullable → NOT NULL로 승격 시 Java 필드 · JPA 매핑 갱신만 필요 (DB 스키마는 이미 NOT NULL)

---

## 2. 판단 지점 · Q1 — 잔재 정리의 우선순위

3개 잔재를 언제 정리할지 · 어떤 순서로 정리할지.

### 잔재 1 우선순위 (concept 컬럼 DROP)

- **위험**: 백필 실패 시 데이터 손실
- **이득**: legacy 필드 동기화 코드 제거 (`create()` · `updateConcepts()`에서 concept 세팅 라인)
- **필요 SP**: 3-phase 3 V (V20~V22) · 리스토어 스크립트 · 프로덕션 데이터 spot check

### 잔재 2 우선순위 (facade.axes 제거)

- **위험**: 다수 파일 refactor · 회귀 위험
- **이득**: 도메인 모델 명확화 · SDD 원안 AC 충족
- **필요 SP**: 5+ (40+ 파일 · 6+ BC 영향 · 통합 테스트 재작성)

### 잔재 3 우선순위 (LearningAxis.layer NOT NULL)

- **위험**: Legacy 팩토리 통과 테스트 실패
- **이득**: 도메인 모델 안전 · null 방어 코드 제거
- **필요 SP**: 1~2 (팩토리 통합 · 테스트 fixture 갱신 · JPA 매핑 갱신)

**⚠️ 개인 판단 요청**: 정리 우선순위를 어떻게 매기실 건가요?

- (a) 잔재 3 → 잔재 1 → 잔재 2 (쉬운 것부터 · 스코프 작은 순)
- (b) 잔재 1 → 잔재 3 → 잔재 2 (DB 정리 우선)
- (c) 잔재 2 → 나머지 (SDD AC 충족 우선 · 큰 결단부터)
- (d) 잔재 2는 무기한 이월 (실용 유지) · 잔재 1·3만 정리
- (e) 다른 순서

---

## 3. 판단 지점 · Q2 — "실용 유지"와 "원안 AC 충족"의 트레이드오프

SDD 원안 AC는 이상. 실용 유지는 현실. 이 갈림에서 늘 되풀이되는 갈등:

- **이상 우선**: "SDD가 원했던 대로 완성하라"
  - 장점: 도메인 모델 순수 · 인지 부담 낮음
  - 단점: refactor 비용 · 회귀 위험
- **실용 우선**: "지금 필요한 최소만 완성"
  - 장점: 시간 절약 · 위험 낮음
  - 단점: 레거시 누적 · 인지 부담 축적

0.0.2v/Tier 1에서 각 잔재별로 이 갈등이 반복됨. 그리고 대체로 실용 우선 결정.

**⚠️ 개인 판단 요청**: 이 트레이드오프에서의 자기 기준은?

- (α) 매번 사안별 판단 (deadline · 스코프 · 위험 개별 평가)
- (β) 이상 우선 기본 · 실용은 예외 (원안 AC를 표준으로 삼음)
- (γ) 실용 우선 기본 · 이상은 계획적 (지금은 빨리, 정리는 나중)
- (δ) "실용 유지하되 문서에 부채 명시" (부채 등록 · 이자 관찰)
- (ε) 다른 관점

---

## 4. 판단 지점 · Q3 — 부채 관찰 · "이자" 매기기

레거시 잔재가 "부채(technical debt)"라면 이자가 있다:

- 잔재 1: 매번 `create()` · `updateConcepts()`에서 legacy 필드 동기화 라인 유지. 실수 시 concept과 concepts[0] 불일치 위험.
- 잔재 2: 새 도메인 로직 작성 시 `facade.getAxes()` vs `layer.getAxes()` 결정 부담. 두 개 있으면 어떤 상황에 무엇을 쓸지 고민 매번.
- 잔재 3: 새 axis 생성 로직 작성 시 layer 없이 생성 가능한 팩토리 위험 인식 필요.

**이자 관찰**: 이 부담이 실제로 개발 속도에 영향을 미치는지 · 발생하는 버그의 원인이 되는지.

**⚠️ 개인 판단 요청**: 부채 이자를 어떻게 관찰?

- (a) 다음 마일스톤 troubleshooting.md에서 각 잔재로 인한 실측 부담 이슈 카운트
- (b) 코드 리뷰에서 "이 결정이 레거시 유지 때문인가?"를 점검 항목화
- (c) 회의에서 정기 언급 (M3, M4 등)
- (d) 관찰 안 함 · 실제 문제 발생 시만 대응 (수동 트리거)
- (e) 다른 방식

---

## 5. 이어질 §5~9 (답변 후 완성)

Q1~Q3 답변 주시면:

- §5. 잔재 1(concept 컬럼) DROP의 3-phase 상세 계획
- §6. 잔재 2(facade.axes) 제거의 단계적 refactor 시나리오 (Big Bang vs Rolling)
- §7. 부채 관리 관행 · `.claude/rules/conventions.md`에 명시할 것
- §8. 미래 시나리오 · scaling · 새 도메인 추가 시의 관행
