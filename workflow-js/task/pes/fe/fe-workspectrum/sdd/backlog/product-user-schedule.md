# [Product] User Schedule FE — maxDuration 입력 & 학습 모드 매핑 UI

## Product Vision

> 사용자가 학습 목표 기간(일수)을 입력하면 시스템이 10일/20일/30일 모드로 자동 매핑하고, 결정된 `maxView`·`softSchedule`을 사용자에게 안내한다. 백엔드 Epic 4 (`workflow/task/pes/workspectrum/sdd/done/product-card.md` §1083-1191)는 완성되어 `PUT /api/v1/users/me/schedule`이 매핑 결과를 반환하지만, 이를 노출·수정할 프론트 화면이 없다. 본 Product는 그 공백을 채우는 **Settings 하위 "학습 스케줄 설정" 서브메뉴**를 추가한다.

## 배경 및 문제

- **현재 상황 (As-Is)**
  - 백엔드: `LearningModeMappingPolicy` (1~14→MODE_10D, 15~24→MODE_20D, 25+→MODE_30D), `UserScheduleConfig` 엔티티, `PUT /api/v1/users/me/schedule` 완성
  - 백엔드 응답: `{ schedule: { rawInputDays, mappedMode, modeDisplayName, maxView, maxDuration, dailyTarget, softScheduleIntervals[] }, mappingGuide, updatedAt }`
  - FE: `fe-workspectrum/sdd/done/`에 `product-card.md`, `product-learningFacade.md`만 존재. Settings 화면·스케줄 관련 SDD·구현 부재.
- **발생하는 문제**
  - 사용자 리포트: "max-Duration을 활용할 때 사용자가 스스로 day를 설정하면 10day mode, 20 day mode, 30day mode로 매칭되는 화면이 없습니다."
  - 백엔드가 개인화 예산을 제공할 준비는 됐지만, 사용자가 자기 값을 볼·바꿀 UI가 없어 실질적으로 기본값만 사용됨 → 개인화 미실현.
  - 매핑 임계값(1~14→10D 등)이 프론트에 노출되지 않아 사용자가 "왜 13일 입력했는데 10일 모드가 됐지?"에서 이유를 못 앎.
- **왜 지금 해결해야 하는가**
  - 백엔드 Epic 4가 이미 done. Frontend 공백만 채우면 즉시 개인화 활성.
  - 진행 중 fix(`fix-axis-deck-full-integration.md`)와 도메인 겹침 없음 — 병렬 진행 가능.
  - Card 도메인의 archive 사유(MAX_VIEW/MAX_DURATION) 관측 UX가 결국 mode에 의존 — 이 화면이 없으면 다음 UX 결정도 봉쇄.

## 목표 (To-Be)

- Settings 화면에 "학습 스케줄 설정" 서브메뉴 추가.
- 사용자가 `maxDuration` 일수를 자유 입력 (정수, 1~∞).
- 저장 시 매핑 결과 안내 표시: "N일을 입력하셨습니다. XX일 모드로 운영됩니다."
- 현재 설정값(rawInputDays, mappedMode, softScheduleIntervals)을 진입 시 즉시 표시.
- 임계값(1~14→10D, 15~24→20D, 25+→30D)을 사전에 시각적으로 안내 (사용자가 결과를 예측 가능하게).
- softScheduleIntervals(예: [1, 3, 7])를 시각화해 "학습 리듬"을 한눈에 이해.

## 설계 결정 (Design Decisions)

- **Settings 화면 하위 서브메뉴로 배치**
  - `/settings/schedule` 라우트. AppShell의 사이드 nav에서 "학습 스케줄" 항목.
  - 온보딩 강제 진입 없음 — 사용자는 기본값(백엔드 default, `dailyTarget=20`)으로 시작 가능.
- **입력 시점 검증은 zod + FE, 서버 검증은 백엔드 응답 신뢰**
  - FE zod: `z.number().int().min(1)` — blank/음수/문자 즉시 차단.
  - 백엔드 응답 `mappingGuide`를 UI에 그대로 표시(가공 금지 — 백엔드 컨벤션 존중).
