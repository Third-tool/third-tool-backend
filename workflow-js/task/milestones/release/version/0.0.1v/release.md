# 첫 사용자 릴리스 계획 — 0.1.0v MVP

> **릴리스 문서의 역할**: `workflow/task/milestones/version/`이 **주간 작업량**을 다룬다면, 본 `release/version/0.0.1v/`는 **"첫 사용자에게 노출할 기능 라인 + 릴리스 시점"** 을 결정한다.
>
> 원칙: 전부 완성 X. **사용자들이 조금씩 쓸 수 있을 정도**의 최소 기능 세트로 초기 3명 사용자에게 릴리스한다. Learn-in-production 사이클을 빠르게 열어 v2/v3 튜닝 근거를 확보하는 게 목적.

---

## 타겟 릴리스

| 항목 | 값 |
|---|---|
| **릴리스 버전** | `0.1.0v` (첫 사용자 릴리스) |
| **타겟 일정** | **2026-08-19 (수) ± 3일** — M8 완료 시점 |
| **대상 사용자** | 초기 3명 (내부 테스트, 사용자님 지정) |
| **환경** | dev(로컬 H2) → prod(AWS + MySQL RDS) |
| **롤아웃 방식** | 3명에게 개별 안내 · 사용 관찰 · 이슈 수집 |

**왜 8월 19일**: 07-02 pivot으로 이슈 #15~#26이 발행됐고, 이관 산출물이 M3(07-08)~M8(08-18) 6주간 순차 착지 예정. M8 종료 직후가 릴리스 파이프라인 dry-run + 발행 자연스러운 시점. (근거: `../../../version/0.0.2v/milestone.md` §다음 마일스톤 후보)

---

## MVP 컨셉 — "핵심 오답노트 사이클 하나"

**릴리스 시점 사용자가 완주할 수 있어야 할 최소 사이클**:

```
1. 회원가입 · 로그인
       ↓
2. LearningFacade 진입 → concepts 1~5개 선언 (예: "백엔드 개발자, 기획자")
       ↓
3. Layer 만들기 (예: "기능의 구현") → Axis 만들기 (예: "하네스 엔지니어링")
       ↓
4. Axis 아래 Roadmap 노드 만들기 (챕터 단위 subtree ASCII)
   ↳ AI Static Adapter가 role 기반으로 초안 제공 (LLM은 v2)
   ↳ 사용자가 편집·수동 추가
       ↓
5. (선택) Selection 노드 몇 개 만들기 (판례/발산 예시)
       ↓
6. 오늘 학습하며 "모르는 것" 발견 → Card 생성 (Cornell 노트)
       ↓
7. Card는 mode 매핑 스케줄 (예: 1,3,7,14) 따라 자동 노출
       ↓
8. 오늘 접근하면 Daily Batch 생성 → 그날 due 카드 짬뽕 큐
       ↓
9. Review 세션 진행 → 카드 clear
       ↓
10. 오늘 대시보드에서 "N/M clear" 확인
       ↓
11. 자정 이후 batch close · 못 본 카드는 skip (누적 없음)
```

이 사이클을 "부드럽게 돌리는 데 필요한 최소 기능"만 v1에 포함.

---

## Product별 릴리스 스코프 순회

### 1. 인증 (`product-auth.md`) — ✅ 이미 완주

- **In**: 회원가입·로그인·JWT·소셜 로그인 (Kakao)
- **Out**: 없음 (M1에 완료 상태)
- **상태**: M2 재편 시점 이미 10/10 Story 완료

### 2. Learning Tower (`product-learning-tower.md`) — 핵심 IN

