# M8 / 0.0.8v — 릴리스 대비 최종 UX 확인 가이드 (Local + Production)

> **파일의 역할**: M8 (2026-08-12 ~ 08-18) 완료 시점에 **릴리스 대비 최종 판정**을 위해 확인해야 할 것들을 정리한다. **본 M8은 릴리스 준비 주간이므로 종전 마일스톤과 달리 "3명 사용자 실측 시나리오 5개" + "릴리스 GO/NO-GO 판정" 절이 중심**이다.
>
> **본 M8의 특성 — M8 하이라이트**: "3명 사용자 UX 5시나리오 실측 + GO/NO-GO 판정 + 프로덕션 첫 릴리스 발행 (08-19)". fix 이슈 잔재 소진 · 릴리스 문서화 · 파이프라인 dry-run · Reviewer 5관점 전체 통합 리뷰. **본 마일스톤이 6주 로드맵의 클라이맥스**.

---

## 확인 목적

- **fix 이슈 소진**: M3~M7 관찰된 P0·P1 이슈 소진 · 회귀 없음
- **3명 사용자 UX 5시나리오 실측**: release.md §사전 검증 시나리오 5건 각각 3명 완주 확인
- **릴리스 문서 완성**: README · onboarding · privacy · terms · runbook 사용자 사전 안내 가능
- **파이프라인 dry-run**: 배포·rollback 리허설 · Alarm 강제 발동
- **관측 안정화**: Grafana · CloudWatch Alarm 24h 관찰
- **GO/NO-GO 판정**: release.md §릴리스 성공 기준 6 신호 최종 판정
- **🚀 릴리스 발행**: 08-19 (Wed) · 0.1.0v · 3명 안내 발송

---

## 사전 조건

- 로컬 · 프로덕션 환경 M7 상태 유지
- **3명 사용자 최종 확정** (D2까지) — 이메일 · 개인 네트워크 · 릴리스 8월 19일 안내 예정 확답
- AWS 콘솔 · Slack · Grafana · CloudWatch 접근권 유지
- 3명 실측 관찰 도구:
  - 사용자 화면 공유 (Zoom · Discord · Teams)
  - 세션 녹화 (선택 · 사용자 동의 후)
  - 관찰 기록 노트 (`docs/observation/2026-08-XX-user-N.md`)

---

## 로컬 부팅 절차 (M7과 동일)

```bash
export GOOGLE_APPLICATION_CREDENTIALS=~/.config/gcloud/thirdtool-dev-key.json
./gradlew clean bootRun

# fix 이슈 소진 후 회귀 테스트
./gradlew test
# → BUILD SUCCESSFUL · 커버리지 80%+ 유지
```

## 프로덕션 확인 (M7과 동일 · 24h 관찰 강화)

```bash
# 프로덕션 URL
curl -s https://api.thirdtool.io/actuator/health
# → {"status":"UP"}

# 실측 트래픽 monitoring (Grafana)
open https://grafana.thirdtool.io
# → P95 latency · 5xx rate · thirdtool.suggestion.calls_total 실시간 관찰
```

---

## 로컬 확인 체크리스트

### ✅ fix 이슈 소진 검증 (30분)

- [ ] **P0 fix 이슈 목록** — M7 review.md §잔여 P0 이슈 절 참조 · 각 이슈 재현 → 해결 → 회귀 테스트 확인
- [ ] **P1 fix 이슈 목록** — M7 review.md §잔여 P1 이슈 절 참조 · 각 이슈 소진 확인
- [ ] **회귀 테스트** — `./gradlew test` BUILD SUCCESSFUL · 커버리지 유지
- [ ] **Log Product 완주** — `product-log.md` Epic 5 마지막 Story 착지 확인
- [ ] **Op Product 완주** — `product-op.md` Epic 4 마지막 Story 착지 확인
- [ ] **Grafana 새 판넬** — Op E4 커스텀 판넬 반영 확인

### ✅ 릴리스 문서 검증 (20분)

