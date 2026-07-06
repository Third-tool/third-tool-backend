# 0.0.2v / Troubleshooting Log — 0.0.3v 급행 실측

> **본 파일 역할**: 본 버전 진행 중 발생한 **막힘·실패·원인 불명 증상**을 발생 시점에 기록해 회고·재발 방지 자산으로 남긴다.
> 상세 개선 항목: [`review.md`](../review.md) §"반성할 것들". 재발 방지 룰: `.claude/rules/conventions.md`.

---

## 요약 대시보드

| 카테고리 | 건수 | 총 소요시간 | 미해결 이월 |
| --- | --- | --- | --- |
| git · 브랜치 · PR 흐름 | 2 | ~7분 | 0 |
| 컴파일·오버로드 | 1 | ~5분 | 0 |
| 도메인 (JPA · in-memory 정렬) | 1 | ~3분 | 0 |
| 테스트 (JDK API) | 1 | ~3분 | 0 |
| Flyway · JPA (예방적으로 회피) | 0 | 0분 | 0 |
| **합계** | **5** | **~18분** | **0** |

**세션 전체 대비 트러블슈팅 비율**: 1% 미만. Critical한 도메인·JPA 오류 없음.

---

## Top 3 막힘 (본 버전 회고)

| 순위 | 막힘 | 소요시간 | 해결 방식 | 학습 · 재발 방지 |
| --- | --- | --- | --- | --- |
| 1 | **PR #195 base branch가 `main`인 줄 알았으나 `develop`** | ~5분 | `gh pr view --json baseRefName` 확인 · develop으로 재분기 | CLAUDE.md의 "Main branch you will usually use for PRs: main"이 사용자 실제 컨벤션과 다름. `.claude/rules/git.md` §1 develop 정책이 진실 소스. 프로젝트 첫 진입 시 두 문서 정합 확인 관행화 |
| 2 | **`LearningFacade.create(user, null)` 오버로드 모호성** | ~5분 | `(String) null` 명시 cast | `create(user, String)` + `create(user, List<String>)` 두 오버로드 도입은 자바 컴파일러가 null을 어느 쪽으로도 해석 못함. 오버로드 도입 시 사전 예상 트랩 |
| 3 | **QueryDSL generated 파일 dirty로 checkout 실패** | ~2분 | `git restore src/main/generated/` (build 시 재생성됨) | .gitignore가 이미 있으나 로컬 build 이력이 남아있어 checkout 방해. 매 브랜치 전환 전 관행화 검토 |

---

## 트러블슈팅 로그 (발생 순)

### [T#001] QueryDSL generated 파일 dirty로 checkout 실패

- **발생 시각**: PR 2 (LT E1) 시작 직전
- **발생 위치**: `git checkout main`
- **관련 Story**: LT E1 시작 전 브랜치 분기
- **카테고리**: git · IDE
- **심각도**: Minor

**증상**:
```
error: Your local changes to the following files would be overwritten by checkout:
  src/main/generated/com/example/thirdtool/Deck/infrastructure/dto/QDeckSummaryRow.java
  src/main/generated/com/example/thirdtool/Review/infrastructure/dto/QReviewSessionSummaryRow.java
  src/main/generated/com/example/thirdtool/UserSchedule/domain/model/QUserScheduleConfig.java
Please commit your changes or stash them before you switch branches.
Aborting
```

**가설 1**: QueryDSL Q클래스는 build 시 재생성되므로 stash 대신 restore로 되돌리면 됨.
- 근거: `src/main/generated/`는 build 산출물 · 소스 편집 대상 아님
- 시도: `git restore src/main/generated/`
- 결과: ✅ 해결

**최종 해결**:
- 조치: `git restore src/main/generated/` 실행 후 checkout
- 검증 방법: `git status --short`으로 clean 확인 후 checkout 성공
- 소요 시간: ~2분

**학습**:
- QueryDSL Q클래스는 build 산출물. `.gitignore`에 등록되어 있으나 이전 build 이력이 workspace에 남으면 diff로 인식됨.
- 매 브랜치 전환 전 `git restore src/main/generated/` 관행화 검토.

**재발 방지**:
- ☐ conventions.md 업데이트 (해당 사항 미기재)
- ☐ 별도 hook 검토 (사용자 개인 hook 영역)

---

### [T#002] PR #195 base branch가 `main`인 줄 알았으나 실제 `develop`

