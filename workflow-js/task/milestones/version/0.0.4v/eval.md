# M4 / 0.0.4v — Eval (AI 응답 품질 평가 프레임)

> **본 파일의 역할**: M3에서 신설된 AI 응답 품질 평가 프레임(`../0.0.3v/eval.md` · greenfield)의 **첫 확장**. M4에 role catalog 3종 (planner · designer · problem-solver) 이 신설되면서 총 4-role 완주. 각 role별 콘텐츠 정합 판정 baseline · v2 golden dataset 진행 · M5 진입 조건을 기록한다.

**작성 시점**: 2026-07-21 (D7) · M4 동결 판정 시점.

---

## 왜 M4에 첫 확장인가

- **M4 하이라이트**: AI Static Adapter role 4종 완주 (backend-developer + planner + designer + problem-solver). M3 baseline (backend-developer 단일)이 4배 확장.
- **role catalog 콘텐츠 다양성**: 각 role별 layers · axes · chapters · selectionOutlines 6-Port 필드가 concepts에 따라 자연스러운 응답 다양성을 만들어야 함. Sceptical Reviewer 판정 절차가 M3 backend-developer.json에서 확립됐고, M4에 3개 role에 반복 적용.
- **v1 릴리스 (2026-08-19) 대비**: 3명 사용자가 실사용 시 role 감지가 자연스러워야 함. concepts "기획자" · "디자이너" · "알고리즘" 등을 넣었을 때 각각 planner/designer/problem-solver로 자동 라우팅 검증 · fallback 원칙 (알 수 없는 concept → backend-developer) 유지.
- **M6 LLM Adapter 대비**: Cascade 폴백 준비. Vertex AI 응답이 오면 shape는 유지 · providerContext만 `llm:...`로 바뀌는 형태. LLM 응답 파싱 실패 시 Static 폴백이 정합돼야 함.

---

## v1 관찰 지표 (M3 5개 유지 + M4 확장)

### M3 유지 지표

- **v1-지표 1**: `providerContext` 응답 필드 — Static vs LLM 소스 추적
  - M4 확장: `static:backend-developer` · `static:planner` · `static:designer` · `static:problem-solver` 4종 값 모두 실측 확인
- **v1-지표 2**: `suggestionsAvailable` flag — 폴백 판정
  - M4 상태: Static Adapter는 항상 `true` · LLM 도입 (M6) 시 파싱 실패 → `false` 폴백 판정에 사용
- **v1-지표 3**: Sceptical Reviewer 세션 — catalog 콘텐츠 판정
  - M4 확장: 3 role catalog 각각 판정 통과 (§M4 baseline 참조)
- **v1-지표 4**: `concept-spec.txt` 카탈로그 (6+5) — roadmap 6 type · selections 5 type
  - M4 상태: 각 role catalog의 chapters/selectionOutlines가 6+5 태깅 정합 · 판정 통과
- **v1-지표 5**: `PromptTemplatesPresenceTest` — 리소스 무결성
  - M4 상태: 7개 template 파일 존재 · `{{include:concept-spec.txt}}` 마커 정합 · 유지

### M4 확장 지표

- **v1-지표 6 (M4 신설)**: **role 감지 정합률** — `RoleDetector`가 concepts 배열에서 정확히 role을 감지하는 비율
  - 실측 방법: 로그에서 `role={detected}` 필드 축적 · concepts vs role 매핑 스팟 체크
  - M4 baseline: "기획자" → planner · "디자이너" → designer · "알고리즘"/"문제해결" → problem-solver · "백엔드 개발자" → backend-developer · 그 외 → backend-developer (fallback)
  - 관찰: v1 릴리스 후 실측으로 오탐 통계 축적 · v2에 튜닝
- **v1-지표 7 (M4 신설)**: **role별 응답 다양성** — 동일 요청(concepts + axisName)에 role별 다른 챕터 리스트 반환 여부
  - 실측 방법: PR#5 종료 신호에서 planner/designer/problem-solver 각각 다른 챕터 목록 확인
  - M4 baseline: 각 role catalog의 챕터 리스트가 상호 다름 · backend-developer 5 챕터와 겹침 없음
  - **아직 정량 지표 없음** · v2에 코사인 유사도 등 도입 검토