- [ ] **`README.md` 갱신** — 프로덕션 URL · 로컬 개발 가이드 · 사용자 안내
- [ ] **`docs/user-guide/onboarding.md`** — 첫 진입 유저 안내 (스크린샷 포함 · 3명 대상)
- [ ] **`docs/user-guide/faq.md`** — 예상 FAQ 5~10건
- [ ] **`docs/legal/privacy-policy.md`** — 개인정보 처리 방침 · 필수 항목 (수집 항목 · 보유 기간 · 제3자 제공)
- [ ] **`docs/legal/terms-of-service.md`** — 서비스 약관 (필수)
- [ ] **`docs/runbooks/*` 정합** — M7 신설분 + M8 신설 (`secrets-rotation.md` 등)
- [ ] **`docs/DOMAIN.md` 최종 갱신** — 릴리스 상태 반영 (M4·M5 재편 · AI 3층 · AIR Session)

### ✅ 파이프라인 dry-run 검증 (60분)

- [ ] **staging 배포 리허설** — 최근 git_sha로 staging 배포 성공
- [ ] **staging rollback 리허설** — 이전 git_sha로 즉시 rollback 성공
- [ ] **Slack `#thirdtool-deploy` 알림** — 배포·rollback 각각 알림 도착
- [ ] **CloudWatch Alarm 강제 발동** — 5종 중 최소 3종 임계값 임시 조정 후 발동 확인:
  - RDS CPU (임시로 CPU 부하 유발)
  - ALB 5xx (임시로 500 응답 유발)
  - ECS memory (임시로 memory limit 하향)
- [ ] **Slack `#thirdtool-alerts` 알림 도착** — Alarm SNS · Slack webhook 정합
- [ ] **ECS Task auto-recover** — `aws ecs stop-task` 강제 종료 → 60초 이내 자동 재시작
- [ ] **Secrets rotation dry-run** — Secrets Manager 값 수동 갱신 → ECS rolling restart → 새 값 로드 확인
- [ ] **PITR restore drill** — M7 완료분 재검증 (staging 복원 성공)
- [ ] **부하 시뮬레이션** — 5·10·20 concurrent user (jmeter or `k6` 간이):
  - P95 latency < 2s
  - 5xx rate 0
  - JVM heap · DB pool 정상 범위

### ✅ 프로덕션 안정화 관찰 (24h · D6~D7)

- [ ] **24h 관찰** — D6 시점 이후 24h 동안 Grafana 모니터링
- [ ] **CloudWatch Alarm 발동 없음** — 임계값 조정 후 실제 트래픽에서 오발동 없음
- [ ] **Slack `#thirdtool-alerts` 조용함** — 실장애 없음
- [ ] **Log 볼륨 안정** — CloudWatch Logs 볼륨 예상 범위
- [ ] **RDS 백업 정상** — daily backup window 04:00 KST 성공
- [ ] **Vertex AI 응답 안정** — 5xx rate 0 · timeout rate < 1%

---

## 🌟 3명 사용자 UX 실측 (M8 하이라이트 · D4~D6)

release.md §사전 검증 시나리오 5건 · 3명 사용자 각각 완주.

### 실측 준비 (D3~D4)

- [ ] 3명 사용자 확정 · 각자 예약 시간 확보 (30~60분/1명)
- [ ] 프로덕션 계정 사전 생성 (or 사용자가 직접 회원가입)
- [ ] 관찰 노트 템플릿 준비 (`docs/observation/template.md`)
- [ ] 화면 공유 도구 준비

### 시나리오 1 — 첫 진입 · 학습 대상 정의 (사용자당 15분)

**절차**:
1. 회원가입 → 로그인
2. concepts 3개 입력 ("백엔드 개발자", "기획자", "AI 엔지니어링")
3. Layer 2개 만들기 ("기능의 구현", "설계 원리")
4. 각 Layer에 Axis 1~2개 만들기
5. 첫 Axis에 Roadmap 노드 만들기 (Static Adapter 초안 → 편집)

**관찰 지표**:
- 회원가입 → 첫 concept 저장까지 소요 시간
- 사용자가 "concepts란 무엇인가"에 대한 이해도 (1~5)
- Static Adapter 응답 만족도 (1~5)
- 사용자가 자연스럽게 편집으로 진입하는가

**기대 결과**: 3명 모두 시나리오 완주 · 시스템 오류 없음. 조작 미스는 무관 (사용자 학습 곡선).

### 시나리오 2 — 카드 저장 · 다음날 노출 (사용자당 20분 · **날짜 조작 필요**)

