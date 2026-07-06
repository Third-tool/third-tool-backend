# M4 / 0.0.4v — Performance (성능 관찰)

> **본 파일의 역할**: M4 스코프는 **정밀 성능 측정 미수행**. 배포 라인이 아직 M7 이관 상태이고, Vertex AI LLM 실호출도 M6 이관이며, k6 부하 테스트도 v1 이후 이관. 대신 **관찰 가능한 정성 지표**와 **재편 후 잠재 성능 영향 지점**을 기록해 M5 이후 재측정 트리거로 활용한다.

**작성 시점**: 2026-07-21 (D7) · M4 동결 판정 시점.

---

## 왜 스킵인가

1. **배포 라인 없음** — M4는 로컬 스코프 유지 · 프로덕션 지연·처리량·에러율 실측 불가 (M7 이관)
2. **LLM 미배선** — Vertex AI 실호출 없음 · 응답 latency 측정 무의미 (Static Adapter는 in-memory · <10ms 상수)
3. **속도 우선 정책** — M4는 Card BC 재편 + Card→Axis 마이그레이션 + role catalog 3종 3축 착지가 우선 · 성능 튜닝은 사용자 실측 이후 (릴리스 이후 · v0.1.1v~)
4. **로컬 하드웨어 의존** — 개발자 로컬 머신 사양이 지표에 영향 · 프로덕션 정합 판정 불가

---

## 대신 확인한 정성 지표 (M4 로컬 실측)

| 항목 | M3 실측 | M4 실측 | 변화 |
| --- | --- | --- | --- |
| `./gradlew test` 전체 시간 | ~3분 20초 | **~3분 45초** (+13%) | Card BC 테스트 재작성 + `CardHybridScenarioTest` 신설로 증가. 허용 범위 |
| 각 PR별 `test` 시간 | 45~55초 | 50~70초 | PR#2 (CARD-E2 · OnFieldBudget 폐기 후 재작성) 가 가장 오래 걸림 |
| `./gradlew compileJava` | 15~45초 | 20~50초 | createdMode·axisId 필드 추가 · CoverageRecalculator 재작성 반영 |
| `@DataJpaTest` Slice | 3~5초 | 4~6초 | Flyway V23~V30 8건 착지로 초기화 시간 소폭 증가 |
| Reviewer 5관점 병렬 세션 | 40~90초 (PR당) | 45~95초 (PR당) | 5 PR 총 4~7분 · M3 3 PR 대비 5회 발사 · 병렬 유지 |
| Static Adapter 응답 시간 | 수십 ms | 수십 ms | catalog 3종 추가에도 응답 시간 상수 유지 (JSON 파일 크기 유의미하게 증가하지 않음) |
| 로컬 부팅 시간 (`./gradlew bootRun`) | 8~10초 | 9~11초 | Flyway 8건 착지 · 유의미한 저하 없음 |

**정성적 관찰**:
- Card BC 재편 후 `CardTest`가 상당량 재작성됐지만 전체 테스트 시간은 M3 대비 +13% 수준. 허용 범위 (400줄+ 코드 변경 감안).
- role catalog 3종 추가 (planner + designer + problem-solver JSON) → 총 catalog 크기 ~4배 증가. `SuggestionCatalogLoader` 부팅 시 캐시 로딩 오버헤드 관찰 가능하지만 유의미한 저하 없음 (수십 ms 유지).
- Flyway 8건 (M3 3건 대비 +166%) 착지에도 로컬 부팅 시간 증가 미미. 3-phase 마이그레이션 각 단계가 작은 SQL이므로 부담 없음.

---

## 관찰 가능한 성능 영향 지점 (M4 재편 후 · M5+ 관찰 대상)

1. **`Card.createdMode` 하이브리드 계산** — `effectiveMaxDays(userCurrentMode)`가 매 카드 view 시점에 호출. 유저의 mode 변경 시 대량 카드 재판정 필요할 수 있음. **N 카드에 대해 N번 호출**되므로 대량 사용자 시 최적화 필요 가능성. v1은 3명 사용자 · 문제 없음. v2에 실측 후 결정.

2. **Coverage 재계산 축 스코프** — `CoverageRecalculator.recalculateByAxis(axisId)`가 매 카드 view · archive · returnToField 시 트리거. Axis 하나에 N 카드가 있을 때 O(N) 카드 상태 조회. Layer coverage summary는 이의 상위 집계 (M5 LT E6 S6-4). **인덱스**: `card(axis_id, status)` 커버링 인덱스 · `card.axis_id` 단독 인덱스가 V30에 신설됐지만 (`status` 조인) 실측 필요.

3. **Flyway V29 백필 JOIN** — `card ← axis_topic ← learning_axis` JOIN. 프로덕션 대량 카드 상태 · 백필 시간 관찰 대상. **M7 프로덕션 첫 착지 시** 실측. 로컬에서는 카드 소량이라 참고값 미미.

