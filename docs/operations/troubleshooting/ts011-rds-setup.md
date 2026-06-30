# ts011: RDS MySQL prod single-AZ 1회성 셋업·검증·트러블슈팅

Story-051로 `infra/rds/`에 MySQL 8.0 prod 인스턴스 + DB Subnet Group + Parameter Group spec JSON이 추가됐다. 본 가이드는 AWS 콘솔(또는 CLI)에서 1회성 셋업하는 절차 + 검증 + 자주 보는 에러 6건을 한 곳에 모았다.

ts009(VPC)·ts008(ECS)·ts010(ALB)·ts012(Secrets Manager, 후속 Story)와 cross-link. 1인 운영자가 1회만 실행.

---

## 1. 전제

- **VPC 셋업 완료** (ts009 §1-8) — VPC ID + data-2a/data-2c subnet ID + db-sg ID 발행
- AWS 콘솔 RDS 관리 권한 (`rds:*`, `kms:DescribeKey`, `logs:CreateLogGroup`, `iam:CreateServiceLinkedRole` — Performance Insights/Enhanced Monitoring용)
- AWS CLI 권장
- **결제**: 다음 비용 즉시 발생
  - RDS 인스턴스 db.t4g.micro = $0.019/시간 = $0.45/일
  - storage 비용 $0.115/GB·월 × 20 = $2.30/월
  - Performance Insights 무료 (7일 retention, AWS 2026-06 docs 기준)
  - Enhanced Monitoring 60s — CloudWatch metric 비용 미미
  - CloudWatch Logs ingestion $0.50/GB + storage $0.03/GB·월 (M1 추정 < $0.50/월 — slow query + error log 합쳐)
  - 자동 백업 — `backupRetentionPeriod` 일수만큼 storage 비용 추가 ($0.095/GB·월 × 20 × 7/30 ≈ $0.44/월 평균)

> **단일 운영자 안전망**: `deletionProtection: true` 명시 — 콘솔에서 실수로 instance 삭제 시도 시 거부. 영구 삭제는 명시적으로 `deletion_protection=false` 변경 후 재시도 필요.

---

## 2. Parameter Group 생성

```bash
aws rds create-db-parameter-group \
  --db-parameter-group-name thirdtool-mysql8-utf8mb4-kst \
  --db-parameter-group-family mysql8.0 \
  --description "ThirdTool MySQL 8.0 — utf8mb4 + Asia/Seoul + slow query" \
  --region ap-northeast-2

# 7 parameters 적용
aws rds modify-db-parameter-group \
  --db-parameter-group-name thirdtool-mysql8-utf8mb4-kst \
  --parameters \
    'ParameterName=character_set_server,ParameterValue=utf8mb4,ApplyMethod=pending-reboot' \
    'ParameterName=collation_server,ParameterValue=utf8mb4_0900_ai_ci,ApplyMethod=pending-reboot' \
    'ParameterName=time_zone,ParameterValue=Asia/Seoul,ApplyMethod=immediate' \
    'ParameterName=slow_query_log,ParameterValue=1,ApplyMethod=immediate' \
    'ParameterName=long_query_time,ParameterValue=1.0,ApplyMethod=immediate' \
    'ParameterName=log_output,ParameterValue=FILE,ApplyMethod=immediate' \
    'ParameterName=max_connections,ParameterValue=100,ApplyMethod=immediate' \
  --region ap-northeast-2
```

> **`pending-reboot` parameter 적용 시점**: `character_set_server`/`collation_server`는 instance reboot 시점에 적용. §5에서 instance 생성 후 첫 reboot 시 적용됨. `time_zone` 등은 즉시 적용.

---

## 3. DB Subnet Group 생성

ts009 §8 출력의 `DATA_2A`/`DATA_2C` subnet ID 필요:

```bash
DATA_2A=subnet-xxxxxxxx  # ts009 §8 출력
DATA_2C=subnet-yyyyyyyy

aws rds create-db-subnet-group \
  --db-subnet-group-name thirdtool-data-subnet-group \
  --db-subnet-group-description "ThirdTool RDS data subnet group" \
  --subnet-ids $DATA_2A $DATA_2C \
  --tags Key=Project,Value=ThirdTool Key=ManagedBy,Value=Story-051 \
  --region ap-northeast-2
```

