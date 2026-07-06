# ADR 후보 우선순위 (SDD 격상 결과 도출)

> 5개 PES 문서를 SDD 스타일로 격상하는 과정에서 식별된 ADR 신설 / 갱신 후보. 작성일: 2026-06-15.
> 본 파일은 `/workflow/`(gitignored) 영역의 로컬 계획 자료다. 각 항목이 ADR로 정식 발행되는 시점은 해당 Epic / Story 진입 시 — 그때 `docs/adr/ADR{NNN}.md`로 이관한다.
> 우선순위 정의:
> - **P0** — 기존 ADR과 정합성 충돌이 있거나, 진행 중 Story가 ADR 부재로 막힘
> - **P1** — 곧 진입할 Epic이 의존. ADR이 없으면 Story 시점에 즉흥 결정 위험
> - **P2** — 미래 결정. 현재는 Open Question / 잠정 정책으로 충분

---

## P0 — 즉시 조치 (정합성 / 차단)

| # | 제목 | 근거 | 조치 |
| --- | --- | --- | --- |
| P0-1 | ADR004 후속 — AxisTopic `name` 수정 시 커버리지 재초기화 정책 | 본 SDD 격상 작업에서 ADR004와 Epic 3 Story 3-1 충돌 발견 → ADR004에 「후속 보완 (2026-06-15)」 섹션 추가로 1차 해소. Epic 3 진입 시 정식 ADR 분리 필요 | ADR004는 보완 완료. Epic 3 진입 시 별도 ADR 발행 |

## P1 — 진행 중 / 직전 Epic 의존

| # | 제목 | 출처 | 비고 |
| --- | --- | --- | --- |
| P1-1 | Token Storage Strategy (AT HttpOnly Cookie / RT React 메모리) | product-auth 설계 결정 | Product 1 Story 1-3 진입 전 ADR 권장 — 추후 보안 정책 토론 시 기준 문서 |
| P1-2 | Card 상태 모델 (ON_FIELD / ARCHIVE 2값 + ArchiveReason) | product-card Product DoD | Story 1-1 진입 전 |
| P1-3 | maxView vs maxDuration 동시 도달 시 우선순위 (`MAX_VIEW > MAX_DURATION`) | product-card Epic 2 | Story 2-3 진입 시 |
| P1-4 | ReviewSession Layer 1 수집 정책 (starvation 회피) | product-card Epic 6 DoD | Story 6-1 진입 시 |
| P1-5 | Tag 시스템 전역 유니크 + find-or-create | product-card Epic 5 DoD | Story 5-1 진입 시 |
| P1-6 | `CardStalenessQueryPort` BC 간 Outbound Port 패턴 vs ADR007 동기 도메인 이벤트 경계 | product-card Open Q6, product-aisuggestion 설계 결정 | **ADR007과 경합** — 두 BC가 동시 의존하므로 ADR007 보완 또는 신규 ADR 둘 중 결정 필요 |
| P1-7 | LearningModeMappingPolicy 매핑 분기점 및 내림 매핑 원칙 | product-card Epic 4 DoD | Story 4-1 진입 시 |

## P2 — 미래 결정 / Open Question 단계

| # | 제목 | 출처 |
| --- | --- | --- |
| P2-1 | `@ConditionalOnProperty` Bean 분기를 Feature Flag로 사용하는 기준 | product-aisuggestion (후속 BC 재사용 가치) |
| P2-2 | Spring AI 1.0 GA 채택 시 호환성 / 회귀 검증 정책 | product-aisuggestion 외부 의존 |
| P2-3 | AI Interactive Roadmap 인터랙션 도메인 BC 격상 여부 | product-ai-interactive-roadmap Epic 1 |
| P2-4 | Roadmap 초안 vs 최종안 diff / 채택·거부 로그 보존 정책 | product-ai-interactive-roadmap 설계 결정 D |
| P2-5 | Roadmap save-readiness 결정형 단독 책임 (결정형 vs 확률형 경계) | product-ai-interactive-roadmap 설계 결정 C |
| P2-6 | Roadmap 인터랙션 트랜잭션 경계 (Aggregate 직접 호출 모델, ADR007 변형) | product-ai-interactive-roadmap |
| P2-7 | `LearningFacadePersonalizationQuery` inbound 쿼리 인터페이스 패턴 | product-learningFacade Epic 5 — v1.5 BC 협력 표준화 |
| P2-8 | RT 탈취 감지 후 대응 강도 (전 세션 무효화 vs 알림만) | product-auth 열린 질문 |
| P2-9 | 다중 디바이스 RT 분리 정책 | product-auth 열린 질문 |
| P2-10 | CSRF 토큰 명시 도입 시점 | product-auth 열린 질문 |

---

