# ADR018: RDS MySQL prod single-AZ + db.t4g.micro + utf8mb4 (Story-051)

- **상태**: Accepted
- **날짜**: 2026-06-29
- **관련**: Product 6 AWS 네트워크 Epic 3 (RDS MySQL) · milestone 0.0.1v item #14 (`workflow/task/milestones/version/0.0.1v/milestone.md`) · `docs/operations/troubleshooting/ts011-rds-setup.md` · `infra/rds/db-instance-spec-prod.json` · `infra/rds/db-subnet-group-spec.json` · `infra/rds/db-parameter-group-spec.json` · ADR015(VPC) · ADR016(ALB) · ADR017(Spring forward-headers)

## 컨텍스트

Story-048(VPC) + Story-047(ECS Task Def secrets DB 4종) + Story-049(ALB) 후 다음 의존 단계는 **실제 데이터 저장소** — RDS MySQL prod 인스턴스. 본 Story가 만들어지면:

- Story-047 `task-definition-prod.json`의 `DB_HOST` placeholder 해소 (Story-052 Secrets Manager 등록 경유)
- Story-048 `db-sg` (app-sg → 3306) 활용
- Story-048 `data-2a`/`data-2c` subnet 활용 (DB Subnet Group 등록)
- application-prod.yml의 `${DB_HOST}:${DB_PORT:3306}/${DB_NAME}` JDBC URL 실제 endpoint로 도달
- Flyway V1~V13 자동 실행 → 본 프로젝트 도메인 schema 완성

추가 결정 영역:
- **Multi-AZ vs single-AZ**: product Epic 3 명세는 prod Multi-AZ + staging single-AZ. milestone item #14는 "prod single-AZ (Multi-AZ는 M2)" 명시 — milestone deviation
- **db.t4g.micro vs t3.medium**: product 명세 t3.medium / milestone cost.md baseline db.t4g.micro
- **utf8mb4 + Asia/Seoul**: application-prod.yml JDBC URL `characterEncoding=UTF-8` + `serverTimezone=Asia/Seoul` 정합
- **Performance Insights, Enhanced Monitoring, slow query log** 활성 여부
- **deletion_protection**, **autoMinorVersionUpgrade**

## 결정

### prod single-AZ + db.t4g.micro

| 항목 | 결정 | 근거 |
| --- | --- | --- |
| 엔진 | MySQL 8.0.39 | application-prod.yml `org.hibernate.dialect.MySQLDialect` + Flyway V1~V13 정합. 8.0.39는 2026-06 시점 LTS |
| 인스턴스 클래스 | `db.t4g.micro` (1 vCPU / 1 GB / ARM Graviton) | cost.md baseline $0.45/일 정합. M1 트래픽 0명에 t3.medium은 5배 과도 |
| Multi-AZ | **false (single-AZ)** | milestone item #14 명시 deviation. M2 multi-AZ는 modify-db-instance로 무중단 활성 가능 |
| AZ | `ap-northeast-2a` | data-2a subnet 배치 |
| 스토리지 | 20 GB gp3 + max 50 GB autoscale | M1 schema(V13) + 시드 데이터 < 1 GB. autoscale로 트래픽 증가 시 자동 확장 |
| 스토리지 암호화 | true (KMS `alias/aws/rds`) | AWS 관리형 default. 운영 시 KMS 비용 없음 |
| 백업 retention | 7일 + automated daily 17:00-18:00 KST | RTO 7일 (PITR 1초 단위). M1 트래픽 0 단계라 충분 |
| Maintenance window | Sun 18:00-19:00 KST | 한국 새벽 시간 회피 |
| deletion_protection | true | 단일 운영자 실수 삭제 차단 안전망 |
| autoMinorVersionUpgrade | true | 8.0.39 → 8.0.40+ 자동 보안 패치. Maintenance window 내 적용 |
| Performance Insights | enabled, 7일 retention | 무료 retention. M1 슬로우 쿼리 패턴 학습 |
| Enhanced Monitoring | 60s interval | $0.025/GB/월 (CloudWatch Logs) — 비용 미미 |
| CloudWatch Logs export | error + slowquery | N+1 / 인덱스 누락 조기 발견 |

