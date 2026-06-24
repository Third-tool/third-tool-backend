# adr001: Surrogate PK는 BIGINT AUTO_INCREMENT로 통일

**영역**: data | **상태**: Accepted | **날짜**: 2026-05-07

> **면접 포인트**
> "왜 UUID/ULID 대신 BIGINT를 선택했나요?"
> → 단일 RDS + 샤딩 계획 없음이라는 운영 제약을 근거로, 인덱스 효율을 가져가면서 소유권 검증으로 IDOR를 방어했다.

---

## 왜 이 결정이 필요했나 (Context)

6개 BC(Card, Deck, LearningFacade, Review, User, UserSchedule) 전체의 PK 전략을 통일해야 했다.

- 운영 환경은 **단일 RDS MySQL 8.0**, 샤딩 계획 없음
- REST API는 본인 리소스만 접근하는 구조(`/api/facade/me`, `/cards/{id}`)라 소유권은 Application Service에서 검증
- 매핑 엔티티(`topic_material`, `card_tag`)에 도메인 문서상 미래 속성 추가 가능성이 명시되어 있어 복합 PK는 부담

---

## 무엇을 결정했나 (Decision)

**모든 테이블 PK: `BIGINT NOT NULL AUTO_INCREMENT`**

- 매핑 엔티티도 surrogate `id` 컬럼 유지 — `(topic_id, material_id)` 복합 PK 미사용
- JPA: `@GeneratedValue(strategy = GenerationType.IDENTITY)` 통일
- 외부 통합이 필요해지면 `public_id CHAR(26)` 컬럼을 별도로 추가하고 내부 관계는 BIGINT 유지 (하이브리드 전환)

---

## 대안과 거부 이유 (Alternatives)

| 대안 | 장점 | 거부 이유 |
|------|------|-----------|
| UUIDv4 (랜덤) | 분산 ID 생성, URL 열거 방지 | InnoDB PK 페이지 분할 빈발, 16바이트 PK+FK 비용. 분산 ID 요구 없음 |
| ULID / UUIDv7 (시간 정렬) | UUID 장점 + 클러스터드 인덱스 친화 | 동일 비용 발생, 외부 통합 요구 없는 시점에 도입 가치 낮음 |
| 매핑 엔티티 복합 PK | PK 폭 절약 | 속성 확장 시 FK 관계 불편. 도메인 문서가 미래 속성 확장을 명시 |

---

## 결과와 트레이드오프 (Consequences)

**긍정적**
- InnoDB 클러스터드 인덱스 친화: 순차 증가 BIGINT로 페이지 분할 최소화
- JPA 매핑·디버깅 단순
- 매핑 엔티티 속성 확장이 자연스럽다

**트레이드오프**
- 순차 ID가 URL에 노출되므로 **소유권 검증을 Application Service에서 반드시 수행해야 한다** (미이행 시 IDOR 위험)
- 외부 통합 시 `public_id` 컬럼 추가 필요 가능성

---

## 재검토 시점

- 외부 시스템과의 통합으로 public ID 노출이 필요해진 시점
- 샤딩 도입 검토 시 (분산 ID 생성 전략 필요)