> **RDS는 최소 2 AZ subnet 필수** — single-AZ instance여도 group에는 2개 등록. 1개만 등록 시 `InsufficientDBSubnetGroupCoverage` 에러 (ts011-1).

---

## 3.5 (CLI 운영자 1회성) Enhanced Monitoring IAM Role 사전 생성

§5에서 `--monitoring-role-arn`을 사용하려면 본 Role이 사전 존재해야 한다. AWS Console에서 RDS 첫 생성 시 자동 생성되지만, CLI 운영자는 다음 명령 1회 실행 필수 (ts011-2 트러블슈팅으로 빠지기 전 사전 차단):

```bash
cat > /tmp/rds-monitoring-trust.json <<'EOF'
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Service": "monitoring.rds.amazonaws.com" },
    "Action": "sts:AssumeRole"
  }]
}
EOF

aws iam create-role --role-name rds-monitoring-role \
  --assume-role-policy-document file:///tmp/rds-monitoring-trust.json

aws iam attach-role-policy --role-name rds-monitoring-role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole

# 1-2분 IAM 전파 대기 권장
```

기존 Role 존재 확인:
```bash
aws iam get-role --role-name rds-monitoring-role 2>/dev/null | jq '.Role.Arn'
# 출력 있으면 skip
```

---

## 4. 초기 master password 생성

운영자가 안전한 임의 password 생성 — 본 password는 Story-052 Secrets Manager에 `DB_PASSWORD`로 등록. RDS 생성 후 즉시 Secrets Manager로 옮기고 운영자 local에서 폐기.

```bash
# 28자 random password
INITIAL_PASSWORD=$(openssl rand -base64 24 | tr -d '/+=' | head -c 28)
echo "INITIAL_PASSWORD=$INITIAL_PASSWORD"
# 이 값을 안전한 곳에 임시 저장 (1Password vault 등). RDS 생성 후 Story-052에서 Secrets Manager에 등록
```

> **MySQL master password 제약**: 8~41자, 영문/숫자/`! # $ % & ' ( ) * + , - . / : ; < = > ? @ [ ] ^ _ \` { | } ~`만 가능. `/`, `"`, `@` 등 일부 special char는 JDBC URL에 들어가면 escaping 필요 — 위 명령은 `/`, `+`, `=` 제거.

> **JDBC URL escaping 회피 보강**: 본 프로젝트는 password를 application-prod.yml `${DB_PASSWORD}` 환경변수로 주입 → JDBC URL에 직접 들어가지 않음 (Hikari가 별도 property로 처리). 따라서 위 password가 `@`, `/` 포함해도 무해. 다만 운영자가 수동으로 `mysql -h ... -p` 명령에 password 입력 시 shell escaping은 별개 — single-quote로 감싸기 `mysql -h $DB_ENDPOINT -p'$INITIAL_PASSWORD'`.

---

## 5. RDS instance 생성

ts009 §8 출력의 `DB_SG` 필요:

```bash
DB_SG=sg-xxxxxxxx  # ts009 §8 출력

aws rds create-db-instance \
  --db-instance-identifier thirdtool-prod-db \
  --engine mysql \
  --engine-version 8.0.39 \
  --db-instance-class db.t4g.micro \
  --allocated-storage 20 \
  --max-allocated-storage 50 \
  --storage-type gp3 \
  --storage-encrypted \
  --kms-key-id alias/aws/rds \
  --no-multi-az \
  --availability-zone ap-northeast-2a \
  --db-subnet-group-name thirdtool-data-subnet-group \
  --vpc-security-group-ids $DB_SG \
  --no-publicly-accessible \
  --port 3306 \
  --master-username thirdtool_admin \
  --master-user-password "$INITIAL_PASSWORD" \
  --db-name thirdtool \
  --db-parameter-group-name thirdtool-mysql8-utf8mb4-kst \
  --backup-retention-period 7 \
  --preferred-backup-window 17:00-18:00 \
  --preferred-maintenance-window Sun:18:00-Sun:19:00 \
  --deletion-protection \
  --no-delete-automated-backups \
  --enable-performance-insights \
  --performance-insights-retention-period 7 \
  --monitoring-interval 60 \
  --monitoring-role-arn "arn:aws:iam::<AWS_ACCOUNT_ID>:role/rds-monitoring-role" \
  --enable-cloudwatch-logs-exports error slowquery \
  --copy-tags-to-snapshot \
  --auto-minor-version-upgrade \
  --tags Key=Project,Value=ThirdTool Key=Environment,Value=prod Key=ManagedBy,Value=Story-051 \
  --region ap-northeast-2
```