- **매핑 결과 표시는 저장 후 응답 기반, 낙관적 UI 없음**
  - `PUT /schedule` 응답이 확정 값이라 낙관적 매핑 예측 불필요. 저장 → 응답 → 결과 카드 렌더.
- **임계값 사전 안내는 정적 문자열 + 툴팁**
  - 매핑 임계값 자체는 백엔드 상수(`LearningModeMappingPolicy`)라 FE가 동일 값을 렌더해도 안전 — 백엔드가 임계값 바꾸면 배포 시 문서 매칭 필요(관련 FE-ADR 후보).
- **softScheduleIntervals 시각화 = 가로 타임라인 dot**
  - Day 0에서 시작 → intervals[0]일차 dot → intervals[1] 등 순차 표시. 별도 라이브러리 미사용, 순수 CSS.
- **이미 설정된 경우 진입 시 GET 없이 캐시 사용? — 아니오, 매 진입 GET**
  - 다른 세션에서 변경했을 가능성 있음. `GET /schedule` 매 진입 호출(200 캐시 short TTL 검토는 후속).
- **미설정 사용자(신규 가입) UX**
  - 백엔드 GET이 404 또는 default 응답인지 확인 필요 (§Open Questions). 후자 가정 시 "기본값입니다. 조정하려면..." 인라인 안내.
  - 전자 가정 시 "아직 설정 안 함" 상태로 표시, 첫 저장 시 POST 대신 PUT(백엔드 upsert 확인).

## 대안 검토 (Alternatives Considered)

### 화면 배치

**Option A (선택) — Settings 서브메뉴 (`/settings/schedule`)**
- 보상: product-card.md Epic 4 스펙과 일치. 명확한 위치. 다른 설정과 동일 UX 패턴.
- 비용: Settings 화면 자체가 아직 미완이면 AppShell 라우팅부터 정비 필요 (§Open Questions).

**Option B — 온보딩 1회성 강제 + Settings 수정**
- 거부 이유: 신규 가입 흐름 재정비 부담. 스코프 확대.
- 백엔드가 default 값을 제공하면 강제 온보딩 불필요.

**Option C — 대시보드 상단 위젯 (`Mode: 10일 모드` 배지 클릭 → 모달)**
- 거부 이유: 상시 노출 필요 없음. UI 공간 낭비.

### 매핑 임계값 사용자 노출 방식

**Option A — 임계값 미노출, 저장 후 결과만**
- 거부 이유: 사용자가 "왜 13일이 10일 모드?" 이유를 알 수 없음. 재시도 예측 불가.

**Option B (선택) — 임계값 툴팁 + 저장 후 mappingGuide 렌더**
- 보상: 예측 가능성 + 저장 후 명확한 안내. 백엔드 mappingGuide 재활용.
- 비용: 임계값을 FE에도 하드코딩(백엔드와 동일). 백엔드 임계값 변경 시 FE 문서 동기화 필요.

**Option C — 실시간 미리보기 (입력 중 예측 렌더)**
- 거부 이유: FE가 매핑 로직을 재구현하는 것은 백엔드 단일 진실 소스 원칙 위반. 임계값 변경 시 드리프트.

### softScheduleIntervals 시각화

**Option A (선택) — 가로 타임라인 CSS dot**
- 보상: 라이브러리 무의존. 3~5 dot 수준이라 CSS로 충분.

**Option B — chart 라이브러리 (recharts 등)**
- 거부 이유: 오버킬. 스코프 확대.

**Option C — 표(days=1, 3, 7) 텍스트만**
- 거부 이유: 시각 표현이 이해 도움. 텍스트 나열은 정보 밀도 낮음.

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 / 라우트 배치

