# 0.0.2v / Infra — 본 버전 스킵 사유

> **스킵 결정**: 본 버전(0.0.2v)은 **로컬 개발 스코프**로 한정. 배포·인프라 진척은 본 버전 산출물이 아니다.

---

## 왜 스킵인가

`milestone.md` §"0.0.2v 스코프 메모" 재편 결정에 따라 본 버전은 도메인 리팩토링 첫 조각 (concepts[] + Layer) 착지에 집중한다. 배포 라인(Product 6~11)은 상태 유지.

- **원안 M2 목표**: LLM 실제 활성화 + 운영 안정화 + 성능 baseline (42 SP)
- **재편 M2 목표**: 도메인 첫 조각 착지 + 용어 표준 확정 (23 SP)
- **배포 재개 판단**: 도메인 안정화 이후 별도 마일스톤에서 재검토

## M1 (0.0.1v) 인프라 산출물 참조

M1 에서 아래 인프라는 이미 dev 환경 배포까지 완료됨:

- ECS Fargate + GHA OIDC + ECR + Dockerfile (Product 5)
- VPC + ALB + RDS (Product 6)
- Secrets Manager 5종 (Product 7 Epic 1)
- Actuator + Prometheus + Grafana (Product 0-b)
- CORS + OAuth URI thirdtool.dev (Story-056)
- 1차 배포 완성 (도메인 · HTTPS · FE CDN)

상세: `../0.0.1v/infra.md` 참조.

## M2 인프라 상태

- ECS dev 환경 여전히 가동 (별도 조치 없음)
- Secrets Manager 5종 유지
- Grafana 대시보드 유지
- **배포 이관 없음**: 본 버전 코드 변경은 배포 대상이 아님 (로컬만)

## 본 버전 인프라 활동 로그 (실측)

- 인프라 리소스 신설: **0건**
- 인프라 리소스 변경: **0건**
- 인프라 리소스 삭제: **0건**
- IAM · 정책 변경: **0건**
- 배포 워크플로우 변경: **0건**
- Tier 2 AI Static Adapter 도입: **로컬 classpath 리소스만** (외부 인프라 무관)

**Tier 1+2 완주 (0.0.3v 급행 스코프 포함) 후에도 인프라 상태 동일** — 배포 재개는 M7 이후.

---

## M3 (0.0.3v) 이후 인프라 재개 조건

다음 조건 중 최소 2개 성립 시 배포 재개 검토:

1. LT Epic 3 (Roadmap/Selection) 완주 → 도메인 큰 재편 종료
2. AS Epic 2·3 완주 → Static Adapter 완성
3. dev 환경 관측성 지표 활용 요구 발생 (사용자 초대 등)
4. 다음 도메인 재편 (Deck 폐기 · Review 재편) 이 배포 검증 필요 판단

---

## M2 종료 시 인프라 산출물 검증

- [ ] dev 환경 여전히 가동 확인 (`curl https://api.thirdtool.dev/actuator/health` → 200)
- [ ] 본 버전 코드가 dev 배포됐다면 그 이력 (본 버전 스코프상 미배포 원칙)
- [ ] Secrets Manager 5종 유지 확인

---

*작성일: 2026-07-02 | 스킵 사유: 로컬 개발 스코프 · rush 정책 | Tier 1+2 완주 후에도 인프라 활동 0건 | 배포 재개 조건 명시 | 상세 M1 인프라: `../0.0.1v/infra.md`*
