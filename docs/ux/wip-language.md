# WIP 언어 가이드 (UI 문구 정책)

> Card 운영 위치(ON_FIELD/ARCHIVE)·자동 만료·Soft Schedule·ReviewSession이 사용자에게 **"지금 몇 장을 얼마 동안 붙잡고 있는가"** 라는 WIP 언어로 일관되게 전달되도록 BE 응답 enum과 FE 노출 문구의 매핑을 정의한다.
>
> 단일 진실 소스. 새 ErrorCode·상태 enum·이벤트를 추가할 때마다 본 문서의 매핑 표를 함께 갱신한다.

`product-card.md` Epic 8 "WIP 모델 기반 사용자 설명 체계"의 DoD 항목.

---

## 1. 원칙

### 1.1 WIP 운영 위치 어휘 (도메인 의도)

| 도메인 개념 | 사용자 노출 표현 | 금지 표현 |
| --- | --- | --- |
| `ON_FIELD` | "현재 학습 중", "지금 보는 카드" | "활성", "활성화된 카드" |
| `ARCHIVE` | "보관됨", "배경 지식", "잠시 쉬는 중" | "**실패**", "탈락", "비활성", "삭제" |
| `archive()` | "보관하기", "잠시 쉬게 두기" | "**실패 처리**", "탈락시키기" |
| `returnToField()` | "다시 학습하기", "꺼내오기" | "복구", "활성화" |
| `SCHEDULE_EXHAUSTED` 도달 | "예정된 학습 스케줄을 모두 마쳤습니다" | "**한도 초과**", "**실패 횟수 초과**" |
| `MODE_DOWNGRADED` 도달 | "학습 모드가 낮아져 잠시 쉬어갑니다" | "**기한 초과**", "**만료**" |
| `viewCount` | "본 횟수", "노출 횟수" | "시도 횟수", "실패 횟수" |
| `lastViewedAt` | "마지막으로 본 때" | "마지막 시도" |
| `dailyTarget` | "오늘 학습 목표" | "할당량", "강제 학습 수" |
| `SoftScheduleState` `INTERVAL_*D` | "오랜만에 만나는 카드", "N일째 카드" | "지연된 카드", "밀린 카드" |
| `NOT_YET` | (응답에서 제외 — 화면 미노출) | — |

**핵심 규칙**: "실패" 어휘는 도메인 전반에서 사용자 노출 문구 어디에도 들어가지 않는다. 운영 위치 전환은 모두 "순환" / "이동" 언어로.

### 1.2 책임 분리 (Spec Option B)

- **BE** — `ArchiveReason` / `CardStatus` / `SoftScheduleState` enum 코드 문자열만 응답에 포함.
- **FE** — 메시지 번들에서 enum → 사용자 문구 매핑. 본 문서가 그 번들의 출처.
- **변경 영향**: PO/디자이너 문구 수정은 FE PR 1건. BE 도메인 변경 없음. 다국어 확장 시도 FE 번들만 추가.

---

## 2. enum → 사용자 노출 문구 매핑 (FE 번들 원본)

### 2.1 카드 퇴장 (`ArchiveReason`)

| Enum 값 | 사용자 노출 문구 (단건) | 묶음 노출 문구 (N건) | 발생 경로 |
| --- | --- | --- | --- |
| `MANUAL` | "카드를 보관했어요. 필요하면 언제든 다시 꺼낼 수 있어요." | "{N}개 카드를 보관했어요." | `CardCommandService.archive` (사용자 명시 액션) |
| `SCHEDULE_EXHAUSTED` | "이 카드는 예정된 학습 스케줄을 모두 마쳤어요. 배경 지식으로 이동합니다." | "{N}개 카드가 예정된 스케줄을 모두 마쳐 배경 지식으로 이동했어요." | M5 DailyLearningBatch (M5 이관 · lazy 판정) |
| `MODE_DOWNGRADED` | "학습 모드가 낮아져 이 카드는 잠시 쉬어갑니다. 잠시 후 다시 만나요 👋" | "{N}개 카드가 낮아진 학습 모드에 맞춰 잠시 쉬어가요. 잠시 후 다시 만나요 👋" | M5 DailyLearningBatch (M5 이관 · lazy 판정) |

**문구 작성 원칙**:
- 첫 문장 종결: "~했어요" / "~합니다" 톤 통일.
- 이모지는 `MANUAL` 외에는 자제 — `MODE_DOWNGRADED` 묶음/단건에만 👋 1개. `SCHEDULE_EXHAUSTED`는 무이모지.
- 사용자 직접 액션(MANUAL) → "되돌릴 수 있다"는 안전 신호를 같이 노출.