```
[app/router]
  /settings                     → SettingsLayout
    /settings/schedule          → UserSchedulePage           ◀ ★ 신설
    /settings/profile           → (기존 또는 스켈레톤)
    /settings/notifications     → (기존 또는 스켈레톤)

[features/user-schedule/]
  ├─ pages/
  │   └─ UserSchedulePage.tsx            — 화면 컨테이너
  ├─ components/
  │   ├─ ScheduleForm.tsx                — inputDays 입력 폼
  │   ├─ MappingResultCard.tsx           — 저장 후 매핑 결과
  │   ├─ ThresholdGuide.tsx              — 임계값 정적 안내
  │   └─ SoftScheduleTimeline.tsx        — softScheduleIntervals 시각화
  ├─ hooks/
  │   ├─ useSchedule.ts                  — GET 조회
  │   └─ useSaveSchedule.ts              — PUT 저장 (mutation)
  ├─ api/
  │   └─ userSchedule.ts                 — endpoint 함수
  └─ schemas/
      └─ userSchedule.ts                 — zod 스키마

[lib/api/schemas/]
  └─ userSchedule.ts                     — 공용 재사용 시 여기

[backend-boundary/]
  └─ user-schedule.md                    — /api/v1/users/me/schedule 계약 스냅샷
```

### 핵심 플로우

**1. 진입 → 현재 설정 로드 → 렌더**
```
/settings/schedule
  └ UserSchedulePage
      ├ useSchedule() → GET /api/v1/users/me/schedule
      │    ├ 200 { schedule: {...}, mappingGuide }
      │    │  → ScheduleForm 초기값=rawInputDays, MappingResultCard 표시
      │    └ 404 (미설정)
      │       → ScheduleForm 초기값=default(예: 14), MappingResultCard 미표시
      │         + "아직 설정 안 됨. 값을 입력하고 저장하세요."
      └ ThresholdGuide (정적)
```

**2. 저장 → 매핑 결과 렌더**
```
사용자 입력 (예: 13) → 저장 클릭
  └ useSaveSchedule.mutate({ inputDays: 13 })
      └ PUT /api/v1/users/me/schedule
         └ 200 { schedule: {mappedMode:"MODE_10D", modeDisplayName:"10일 모드", ...}, mappingGuide:"13일을 입력하셨습니다. 10일 모드로 운영됩니다.", updatedAt }
            → MappingResultCard 리렌더
               - "10일 모드" 배지
               - mappingGuide 문구
               - SoftScheduleTimeline([1,3,7])
               - maxView / maxDuration / dailyTarget 필드값
```

**3. 실패 케이스**
```
PUT 실패 (400 검증 실패 — inputDays<1 등 백엔드 검증):
  → ApiError code SCHEDULE_INPUT_INVALID (또는 유사) → 인라인 필드 에러 표시
PUT 실패 (5xx):
  → 토스트 "저장에 실패했습니다. 잠시 후 다시 시도하세요."
PUT 실패 (401):
  → 인터셉터가 refresh 시도 → 재시도. 최종 실패 시 로그인 리다이렉트 (전역 정책).
```

### 외부 의존

- **백엔드 `/api/v1/users/me/schedule`**: GET · PUT. 인증 헤더 표준 (기존 인터셉터).
- **CSS**: Tailwind 또는 기존 style system 재사용.
- **product-auth**: 401 인터셉터 + refresh 재시도 재사용.

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ApiError code (예상) | HTTP | 클라이언트 권장 동작 (UX) |
|---|---|---|---|
| inputDays 미입력 / 0 / 음수 | FE 사전 차단 (zod) | — | 필드 하단 인라인 에러 |
| inputDays 소수·문자 | FE 사전 차단 (zod) | — | 필드 하단 인라인 에러 |
| 백엔드 검증 실패 | (Open Q: 확인 필요) | 400 | 필드 하단 인라인 에러 |
| 인증 만료 | (전역 인터셉터) | 401 | refresh 1회 시도 → 실패 시 로그인 |
| 서버 오류 | `INTERNAL_SERVER_ERROR` | 5xx | 토스트 "다시 시도하세요" |
| 미설정 사용자 첫 진입 GET | (Open Q) | 200 default 또는 404 | 200이면 값 표시, 404면 "아직 설정 안 됨" 상태 |
| 네트워크 오프라인 | (fetch failure) | — | 토스트 "네트워크 확인" |

MSW handler로 4개 서버 시나리오 재현. `msw/handlers/userSchedule.ts`.

### 로깅 정책 (FE)

- **항상 기록** (Sentry 또는 console):
  - 저장 실패 시 `{ code, status, requestId, inputDays }` (inputDays는 개인정보 아님)