---

## 현재 M4 baseline 실측

### role 4종 정합 표

각 role catalog가 concept-spec 6+5 태깅 정합 판정을 통과했는지:

| Role | Layers | Axes | Chapters (roadmap 6 type 정합) | SelectionOutlines (selections 5 type 정합) | Sceptical 판정 |
| --- | --- | --- | --- | --- | --- |
| backend-developer (M3) | 6 | 여러 (하네스 엔지니어링 5 챕터 등) | ✅ 판정 통과 | ✅ 판정 통과 | ✅ M3 baseline |
| planner (M4 신설 · PR#5 S3-3) | 4 | 4 (요구사항 정의 · 유저 스토리 매핑 · ...) | ✅ 판정 통과 | ✅ 판정 통과 | ✅ M4 baseline |
| designer (M4 신설 · S3-4) | 4 | 4 | ✅ 판정 통과 | ✅ 판정 통과 | ✅ M4 baseline |
| problem-solver (M4 신설 · S3-5) | 4 | 4 (알고리즘/트러블슈팅 중심) | ✅ 판정 통과 | ✅ 판정 통과 | ✅ M4 baseline |

### 각 role별 응답 다양성 (chapters 예시)

**backend-developer** (하네스 엔지니어링):
- "1. 통합 테스트 하네스 설계"
- "2. 픽스처와 팩토리"
- "3. 격리 전략 (트랜잭션 vs cleanup)"
- "4. Slice 테스트와 통합 테스트 경계"
- "5. CI 파이프라인 병렬화"

**planner** (요구사항 정의):
- "1. 요구사항 프레이밍"
- "2. 유저 스토리 매핑"
- "3. 우선순위 프레임 (MoSCoW·RICE)"
- "4. 이해관계자 조율"

**designer** (UX 초기 프레이밍):
- "1. 사용자 여정 매핑"
- "2. 페인 포인트 정의"
- "3. 정보 구조 초안"
- "4. 프로토타입 반복"

**problem-solver** (알고리즘/트러블슈팅):
- "1. 문제 분해 (subproblem)"
- "2. 반례 · 엣지 케이스 도출"
- "3. 트레이드오프 분석"
- "4. 재현 가능한 실험 설정"

**상호 겹침 없음** — Sceptical Reviewer가 판정 통과.

### Sceptical Reviewer M4 판정 요약

- **판정 관점**: 각 role의 chapters가 "수렴된 판단 프레임"인지 · 도구 이름·특정 옵션 비교 침투 없음 · concept-spec 정합 · 사용자 학습 가치.
- **판정 결과**: 3 role catalog 모두 판정 통과. 예외 · Nit 지적:
  - planner의 "3. 우선순위 프레임 (MoSCoW·RICE)"에서 특정 도구명 인용 여부 — Sceptical Reviewer가 "판단 프레임 대표 명칭이므로 도구 침투 아님" 판정
  - designer의 "3. 정보 구조 초안"에서 IA (Information Architecture) 축약어 사용 · 확장 표기 권장 (Nit)
  - problem-solver의 "2. 반례 · 엣지 케이스 도출"이 개발자 관점 편향 우려 · concepts에 "AI 엔지니어링"·"연구자" 등 다른 방향 concepts 시 응답 다양성 관찰 필요 (M5+ 관찰)

---

## eval 실행 트리거 (M4 확장)

M3 4개 트리거 유지 + M4 확장:

- **Trigger 1**: 새 role catalog 신설 — planner·designer·problem-solver (M4에 실행) · 다음 role은 M6~M7 논의 (예: `researcher.json` · `data-engineer.json`)
- **Trigger 2**: 기존 role catalog 확장·갱신 — 카탈로그 콘텐츠 튜닝 시 반복 판정
- **Trigger 3**: LLM Adapter 도입 (M6) — Vertex AI 응답 shape 정합 · Cascade 폴백 판정
- **Trigger 4**: 릴리스 시점 (v1 · 2026-08-19) — 3명 사용자 실측 데이터로 v2 튜닝 근거 확보
- **Trigger 5 (M4 신설)**: **role 감지 오탐 관찰** — 사용자 실측에서 concepts vs 감지 role 매핑 오탐 통계 축적 · 3주+ 관찰 후 v2 튜닝 착수

---

## v2 golden dataset 방향 (M3 유지 · M4 진행 상황)

**A안 (권장 · M3 결정)**: 100 concepts × axis pairs + LLM-as-judge 점수화
- M4 진행: **미착수**. M5 여유 시점에 검토 예정.
- 데이터셋 구성: `docs/eval-dataset/v2-goldens.jsonl` (신설 예정) · concepts × axis pairs 100건 · 각 case에 기대 role · 기대 챕터 topics
- 점수화: 별도 LLM (Gemini Pro or Claude) 을 judge로 · 응답 shape 정합 · 챕터 다양성 · 카탈로그 정합 3축 점수

**B안 (v2+ 검토)**: 사용자 implicit signals + feedback
- v1 릴리스 후 사용자 접속 시 axisDraft refresh 클릭 · abandon · 편집률 등 implicit signals 축적
- 3명 사용자 · 3주 데이터로는 통계 유의성 부족 · 사용자 확대 시점 (v0.2.0v~) 착수

**M4 진행률**: A안 착수 안함. M5~M7에 여유 시점 검토 · v1 릴리스 이후 v0.1.1v에 우선순위 재검토.

---

## 다음 버전 (M5 진입 조건) · eval 프레임 확장 체크리스트

- [ ] role 4종 catalog 정합 판정 문서화 완료 · `./M4-baseline.md` 별도 파일로 축적 검토 (M5 여유 시점)
- [ ] AS Epic 3 안정화 관찰 (M5 스토리 신규 없음 · eval.md §M5 baseline로 흡수)
- [ ] role 감지 오탐 통계 축적 방법 확립 — 로그에서 `role={detected}` 필드 스팟 체크 스크립트 준비
- [ ] Sceptical Reviewer 판정 절차 표준화 — `.claude/rules/review.md` §Sceptical Reviewer 확장 · role catalog 판정 절차 명시
- [ ] v2 golden dataset A안 착수 여부 M5~M6 결정
- [ ] LLM Adapter (M6) 도착 시점의 shape 정합 사전 준비 — Response DTO가 Static/LLM 무관 동일 shape 유지 · Cascade 폴백 shape 정합 사전 검증
- [ ] 3명 사용자 릴리스 시 role 감지 실사용자 데이터 관찰 프레임 (`./M8-user-eval.md` 신설 예정 · M8 시점)

---

## 참고

- 상위 계획: `./milestone.md` (M4 스코프)
- 성과: `./outcome.md` (기술 자산 · role catalog 3종 신설)
- 회고: `./review.md` (R6 · Sceptical Reviewer 판정 절차 표준화)
- 이전 eval baseline: `../0.0.3v/eval.md` (M3 greenfield · 5 v1 지표 · backend-developer 단독)
- Sceptical Reviewer 관점: `.claude/rules/review.md` §Sceptical Reviewer
- `concept-spec.txt`: `src/main/resources/ai/prompts/concept-spec.txt` (6+5 카탈로그 태깅)
- role catalog:
  - `src/main/resources/ai/catalog/backend-developer.json` (M3)
  - `src/main/resources/ai/catalog/planner.json` (M4 · S3-3)
  - `src/main/resources/ai/catalog/designer.json` (M4 · S3-4)
  - `src/main/resources/ai/catalog/problem-solver.json` (M4 · S3-5)
- `RoleDetector`: `src/main/java/com/example/thirdtool/LearningFacade/infrastructure/suggestion/RoleDetector.java`
- 다음 마일스톤: `../0.0.5v/milestone.md` (AS Epic 3 안정화 관찰 · Story 신규 없음)
- LLM Adapter 예정: `../0.0.6v/milestone.md` (M6 · Vertex AI Gemini Flash 2.5 · Cascade 폴백)
- 릴리스 로드맵: `../../release/version/0.0.1v/release.md` (v1 릴리스 · 3명 사용자 · 2026-08-19)
