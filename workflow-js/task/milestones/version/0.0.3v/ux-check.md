# M3 / 0.0.3v — Local UX 확인 가이드

> **파일의 역할**: M3 (2026-07-08 ~ 07-14) 완료 시점에 **배포 없이 로컬에서 BE + FE를 켜서 확인 가능한 것**을 정리한다. 사용자님이 릴리스 없이 진척 상황을 직접 눈으로 볼 수 있는 창.
>
> **본 M3의 특성 — M3 하이라이트**: "AI 결과 첫 노출". Static Adapter의 `backend-developer` catalog로 로컬에서 실제 챕터 outline · subtree ASCII 트리 응답을 확인할 수 있다. concepts[]·Layer·Axis·Roadmap 노드·Selection 노드 CRUD도 모두 로컬에서 완주 가능.

---

## 확인 목적

- BE·FE가 새 스키마·API·컴포넌트로 **여전히 로컬에서 부팅되는가** — Flyway V17~V20 정상 착지
- **6개 축 실 기능 확인**:
  1. concepts[] 다중화 입력 (LT E1)
  2. Layer / Axis 계층 CRUD (LT E2)
  3. Roadmap 챕터 노드 CRUD + 순서변경 (LT E3 S3-6~S3-8)
  4. Selection 컨테이너 + 자식 노드 CRUD (LT E3 S3-9~S3-11)
  5. **AI 첫 응답** — Static Adapter `backend-developer` role (AS E1·E2·E3·E7)
  6. 문서·프롬프트 자산 정합 (ADR023 · `concept-spec.txt` · `backend-developer.json`)

---

## 사전 조건

- Java 21 toolchain 활성 (`./gradlew --version` 통과)
- Node 20+ + pnpm 활성 (FE 프로젝트 기준)
- 브라우저 (Chrome/Edge — DevTools Network 탭 활용)
- 로컬 포트 열림: BE 8080, FE 3000 or 5173
- Postman or `curl` (BE API 확인용)

---

## BE 로컬 부팅 절차 (dev 프로필 · H2)

```bash
# 1. 저장소 정리
./gradlew clean

# 2. 부팅 (dev 프로필 자동, H2 in-memory)
./gradlew bootRun

# 3. 정상 부팅 확인 (별도 터미널)
curl -s http://localhost:8080/actuator/health
# → {"status":"UP"} 응답

# 4. Flyway 이력 V20까지 도달 확인
curl -s http://localhost:8080/actuator/flyway
# → migrations 배열에 V16 ~ V20 성공 상태
```

**주의**: M3에서 V17 (learning_layer), V18 (axis_roadmap_node + axis_roadmap archive), V19 (axis_selection_node), V20 (axis_selection.content archive) 4건 신규 착지.

## FE 로컬 부팅 절차

```bash
# FE 프로젝트 루트에서
pnpm install
pnpm dev
# → http://localhost:5173 부팅
```

`VITE_API_BASE_URL=http://localhost:8080` 설정 확인 (`.env.development`).

---

## 로컬 확인 체크리스트

### ✅ BE 단독 확인 (10분)

- [ ] **부팅 성공** — `./gradlew bootRun` 로그에 `Started ThirdToolApplication` 출력
- [ ] **Actuator health** — `curl localhost:8080/actuator/health` → UP
- [ ] **Swagger UI 진입** — `http://localhost:8080/swagger-ui.html` 페이지 렌더 · **6개 신규 엔드포인트 그룹 표시** (learning-facade / layer / roadmap-node / selection-node / suggestion / auth)
- [ ] **H2 콘솔** — `http://localhost:8080/h2-console` 진입 · JDBC URL `jdbc:h2:mem:testdb`
- [ ] **Flyway V17~V20 이력** — H2 콘솔:
  ```sql
  SELECT version, description, success FROM flyway_schema_history
  WHERE version IN ('17','18','19','20') ORDER BY installed_rank;
  ```
  → 4건 모두 `success=true`