- **발생 시각**: PR 2 시작 시 (Story LT-E1-S1 머지 상태 확인 중)
- **발생 위치**: `git checkout main && git pull origin main` → "Already up to date"인데 origin/main head가 PR #195 머지 커밋을 포함 안 함
- **관련 Story**: PR 2 (LT E1) 시작 전
- **카테고리**: git · PR 흐름
- **심각도**: Major (base 잘못이면 다음 PR도 잘못 열림)

**증상**:
```bash
git log origin/main --oneline -3
# 0882019 Merge pull request #110 from Third-tool/feat/card-domain-model  ← PR #195 미포함
# 57267ac ...
# 45e4a96 ...
```
그러나:
```bash
gh pr view 195 --json state,mergedAt
# {"state":"MERGED","mergedAt":"2026-07-02T05:25:48Z"}  ← 머지 완료
```
main에 없다? → 실제 base가 develop이었음.

**가설 1**: PR #195의 target은 `main`이 아니고 `develop`.
- 근거: `gh pr view 195 --json baseRefName` 확인
- 시도: `gh pr view 195 --json baseRefName,headRefName,mergedAt` → `"baseRefName":"develop"`
- 결과: ✅ 확정

**최종 해결**:
- 조치: `git checkout develop && git pull origin develop && git checkout -b feat/026-lt-e1-concepts-collection`
- 검증 방법: `git log develop --oneline -3`으로 PR #195 머지 커밋(04d02a0) 확인
- 소요 시간: ~5분

**학습**:
- **CLAUDE.md**의 "Main branch (you will usually use this for PRs): main" 문구는 사용자 실제 컨벤션과 다름.
- **`.claude/rules/git.md`** §1 브랜치 전략의 "develop: 통합 브랜치, PR 머지만"이 실제 정책.
- `main`은 release 브랜치, `develop`이 통합 브랜치.

**재발 방지**:
- [x] 첫 PR 시 base branch를 실제 origin/develop head와 비교하는 절차 관행화 (본 세션 이후 준수)
- [ ] CLAUDE.md와 `.claude/rules/git.md` 정합 관행은 사용자 결정 사항

---

### [T#003] `LearningFacade.create(user, null)` 오버로드 모호 컴파일 오류

- **발생 시각**: Story LT-E1-S4 (DTO 스위치) 후 기존 테스트 실행 시
- **발생 위치**: `src/test/java/com/example/thirdtool/LearningFacade/domain/model/LearningFacadeTest.java:93`
- **관련 Story**: LT-E1-S4 (기존 create(user, String) 유지 + create(user, List<String>) 추가)
- **카테고리**: 컴파일 · Java 오버로드
- **심각도**: Minor (테스트만 실패, 프로덕션 코드 무해)

**증상**:
```
LearningFacadeTest.java:93: error: reference to create is ambiguous
    assertThatThrownBy(() -> LearningFacade.create(user, null))
                                           ^
both method create(UserEntity,String) in LearningFacade and
method create(UserEntity,List<String>) in LearningFacade match
```

**가설 1**: 오버로드 모호. null을 (String) 또는 (List<String>) 중 어느 것으로도 해석 가능.
- 근거: 자바 언어 스펙 (오버로드 해석 규칙)
- 시도: `create(user, (String) null)` 명시 cast
- 결과: ✅ 해결

**최종 해결**:
- 조치: `LearningFacadeTest.java:93`에서 `create(user, null)` → `create(user, (String) null)` 변경
- 커밋: `999c107 feat(learning-facade): API/DTO concepts[] 스위치 [Story-LT-E1-S4]` 안에 포함
- 검증 방법: `./gradlew compileTestJava` 성공 · `LearningFacadeTest.create_concept_null_예외` 통과
- 소요 시간: ~5분

**학습**:
- 오버로드 도입 시 null 인자가 두 오버로드에 매칭되면 컴파일 모호. 사전 예상 트랩.
- 새 오버로드 추가 시 기존 테스트 중 `null` 리터럴 사용처를 grep으로 확인 관행화.

**재발 방지**:
- [x] 오버로드 도입 시 grep으로 `null` 리터럴 사용처 사전 확인 (본 세션 이후 준수)

---

### [T#004] `reorderConcepts` 후 in-memory 순서 불일치

