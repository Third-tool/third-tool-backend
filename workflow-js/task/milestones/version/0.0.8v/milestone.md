# M8 / 0.0.8v — Week of 2026-08-12 ~ 2026-08-18 (D1 = 2026-08-12 Wed) · 🚀 **릴리스 준비 주간**

> **마일스톤의 역할**: M7이 08-11에 동결됐다 (프로덕션 첫 URL 도달 + AWS 실물 신설 + Grafana 관찰 배선 + AS Epic 6 + AIR Epic 2 완주). 본 M8은 **첫 사용자 릴리스 (0.1.0v · 2026-08-19 수) 최종 준비 주간** — fix 이슈 잔재 소진 + 3명 사용자 UX 5시나리오 실측 + Reviewer 5관점 전체 통합 리뷰 + 파이프라인 dry-run + 릴리스 문서화. **릴리스 GO/NO-GO 판정이 D6 (08-17 월)** 에 이루어진다.
>
> 한 주 = 한 버전 = `version/0.0.X v/` 폴더 하나. 본 버전(0.0.8v)에는 다음 8 파일이 들어간다 (M8은 릴리스 준비 주간이라 산출물 1개 추가):
> - `milestone.md` *(본 문서)* — 잡힌 양 + 일정 + 의존 + Epic PR 매트릭스
> - `infra.md` — M7 신설 리소스 안정화 관찰 · 백업 정합 · Alarm 튜닝
> - `performance.md` — 3명 사용자 실측 Web Vitals + P95 latency baseline 확정
> - `outcome.md` — 사용자·기능 성과 (릴리스 준비 완료 · 사이클 10단계 3명 완주)
> - `cost.md` — 비용 성과 (AWS + Vertex AI 첫 주간 실측)
> - `review.md` — 회고 + **릴리스 GO/NO-GO 판정 결과**
> - `eval.md` — AI 응답 품질 평가 프레임 종합 (M3~M7 baseline 통합 · 릴리스 시점 baseline 확정)
> - **`release-checklist.md`** *(M8 신규)* — 릴리스 D-Day 체크리스트 · GO/NO-GO 판정 기록 · 3명 사용자 안내 문서 링크

**릴리스 대응**: 본 M8은 첫 사용자 릴리스(0.1.0v, 2026-08-19) 6주 로드맵의 **일곱 번째이자 마지막** 마일스톤. **본주 D6에 릴리스 GO/NO-GO 최종 판정** · D7 (08-18) 프로덕션 마지막 안정화 · **08-19 수 릴리스 발행**.

**FE 대응 마일스톤**: `workflow/task/pes/fe/fe-milestones/version/0.0.8v/milestone.md` — 동일 주간, FE 릴리스 준비 · 사용자 안내 UI · 첫 진입 온보딩.

---

## 0.0.8v 스코프 결정 (M7 이후, 2026-08-12)

**M7 착지 결과 요약**:
- ✅ 프로덕션 첫 URL 도달 (`https://api.thirdtool.io`) · AWS VPC + ALB + RDS + ECS + Secrets Manager + Route53 + ACM 실물 신설
- ✅ Grafana 4 판넬 baseline (P95 latency · JVM · DB pool · 5xx rate)
- ✅ Prometheus scrape · `thirdtool.suggestion.*` 메트릭 · Slack 배포 알림 · CloudWatch Alarms 5종
- ✅ AS Epic 6 완주 (관측성 + Runbook 5 시나리오)
- ✅ AIR Epic 2 완주 (axisDraft VO + 6-Port 호출 + refresh + 실패 처리)
- ✅ User BC 잔재 완주 (100%)
- ✅ Flyway V40 프로덕션 착지
- ✅ Reviewer 5관점 세션 7회 진행

**M8 축 결정 — 릴리스 준비 · 사용자 실측 · GO/NO-GO 판정**:

M7 review.md §다음 마일스톤 결정 예고 반영. **본주는 신규 기능 최소 · 관측·검증·문서화 중심**. release.md §릴리스 성공 기준 6 신호 · §사전 검증 시나리오 5개 3명 완주 · §GO/NO-GO NO-GO 조건 판정.

이 지시를 반영해 본 M8은 다음 5축으로 구성:

1. **fix 이슈 잔재 소진** — M3~M7 관찰된 세부 이슈 5~8건 처리 (P0/P1 이슈 우선). **주력 (~35%)**.
2. **3명 사용자 UX 5시나리오 실측** — release.md §사전 검증 시나리오 5건 각각 3명 완주 검증. **주력 (~30%)**.
3. **Reviewer 5관점 전체 통합 리뷰** — M1~M7 산출물 통합 · 특히 Card lifecycle · Review · AI 3종 축 정합. **부차 (~15%)**.
4. **파이프라인 dry-run + 문서화** — 프로덕션 배포 리허설 · rollback 리허설 · Slack · Alarm 발동 · README · 사용자 안내. **부차 (~15%)**.
5. **관측 지표 임계값 튜닝 + 남은 잔재 Product Epic 소진** — M7 실측 후 CloudWatch Alarm 임계값 · log Epic 5 · op Epic 4 잔재. **경량 (~5%)**.

**Epic PR 예상 5건 (본주 목표)**:

| Epic PR | 대응 | 예상 SP | 종료 목표 |
|---|---|---|---|
| PR#1 (FIX-BATCH-P0) | M3~M7 관찰 fix 이슈 P0 소진 (예상 3~4건) | 5 | D3 |
| PR#2 (FIX-BATCH-P1) | P1 fix 이슈 소진 (예상 2~4건) + Log/Op Epic 잔재 (E5/E4) | 4 | D4 |
| PR#3 (RELEASE-DOCS) | README + 사용자 안내 문서 + Privacy + 서비스 약관 최소판 + `docs/runbooks/*` 정합 | 3 | D5 |
| PR#4 (PIPELINE-DRY-RUN) | 프로덕션 배포 리허설 + rollback 리허설 + Slack/Alarm 강제 발동 검증 + CloudWatch Alarm 임계값 튜닝 | 3 | D5 |
| PR#5 (UX-TEST-FINDINGS) | 3명 사용자 UX 5시나리오 실측 후 발견된 blocker 이슈 소진 | 3 | D6 |

**Total: 5 Epic PR · 총 18 SP** (M7 30 SP 대비 -40% · **여유 확보**. UX 실측·판정에 여유). Reviewer 5관점 세션 5회 (본주는 특히 통합 리뷰 세션 1건 별도).

**본 버전 제외 사유**:
- **AIR Epic 3·4·5** — v2 이관 유지
- **AS SUPERSEDED 잔재** — v2 이관 유지
- **LT E3 SUPERSEDED 잔재** — v2 이관 유지
- **검색 · 미디어 CDN · 캐시 · 알림 (in-app 채널) · Admin · 부하 테스트** — v2 이관 유지
- **AI 비용 예산 cap 자동 컷** — v2 이관 유지 (v1은 관찰 지표만)
- **다중 리전 · APM · A/B 실험** — v2 이관 유지
- **오토스케일링 정책 세부** — v1은 최소 인스턴스 · v2

---

## 진행 중 Product 잔여 인벤토리 (Before/After — M8 진입 시 vs 릴리스 시점 예상)

| Product | 총 Story | M8 진입 시 완료 | M8 진입 시 잔여 | M8 대상 | **릴리스 시점 예상 잔여** | **해결율** |
| --- | --- | --- | --- | --- | --- | --- |
| 1. 인증 (`product-auth.md`) | 10 | 10 | 0 | — | 0 | ✅ 완주 |
| 2. User BC (`Product.md`) | 8 | 8 | 0 | — | 0 | ✅ 완주 (M7) |
| 3. Learning Tower (`product-learning-tower.md`) | 42 | 32 | 10 | — (SUPERSEDED 잔재) | 10 | 0% |
| 4. AI Suggestion (`product-ai-suggestion.md`) | 34 | 27 | 7 | — (SUPERSEDED 잔재 v2) | 7 | 0% |
| 5. AI Interactive Roadmap (`product-ai-interactive-roadmap.md`) | ~19 | 8 (E1·E2) | ~11 | — (Epic 3·4·5 v2 이관) | ~11 | 0% |
| 6. Card (`product-card.md`) | 16 | 16 | 0 | — | 0 | ✅ 완주 |
| 7. Review (`product-review.md`) | 23 | 23 | 0 | — | 0 | ✅ 완주 |
| 8. 컨테이너 배포 (`product-infra-deploy.md`) | 9 | 9 | 0 | — | 0 | ✅ 완주 (M7) |
| 9. AWS 네트워크 (`product-infra-network.md`) | 8 | 6 | 2 | — (CloudFront/CDN v2) | 2 | 0% |
| 10. Secrets·백업·관측 (`product-infra-ops.md`) | 10 | 4 | 6 | — (Alarm 튜닝만 · 나머지 v2) | 6 | 0% |
| 11. **로깅** (`product-log.md`) | 9 | 8 | 1 | **1 Story (E5)** | **0** | ✅ 완주 |
| 12. **메트릭** (`product-op.md`) | 7 | 6 | 1 | **1 Story (E4)** | **0** | ✅ 완주 |
| 13. 검색 (`product-search.md`) | ~18 | 0 | ~18 | — | ~18 | 0% (v2) |
| 14. 미디어 · 캐시 · 알림 · Admin · FE CDN | — | 0 | — | — | — | — |
| 15. 부하 테스트 (`product-load-test.md`) | 6 | 0 | 6 | — | 6 | 0% (v1 이후) |
| **합계** | **~206** | **~157** | **~50** | **~10 fix 이슈 + 2 Story 완주 + 3명 UX 실측** | **~48** | **~5%** |