> **Enhanced Monitoring IAM Role**: `monitoring-interval > 0` 사용 시 `rds-monitoring-role`이 AWS에 존재해야 함. 미존재 시 AWS Console에서 RDS 첫 생성 시 자동 생성됨. CLI 사용 시 사전 생성 필요 — `aws iam create-role` + AmazonRDSEnhancedMonitoringRole 정책 부착. ts011-2 참조.

---

## 6. instance available 대기 (5-10분)

```bash
aws rds wait db-instance-available --db-instance-identifier thirdtool-prod-db --region ap-northeast-2

# 또는 polling
aws rds describe-db-instances --db-instance-identifier thirdtool-prod-db --region ap-northeast-2 \
  | jq '.DBInstances[0] | {Status: .DBInstanceStatus, Endpoint: .Endpoint.Address}'
# 기대: Status="available", Endpoint="thirdtool-prod-db.xxx.ap-northeast-2.rds.amazonaws.com"
```

`pending-reboot` parameter 적용을 위해 첫 reboot:
```bash
aws rds reboot-db-instance --db-instance-identifier thirdtool-prod-db --region ap-northeast-2
aws rds wait db-instance-available --db-instance-identifier thirdtool-prod-db --region ap-northeast-2
```

---

## 7. 검증

### 7.1 ARN/endpoint 출력 (후속 Story placeholder 치환용)

```bash
DB_ENDPOINT=$(aws rds describe-db-instances --db-instance-identifier thirdtool-prod-db --region ap-northeast-2 \
  | jq -r '.DBInstances[0].Endpoint.Address')
DB_PORT=$(aws rds describe-db-instances --db-instance-identifier thirdtool-prod-db --region ap-northeast-2 \
  | jq -r '.DBInstances[0].Endpoint.Port')

echo "DB_ENDPOINT=$DB_ENDPOINT"   # → Story-052 Secrets Manager 'thirdtool/prod/db:HOST' 값
echo "DB_PORT=$DB_PORT"           # → 3306 (확인)
echo "DB_NAME=thirdtool"           # → Story-052 'thirdtool/prod/db:NAME' 값
echo "DB_USERNAME=thirdtool_admin" # → Story-052 'thirdtool/prod/db:USERNAME' 값
echo "DB_PASSWORD=$INITIAL_PASSWORD"  # → Story-052 'thirdtool/prod/db:PASSWORD' 값
```

### 7.2 backup + Performance Insights 확인

```bash
aws rds describe-db-instances --db-instance-identifier thirdtool-prod-db --region ap-northeast-2 \
  | jq '.DBInstances[0] | {
      BackupRetentionPeriod,
      PerformanceInsightsEnabled,
      MultiAZ,
      DeletionProtection,
      StorageEncrypted,
      AutoMinorVersionUpgrade
    }'
# 기대:
# BackupRetentionPeriod=7
# PerformanceInsightsEnabled=true
# MultiAZ=false (M1 — Multi-AZ는 M2)
# DeletionProtection=true
# StorageEncrypted=true
# AutoMinorVersionUpgrade=true
```

### 7.3 parameter group 적용 확인

```bash
aws rds describe-db-parameters --db-parameter-group-name thirdtool-mysql8-utf8mb4-kst --region ap-northeast-2 \
  | jq '.Parameters[] | select(.ParameterName | IN("character_set_server","time_zone","max_connections","slow_query_log")) | {ParameterName, ParameterValue, ApplyMethod}'
# 7 parameters 모두 ParameterValue가 정의된 값으로 보임. character_set_server는 reboot 후 적용
```

