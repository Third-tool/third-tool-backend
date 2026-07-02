# ADR Index

| # | 제목 | 상태 | 날짜 |
| --- | --- | --- | --- |
| [ADR001](ADR001.md) | Surrogate PK는 BIGINT AUTO_INCREMENT로 통일 | Accepted | 2026-05-07 |
| [ADR002](ADR002.md) | Enum 컬럼은 VARCHAR + CHECK 제약으로 저장한다 | Accepted | 2026-05-07 |
| [ADR003](ADR003.md) | Soft Delete는 사용자 자산성 도메인에만 선택 적용한다 | Accepted | 2026-05-07 |
| [ADR004](ADR004.md) | 학습 주제는 명사구 표현으로 정의한다 — 단일 동사 강제 폐지 | Accepted | 2026-05-07 |
| [ADR005](ADR005.md) | Controller↔Service 경계에 Command/Query record 도입 | Accepted | 2026-05-12 |
| [ADR006](ADR006.md) | 문서 체계를 docs/ 단일 진입으로 압축한다 | Accepted | 2026-05-14 |
| [ADR007](ADR007.md) | BC 간 협력에 동기 도메인 이벤트를 도입한다 | Accepted | 2026-05-14 |
| [ADR008](ADR008.md) | 로깅 인프라 — profile별 Appender 분기 + LogstashEncoder + MDC 화이트리스트 | Accepted | 2026-05-17 |
| [ADR009](ADR009.md) | Access Token은 HttpOnly Cookie, Refresh Token은 React 메모리에 저장한다 | Accepted | 2026-05-27 |
| [ADR010](ADR010.md) | AI 제안 호출 실패는 5xx 미노출 — 빈 목록 + suggestionsAvailable 플래그로 변환 | Accepted | 2026-06-22 |
| [ADR011](ADR011.md) | MDC 화이트리스트에 errorCode 키를 추가한다 (Story 3-1) | Accepted | 2026-06-23 |
| [ADR012](ADR012.md) | 운영 컨테이너 base image `eclipse-temurin:21-jre-alpine` 채택, Distroless 보류 (Story 1-1) | Accepted | 2026-06-25 |
| [ADR013](ADR013-gha-oidc-assume-role.md) | GitHub Actions → AWS 인증을 OIDC AssumeRole로 전환 (Story-046) | Accepted | 2026-06-29 |
| [ADR014](ADR014-ecs-task-iam-role-separation.md) | ECS Task IAM Role 3종 분리 — Execution/Task/GHA Deploy (Story-047) | Accepted | 2026-06-29 |
| [ADR015](ADR015-vpc-3-layer-network-design.md) | VPC 10.0.0.0/16 + 2 AZ × 3-layer subnet + 단일 NAT (Story-048) | Accepted | 2026-06-29 |
| [ADR016](ADR016-alb-listener-target-group.md) | ALB 1개 공유 + Listener 80→443 + Target Group type=ip + TLS 1.3 (Story-049) | Accepted | 2026-06-29 |
| [ADR017](ADR017-spring-forward-headers-graceful-shutdown.md) | Spring `forward-headers-strategy=native` + `server.shutdown=graceful` (Story-050) | Accepted | 2026-06-29 |
| [ADR018](ADR018-rds-mysql-single-az-prod.md) | RDS MySQL prod single-AZ + db.t4g.micro + utf8mb4 + Asia/Seoul (Story-051) | Accepted | 2026-06-29 |
| [ADR019](ADR019-secrets-manager-naming-and-task-role-scope.md) | Secrets Manager 5종 비밀 명명 + env-prefix 격리 + Task Role Resource scope (Story-052) | Accepted | 2026-06-30 |
| [ADR020](ADR020-deck-axis-visibility.md) | Deck↔Axis 가시화는 read-model 노출로 한정, 도메인 연관 승격 거부 (fix-deck-axis-visibility 0.0.2v) | Accepted | 2026-06-30 |
| [ADR021](ADR021-axis-deck-full-integration.md) | Deck 생성은 Axis 이벤트 자동 경로로 유일화 + LearningAxis Soft Delete 승격 (fix-axis-deck-full-integration 0.0.2v) | Accepted | 2026-07-01 |
| [ADR023](ADR023-terminology-roadmap-selections.md) | Roadmap = 헌법(축 순서 청사진), Selection = 판례(구체 사례) — SDD 용어 재정의 (LT Epic 1·2 착지 후) | Accepted | 2026-07-02 |
