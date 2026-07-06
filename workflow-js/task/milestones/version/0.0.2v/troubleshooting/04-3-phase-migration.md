# Deep-dive · Part 04 — Axis.layer_id 재배선을 3-phase 마이그레이션으로 결정하기까지

> **본 편의 성격**: Flyway V19의 3-phase(ADD NULL → 백필 → NOT NULL + FK) 결정. 이 결정은 "이렇게 하는 게 정답"으로 명시되기 전에 **여러 대안(blue-green · shadow table · online DDL · 즉시 NOT NULL 등)이 저울에 있었다**. 왜 3-phase가 이겼는지, 어떤 상황에서 무너지는지.

---

## 0. 문제 상황 — 이미 있는 데이터에 NOT NULL FK 컬럼 추가

Story LT-E2-S2/S3의 요구:
- `learning_axis` 테이블에 `learning_layer_id BIGINT NOT NULL` 컬럼 추가
- FK: `REFERENCES learning_layer(learning_layer_id) ON DELETE RESTRICT`
- 기존 axis 데이터가 이미 있음. 어떤 layer로 연결할지 정해야 함.
- default Uncategorized Layer가 facade마다 자동 생성돼야 함 (Story 2-3)

**직접 도전**:
```sql
-- 순진한 시도
ALTER TABLE learning_axis
    ADD COLUMN learning_layer_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_learning_axis_layer FOREIGN KEY (learning_layer_id)
        REFERENCES learning_layer (learning_layer_id) ON DELETE RESTRICT;
```

**MySQL 결과**: 실패. NOT NULL 컬럼을 기본값 없이 추가 · 기존 row에 값 없음 · Constraint 위반 · 마이그레이션 rollback.

---

## 1. 대안들 (기각된 것 포함)

### Option A — 즉시 NOT NULL + DEFAULT 값

```sql
ALTER TABLE learning_axis
    ADD COLUMN learning_layer_id BIGINT NOT NULL DEFAULT 0;
```

- **기각 사유**: 0은 유효한 layer_id가 아님. FK 제약이 결국 실패. 어떤 실제 layer로 연결할지 여전히 미해결.
- 학습: DEFAULT는 non-null 값을 강제하지만 그 값이 유의미해야 함. 임의값은 semantic 파열.

### Option B — 백필 스크립트 (별도 실행)

```
1. 마이그레이션에서 ADD COLUMN NULL
2. 부팅 후 별도 Java 스크립트 실행 · 백필
3. 다음 마이그레이션에서 MODIFY NOT NULL
```

- **기각 사유**: 단계 간 상태(부팅 후 백필 전)에서 컬럼이 nullable. 그 상태에 배포·재부팅 시 부정합 데이터 유입 위험. 배포 로직에 "백필 완료 여부 체크" 추가 · 복잡.
- 학습: **마이그레이션은 자기완결이어야 함**. 외부 스크립트 의존은 배포 순서 관리 부담.

### Option C — Blue-Green (읽기 새 컬럼 · 쓰기 병행)

```
Deploy v_N   : 새 컬럼 · 옛 컬럼 병행 · 쓰기는 양쪽 · 읽기는 옛 컬럼
Backfill    : 스크립트로 옛 → 새
Deploy v_N+1 : 읽기 새 컬럼 · 쓰기 새 컬럼만
Cleanup      : 옛 컬럼 DROP
```

- **적합 상황**: 대규모(수백만 row) · 다운타임 불허 · 배포 여러 번 가능한 환경.
- **기각 사유**: 현재 스코프에 과함 · dev/prod row 수 < 1000 · 배포 부담 · 마이그레이션이 여러 릴리스 걸침. 학습: **오버킬 조건이 있으면 단순 안을 우선**.

### Option D — Shadow Table

```
1. learning_axis_new 신설 (완성된 스키마)
2. 트리거 · dual write
3. 백필
4. RENAME
```

- **적합 상황**: 온라인 DDL 미지원 DB · 매우 큰 테이블.
- **기각 사유**: MySQL 8.0은 online DDL 지원 · 스코프 과함. 학습: **인프라 능력 실측 후 결정**. DB가 이미 지원하는 걸 우회하지 말 것.

### Option E — 3-phase (채택) — ADD NULL → 백필 → NOT NULL + FK

