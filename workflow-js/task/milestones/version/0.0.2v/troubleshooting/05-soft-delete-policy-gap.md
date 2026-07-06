# Deep-dive · Part 05 — Soft Delete 관행 갈림 (`@SQLRestriction` vs `boolean deleted`) 재발

> **본 편의 성격**: 프로젝트 내 Soft Delete 구현이 두 관행으로 갈라져 있고, 0.0.2v에서 `LearningLayer`를 신설하면서 **한 번 더 갈림을 확대**했다. 이 결정을 어떻게 해석해야 하나 · 통일이 가능하다면 어느 방향으로 가야 하나.

---

## 0. 갈라진 현재 상태 (실측)

### 관행 A — `boolean deleted` + Repository 명시 필터

**대상 도메인**: `Card`, `Deck`, `LearningFacade`, `LearningMaterial`, `User`

```java
@Entity
public class Card {
    @Column(name = "deleted")
    private boolean deleted;

    public void softDelete() { this.deleted = true; }
    public boolean isDeleted() { return deleted; }
}
```

**Repository 조회**:
```java
List<Card> findByUserIdAndDeletedFalse(Long userId);
```

**특징**:
- Java 도메인이 boolean 필드로 상태 소유
- Repository 쿼리마다 `deleted = false` 명시 필터
- Spring Data JPA 메서드 명명 규칙과 자연 결합
- 삭제 시각(언제 삭제됐는지) 정보 없음

### 관행 B — `deleted_at DATETIME(6) NULL` + `@SQLRestriction` + `@SQLDelete`

**대상 도메인**: `LearningAxis` (ADR021, 2026-07-01), `LearningLayer` (0.0.2v Story LT-E2-S1)

```java
@Entity
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE learning_layer SET deleted_at = CURRENT_TIMESTAMP(6) WHERE learning_layer_id = ?")
public class LearningLayer {
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isDeleted() { return deletedAt != null; }
    public void softDelete() { this.deletedAt = LocalDateTime.now(); }
}
```

**Repository 조회**:
```java
List<LearningLayer> findByFacadeId(Long facadeId);
// @SQLRestriction이 WHERE deleted_at IS NULL 자동 추가
```

**특징**:
- Hibernate 어노테이션이 조회·삭제 SQL을 자동 변환
- Repository 쿼리에 필터 명시 불요 (자동)
- 삭제 시각 저장 (감사 · 복원 UI에 활용 가능)
- 벤더 종속 (Hibernate 관행)

---

## 1. 갈라진 이력 (recap)

### 초기 (~M1)

- Card · Deck · LearningFacade 등 **관행 A** 채택. `.claude/rules/conventions.md` §3.4가 두 관행 모두 예시로 표기.

### ADR021 (2026-07-01, "fix-axis-deck-full-integration")

- `LearningAxis`를 사용자 자산성 도메인으로 승격 · Soft Delete 필요.
- 결정: `@SQLRestriction` + `@SQLDelete` (관행 B).
- ADR021이 이 결정을 명시하며 함께 "관행 통일" 후속 리팩토링을 재검토 시점으로 남김:
  > "@SQLRestriction vs boolean deleted 관행 통일 — 프로젝트 전체 Soft Delete 구현을 하나의 관행으로 통일하는 리팩토링."

### 0.0.2v Story LT-E2-S1 (2026-07-02, `LearningLayer` 신설)

- 새 도메인 `LearningLayer`도 Soft Delete 필요.
- 결정: 관행 B 계승 (`LearningAxis`와 동일 관행).
- **결과**: 관행 A(5개 도메인), 관행 B(2개 도메인). 갈림이 재발됐고 폭이 커짐.

---

## 2. 왜 갈라졌나 (정직한 회고)

### 즉시적 원인

- ADR021이 관행 B 채택 시 통일 리팩토링을 "재검토 시점"으로 미룸 (당시 스코프 초과)
- 0.0.2v Story LT-E2-S1에서 관행 B를 그대로 계승 (LearningAxis와 대칭)
- 통일 리팩토링 착수 없이 새 도메인 추가 → 갈림 확대

