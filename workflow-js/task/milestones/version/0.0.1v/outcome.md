# 0.0.1v / 성과 (Outcome)

> M1 종료 시점에 **사용자·기능·기술 자산 측면에서 무엇이 새로 가능해졌는가**의 산출물 기록.
> 인프라 산출물은 `infra.md`, 성능 수치는 `performance.md`, 비용 영향은 `cost.md`. 본 파일은 그 외 "결과" 전반.

---

## 본주 머지된 Story (실측)

> D6 종료 시 git log 기반 채움.

| # | Product | Story | PR | 머지 commit | 비고 |
| --- | --- | --- | --- | --- | --- |
| 1 | 0-a 로깅 | 1-1 Logback + LogstashEncoder | #___ | _____ | |
| 2 | 0-a 로깅 | 2-1 MdcLoggingFilter | #___ | _____ | |
| 3 | 0-a 로깅 | 3-1 ExceptionHandler 로그 레벨 | #___ | _____ | |
| 4 | 0-b 메트릭 | 1-1 Actuator + Micrometer | #___ | _____ | |
| 5 | 0-b 메트릭 | 2-1 Prometheus + Grafana 스택 | #___ | _____ | |
| 6 | 3 AI 제안 | 2-1 AxisTopicSuggestionPort + VO | #___ | _____ | |
| 7 | 3 AI 제안 | 2-2 StaticAxisTopicAdapter | #___ | _____ | |
| 8 | 2 User BC | 5-4 UserUpdateRequestDTO 정리 | #___ | _____ | |
| 9 | 5 ECS | 1-1 단일 Dockerfile | #___ | _____ | |
| 10 | 5 ECS | 2-1 GHA OIDC | #___ | _____ | |
| 11 | 5 ECS | 2-2 ECS Task Def + Service | #___ | _____ | |
| 12 | 6 네트워크 | 1-1 VPC + 2 AZ | #___ | _____ | |
| 13 | 6 네트워크 | 1-2 ALB | #___ | _____ | |
| 14 | 6 네트워크 | 2-1 RDS | #___ | _____ | |
| 15 | 7 Secrets | 1-1 Secrets Manager 5종 | #___ | _____ | |
| (Want) 16 | 0-b 메트릭 | 3-1 Grafana 대시보드 | #___ | _____ | |
| (Want) 17 | 5 ECS | 1-2 git_sha 태깅 | #___ | _____ | |
| (Want) 18 | 5 ECS | 3-1 ALB health check | #___ | _____ | |
| (Want) 19 | 7 Secrets | 1-2 Spring Boot 로딩 | #___ | _____ | |

---

## 사용자에게 보이는 변화 (M1 종료 시)

> dev 환경 기준. 본격 베타 사용자는 M3 이후 가정.

| 영역 | M1 전 | M1 후 |
| --- | --- | --- |
| 접근 가능 환경 | 로컬만 | **dev (퍼블릭 URL) + 로컬** |
| 인증 흐름 | 로컬 H2 + 메모리 RT | **MySQL + Secrets Manager로 JWT 비밀 관리** |
| AI 제안 | axis만 (Static) | axis + **axisTopic 양쪽 Static 응답 가능** (Controller 미노출) |
| 로그 | 평문 콘솔 | **JSON + X-Request-Id echo + MDC 컨텍스트** |
| 메트릭 | 없음 | **Prometheus scrape + Grafana 대시보드 (Want)** |

---

## 기술 자산 증가

### 산출 코드
- 새 어댑터: `StaticAxisTopicAdapter`
- 새 필터: `MdcLoggingFilter`
- 새 설정: `LogbackAppender` (LogstashEncoder)
- 새 Bean: `ChatClient` (M2에서 활성) — Actuator endpoint 화이트리스트
- DTO 정리: `UserUpdateRequestDTO`에서 `username` 필드 제거

### 산출 인프라 (코드/문서로 남은 것)
- 통합 `Dockerfile` (multi-stage)
- GitHub Actions workflow (OIDC AssumeRole)
- ECS Task Definition JSON
- Secrets Manager 비밀 명세 (5종, 이름 패턴 `thirdtool/dev/*`)

### 신규 ADR 후보
- ADR011 후보 — 로깅 포맷 표준 (`local`/`dev`/`prod` 톤 분기)
- ADR012 후보 — 비밀 관리 표준 (Secrets Manager 우선 + Boot 로딩 패턴)
- ADR013 후보 — 배포 패턴 (ECS Fargate + GHA OIDC)
- ADR014 후보 — 관측성 토대 (Actuator + Prometheus + Grafana 스택)

> 본 ADR들은 M1 종료 후 `docs/adr/`에 별도 작성 (마일스톤 외 작업).

---

## 도달한 product 상태 변화

| Product | M1 시작 | M1 종료 | 다음 마일스톤 |
| --- | --- | --- | --- |
| 2. User BC | 6/8 완료 | **7/8 완료** (또는 8/8) | Story 4-1 deprecated 확정 → `done/`으로 이동 |
| 3. AI Suggestion | 3/14 | **5/14** | Epic 3 (LLM ChatClient) — M2 |
| 5. ECS Fargate | 0/9 | **3~5/9** | 잔여 Story → M2 |
| 6. AWS 네트워크 | 0/8 | **3/8** | Route53·ACM·보안그룹 → M2 |
| 7. Secrets·Terraform | 0/10 | **1~2/10** | Terraform IaC (Epic 2) → M2 또는 M3 |
| 0-a. 로깅 | 0/9 | **3/9** | Epic 4 (운영 가이드) → M2 |
| 0-b. 메트릭 | 0/7 | **2~3/7** | k6 부하 테스트 (Product 8) → M2 |

---

## 가치 향상 (정성)

- [ ] **운영 가시성**: 처음으로 prod-like 환경 + 메트릭 + 구조화 로그 확보
- [ ] **배포 자신감**: EC2 SSH 의존 폐기 → 키 회전·롤백·복제 가능
- [ ] **보안 자세**: 비밀이 코드/Github Secrets 외부로 이동
- [ ] **AI 제안 확장 기반**: axis + axisTopic 양쪽 Port 정착 → LLM 어댑터 추가만 남음
- [ ] **포트폴리오 자산**: "ECS Fargate / OIDC / IaC 일부 / Grafana 대시보드" 면접 답변 가능

---

## 사용자 시나리오 검증 (선택)

> M1 종료 시점에 dev 환경에서 본인이 직접 시나리오 7종 통과 시도 (선택 — 실패 무방)

- [ ] [01-onboarding] 가입 → 온보딩 → HomePage 도달
- [ ] [02-daily-learning] HomePage → /study → 카드 1장 학습
- [ ] [03-map-construction] /map 진입 → 축 1개 + 주제 1개 추가
- [ ] [04-card-lifecycle] 카드 작성 → Tag 부착 → Archive 보관 → 복귀
- [ ] [05-edge-empty-error] /maintenance + 404 정상 표시
- [ ] [07-me-profile] /me 진입

→ 통과 / 미통과 시나리오는 [`review.md`](./review.md)에 기록