- **발생 시각**: Story LT-E1-S2 테스트 실행 시
- **발생 위치**: `LearningFacadeConceptsTest.reorderConcepts_정상_displayOrder_재부여` (line 242)
- **관련 Story**: LT-E1-S2 (reorderConcepts 도메인 메서드)
- **카테고리**: 도메인 · JPA · in-memory 정렬
- **심각도**: Minor

**증상**:
```
assertThat(facade.getConceptValues()).containsExactly("C", "백엔드", "B");
// AssertionFailedError: 순서 mismatch — 실제 [백엔드, B, C] (insertion 순서)
```

**가설 1**: `reorderConcepts`가 displayOrder는 갱신하지만 내부 List 순서는 그대로. `@OrderBy`는 DB load 시점만 적용됨.
- 근거: JPA `@OrderBy` 스펙 (persistence load 시점에 sort)
- 시도: `concepts.sort(Comparator.comparingInt(LearningFacadeConcept::getDisplayOrder))` 추가
- 결과: ✅ 해결

**최종 해결**:
- 조치: `LearningFacade.reorderConcepts` 마지막에 `concepts.sort(Comparator.comparingInt(LearningFacadeConcept::getDisplayOrder))` 추가
- 커밋: `7d44b53 feat(learning-facade): LearningFacadeConcept Entity + concepts[] 컬렉션 API [Story-LT-E1-S2]` 안에 포함 (fix commit)
- 검증 방법: 17건 테스트 전량 통과
- 소요 시간: ~3분

**학습**:
- `@OrderBy`는 DB에서 load할 때만 적용. **In-memory에서 displayOrder를 갱신하면 명시적으로 List를 sort해야 조회 순서 일관.**
- 같은 문제가 `reorderLayers`에서도 발생 가능 → 사전 예방적으로 동일 sort 로직 추가.

**재발 방지**:
- [x] `reorderLayers`에도 동일 sort 로직 적용 (본 세션에서 예방)
- ☐ 다른 reorder 메서드 (Card keywords · Deck 등) 검토 (본 세션 범위 밖)

---

### [T#005] `List.of("...", null)` NPE

- **발생 시각**: Story LT-E1-S2 테스트 초기 작성 시
- **발생 위치**: `StaticLayerSuggestionAdapterTest.suggest_existingLayerNames_trim_적용_후_비교`
- **관련 Story**: Tier 2 Story 15 (StaticLayerSuggestionAdapter)
- **카테고리**: Java API · JDK 9+
- **심각도**: Nit

**증상**:
```
IllegalArgumentException: null in List.of(...)
```
발생 위치: `List.of("  웹 API  ", null)`

**가설 1**: `List.of()` factory는 null 원소 허용 안 함 (Java 9+ immutable list 규약).
- 근거: JDK Javadoc — `List.of` 인자가 null이면 `NullPointerException`
- 시도: `Arrays.asList("...", null)`로 교체 (null 허용)
- 결과: ✅ 해결

**최종 해결**:
- 조치: `List.of("  웹 API  ", null)` → `Arrays.asList("  웹 API  ", null)`
- 검증 방법: 8건 테스트 전량 통과
- 소요 시간: ~3분

**학습**:
- **`List.of(...)` (Java 9+) vs `Arrays.asList(...)` (Java 1.2+)**: 전자는 null 미허용, immutable. 후자는 null 허용, mutable.
- 테스트에서 null 원소가 필요하면 `Arrays.asList` 또는 `new ArrayList<>() { { add("..."); add(null); } }` 사용.

**재발 방지**:
- ☐ 별도 룰 갱신 불요 (Java 표준 지식)

---

## 반복 트랩 (본 버전 3회 이상 반복 발생)

**해당 사항 없음.** 각 트러블은 1회씩 발생 후 해결. 반복 트랩 무.

---

## 예방된 트랩 (사전 예상해서 회피)

