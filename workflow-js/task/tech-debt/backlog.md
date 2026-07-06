# tech-debt / backlog

> "알고 있지만 이번엔 안 하기로 한 것"을 모으는 곳.
> Story DoD의 Out of Scope, milestone의 Tier 2 컷, reviewer 지적 중 수용 보류 항목이 여기로 온다.
> 다음 milestone 계획 시 이 목록을 읽고 Tier 2 후보로 올릴지 결정한다.

---

## 사용법

- **추가 시점**: Story 작업 완료 직후 "이번엔 안 함" 항목 발견 즉시
- **추가 시점**: milestone Tier 2 컷 시 해당 Story들을 여기로 이동
- **추가 시점**: Reviewer 세션에서 "Minor / Nit 수용 보류" 결정 시
- **처리 시점**: milestone 계획 전 이 목록 스캔 → Tier 2 후보 발굴

---

## 컬럼 설명

| 컬럼 | 설명 |
| --- | --- |
| ID | `TD-{NNN}` 순번 |
| 항목 | 무엇을 안 했는가 (1줄) |
| 발견 버전 | 어느 milestone/Story에서 발견했는가 |
| 영향 | 안 하면 어떤 문제가 생기는가 (Low / Medium / High) |
| 처리 예정 M | 어느 milestone에서 처리할지 예상 (없으면 `미정`) |
| 상태 | `대기` / `M{N} Tier2 편입` / `완료` / `폐기` |
| 비고 | 관련 파일, reviewer 지적 출처 등 |

---

## 백로그

| ID | 항목 | 발견 버전 | 영향 | 처리 예정 M | 상태 | 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| TD-001 | RDS Multi-AZ 전환 (현재 single-AZ) | 0.0.1v | Medium | M3 이후 | 대기 | product-infra-network.md 6 2-1 비고 |
| TD-002 | ALB Circuit Breaker + 자동 롤백 (5 3-2·3-3) | 0.0.1v | Medium | M2 | 대기 | milestone 0.0.1v Tier2 컷 |
| TD-003 | ECR 라이프사이클 정책 (이미지 보존 N개 룰) | 0.0.1v | Low | M2 | 대기 | product-infra-deploy.md 5 1-3 |
| TD-004 | Terraform IaC 모듈화 (현재 콘솔 수동) | 0.0.1v | High | M2~M3 | 대기 | product-infra-ops.md Epic 2 전체 |
| TD-005 | NAT Gateway → VPC Endpoint 비용 회피 검토 | 0.0.1v | Medium | 미정 | 대기 | review.md 신규 후보 |

> 새 항목은 위 표 **최하단**에 추가. ID는 순번대로.

---

## 처리 완료 / 폐기 아카이브

완료되거나 폐기된 항목은 아래로 이동 (표 위에서 삭제하지 말 것 — 이력 보존).

| ID | 항목 | 발견 버전 | 처리 버전 | 처리 방식 |
| --- | --- | --- | --- | --- |
| — | — | — | — | — |
