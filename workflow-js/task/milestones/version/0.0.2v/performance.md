# 0.0.2v / Performance — 스킵 사유 명시 (0.0.3v 급행)

> **결정**: 0.0.2v/Tier 1+2 스코프는 **정밀 성능 측정 미수행**. 사유는 아래 §"왜 스킵인가".
> **본 파일 역할**: 스킵 결정 근거 · 관찰된 정성 지표 · 다음 버전 재측정 트리거 · 로컬 관찰 요령 기록.
> 상세 배포·k6 성능 baseline은 배포 재개 마일스톤(M7 이후)에서 실측 예정.

---

## 왜 스킵인가

`milestone.md` §"0.0.2v 스코프 메모"의 재편 결정에 이어, D2 pivot 이후 사용자 rush 정책 ("0.0.3v까지 빨리 내야해서, 일일히 코드를 확인하지 않을거고") 하에 다음 판단:

1. **배포 미포함** — Prometheus / Grafana / k6 부하 테스트 등 정밀 도구가 dev·prod 환경에 있어야 함. 본 버전은 로컬 스코프.
2. **도메인 리팩토링이 주력** — 성능 영향은 로컬에서 정성적으로 확인 가능하고, 정밀 회귀 감지는 dev 배포 이후 baseline과 비교해야 유효.
3. **속도 우선** — 5관점 Reviewer 세션 스킵과 동일 정책. Story별 회귀 감지는 `./gradlew test` 통과와 로컬 부팅 시각화로 대체.
4. **정확도 대비 시간 비용** — 로컬 개발자 하드웨어에 종속된 실측은 상대값으로만 유효. baseline이 명확한 배포 환경에서 재측정이 훨씬 신뢰도 높음.

---

## 대신 확인한 정성 지표 (본 버전 진행 중 관찰)

| 관찰 | 방법 | 결과 |
| --- | --- | --- |
| `./gradlew test` 전체 통과 | 매 Story 완료 시점 | ✅ 전 세션 BUILD SUCCESSFUL 유지 |
| `./gradlew test --tests "com.example.thirdtool.LearningFacade.*"` | Epic 완료 시점 | ✅ 약 1분 내 완료 (기존 baseline 대비 급 회귀 없음) |
| `./gradlew compileJava` 시간 | 각 도메인 파일 편집 후 | ✅ 8~30초 유지 (Java 21 + Gradle 8.x) |
| Repository Slice 테스트 시간 | @DataJpaTest 클래스별 | ✅ 개별 클래스 5초 이내 |
| 통합 테스트 (@SpringBootTest) | 기존 재활용 | ✅ 회귀 없음 (테스트 통과) |

**정성 판정**: 도메인 리팩토링(V16~V19 · concepts/Layer 신설)에 의한 급 회귀 신호 없음. 정밀 수치 baseline은 배포 재개 후.

---

## 관찰 가능한 성능 영향 지점 (다음 버전 실측 대상)

### 1. Spring Boot 부팅 시간

- **예상 영향**: 신규 4 V 버전 (V16~V19) 마이그레이션 실행 시간 추가. 로컬 H2에서는 무의미 수준 (밀리초).
- **prod 예상**: MySQL에서 V19 3-phase가 alter/backfill/alter 3연속. 데이터 규모(현재 dev < 100 facade)에서는 초 단위.
- **재측정**: 배포 재개 시 `Started ThirdToolApplication in X seconds` 로그 확인.

### 2. Flyway 마이그레이션 시간

- **V19 3-phase 이론적 O(N)**: 
  - Phase 2-a: `INSERT INTO learning_layer SELECT ...` — 활성 facade 수(N)만큼 row 생성.
  - Phase 2-b: `UPDATE learning_axis la JOIN learning_layer ll ...` — 활성 axis 수(M)만큼 UPDATE.
- **idempotent NOT EXISTS**: V17 재실행 안전. V19는 `WHERE la.learning_layer_id IS NULL`로 재실행 시 no-op.
- **재측정**: 로그 `Successfully applied N migrations to schema "PUBLIC"` 실행 시간.

### 3. GET /facades/me 응답 크기