### 📊 M8 예상 성과 카드

- **총 해결 대상**: fix 이슈 5~8건 + 2 Story (Log E5 + Op E4) + 3명 사용자 UX 5시나리오 실측 + 릴리스 문서 · 파이프라인 dry-run
- **완주 예상 SDD Product**:
  - `product-log.md` — 완주 100% (E5 마지막 Story)
  - `product-op.md` — 완주 100% (E4 마지막 Story)
- **릴리스 시점 남는 것** (v2 이관):
  - Learning Tower **10 Story 잔여** (Epic 3 SUPERSEDED 잔재 4 · 기타 backlog 6)
  - AI Suggestion **7 Story 잔여** (SUPERSEDED)
  - AI Interactive Roadmap **11 Story 잔여** (Epic 3·4·5)
  - 인프라 잔재 **8 Story** (CloudFront · rotation 자동 · APM 등)
  - 검색 **~18 Story** (v2)
  - 부하 테스트 **6 Story** (v1 이후 · 사용자 확대 시점)

---

## Epic PR 매트릭스 (본주 잡힌 양)

**본 M8의 특성**: 신규 Story 최소 · fix 이슈 소진 · 사용자 실측 · 문서화 중심. Reviewer 세션은 각 PR + 별도 통합 리뷰 1회.

### Epic PR #1 — `FIX-BATCH-P0` (P0 fix 이슈 3~4건 소진)

**Base 브랜치**: `feat/053-fix-batch-p0`

**대응 fix 이슈** (M3~M7 관찰 기반 · 실제 목록은 M7 review.md에서 확정):
- M4~M5 관찰 P0 fix 이슈 (예상 3~4건) — Card lifecycle 회귀 · Review batch cron 오차 · LLM 응답 파싱 실패 시나리오 · UI 회귀 등
- 각 이슈는 `workflow/task/fix/brainstorming/version/0.0.3v/` (신설 예정)에서 관리

| # | 예상 이슈 | 한 줄 | SP |
|---|---|---|---|
| 1 | (M7 관찰) 프로덕션 첫 배포 후 발견된 P0 | (관찰 후 확정) | 1~2 |
| 2 | (M6 관찰) LLM Cascade 폴백 실측 edge case | (관찰 후 확정) | 1~2 |
| 3 | (M5 관찰) DailyBatch cron 자정 오차 edge | (관찰 후 확정) | 1 |
| 4 | (M4 관찰) Card `createdMode` 하이브리드 실측 edge | (관찰 후 확정) | 1 |

**PR 종료 신호**: 각 P0 이슈 재현 → 해결 → 회귀 테스트 통과. `./gradlew test` BUILD SUCCESSFUL.

**Reviewer 세션**: 5관점 발사. **특히 Sceptical + Test Reviewer가 회귀 커버리지 판정**.

### Epic PR #2 — `FIX-BATCH-P1` (P1 fix + Log E5 + Op E4 잔재)

**Base 브랜치**: `feat/054-fix-batch-p1`