4. **role catalog JSON 로딩 · in-memory 캐시** — `SuggestionCatalogLoader`가 부팅 시 4개 role JSON 파일을 파싱해 in-memory 캐시. catalog 재로딩 없음 (부팅 후 정적). 캐시 hit ratio 100%. 프로덕션 부팅 시간에 미미 영향.

5. **Reviewer 5관점 세션 병렬 발사** — M4는 5 PR × 5관점 = 25 reviewer 발사. M3 15건 대비 +67%. 각 세션 40~90초 병렬 · 총 소요는 병렬 유지 시 M3와 유사. 발사 후 종합 시간은 사용자 판단 시간이 더 큼. **개발자 대기 시간**이 실제 병목. Sceptical Reviewer가 반복 판정 시 이전 판정 문서화(eval.md)로 시간 단축 여지.

---

## 성능 도구 준비 상태 (M7 이후 배선)

| 도구 | M4 상태 | 배선 시점 |
| --- | --- | --- |
| Actuator (`/actuator/health`·`/prometheus`) | dev 배선 · 프로덕션 미배선 | M7 (INFRA-DEPLOY + OP-BASELINE) |
| Prometheus scrape | dev docker-compose · 프로덕션 미배선 | M7 (OP E1·E2) |
| Grafana dashboard | dev docker-compose · 프로덕션 미배선 | M7 (OP E3 baseline) |
| k6 부하 테스트 | 미배선 | v1 이후 (사용자 확대 시점) |
| JVM 메트릭 (heap · GC · 스레드) | dev 로컬만 · 로그 없음 | M7 (Micrometer 배선) |
| Hibernate stats (N+1 감지) | 미배선 | M7 (SQL 로깅 추가 시) |

**M4 시점 조치**: `application-dev.yml`에 `spring.jpa.properties.hibernate.generate_statistics=true` 활성 상태 · SlowQuery 로깅 (>100ms) 활성. 실측 데이터 축적 없음 (배포 없음).

---

## 트리거 (M5 이후 성능 재측정 조건)

1. **M7 배포 완주** — 프로덕션 URL 접속 · Grafana baseline 실측 · P95 latency · JVM · DB pool 첫 측정 (`../0.0.7v/performance.md` 신설 예정 · 첫 실질 콘텐츠)
2. **응답 크기 UX 이슈 발견** — role catalog 3종 응답이 사용자에게 크게 느껴진다는 피드백 발생 시 · Response DTO 최적화 검토
3. **N+1 SQL 로그 발견** — Card→Axis 재편 후 `CardResponse` 생성 시 axis 정보 조회에 N+1 발생 가능성 · M5 로컬 관찰 필요
4. **M6 LLM Adapter 도입** — Vertex AI Gemini Flash 2.5 latency 실측 · Cascade 폴백 판정 시 timeout 임계값 튜닝
5. **k6 부하 테스트 · v0.1.1v 이후** — 3명 사용자 확대 후 유의미한 부하 관찰 시 착수
6. **Reviewer 세션이 병목** — 5 PR × 5관점 병렬 발사가 개발 대기의 주요 병목. M5 4 PR · M6 4 PR · M7 7 PR에서 발사 시간 관찰 · 사용자 판단 시간 단축 방안 (요약 자동화 · 이전 판정 참조 문서화)

---

## 관찰 원칙 (M4~M7 로컬 스코프)

- **상대값만 유효** — 개발자 로컬 머신 사양 의존 · 절대값은 프로덕션 정합 불가
- **테스트 pass 시간을 baseline으로** — `./gradlew test` 전체 시간 · 각 PR별 시간 · 재편 후 증감 관찰
- **정밀 측정은 배포 이후** — M7 프로덕션 URL 도달 시점부터 Grafana · CloudWatch · k6 활용
- **Static Adapter 응답 시간 무의미** — LLM 배선 (M6) 이전 · in-memory 조회이므로 <10ms 상수. LLM 도입 후 3~7초 latency와 비교 baseline
- **Reviewer 세션 시간은 개발 프로세스 지표** — 성능 지표는 아니지만 개발 속도 측면에서 관찰 대상

---

## 참고

- 상위 계획: `./milestone.md`
- 성과: `./outcome.md` (기술 자산 증가)
- 회고: `./review.md` (계획 vs 실제 · Reviewer 세션 시간 관찰)
- 인프라: `./infra.md` (성능 도구 준비 상태 · M7 배선 예정)
- 이전 마일스톤: `../0.0.3v/performance.md` (M3 정성 지표 · 동일 스킵 사유)
- 다음 마일스톤: `../0.0.5v/milestone.md` (Review 신설 · 여전히 로컬 스코프)
- **M7 프로덕션 baseline**: `../0.0.7v/performance.md` (신설 예정 · Grafana 실측 첫 콘텐츠)
- 릴리스 로드맵: `../../release/version/0.0.1v/release.md`
