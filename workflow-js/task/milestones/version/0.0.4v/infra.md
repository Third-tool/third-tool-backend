# M4 / 0.0.4v — Infra (인프라 활동)

> **본 파일의 역할**: M4 스코프는 **인프라 배선 활동 미수행**. AWS 리소스 신규 생성·수정·삭제 0건 · IAM·Secrets Manager 변경 없음 · GHA 배포 워크플로우 변경 없음. M1(0.0.1v)에 신설된 인프라를 dev 프로필로 유지하는 상태. 본 파일은 그 결정 근거 · M1 baseline 참조 · M5 이후 재개 조건을 기록한다.

**작성 시점**: 2026-07-21 (D7) · M4 동결 판정 시점.

---

## 왜 스킵인가

**스킵 결정**: 본 버전(0.0.4v)은 M2·M3와 동일한 **로컬 개발 스코프**.

- `milestone.md`가 M4 스코프에서 배포 라인을 명시적으로 제외 (§본 버전 제외 사유: "배포 라인 · 성능 baseline — M7 이관 유지")
- Card BC 대재편·Card→Axis 3-phase 마이그레이션·role catalog 3종 신설이 모두 classpath / dev H2 스코프
- 프로덕션 RDS·MySQL 마이그레이션은 M7 배포 재개 시점에 사전 dry-run 후 착지 예정
- Prometheus/Grafana는 M1 dev 환경에서 이미 배선된 상태 · 프로덕션 배선은 M7 (product-op.md Epic 1~3)

---

## M1 (0.0.1v) 인프라 산출물 참조

M1에 신설된 AWS 인프라 · M4 시점 dev 프로필로 유지 중:

| 리소스 | 카테고리 | M1 신설 · M4 유지 | 비고 |
| --- | --- | --- | --- |
| ECS Fargate cluster (dev) | 컴퓨트 | ✅ 유지 | dev Task 1개 실행 · M7에 prod cluster 신설 예정 |
| GHA OIDC 인증 | CI/CD | ✅ 유지 | dev 배포 파이프라인만 · prod는 M7 |
| ECR repository `thirdtool` | 아티팩트 | ✅ 유지 | dev 이미지만 push · lifecycle 정책 유지 |
| VPC (dev CIDR) | 네트워크 | ✅ 유지 | 2 AZ · dev 서브넷 · prod VPC는 M7 |
| ALB (dev) | 네트워크 | ✅ 유지 | dev host-header routing · prod는 M7 |
| RDS MySQL 8 (dev) | 데이터베이스 | ✅ 유지 | dev 인스턴스 · Flyway V1~V22 적용 상태 · V23~V30은 로컬 H2에만 · prod는 M7 |
| Secrets Manager (5 secret) | 보안 | ✅ 유지 | 5개 secret (db-master · jwt-signing · vertex-ai-key · kakao-oauth · smtp) 유지 · vertex-ai-key는 M6에 실호출 착수 |
| Actuator / Prometheus / Grafana | 관측 | ✅ 유지 (dev docker-compose) | prod 배선은 M7 (OP-BASELINE) |
| CORS / OAuth (Kakao) | 인증 | ✅ 유지 | M1 완주 상태 · 변경 없음 |

**총 M1 신설 리소스**: 9개 카테고리 · 모두 dev 유지.

---

## M4 인프라 상태

**dev 환경**:
- ECS Fargate dev Task 여전히 실행 · M1~M4 개발자 접속 가능
- Secrets Manager 5 secret 유지 · vertex-ai-key는 M6에 실호출 착수 예정
- Grafana dashboard 유지 · M4 실측 메트릭 없음 (dev H2만 · 프로덕션 트래픽 없음)
- Flyway V1~V22가 dev RDS에 이미 적용 · M4의 V23~V30은 dev H2에만 · prod RDS 미착지

**신규 배포 없음**:
- Card BC 대재편 · Card→Axis 3-phase · role catalog 3종 신설이 모두 로컬 classpath만
- 5개 Epic PR 어느 것도 프로덕션 배포되지 않음 · 로컬 부팅 · Reviewer 세션 · gradle test만
- GHA 배포 워크플로우 변경 없음

---

## 본 버전 인프라 활동 로그 (M4)