**In (릴리스 대상)**:
- LearningFacade.concepts[] 1~5개 (Epic 1) — M2 Story 1 완료, Story 2~5는 M3
- Layer / Axis 생성·편집·softDelete (Epic 2) — M3
- **Roadmap 노드 스키마 신설 (이슈 #15 이관)** — M3
  - `axis_roadmap_node` 테이블 · `AxisRoadmapNode` Aggregate · 챕터 노드 CRUD API
  - body TEXT ASCII 통짜 저장 (사용자 직접 편집 가능)
- **Selection 노드 스키마 신설 (이슈 #16 이관)** — M3
  - `axis_selection` 컨테이너 + `axis_selection_node` 자식 · CRUD API
- Card ↔ Axis 직접 매핑 (이슈 #07 이관) — M4

**Out (v1 제외)**:
- Layer 시각화 진행률 API — v2
- Roadmap/Selection 개념 명세 도메인 spec의 팀 이식용 정의문 (spec에는 있지만 UX 노출 X)

### 3. Card (`product-card.md`, 신설) — 핵심 IN

**In**:
- Card Cornell 노트 구조 (MainNote text/image, Summary, Keyword ≥1, Tag ≤3) — 기존 구조 유지
- Mode enum 재편 `MODE_7D/14D/28D/60D` (이슈 #21) — M4
- OnFieldBudget 폐기 → fixed interval queue (이슈 #22) — M4
- Card `createdMode` 필드 + M3 하이브리드 (이슈 #23) — M4
  - Down cap 즉시 반영, up 새 card만 확장
- `returnToField` fresh 재시작

**Out**:
- Card 편집 이력 세부 UI (읽기만 v1)
- 챕터 노드 재생성 API + hint (이슈 #18) — v2. 초기엔 사용자가 직접 편집

### 4. Review (`product-review.md`, 신설) — 핵심 IN

**In**:
- `DailyLearningBatch` Aggregate + `DailyCardEntry` (이슈 #24) — M5
- Cross-layer 짬뽕 큐 (layer 경계 폐기, 이슈 #25) — M5
- 자정 close cron
- Streak realtime 계산
- `ReviewSession` 재편 (batch 참조, 인스턴스 개념 유지)
- **대시보드 최소판**: 오늘 completed/total + 최근 7일 completion + streak 표시만 (이슈 #26 L1~L2 수준으로 축소)

**Out (v1 제외)**:
- L3 규칙 기반 추천 안내 배지 — v2 (관찰 지표만 v1에서 축적)
- T3 조건부 주간 요약 알림 — v2 (관찰 데이터 3주 축적 후 v2에서 판정)
- 자동 mode 조정 (L4) — v2
- In-app notification 채널·프론트 polling — v2 (실시간 push 논의 v2)
- `RecommendationEngine` 도메인 서비스 — v2 (관찰 지표만 v1)

### 5. AI Suggestion (`product-ai-suggestion.md`) — 부분 IN

**In**:
- 6-Port 인터페이스 (Layer / Axis / ChaptersOutline / ChapterSubtree / SelectionOutline / SelectionSubtree) — M4~M5
- **Static Adapter만 활성** (4-role catalog: backend-developer / planner / designer / problem-solver)
- Role Catalog 4종 JSON — M5
- Prompt template 리소스 배치 (`concept-spec.txt` + few-shot) — M6 (Static도 카탈로그 세밀도 위해 사용)
- 응답 형식 `suggestionsAvailable: bool` + 폴백 원칙 (ADR010)

**Out (v1 제외)**:
- LLM Adapter (Vertex AI Gemini) — v2 (Static만으로 "학습 방향 초안" 목적 달성)
- Cost budget cap (이슈 #20) — 이미 이슈에 v2 명시 · 관찰 지표만 v1
- Cascade 자동 폴백 (LLM 실패 → Static) — v2 (v1은 Static만 있으므로 폴백 불필요)
- rate limit 10rpm → session 예산 전환 — v2

### 6. AI Interactive Roadmap (`product-ai-interactive-roadmap.md`) — 최소 IN

**In**:
- `RoadmapInteractionSession` 최소 상태 머신 (Epic 1~2) — M7
  - `INIT → LAYERS_DRAFTED → AXES_DRAFTED → CHAPTERS_DRAFTED → SUBTREES_DRAFTED → REVIEWING → COMMITTED`
  - Static Adapter만 사용
- 세션 API 최소 세트 (draft / confirm / refresh)

**Out (v1 제외)**:
- Selection 컨테이너 outline·subtree Port 활성 — v2 (v1은 Selection 수동 생성만)
- 노드 단위 재생성 API + hint (이슈 #18) — v2
- Advanced 세션 상태 (다중 axis 병렬 draft 등) — v2

### 7. 배포 라인 (`product-infra-deploy` / `product-infra-network` / `product-infra-ops`) — 필수 IN

**In (릴리스 필수)**:
- 컨테이너 배포 파이프라인 완주 (원안 M2에서 미룬 4 Story) — M7
- AWS VPC + ALB + 도메인 (원안 M2에서 미룬 5 Story) — M7
- MySQL RDS + Flyway 프로덕션 마이그레이션
- Secrets Manager + 백업 + 최소 관측 (원안 M2에서 미룬 8 Story) — M7
- HTTPS · 로그인 세션 유지

**Out**:
- 오토스케일링 정책 세부 — v1은 최소 인스턴스
- 다중 리전 · CDN 세밀 최적화

### 8. 관측 (`product-log` / `product-op`) — baseline IN

**In**:
- 구조화 로깅 · MDC · 에러 로깅 (원안 M2에서 미룬 6 Story) — M7
- Actuator + Prometheus + Grafana baseline (원안 M2에서 미룬 4 Story) — M7
- 관찰 지표 노출 (M2 pivot에서 정의한 이슈 #20/#26 축적용 지표)
- 배포 이벤트 알림 (Slack) 최소

**Out**:
- 상세 대시보드 (Grafana 여러 판) — v2
- APM (분산 트레이싱) — v2

### 9. 검색 (`product-search.md`) — v1 OUT

- **Out**: 전량. Card·Roadmap 검색은 v2. v1은 리스트 스크롤로 충분 (3명 사용자·데이터 소량 전제).

### 10. 미디어 (`product-media.md`) — v1 최소만

- **In**: Cornell 노트의 imageUrl 저장 (이미 존재)
- **Out**: 이미지 업로드 프론트·CDN 세부 UX — v2 (이미 URL 붙여넣기로 시작)

### 11. FE CDN (`product-fe-cdn.md`) — 필수 최소

- **In**: 프론트 빌드 · CDN 배포 · HTTPS
- **Out**: A/B 실험 · 세부 캐시 정책 튜닝 — v2

### 12. 부하 테스트 (`product-load-test.md`) — v1 OUT

- **Out**: 전량. 초기 3명 규모라 부하 테스트 불필요. v2 사용자 확대 시점에 실행.

---

## 사전 이관 필요 마일스톤 (M3 ~ M8)

각 마일스톤이 릴리스 스코프의 어느 조각을 담당하는지 요약. 상세 스코프는 `../../version/0.0.2v/milestone.md` §다음 마일스톤 후보 참조.

| 마일스톤 | 기간 | 릴리스 기여 |
|---|---|---|
| **M3 / 0.0.3v** | 07-08 ~ 07-14 | LT Epic 1·2 완주 (concepts[] + Layer/Axis) + 이슈 #15/#16 이관 착수 (Roadmap/Selection 노드 스키마) + ADR023 |
| **M4 / 0.0.4v** | 07-15 ~ 07-21 | Card 리팩토링 (이슈 #21·#22·#23) + AI 6-Port 스켈레톤 착수 + Card→Axis 직접 매핑 |
| **M5 / 0.0.5v** | 07-22 ~ 07-28 | Review 리팩토링 (이슈 #24·#25) + AI Static Adapter 완주 (4-role catalog) + Deck 폐기 클러스터 |
| **M6 / 0.0.6v** | 07-29 ~ 08-04 | 프롬프트 embed (이슈 #19) + 대시보드 최소판 (이슈 #26 축소) + Interactive Roadmap Session Epic 1 시작 |
| **M7 / 0.0.7v** | 08-05 ~ 08-11 | 배포 라인 완주 · 관측 baseline · E2E 통합 테스트 · Session Epic 2 완주 |
| **M8 / 0.0.8v** | 08-12 ~ 08-18 | 릴리스 대비 잔여 소진 + UX 테스트 3명 시나리오 + Reviewer 5관점 발사 + 파이프라인 dry-run |
| **🚀 릴리스** | **08-19 (수)** | **0.1.0v 발행** |

**변수·리스크**:
- LT Epic 1·2가 M3 안에 안 끝나면 M4 이후 전체가 밀림 (일주일 지연)
- 배포 라인 (M7) 8 Story 부담 — 원안 M2에서 미뤄왔던 것이라 실제 착수 시 예상보다 클 가능성
- AI Static Adapter 6-Port × 4-role catalog = 24개 payload 생성 (M5) — content 작성 부담

**완충 방안**:
- 매 마일스톤 종료 시 진척률 D6 판정. 지연 시 다음 릴리스(0.1.1v)로 일부 이관.
- M8 최종 GO/NO-GO 판정 (§릴리스 성공 기준 6가지 신호 참조).

---

## 릴리스 성공 기준 (GO/NO-GO 신호)

릴리스 직전(M8 D6) 판정. 다음 6가지 신호 중 **5개 이상 성립** 시 릴리스 GO.

- [ ] **핵심 사이클 신호**: 초기 3명 사용자가 프로덕션 환경에서 §MVP 컨셉의 10단계 사이클 순회 성공 (Cornell 노트 저장 → 다음날 batch에 노출 → clear → 대시보드 확인)
- [ ] **Mode 매핑 신호**: M3 하이브리드 down/up 시나리오 프로덕션 검증 — mode 다운 시 즉시 load 감소 확인 + up 시 진행 카드 변화 없음 확인
- [ ] **Cross-layer 짬뽕 신호**: 여러 layer의 axis card가 하나의 daily 큐에 정확히 조합 (`DailyLearningBatch.entries.count` = sum of `card.isDueOn(today)` across all axes)
- [ ] **AI Static 신호**: 4-role 감지 + Static Adapter의 6-Port(outline·subtree) 응답이 카탈로그 JSON에 기반해 정상 반환 (`suggestionsAvailable: true`)
- [ ] **관측 신호**: Grafana에서 `daily_batch.generated_total` · `card.archived_total{reason}` · `review_session.started_total` 3개 지표가 실시간 갱신
- [ ] **자정 close 신호**: KST 매일 00:05에 어제 batch 자동 close 검증 (staging 환경 24시간 관찰)

**NO-GO 조건 (하나라도 해당 시 릴리스 연기)**:
- 프로덕션 배포 시 DB 마이그레이션 실패
- 3명 사용자 중 1명 이상이 로그인 · 카드 생성 · Review 세션 진입 3단계 중 실패
- LT/Card/Review 단위 테스트 커버리지 < 80%
- 심각한 장애 이력 (Static Adapter 응답 실패 등)

---

## 사전 검증 시나리오 (M8 UX 테스트)

M8 D3~D5에 3명 사용자로 다음 5개 시나리오 각각 완주 확인.

**시나리오 1 — 첫 진입 · 학습 대상 정의**
1. 회원가입 → 로그인
2. concepts 3개 입력 ("백엔드 개발자", "기획자", "AI 엔지니어링")
3. Layer 2개 만들기 ("기능의 구현", "설계 원리")
4. 각 Layer에 Axis 1~2개 만들기
5. 첫 Axis에 Roadmap 노드 만들기 (Static Adapter 초안 → 편집)

**시나리오 2 — 카드 저장 · 다음날 노출**
1. 시나리오 1 완주 상태에서 카드 3장 생성 (모르는 개념)
2. 다음날 접근 시 daily batch에 3장 모두 노출
3. Review 세션에서 2장 clear, 1장 미완료
4. 자정 지나 batch close 후 다음날 접근 → 미완료 1장은 skip (누적 없음)

**시나리오 3 — Mode 다운 · load 즉시 감소**
1. 시나리오 2 완주 상태에서 mode를 MODE_14D에서 MODE_7D로 다운
2. 다음날 daily batch에 이미 7일 넘긴 카드가 자동 archive (MODE_DOWNGRADED reason)
3. 대시보드에서 오늘 batch 크기 축소 확인

**시나리오 4 — Cross-layer 짬뽕**
1. 2개 Layer에 카드 각각 5장씩 존재
2. 다음날 daily batch에 10장 모두 하나의 큐로 조합
3. Review 세션에서 layer 순서 없이 자연스러운 순차 노출

**시나리오 5 — Streak · 대시보드**
1. 3일 연속 batch perfect clear
2. 대시보드에서 `streak.current = 3` 표시
3. 4일차 미완료 → streak break

**성공 기준**: 각 시나리오 3명 모두 완주 (조작 미스 무관, 시스템 오류 없이).

---

## 릴리스 리스크와 관찰 포인트

| 영역 | 리스크 | 완화 |
|---|---|---|
| 6주간 순차 마일스톤 | 한 마일스톤 지연 시 릴리스 전체 밀림 | 매주 D6 진척률 판정 + 지연 시 스코프 재조정 (v2 이관) |
| M4 데이터 마이그레이션 (Mode enum 재편) | 기존 카드 backfill 실패 시 lifecycle 판정 불가 | Flyway 3단계 분리 (nullable → backfill → NOT NULL). 스팟 체크 필수 |
| M5 Review 재편 | Deck 폐기 + Session 재편 + Batch 신설이 겹침. 통합 테스트 부담 | M5 D5 이전에 통합 테스트 시나리오 완성 · D6에 3명 실측 |
| M6 프롬프트 튜닝 | Static Adapter 카탈로그 품질이 초기 사용자 만족의 원천. 튜닝 시간 소요 | M5에 카탈로그 4-role JSON 초안 · M6에 사용자 예시(하네스 로드맵) 기반 튜닝 |
| M7 배포 라인 | 원안 M2에서 미뤄왔던 8+ Story · AWS 실제 리소스 신설 부담 | M6 종료 시점부터 병렬 준비. AWS 리소스는 M6에 dev 환경 dry-run |
| M8 fix 이슈 잔여 | 5주간 관찰된 세부 이슈가 쌓임 | M8 D1~D3에 fix 이슈 소진 집중 · D4~D6에 UX 테스트 |
| 사용자 3명 확보 | 실제 사용자가 릴리스 시점에 준비돼야 함 | 사용자님 개인 네트워크 + 릴리스 1주 전 안내 확정 |
| Static Adapter만으로 사용자 만족 | LLM 없이 Static 카탈로그로 "초안 품질" 만족 여부 불확실 | Static 카탈로그를 개념 명세(이슈 #19)에 정확히 정합시키고, 사용자에게 "초안 후 수동 편집" UX로 안내 |

---

## 릴리스 이후 로드맵 (참고)

**0.1.1v (~2026-09-02, 2주 후) — LLM Adapter 도입**
- AS Epic 4 (Vertex AI Gemini) + Cascade fallback
- 챕터 노드 재생성 API + hint (이슈 #18)
- Interactive Roadmap Session advanced (Selection 자동 생성)

**0.2.0v (~2026-09-30, 6주 후) — L3 대시보드 + T3 알림**
- L3 규칙 기반 추천 안내 (이슈 #26)
- T3 조건부 주간 요약 알림 (이슈 #26)
- In-app notification 채널 (WebSocket / FCM 검토)
- 3명 사용자 관찰 데이터로 임계값 튜닝

**0.3.0v (~2026-11-30) — 검색 + Layer 시각화**
- Card·Roadmap 검색 (`product-search.md`)
- Layer 진행률 파생 API
- 미디어 업로드 세부 UX

**0.4.0v ~ 1.0.0v** — 스코프 미확정. 사용자 관찰 결과에 따라 결정.

---

## 관련 문서

- 상위 마일스톤 문서: `../../../version/0.0.2v/milestone.md` — M2 pivot 배경 · M3~M8 스코프 요약
- Product spec 원천: `workflow/task/pes/workspectrum/sdd/in-progress/` 아래 15+ 파일
- 07-02 pivot 이슈: `workflow/task/fix/brainstorming/version/0.0.2v/issue-15 ~ #26`
- 이전 릴리스 문서: 없음 (본 문서가 첫 번째)
- 이전 마일스톤 문서:
  - `../../../version/0.0.1v/milestone.md` — M1 (2026-06-23~06-28)
  - `../../../version/0.0.2v/milestone.md` — M2 (2026-07-01~07-07, 본 릴리스 결정 시점)

---

## 자가 점검 체크리스트 (D6 GO/NO-GO 판정 시)

- [ ] MVP 사이클 10단계 3명 모두 완주 검증됨
- [ ] Product 릴리스 스코프 In/Out 표가 코드·문서와 일치
- [ ] M3~M8 각 마일스톤 산출물이 Product 스코프의 어느 조각을 담당하는지 트레이스 명확
- [ ] 성공 기준 6개 신호 각각 정량 측정 가능 (관측 지표 · 테스트 시나리오)
- [ ] NO-GO 조건 명시적으로 판정 가능
- [ ] 릴리스 이후 로드맵 (0.1.1v ~ 0.3.0v)이 v1 out of scope 항목을 커버
