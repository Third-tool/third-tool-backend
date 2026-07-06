# Issue: 학습 캐시 측정 대시보드 + 규칙 기반 추천 안내 + 조건부 주간 요약

## 배경
사용자 지시 (2026-07-02 fix 회의):
> "이 daily session은 그 날 학습 reviewSession에 날라오는 card를 얼마나 clear를 하는지 , clear를 했다면 며칠 연속으로 완벽하게 clear를 했는지 이 clear 여부를 통해서 자기의 캐시의 양을 측정한다"
> "1주일마다 하루 전부 clear 한 날짜를 알려주고 난이도 조절을 말씀드림 굳이 14일마다 다시 안보는게 낮다고 추천 드리거나, 그냥 1,3,7 마다만 보는게 양이 괜찮다고 이런식으로"

**핵심 목적**: 사용자가 자기 학습 캐시 용량을 **데이터로 파악**하도록 지원. 사용자는 종종 자기 능력 과대평가("1,3,7,14,30까지 가능해!") → 시스템이 실측치로 교정.

**v1 스코프 (L3, T3)**:
- **L3**: 대시보드 + streak + 규칙 기반 추천 안내 (자동 조정은 v2)
- **T3**: 조건부 자동 트리거 — 3주 연속 저완료율 감지 시 알림

## 조사 결과 — 현재 상태

- 대시보드 개념 자체 없음.
- Streak 개념 없음.
- Mode 다운/업 추천 없음. 사용자가 자기 판단으로만 조정.
- 관찰 지표 로깅 없음 (mode별 실측 완료율 데이터 없음).

## 옵션 비교

**Option A — L3 대시보드 + T3 조건부 자동 (채택)**
- 대시보드: 오늘/최근 7일/최근 30일 completion + streak
- 추천 안내: 규칙 기반 (예: 최근 3주 batch 평균 완료율 < 50% → 다운 추천 배지)
- 주간 요약: 조건부 자동 (3주 연속 저완료율 감지 시 알림)
- 자동 mode 조정 없음 → 사용자 자율성 보존

**Option B — L2 (대시보드만, 추천 없음)**
- 사용자님 명시 요구("난이도 조절 말씀드림")와 배치.

**Option C — L4 (자동 조정)**
- 사용자 자율성 침해. v1 관찰 데이터 없이 자동 조정 임계값 결정 어려움.

## 선택: Option A (L3 + T3)

## 부속 결정

### 대시보드 데이터 (`GET /api/v1/learning-dashboard`)

```json
{
  "userMode": "MODE_14D",
  "today": {
    "batchDate": "2026-07-02",
    "isBatchGenerated": true,
    "totalCards": 47,
    "viewedCards": 12,
    "completionRatio": 0.255,
    "isClosed": false
  },
  "recent7Days": {
    "avgCompletionRatio": 0.63,
    "perfectClearDays": 2,
    "totalDays": 7
  },
  "recent30Days": {
    "avgCompletionRatio": 0.58,
    "perfectClearDays": 6,
    "totalDays": 30
  },
  "streak": {
    "current": 2,
    "longest": 5
  },
  "recommendation": {
    "type": "SUGGEST_DOWNGRADE",
    "fromMode": "MODE_14D",
    "toMode": "MODE_7D",
    "reason": "최근 3주 평균 완료율 42% — 캐시 용량이 이 mode를 감당하기 어려운 것으로 보입니다.",
    "displayedAt": "2026-07-02T09:00:00+09:00"
  }
}
```