### 근본 원인

- **통일 관행이 확정되지 않은 상태에서 신규 도메인 추가는 갈림 확대**를 만든다는 원칙이 명시되지 않음
- 각 신규 도메인의 저자 판단으로 선택 · 시점에 따라 다를 수 있음

---

## 3. 판단 지점 · Q1 — 통일 방향

두 관행을 통일하려면 방향 결정 필요. 세 후보:

### Option A — 전면 관행 A(`boolean deleted`)로 회귀

- `LearningAxis` · `LearningLayer`를 `boolean deleted` + `deleted_at LocalDateTime` 병행으로 재구현
- Repository 필터 명시 · `@SQLRestriction` 제거
- **장점**: 기존 관행이 5개 도메인 · 다수결 · Spring Data JPA 명명 규칙과 자연
- **단점**: 최신 도메인 리팩토링 · 관행 B의 이점(자동 필터) 상실 · 이미 채택 후 되돌리는 부담

### Option B — 전면 관행 B(`@SQLRestriction`)로 이관

- `Card` · `Deck` · `LearningFacade` · `LearningMaterial` · `User`를 `@SQLRestriction` + `@SQLDelete`로 이관
- Repository 필터 코드 제거 · 벤더 종속 감수
- **장점**: 현대 Hibernate 관행 · 필터 코드 감소 · 삭제 시각 저장 표준화
- **단점**: 5개 도메인 리팩토링 · 기존 Repository 쿼리·테스트 다수 영향

### Option C — 병존 유지 · 신규는 관행 B로

- 기존 관행 A는 그대로 · 새 도메인은 관행 B
- **장점**: 리팩토링 부담 없음
- **단점**: 갈림 유지 · 개발자 인지 부담 · 신규 도메인마다 어느 관행 결정 필요

**📌 실제 판단 (사용자 답변)**: **(a) A로 회귀 (boolean deleted)** — 다수결 존중 · Spring Data JPA 명명 규칙과 자연.

이 답의 함의:
- **명시성 우선**: `@SQLRestriction`이 자동 필터를 제공하지만 그건 "숨김"이기도 함. Repository 쿼리에서 `deleted = false`를 명시하는 편이 코드 리뷰·디버깅 시 명확.
- **관행의 다수결 존중**: 5:2로 관행 A가 다수. 새 관행을 계속 확산하기보다 다수결에 맞춰 통일하는 것이 코드베이스 인지 부담 최소.
- **Spring Data JPA 명명 규칙과 자연**: `findByUserIdAndDeletedFalse` 같은 메서드 자동 파싱. `@SQLRestriction`은 이 흐름과 어긋남.
- **삭제 시각 저장 손실은?**: `deleted_at DATETIME(6) NULL`을 병행 유지하는 것으로 대응 (도메인은 boolean 필드 + timestamp 필드 병존). 삭제 시각은 저장하되 필터는 boolean으로.
- **면접 답변 카드**: "관행이 갈렸을 때 어떻게 통일?" → "다수결 존중 · 명시성 우선 · Spring Data JPA 자연 결합. 자동 필터의 편의보다 리뷰·디버깅의 명확성이 우선."

---

## 4. 판단 지점 · Q2 — 통일 시 트리거 조건

Option A/B/D 어느 것이든 리팩토링이 필요. 언제 착수?

- **트리거 후보 1**: 세 번째 신규 Soft Delete 도메인 등장 시 (다음 갈림 확대 방지)
- **트리거 후보 2**: 기존 도메인에 삭제 시각 저장 요구 부상 시 (복원 UX · 감사)
- **트리거 후보 3**: 통합 테스트에서 Soft Delete 관련 회귀 발견 시
- **트리거 후보 4**: 그냥 별도 마일스톤에 계획적 착수 (M4~M6 어느 시점)