```sql
-- Phase 1: nullable 컬럼 추가
ALTER TABLE learning_axis ADD COLUMN learning_layer_id BIGINT NULL;

-- Phase 2-a: default layer 생성 (idempotent)
INSERT INTO learning_layer (facade_id, name, ...)
SELECT ... FROM learning_facade WHERE ... AND NOT EXISTS (...);

-- Phase 2-b: axis 백필
UPDATE learning_axis la JOIN learning_layer ll ...
SET la.learning_layer_id = ll.learning_layer_id
WHERE la.learning_layer_id IS NULL;

-- Phase 3-a: NOT NULL 승격
ALTER TABLE learning_axis MODIFY COLUMN learning_layer_id BIGINT NOT NULL;

-- Phase 3-b: FK 제약
ALTER TABLE learning_axis ADD CONSTRAINT fk_learning_axis_layer ...;
```

- **자기완결**: 하나의 V19 파일 · Flyway가 원자적으로 실행.
- **롤백 가능**: R19가 역순 · 실패 시 안전.
- **재실행 안전**: Phase 2가 idempotent (NOT EXISTS · WHERE IS NULL 필터).

---

## 2. 판단 지점 · Q1 — 왜 Option E가 이겼나

각 옵션의 트레이드오프:

| Option | 자기완결 | 재실행 안전 | 복잡도 | 스케일 |
| --- | --- | --- | --- | --- |
| A (즉시 NOT NULL) | ✅ | ✅ | 낮음 | X (semantic 파열) |
| B (별도 백필 스크립트) | ❌ | 부분 | 중 | ✅ |
| C (Blue-Green) | ❌ (여러 릴리스) | 부분 | 높음 | ✅ (대규모) |
| D (Shadow Table) | ❌ | 부분 | 매우 높음 | ✅ (매우 큰) |
| **E (3-phase, 채택)** | ✅ | ✅ (idempotent) | 중 | 중 |

**📌 실제 판단 (사용자 답변)**: **(α) 자기완결성** — 하나의 V 파일 안에 완결.

이 답의 함의:
- **배포 순서 관리 부담 최소화**: 여러 스크립트·여러 릴리스에 걸치면 배포 시점 오류·순서 실수 위험. 하나의 V로 원자적 실행이 최고 안전.
- **Flyway의 원자성 활용**: V 파일 실행은 Flyway가 트랜잭션으로 감쌈 (또는 실패 시 명확히 fail). 3-phase가 한 파일이어야 이 원자성 이득 최대.
- **회귀 대응 단순**: 문제 발생 시 "V19를 다시 실행하면 됨" 또는 "R19를 실행" 두 명령. 여러 파일·여러 스크립트 · 부분 상태 관리 필요 없음.
- **면접 답변 카드**: "3-phase를 어떻게 담았나?" → "하나의 V 파일 안에 순차 실행. Flyway가 원자적으로 실행하고 부분 실패는 rollback. 배포 순서 관리 부담 최소화."

---

## 3. 판단 지점 · Q2 — Phase 2-a와 2-b를 왜 한 V 파일에

3-phase의 미묘한 결정: default layer 생성(2-a)과 axis 매핑(2-b)을 **한 V19 안에** 넣었다.

대안: default layer 생성을 V18(learning_layer 신설)에서 하고, V19는 axis 매핑만.

### 채택안 (한 V19)

```
V18: learning_layer 테이블만 (스키마)
V19: axis 컬럼 + Uncategorized layer 백필 + axis 백필 + NOT NULL + FK
```

- **장점**: axis 재배선과 관련된 모든 마이그레이션이 한 파일 · 추적 명료.
- **단점**: V19 파일이 큼 (~50 line).

### 대안 (분리)

```
V18: learning_layer 테이블 + 각 facade에 default 발행
V19: axis 컬럼 · 백필 · NOT NULL · FK
```

- **장점**: 각 V 파일 관심사 명확 (스키마 vs FK 재배선).
- **단점**: V18 실행 후·V19 실행 전에 axis가 layer 없이 있는 상태. Flyway가 원자적이라 문제 없지만 개념적으로 축이 layer 미매핑 상태.

**📌 실제 판단 (사용자 답변)**: **(a) 관련 마이그레이션 응집 · 추적 명료** — axis 재배선과 관련된 모든 마이그레이션이 한 파일.