**절차**:
1. 시나리오 1 완주 상태에서 카드 3장 생성 (Cornell 노트: MainNote + Summary + Keyword)
2. **날짜 조작**: 사용자 로컬에서 시각을 다음날로 조정 or 개발자 조작으로 시각 조정
3. 다음날 접근 → daily batch에 3장 모두 노출 확인
4. Review 세션 시작 → 2장 clear · 1장 미완료
5. 자정 지나 batch close → 다음날 접근 → 미완료 1장 skip (누적 없음)

**관찰 지표**:
- 카드 생성 UX 자연스러움
- daily batch 큐 표시 명확성
- Review 세션 흐름 이해도
- 자정 close 후 skip 정책 이해

**기대 결과**: 3명 완주 · Review 세션 완주 (2/3 clear 상태로도 무관).

### 시나리오 3 — Mode 다운 · load 즉시 감소 (사용자당 15분)

**절차**:
1. 시나리오 2 완주 상태에서 mode `MODE_14D` → `MODE_7D` 다운
2. 다음날 daily batch에 이미 7일 넘긴 카드 자동 archive (`MODE_DOWNGRADED`)
3. 대시보드에서 오늘 batch 크기 축소 확인

**관찰 지표**:
- Mode 변경 UX (RawInputDaysInput 이해도)
- `MODE_DOWNGRADED` 안내 문구 이해도
- 대시보드 크기 축소 시각적 반영

**기대 결과**: 3명 완주 · Mode 다운 후 load 감소 즉시 확인.

### 시나리오 4 — Cross-layer 짬뽕 (사용자당 15분)

**절차**:
1. 2개 Layer에 카드 각각 5장씩 존재 (총 10장)
2. 다음날 daily batch에 10장 모두 하나의 큐로 조합
3. Review 세션에서 layer 순서 없이 자연스러운 순차 노출

**관찰 지표**:
- Cross-layer 큐가 사용자에게 이질감 없는가
- Layer 표시 vs Axis 표시 시각적 조화
- 순차 노출 흐름 자연스러움

**기대 결과**: 3명 완주 · 짬뽕 큐 이해도 4/5 이상.

### 시나리오 5 — Streak · 대시보드 (사용자당 15분 · **3일 연속 필요**)

**절차**:
1. 3일 연속 batch perfect clear (날짜 조작 or 개발자 조작)
2. 대시보드에서 `streak.current = 3` 표시 확인
3. 4일차 미완료 (0 clear) → streak break · `streak.current = 0`

**관찰 지표**:
- Streak 시각화 자연스러움 (뱃지·연속일 표시)
- Streak break 즉시 반영
- 사용자 동기부여 UX

**기대 결과**: 3명 완주 · Streak 개념 이해도 확인.

---

## 🚀 릴리스 GO/NO-GO 판정 절차 (D6 · 2026-08-17 Mon)

### 정량 지표 수집 (D5~D6)

| 지표 | 데이터 소스 | 목표값 | 실측 |
| --- | --- | --- | --- |
| MVP 사이클 10단계 완주율 | 3명 UX 실측 | 3/3 | (D5~D6 후 기록) |
| 3명 각 시나리오 완주율 | UX 관찰 노트 | 15/15 (5시나리오 × 3명) | (D6 기록) |
| LT/Card/Review 단위 테스트 커버리지 | `./gradlew jacocoTestReport` | ≥ 80% | (D6 실측) |
| 프로덕션 URL 24h uptime | Grafana | 100% | (D6~D7 실측) |
| P95 latency | Grafana | < 2s | (D6 실측) |
| 5xx rate | Grafana | 0 | (D6 실측) |
| Static Adapter 응답률 | 로그 · 관측 지표 | 100% (fallback 포함) | (D6 실측) |
| LLM 응답률 | 로그 | > 95% (실패는 Cascade 폴백) | (D6 실측) |
| 자정 close cron | staging 24h 관찰 | 100% | (D5~D7 실측) |

### GO 조건 (6 신호 중 5개 이상 성립)