**📌 미결정** — 사용자가 명시적 답변 없이 지나감. 트리거 후보 재확인:

- (α) 세 번째 신규 Soft Delete 도메인 등장 시 (자동 트리거)
- (β) 실제 문제(회귀 · 복원 요구 · 감사 부상) 발생 시 (수동 트리거)
- (γ) 계획적 마일스톤 (M5 등, 별도 스토리로 관행 통일)
- (δ) 배포 재개 전 (M7 이전에 관행 확정 · dev/prod 관측 안정성 확보)

**권장 조합**: (γ) + (δ) — M5~M6 사이에 계획적 착수하되 M7 배포 재개 전 완료.

**즉시 실행 가능한 부분** (0.0.3v 다음 마일스톤 검토):
1. `.claude/rules/conventions.md` §3.4에 방향 명시 ("관행 A boolean deleted를 표준으로. 관행 B는 legacy로 유지")
2. 신규 도메인의 관행 통일: 다음 Soft Delete 도메인 신설 시 관행 A로 강제
3. LearningAxis/LearningLayer 회귀 스토리: 계획 세우고 M5+ 착수

---

## 5. 판단 지점 · Q3 — 관행 B의 벤더 종속 걱정

`@SQLRestriction`은 Hibernate 종속. 다른 JPA 구현체(EclipseLink 등)로 마이그레이션 필요 시 재작성.

- **현재 가능성**: 낮음. Spring Boot 기본 Hibernate · 이관 계획 없음.
- **미래 가능성**: DB 이관 (MySQL → PostgreSQL) 시 재검증 필요할 수 있음 (SQL 문법 차이).

**📌 실제 판단 (사용자 답변)**: **(a) 무시 · Hibernate가 사실상 표준**. 다른 구현체 이관 우려 낮음. MySQL 이관 등은 다른 이슈.

이 답의 함의 (Q1 답변과 겹쳐서 해석):
- **Q1(A 회귀)와 Q3(벤더 종속 무시)는 얼핏 상충**하지만 실제로는 정합. **관행 A를 채택하는 이유가 "벤더 종속"이 아니라 "명시성·다수결·Spring Data JPA 자연 결합"**임을 확인.
- 관행 B의 기술적 리스크(벤더 종속)는 문제가 아님 · 관행 B가 나쁜 것도 아님 · 그저 **다수결과 명시성 우선**의 결과로 A 채택.
- 이 정리는 **Option B로 이관하는 대안이 여전히 살아있음**을 함의 (미래 결단 시 A/B 어느 쪽도 정당함).
- **면접 답변 카드**: "관행을 통일할 때 벤더 종속을 어떻게 다루나?" → "벤더 종속만으로는 관행 선택 근거가 안 됨. 명시성 · 팀 관행 · Spring Data와의 조화가 우선 기준."

---

## 6. AbstractSoftDeletable 안 (Option D) — 참고 · 미채택

Java에서 mixin 상속을 구현하는 표준 방법:

```java
@MappedSuperclass
public abstract class AbstractSoftDeletable {
    @Column(name = "deleted")
    private boolean deleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isDeleted() { return deleted; }
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}

@Entity
public class LearningLayer extends AbstractSoftDeletable {
    // ...
}
```

**장점**:
- 필드·메서드 응집 · 상속 계층으로 표현
- boolean 필드(관행 A)와 timestamp 필드(감사) 둘 다 포함
- 도메인 별 Soft Delete 로직 중복 감소

**미채택 이유**:
- Aggregate Root마다 override 요구 (softDelete 정책은 도메인별 다름 — LearningLayer는 활성 axis 있으면 예외, Card는 즉시 삭제)
- 상속보다 조합(composition)이 domain-driven 정합
- 소규모 프로젝트에서 상속 도입은 오버킬

**면접 답변 카드**: "공통 로직 응집을 위한 상속 vs 조합?" → "도메인 별 override 필요성이 크면 상속 부담 · 정책이 도메인마다 다르므로 각 Aggregate가 자기 로직 소유가 자연."