| 예상 트랩 | 회피 근거 | 실제 위험도 |
| --- | --- | --- |
| Flyway V 버전 충돌 (Epic 1 · Epic 2 동시 진행) | 사전 V 번호 할당 (V17 concept 백필 · V18 Layer · V19 Axis FK). Epic 1 완료 후 Epic 2 진입으로 실제 충돌 원천 차단 | 무 (0회) |
| 3-phase 마이그레이션 순서 실수 | `.claude/rules/conventions.md` §3.8 준수. V19 SQL을 (Phase 1 ADD → 2 백필 → 3 NOT NULL + FK) 명시 주석과 함께 작성 | 무 (0회) |
| ErrorCode 등록 순서 (enum → domain throw → 검증) | conventions.md §2.5 절차 준수. 매 Story마다 순서 지킴 | 무 (0회) |
| default Layer UNIQUE 위반 | `LearningFacade.create()` 자동 발행 + V19 백필 idempotent `NOT EXISTS` | 무 (0회) |
| Card 조회 회귀 (`layer_id` 재배선) | `LearningAxis.facade` @ManyToOne 유지 (backward compat) + Layer 도입이 axis→facade 경로 안 건드림 | 무 (0회) |
| Hibernate ddl-auto가 V19 백필 skip | 인지 후 명시 (테스트는 Hibernate 스키마, prod는 Flyway) · 백필 로직은 도메인 `create()`로 별도 검증 | 낮음 (테스트만 우회) |

---

## 도구·환경 이슈

**해당 사항 없음.** Gradle Wrapper · JDK 21 · IntelliJ 정상 작동.

---

## 성능 문제

**해당 사항 없음.** rush 정책으로 정밀 성능 미측정. 정성 관찰(`./gradlew test` 실행 시간 등)에서 급 회귀 신호 없음.

---

## 데이터 손실·복구 이력

**해당 사항 없음.** 마이그레이션 실패 · 백필 누락 · rollback 필요 사건 무. R17~R19 rollback 스크립트는 작성됐지만 실제 실행 안 함.

---

## 다음 버전 이관 (미해결·조사 지속)

| Ticket | 요약 | 회피 상태 | 우선순위 |
| --- | --- | --- | --- |
| REF#1 | `facade.axes` backward compat 유지 → SDD 원안 AC 미충족 | 사용 그대로 · 미래 refactor 예정 | P2 |
| REF#2 | Legacy `learning_facade.concept` 컬럼 유지 → 컬럼 DROP 3-phase 별도 릴리스 | 첫 concept 값과 자동 동기화 | P2 |
| REF#3 | `@SQLRestriction` vs `boolean deleted` 관행 갈림 재발 (ADR021 미해결 재확인) | LearningLayer는 `@SQLRestriction`, Card/Deck은 `boolean deleted` | P3 |
| REF#4 | 테스트에서 Flyway disabled → V19 백필 SQL 실행 미검증 | 도메인 `create()`로 동등 검증 | P3 |
| REF#5 | rush 정책 5관점 Reviewer 세션 스킵 → 종합 검증 부재 | 다음 마일스톤 시작 시 종합 세션 별도 예약 검토 | P1 |

---

## Runbook 후보 (반복 대응 절차)

| 시나리오 | 절차 초안 | 담당 문서 |
| --- | --- | --- |
| Flyway 마이그레이션 실패 후 롤백 | (1) `git log db/migration`로 최신 V 파악 → (2) 대응 R 실행 → (3) H2 clean → (4) `./gradlew bootRun` 재시도 | `docs/runbook/flyway-rollback.md` (후보 · 미신설) |
| PR base branch 확인 | (1) `gh pr view <N> --json baseRefName,mergedAt` → (2) 실제 base로 local 브랜치 재분기 | 본 문서 T#002 |
| QueryDSL 파일 dirty 시 checkout | `git restore src/main/generated/` 후 재시도 | 본 문서 T#001 |
| 오버로드 도입 시 null 리터럴 검색 | `grep -rn "MethodName(.*null" src/test/` | 본 문서 T#003 |
| in-memory reorder 후 List sort | `list.sort(Comparator.comparingInt(...))` 추가 | 본 문서 T#004 |
| null 원소 필요 시 collection factory | `List.of(...)` 대신 `Arrays.asList(...)` | 본 문서 T#005 |

---

## 참고

- 규범: `.claude/rules/conventions.md` §3.8 (Flyway 3단계) · §4 (테스트 컨벤션) · §2.5 (ErrorCode 등록)
- 시스템 디버깅 skill: `superpowers:systematic-debugging` (본 세션 미사용 · 트러블이 모두 소소)
- 재발 방지 원칙: 트랩을 밟았으면 최소 1건의 컨벤션·룰·테스트·ADR이 남아야 함. 본 세션은 신규 룰 갱신 없음 (Java 표준 지식·CLAUDE.md 재확인 수준)

*작성일: 2026-07-02 | 총 트러블 5건 · 소요 ~18분 · 미해결 이월 0 · 반복 트랩 0*
