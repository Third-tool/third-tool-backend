# adr003: Soft Delete는 사용자 자산성 도메인에만 선택 적용한다

**영역**: data | **상태**: Accepted | **날짜**: 2026-05-07

> **면접 포인트**
> "Soft delete를 왜 모든 테이블에 적용하지 않았나요?"
> → 도메인마다 삭제의 의미가 다르다. 사용자 자산(복원 가치 있음)과 구조 편집(재구성이 자연스러움)을 구분해 인덱스 오버헤드를 최소화했다.

---

## 왜 이 결정이 필요했나 (Context)

도메인마다 삭제 의미가 다르다:

- **사용자 자산성 도메인**: Card·Deck·LearningFacade·LearningMaterial — 사용자가 시간 들여 만든 것. 잘못 삭제했을 때 복구 요구 가능성이 높고, 활동 패턴 분석 가치도 있다.
- **구조 편집 도메인**: LearningAxis·AxisTopic·TopicMaterial·CardTag — 책 목차처럼 자유롭게 추가·삭제하는 편집 단위. 복원 요구가 사실상 없다.

MySQL 8.0은 PostgreSQL의 부분 인덱스(`WHERE deleted_at IS NULL`)를 지원하지 않는다. Soft delete를 광범위하게 적용하면 모든 인덱스의 카디널리티가 활성+삭제 데이터로 부풀어 오른다.

---

## 무엇을 결정했나 (Decision)

### 적용 대상 (`deleted_at DATETIME(6) NULL` + `@SQLRestriction` + `@SQLDelete`)

| 테이블 | 근거 |
|--------|------|
| `card` | 사용자 핵심 학습 자산. 복구 요구 가능 |
| `deck` | 카드 컨테이너. 잘못된 삭제 복원 가치 |
| `learning_facade` | 컨셉·축·자료의 루트. 전체 학습 맥락 |
| `learning_material` | 사용자가 직접 등록한 외부 자료 |
| `users` | 계정 자체 |

### 미적용 대상 — 일반 DELETE 또는 `orphanRemoval`

| 테이블 | 근거 |
|--------|------|
| `learning_axis`, `axis_topic` | 책 목차처럼 동적으로 재구성되는 편집 단위 |
| `topic_material`, `card_tag` | 연결 사실 기록 — 해제는 사실의 종료 |
| `keyword_cue` | Card에 종속된 자식, Card 삭제 시 함께 처리 |

### JPA 매핑 표준

```java
@Entity
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE table_name SET deleted_at = NOW(6) WHERE id = ?")
public class XxxEntity { ... }
```

하드 삭제 금지. 도메인은 `softDelete()` 메서드를 통해서만 제거 의도를 표현한다.

### 물리 삭제 정책

`deleted_at` 설정 후 90일 경과한 행은 월 1회 배치로 `archive` 스키마로 이동 후 본 테이블에서 DELETE. 현재 데이터 규모가 작아 즉시 도입은 불필요 — 6개월 이상 운영 후 규모 확인 뒤 결정.

---

## 대안과 거부 이유 (Alternatives)

| 대안 | 장점 | 거부 이유 |
|------|------|-----------|
| 모든 테이블에 soft delete | 일관된 정책, 실수 방지 | 구조 편집 도메인의 인덱스·쿼리 부담. 의미 없는 곳까지 보존 비용 발생 |
| 모든 테이블 하드 삭제 | 구조 단순, 인덱스 효율 | 사용자 복원 요구 대응 불가. 활동 추적 자료 손실 |
| 별도 archive 테이블로 이관 | 본 테이블 깨끗, 인덱스 효율 | 즉시 복원 비용 높음. 현재 규모에서 과도 |

---

## 결과와 트레이드오프 (Consequences)

**긍정적**
- 사용자 복원 요청 즉시 대응 가능
- 활동 패턴 분석·감사 추적 자료 보존
- 구조 편집 도메인은 일반 DELETE 단순성 유지 — 인덱스·쿼리 부담 최소화

**트레이드오프**
- 모든 조회에 `deleted_at IS NULL` 조건 필요 — `@SQLRestriction` 자동 적용이지만, 네이티브 쿼리·QueryDSL 직접 작성 시 누락 위험
- MySQL 부분 인덱스 미지원으로 인덱스 카디널리티에 삭제 데이터가 포함됨 → 90일 경과 행 archive 배치가 결국 필요
- UNIQUE 제약이 deleted 행도 포함하므로, 같은 이름으로 재생성 시 deleted 행 정리가 선행되어야 할 수 있음

---

## 재검토 시점

- 활성 행 대비 deleted 행이 30% 이상으로 커지는 시점 → 90일 archive 배치 도입
- PostgreSQL 또는 MySQL 부분 인덱스 지원 환경으로 이전하는 경우
- 새 도메인 추가 시 → 자산성 vs 구조 편집 분류 기준으로 판단