이 답의 함의:
- **"응집도" 원칙이 마이그레이션에도 적용**: Part 01 Q1(BC 응집도)의 사고 패턴이 여기서도 반복. 관련성 있으면 한 파일, 없으면 분리.
- **미래 유지보수 시각**: 6개월 후 axis-layer 재배선 이력을 찾을 때 V19 하나만 보면 됨. V18/V19 분산은 두 파일 모두 봐야 함.
- **관심사 기준**: V18은 "learning_layer라는 새 도메인 소개", V19는 "axis를 layer로 재배선"이라는 다른 관심사. default layer 발행은 재배선 로직의 일부이므로 V19가 자연 위치.
- **면접 답변 카드**: "여러 마이그레이션을 언제 한 파일에 담나?" → "관련성 있는 마이그레이션(같은 이관 · 같은 refactor)은 한 V. 관심사가 다르면 분리. 응집도가 기준."

---

## 4. 판단 지점 · Q3 — Hibernate ddl-auto와의 관계

**중요 실측**: `application-test.yml`이 `spring.flyway.enabled: false`. 테스트는 Hibernate가 스키마 생성.

이 상황에서 V19의 실제 백필 SQL은 **테스트에서 검증 안 됨**. Hibernate가 create-drop으로 빈 테이블 만들어버림.

- **영향**: V19 SQL 오류가 테스트에서 안 잡힘 · prod 배포 시점에 발견 지연.
- **대응 후보**:
  - (i) @SpringBootTest에서 별도 profile로 Flyway 활성화
  - (ii) TestContainers + MySQL + Flyway 활성 · Slice 테스트
  - (iii) 로컬 bootRun에서 spot check (0.0.3v 채택안)
  - (iv) 별도 CI 단계로 dev 환경 dry-run

**📌 실제 판단 (사용자 답변)**: **(a) rush 정책** — 정밀 검증은 배포 재개 후로 미룸.

이 답의 함의:
- **rush의 정직한 함의**: 이 결정은 이론적 우수함이 아니라 시간 제약 하 실용 선택. 이를 문서에 명시하는 것이 나중을 위한 정직성.
- **위험 격리**: V19 SQL 오류 발견 지연 위험은 R19 rollback 즉시 실행 가능성으로 완화. 부팅 실패 시 R19 실행 → 원복.
- **후속 대응 명시**: 배포 재개 시점(M7+)에 dev 환경에서 실제 V19 실행 · 백필 결과 spot check. 이 시점을 놓치면 위험 무한 이월.
- **면접 답변 카드**: "rush 정책 하 검증 부담을 어떻게 완화?" → "위험 격리 (rollback 스크립트 준비) + 후속 검증 시점 명시. rush의 정직함은 위험을 숨기지 않고 재검증 트리거를 남기는 것."

---

## 5. 미결정 남긴 것 — R19의 default layer 처리

R19 rollback SQL:

```sql
DROP INDEX idx_learning_axis_layer ON learning_axis;
ALTER TABLE learning_axis DROP FOREIGN KEY fk_learning_axis_layer;
ALTER TABLE learning_axis MODIFY COLUMN learning_layer_id BIGINT NULL;
ALTER TABLE learning_axis DROP COLUMN learning_layer_id;
```

**주목**: V19에서 생성한 Uncategorized layer를 rollback에서 **DELETE 안 함**. 이유:
- R18(learning_layer 폐기)이 이 처리 담당하는 것이 자연스러움
- 데이터 손실 방지 (사용자가 Uncategorized에 axis 추가했을 수 있음)
- rollback은 최소 변경 원칙

**📌 원칙 정리 (Q4 시각적 명료성을 위해 논쟁 없이 원칙만 명시)**:
- rollback은 **"생성한 스키마 되돌리기 + 데이터 손실 방지"** 이중 원칙.
- 스키마: 되돌린다 (컬럼 · FK · index).
- 데이터: 되돌리지 않는다 (사용자가 추가한 것이 있을 수 있음).
- rollback 후 재 migration은 idempotent로 안전 (NOT EXISTS · WHERE IS NULL).
- **면접 답변 카드**: "rollback 스크립트의 정책?" → "스키마는 되돌리고 데이터는 보존. rollback 후 재 migration이 idempotent해서 두 번 실행 안전."

---