- **변화**: 응답 JSON에 `concepts: List<String>` + `layers: List<LayerItem(...)>` 추가.
- **예상 회귀**: 사용자당 최대 5 concepts + 5 layers → 응답 크기 ~30% 증가 예상 (기존 axes/topics 대비).
- **N+1 위험**: `layers → axes → topics` 계층 조회. `@OneToMany(fetch=LAZY)`에 fetch join / EntityGraph 대응 필요 관찰 지점.
- **재측정**: k6 시나리오에서 P50/P95 응답 시간 + `hibernate.session.metrics` 활성화 시 SQL 카운트 관찰.

### 4. Layer.softDelete 활성 axis 스캔

- **로직**: `axes.stream().anyMatch(a -> !a.isDeleted())` — 컬렉션 크기(활성 axis 수)에 O(N).
- **부하 예상**: 사용자당 최대 활성 axis 수는 도메인 상 제한적 (~10). 무의미 수준.

### 5. RoleDetector.detect 매칭 오버헤드

- **로직**: 4 role × keyword set · concepts 리스트 이중 루프. `substring` 매칭.
- **최악 케이스**: concepts 5개 × keyword 평균 10개 × 4 role = 200 substring 비교. 마이크로초 수준.
- **재측정**: 사용자 concepts가 role 매칭이 자주 실패해 generic으로 폴백되는 UX 문제가 있으면 오탐 통계 관찰.

---

## 성능 도구 준비 상태 (다음 버전 활용)

| 도구 | 상태 | 활용 계획 |
| --- | --- | --- |
| Actuator `/actuator/metrics` | 미도입 | dev 배포 재개 시 Spring Boot Actuator 등록 |
| Prometheus | dev 환경 가동 (M1 산출) | 배포 재개 후 M2 신규 endpoint 스크레이핑 재개 |
| Grafana | 대시보드 유지 (M1 산출) | 신규 endpoint 대응 대시보드 추가 |
| k6 부하 테스트 (VU 50) | 미실행 (Product 9) | M7 배포 재개 후 첫 실행 · 신규 API baseline |
| JVM `-verbose:gc` / `jstat` | 로컬 언제든 가능 | 로컬 이슈 발생 시 즉시 |
| `./gradlew test --info` | 상시 활용 | 느린 테스트 클래스 발견 |

---

## 트리거 — 다음 버전 성능 재측정 조건

다음 중 최소 1개 성립 시 재측정 착수:

1. **배포 재개 (M7 이후)** — prod-like MySQL 환경에서 V16~V19 실제 실행 시간 baseline
2. **`GET /facades/me` 응답 크기 UX 이슈 부상** — concepts + layers 계층으로 응답이 크다는 사용자 피드백
3. **N+1 SQL 로그 발견** — layer → axes → topics 조회 시 SQL count 급증 관찰
4. **LLM Adapter 도입 (AS Epic 4)** — Static → LLM 교체 시 응답 시간 변동 baseline 필요
5. **k6 부하 테스트 (Product 9)** — VU 50 · 지속 5분 시나리오 실행

---

## 관찰 원칙 (로컬)

- **회귀 여부는 상대값**: 로컬 개발자 하드웨어에 좌우되므로 절대값 baseline 불신. "이전 실행 대비" 비교만 유효.
- **테스트 통과 = 최소 조건**: 성능 회귀는 통과 여부로 감지 안 됨. 별도 관찰 필요.
- **정밀 성능은 배포 이후**: 로컬 성능 실측은 참고용, prod 실측이 진실 소스.

---

## 참고

- k6 부하 테스트 계획: `../../pes/workspectrum/sdd/in-progress/product-load-test.md` (M7 이후)
- Prometheus 지표 정의: `../../pes/workspectrum/sdd/in-progress/product-op.md` (M1 산출)
- 배포 파이프라인: `../../pes/workspectrum/sdd/in-progress/product-infra-deploy.md`
- 상세 배포 재개 조건: `infra.md` §"M3 이후 인프라 재개 조건"

*작성일: 2026-07-02 | 스킵 사유: 로컬 스코프 + rush 정책 | 재측정 트리거 명시 | 배포 재개 시점(M7+)에 정밀 baseline*