**대응**:
- P1 fix 이슈 2~4건 소진
- `product-log.md` Epic 5 마지막 Story (예: 로그 aggregation query 최적화 or 로그 rotation 정책)
- `product-op.md` Epic 4 마지막 Story (예: 커스텀 Grafana 판넬 or JVM heap threshold 조정)

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | P1 fix batch | 4건 소진 (관찰 후 확정) | 2 |
| 2 | LOG E5 | 로그 aggregation 최적화 or rotation 정책 | 1 |
| 3 | OP E4 | 커스텀 Grafana 판넬 or JVM heap threshold | 1 |

**PR 종료 신호**: P1 이슈 소진 · Log/Op Product 완주 100% · Grafana 커스텀 판넬 반영.

**Reviewer 세션**: 5관점 발사.

### Epic PR #3 — `RELEASE-DOCS` (README + 사용자 안내 + 정책 문서)

**Base 브랜치**: `feat/055-release-docs`

**대응**:
- `README.md` 갱신 (프로덕션 URL · 사용법 · 로컬 개발 가이드)
- `docs/user-guide/onboarding.md` 신설 — 3명 사용자 안내 문서 (스크린샷 포함)
- `docs/user-guide/faq.md` 신설 — 예상 FAQ 5~10건
- `docs/legal/privacy-policy.md` 신설 — 개인정보 처리 방침 최소판
- `docs/legal/terms-of-service.md` 신설 — 서비스 약관 최소판
- `docs/runbooks/*` 갱신 (M7 신설분 정합)
- `docs/DOMAIN.md` 최종 갱신 (릴리스 상태 반영)

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | Docs S3-1 | README 프로덕션 URL · 사용법 · 로컬 가이드 갱신 | 0.5 |
| 2 | Docs S3-2 | `docs/user-guide/onboarding.md` + `faq.md` 신설 | 1 |
| 3 | Docs S3-3 | `docs/legal/privacy-policy.md` + `terms-of-service.md` 신설 | 1 |
| 4 | Docs S3-4 | `docs/runbooks/*` 정합 + `docs/DOMAIN.md` 최종 갱신 | 0.5 |

**PR 종료 신호**: 문서 5개 파일 존재 · 프로덕션 URL 사용자 화면에서 링크 접근 · 3명 사용자 사전 안내 자료 완성.

**Reviewer 세션**: 5관점 발사. **특히 Sceptical Reviewer가 "3명 사용자가 이 문서만으로 첫 진입 가능한가"를 판정** (사용자 관점 · 기술 용어 최소).

### Epic PR #4 — `PIPELINE-DRY-RUN` (배포·rollback 리허설 + Alarm 튜닝)

**Base 브랜치**: `feat/056-pipeline-dry-run`

**대응**:
- staging 환경에서 배포 → rollback 리허설
- CloudWatch Alarm 5종 임계값 튜닝 (M7 실측 데이터 기반)
- Slack `#thirdtool-deploy` · `#thirdtool-alerts` 채널 실제 알림 실측 확인
- ECS Task auto-recover 시나리오 검증 (강제 kill → 자동 재시작)
- Secrets Manager rotation 절차 문서화 (자동 rotation은 v2)
- 부하 5명 · 10명 · 20명 시뮬레이션 (jmeter 최소 사용 · v1 단순 검증)

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | Pipeline S4-1 | staging 배포 → rollback 리허설 (git_sha 지정 배포) | 1 |
| 2 | Pipeline S4-2 | CloudWatch Alarm 임계값 튜닝 (M7 실측 기반) | 0.5 |
| 3 | Pipeline S4-3 | ECS Task auto-recover 시나리오 검증 + Secrets rotation 절차 문서화 | 1 |
| 4 | Pipeline S4-4 | 5·10·20 concurrent user 시뮬레이션 (P95 latency · 5xx rate 관찰) | 0.5 |

**PR 종료 신호**:
- staging에서 배포 → rollback → 재배포 successful
- Alarm 5종 임계값 명시 · 강제 발동 확인
- Task 강제 kill 후 60초 이내 자동 재시작
- Secrets rotation 수동 절차 문서화 (`docs/runbooks/secrets-rotation.md`)
- 20 concurrent user 시나리오 P95 < 2s · 5xx rate 0

**Reviewer 세션**: 5관점 발사. **특히 Sceptical + Architecture Reviewer가 "rollback 리허설이 실제 장애 상황에서 유효한가"를 판정**.

### Epic PR #5 — `UX-TEST-FINDINGS` (3명 UX 실측 후 blocker 이슈 소진)

**Base 브랜치**: `feat/057-ux-test-findings`

