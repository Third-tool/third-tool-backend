# 0.0.1v / Review (회고)

> M1 종료 시점에 작성. 본 파일이 채워지면 0.0.1v는 동결되고 다음 마일스톤 진입.

---

## 작성 메타

- 작성일: _____ (예상 2026-06-29 월요일)
- 작성자: 운영자 (1인)
- 본 버전 동결 여부: ☐ 동결 / ☐ 0.0.1.1v 패치 발행 (미합격 처리)

---

## 계획 vs 실제 (정량)

| 항목 | 계획 (M1 milestone.md) | 실제 | 차이 사유 |
| --- | --- | --- | --- |
| Story 머지 수 (Tier 1) | 15 | _____ | _____ |
| Story 머지 수 (Tier 2 — Want) | 4 | _____ | _____ |
| 전체 머지 수 | 19 | _____ | _____ |
| dev 환경 가동 시점 | D5(토) | _____ | _____ |
| 첫 `/health` 200 시점 | D5(토) | _____ | _____ |
| 본주 작업 시간 누적 (시간) | (목표 미설정) | _____ | _____ |

---

## 종료 신호 충족 여부

milestone.md에 정의된 8 신호 중 충족된 항목 체크:

- [ ] 머지 신호 (Tier 1 80% = 12 Story 이상 머지)
- [ ] 배포 신호 (ECS Task RUNNING + ALB 라우팅)
- [ ] 헬스 신호 (`/health` 200)
- [ ] 인증 신호 (`POST /login` 성공)
- [ ] 관측 신호 (`/actuator/prometheus` + Grafana)
- [ ] 로그 신호 (JSON + X-Request-Id)
- [ ] 비밀 신호 (Secrets Manager 주입)
- [ ] AI 신호 (Static axis + axisTopic 동시 응답)

**합격 기준**: 8 신호 중 6 이상. 6 미만이면 0.0.1.1v 패치로 연장.

---

## 가장 큰 막힘 (Top 3)

| 순위 | 막힘 | 소요 시간 | 해결 방식 | 학습 |
| --- | --- | --- | --- | --- |
| 1 | _____ | _____ | _____ | _____ |
| 2 | _____ | _____ | _____ | _____ |
| 3 | _____ | _____ | _____ | _____ |

---

## 기대 vs 의외

| 영역 | 기대 | 실제 |
| --- | --- | --- |
| 본주 속도 (Story/주) | 19 | _____ |
| 가장 시간 든 Story | (예상: 6 1-1 VPC 또는 5 2-2 Task Def) | _____ |
| 가장 빨리 끝난 Story | (예상: 2 5-4 DTO 정리) | _____ |
| 비용 일 평균 | $3.27 | $_____ |
| 발견된 신규 리스크 (M2로 이월) | — | _____ |

---

## 다음 마일스톤(M2 / 0.0.2v) 결정 보정

### 다음 주 잡힐 양 (속도 실측 후 조정)

- 본주 속도가 19 Story 통과 → M2는 동등하거나 +10% 잡기 가능
- 본주 속도가 12~15 Story → M2는 15 정도 보수적
- 본주 속도가 12 미만 → M2는 10 + 0.0.1v 잔여 흡수

### M2 우선순위 (M1 결과 반영)

- [ ] M1에서 미완료된 Tier 1 Story → M2 최우선
- [ ] M1에서 깔린 인프라 정련 (Route53·ACM·보안그룹 등)
- [ ] AI Epic 3 진입 (LLM ChatClient 첫 등록)
- [ ] k6 부하 테스트 baseline (메트릭 위에서)
- [ ] AI Suggestion Controller 노출 → FE OnboardingPage v1.5 활성

### brainstorming 0.0.2v 갱신 트리거

본주 결과로 brainstorming 후보들의 상태 전이가 다수 발생. `brainstorming/0.0.2v/` 폴더 신설 권장.

특히 상태 전이 후보:
- **promoted**: ops.md 후보 1(SLO/SLI), 후보 3(헬스체크 깊이) → Product 0-b로 흡수됨
- **promoted**: deploy.md 후보 1(Blue-Green) → 부분 진척 (현재 rolling, M2 격상 후보)
- **promoted**: deploy.md 후보 6(환경별 설정·비밀) → Secrets Manager 도입으로 부분 resolved
- **resolved**: dev.md 후보 8(릴리스 노트 자동화) → 별개로 도입 가능
- **신규 후보**: AWS NAT Gateway 비용 회피 (VPC endpoint vs NAT)
- **신규 후보**: ALB target unhealthy 디버깅 패턴

---

## 본 버전 동결 선언

위 종료 신호 6+ 충족 시 다음을 실행:

- [ ] `version/0.0.1v/` 폴더 내 5 파일 모두 작성 완료
- [ ] git commit: `docs(milestone): 0.0.1v 동결 — Tier 1 N/15 머지`
- [ ] 다음 폴더 생성 시 본 버전을 복사 후 갱신: `cp -r version/0.0.1v version/0.0.2v` → milestone.md만 새로 작성
- [ ] brainstorming 0.0.2v 신설 검토

종료 신호 6 미만이면:
- [ ] 0.0.1.1v 발행: `cp -r version/0.0.1v version/0.0.1.1v` → milestone.md에 연장 사유 + 남은 Story 명시
- [ ] 다음 주를 0.0.1.1v 패치 + 0.0.2v 둘로 나누지 않고 0.0.1.1v 완수 후 0.0.2v 진입
