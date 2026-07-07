# Decisions — ADR Index (Living)

> **성격**: living-docs — 항상 최신본. `docs/adr/`의 ADR 원본 인덱스 + 한 줄 결정 요약.
> **원본 진입**: `docs/adr/index.md` (같은 표) + 각 `docs/adr/ADR{NNN}.md` (배경·대안·트레이드오프·재검토 시점).
> **성장 방향**: ADR이 많아지면 카테고리별 파일로 분화 (`decisions/data.md`, `decisions/infra.md` …).
> **신규 ADR 작성 트리거**: `.claude/rules/adr.md` 참조.

---

## 카테고리별 결정

### 데이터 · 지속성 (5건)
| # | 제목 | 결정 요약 | 링크 |
| --- | --- | --- | --- |
| ADR001 | Surrogate PK | 모든 테이블 PK = `BIGINT AUTO_INCREMENT`. UUID/ULID 미채택. 단일 RDS·샤딩 계획 없음 | [ADR001](../../../docs/adr/ADR001.md) |
| ADR002 | Enum 저장 | `VARCHAR + CHECK 제약` + `@Enumerated(EnumType.STRING)`. ORDINAL 금지 | [ADR002](../../../docs/adr/ADR002.md) |
| ADR003 | Soft Delete 범위 | 사용자 자산성 도메인에만 선택 적용 (Card/Deck/Facade/Material/Axis). 매핑·이력 엔티티는 hard delete | [ADR003](../../../docs/adr/ADR003.md) |
| ADR018 | RDS MySQL prod | Single-AZ + `db.t4g.micro` + utf8mb4 + Asia/Seoul (Story-051) | [ADR018](../../../docs/adr/ADR018-rds-mysql-single-az-prod.md) |
| ADR021 | LearningAxis Soft Delete 승격 | Deck 생성 유일화 (Axis 이벤트) + LearningAxis softDelete로 승격. `orphanRemoval=false`로 회귀 트랩 차단 | [ADR021](../../../docs/adr/ADR021-axis-deck-full-integration.md) |

### 도메인 규칙 (3건)
| # | 제목 | 결정 요약 | 링크 |
| --- | --- | --- | --- |
| ADR004 | AxisTopic 명사구 표현 | 이전 AxisAction 단일 동사 강제 폐지. 명사구 자유 형식으로 정의 | [ADR004](../../../docs/adr/ADR004.md) |
| ADR020 | Deck↔Axis 가시화 | read-model 노출로 한정. 도메인 연관 승격은 거부 (fix-deck-axis-visibility) | [ADR020](../../../docs/adr/ADR020-deck-axis-visibility.md) |
| ADR023 | Roadmap/Selection 용어 재정의 | Roadmap = 헌법(축 순서 청사진), Selection = 판례(구체 사례) | [ADR023](../../../docs/adr/ADR023-terminology-roadmap-selections.md) |

### API · Application 경계 (3건)
| # | 제목 | 결정 요약 | 링크 |
| --- | --- | --- | --- |
| ADR005 | Command/Query record | Controller↔Service 경계에 record 도입. Service public 메서드는 record 단일 인자만 | [ADR005](../../../docs/adr/ADR005.md) |
| ADR007 | BC 간 협력 = 동기 이벤트 | `ApplicationEventPublisher` + `@EventListener` 동기 처리. `@Async`/`@TransactionalEventListener` 금지 | [ADR007](../../../docs/adr/ADR007.md) |
| ADR010 | AI 호출 실패 처리 | 5xx 미노출. 상위 Service가 catch → 빈 목록 + `suggestionsAvailable=false` 변환 | [ADR010](../../../docs/adr/ADR010.md) |

### 인증 · 보안 (1건)
| # | 제목 | 결정 요약 | 링크 |
| --- | --- | --- | --- |
| ADR009 | Access/Refresh Token 분리 저장 | Access = HttpOnly Cookie, Refresh = React 메모리 | [ADR009](../../../docs/adr/ADR009.md) |

### 관측성 · 로깅 (2건)
| # | 제목 | 결정 요약 | 링크 |
| --- | --- | --- | --- |
| ADR008 | 로깅 인프라 | profile별 Appender 분기 + LogstashEncoder + MDC 화이트리스트 | [ADR008](../../../docs/adr/ADR008.md) |
| ADR011 | MDC에 errorCode 추가 | MDC 화이트리스트에 errorCode 키 추가 (Story 3-1) | [ADR011](../../../docs/adr/ADR011.md) |