## 6. Flyway `NOT EXISTS` idempotent 패턴의 재사용 자산

V17(concept 백필) · V19 Phase 2-a(layer 발행) 둘 다 동일 패턴:

```sql
INSERT INTO {target_table} (...)
SELECT ...
FROM   {source_table}
WHERE  {conditions}
  AND  NOT EXISTS (SELECT 1 FROM {target_table} WHERE {match_key});
```

**패턴의 이점**:
- 재실행 안전 (부분 실패 후 재시도 시 이미 삽입된 것은 skip)
- rollback 후 재migration도 안전
- 배포 환경 간 편차 (dev 이미 반영 · prod 아직) 대응

**패턴의 함정**:
- `NOT EXISTS`가 자주 참조되는 컬럼은 인덱스 필요 (성능)
- `SELECT ... WHERE NOT EXISTS`는 대규모 데이터에서 조인 · 서브쿼리 비용 큼

**재사용 자산화 제안**: `docs/runbook/idempotent-backfill.md` 문서화 검토 (다음 마일스톤).

---

## 7. legacy 컬럼 정리 로드맵 (Part 06 예고)

- `learning_facade.concept` NOT NULL 컬럼 유지 상태.
- 3-phase 계획:
  1. `MODIFY concept BIGINT NULL` (NOT NULL 해제)
  2. `UPDATE learning_facade SET concept = NULL WHERE concepts 자식이 있는 경우` (backfill)
  3. `ALTER TABLE learning_facade DROP COLUMN concept`
- 각 단계 별도 V로 · 배포 여러 번 걸침 · Blue-Green 패턴 소규모 적용.
- Part 06에서 상세.

---

## 8. 재검토 시점

- **row 수 폭증** (사용자 대량 유입) — 3-phase의 Phase 2 백필이 초 단위 → 분 단위 · 배포 부담 증가 → Blue-Green 검토
- **다중 DB 지원** (PostgreSQL 등) — MySQL의 NULL UNIQUE 취급 활용한 로직이 다른 DBMS에서 다르게 동작 · partial index로 재구현
- **컬럼 DROP 요구 증가** — 여러 legacy 컬럼 정리 시 자산화 필요

---

## 종합 · 면접 답변 카드

### 핵심 통찰 3개

1. **자기완결성 우선** — 하나의 V 파일이 원자적 실행 · 배포 순서 관리 부담 최소화. 여러 파일·스크립트 분산은 오류 표면.
2. **응집도 기준 파일 분할** — 관심사 같으면 한 파일 · 다르면 분리. Part 01 BC 응집도 사고와 동일 원칙.
3. **rush의 정직함** — 검증 지연을 숨기지 않고 후속 트리거 명시. rollback 스크립트로 위험 격리.

### 대안 5개 정리

| 대안 | 왜 기각 | 무엇을 남겼나 |
| --- | --- | --- |
| 즉시 NOT NULL + DEFAULT | semantic 파열 (0은 유효 layer_id 아님) | "DEFAULT는 유의미해야" 원칙 |
| 별도 백필 스크립트 | 마이그레이션 자기완결성 파열 | "마이그레이션은 자기완결" 원칙 |
| Blue-Green | 스코프 과함 | "오버킬 조건 있으면 단순 안 우선" |
| Shadow Table | MySQL online DDL 지원 | "인프라 능력 실측 후 결정" |
| 3-phase (채택) | | 표준 관행 |

### 재검토 시점

- row 수 폭증 시 · 다중 DB 지원 시 · 컬럼 DROP 자산화 시

---

*작성: 2026-07-02 | 편성: Part 04/06 | 사용자 실제 판단 반영 · Q1(자기완결성) · Q2(응집 · 추적 명료) · Q3(rush 정직함)*


---

## 6. 이어질 §6~9 (답변 후 완성)

Q1~Q3 답변 주시면:

- §6. R19 rollback 스크립트 실제 실행 여부 (본 세션에서 안 함) · 실행 필요 시나리오
- §7. Flyway `NOT EXISTS` idempotent 패턴의 재사용 자산화 (다른 백필에서도 활용 가능)
- §8. 미래 · concept 컬럼 DROP(Part 06)의 3-phase 계획 · legacy 컬럼 정리 로드맵
- §9. 재검토 시점 · scaling 시나리오
