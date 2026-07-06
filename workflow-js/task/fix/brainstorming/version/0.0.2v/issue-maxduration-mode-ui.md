# Issue: maxDuration→10/20/30D Mode 매핑 화면 부재

## 배경
사용자 리포트:
> "LearningFacade의 workflow/task/pes/workspectrum/sdd/done/product-card.md 여기에서 max-Duration을
> 활용할 때 사용자가 스스로 day를 설정하면 10day mode, 20 day mode, 30day mode로 매칭되는 화면이 없습니다."

## 조사 결과 — Backend 완성, Frontend 미완

### Backend (Epic 4, 완성)
- `LearningModeMappingPolicy` — 1~14→MODE_10D, 15~24→MODE_20D, 25+→MODE_30D
- `LearningMode` enum — MODE_10D/20D/30D + softScheduleIntervals + `toOnFieldBudget()`
- `UserScheduleConfig` 엔티티 + `user_schedule_config` 테이블 (unique on user_id)
- `PUT /api/v1/users/me/schedule` — request `{ inputDays }`, response:
  ```json
  {
    "schedule": {
      "rawInputDays": 13,
      "mappedMode": "MODE_10D",
      "modeDisplayName": "10일 모드",
      "maxView": 3,
      "maxDuration": 10,
      "dailyTarget": 20,
      "softScheduleIntervals": [1, 3, 7]
    },
    "mappingGuide": "13일을 입력하셨습니다. 10일 모드로 운영됩니다.",
    "updatedAt": "..."
  }
  ```

### Frontend (미완)
- `fe-workspectrum/sdd/done/`에 `product-card.md`, `product-learningFacade.md`만 존재
- 학습 스케줄 설정 관련 SDD·화면 부재
- product-card.md Epic 4 스펙은 있지만 FE 반영 문서·화면 없음

## 옵션 비교

### 화면 배치
- **A. Settings 서브메뉴 (채택)** — 사용자 설정 아래 "학습 스케줄 설정". product-card.md Epic 4와 일치.
- B. 온보딩 1회성 + Settings 수정 — 범위 과잉.
- C. 대시보드 위젯 — YAGNI.

### SDD 파일 구조
- **A. 신규 `product-user-schedule.md` 1개 (채택)** — 작업 단위 명확, 기존 done 문서 훼손 없음.
- B. `product-card.md` in-progress로 옮기고 Section 추가 — done 문서 되돌리는 이상한 구조.
- C. `product-settings.md` 설정 전체 SDD — 범위 과잉, YAGNI.

## 이관 산출물
- **FE-Story 1** (`docs/product-user-schedule-sdd`): `product-user-schedule.md` SDD 신규 작성
- **FE-Story 2** (`feature/user-schedule-settings`): Settings > 학습 스케줄 설정 화면 구현 (FE-Story 1 승인 후)