- **debug**: 저장 성공 시 mode 매핑 결과 (개발 편의)
- **절대 금지**: 사용자 개인 식별자·이메일 reflection (없음, 문제 없음)

### 관측 지표

- `settings_schedule_save_success_rate` — PUT 200 비율 ≥ 95%
- `settings_schedule_page_lcp_p95` — 페이지 LCP ≤ 1.5s (Web Vitals)
- `mapping_result_display_rate` — 저장 후 MappingResultCard 표시율 = 100%
- inputDays 값 분포 (관측만, 개인화 개선 근거)

## 롤아웃 / 마이그레이션 (Rollout)

### 전제
- 사용자 M1 단일. 기존 사용자 데이터 마이그 불필요.
- 백엔드 `PUT /schedule`은 upsert 방식으로 확인됨(가정, §Open Questions Q1).

### Product 의존성
- **선행**: `../in-progress/product-auth.md` (인터셉터 재사용). 백엔드 Epic 4는 이미 완성.
- **후행**: Card archive 사유 관측 UX (mode 값 노출을 기반으로 재검토), `./product-notification.md` (트리거 명세가 mode 값에 의존).

### Epic·Story 의존성 그래프

```
Epic 1 (설정 조회) ──► Epic 2 (설정 저장 & 매핑 결과 표시) ──► Epic 3 (softSchedule 시각화)
```

### 환경별 설정 분기
| 항목 | dev (`.env.development`) | prod (`.env.production`) |
|---|---|---|
| `VITE_API_BASE_URL` | http://localhost:8080 | https://api.thirdtool.dev |
| MSW | enabled (GET/PUT /users/me/schedule 핸들러) | disabled |
| Sentry | local | enabled |

## 성공 지표 (KPI)

- Settings 진입 후 저장 성공률 ≥ 95% (실패 시 인라인/토스트가 이유 안내).
- 사용자가 값 변경 후 "왜 이 모드로 매핑됐는지" 이해도(정성) — mappingGuide + ThresholdGuide 조합.
- 페이지 LCP P95 ≤ 1.5s (Web Vitals).
- MappingResultCard 표시율 = 100% (저장 성공 후).

## Scope

**In Scope**:
- `/settings/schedule` 라우트 신설
- GET/PUT `/api/v1/users/me/schedule` 연동
- ScheduleForm + MappingResultCard + ThresholdGuide + SoftScheduleTimeline
- zod 스키마 + MSW handler
- 단위 테스트 (Vitest + Testing Library)

**Out of Scope**:
- Settings 화면 전체 재설계 (본 Product는 서브메뉴 1개만 추가; SettingsLayout이 미존재하면 스켈레톤만)
- 온보딩 흐름 개편 — 사유: 스코프 과잉
- softSchedule 편집 (interval 커스터마이징) — 사유: 백엔드 미지원, mode enum 기반
- `dailyTarget` 편집 — 사유: 별도 story로 분리 필요 여부 검증(백엔드 필드 존재 확인 후 결정)
- 모드 전환 시 예정된 archive 재계산 프리뷰 — 사유: 백엔드가 실시간 미리보기 미제공

## 대상 사용자

- **학습자(M1)**: 자기 학습 리듬에 맞춰 예산(일수) 조정 → 개인화된 archive 시점 확보. mode를 이해하고 학습 계획 세울 수 있음.
- **미설정 신규 사용자**: 진입 시 기본값 안내 → 첫 조정 자연스러운 진입점.
- **관리자**: (해당 없음 — 개인 설정)

## 연결된 Epic 목록 (진행 순서)

| 순서 | Epic | 제목 | Story 수 | 선행 의존 |
| --- | --- | --- | --- | --- |
| 1 | Epic 1 | 현재 스케줄 설정 조회 & 표시 | 3 | (없음) |
| 2 | Epic 2 | 스케줄 저장 & 매핑 결과 안내 | 3 | Epic 1 |
| 3 | Epic 3 | softScheduleIntervals 시각화 & 임계값 안내 | 2 | Epic 2 |

- [ ] Epic 1: 현재 스케줄 설정 조회 & 표시
- [ ] Epic 2: 스케줄 저장 & 매핑 결과 안내
- [ ] Epic 3: softScheduleIntervals 시각화 & 임계값 안내

