# adr002: Enum 컬럼은 VARCHAR + CHECK 제약으로 저장한다

**영역**: data | **상태**: Accepted | **날짜**: 2026-05-07

> **면접 포인트**
> "@Enumerated(EnumType.ORDINAL) vs STRING, 왜 STRING에 CHECK 제약까지 추가했나요?"
> → ORDINAL은 선언 순서 변경 시 데이터가 조용히 오염된다. CHECK 제약은 애플리케이션 검증 누락 시 DB가 2차 방어를 제공한다.

---

## 왜 이 결정이 필요했나 (Context)

`MaterialType`, `ProficiencyLevel`, `CoverageStatus`, `CardStatus` 등 여러 Enum 타입 도메인 값을 RDB에 저장하는 방식을 통일해야 했다.

- 본 프로젝트는 도메인 모델 변경이 빈번 (예: `AxisAction → AxisTopic` 전환)
- Enum 값 추가·재배치 가능성 존재
- JPA 기본 `@Enumerated(EnumType.ORDINAL)`은 순서를 INT로 저장 → 선언 순서가 바뀌면 기존 데이터가 다른 의미로 읽힘
- 운영 디버깅 시 DB 콘솔에서 숫자가 아닌 의미 있는 값이 보여야 함

---

## 무엇을 결정했나 (Decision)

모든 Enum 컬럼에 3요소를 함께 적용:

1. DB 컬럼: **`VARCHAR(N) NOT NULL`** (보통 N=20)
2. DB 제약: **`CHECK (col IN ('VALUE_A', 'VALUE_B', ...))`**
3. JPA 매핑: **`@Enumerated(EnumType.STRING)`**

```java
@Enumerated(EnumType.STRING)
@Column(name = "coverage_status", length = 20, nullable = false)
private CoverageStatus coverageStatus;
```

```sql
CONSTRAINT chk_axis_topic_coverage
    CHECK (coverage_status IN ('NO_MATERIAL', 'PARTIAL', 'COVERED'))
```

---

## 대안과 거부 이유 (Alternatives)

| 대안 | 장점 | 거부 이유 |
|------|------|-----------|
| `ORDINAL` (INT 저장) | 저장 공간 작음, 인덱스 효율 | Enum 선언 순서 변경 시 데이터 조용히 오염. 운영 콘솔에서 의미 안 보임 |
| 룩업 테이블 + FK | 정규화 형식, 메타데이터 부착 가능 | JOIN 비용. Enum이 도메인 코드와 1:1이라 독립 변경 사유 없음 |
| 자유 VARCHAR (CHECK 없음) | 마이그레이션 비용 작음 | 잘못된 값 진입 차단 안 됨. 도메인 검증 누락 시 데이터 오염 |

---

## 결과와 트레이드오프 (Consequences)

**긍정적**
- Enum 선언 순서 변경 시 데이터 오염 없음
- DB 콘솔에서 의미 직접 확인 가능 → 운영·디버깅 효율
- CHECK 제약이 애플리케이션 검증 누락을 DB 레벨에서 2차 방어

**트레이드오프**
- INT 대비 저장 공간 약간 더 필요 (카디널리티 낮은 Enum이라 실측 영향 미미)
- Enum 값 이름 변경 시 DDL+UPDATE 마이그레이션 필요
- Enum 값 추가 시 모든 환경의 CHECK 제약을 Flyway로 갱신해야 함

---

## 재검토 시점

- Enum이 표시 라벨·설명·순서 등 메타데이터를 가져야 하는 경우 → 룩업 테이블 전환 검토
- Enum 값이 수십 개 이상으로 늘어 CHECK 제약 갱신이 빈번해지는 시점