### 7.4 ECS Exec 또는 Bastion에서 연결 검증 (선택)

ECS Task가 실제 부팅된 후(Story-052 Secrets Manager 등록 + ECS Service 등록 완료 시점):

```bash
# ECS Task 진입 후
mysql -h $DB_ENDPOINT -P 3306 -u thirdtool_admin -p
# password 입력 후
# SHOW VARIABLES LIKE 'character_set%';
# 기대: character_set_server = utf8mb4 (reboot 적용 후)
# SHOW VARIABLES LIKE 'time_zone';
# 기대: Asia/Seoul
```

---

## 8. Flyway 마이그레이션 검증 (선택)

RDS endpoint가 application-prod.yml `${DB_HOST}` 환경변수로 주입되면 Spring Boot 부팅 시 Flyway가 V1~V13 자동 실행.

ECS Task 부팅 후 CloudWatch Logs `/ecs/thirdtool-prod` 최신 stream에서 다음 라인 확인:
```
Flyway Community Edition X.X.X by Redgate
Successfully validated 13 migrations (execution time XX.XXXms)
Schema history table `thirdtool`.`flyway_schema_history` does not exist yet
Successfully baselined schema with version: 1
Migrating schema `thirdtool` to version "1 - init"
...
Successfully applied 13 migrations to schema `thirdtool`, now at version v13
```

`flyway_schema_history` 테이블이 V1 적용 후 자동 생성. `baseline-on-migrate: true` (application-prod.yml line 34) 정합.

---

## 9. 트러블슈팅

### ts011-1: `InsufficientDBSubnetGroupCoverage`

**원인**: DB Subnet Group에 단일 AZ subnet만 등록. RDS는 single-AZ instance여도 최소 2 AZ subnet 필수.

**해결**:
```bash
aws rds describe-db-subnet-groups --db-subnet-group-name thirdtool-data-subnet-group --region ap-northeast-2 \
  | jq '.DBSubnetGroups[0].Subnets[] | {SubnetIdentifier, AvailabilityZone}'
# data-2a (ap-northeast-2a) + data-2c (ap-northeast-2c) 2건 확인. 1건만이면 §3 재실행
```

### ts011-2: `InvalidParameterValue: monitoring-role-arn role not found`

**원인**: Enhanced Monitoring IAM Role(`rds-monitoring-role`) 부재. CLI로 생성 시 사전 생성 필요.

**해결**:
```bash
# trust policy
cat > /tmp/rds-monitoring-trust.json <<'EOF'
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Service": "monitoring.rds.amazonaws.com" },
    "Action": "sts:AssumeRole"
  }]
}
EOF

aws iam create-role --role-name rds-monitoring-role \
  --assume-role-policy-document file:///tmp/rds-monitoring-trust.json

aws iam attach-role-policy --role-name rds-monitoring-role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole

# 1-2분 IAM 전파 대기 후 §5 RDS 생성 재시도
```

### ts011-3: `pending-reboot` parameter 미적용

**원인**: `character_set_server`, `collation_server` 등 일부 parameter는 instance reboot 후 적용. instance 생성 직후 `SHOW VARIABLES LIKE 'character_set%'`이 기본값(latin1) 반환 가능.

**해결**:
```bash
aws rds reboot-db-instance --db-instance-identifier thirdtool-prod-db --region ap-northeast-2
aws rds wait db-instance-available --db-instance-identifier thirdtool-prod-db --region ap-northeast-2

# reboot 후 다시 확인
# MySQL 진입 후: SHOW VARIABLES LIKE 'character_set_server';
# 기대: utf8mb4
```

### ts011-4: Flyway baseline 첫 부팅 실패

**원인**: V1 파일명 오타 `V1__init.sql.sql` (이중 `.sql`) — Spring Boot 환경에서 Flyway가 파일명 인식. 마이그레이션 디렉토리 통상 `V1__init.sql`이지만 본 프로젝트는 `.sql.sql` 명시 (의도된 historical 잔재 가능성).