## 관련 문서

- **의존 Product 스펙 원본**: `workflow/task/pes/workspectrum/sdd/done/product-card.md` §1083-1191 (Epic 4 — maxDuration 입력 & 모드 매핑)
- **백엔드 코드 진실 소스**:
  - `src/main/java/com/example/thirdtool/UserSchedule/domain/model/LearningMode.java` — MODE_10D/20D/30D + softScheduleIntervals
  - `src/main/java/com/example/thirdtool/UserSchedule/domain/model/LearningModeMappingPolicy.java` — 매핑 임계값
  - `src/main/java/com/example/thirdtool/UserSchedule/presentation/UserScheduleController.java` — PUT /api/v1/users/me/schedule
  - `src/main/java/com/example/thirdtool/UserSchedule/presentation/dto/UserScheduleResponse.java` — ScheduleDto
  - `src/main/resources/db/migration/V12__user_schedule_config.sql`
- **연계 브레인스토밍**: `workflow/task/fix/brainstorming/version/0.0.2v/issue-maxduration-mode-ui.md`
- **관련 ADR**: 신규 발행 여부 검토 (필요 시 "FE-ADR — 백엔드 상수 임계값 프론트 중복 렌더 정책")

## 열린 질문 (Open Questions)

1. **미설정 사용자에 대한 GET 응답 형식**:
   - 404 (미존재)? 200 default? 스켈레톤 렌더 여부 결정 트리거.
   - 백엔드 확인 필요.
2. **PUT의 upsert 성격 확인**:
   - 백엔드 코드에 `save`로 upsert하는지, insert/update 분리인지 재확인.
3. **`SettingsLayout` 및 사이드 nav 존재 여부**:
   - 현재 fe-workspectrum에 Settings 관련 라우팅 스켈레톤이 있는지, 없다면 최소한만 신설.
4. **매핑 임계값 하드코딩 시 백엔드 변경 감지**:
   - `LearningModeMappingPolicy` 상수 변경 시 FE도 갱신되도록 백엔드-FE 계약 스냅샷(`backend-boundary/user-schedule.md`)에 기록. FE-ADR 후보.
5. **`dailyTarget` 필드 편집 UX 포함 여부**:
   - 백엔드 DTO에 존재하지만 편집 API 별도인지 확인. Scope 재정의 대상.
6. **모드 변경 시 기존 카드에 미치는 영향 사용자 안내**:
   - `maxView`가 3→5로 커지면 archive 시점이 미뤄짐. 사용자에게 프리뷰 필요 여부. 백엔드 미지원 시 안내 문구만.
7. **FE 코드 구조 (features/ 폴더 규약)**:
   - 기존 features 디렉토리 컨벤션과 일치 여부. 코드베이스 스캔 후 착수 시 재확인.

---

*작성일: 2026-07-01 | 기반: `issue-maxduration-mode-ui.md` 회의 · 백엔드 Epic 4 완성 확인 | 상태: **backlog** (병렬 착수 가능)*

---

# [Epic 1] 현재 스케줄 설정 조회 & 표시

## Epic 목표

`/settings/schedule` 라우트 진입 시 `GET /api/v1/users/me/schedule` 호출 → 현재 설정을 ScheduleForm 초기값으로 반영. 미설정 사용자에게는 안내 문구.

## 배경

Product 진입점. 후속 Epic(저장·시각화)은 본 Epic이 확립한 페이지·hook 위에서 확장.

## 완료 기준

- [ ] Story 1-1, 1-2, 1-3 완료
- [ ] `useSchedule` hook 단위 테스트 (Vitest + MSW)
- [ ] 미설정 사용자 시나리오 E2E

## [Story 1-1] `/settings/schedule` 라우트 + SettingsLayout 최소 확립

### User Story
- As a 사용자
- I want `/settings/schedule` 진입 시 페이지가 로드되기를
- so that Settings 사이드 nav에서 학습 스케줄로 진입 가능하다

### 설명
- `features/user-schedule/pages/UserSchedulePage.tsx`
- SettingsLayout 미존재 시 최소 스켈레톤만 신설 (열린 질문 Q3)
- 사이드 nav에 "학습 스케줄" 항목 추가