**대응**:
- D4~D5에 3명 사용자 UX 5시나리오 실측 (release.md §사전 검증 시나리오)
- 발견된 blocker 이슈 즉시 소진 (예상 2~5건)
- FE 사용자 안내 UX 개선 (첫 진입 온보딩 · 오류 메시지)
- 실측 데이터 `outcome.md`·`review.md`·`performance.md`·`cost.md`에 반영

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | UX S5-1 | 시나리오 1·2 (첫 진입 · 카드 저장·노출) 실측 blocker 소진 | 1 |
| 2 | UX S5-2 | 시나리오 3·4 (Mode 다운 · Cross-layer 짬뽕) 실측 blocker 소진 | 1 |
| 3 | UX S5-3 | 시나리오 5 (Streak · 대시보드) 실측 blocker 소진 | 0.5 |
| 4 | UX S5-4 | FE 사용자 안내 UX 개선 (온보딩 · 오류 메시지) | 0.5 |

**PR 종료 신호**: 5시나리오 각각 3명 완주 · blocker 이슈 소진 · FE 온보딩 완성.

**Reviewer 세션**: 5관점 발사. **특히 Sceptical Reviewer가 "사용자 관점에서 자연스러운 흐름인가"를 판정 · 개발자 편의 관점 제외**.

---

## 별도 세션 — 전체 통합 리뷰 (D6)

**Reviewer 5관점 전체 통합 리뷰** (M1~M7 산출물 통합):
- Card lifecycle 재편 (M4) — Mode·Budget·createdMode 하이브리드 정합
- Review BC 신설 (M5) — DailyBatch·Session·Dashboard 축 정합
- AI Suggestion 3층 (M3~M6) — Static 4-role · LLM Adapter · Cascade 폴백
- AIR Session Wizard (M6~M7) — Aggregate · axisDraft 흐름
- 인프라 배포 라인 (M7) — VPC · ALB · RDS · ECS · Secrets · Grafana

Reviewer 5관점 병렬 발사 · 각 관점이 M1~M7 산출물 전체를 훑고 릴리스 GO/NO-GO 판정에 기여.

---

## Story 카테고리별 합계

| 카테고리 | 대응 | Story 수 | SP | 비중 |
| --- | --- | --- | --- | --- |
| P0 fix 소진 | PR#1 | 3~4 | 5 | 28% |
| P1 fix + Log/Op 잔재 | PR#2 | 5~7 | 4 | 22% |
| 릴리스 문서화 | PR#3 | 4 | 3 | 17% |
| 파이프라인 dry-run + Alarm 튜닝 | PR#4 | 4 | 3 | 17% |
| 3명 UX 실측 blocker | PR#5 | 4 | 3 | 17% |
| **합계** | | **~20+** | **18** | 100% |

**분배 근거**:
- M7 실 착지 30 SP 대비 -40% · 릴리스 준비 주간 특성상 여유 확보 · UX 실측·판정 시간 확보.
- **fix + 잔재 50%** — M3~M7 관찰 이슈 정리 · Product 완주 마무리.
- **문서·dry-run·UX 50%** — 릴리스 발행 필수 · 사용자 실측 데이터 확보.
- **5개 Epic PR**: 각 PR 독립. Reviewer 세션 5회 + 통합 리뷰 1회.

---

## 종료 신호 · 🚀 릴리스 GO/NO-GO 판정 (D6 · 2026-08-17 Mon)

본주 D6 시점에 다음 신호로 릴리스 GO/NO-GO 최종 판정. release.md §릴리스 성공 기준 6 신호와 정합.

### GO 조건 (6 신호 중 5개 이상 성립 시 GO)

- [ ] **핵심 사이클 신호**: 3명 사용자가 프로덕션에서 §MVP 컨셉 10단계 사이클 순회 성공 (Cornell 노트 저장 → 다음날 batch에 노출 → clear → 대시보드 확인)
- [ ] **Mode 매핑 신호**: M3 하이브리드 down/up 시나리오 프로덕션 검증 (down 시 즉시 load 감소 확인 + up 시 진행 카드 변화 없음)
- [ ] **Cross-layer 짬뽕 신호**: 여러 layer의 axis card가 하나의 daily 큐에 정확히 조합 (`DailyLearningBatch.entries.count` = sum of `card.isDueOn(today)` across all axes)
- [ ] **AI Static + LLM 신호**: 4-role 감지 + Static Adapter 6-Port(outline·subtree) + LLM Cascade + Rate Limit + Session axisDraft 모두 프로덕션에서 정상 반환 (`suggestionsAvailable: true`)
- [ ] **관측 신호**: Grafana에서 `daily_batch.generated_total` · `card.archived_total{reason}` · `review_session.started_total` · `thirdtool.suggestion.calls_total{provider}` 4개 지표 실시간 갱신
- [ ] **자정 close 신호**: KST 매일 00:05에 어제 batch 자동 close 검증 (staging 24시간 관찰 · 프로덕션 1회 검증)

