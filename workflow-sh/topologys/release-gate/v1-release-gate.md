# Pinned Topology — `release-gate` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 신호 임계값·릴리스 버전·사용자 명단은 여기 없다)

**목적**: 릴리스 GO/NO-GO 판정의 반복 원칙을 pin. 첫 사용자 릴리스 (0.1.0v · M8) 부터 이후 매 릴리스에 반복 지켜져야 함. `environments`와 협력.

---

## 0. 유효 구간 (Validity)

| 항목 | 값 |
| --- | --- |
| Pinned at | 2026-07-21 |
| Valid for | 본 워크플로우 v1 구간 — re-pin trigger 발생 전까지 |
| Owner | 메인 Claude + 사용자 합의 |
| Re-pin trigger | 아래 §4 |

---

## 1. How Claude Code must use this file — 행동 계약

1. **릴리스 판정·게이트 관련 작업 시 이 파일을 먼저 읽는다.** §2의 신호·drill·판정 주체는 본 구간 **고정 제약**.
2. **GO 신호 개수를 임의로 낮추려는 정황이 보이면 STOP하고 보고한다.**
3. **NO-GO 조건 하나라도 발동 상태에서 릴리스를 진행하려는 정황이 보이면 보고**한다.
4. **staging drill 우회 · rollback 리허설 없이 릴리스 진행 정황이 보이면 보고**한다.
5. **사용자 최종 판정 없이 자동 GO 판정 정황이 보이면 보고**한다. Claude가 GO/NO-GO 결정하지 않음.
6. **이 파일에 실제 신호 임계값·사용자 명단을 적지 않는다.** 그건 `release/version/{Nv}/release.md`에.

---

## 2. The pinned topology

### Nodes
- `릴리스 후보` — 특정 버전 (0.1.0v 등) 의 배포 대상 코드·인프라·문서 묶음
- `GO 신호` — 릴리스 발행 근거 (성공 신호 · 정량 조건)
- `NO-GO 조건` — 릴리스 발행 차단 근거 (하나라도 발동 시 연기)
- `조건부 GO` — 신호 일부 미충족 상태에서 스코프 축소 후 발행 결정
- `staging drill` — 프로덕션 배포 리허설 (staging 환경에서 배포·rollback 검증)
- `rollback 리허설` — 이전 git_sha 재배포 성공 확인
- `Alarm 강제 발동` — CloudWatch Alarm이 임시로 트리거되는지 확인 (SNS·Slack 알림 도착)
- `사용자 실측` — 초기 사용자 UX 시나리오 완주 확인 (릴리스 이전 사전 검증)
- `사용자 최종 판정` — 릴리스 GO/NO-GO 결정 주체 (사용자님, Claude 아님)
- `릴리스 D-Day` — 실제 발행 일자·절차
- `24h 관찰` — 릴리스 후 초기 대기·모니터링
- `연기 판정` — NO-GO 시 다음 릴리스 일자로 이관

### Edges
- 마일스톤 완주 → `릴리스 후보` : 릴리스 준비 주간 진입
- `릴리스 후보` → `GO 신호` 판정 : 정량 조건 대조
- `릴리스 후보` → `NO-GO 조건` 검사 : 발동 여부 확인
- `릴리스 후보` → `staging drill` · `rollback 리허설` · `Alarm 강제 발동` : 필수 사전 검증
- `릴리스 후보` → `사용자 실측` : 초기 UX 완주 (n명 기준)
- 위 검증 완료 → `사용자 최종 판정` (GO / NO-GO / 조건부 GO)
- `사용자 최종 판정` GO → `릴리스 D-Day` → `24h 관찰`
- `사용자 최종 판정` NO-GO → `연기 판정` (다음 릴리스 일자로)
- `사용자 최종 판정` 조건부 GO → 스코프 축소 후 발행 → `24h 관찰`
- `24h 관찰` 이상 감지 → `프로덕션 rollback` (environments topology 계승)