## 2차 격상 (`/workflow/product/pes/` 루트 7개 PES) — 2026-06-15 추가

### P1 — 진행 중 / 직전 Epic 의존 (User BC)

| # | 제목 | 출처 |
| --- | --- | --- |
| P1-8 | `@PreAuthorize` 도입 vs Controller 분기 — 권한 정책 다층화 시점 | Product.md (User BC) Epic 5 |
| P1-9 | `SOCIAL_AUTH_FAILED` ErrorCode 신설 + OAuth Provider 5xx retry-after 정책 | Product.md (User BC) 열린 질문 |

### P1 — 인프라 (deploy / network / log / op)

| # | 제목 | 출처 |
| --- | --- | --- |
| P1-10 | Auto-Rollback Dual Gate (CodeDeploy Circuit Breaker + 5xx 폴링) | product-infra-deploy Epic 3 |
| P1-11 | Branch Strategy (main=prod / develop=staging) | product-infra-deploy Epic 4 |
| P1-12 | ECR Lifecycle Policy (10+7 days, latest 보존) | product-infra-deploy Epic 1 |
| P1-13 | NAT 전략 (Single AZ vs Multi-AZ vs Endpoint-only) | product-infra-network Epic 1 |
| P1-14 | Subnet 3-Layer 분할 (Public / App / Data) 강제 정책 | product-infra-network Epic 1 |
| P1-15 | ALB Listener Rule 환경 분리 (prod·staging 분리 ALB vs Listener Rule 호스트) | product-infra-network Epic 2 |
| P1-16 | HikariCP pool-size 환경별 차등 정책 (local=5 / staging=10 / prod=20) | product-infra-network Epic 3 |
| P1-17 | Flyway out-of-order 환경별 정책 (prod=strict / dev=tolerant) | product-infra-network Epic 3 |
| P1-18 | Tracing Scope — 자체 traceId (MDC UUID) + OTel 표준명 호환 | product-log Epic 2 |
| P1-19 | Exception-to-LogLevel Mapping (ERROR/WARN/INFO 분기 표) | product-log Epic 3 |
| P1-20 | Actuator Security — `permitAll` 화이트리스트 + `denyAll(/actuator/**)` 기본값 | product-op Epic 1 |
| P1-21 | Histogram-based Quantile (`management.metrics.distribution.percentiles-histogram`) | product-op Epic 1 |
| P1-22 | Monitoring Stack Lifecycle — 별도 docker-compose 분리 정책 | product-op Epic 2 |
| P1-23 | Grafana Dashboard as Code (Provisioning + Git 추적) | product-op Epic 3 |

### 정합성 알림 — 코드 vs PES 불일치 (product-log 작업 중 발견)

- **`AccessDeniedException` 로그 레벨**: PES 본문은 INFO로 명시되어 있으나 **현행 코드(`GlobalExceptionHandler:75`)는 `log.warn`**. SoT 원칙(CLAUDE.md "코드가 진실 소스")에 따라 Epic 3 Epic-Level Alternative에서 **WARN 채택**으로 정리. PES 본문 정정은 별도 hygiene 작업 필요.
- **패키지명**: 일부 PES가 `com.thirdtool`로 적힘. 실제 코드는 `com.example.thirdtool`. Epic-Level Alternative 메모로만 정정.
- **`management.endpoints.web.exposure.include: []`**: 현 `application.yml`에 빈 배열로 설정되어 있어 PES "actuator 설정이 전혀 없다" 진술과 미세 불일치. 환경별 설정 분기 섹션에서 실측값 반영.

---

## 정합성 이슈 — 사용자 결정이 남은 항목

(SDD 격상 결과 발견. P0-1 외에는 Open Question으로 PES 본문에 명시되어 있어 결정 시점이 도래하면 검토)

1. ✅ **ADR004 vs Epic 3 Story 3-1** — P0-1로 해소
2. ⏳ **CardStalenessQuery Port 위치** (Card BC inbound vs aisuggestion BC outbound) — P1-6로 등재
3. ⏳ **인터랙션 도메인 BC 격상 여부** — P2-3로 등재
4. ⏳ **Tag 삭제 정책** (운영 후 dead Tag cleanup) — product-card Open Question 5
5. ⏳ **Epic 8 문구 번들 위치** (FE bundle vs ErrorCode 메시지) — product-card Open Question 추가

---

## 후속 액션 제안

1. **즉시**: P0-1 외에 추가 정합성 충돌 없음 — Story 진행에 차단 없음
2. **다음 Story 진입 전**: P1 7건 중 해당 Story가 의존하는 항목부터 ADR 작성
3. **분기별 점검**: P2 항목을 Story 진행에 따라 P1로 승격 또는 Open Question 유지 판단