### NO-GO 조건 (하나라도 해당 시 릴리스 08-19 → 08-26 연기)

- [ ] 프로덕션 배포 시 DB 마이그레이션 실패
- [ ] 3명 사용자 중 1명 이상이 로그인 · 카드 생성 · Review 세션 진입 3단계 중 실패
- [ ] LT/Card/Review 단위 테스트 커버리지 < 80%
- [ ] 심각한 장애 이력 (Static Adapter 응답 실패 · LLM 완전 무응답 · DB 접속 불가 · Alarm 5xx rate > 5%)
- [ ] 프로덕션 URL이 D6 이전에 24시간 이상 down

### 판정 결과 기록

`review.md` §릴리스 판정 절에 **GO / NO-GO / 조건부 GO (일부 스코프 조정 후 발행)** 결과 · 각 신호별 정량 데이터 · 판정자 (사용자님) 서명 기록.

---

## 의존 chain

```
[선행: M7 착지]
프로덕션 URL 도달 · Grafana 관찰 배선 · AIR Epic 2 완주 ✅

[D1~D2: fix 이슈 소진 우선]
PR#1 (FIX-BATCH-P0) · PR#2 (FIX-BATCH-P1 · Log/Op 잔재)
     │
     ▼
[D3~D5: 문서화 + dry-run + UX 실측 병렬]
PR#3 (RELEASE-DOCS)  ──►  PR#4 (PIPELINE-DRY-RUN)  ──►  UX 5시나리오 3명 실측
     │
     ▼
[D6: 통합 리뷰 + PR#5 blocker 소진 + 🚀 GO/NO-GO 판정]
PR#5 (UX-TEST-FINDINGS) · 전체 통합 Reviewer 세션 · 릴리스 판정
     │
     ▼
[D7: 최종 안정화]
프로덕션 마지막 안정화 · Alarm 관찰 · 사용자 3명 사전 안내
     │
     ▼
🚀 2026-08-19 (Wed) · 0.1.0v 릴리스 발행 · 3명 사용자 안내 발송
```

---

## 작업 일정

D1 = 2026-08-12 (Wed). 종료 D7 = 2026-08-18 (Tue). **릴리스 = 2026-08-19 (Wed)**.

| 일 | 날짜 | 잡힌 작업 |
| --- | --- | --- |
| D1 (수) | 08-12 | **fix 이슈 P0 목록 확정** (M7 review.md 참조) · PR#1 착수 · M7 관찰 세부 파악 |
| D2 (목) | 08-13 | PR#1 P0 소진 진행 · PR#2 착수 (P1 + Log/Op 잔재) |
| D3 (금) | 08-14 | PR#1 완주 → **PR#1 머지 + Reviewer 세션** · PR#3 착수 (문서) · PR#4 착수 (dry-run) |
| D4 (토) | 08-15 | PR#2 완주 → **PR#2 머지 + Reviewer 세션** · PR#3·PR#4 진행 · **3명 사용자 UX 실측 시나리오 1·2 착수** (첫 진입 · 카드 저장) |
| D5 (일) | 08-16 | PR#3 완주 → **PR#3 머지 + Reviewer 세션** · PR#4 완주 → **PR#4 머지 + Reviewer 세션** · **UX 시나리오 3·4·5 완주** (Mode 다운 · Cross-layer · Streak) |
| D6 (월) | 08-17 | **PR#5 (UX blocker 소진) 착수·완주 → PR#5 머지 + Reviewer 세션** · **전체 통합 Reviewer 5관점 세션 (M1~M7 산출물 통합)** · **🚀 릴리스 GO/NO-GO 판정** · `review.md` §판정 결과 기록 |
| D7 (화) | 08-18 | **최종 안정화** · Alarm 24h 관찰 · 3명 사용자 사전 안내 발송 (릴리스 8월 19일 안내) · `outcome.md` · `cost.md` · `eval.md` · `release-checklist.md` 최종화 · 0.0.8v 동결 |
| 🚀 릴리스 | **08-19 (Wed)** | **0.1.0v 발행** · GitHub release tag · 3명 사용자 접속 시작 · Grafana 관찰 강화 (24h 대기) |