### Boundaries
- **판정 주체 경계**: GO/NO-GO 최종 판정은 **사용자님**. Claude·자동 시스템이 결정하지 않음.
- **GO 신호 개수 경계**: 릴리스 문서 (`release.md`) 에 명시된 GO 신호 총 N개 중 **명시된 개수 이상 성립** 시 GO. 임의 하향 금지.
- **NO-GO 조건 경계**: NO-GO 조건 **하나라도 발동** 시 릴리스 연기 필수. 조건 무시 금지.
- **staging drill 경계**: 프로덕션 배포 전 staging drill (배포·rollback·Alarm) **필수**. 우회 금지.
- **사용자 실측 경계**: 초기 사용자 시나리오 (release.md 정의) 를 사전 검증. 실측 미완주 시 GO 신호 미충족 취급.
- **조건부 GO 경계**: 조건부 GO 시 축소된 스코프 명시 · 미포함 항목의 다음 릴리스 이관 명시 (`release.md` 갱신).
- **24h 관찰 경계**: 릴리스 직후 24h 는 대기·관찰 강화. 이 기간 P0 장애 발생 시 즉시 rollback 판정.
- **연기 경계**: NO-GO 시 즉시 다음 릴리스 일자 확정 · 이관 항목·완주 조건 명시.

### Invariants
- Claude가 GO/NO-GO 판정을 스스로 결정한 사례 0건
  - 감지법: 판정 이력 리뷰 · 사용자 서명 확인
- GO 신호 개수를 임의 하향 (예: 6 신호 중 5 → 3) 조정한 사례 0건
  - 감지법: release.md 원안 vs 판정 시점 조건 diff
- NO-GO 조건 발동 상태에서 릴리스 진행 사례 0건
  - 감지법: NO-GO 검사 로그 · 판정 기록
- staging drill 없이 프로덕션 배포 사례 0건 (environments topology와 중복 강제)
  - 감지법: staging 배포 이력 vs 프로덕션 배포 이력 매칭
- rollback 리허설 미검증 상태에서 릴리스 진행 사례 0건
  - 감지법: 리허설 이력 확인
- 사용자 실측 미완주 상태에서 릴리스 진행 사례 0건
  - 감지법: 실측 관찰 노트 vs 판정 시점 이력
- 릴리스 후 24h 관찰 없이 다음 작업 진입 사례 0건
  - 감지법: 릴리스 D+1 이력 리뷰

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 GO 신호 개수·NO-GO 조건 상세 → `workflow/task/milestones/release/version/{Nv}/release.md`
- 실제 사용자 명단·연락처 → 개인 관리 (릴리스 문서에 기록 지양)
- 실제 사용자 UX 시나리오 → `release.md` §사전 검증 시나리오
- 실제 staging drill 절차 → `docs/runbooks/deployment-drydrun.md`
- 실제 rollback 명령어 → `docs/runbooks/production-rollback.md`
- 실제 Alarm 정의·임계값 → `observability` topology · CloudWatch 콘솔
- 릴리스 D-Day 절차 상세 → `workflow/task/milestones/version/{Nv}/release-checklist.md`
- 릴리스 이후 로드맵 → `release.md` §릴리스 이후 로드맵
- 왜 이렇게 박혔는지 → `docs/adr/` 릴리스 정책 ADR (미신설)

---

## 4. Re-pin trigger

- GO/NO-GO 판정 자동화 도입 (Claude·CI가 결정 · 사용자 승인 게이트 폐기)
- GO 신호·NO-GO 조건 프레임 변경 (다른 판정 프레임워크 도입)
- staging drill 폐기 (프로덕션 직접 배포)
- 조건부 GO 개념 폐기 (all-or-nothing 판정)
- 24h 관찰 기간 변경 (48h 등)
- Canary 배포 도입으로 릴리스 게이트 개념 대체
- A/B 실험 배포 도입으로 부분 사용자 대상 게이트 신설
- 사용자 실측 미완주 허용 (내부 QA 만으로 GO 판정)