### 2.2 운영 위치 (`CardStatus`)

| Enum 값 | 뱃지 라벨 | 카드 목록 헤더 |
| --- | --- | --- |
| `ON_FIELD` | "학습 중" | "지금 보는 카드" |
| `ARCHIVE` | "보관됨" | "보관된 카드" |

### 2.3 Soft Schedule 상태 (`SoftScheduleState`)

| Enum 값 | 사용자 노출 표현 | 화면 사용 위치 |
| --- | --- | --- |
| `FRESH` | "처음 보는 카드" | ReviewSession 후보 섹션 헤더 |
| `INTERVAL_1D` | "어제 본 카드" | 〃 |
| `INTERVAL_3D` | "3일 전 본 카드" | 〃 |
| `INTERVAL_7D` | "일주일 전 본 카드" | 〃 |
| `INTERVAL_14D` | "2주 전 본 카드" | 〃 |
| `INTERVAL_21D` | "3주 전 본 카드" | 〃 |
| `NOT_YET` | (응답에서 제외) | — |

응답 DTO `ReviewResponse.TodayCandidates.byState` / `recommendedByState`의 키 enum을 그대로 매핑.

### 2.4 ReviewSession 추천 메시지

| 응답 필드 | 사용자 노출 문구 예시 | 비고 |
| --- | --- | --- |
| `recommendedTotal == dailyTarget` (정확히 매칭) | "오늘은 {dailyTarget}장 학습을 추천해요." | 기본 정상 상태 |
| `recommendedTotal < target` (풀 소진) | "오늘 학습 가능한 카드는 {recommendedTotal}장이에요. 모두 완료하면 오늘 학습 끝!" | Story 6-3 "+N장" 응답 |
| `total == 0` (후보 0건) | "오늘은 학습할 카드가 없어요. 새 카드를 추가하거나 내일 다시 만나요." | 신규 사용자 / 모든 카드 NOT_YET |
| `+N장` 액션 라벨 | "+{count}장 더 학습" | Story 6-3 기본 N=10 |

### 2.5 Deck 진행 상태 (`DeckProgressStatus`)

| Enum 값 | 뱃지 라벨 |
| --- | --- |
| `NOT_STARTED` | "시작 전" |
| `IN_PROGRESS` | "학습 중" |
| `COMPLETED` | "순환 완료" |

`COMPLETED`는 "끝" 인상이 강해 "**순환** 완료"로 표현해 다시 시작할 수 있음을 암시한다.

---

## 3. 묶음 안내 정책 (Spec Epic 8 Alternative B)

- **트리거**: 일정 시간(예: 5분) 안에 동일 사용자의 동일 `reason` 자동 퇴장이 2건 이상 발생.
- **대상**: `SCHEDULE_EXHAUSTED` / `MODE_DOWNGRADED` (M5 DailyLearningBatch가 lazy 판정 후 발행). `MANUAL`은 사용자 명시 액션이라 단건 안내.
- **표현**: 위 §2.1 묶음 노출 문구 컬럼.
- **구현 위치**: FE 알림 큐 또는 push 통합 시점. BE는 알림 발행을 책임지지 않는다 (이벤트만 노출 — v2 `card_state_transition` Metric 합류).

---

## 4. "실패" 어휘 차단 CI 규약 (Spec Epic 8 Alternative B)

> 본 절은 FE 메시지 번들에 적용. BE Java 소스에는 도메인 enum/ErrorCode만 있어 별도 grep 불요.

- FE 빌드 파이프라인에 다음 단계 추가 권장:
  ```
  grep -rE "(실패|탈락|만료)" frontend/src/messages/ && exit 1 || exit 0
  ```
  매치되면 빌드 실패. 도메인 외 문맥(예: "비밀번호 입력 실패")은 별도 디렉토리로 격리.
- 신규 ErrorCode·도메인 메시지 추가 시 본 문서 §2의 표를 함께 갱신해 누락 차단.
- 성공 지표(`product-card.md` §성공 지표 — "퇴장 메시지 '실패' 어휘 = 0건") 자동 검증의 단일 진실 소스.

---

## 5. 운영 규칙 요약 페이지 가이드 (Story 8-2)

FE에서 작성할 "운영 규칙 보기" 한 페이지의 권장 섹션 구조.

1. **이 시스템이 돌아가는 방식 (3줄)**
   - 카드를 만들면 "학습 중(ON_FIELD)"에 올라가요.
   - 일정 기간/노출 횟수를 채우면 자동으로 "보관됨(ARCHIVE)"으로 이동해요.
   - 보관된 카드는 언제든 다시 꺼낼 수 있어요.