- **AWS 리소스 신규 생성**: 0
- **AWS 리소스 수정**: 0
- **AWS 리소스 삭제**: 0
- **IAM 정책 변경**: 0
- **Secrets Manager 신규 등록**: 0
- **GHA 배포 워크플로우 변경**: 0
- **프로덕션 Flyway 마이그레이션 적용**: 0 (M4의 V23~V30은 dev H2 · 로컬만)
- **CloudWatch Alarm 변경**: 0
- **Terraform / IaC 코드 변경**: N/A (M4는 IaC 미도입 · 콘솔 클릭 · CLI 위주)

**결론**: 5 Epic PR 착지 후에도 배포 라인에 영향 0. M1 인프라를 그대로 유지.

---

## M5 이후 인프라 재개 조건

1. **M5 Deck 폐기 · Review BC 신설 · Flyway V31~V37** — 여전히 dev H2 스코프. 인프라 활동 미수행 예정. 단, MySQL prod 마이그레이션 사전 준비는 M5 진행 중 계속 (rollback R페어 축적)
2. **M6 LLM Adapter 배선** — Vertex AI 크레덴셜 로컬 배선 · Secrets Manager에 vertex-ai-key 실 사용 착수 · dev 스코프이지만 GCP 크레덴셜 확립 필요
3. **M7 배포 라인 완주** — **첫 실질 인프라 활동**. VPC·ALB·RDS·ECS·Route53·ACM·Secrets Manager prod 신설 · `../0.0.7v/infra.md` §M7 신설 리소스 리스트 첫 실질 콘텐츠 확장. Flyway V1~V40 프로덕션 첫 착지
4. **관측 요구 발동** — M4 정성 지표 관찰 대상(N+1 SQL · Coverage 재계산 부담 등)이 실측 필요해질 때 · v1 릴리스 후 사용자 관찰
5. **다음 도메인 재편** — Deck 폐기 (M5) · Review 신설 (M5) 후에도 도메인 회귀 없이 안정화된 상태에서만 인프라 재개

---

## M4 종료 시 인프라 산출물 검증 체크리스트

- [x] dev 환경 health check 통과 (M4 Card BC 재편 후 · 로컬 부팅 정상)
- [x] dev 배포 이력 (GHA) M3 이후 변경 없음 확인
- [x] Secrets Manager 5 secret 유지 확인 (vertex-ai-key 준비 상태)
- [x] Grafana dev dashboard 유지 (M4 실측 데이터는 dev 트래픽 미미 · 별도 계획 없음)
- [x] ECR 이미지 정책 유지 (M4 신규 이미지 push 없음)
- [x] Flyway prod 이력 (V1~V22)이 M4의 로컬 V23~V30과 충돌 없음 확인 · M7 prod 착지 시 순차 적용 가능

---

## v1 릴리스 (0.1.0v · 2026-08-19) 대비 인프라 조기 알림

- **M7 배포 라인 완주가 v1 릴리스의 필수 선행**. M6까지는 계속 로컬 스코프. M7 D1부터 AWS 실물 신설 착수 · D7까지 프로덕션 URL 도달 목표.
- **Vertex AI 크레덴셜 조기 확립** — M6 배선 시점에 GCP service account · Secrets Manager 등록 · IAM task role 권한 확립 필요. M4 시점에 사전 준비 확인만.
- **Prometheus/Grafana 리프레시** — dev docker-compose 상태 유지 · M7에 prod scrape config 확립. Grafana dashboard M7 baseline (P50/P95/P99 · JVM · DB pool · 5xx rate).
- **RDS Flyway 마이그레이션 사전 dry-run** — M7 D1~D3에 staging RDS에 V1~V40 사전 실행 · rollback R페어 검증 · 프로덕션 첫 적용 위험 최소화.

---

## 참고

- 상위 계획: `./milestone.md`
- 성과: `./outcome.md` (기술 자산 · 로컬 스코프만)
- 회고: `./review.md` (배포 관련 사항 없음)
- 성능: `./performance.md` (인프라 관련 · M7 배선 예정)
- 비용: `./cost.md` (M1 잔존 · 신규 $0)
- 이전 마일스톤: `../0.0.3v/infra.md` (M3 동일 스킵)
- **M7 신설**: `../0.0.7v/infra.md` — 신설 예정 · AWS 리소스 실물 리스트 첫 실질 콘텐츠
- 릴리스 로드맵: `../../release/version/0.0.1v/release.md` §Product 릴리스 스코프 §7. 배포 라인