- [ ] **신규 테이블 스키마 정합** (H2 콘솔 `SHOW COLUMNS FROM ...`):
  - `learning_layer` — id, facade_id, name, display_order, deleted_at, created_at, updated_at
  - `axis_roadmap_node` — id, axis_id, display_order, title, rationale, body(TEXT), deleted_at, audit
  - `axis_selection_node` — id, selection_id, display_order, title, rationale, body(TEXT), audit
  - `_archived_axis_roadmap` — 원본 `axis_roadmap` 아카이브 존재
  - `axis_selection._archived_content` — 원 content 컬럼 아카이브
- [ ] **UNIQUE 제약 정합**:
  - `learning_layer` — `UNIQUE(facade_id, name)`
  - `axis_selection` — `UNIQUE(axis_id, name)` (이슈 #11 계승)
- [ ] **`ArchiveReason` CHECK 제약 M3엔 미변경 확인** — M4 이관 (`MANUAL/MAX_VIEW/MAX_DURATION` 그대로 유지)
- [ ] **Static Adapter 리소스 파일 존재**:
  - `src/main/resources/ai/catalog/backend-developer.json` 존재 · 유효 JSON
  - `src/main/resources/prompts/concept-spec.txt` 존재 · roadmap 카탈로그 6종 + selections 5종 명시

### ✅ BE API 실행 확인 (25분, curl 예시)

이하 예시는 로그인 후 `{JWT}` 획득한 상태 전제.

**① concepts[] 다중화 (LT E1)**
```bash
# 다중 입력
curl -X POST http://localhost:8080/api/v1/facades/me/concepts \
  -H "Authorization: Bearer {JWT}" -H "Content-Type: application/json" \
  -d '["백엔드 개발자", "기획자", "AI 엔지니어링"]'
# → 201 + Response { concepts: ["백엔드 개발자", "기획자", "AI 엔지니어링"] }

# 조회
curl http://localhost:8080/api/v1/facades/me -H "Authorization: Bearer {JWT}"
# → concepts 3개 배열 확인

# 예외: 6개 초과
curl -X POST http://localhost:8080/api/v1/facades/me/concepts ... -d '["A","B","C","D","E","F"]'
# → 400 + ErrorCode "LEARNING_FACADE_CONCEPTS_LIMIT_EXCEEDED"

# 예외: 중복
curl -X POST ... -d '["A","A"]'
# → 400 + "LEARNING_FACADE_CONCEPTS_DUPLICATE"
```

**② Layer / Axis 계층 CRUD (LT E2)**
```bash
# Layer 생성
curl -X POST http://localhost:8080/api/v1/facades/me/layers \
  -H "Authorization: Bearer {JWT}" -H "Content-Type: application/json" \
  -d '{"name":"기능의 구현"}'
# → 201 + { id: 1, name: "기능의 구현", displayOrder: 1 }

# Axis 생성 (Layer 아래)
curl -X POST http://localhost:8080/api/v1/layers/1/axes \
  -H "Authorization: Bearer {JWT}" -H "Content-Type: application/json" \
  -d '{"name":"하네스 엔지니어링"}'
# → 200 + { id: 5, name: "하네스 엔지니어링", layerId: 1 }

# 계층 조회
curl http://localhost:8080/api/v1/facades/me -H "Authorization: Bearer {JWT}"
# → { concepts: [...], layers: [{ id: 1, name: "기능의 구현", axes: [{id:5, name:"하네스 엔지니어링"}] }] }
```

**③ Roadmap 챕터 노드 CRUD (LT E3 S3-6~S3-8)**
```bash
# 노드 추가
curl -X POST http://localhost:8080/api/v1/axes/5/roadmap-nodes \
  -H "Authorization: Bearer {JWT}" -H "Content-Type: application/json" \
  -d '{
    "title": "1. 하네스 엔지니어링 기초",
    "rationale": "AI 에이전트의 기본 프레임 이해",
    "body": "├── 1-1. 정의와 본질\n│       모델 + 하네스 — 에이전트 = 추론 엔진 + 런타임\n..."
  }'
# → 201 + { id: 10, displayOrder: 1, title, rationale, body }

# 두 번째 노드 추가 → displayOrder 자동 2
# 순서 재부여
curl -X PUT http://localhost:8080/api/v1/axes/5/roadmap-nodes/order \
  -H "Authorization: Bearer {JWT}" -H "Content-Type: application/json" \
  -d '[11, 10]'
# → 200 + display_order 재부여 (10→2, 11→1)

# 기존 폐기 엔드포인트 확인
curl -X PUT http://localhost:8080/api/v1/axes/5/roadmap -d '{"content":"..."}'
# → 410 Gone
```

**④ Selection 컨테이너 + 노드 CRUD (LT E3 S3-9~S3-11)**
```bash
# 컨테이너 생성 (이슈 #11 정책 유지)
curl -X POST http://localhost:8080/api/v1/axes/5/selections \
  -H "Authorization: Bearer {JWT}" -H "Content-Type: application/json" \
  -d '{"name": "능 아키텍처 selections v2"}'
# → 201 + { id: 20, axisId: 5, name: "..." }

# 컨테이너 하위 노드 추가
curl -X POST http://localhost:8080/api/v1/selections/20/nodes \
  -H "Authorization: Bearer {JWT}" -H "Content-Type: application/json" \
  -d '{
    "title": "1. IoC & DI 핵심 원리",
    "rationale": "...",
    "body": "├── 1-1. IoC 컨테이너 동작 원리\n..."
  }'
# → 201

# 예외: 동일 axis에 같은 name 재저장
curl -X POST http://localhost:8080/api/v1/axes/5/selections -d '{"name":"능 아키텍처 selections v2"}'
# → 409 + "AXIS_SELECTION_NAME_ALREADY_EXISTS"
```

**⑤ 🌟 AI 첫 응답 (AS E1·E2·E3·E7 · M3 하이라이트)**
```bash
# 챕터 outline 요청 (Static Adapter · backend-developer role 자동 감지)
curl -X POST http://localhost:8080/api/v1/suggestions/chapters-outline \
  -H "Authorization: Bearer {JWT}" -H "Content-Type: application/json" \
  -d '{
    "concepts": ["백엔드 개발자", "기획자"],
    "layerName": "기능의 구현",
    "axisName": "하네스 엔지니어링",
    "axisReason": "AI 에이전트의 기본 프레임 이해"
  }'
# → 200
# {
#   "chapters": [
#     {"title": "1. 하네스 엔지니어링 기초", "rationale": "..."},
#     {"title": "2. 에이전트 루프 (ReAct/TAO)", "rationale": "..."},
#     {"title": "3. 시스템 프롬프트 조립", "rationale": "..."},
#     {"title": "4. 도구 디스패치 & 레지스트리", "rationale": "..."},
#     {"title": "5. 컨텍스트 관리 메커니즘", "rationale": "..."}
#   ],
#   "providerContext": "static:backend-developer",   ★ Static Adapter · role 감지 검증
#   "suggestionsAvailable": true
# }

# 승인된 챕터의 subtree 요청 (병렬 가능 · 여기선 순차 예시)
curl -X POST http://localhost:8080/api/v1/suggestions/chapter-subtree \
  -H "Authorization: Bearer {JWT}" -H "Content-Type: application/json" \
  -d '{
    "concepts": ["백엔드 개발자"],
    "layerName": "기능의 구현",
    "axisName": "하네스 엔지니어링",
    "chapter": {"title": "1. 하네스 엔지니어링 기초", "rationale": "..."},
    "siblingChapters": [...]
  }'
# → 200
# {
#   "bodyAsciiTree": "├── 1-1. 정의와 본질\n│       모델 + 하네스 — 에이전트 = 추론 엔진 + 런타임\n│       \"모델 아니면 하네스\" — 비모델 코드 전체가 하네스\n...",
#   "providerContext": "static:backend-developer",
#   "suggestionsAvailable": true
# }

# Rate limit 도달 검증 (10 rpm)
# 위 요청을 11회 연속 실행 → 11번째 → 429 + Retry-After 헤더
```

**⑥ Layer / Axis / Roadmap 노드 저장 → AI 응답 저장 flow (E2E)**
```bash
# ① concepts 다중 저장 → ② Layer 생성 → ③ Axis 생성 → ④ AI outline 요청 (Static)
#   → ⑤ 사용자 편집 → ⑥ 각 챕터마다 subtree 요청 → ⑦ 저장 API로 Roadmap 노드 추가
# 이 사이클이 로컬에서 순차로 완주하면 M3 종료 신호 ✅
```

### ✅ FE 단독 확인 (10분)

- [ ] **FE 부팅 성공** — `pnpm dev` 로그에 `Local: http://localhost:5173/` 출력
- [ ] **홈 진입 · 콘솔 에러 없음** — 브라우저 DevTools Console에서 red error 없음
- [ ] **`<ConceptsInput>` chip UI 렌더** — LearningFacadePage에서 chip 입력·삭제 UI 동작
- [ ] **`<LayersListPage>` 진입** — Layer 목록·생성·수정 UI 렌더
- [ ] **`<LayerFormDialog>` 열림** — Layer 생성 폼 다이얼로그
- [ ] **`<AxisSelect>` 정상** — 카드 편집기·Roadmap 노드 편집기에서 Axis 선택 드롭다운 렌더
- [ ] **`<RoadmapNodeList>` 렌더** — Roadmap 노드 카드 리스트 UI (M3 신규 컴포넌트)
- [ ] **`<RoadmapNodeCard>` 편집** — 챕터 카드에서 title/rationale/body 편집 UI
- [ ] **`<NodeBodyEditor>` monospace** — body ASCII textarea가 monospace font · tab indent 힌트
- [ ] **`<SelectionContainerList>` 렌더** — Selection 컨테이너 리스트 · 신규 생성 버튼
- [ ] **`<ChaptersOutlineButton>` 클릭** — AxisDetailPage/Roadmap 탭에 배치 · 클릭 시 요청
- [ ] **`<ConceptSpecTooltip>` 표시** — Roadmap/Selection 편집 진입 시 첫 1회 "수렴/발산" 안내
- [ ] **Zod 스키마 파싱** — DevTools Network에서 각 API 응답이 Zod 스키마 통과 (콘솔에 Zod error 없음)

### ✅ BE+FE 통합 확인 (30분 · 사용자 시점 시나리오)

**시나리오 A — 학습 대상 정의 (concepts[] + Layer + Axis)**

1. FE 홈 진입 · 로그인
2. LearningFacadePage 진입 → `<ConceptsInput>` 열기
3. chip 3개 입력: "백엔드 개발자" · "기획자" · "AI 엔지니어링" → 저장
4. → `PUT /facades/me` payload에 `concepts: ["백엔드 개발자", "기획자", "AI 엔지니어링"]` 전송 (DevTools Network 확인)
5. LayersListPage 진입 → [Layer 추가] → 이름 "기능의 구현" 입력 → 저장
6. 생성된 Layer 진입 → [Axis 추가] → "하네스 엔지니어링" → 저장
7. **관찰**: 계층 트리 UI에 concepts[3] > Layer["기능의 구현"] > Axis["하네스 엔지니어링"] 렌더

**시나리오 B — Roadmap 챕터 노드 CRUD + 순서변경**

1. 시나리오 A 완주 상태
2. Axis "하네스 엔지니어링" 상세 화면 진입 → Roadmap 탭
3. `<ConceptSpecTooltip mode="roadmap">` 첫 진입 안내 (닫으면 sessionStorage 저장)
4. [+ 챕터 노드 추가] → 카드 생성
5. `<RoadmapNodeCard>` 편집: title="1. 하네스 엔지니어링 기초", rationale="AI 에이전트 기본 프레임", body=`├── 1-1. 정의와 본질\n│       ...` (사용자 예시 형태)
6. 저장 (`PATCH /roadmap-nodes/{id}`)
7. 두 번째 노드 추가 → displayOrder 자동 2
8. 드래그 앤 드롭으로 순서 재배치 → `PUT .../order` 호출 → 순서 정합 확인

**시나리오 C — Selection 컨테이너 + 노드 CRUD**

1. 동일 Axis 상세 → Selections 탭
2. [+ Selection 컨테이너 추가] → name="능 아키텍처 selections v2" → 저장
3. 컨테이너 진입 → [+ 노드 추가] → title/rationale/body 입력 → 저장
4. **회귀 검증**: 같은 name 재입력 시도 → 409 응답 → 인라인 에러 "이미 존재. 다른 이름 사용"
5. **하드 delete 확인**: 컨테이너 삭제 → `<ConfirmDialog>` "삭제하면 자식 노드까지 복원 불가" → 확인 → CASCADE 삭제

**시나리오 D — 🌟 AI 첫 응답 (M3 하이라이트)**

1. 동일 Axis 상세 → Roadmap 탭
2. `<ChaptersOutlineButton>` 클릭
3. `<ChaptersOutlineDialog>` open · 로딩 스피너
4. 응답 도착 → 5개 챕터 리스트 표시:
   - "1. 하네스 엔지니어링 기초"
   - "2. 에이전트 루프 (ReAct/TAO)"
   - "3. 시스템 프롬프트 조립"
   - "4. 도구 디스패치 & 레지스트리"
   - "5. 컨텍스트 관리 메커니즘"
5. **관찰 지표**: DevTools Network Response Header `providerContext: static:backend-developer` 확인
6. **관찰 지표 (dev only)**: `<ProviderContextIndicator>` 배지 화면에 "static:backend-developer" 표시
7. 5개 챕터 모두 승인 → `<ChapterSubtreeProgress>` 로딩 → 5회 병렬 `POST /suggestions/chapter-subtree` 호출
8. 각 챕터 body ASCII 트리 응답 → UI에 렌더 (챕터 카드에 body 편집기 자동 채워짐)
9. **사용자 편집** → 저장 (`POST /axes/{axisId}/roadmap-nodes`) — AI 초안이 실제 저장 가능한지 검증
10. Rate limit 검증: 10 rpm 초과 시 `<RateLimitToast>` 표시 · Retry-After 카운트다운

**시나리오 E — 개발자용 · 프롬프트 자산 정합**

1. BE 저장소 검색:
   - `src/main/resources/ai/catalog/backend-developer.json` 존재 · Layer 후보 + Axis 후보 + 챕터 outline + subtree + Selection outline 필드 포함
   - `src/main/resources/prompts/concept-spec.txt` 존재 · roadmap 카탈로그 6종 + selections 카탈로그 5종 + 판별 기준 5+5 문자열 포함
   - `src/main/resources/prompts/chapters-outline.txt` · `chapter-subtree.txt` · `selection-outline.txt` · `selection-subtree.txt` · `layer.txt` · `axis.txt` — 6개 프롬프트 템플릿 존재
2. 각 프롬프트 템플릿 상단에 `{{concept-spec.txt}}` include 지시 or `@Value("classpath:...")` 조립 확인
3. roadmap Port 프롬프트에 "도구 이름·특정 옵션 비교 금지" 지시 grep 통과
4. selections Port 프롬프트에 "기준은 roadmap 담당, 발산된 관점만" 지시 grep 통과

---

## M3 확인 못하는 것 (다음 마일스톤 이관)

배포 없이 로컬에서 확인 불가한 M3 out of scope 항목:

- ❌ Card 리팩토링 · Mode enum 재편 · M3 하이브리드 · `createdMode` (**M4 이관 · 이슈 #21/#22/#23**)
- ❌ Daily Batch · Cross-layer Review · Session 재편 (**M5 · 이슈 #24/#25**)
- ❌ 캐시 측정 대시보드 v1 (오늘·7일·streak) (**M6 · 이슈 #26**)
- ❌ Static → LLM Adapter Cascade fallback (**M6 · Vertex AI Gemini**)
- ❌ 챕터 노드 재생성 API + hint UI (**v2 · 이슈 #18**)
- ❌ 배포·CloudFront·AWS 인프라 · Web Vitals baseline (**M7**)
- ❌ 관측 지표 대시보드 (Grafana) (**M7 · product-op.md**)
- ❌ 검색·미디어·부하 테스트 (**v2 · v1 이후**)
- ❌ Selection Static Adapter의 다중 role catalog (planner/designer/problem-solver) (**M4~M5 · 이슈 #10 확장분**)
- ❌ AI Interactive Roadmap Session Wizard UI (7단계) (**M6~M7**)

---

## 관찰 지표 로깅 확인 (M3 초기 도입)

M3에서 이슈 #20의 관찰 지표 로깅 인프라 착지 예정. 로컬 확인 시:

- 로그 파일 위치: `build/logs/thirdtool.log`
- **AI Static Adapter 호출 시 로그 항목**:
  - `port=chapters-outline`
  - `provider=static`
  - `role=backend-developer`
  - `userId={...}` · `duration=Xms`
- **AI 요청·응답 형식**:
  - Request: DEBUG 레벨 (JSON payload) — dev만
  - Response: INFO 레벨 (요약: chapter count · providerContext · suggestionsAvailable)
- Rate limit 발동 시:
  - `WARN: rate_limit_exceeded userId={...} bucket=user_10rpm`

M4~M6에 대시보드·주간 요약 로깅 항목 확장 예정.

---

## 문제 발생 시 (Troubleshooting)

| 증상 | 원인 후보 | 해결 |
| --- | --- | --- |
| `./gradlew bootRun` 실패 with "Flyway migration failed at V18" | V18 파싱 마이그레이션 로직 오류 or 기존 `axis_roadmap.content` 데이터 파싱 실패 | `./gradlew clean` (dev H2 초기화) 후 재시도. prod MySQL이면 파서 로그 확인 |
| API 404 `/api/v1/axes/{id}/roadmap-nodes` | Controller 미착지 or Base 브랜치 mismatch | Epic PR #3 머지 확인. `git log` |
| Static Adapter 응답 `suggestionsAvailable: false` | catalog JSON 파싱 실패 or `backend-developer.json` 미배치 | 리소스 파일 존재 확인 · JSON 문법 검증 (`jq .` 통과) |
| `providerContext` 필드 없음 | Response DTO 미갱신 (AS E1 S1-5~1-8 record 실수) | Zod schema 대조 · Response record 필드 재확인 |
| FE Zod 파싱 오류 (roadmap-node.body 타입) | 백엔드 응답에 body가 문자열 vs 배열 mismatch | Response DTO에 `body: String` 확인 · Zod `z.string()` 정합 |
| `<ChaptersOutlineButton>` 클릭 후 무응답 | CORS or Rate limit 도달 | DevTools Network 확인 · 429 응답 여부 |
| Layer 순서변경이 반영 안 됨 | `PUT .../order` 호출 페이로드에 id 배열 불일치 | 요청 페이로드 검증 · `ROADMAP_NODE_ORDER_MISMATCH` 응답이면 id 집합 재확인 |

---

## 마무리 판정

M3 완료 GO/NO-GO는 아래 5개 조건 모두 만족 시 통과:

- [ ] BE 부팅 성공 + Flyway V17~V20 4건 착지 + 신규 테이블 스키마 정합
- [ ] BE API 6개 축 실행 확인 (concepts / Layer / Axis / Roadmap 노드 / Selection 노드 / **AI Static Adapter 응답**)
- [ ] FE 부팅 성공 + M3 신규 컴포넌트 (`<RoadmapNodeList>` · `<SelectionContainerList>` · `<ChaptersOutlineButton>` · `<ConceptSpecTooltip>`) 렌더
- [ ] 5개 시나리오 완주 — 특히 **시나리오 D (AI 첫 응답)** · 시나리오 E (프롬프트 자산)
- [ ] ADR023 문서 착지 + `concept-spec.txt` + `backend-developer.json` 정합

→ 조건 만족 시 M3 동결 · **M4 진입** (2026-07-15 Wed).

---

## 참고

- 마일스톤 원본: `./milestone.md` — Epic PR 매트릭스 · SDD 위치 · 종료 신호
- 이전 마일스톤 UX 확인: `../0.0.2v/ux-check.md`
- 릴리스 스코프: `../../release/version/0.0.1v/release.md` — 0.1.0v (~08-19) 사이클 10단계
- 다음 마일스톤: 07-15 진입 · `../0.0.4v/milestone.md` (M3 종료 후 신설)
- FE 대응 마일스톤: `../../../pes/fe/fe-milestones/version/0.0.3v/milestone.md`
- SDD 참조:
  - `../../../pes/workspectrum/sdd/in-progress/product-learning-tower.md` — Epic 1·2·3 재정의판
  - `../../../pes/workspectrum/sdd/in-progress/product-ai-suggestion.md` — Epic 1·2·3·5·7
- 07-02 pivot 이슈: `../../../fix/brainstorming/version/0.0.2v/issue-15 ~ #26` 12건 중 M3 처리분: #15/#16/#17/#19