### 완료 기준 (AC)
- Given `/settings/schedule` 진입 / When 렌더 / Then UserSchedulePage 표시
- Given 사이드 nav 클릭 / When 이동 / Then `/settings/schedule`
- *(엣지 - 미로그인)* Given 세션 없음 / When 진입 / Then ProtectedRoute가 `/login` redirect

### 의존성
- 선행: (없음)
- 후행: Story 1-2

## [Story 1-2] `useSchedule()` hook

### User Story
- As a UserSchedulePage
- I want 현재 스케줄 설정을 hook으로 위임 조회하기를
- so that 페이지가 fetch 세부를 몰라도 되도록

### 설명
- `features/user-schedule/hooks/useSchedule.ts` — TanStack Query
- `GET /api/v1/users/me/schedule`
- Zod parse (userSchedule 스키마)
- 미설정 시 404 or 200 default 처리 (열린 질문 Q1)

### 완료 기준 (AC)
- Given 정상 응답 / When fetch / Then `{ schedule, mappingGuide }` 반환
- Given 404 / When fetch / Then `{ isEmpty: true }` 상태
- *(엣지 - 5xx)* Given 5xx / When fetch / Then error state + 재시도 CTA

### 의존성
- 선행: Story 1-1
- 후행: Story 1-3

## [Story 1-3] ScheduleForm 초기값 렌더 + MappingResultCard 조건부

### User Story
- As a 사용자
- I want 진입 시 현재 설정값(rawInputDays, mode)과 매핑 결과 카드를 즉시 보기를
- so that 재조정 판단이 빠르다

### 설명
- `ScheduleForm.tsx` — inputDays 입력 필드 (초기값 = rawInputDays or default)
- `MappingResultCard.tsx` — mappedMode 배지 + mappingGuide + 필드값
- 미설정 상태일 때 MappingResultCard 미표시 + 안내 문구

### 완료 기준 (AC)
- Given useSchedule 데이터 / When 렌더 / Then ScheduleForm 초기값 = rawInputDays, MappingResultCard 표시
- Given isEmpty=true / When 렌더 / Then "아직 설정 안 됨" 안내 + 기본값 입력창
- *(엣지 - 로딩)* Given fetching / When 렌더 / Then 스켈레톤

### 의존성
- 선행: Story 1-2
- 후행: Epic 2

---

# [Epic 2] 스케줄 저장 & 매핑 결과 안내

## Epic 목표

사용자가 inputDays 값을 저장하면 `PUT /schedule` 호출 → 응답의 mappingGuide + mappedMode를 MappingResultCard로 표시. 실패 시 인라인 에러.

## 배경

Product의 핵심 UX. 개인화 매핑을 사용자에게 명시적으로 드러내는 지점.

## 완료 기준

- [ ] Story 2-1, 2-2, 2-3 완료
- [ ] `useSaveSchedule` mutation 단위 테스트
- [ ] MSW handler로 저장 성공/400/5xx 재현

## [Story 2-1] `useSaveSchedule()` mutation

### User Story
- As a ScheduleForm
- I want inputDays 저장을 mutation hook으로 위임하기를
- so that 저장 상태와 응답을 폼이 소비만 하면 되도록

### 설명
- `features/user-schedule/hooks/useSaveSchedule.ts`
- `PUT /api/v1/users/me/schedule { inputDays }`
- 응답 { schedule, mappingGuide, updatedAt }
- 성공 시 useSchedule cache 갱신

### 완료 기준 (AC)
- Given valid inputDays / When mutate / Then 200 + cache 갱신
- Given 백엔드 400 / When mutate / Then error state + code 노출
- *(엣지 - 401)* Given 401 / When mutate / Then 인터셉터가 refresh 시도

### 의존성
- 선행: Epic 1
- 후행: Story 2-2

## [Story 2-2] ScheduleForm 저장 + zod 사전 검증

### User Story
- As a 사용자
- I want inputDays에 0/음수/문자 입력 시 즉시 인라인 에러를 보기를
- so that 저장 시도 전에 오류를 인지한다