2. **왜 카드가 자동으로 이동하나요?**
   - "완벽히 외울 때까지 반복"이 아니라 "WIP 예산 안에서 순환"이 이 시스템의 철학입니다.
   - 한 번에 너무 많은 카드를 붙잡지 않아야 학습 에너지가 분산되지 않아요.
3. **내 학습 모드 (MODE_10D / 20D / 30D)**
   - 입력값에 따라 자동으로 모드가 매핑돼요.
   - 각 모드의 soft schedule 간격을 표로 (Story 4-1 모드 표 그대로).
4. **ReviewSession은 어떻게 카드를 골라주나요?**
   - 본인 직업 컨셉(Layer 1) 안의 모든 Deck에서 수집해요.
   - 오늘 학습 가능한(soft schedule 통과) 카드를 모은 뒤, state별 비율로 `dailyTarget`만큼 추천해요.
   - 예: dailyTarget 20장 + 풀이 1일/3일/7일 각 10·5·5장이면 → 추천도 10·5·5.
5. **묶음 안내**
   - M5 DailyLearningBatch 실행 시 N장이 한 번에 보관되면 묶음 안내 1건으로 받아요. 알림 폭주 없음.
6. **링크**
   - 학습 모드 설정 → `PATCH /api/v1/users/me/schedule` (FE 라우트로 매핑).
   - 일일 학습 목표 설정 → `PATCH /api/v1/users/me/schedule/daily-target`.

---

## 6. 변경 트리거

다음 영역에 변경이 생길 때 본 문서를 같이 갱신한다 (CI grep과는 별개의 컨벤션).

- 새 `ArchiveReason` enum 값 추가 → §2.1 표 갱신.
- 새 `CardStatus` 값 추가 → §2.2 표 갱신.
- 새 `SoftScheduleState` 값 추가 (예: `INTERVAL_30D`) → §2.3 표 갱신.
- `LearningMode` 추가/분기점 변경 → §5.3 (운영 규칙 요약) 모드 표 갱신.
- 새 사용자 노출 ErrorCode 추가 → 본 문서에 매핑이 필요한지 검토.
- 응답 DTO 추가 (`ReviewResponse.*` / `CardResponse.*`)로 사용자 노출 문구 자리가 생기면 §2 / §4에 매핑 추가.

---

## 7. 관련 문서

- `workflow/product/pes/ready/product-card.md` Epic 8 (Spec 원본)
- `docs/DOMAIN.md` § Card / ReviewSession (도메인 어휘 사전)
- `docs/PACKAGE.md` §6 (BC 의존 — Review → Card·UserSchedule·LearningFacade)
- `Common/Exception/ErrorCode/ErrorCode` (사용자 노출 에러 코드 enum, 메시지는 같은 enum 한국어 그대로)

---

## 8. Review 스코프 라벨 매핑 (LT E6 · M5 · Story 6-5)

`ReviewSession.scope` enum 값의 UI 라벨 매핑. "Layer 1" 이라는 모호한 라벨은 대체됨.

| `ReviewScope` | 응답 필드 값 (기술) | UI 라벨 (사용자 노출) | 부가 설명 |
| --- | --- | --- | --- |
| `AXIS` | `"AXIS"` | "축 리뷰: {axisName}" | 단일 축 세션 · 기존 흐름 |
| `LAYER` | `"LAYER"` | "그룹 리뷰: {layerName}" | 그룹핑 여러 축의 카드가 통합된 세션 (신설) |

**Layer.progressStatus 라벨**:

| `LayerProgressStatus` | UI 라벨 | 원칙 |
| --- | --- | --- |
| `NOT_STARTED` | "학습 전" · "아직 시작 안 함" | 축 0건 · 모든 axis NOT_STARTED |
| `IN_PROGRESS` | "학습 중" · "진행 중" | 축 하나 이상 IN_PROGRESS · 혼재 |
| `COMPLETED` | "학습 완료" · "그룹 완주" | 모든 axis COMPLETED |

**금지 표현**:
- "Layer 1", "Layer 2" 같은 순서 표시 — Layer는 사용자가 만든 그룹 이름으로 표시
- "레이어" — "그룹"으로 통일 (기술 용어 사용자 노출 X)
- "완료율 100%" 같은 정량 강조 — "학습 완료" 정성 표현으로

**궤적 관리 (milestone.md § M5 PR#2 리스크)**:
S6-1·S6-2·S6-3의 scope enum·엔드포인트는 후속 issue-25 supersede로 M5 PR#4 (Review E2)가 폐기 예정.
cross-layer 짬뽕 큐 방향으로 재편 · UI 라벨도 그때 재검토 · 본 매핑은 M5 궤적 유지 기간의 임시 안내.
