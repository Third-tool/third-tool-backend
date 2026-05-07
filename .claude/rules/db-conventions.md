# Rule: DB·JPA 매핑 컨벤션

> private-docs/table/* 전반의 일관된 스키마·매핑 규칙. JPA 엔티티 매핑, Flyway 마이그레이션, QueryDSL 작성 시 적용한다.
> 대상 DBMS: MySQL 8.0 (운영) / H2 호환 모드 (개발).

---

## 1. PK 전략

- 모든 테이블 PK는 **`BIGINT NOT NULL AUTO_INCREMENT`**.
- UUID / ULID 미사용 (단일 RDS, 샤딩 계획 없음, public ID 노출 표면 작음).
- 매핑 엔티티(`topic_material`, `card_tag`)도 **surrogate `id` 컬럼 유지** — 복합 PK 대신. 미래 속성 확장(메모, 적합도 점수 등) 대비.
- 외부 통합으로 public ID가 필요해지면 **`public_id CHAR(26)` 별도 컬럼**을 추가하고 내부 관계는 BIGINT 유지하는 하이브리드로 간다.

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

---

## 2. Enum 저장 방식

**VARCHAR + CHECK 제약 + `@Enumerated(EnumType.STRING)`**.

```java
@Enumerated(EnumType.STRING)
@Column(name = "coverage_status", length = 20, nullable = false)
private CoverageStatus coverageStatus;
```

```sql
CONSTRAINT chk_axis_topic_coverage
    CHECK (coverage_status IN ('NO_MATERIAL', 'PARTIAL', 'COVERED'))
```

**금지**: `@Enumerated(EnumType.ORDINAL)` — Enum 선언 순서가 바뀌면 데이터가 조용히 오염된다.

**컬럼 길이 가이드**: 짧은 enum은 `VARCHAR(20)`, 더 긴 코드 값은 길이를 명시. 가변 길이로 두지 않는다.

---

## 3. Audit 컬럼

| 컬럼 | 타입 | 적용 대상 |
| --- | --- | --- |
| `created_at` | `DATETIME(6) NOT NULL` | 거의 모든 엔티티 |
| `updated_at` | `DATETIME(6) NOT NULL` | **수정 행위가 있는 엔티티에만** |
| `deleted_at` | `DATETIME(6) NULL` | Soft Delete 적용 엔티티에만 |
| `linked_at` / `changed_at` | `DATETIME(6) NOT NULL` | 매핑·이력 엔티티의 사실 기록 시각 |

**규칙**:
- 정밀도는 **microsecond (`DATETIME(6)`)**. 모든 audit 컬럼 통일.
- 수정 행위가 없는 매핑 테이블(`topic_material`, `card_tag`)은 **`updated_at` 컬럼을 두지 않는다** — 연결·해제만 있고 수정은 없으므로.
- 외부에서 audit 컬럼을 직접 주입하지 않는다 — JPA Auditing(`@CreatedDate`, `@LastModifiedDate`) 또는 `@CreationTimestamp`로 자동 기록.
- `changed_by` 컬럼은 v1에서 추가하지 않는다 (모든 변경 주체가 소유 유저 본인). Admin BC 도입 시 추가.

---

## 4. Soft Delete

| 적용 여부 | 도메인 | 근거 |
| --- | --- | --- |
| ✅ 적용 | `Card`, `Deck`, `LearningFacade`, `LearningMaterial`, `User` | 사용자가 명시적으로 삭제하지만 복원 가치가 있는 핵심 자산 |
| ❌ 미적용 | `LearningAxis`, `AxisTopic`, `TopicMaterial`, `CardTag`, `KeywordCue` | 구조 편집·연결 사실 기록. 복원 요구 낮음 |

### JPA 매핑

```java
@Entity
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE learning_facade SET deleted_at = NOW(6) WHERE id = ?")
public class LearningFacade { ... }
```

- `@SQLRestriction` (Hibernate 6.3+) — 6.3 이전이면 `@Where(clause = "deleted_at IS NULL")`.
- 하드 삭제는 **금지**. `softDelete()` 메서드를 통해서만 (CLAUDE.md 규칙).

### MySQL 부분 인덱스 미지원
- PostgreSQL의 `CREATE INDEX ... WHERE deleted_at IS NULL` 같은 부분 인덱스는 MySQL 8.0에서 지원되지 않는다.
- 일반 인덱스 사용 + 모든 조회에 `deleted_at IS NULL` 자동 적용 (`@SQLRestriction`).
- 카디널리티가 커지면 함수 인덱스로 우회 — 현재 단계에서는 불필요.

---

## 5. 인덱스 작성 가이드

| 패턴 | 인덱스 |
| --- | --- |
| `WHERE parent_id = ? ORDER BY display_order ASC` | `INDEX (parent_id, display_order)` (커버링) |
| 외래 키 컬럼 단독 조회 | `INDEX (fk_column)` |
| 자주 필터되는 enum 컬럼 | `INDEX (enum_column)` (예: `coverage_status`) |
| 중복 방지 | `UNIQUE` 제약 (인덱스 자동 생성) |
| 양방향 조회 매핑 테이블 | 양쪽 FK에 각각 단독 인덱스 (`idx_topic_material_topic`, `idx_topic_material_material`) |
| Soft Delete + UNIQUE | UNIQUE는 `deleted_at IS NULL` 조건과 함께 도메인 레벨 유일성을 보장. MySQL 부분 UNIQUE 미지원이므로 운영상 필요 시 함수 인덱스로 우회 |

**원칙**: 도메인 모델 문서의 Repository 메서드 시그니처가 인덱스 설계의 1차 근거다 (예: `findByAxisIdOrderByDisplayOrderAsc` → 복합 인덱스 필수).

---

## 6. 외래 키 + JPA orphanRemoval

DB FK와 JPA `orphanRemoval`은 **이중으로** 둔다.

```sql
CONSTRAINT fk_axis_topic_axis
    FOREIGN KEY (axis_id) REFERENCES learning_axis (id)
    ON DELETE CASCADE
```

```java
@OneToMany(mappedBy = "axis", cascade = CascadeType.ALL, orphanRemoval = true)
private List<AxisTopic> topics = new ArrayList<>();
```

- DB `ON DELETE CASCADE` — DB 레벨 안전망. 직접 SQL 삭제에도 유효.
- JPA `orphanRemoval = true` — Aggregate에서 컬렉션 제거 시 즉시 자식 삭제.
- 두 메커니즘이 함께 있어야 도메인 행위와 직접 SQL 양쪽에서 자식이 정합 상태로 유지된다.

---

## 7. 정규화 / 역정규화 원칙

기본은 **3NF**. 역정규화는 명시적 근거 + 동기화 책임 명시 필요.

| 케이스 | 처리 |
| --- | --- |
| 빈번한 목록 조회에 매번 JOIN+집계가 필요 | 역정규화 컬럼 추가 (예: `axis_topic.coverage_status`) |
| 동기화 | **이벤트 기반 비동기 불허**. 변경을 일으킨 동일 트랜잭션에서 Application Service가 갱신 |
| 안전망 | 야간 배치로 전체 재계산 |
| "통계용" 카운터 (대시보드) | **역정규화하지 않는다** — 별도 집계 쿼리로 충분 |

쓰기 경로를 복잡하게 만들 만한 가치가 있는지 확인 후 추가한다. 없으면 두지 않는다.

---

## 8. Flyway 마이그레이션 작성 규칙

(CLAUDE.md 규칙 보강)

- **새 V 버전 파일만 추가**. 기존 V 파일 수정 금지 (Flyway 정합성 깨짐).
- 파일명: `V{n}__{snake_case_purpose}.sql`. 한 마이그레이션은 한 의미 단위.
- 데이터 이관이 동반되는 마이그레이션은 **롤백 스크립트(`R{n}__rollback_*.sql`) 동반 작성**.
- 데이터 이관 시 검증 SQL을 PR 본문 또는 마이그레이션 주석에 기재 (예: `SELECT COUNT(*) FROM old = SELECT COUNT(*) FROM new`).
- 컬럼 추가 + NOT NULL 전환은 **3단계로 나눈다**:
  1. `ADD COLUMN ... NULL`
  2. `UPDATE ... SET col = default WHERE col IS NULL` (백필)
  3. `MODIFY COLUMN ... NOT NULL`
- AUTO_INCREMENT 카운터를 직접 INSERT한 데이터 이관 후에는 `ALTER TABLE ... AUTO_INCREMENT = MAX(id) + 1`로 수동 보정.
- 폐기 테이블은 **즉시 DROP하지 않고 RENAME으로 아카이브** 우선 (예: `action_revision` → `axis_action_description_history`). 6개월 이상 미사용 확인 후 DROP 검토.

---

## 9. URL 컬럼 길이

- 외부 URL 컬럼은 `VARCHAR(2048)` (브라우저 표준 URL 최대치).
- nullable 허용 — 권장하지만 필수 아님.

---

## 10. 자유 텍스트 컬럼 길이 가이드

| 의미 | 길이 |
| --- | --- |
| 짧은 한 줄 입력 (컨셉, 축 이름) | `VARCHAR(50)` ~ `VARCHAR(100)` |
| 명사구 제목 (주제 이름) | `VARCHAR(100)` |
| 자료명 | `VARCHAR(200)` |
| 부연 설명 | `VARCHAR(500)` (긴 메모는 별도 도메인으로 유도) |
| URL | `VARCHAR(2048)` |
| 본문 / 자유 텍스트 | `TEXT` |

길이는 도메인 문서의 명시값을 우선 따른다. 임의로 늘리지 않는다.