### 설명
- `ScheduleForm.tsx` + React Hook Form + zod
- `z.number().int().min(1)` — blank/음수/문자 사전 차단
- valid 상태에서만 저장 버튼 활성

### 완료 기준 (AC)
- Given inputDays = -1 / When submit / Then zod 인라인 에러, mutation 호출 안 함
- Given inputDays = "abc" / When 입력 / Then 인라인 에러
- *(엣지 - 백엔드 검증)* Given FE 통과 후 백엔드 400 / When mutate / Then 인라인 에러 + code

### 의존성
- 선행: Story 2-1
- 후행: Story 2-3

## [Story 2-3] MappingResultCard 응답 기반 렌더

### User Story
- As a 사용자
- I want 저장 후 mappingGuide 문구와 결정된 mode를 즉시 확인하기를
- so that "13일 → 10일 모드" 이유를 이해한다

### 설명
- `MappingResultCard.tsx`
- 응답 `mappingGuide` 원문 그대로 표시 (가공 금지, 백엔드 컨벤션 존중)
- 배지: `modeDisplayName` ("10일 모드")
- 필드: maxView, maxDuration, dailyTarget

### 완료 기준 (AC)
- Given 저장 성공 / When 렌더 / Then MappingResultCard에 mappingGuide + 배지 + 필드값
- Given mappedMode 변경 (10D → 20D) / When 재저장 / Then 배지·필드 갱신
- *(엣지 - 접근성)* Given 스크린리더 / When 저장 완료 / Then aria-live로 mappingGuide announced

### 의존성
- 선행: Story 2-2
- 후행: Epic 3

---

# [Epic 3] softScheduleIntervals 시각화 & 임계값 안내

## Epic 목표

`SoftScheduleTimeline`으로 학습 리듬(intervals: [1, 3, 7])을 가로 dot 타임라인 시각화 + `ThresholdGuide`로 매핑 임계값 사전 안내.

## 배경

정보 밀도 개선. Epic 2의 저장 결과 위에 시각적 컨텍스트를 얹어 이해도 상승.

## 완료 기준

- [ ] Story 3-1, 3-2 완료
- [ ] SoftScheduleTimeline 렌더 접근성 통과 (aria-label)
- [ ] ThresholdGuide 툴팁 키보드 탐색 통과

## [Story 3-1] SoftScheduleTimeline 시각화

### User Story
- As a 사용자
- I want softScheduleIntervals ([1, 3, 7])를 가로 타임라인 dot으로 보기를
- so that 학습 리듬을 한눈에 이해한다

### 설명
- `features/user-schedule/components/SoftScheduleTimeline.tsx`
- Day 0 시작 → intervals[0]일차 → intervals[1] → ...
- 순수 CSS (라이브러리 무의존)
- 각 dot에 hover 시 "N일차" 툴팁

### 완료 기준 (AC)
- Given intervals=[1,3,7] / When 렌더 / Then 3 dot 표시
- Given intervals=[] (v2 미지원) / When 렌더 / Then 안내 문구
- *(엣지 - 접근성)* Given 스크린리더 / When 렌더 / Then aria-label "1일차, 3일차, 7일차 학습 예정"

### 의존성
- 선행: Epic 2
- 후행: Story 3-2

## [Story 3-2] ThresholdGuide 정적 안내

### User Story
- As a 사용자
- I want 매핑 임계값(1~14→10D, 15~24→20D, 25+→30D)을 사전에 보기를
- so that inputDays 조정 결과를 예측 가능하다

### 설명
- `features/user-schedule/components/ThresholdGuide.tsx`
- 정적 표시 (백엔드 상수와 동일 하드코딩)
- "?" 아이콘 hover 시 툴팁으로 세부 설명
- 백엔드 임계값 변경 시 FE 문서 동기화 규칙 명시 (FE-ADR)

### 완료 기준 (AC)
- Given 페이지 진입 / When 렌더 / Then ThresholdGuide 표시
- Given "?" hover / When hover / Then 툴팁 세부 설명
- *(엣지 - 임계값 드리프트)* Given 백엔드 임계값 변경 후 FE 미갱신 / Then 저장 응답이 예상과 다름 → E2E 회귀 catch

### 의존성
- 선행: Story 3-1
- 후행: (없음)
