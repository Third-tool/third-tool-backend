# 0.0.1v / Performance 향상

> M1 종료 시점에 dev 환경의 응답 성능 / 자원 사용 / 처리 안정성을 첫 baseline으로 기록.
> 본격 부하 테스트(`product-load-test.md` k6)는 M2 진입 시점에 진행 — 본 파일은 **smoke test 수준 + Prometheus 메트릭 단순 관찰**.

---

## 측정 도구

- **Actuator** (Product 0-b 1-1) — `/actuator/prometheus` endpoint
- **Prometheus** (Product 0-b 2-1) — scrape interval: 15s
- **Grafana** (Product 0-b 3-1, Want) — 4섹션 대시보드
- **수기 curl + 브라우저 DevTools** — k6 도입 전 임시
- (M2에서 추가) k6 — VU 50 부하 패턴

---

## baseline 측정 대상 (M1 — smoke 수준)

수동 호출 시 한 번에 측정 가능한 단순 응답 시간을 본주 종료 시 1회 기록.

| 엔드포인트 | 측정 항목 | M1 측정값 | 참고 임계 (P95) |
| --- | --- | --- | --- |
| `GET /health` | latency | _____ ms | — |
| `GET /user` (인증 후) | latency | _____ ms | — |
| `POST /login` | latency | _____ ms | < 500 ms |
| `GET /api/v1/learning-facade` | latency | _____ ms | < 500 ms |
| `GET /api/v1/review-session/today` | latency | _____ ms | **< 500 ms (baseline)** |
| `POST /api/v1/decks/{deckId}/cards` | latency | _____ ms | < 500 ms |
| `PATCH /api/v1/reviews/{sessionId}/next` | latency | _____ ms | < 500 ms |
| `GET /actuator/prometheus` | latency | _____ ms | < 200 ms |

> P95 임계는 `product-load-test.md` 명세 기준. M1에서는 P50만 확인 가능 — P95는 M2 k6에서.

---

## 자원 사용 baseline (Prometheus 메트릭)

본주 종료 시점에 Grafana 또는 `/actuator/prometheus` curl로 수집.

| 메트릭 | M1 측정값 | 비고 |
| --- | --- | --- |
| `jvm_memory_used_bytes{area="heap"}` | _____ MB | 부팅 직후 + 1시간 후 |
| `jvm_threads_live_threads` | _____ | 평상시 |
| `process_cpu_usage` | _____ % | 평상시 |
| `hikaricp_connections{state="active"}` | _____ | 평상시 |
| `hikaricp_connections{state="idle"}` | _____ | 평상시 |
| `http_server_requests_seconds_count` | _____ | 총 요청 수 |
| `http_server_requests_seconds_max{uri=...}` | _____ | 가장 느린 엔드포인트 |

---

## 안정성 baseline

| 항목 | M1 측정값 |
| --- | --- |
| Task 부팅 시간 (시작 → ALB healthy) | _____ 초 |
| 부팅 직후 OOM 발생 여부 | (예/아니오) |
| 24시간 가동 중 Task 재시작 횟수 | _____ |
| 5xx 누적 (24시간) | _____ |
| logback 로그 라인 / 분 (평상시) | _____ |
| `/actuator/prometheus` scrape 성공률 | _____ % |

---

## 발견된 병목 / 의외점

> D6 종료 시점에 채움.

| 병목 | 증상 | 가설 | M2 액션 후보 |
| --- | --- | --- | --- |
| (예시) `GET /today` 느림 | P50 800ms | Layer 1 필터 풀스캔 | k6 → 인덱스 확인 |
| | | | |

---

## 성능 향상 측면 — 본주 작업이 만든 변화

| 작업 | 성능 영향 |
| --- | --- |
| MdcLoggingFilter 도입 | 요청당 ~0.X ms 추가 (MDC put/clear 비용) — 측정값: _____ |
| Logback JSON 포맷 전환 | 로그 라인당 ~N% 크기 증가 — CloudWatch 비용 영향 |
| Static AxisTopicAdapter | LLM 호출 없음 — 0 ms 추가 |
| GlobalExceptionHandler 로그 레벨 분리 | 노이즈 감소 → 디버깅 시간 단축 (정성) |

---

## M2 부하 테스트 (k6) 준비 사항

- 본주 측정한 baseline이 M2 k6 시나리오의 기대값
- k6 시나리오 4개 (login·cards·review·schedule) → `product-load-test.md` Story 1-2
- VU 50 단계적 부하 → Story 1-3
- 본주 메트릭 위에서 직접 측정 가능 — 추가 인프라 0
