# ThirdTool 모니터링 스택

Spring Boot Actuator(`/actuator/prometheus`)를 Prometheus가 10초 간격으로 스크랩하고, Grafana가 시각화하는 로컬 개발용 모니터링 스택입니다 (Story 2-1, Product 0-b 메트릭 Epic 2).

## 구성

```
monitoring/
├── docker-compose.monitoring.yml      # Prometheus + Grafana 컨테이너 정의
├── .env.monitoring.example            # GRAFANA_ADMIN_PASSWORD 예시 (실제 .env.monitoring은 .gitignore)
├── prometheus/
│   └── prometheus.yml                 # scrape config (thirdtool job, 10s 간격)
└── grafana/
    ├── provisioning/
    │   ├── datasources/datasource.yml # Prometheus를 default Datasource로 자동 등록
    │   └── dashboards/dashboard.yml   # /var/lib/grafana/dashboards 자동 로드 provider
    └── dashboards/                    # Epic 3 Story 3-1에서 thirdtool.json 추가 예정
```

## 실행 절차

### 1. Spring Boot 앱 실행 (별도 터미널)

ThirdTool 앱이 `:8080`에서 떠 있어야 Prometheus가 스크랩 대상을 찾는다.

```bash
./gradlew bootRun
```

`application.yml`에 actuator 노출 설정 필수 — `docs/operations/troubleshooting/ts001-actuator-prometheus-application-yml.md` 가이드 따라 갱신.

### 2. 환경변수 파일 생성

```bash
cp monitoring/.env.monitoring.example monitoring/.env.monitoring
# 에디터로 GRAFANA_ADMIN_PASSWORD 변경 (기본 'change-me-...' 그대로 두지 말 것)
```

### 3. 모니터링 스택 기동

```bash
docker compose --env-file monitoring/.env.monitoring \
               -f monitoring/docker-compose.monitoring.yml up -d
```

### 4. 접속 확인

| URL | 용도 | 로그인 |
| --- | --- | --- |
| http://localhost:9090 | Prometheus UI | 없음 |
| http://localhost:9090/targets | scrape 대상 상태 확인 (`thirdtool` job UP 여부) | 없음 |
| http://localhost:3000 | Grafana UI | `admin` / `.env.monitoring`의 비밀번호 |

Grafana 로그인 후 `Configuration → Data sources`에서 **Prometheus**가 default로 등록되어 있어야 한다.

### 5. 중단·재시작

```bash
# 중단 (시계열·대시보드 설정은 named volume에 유지)
docker compose -f monitoring/docker-compose.monitoring.yml down

# 다시 기동 — 데이터 복원
docker compose --env-file monitoring/.env.monitoring \
               -f monitoring/docker-compose.monitoring.yml up -d

# 데이터까지 완전 초기화
docker compose -f monitoring/docker-compose.monitoring.yml down -v
```

## 환경별 주의

- **Mac / Windows**: `host.docker.internal:8080`이 호스트의 Spring Boot로 자동 해석된다.
- **Linux**: `docker-compose.monitoring.yml`에 `extra_hosts: ["host.docker.internal:host-gateway"]`를 명시해 동일 동작을 보장. 그래도 안 되면 `network_mode: host` 또는 호스트 IP 직접 지정 — `docs/operations/troubleshooting/ts004-monitoring-stack-runtime.md` 참고.

## 검증

```bash
# 1. Prometheus가 thirdtool 메트릭 수집 중인지 query
curl -s 'http://localhost:9090/api/v1/query?query=up{job="thirdtool"}' | jq

# 2. Grafana Datasource 자동 등록 확인
curl -s -u admin:<PASSWORD> http://localhost:3000/api/datasources | jq '.[].name'

# 3. 재시작 후 데이터 유지
docker compose -f monitoring/docker-compose.monitoring.yml restart
sleep 5
curl -s 'http://localhost:9090/api/v1/query?query=up{job="thirdtool"}' | jq
```

## 후속

- Epic 3 Story 3-1 — Grafana 대시보드 4 섹션(API/JVM/DB/상태) + `thirdtool.json` 커밋
- Product 8 — k6 부하 테스트의 baseline 측정 도구로 본 스택 활용
- 운영 환경 도입 시 — 별도 `prometheus-staging.yml`/`prometheus-prod.yml`로 분리 또는 환경변수 치환 (현재는 local 한정)