- `recommendation`은 nullable — 규칙 조건 미충족 시 null.
- `streak.current` — realtime 계산 (BatchStreakCalculator, 이슈 #24).
- `perfectClearDays` — 조회 기간 내 `batch.isPerfectClear()` true 개수.

### 추천 규칙 (v1 default)

| 조건 | 추천 |
|---|---|
| 최근 3주 batch 평균 완료율 < 50% | `SUGGEST_DOWNGRADE` — 한 단계 낮은 mode 안내 |
| 최근 4주 batch **모든 날** perfect clear + 사용자 mode < MODE_60D | `SUGGEST_UPGRADE` — 한 단계 높은 mode 안내 |
| 그 외 | 추천 없음 |

- 임계값(50%, 3주, 4주 전부 perfect)은 **v1 기본값**. 이슈 #26에 명시적으로 `AppConfig` 로 노출해 프로덕션에서 튜닝 가능하게.
- `SUGGEST_UPGRADE`는 매우 보수적 조건 (자기 과대평가 방지).

### 주간 요약 조건부 트리거 (T3)

- 매주 월요일 09:00 KST cron 실행.
- 각 사용자별 최근 3주 batch 평균 완료율 계산.
- `< 50%` 인 사용자에게만 알림 발송 (in-app notification + optional push).
- 알림 내용: 
  ```
  "지난 3주 학습 완료율 42%였어요.
   현재 모드(1,3,7,14)가 너무 무거울 수 있습니다.
   1,3,7만 보는 모드(MODE_7D)를 고려해보세요."
  ```
- **너무 완료율 좋은 유저** → 알림 없음 (조용함 유지).
- **완료율 애매한 유저 (50~80%)** → 알림 없음. 대시보드로만 확인 가능.

### 관찰 지표 로깅 (v2 임계값 근거)

이슈 #20 로깅 정책 계승 + 추가:

| 지표 | 스코프 | 목적 |
|---|---|---|
| Mode별 batch 평균 완료율 | 사용자·mode 조합 | 각 mode가 사용자에게 실제 어느 완료율을 낳는지 |
| Streak 분포 | 사용자별 | current/longest streak 히스토그램 |
| Mode 변경 이력 (up/down 시각·직전·직후) | 사용자별 | 자기 조정 패턴 관찰 |
| `MODE_DOWNGRADED` archive 비율 | 사용자별 | 다운 시 얼마나 많은 카드가 즉시 archive됐는지 |
| 추천 배지 표시·수용 이력 | 사용자별 | 추천이 실제 mode 변경으로 이어지는가 |

### API 표면

| 메서드 | 경로 | 목적 |
|---|---|---|
| GET | `/api/v1/learning-dashboard` | 대시보드 데이터 (위 JSON) |
| GET | `/api/v1/learning-dashboard/weekly-summary?week=2026-W27` | 특정 주간 요약 (사용자 명시 조회) |
| POST | `/api/v1/learning-dashboard/recommendations/{recommendationId}/accept` | 추천 수용 → mode 변경 트리거 |
| POST | `/api/v1/learning-dashboard/recommendations/{recommendationId}/dismiss` | 추천 무시 |

- 추천 accept 시 `UserScheduleConfig.mapped_mode` 변경 → M3 하이브리드 규칙 자동 적용 (이슈 #23).

### FE UX 원칙

- 대시보드 = **선택적 진입** (앱 첫 화면 아님). 사용자가 확인하고 싶을 때만 보는 곳.
- 추천 배지 = 부드러운 안내. 강제 클릭·모달 없음.
- 주간 알림 = 조건부이므로 흔하지 않음. 사용자에겐 "감지된 신호"의 의미.

## 이관 산출물

- **BE-Story #26-1**: `LearningDashboardQueryService` — 대시보드 데이터 집계 (오늘/7일/30일/streak/recommendation 조합).
- **BE-Story #26-2**: `RecommendationEngine` 도메인 서비스 — 규칙 판정 (임계값은 config).
- **BE-Story #26-3**: 주간 요약 cron (`0 0 9 ? * MON`) + 조건 판정 + in-app notification 발송.
- **BE-Story #26-4**: `LearningDashboardController` + DTO — 위 API 표.
- **BE-Story #26-5**: 추천 accept 시 `UserScheduleCommandService.updateMode(userId, newMode)` 호출 orchestration.
- **BE-Story #26-6**: 관찰 지표 로깅 강화 — 위 지표 표. Grafana or 로그 aggregator 대응.
- **BE-Story #26-7**: Application config — 추천 임계값 노출 (`app.learning.recommendation.downgrade-threshold-ratio=0.5`, `.downgrade-window-weeks=3`, `.upgrade-window-weeks=4`).
- **BE-Story #26-8**: 통합 테스트 — 3주 batch 데이터 시나리오 → 추천 발동 검증.
- **BE-Story #26-9**: In-app notification 스키마·전달 채널 — 별도 이슈로 분리할지 검토 (v1 minimal은 DB row로만 저장 + 프론트 polling).
- **FE-Story #26-10**: 대시보드 화면 (오늘 진행률, 7일/30일 그래프, streak 표시, 추천 배지).
- **FE-Story #26-11**: 주간 요약 알림 UI + 수용/무시 액션.
- **SDD 개정**: `product-review.md` (신설) — 캐시 측정·추천 명세.

## 관련 이슈 / 문서

- 데이터 소스: [#24 DailyLearningBatch](./issue-24-daily-learning-batch.md).
- 소비하는 mode: [#21 Interval ladder + Mode 재편](./issue-21-interval-ladder-fixed-mode-reorganize.md).
- 연동: [#23 Card createdMode 하이브리드](./issue-23-card-createdmode-hybrid.md) — 추천 accept 시 mode 변경으로 down cap 트리거.
- 관찰 지표 컨텍스트: [#20 AI 비용 예산 backlog](./issue-20-ai-cost-budget-cap.md) — 로깅 정책 계승.
- SDD 신설: `product-review.md`.