### Parameter Group (`thirdtool-mysql8-utf8mb4-kst`)

| Parameter | Value | 적용 시점 | 근거 |
| --- | --- | --- | --- |
| `character_set_server` | utf8mb4 | pending-reboot | JDBC URL `characterEncoding=UTF-8` 정합. emoji + 한글 multi-byte 정확 |
| `collation_server` | utf8mb4_0900_ai_ci | pending-reboot | MySQL 8.0 default + accent/case insensitive 한글 정렬 |
| `time_zone` | Asia/Seoul | immediate | JDBC URL `serverTimezone=Asia/Seoul` + logback + Docker TZ 정합 |
| `slow_query_log` | 1 | immediate | 1초 초과 쿼리 로깅 활성 |
| `long_query_time` | 1.0 | immediate | OLTP CRUD ms 단위라 1초 초과는 비정상 |
| `log_output` | FILE | immediate | CloudWatch Logs export 가능 (TABLE은 mysql.slow_log 부하) |
| `max_connections` | 100 | immediate | Hikari pool 10 × ECS Task 2 = 20 in-use + 여유. db.t4g.micro default ~66 → 100 |

### DB Subnet Group (`thirdtool-data-subnet-group`)

- `data-2a` + `data-2c` 2 subnet 등록 — **RDS는 single-AZ instance여도 최소 2 AZ subnet 필수**
- M2 Multi-AZ 활성 시 data-2c가 자동 standby AZ

### 본 ADR이 다루지 않는 범위

- **staging RDS**: 환경 분리 Epic. milestone item #14는 prod only
- **read replica**: M2-M3 트래픽 분산
- **Aurora 전환**: M3 호환성 + 비용 분석 후
- **RDS Proxy**: connection pooling 임계 도달 시
- **자동 페일오버 검증**: Multi-AZ 전환과 함께 (single-AZ는 페일오버 자체 부재)
- **Secrets Manager 등록**: milestone item #15 (Story-052)에서 endpoint + credentials Secrets Manager 등록
- **slow query CloudWatch alarm**: 별도 관측성 Story
- **Performance Insights 23개월 retention**: M2 비용 검토

## 결과 (Consequences)

### 긍정적

- **cost baseline 정합**: $0.45/일 (db.t4g.micro single-AZ). cost.md M1 추정과 정확 매칭
- **Hikari pool ↔ max_connections 충분 여유**: 20 in-use × 5배 = 100 max → 트래픽 spike에도 안정
- **Flyway V1~V13 그대로 동작**: 본 ADR이 application 측 변경 0건. Spring Boot 부팅 시 자동 마이그레이션
- **deletion_protection로 실수 삭제 차단**: 단일 운영자 안전망. 명시적 비활성 후만 삭제 가능
- **storage encryption 기본 활성**: KMS default. 비용 없이 보안 baseline
- **slow query → CloudWatch Logs export**: N+1 / 인덱스 누락 조기 발견 채널
- **autoMinorVersionUpgrade로 보안 패치 자동**: Maintenance window 내 적용. 운영자 개입 불요

### 트레이드오프 / 부정적

- **single-AZ AZ 장애 시 ~15분 다운타임**: AWS RDS single-AZ는 instance fail-over 자동 안 됨. M1 트래픽 0 수용. M2 Multi-AZ로 전환 시 modify-db-instance로 무중단 활성 가능
- **db.t4g.micro 1 vCPU/1 GB 메모리**: staging 부하 테스트 baseline 측정 시 부족 가능. staging RDS는 별도 인스턴스로 분리 (현재 부재 — 환경 분리 Epic)
- **product Epic 3 명세(prod Multi-AZ) deviation**: milestone item #14가 우선. product 명세는 본 ADR로 override — product-infra-network.md 명세 갱신 별도 필요
- **`pending-reboot` parameter 초기 적용 미흡**: instance 생성 후 첫 reboot 시점에 적용. ts011 §6에 명시
- **MySQL master password JDBC escaping 위험**: `/`, `+`, `=`, `"`, `@` 등 special char가 password에 들어가면 JDBC URL escaping 복잡. ts011 §4에 escaping 회피 char set 명시
- **Performance Insights 7일 무료 → 23개월 유료 전환 가능성**: M2 트래픽 분석 깊이 필요 시 비용 발생