**Reviewer 세션 규칙**: 각 Epic PR 머지 직전 5관점 병렬 발사 + D6 통합 세션 별도 1회 (총 6회). M7 표준 스케일 유지.

**Flyway V 버전 순서 관리** (M7에서 V40 소모):
- V41: (예약 · fix 이슈 소진 중 신설 스키마 있으면 착지 · 없으면 스킵)
- **원칙**: M8은 신규 스키마 최소화. fix 이슈 처리 중 필요 시에만 V번호 부여.

---

## 리스크와 관찰 포인트

| 영역 | 리스크 | 관찰 포인트·완화 |
| --- | --- | --- |
| 3명 사용자 확보 지연 | 릴리스 시점 사용자 준비 안 됨 · 안내 시점 놓침 | D2에 3명 최종 확정 · D7 사전 안내 발송 · 릴리스 후 24h 안내 지원 |
| UX 5시나리오 실측 blocker | 사용자가 시나리오 완주 실패 · 대량 blocker 발견 시 릴리스 지연 | D4~D5 실측 시점에 즉시 blocker 대응 · 소진 어려운 blocker는 v0.1.1v로 이관 · NO-GO 조건 발동 시 릴리스 08-26 연기 |
| 프로덕션 첫 사용자 접속 부하 | 3명이지만 동시 접속 시 예상외 문제 | D7 24h 사전 관찰 · Alarm 튜닝 · rollback 절차 리허설 완료 |
| 릴리스 후 즉시 장애 | 첫 24h가 가장 위험 · P0 장애 발생 시 즉시 대응 | 릴리스 후 24h 근무 대기 (사용자님) · Runbook 5 시나리오 활용 · Slack 알림 활성 |
| 문서 부족 · 사용자 이해도 낮음 | 사용자 안내 문서가 기술 용어 · 사용자 관점 부족 | PR#3 Sceptical Reviewer가 사용자 관점 판정 · D5~D6 3명 실측 시 문서 이해도 관찰 |
| fix 이슈 예상 대비 많음 | M3~M7 관찰 이슈가 5~8건 넘게 발견될 경우 | P0 우선 소진 · P1·P2는 v0.1.1v로 이관 · 스코프 조정 |
| 통합 Reviewer 세션 병목 | M1~M7 전체 훑기가 각 관점당 2~3시간 · D6에 몰림 | 병렬 발사 유지 · 세션 결과가 GO/NO-GO에 직접 반영되므로 필수 · 시간 부족 시 D5부터 착수 |
| Vertex AI · AWS 비용 폭주 | 3명 사용자 실측 · 반복 호출 시 비용 증가 | `cost.md` 실시간 관찰 · Vertex AI daily budget 알림 활성 · v2 cap 이관 |
| 프로덕션 DB 데이터 무결성 | 3명 사용자 실데이터 저장 · PITR 백업 정합 | D3 PITR restore drill 완료 · daily backup 활성 확인 |
| 릴리스 8월 19일 확정 vs 지연 | GO 신호 5개 미만 시 08-26 연기 필요 | D6 판정 결과에 따라 즉시 결정 · 사용자님 GO/NO-GO 최종 승인 |
| SUPERSEDED 잔재 처리 | LT E3·AS Story 잔재가 v2 이관되지만 SDD 문서 정합성 관리 부담 | M8 D5~D6에 `sdd/in-progress/*.md`에 SUPERSEDED 표기 정합 · v2 이관 명시 |

---

## 다음 마일스톤 (릴리스 이후)