### 인프라 · 배포 (7건)
| # | 제목 | 결정 요약 | 링크 |
| --- | --- | --- | --- |
| ADR012 | 컨테이너 base image | `eclipse-temurin:21-jre-alpine`. Distroless는 보류 (Story 1-1) | [ADR012](../../../docs/adr/ADR012.md) |
| ADR013 | GHA → AWS OIDC | Long-lived access key 폐기. AssumeRole 신뢰정책 sub 조건으로 main branch 잠금 (Story-046) | [ADR013](../../../docs/adr/ADR013-gha-oidc-assume-role.md) |
| ADR014 | ECS Task IAM Role 3종 분리 | Execution / Task / GHA Deploy Role 분리 (Story-047) | [ADR014](../../../docs/adr/ADR014-ecs-task-iam-role-separation.md) |
| ADR015 | VPC 네트워크 설계 | 10.0.0.0/16 + 2 AZ × 3-layer subnet + 단일 NAT (Story-048) | [ADR015](../../../docs/adr/ADR015-vpc-3-layer-network-design.md) |
| ADR016 | ALB · Target Group | ALB 공유 + Listener 80→443 + Target Group type=ip + TLS 1.3 (Story-049) | [ADR016](../../../docs/adr/ADR016-alb-listener-target-group.md) |
| ADR017 | Spring 프록시 헤더 · graceful shutdown | `forward-headers-strategy=native` + `server.shutdown=graceful` 30s (Story-050) | [ADR017](../../../docs/adr/ADR017-spring-forward-headers-graceful-shutdown.md) |
| ADR019 | Secrets Manager 명명 · 스코프 | 5종 비밀 명명 규칙 + env-prefix 격리 + Task Role Resource scope (Story-052) | [ADR019](../../../docs/adr/ADR019-secrets-manager-naming-and-task-role-scope.md) |

### 문서 · 프로세스 (1건)
| # | 제목 | 결정 요약 | 링크 |
| --- | --- | --- | --- |
| ADR006 | 문서 체계 단일 진입 | `docs/` 단일 진입으로 압축. private-docs/·update-docs/ 폐기 | [ADR006](../../../docs/adr/ADR006.md) |

---

## 시간순 로그 (참조용)

| 날짜 | ADR | 무엇을 결정했나 |
| --- | --- | --- |
| 2026-07-02 | ADR023 | Roadmap/Selection 용어 재정의 |
| 2026-07-01 | ADR021 | Deck 자동 생성 유일화 + Axis softDelete 승격 |
| 2026-06-30 | ADR019, ADR020 | Secrets Manager 명명 · Deck↔Axis 가시화 |
| 2026-06-29 | ADR013~ADR018 | GHA OIDC · ECS IAM · VPC · ALB · Spring 셋팅 · RDS |
| 2026-06-25 | ADR012 | 컨테이너 base image |
| 2026-06-23 | ADR011 | MDC errorCode |
| 2026-06-22 | ADR010 | AI 호출 실패 처리 |
| 2026-05-27 | ADR009 | Access/Refresh Token 저장 |
| 2026-05-17 | ADR008 | 로깅 인프라 |
| 2026-05-14 | ADR006, ADR007 | 문서 체계 · 동기 도메인 이벤트 |
| 2026-05-12 | ADR005 | Command/Query record |
| 2026-05-07 | ADR001~ADR004 | PK · Enum · Soft Delete · AxisTopic 명사구 |

---

## 결번 · 이슈

- **ADR022 결번** — 번호 스킵. 재사용 여부 미결정 (신규 ADR은 ADR024부터).
- **v4 도메인 결정 12건** — DOMAIN.md §3에 남아있는 결정 중 ADR로 승격되지 않은 항목. 필요 시 개별 ADR로 분리 가능.

---

## 신규 ADR 작성 트리거

`.claude/rules/adr.md`에 상세. 요약:
- **의존 방향·BC 경계 변경** — topology 재pin 동반 시.
- **DB 스키마 방식 결정** — Soft Delete 범위 확장, PK 전략 변경, Enum 저장 방식 등.
- **외부 통합 방식** — 새 인증 제공자, 새 AI provider, 새 인프라 도구.
- **롤백 불가능한 마이그레이션** — 3-phase 이관, 컬럼 SUPERSEDED 등.
- **의견 갈리는 트레이드오프** — 대안 비교와 거부 사유가 코드에 남지 않는 경우.

---

## 참조

- 원본: `docs/adr/index.md` + `docs/adr/ADR{NNN}.md`
- 작성 트리거: `.claude/rules/adr.md`
- 관련 living-docs:
  - `architecture-system-design/architecture.md` — 결정이 코드에 어떻게 반영됐는지
  - `boundary-trace/boundary.md` — ADR007 이벤트 실체
  - `infra-map/infra.md` — ADR013~ADR019 인프라 결정 실체

*최신 갱신: 2026-07-03 · ADR001~ADR023 (ADR022 결번) 22건 반영*