- [ ] **핵심 사이클 신호**: 3명 사용자 프로덕션에서 MVP 10단계 완주 (시나리오 1~2 통합)
- [ ] **Mode 매핑 신호**: 시나리오 3 완주 · mode 하이브리드 down/up 검증
- [ ] **Cross-layer 짬뽕 신호**: 시나리오 4 완주 · `DailyLearningBatch.entries` 수식 검증
- [ ] **AI Static + LLM 신호**: 시나리오 1의 Static/LLM 응답 정상 + Cascade 폴백 + Rate Limit + Session axisDraft
- [ ] **관측 신호**: Grafana에서 4개 지표 실시간 (daily_batch · card.archived · review_session · thirdtool.suggestion)
- [ ] **자정 close 신호**: staging 24h 관찰 성공 + 프로덕션 최소 1회 검증

### NO-GO 조건 (하나라도 해당 시 릴리스 08-26 연기)

- [ ] 프로덕션 DB 마이그레이션 실패 (D3~D6 관찰)
- [ ] 3명 중 1명 이상 회원가입·카드 생성·Review 진입 3단계 실패
- [ ] 단위 테스트 커버리지 < 80%
- [ ] P0 장애 이력 (Static 완전 실패 · LLM 무응답 · DB 접속 불가 · 5xx rate > 5%)
- [ ] 프로덕션 24h 이상 down

### 판정 기록

D6 세션 종료 시 `release-checklist.md`에 다음 형식으로 기록:

```markdown
## 🚀 릴리스 GO/NO-GO 판정 (2026-08-17 Mon)

**판정 결과**: [GO / NO-GO / 조건부 GO]

**GO 신호 성립 수**: N/6

### 신호별 판정
- [x] 핵심 사이클: 3/3 완주 ✅
- [x] Mode 매핑: 시나리오 3 완주 ✅
- ...

### NO-GO 조건 발동
- (없음 or 발동 항목 명시)

### 판정자
- 사용자님 (@junseong-kim)

### 릴리스 일정
- **확정**: 2026-08-19 (Wed) or **연기**: 2026-08-26 (Wed)

### 릴리스 후 24h 대기 계획
- 사용자님 대기 · Slack 알림 상시 확인
- Runbook 5 시나리오 준비
```

---

## 릴리스 D-Day 체크리스트 (2026-08-19 Wed · `release-checklist.md`)

**D-Day 절차** (릴리스 당일):

- [ ] **08:00 KST** — 프로덕션 최종 health check (`/actuator/health` · Grafana empty green)
- [ ] **09:00 KST** — GitHub release tag `v0.1.0` 생성 · 릴리스 노트 작성 (M3~M7 산출 요약)
- [ ] **10:00 KST** — 3명 사용자에게 안내 이메일 · 개별 메시지 발송 (`docs/user-guide/onboarding.md` 링크 첨부)
- [ ] **11:00 KST** — 첫 사용자 접속 관찰 (Grafana · CloudWatch Logs 실시간)
- [ ] **12:00~18:00 KST** — 24h 관찰 시작 · Slack 알림 상시 확인 · 문제 발생 시 즉시 대응
- [ ] **24h 후 (08-20 09:00 KST)** — 초기 관찰 결과 정리 · `outcome.md` 최종화 · `review.md` §릴리스 후 관찰 절 신설

---

## M8 확인 못하는 것 (v0.1.1v 이관)