**0.1.1v (~2026-09-02) — 릴리스 후 첫 튜닝 · 2주**
- 3명 사용자 관찰 실측 대응 (fix 이슈 · UX 개선)
- **챕터 노드 재생성 API + hint** (이슈 #18)
- AIR Epic 3 (Roadmap/Selection draft)
- 관측 지표 임계값 튜닝

**0.2.0v (~2026-09-30) — L3 대시보드 + T3 알림**
- L3 규칙 기반 추천 안내 (이슈 #26 확장 · M5 관찰 3주 데이터 활용)
- T3 조건부 주간 요약 알림
- In-app notification 채널
- AS Epic 관측성 확장

**0.3.0v (~2026-11-30) — 검색 + Layer 시각화**
- `product-search.md` 착수
- Layer 진행률 파생 API
- 미디어 업로드 세부 UX

**0.4.0v ~ 1.0.0v** — 스코프 미확정 · 사용자 관찰 결과에 따라 결정.

**로드맵 참조**: `workflow/task/milestones/release/version/0.0.1v/release.md` §릴리스 이후 로드맵.

---

## Product 상태 전환 신호 (M8 종료 · 릴리스 시점)

- `in-progress/product-log.md` — **완주** (Epic 5 마지막 Story 소진)
- `in-progress/product-op.md` — **완주** (Epic 4 마지막 Story 소진)
- 나머지 Product는 M7 상태 유지 (v2 이관 명시)
- **SDD Product Move**: 완주된 SDD Product는 `sdd/in-progress/` → `sdd/released/0.1.0v/` 로 이동 검토 (v1 릴리스 후)
- `fix/brainstorming/version/0.0.2v/` — 이슈 폴더 archive (v1 pivot 이관 완료)
- **`fix/brainstorming/version/0.0.3v/`** — 신설 (v1 릴리스 후 사용자 관찰 이슈 축적처)

---

## brainstorming 트리거

본 M8 완료 후 `workflow/task/pes/brainstorming/0.1.1v/` 신설:
- 3명 사용자 첫 주 관찰 데이터 (`brainstorming/0.1.1v/first-week-user-observation.md`)
- 챕터 노드 재생성 UX 초안 (`brainstorming/0.1.1v/chapter-regeneration-ux.md`) — 이슈 #18 대비
- AIR Epic 3·4·5 우선순위 (`brainstorming/0.1.1v/air-epic-priority.md`)
- v0.2.0v L3·T3 대시보드 확장 스코프 (`brainstorming/0.1.1v/dashboard-l3-scope.md`)
- v0.3.0v 검색 착수 시점 (`brainstorming/0.1.1v/search-timing.md`)

---

## 참고

- 잔여 Story 인벤토리 출처: `workflow/task/pes/workspectrum/sdd/in-progress/` 19개 Product 파일
- fix 이슈 원천: M3~M7 각 `review.md` §잔여 이슈 절
- 마일스톤 패키지 의도: `workflow/task/milestones/references/001.md`
- FE 대응 마일스톤: `workflow/task/pes/fe/fe-milestones/version/0.0.8v/milestone.md` (릴리스 준비 주간)
- 이전 마일스톤: `workflow/task/milestones/version/0.0.7v/milestone.md` — M7 원안 · `../0.0.7v/outcome.md` · `../0.0.7v/review.md` · `../0.0.7v/infra.md` (AWS 리소스) · `../0.0.7v/performance.md` (Grafana baseline)
- **첫 릴리스 계획**: `workflow/task/milestones/release/version/0.0.1v/release.md` — 0.1.0v (2026-08-19) 스코프 · **M8 목표: "릴리스 대비 잔여 소진 + UX 테스트 3명 시나리오 + Reviewer 5관점 발사 + 파이프라인 dry-run"**
- 본 버전의 산출물 8종 (M8 신규 1종): `milestone.md`(본 문서) · `infra.md` · `performance.md` · `outcome.md` · `cost.md` · `review.md` · `eval.md` · **`release-checklist.md` (M8 신규 · 릴리스 D-Day 체크리스트)**
- 양식 진화: 본 milestone은 0.0.7v milestone.md 양식 답습. **본 M8은 릴리스 준비 주간이라 `release-checklist.md`를 8번째 산출물로 신설**. Story 수는 적으나 UX 실측 · Reviewer 통합 · GO/NO-GO 판정이 축.
- **주요 소진 참조**:
  - fix 이슈 (M3~M7 관찰) **~50%**
  - 릴리스 문서 · dry-run · UX **~50%**
- **다음 릴리스 이후 브레인스토밍**: `workflow/task/pes/brainstorming/0.1.1v/` 신설 예정
- **🚀 릴리스 D-Day**: 2026-08-19 (수) · 0.1.0v · 3명 사용자
