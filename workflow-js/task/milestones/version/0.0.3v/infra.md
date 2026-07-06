# 0.0.3v / Infra — 본 버전 스킵 사유

> **스킵 결정**: 본 버전(0.0.3v)은 M2와 동일한 **로컬 개발 스코프**. 배포·인프라 진척은 본 버전 산출물이 아니다.

---

## 왜 스킵인가

`milestone.md` §"본 버전 제외 사유"에서 배포 라인·성능 baseline은 M7 이관으로 확정됨. M3는 도메인 재편 두 번째 조각(Roadmap/Selection 노드) + AI 첫 응답 실현에 집중.

- **원안 M3 목표 재확인**: 도메인 완주(concepts+Layer M2 유지) + Roadmap/Selection 노드 신설 + AI Static Adapter 6-Port 완주 + AI 첫 응답 도착 (계획 43 SP → 실 착지 ~24.5 SP)
- **배포 재개 판단**: M2와 동일 · M7 이후 유지
- **관측성 배선 (Prometheus/Grafana)**: dev 환경엔 이미 M1에 배선 완료 · 재개는 배포 재개와 함께

## M1 (0.0.1v) 인프라 산출물 참조

M1에서 아래 인프라는 이미 dev 환경 배포까지 완료됨:

- ECS Fargate + GHA OIDC + ECR + Dockerfile (Product 5)
- VPC + ALB + RDS (Product 6)
- Secrets Manager 5종 (Product 7 Epic 1)
- Actuator + Prometheus + Grafana (Product 0-b)
- CORS + OAuth URI thirdtool.dev (Story-056)
- 1차 배포 완성 (도메인 · HTTPS · FE CDN)

상세: `../0.0.1v/infra.md` · `../0.0.2v/infra.md` 참조.

## M3 인프라 상태

- ECS dev 환경 여전히 가동 (M2에서 이관 · M3에서 별도 조치 없음)
- Secrets Manager 5종 유지
- Grafana 대시보드 유지 · M3 신규 지표(예: `thirdtool.suggestion.chapters_outline.requests_total`) 대응 대시보드 미추가
- **배포 이관 없음**: M3 코드(3 Epic PR · 18 신규 엔드포인트)는 배포 대상이 아님 (로컬만)

## 본 버전 인프라 활동 로그 (실측)

- 인프라 리소스 신설: **0건**
- 인프라 리소스 변경: **0건**
- 인프라 리소스 삭제: **0건**
- IAM · 정책 변경: **0건**
- 배포 워크플로우 변경: **0건**
- M3 착지 자산이 배포 라인에 미친 영향: **0건** (로컬 classpath 리소스만 · Static Adapter · Flyway V20~V22는 로컬 H2/dev MySQL 배포 재개 시 자동 적용될 예정)

**3 Epic PR (Roadmap 노드 · Selection 노드 · AI 6-Port) 완주 후에도 인프라 상태 동일** — 배포 재개는 M7 이후.

---

## M4 이후 인프라 재개 조건

다음 조건 중 최소 2개 성립 시 배포 재개 검토 (M2와 동일 정책 유지 + M3 대응 신설 조건):

1. LT Epic 4 (Card→Axis 매핑) 완주 → 도메인 재편의 두 번째 큰 조각 종료
2. AS Epic 3 role catalog 확장 (planner/designer/problem-solver 3종) 완주 → Static Adapter 완성
3. AS Epic 4 LLM Adapter (Vertex AI Gemini Flash 2.5) 물리 배선 시작 → dev 환경 secrets/ADC 필요
4. dev 환경 관측성 지표 활용 요구 발생 (사용자 초대 · 첫 릴리스 0.1.0v 준비)
5. 다음 도메인 재편 (Deck 폐기 · Review 재편) 이 배포 검증 필요 판단

## M3 종료 시 인프라 산출물 검증

- [ ] dev 환경 여전히 가동 확인 (`curl https://api.thirdtool.dev/actuator/health` → 200)
- [ ] M3 코드가 dev 배포됐다면 그 이력 (본 버전 스코프상 미배포 원칙)
- [ ] Secrets Manager 5종 유지 확인 (Vertex AI 관련 신규 없음)
- [ ] Grafana 대시보드에 M3 신규 지표 대응 계획만 문서화 (`performance.md` §다음 버전 실측 대상 참조)

---

## v1 릴리스 (0.1.0v · 2026-08-19) 대비 인프라 조기 알림

M3 종료 시점 릴리스까지 6주 남은 상태. 배포 라인 M7 이관이지만 다음 시점부터 인프라 관측 정도는 조기 준비 필요:

- LLM Adapter 배선 (Vertex AI GCP ADC · Project 설정) → **M6 배선 시작 이전에 GCP 프로젝트 사용자 계정 확인**
- 배포 라인 재개 D-day (v1 릴리스 -2주 시점) → M7 첫 주에 dev 배포 재활성화 · 성능 baseline 실측
- 관측성 (Prometheus scraping · Grafana 대시보드) → M7 재개와 동시에 M3 신규 엔드포인트 반영

---

*작성일: 2026-07-03 | 스킵 사유: 로컬 개발 스코프 · Reviewer 재개했지만 배포 라인 무영향 | 3 Epic PR 완주 후에도 인프라 활동 0건 | 배포 재개 조건 M4~ 명시 | 상세 M1/M2 인프라: `../0.0.1v/infra.md` · `../0.0.2v/infra.md`*