## 대안 비교

| 대안 | 장점 | 거부 사유 |
| --- | --- | --- |
| **A. prod Multi-AZ + db.t4g.micro (product 명세)** | AZ 장애 시 자동 페일오버 | $0.90/일 (single-AZ의 2배). M1 트래픽 0에 과도 |
| **B (선택). prod single-AZ + db.t4g.micro** | cost.md 정합, 충분한 max_connections | AZ 장애 ~15분 다운타임 — M1 수용 |
| **C. db.t3.medium (product 명세)** | 4 vCPU / 4 GB — 부하 여유 | $1.40/일. M1 트래픽 0에 과도. 인스턴스 클래스 modify는 short downtime 동반 |
| **D. db.t3.small** | 2 vCPU / 2 GB — 중간 옵션 | t4g(ARM)이 가성비 우수. ARM 호환 검증 완료(MySQL official) |
| **E. Aurora MySQL Serverless v2** | 0.5-1 ACU 자동 스케일, 무중단 Multi-AZ | ACU 시간 비용($0.18/시간) 항상 발생. 최소 0.5 ACU 강제로 db.t4g.micro 대비 2배+ |
| **F. Aurora MySQL Provisioned** | 더 빠른 페일오버 + 자동 백업 | 시간 비용 $0.073/시간 = $1.75/일 (4배). M1 과도 |
| **G. RDS Proxy 도입** | connection pooling 안정성 | 추가 시간 비용 ($0.015/시간 × ACU) + connection limit 동일. Hikari pool로 충분 |
| **H. utf8 character set (legacy)** | storage 절감 | 한글 4-byte char + emoji 미지원. utf8mb4 필수 |

## 알려진 follow-up (본 ADR 범위 외)

- **Story-TBD(Secrets Manager 등록)**: milestone item #15 — DB endpoint + credentials Secrets Manager `thirdtool/prod/db` 등록. Task Def `<SUFFIX>` placeholder 해소
- **Story-TBD(Multi-AZ 활성)**: M2 트래픽 발생 후 — `aws rds modify-db-instance --multi-az --apply-immediately`. 무중단 활성. data-2c standby 자동 생성 + 페일오버 검증
- **Story-TBD(read replica)**: M2-M3 트래픽 분산 + 분석 쿼리 분리
- **Story-TBD(slow query CloudWatch alarm)**: 1초 초과 쿼리 빈도 임계 도달 시 알람 — 별도 관측성 Story
- **Story-TBD(Performance Insights 23개월 retention)**: 비용 검토 후 활성. 6개월 후 트래픽 패턴 분석에 가치 발생 시
- **Story-TBD(staging RDS)**: 환경 분리 Epic — 별도 db.t4g.micro single-AZ + 백업 1일
- **Story-TBD(Aurora 전환)**: M3 호환성 + 비용 분석 후
- **Story-TBD(RDS Proxy)**: connection pooling 임계 도달 시 (Hikari pool이 RDS max_connections 초과 시점)
- **Story-TBD(product-infra-network.md Epic 3 명세 갱신)**: prod Multi-AZ → "prod single-AZ M1 deviation, M2 Multi-AZ 전환" 명시. ADR015 패턴 답습

## 다시 검토할 시점

- **트래픽 발생 시점**: Multi-AZ 활성 + read replica 검토
- **max_connections 100 도달 시점**: ECS Service desiredCount 증가 또는 Hikari pool size 증가. RDS Proxy 검토
- **storage 80% 도달 시점**: gp3 max 50 GB autoscale → 추가 확장 또는 archive 전략
- **utf8mb4 → utf8mb4_0900_bin 변경 필요 시점**: case-sensitive 정렬 필요 도메인 도입 시 (현재 axis topic 이름 등은 case-insensitive 정합)
- **Performance Insights 7일 부족 시점**: 6개월 후 슬로우 쿼리 트렌드 분석 필요 시 retention 확장
- **Aurora 비용 합리화 시점**: 트래픽 100+ RPS 도달 + 다중 read replica 필요 시 Aurora 검토