---

## 7. 리팩토링 계획 (관행 A 통일 방향 · 미래)

### Story 후보

- **Story-SDU-1**: `.claude/rules/conventions.md` §3.4 갱신 — 관행 A 표준 명시 · 관행 B는 legacy · 신규 도메인은 A 강제
- **Story-SDU-2**: LearningAxis를 관행 A로 회귀 — `@SQLRestriction` 제거 · Repository 명시 필터 · `deleted_at` 컬럼 유지
- **Story-SDU-3**: LearningLayer를 관행 A로 회귀 — 동일 패턴
- **Story-SDU-4**: 신규 도메인 관행 강제 lint (있으면) 또는 review 체크리스트 추가

### 순서 · SP

- SDU-1 (0.5d) · SDU-2 (1d) · SDU-3 (1d) · SDU-4 (0.5d) 합 3d.

### 마이그레이션 필요 여부

- 컬럼 스키마는 그대로 (`deleted_at` 유지) · Java 도메인만 변경. Flyway 마이그레이션 없음.
- Repository 쿼리 갱신 (5~10 파일 예상).
- 테스트 갱신 (10~20 파일 예상 — 대부분 회귀 없음 · 자동 파악).

---

## 8. 관행 통일 이후의 문서화

- `.claude/rules/conventions.md` §3.4:
  - **[표준]** `boolean deleted` + Repository 명시 필터 + `deleted_at DATETIME(6) NULL` (감사)
  - **[legacy]** `@SQLRestriction` + `@SQLDelete` — LearningAxis · LearningLayer 회귀 대상
  - **[신규 도메인 규칙]** Soft Delete 필요 시 표준 관행만 사용
- `docs/adr/ADR003.md` 갱신 검토: Soft Delete 정책 명시.
- `docs/DOMAIN.md` §Soft Delete 절 신설.

---

## 9. 재검토 트리거 · 관찰 metrics

- **트리거**: 세 번째 Soft Delete 도메인 신설 요구 시 (즉시 착수 검토)
- **트리거**: LearningAxis/Layer의 SQLRestriction 이슈 발생 시 (테스트 · 쿼리 오류)
- **metrics**: 관행 A 도메인 개수 / 관행 B 도메인 개수 비율. 목표 7:0 (통일 완주)

---

## 종합 · 면접 답변 카드

### 핵심 통찰 3개

1. **관행이 갈렸을 때 명시성 · 다수결 · 팀 관행이 우선** — 기술적 우수함(자동 필터)보다 인지 부담 · 리뷰 명확성 우선. Q1 답변 근거.
2. **벤더 종속과 관행 선택은 독립** — Hibernate 사실상 표준이라도 관행 선택 근거로 부족. Q3 답변 근거.
3. **관행 통일 리팩토링은 계획적 착수 + 배포 재개 전 완료** — 자동 트리거(신규 도메인 등장) + 계획 트리거 조합.

### 재확인 시점

- 다음 Soft Delete 도메인 신설 시 즉시 관행 A로 강제
- M5~M6에 SDU-1~SDU-4 스토리 실행
- M7 배포 재개 전 관행 통일 완료 확인

---

*작성: 2026-07-02 | 편성: Part 05/06 | 사용자 실제 판단 반영 · Q1(A 회귀) · Q2(미결정 · 권장 조합) · Q3(벤더 종속 무시)*


---

## 6. 이어질 §6~9 (답변 후 완성)

Q1~Q3 답변 주시면:

- §6. **AbstractSoftDeletable** mixin 안의 실현 가능성 (Option D · JPA @MappedSuperclass 활용)
- §7. 실제 리팩토링 계획 (선택된 방향에 따라 · 순서 · Story 분해)
- §8. 관행 통일 이후의 문서화 (`.claude/rules/conventions.md` §3.4 갱신 · 예시 정리)
- §9. 재검토 트리거 · 관찰 metrics