**해결**:
1. `src/main/resources/db/migration/V1__init.sql.sql` 파일명 확인
2. Flyway 기본 `sqlMigrationSuffixes: .sql`이 `.sql.sql`도 매치하는지 — Flyway는 마지막 `.sql` 확장자만 검사하므로 정상 매치
3. 부팅 실패 시 CloudWatch Logs에 `Validate failed: Detected resolved migration not applied to database` 메시지 — application.yml `flyway.baseline-on-migrate: true` 명시되어 있어 schema가 비어있으면 V1부터 적용. ECS Task 새로 부팅 후 재시도

### ts011-5: Hikari `Connection is not available, request timed out after 30000ms`

**원인**: max_connections 부족 또는 Hikari pool 설정 불일치.

**해결**:
1. `SHOW STATUS LIKE 'Threads_connected';` — 현재 연결 수
2. `SHOW VARIABLES LIKE 'max_connections';` — 100 확인
3. Hikari pool size(application-prod.yml line 14: `maximum-pool-size: 10`) × ECS Task 수(desiredCount=2) = 20 in-use. 100 max는 5배 여유 — 정상 운영 충분
4. 만약 부족하면 ECS Service 임시 desiredCount=1로 축소 + parameter group `max_connections=200` modify + reboot

### ts011-6: deletion_protection 활성으로 instance 삭제 차단

**원인**: 의도된 안전망. 운영자가 instance 영구 삭제하려면 명시적으로 비활성 필요.

**해결**:
```bash
# 1. deletionProtection 비활성
aws rds modify-db-instance --db-instance-identifier thirdtool-prod-db \
  --no-deletion-protection --apply-immediately --region ap-northeast-2

# 2. 삭제 (최종 snapshot 생성 권장)
aws rds delete-db-instance --db-instance-identifier thirdtool-prod-db \
  --final-db-snapshot-identifier thirdtool-prod-db-final-$(date +%Y%m%d) \
  --region ap-northeast-2
```

---

## 10. 후속 (별도 Story)

- **Story-TBD(Secrets Manager 등록)**: milestone item #15. RDS endpoint + master credentials를 Secrets Manager `thirdtool/prod/db` 시크릿에 등록. Task Def `<SUFFIX>` placeholder 해소
- **Story-TBD(Multi-AZ 활성)**: M2 트래픽 발생 후 — `aws rds modify-db-instance --multi-az --apply-immediately`. data-2c subnet으로 standby 자동 생성 + failover 검증
- **Story-TBD(read replica)**: M2-M3 트래픽 분산
- **Story-TBD(slow query CloudWatch alarm)**: 1초 초과 쿼리 빈도 임계 도달 시 알람 — 관측성 Story
- **Story-TBD(Performance Insights 23개월 retention)**: 비용 검토 ($X/instance/월)
- **Story-TBD(Aurora 전환)**: M3 호환성 + 비용 분석 후
- **Story-TBD(RDS Proxy)**: connection pooling 트래픽 임계 도달 시
- **Story-TBD(staging RDS)**: 환경 분리 Epic — 별도 db.t4g.micro single-AZ + 백업 1일

---

## 11. 관련

- [`ts009-vpc-setup.md`](ts009-vpc-setup.md) — db-sg + data-2a/2c subnet 발행 (선행)
- [`ts008-ecs-cluster-setup.md`](ts008-ecs-cluster-setup.md) — ECS Task가 RDS endpoint 사용
- [ADR018](../../adr/ADR018-rds-mysql-single-az-prod.md) — single-AZ + db.t4g.micro + utf8mb4 결정 배경
- `infra/rds/db-instance-spec-prod.json` · `db-subnet-group-spec.json` · `db-parameter-group-spec.json` — 단일 진실 소스
- `src/main/resources/application-prod.yml` line 8-18 — datasource + Hikari + Flyway 설정
- `src/main/resources/db/migration/V1*.sql ~ V13*.sql` — 13 마이그레이션
- Story-051 — milestone 0.0.1v item #14
- AWS handoff 통합: `workflow/task/pes/handoff/aws-setup-0.0.1v.md` Stage 6 (gitignored)