- ❌ AIR Epic 3·4·5 (Roadmap draft · 사용자 액션 · Commit) (**v0.1.1v ~**)
- ❌ 챕터 노드 재생성 API + hint UI (**v0.1.1v · 이슈 #18**)
- ❌ AI 비용 예산 cap 자동 컷 (**v2**)
- ❌ 부하 테스트 · APM · 검색 · 미디어 CDN 세부 · 알림 in-app 채널 (**v2**)
- ❌ 자동 mode 조정 (L4) · L3 규칙 기반 추천 안내 · T3 조건부 주간 요약 (**v0.2.0v**)
- ❌ 검색 · Layer 시각화 (**v0.3.0v**)
- ❌ Admin 페이지 · A/B 실험 (**v2**)

---

## 관찰 지표 로깅 확인 (M7 계승)

M8은 신규 로그 항목 최소. M7 로깅 인프라 정합만 확인.

- **fix 이슈 소진 시 로그** — 각 이슈 재현 시 로그 형식 정합
- **UX 실측 시 로그** — 3명 사용자 접속 시 `requestId`·`userId` MDC 정합 · 각 요청 traceability 확보
- **릴리스 배포 로그** — GHA · Slack `deploy.completed v0.1.0 git_sha=... env=prod`

---

## 문제 발생 시 (Troubleshooting)

| 증상 | 원인 후보 | 해결 |
| --- | --- | --- |
| P0 fix 이슈 소진 실패 | 이슈가 상호 의존 · 근본 원인 미파악 | 시스템적 디버깅 · Runbook 참조 · 필요 시 v0.1.1v로 이관 |
| 3명 UX 실측 blocker 다수 발견 | 시스템 · UX 결함 | 즉시 소진 우선 · 시간 부족 시 GO 신호 재판정 · 조건부 GO 검토 |
| Alarm 강제 발동 실패 | Alarm 설정 오류 · SNS 배선 오류 | Alarm history 확인 · 임계값 재설정 · SNS topic 확인 |
| rollback 리허설 실패 | ECS Task Definition 이전 버전 없음 · Docker image 삭제 | ECR lifecycle policy 확인 · 이미지 존재 여부 · rollback 절차 재검토 |
| 프로덕션 첫 사용자 접속 시 500 | 프로덕션 특유 환경 문제 (staging에서 미검증) | CloudWatch Logs 즉시 확인 · rollback 즉시 판정 · Runbook 대응 |
| Vertex AI daily quota 초과 | 3명 실측 반복 호출 · quota 소진 | GCP Console quota · 임시 quota 증설 요청 · `cost.md` 반영 |
| GO 신호 5개 미만 | 시나리오 실측 실패 · 관측 미달 | 08-19 릴리스 연기 · 08-26 재판정 · v0.1.0v 스코프 축소 검토 |
| 릴리스 후 24h 내 P0 장애 | 실사용 시 발견되는 edge case | Runbook 대응 · 필요 시 프로덕션 rollback · 3명 사용자 안내 |

---

## 마무리 판정 (0.0.8v 동결)

M8 완료는 아래 5개 조건 모두 만족 시 통과:

- [ ] fix 이슈 P0/P1 소진 + Log/Op 완주 + 회귀 테스트 통과
- [ ] 릴리스 문서 5개 파일 완성 + 3명 사용자 사전 안내 자료
- [ ] 파이프라인 dry-run 성공 (배포·rollback·Alarm·auto-recover)
- [ ] 3명 사용자 UX 5시나리오 완주 (15/15 완주율)
- [ ] **🚀 릴리스 GO 판정** — 6 신호 중 5개 이상 성립 · NO-GO 조건 발동 없음

→ 조건 만족 시 M8 동결 · **🚀 2026-08-19 (Wed) 릴리스 발행 · 0.1.0v · 3명 사용자 안내 발송**.

→ 조건 미달 시 08-26 (Wed) 릴리스 연기 · M8.1 패치 발행 · 재판정.

---

## 참고

- 마일스톤 원본: `./milestone.md`
- 이전 마일스톤 UX 확인: `../0.0.7v/ux-check.md`
- **첫 릴리스 계획**: `../../release/version/0.0.1v/release.md` — 0.1.0v (2026-08-19) 사이클 10단계 · §릴리스 성공 기준 6 신호 · §사전 검증 시나리오 5개
- 릴리스 D-Day 체크리스트: `./release-checklist.md` (본 M8 신설)
- 사용자 안내: `docs/user-guide/onboarding.md` · `docs/user-guide/faq.md` (본 M8 신설)
- 정책 문서: `docs/legal/privacy-policy.md` · `docs/legal/terms-of-service.md` (본 M8 신설)
- Runbook: `docs/runbooks/*` (M7 신설 · M8 정합)
- FE 대응 마일스톤: `../../../pes/fe/fe-milestones/version/0.0.8v/milestone.md`
- **다음 마일스톤**: 릴리스 후 관찰 주간 · `../0.1.1v/milestone.md` (신설 예정 · 2주 후)
- brainstorming 트리거: `workflow/task/pes/brainstorming/0.1.1v/` (본 M8 완료 후 신설)
- fix 이슈 다음 축적처: `workflow/task/fix/brainstorming/version/0.0.3v/` (본 M8 완료 후 신설 · v1 사용자 관찰 이슈)
- **🚀 릴리스 D-Day**: **2026-08-19 (Wed)** · 0.1.0v · 3명 사용자
