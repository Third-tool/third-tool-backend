# ts004: 모니터링 스택 (Prometheus + Grafana) 기동·환경별 이슈

Story 2-1로 추가된 `monitoring/docker-compose.monitoring.yml` 실행 시 자주 마주치는 케이스 정리.

## ts004-1: Prometheus `targets` 화면에서 `thirdtool` job이 DOWN

### 증상
http://localhost:9090/targets 접속 시 `thirdtool` job이 `DOWN` + Error에 `connection refused` 또는 `dial tcp ...: i/o timeout`.

### 원인 분기

| 원인 | 확인 | 해결 |
| --- | --- | --- |
| Spring Boot 앱이 미실행 | `curl localhost:8080/actuator/prometheus` 응답 여부 | 별도 터미널에서 `./gradlew bootRun` |
| `application.yml`의 actuator 노출 미설정 | `curl localhost:8080/actuator/prometheus` 404 응답 | ts001 가이드 따라 `management.endpoints.web.exposure.include`에 `prometheus` 추가 |
| Linux에서 `host.docker.internal` 미해석 | Prometheus 컨테이너 안 `curl host.docker.internal:8080` 실패 | 본 문서 ts004-2 참고 |
| 8080 포트 다른 프로세스 점유 | `lsof -i :8080` 또는 `netstat -ano | findstr 8080` | 다른 포트로 부팅하거나 충돌 프로세스 종료 |

## ts004-2: Linux에서 `host.docker.internal` 해석 실패

### 증상
Mac/Windows에선 정상이나 Linux 호스트에서 Prometheus 컨테이너가 `host.docker.internal:8080`을 못 찾음.

### 원인
Docker Desktop(Mac/Windows)은 기본으로 `host.docker.internal`을 호스트 IP로 자동 매핑한다. 그러나 순수 Docker Engine(Linux)은 이 기능이 없다.

### 해결 — 본 PR 적용 사항

`docker-compose.monitoring.yml`에 이미 `extra_hosts: ["host.docker.internal:host-gateway"]`가 명시되어 있다. Docker 20.10+ 에서 `host-gateway` 키워드가 자동으로 호스트 게이트웨이 IP를 매핑.

여전히 실패 시 대안:

```yaml
# 옵션 A — network_mode: host (Linux 전용. Mac/Windows 호환 X)
services:
  prometheus:
    network_mode: host
    # ports: 매핑 제거 (host 네트워크엔 무효)
```

또는 호스트 IP 직접 지정:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: thirdtool
    static_configs:
      - targets: ["172.17.0.1:8080"]   # Linux Docker bridge gateway IP
```

호스트 IP 확인: `ip addr show docker0` → inet 값.

## ts004-3: `.env.monitoring` 누락 시 부팅 실패

### 증상
```
WARN[0000] The "GRAFANA_ADMIN_PASSWORD" variable is not set. Defaulting to a blank string.
...
service "grafana" can't be started: ...
```

### 해결
```bash
cp monitoring/.env.monitoring.example monitoring/.env.monitoring
# 에디터로 GRAFANA_ADMIN_PASSWORD 변경
docker compose --env-file monitoring/.env.monitoring \
               -f monitoring/docker-compose.monitoring.yml up -d
```

`--env-file` 플래그 누락도 동일 증상. 명령 그대로 복사.

## ts004-4: 재시작 후 Grafana 로그인 비밀번호 변경 안 됨

### 증상
`.env.monitoring`의 `GRAFANA_ADMIN_PASSWORD`를 변경 후 `docker compose up -d`해도 이전 비밀번호로만 로그인됨.

### 원인
Grafana는 첫 부팅 시에만 `GF_SECURITY_ADMIN_PASSWORD` 환경변수를 읽어 admin 비밀번호를 설정한다. 이후엔 named volume(`grafana-data`)의 sqlite DB 값이 우선.

### 해결
- 일회성 변경: Grafana UI(Configuration → Users → admin → Change password)
- 강제 초기화: `docker compose down -v` 후 다시 up — 시계열·대시보드 모두 삭제됨

## ts004-5: Prometheus 시계열이 컨테이너 재시작 후 사라짐

### 증상
`docker compose down && up`을 했더니 1주일 추이 데이터가 모두 사라짐.

### 원인
`-v` 플래그 또는 `volumes:` 정의 누락으로 named volume이 제거됨.

### 확인
```bash
docker volume ls | grep -E "prometheus-data|grafana-data"
# 둘 다 존재해야 정상
```

### 해결
- `-v` 플래그 없이 `down`만 사용 — `docker compose -f monitoring/docker-compose.monitoring.yml down`
- 의도적 초기화만 `down -v` 사용

## 관련

- Story 2-1 (Product 0-b 메트릭 Epic 2) — Prometheus + Grafana docker-compose
- ts001 — Story 4-1 `/actuator/prometheus` 노출을 위한 application.yml 갱신
- 후속 Epic 3 Story 3-1 — Grafana 대시보드 4 섹션 + `thirdtool.json`
