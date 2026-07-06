# M2 / 0.0.2v — Local UX 확인 가이드

> **파일의 역할**: M2 (2026-07-01 ~ 07-07) 완료 시점에 **배포 없이 로컬에서 BE + FE를 켜서 확인 가능한 것**을 정리한다. 사용자님이 릴리스 없이 진척 상황을 직접 눈으로 볼 수 있는 창.
>
> **본 M2의 특성**: D2 pivot(07-02)으로 대부분 산출물이 **도큐먼트·이슈·릴리스 계획 정합** — 실 코드 산출물은 `LT Epic 1 Story 1 (concept 테이블 V16)` 한 건. 따라서 UX 확인 대상도 최소 수준.

---

## 확인 목적

- BE·FE가 **여전히 로컬에서 부팅되는가** — D2 pivot 이후 스키마·설정 붕괴 없음 검증
- **`learning_facade_concept` 테이블이 실제 존재**하는가 (H2 dev + 준비된 MySQL prod 스키마 정합)
- Flyway 마이그레이션 이력이 V16까지 도달했는가
- FE Zod schema · 컴포넌트 지도가 새로 반영된 5개 spec (개정 3 + 신설 2)에 사전 대응 가능한가
- 문서 산출물(이슈 12건 · 릴리스 계획 · FE 마일스톤 동기화)이 정합인가

---

## 사전 조건

- Java 21 toolchain 활성 (`./gradlew --version` 통과)
- Node 20+ + pnpm 활성 (FE 프로젝트 기준)
- 브라우저 (Chrome/Edge — DevTools Network 탭 사용)
- 로컬 포트 열림: BE 8080, FE 3000 or 5173

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
```

## FE 로컬 부팅 절차

```bash
# FE 프로젝트 루트에서
pnpm install
pnpm dev
# → http://localhost:5173 (또는 3000) 부팅
```

`VITE_API_BASE_URL=http://localhost:8080` 설정 확인 (dev.env).

---

## 로컬 확인 체크리스트

### ✅ BE 단독 확인 (5분 이내)

- [ ] **부팅 성공** — `./gradlew bootRun` 로그에 `Started ThirdToolApplication` 출력
- [ ] **Actuator health** — `curl localhost:8080/actuator/health` → `{"status":"UP"}`
- [ ] **Swagger UI 접근** — 브라우저에서 `http://localhost:8080/swagger-ui.html` 진입 · 페이지 렌더
- [ ] **H2 콘솔 접근** — `http://localhost:8080/h2-console` (dev만) · JDBC URL `jdbc:h2:mem:testdb`
- [ ] **`learning_facade_concept` 테이블 존재 확인** — H2 콘솔에서 `SHOW TABLES;` → `LEARNING_FACADE_CONCEPT` 포함
- [ ] **Flyway 이력 V16 도달** — H2 콘솔 `SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;` → 최상단이 `16 | learning_facade_concept | true`
- [ ] **테이블 스키마 정합** — `SHOW COLUMNS FROM learning_facade_concept;` → `id, facade_id, value, display_order, created_at, updated_at` 컬럼 존재 · `UNIQUE(facade_id, value)` 확인 (`SHOW INDEX FROM learning_facade_concept`)

### ✅ 기존 API 회귀 미발생 확인 (10분)

M2 재편으로 스키마·API가 깨지지 않았는지 확인.

- [ ] **회원가입/로그인 정상** — `POST /api/v1/auth/signup` → `POST /api/v1/auth/login` → JWT 발급
- [ ] **LearningFacade 조회 정상** — `GET /api/v1/facades/me` → 200 응답 (**주의**: `concept: string` 단일 필드는 아직 유지 — Story 1-2~1-5는 M3 진입)
- [ ] **Card CRUD 정상** — 기존 Card 생성·조회·아카이브 API 정상 (Card 리팩토링은 M4)
- [ ] **Review 세션 정상** — 기존 Review 세션 시작·카드 view · finish (Review 재편은 M5)

**주의**: M2엔 신규 기능 API 미도입. 기존 API의 회귀 방지 확인만.

### ✅ FE 단독 확인 (5분)

- [ ] **FE 부팅 성공** — `pnpm dev` 로그에 `Local: http://localhost:5173/` 출력
- [ ] **홈 진입** — 브라우저 `http://localhost:5173` → 로그인 or 홈 화면 렌더 · 콘솔 에러 없음
- [ ] **로그인 flow 정상** — BE에 로그인 후 홈 진입 · JWT 쿠키 or 헤더 정합
- [ ] **기존 화면 회귀 없음** — LearningFacadePage · 카드 목록 · Review 세션 진입 등 기존 화면 무결

### ✅ 문서 산출물 확인 (5분 · 리뷰용)

- [ ] **12개 신규 이슈 존재** — `workflow/task/fix/brainstorming/version/0.0.2v/issue-15 ~ #26` 파일 12건 확인
- [ ] **5개 spec 개정·신설 확인** — `workflow/task/pes/workspectrum/sdd/in-progress/`:
  - `product-learning-tower.md` 최상단에 🔄 Fix 개정 섹션 (2026-07-02) 존재
  - `product-ai-suggestion.md` 최상단에 🔄 Fix 개정 섹션
  - `product-ai-interactive-roadmap.md` 최상단에 🔄 Fix 개정 섹션
  - `product-card.md` 신설 파일 존재 (SDD 15섹션 + Epic 3개)
  - `product-review.md` 신설 파일 존재 (SDD 15섹션 + Epic 3개)
- [ ] **릴리스 계획 존재** — `workflow/task/milestones/release/version/0.0.1v/release.md` 존재 · 0.1.0v ~ 08-19 타겟 명시
- [ ] **FE 마일스톤 동기화** — `workflow/task/pes/fe/fe-milestones/version/0.0.2v/milestone.md`에 07-02 pivot 반영

---

## 시나리오 확인 (사용자 시점)

**시나리오 A — "M2 완료 후 앱을 켰을 때 사용자가 보는 것"**

1. 사용자가 FE `http://localhost:5173` 진입
2. 로그인 (기존 flow)
3. 홈 화면 · LearningFacadePage 도달
4. **관찰**: 컴포넌트 위치·화면 흐름이 M1과 동일 (신규 기능 노출 없음)
5. **주의**: `<ConceptsInput>` chip UI · `<LayerFormDialog>` 등은 **미완주 (M3에 진입)**. 사용자 관점에서 M2 완료는 "겉으로 안 보이는 변화" — 도큐먼트·이슈 기반 재편

**시나리오 B — "FE 개발자가 새 spec 문서로 다음 단계 준비"**

1. 개발자가 `product-learning-tower.md` (FE) 🔄 Fix 개정 섹션 읽기 → 이후 M3에 무엇을 만들지 명확
2. `product-card.md` (FE) 신설 파일 확인 → Card lifecycle UI (Mode selector · CardScheduleBadge 등) 미리 청사진 확보
3. `product-review.md` (FE) 신설 파일 확인 → Daily batch UI · 대시보드 spec 확인

**시나리오 C — "M3 진입 준비 확인"**

1. `workflow/task/milestones/version/0.0.3v/milestone.md`가 존재 · 6 Epic PR 명시
2. M2에서 완주된 `LT Epic 1 Story 1` 자국이 M3 milestone의 인벤토리에 반영됨 (`M3 진입 시 완료: 1 (E1S1)`)
3. M3의 첫 브랜치 `feat/026-lt-e1-concepts-multiplex-complete` 시작 준비 완료 상태

---

## M2 확인 못하는 것 (다음 마일스톤 이관)

M2는 도큐먼트 중심이라 대부분의 실 기능은 미도입. **로컬에서 확인 불가한 것들**:

- ❌ concepts[] 다중화 입력 (M3 · LT E1 S1-2~S1-5)
- ❌ Layer / Axis 계층 UI (M3 · LT E2)
- ❌ Roadmap 노드 CRUD (M3 · LT E3 S3-6~S3-8)
- ❌ Selection 노드 CRUD (M3 · LT E3 S3-9~S3-11)
- ❌ AI 응답 (M3 · AS E1·E2·E7)
- ❌ Card `createdMode` · M3 하이브리드 (M4)
- ❌ Daily Batch · Cross-layer Review (M5)
- ❌ 대시보드 (M6)
- ❌ 배포·관측·로깅 (M7)

**M2 대상은 오직 "재편 문서 착지"** — 실제 UX 향상은 M3부터.

---

## 관찰 지표 로깅 확인 (v1 관찰 인프라 미도입)

M2엔 관찰 지표 로깅 인프라 아직 미도입 (M7 · `product-op.md` 이관). 로컬 확인 시:

- 로그 파일 위치: `build/logs/thirdtool.log` (`application-dev.yml` 설정 기준)
- Flyway 이력만 확인 가능 (H2 콘솔)

M3부터 이슈 #20의 관찰 지표 로깅 (AI 호출 수 · 토큰 · fallback 발동)이 코드 레벨에서 착지 예정 (M6).

---

## 문제 발생 시 (Troubleshooting)

| 증상 | 원인 후보 | 해결 |
| --- | --- | --- |
| `./gradlew bootRun` 실패 with "Flyway migration failed" | V16 스크립트 문법 오류 or DB 상태 손상 | `./gradlew clean` 후 재시도. dev는 H2 in-memory라 clean 시 초기화 |
| Swagger UI 미접근 | Springdoc 미포함 or 프로필 이슈 | `build.gradle`에 `springdoc-openapi-starter-webmvc-ui` 의존 확인 |
| H2 콘솔 미접근 | `spring.h2.console.enabled=false` | `application-dev.yml`에서 `true` 확인 |
| FE 부팅 후 API 호출 401 | JWT 만료 or 미발급 | 로그인 재수행 · localStorage 확인 |
| FE Zod 파싱 오류 (기존 필드) | M2 재편 중 스키마 이관 실수 | 콘솔 에러 캡처 · 해당 Zod 파일 확인 (`src/lib/api/schemas/`) |

---

## 마무리 판정

M2 완료 GO/NO-GO는 아래 3개 조건 모두 만족 시 통과:

- [ ] BE 부팅 성공 + Flyway V16 착지 + 기존 API 회귀 없음
- [ ] FE 부팅 성공 + 기존 화면 회귀 없음
- [ ] 문서 산출물 12 이슈 + 5 spec + 릴리스 계획 + FE 마일스톤 정합

→ 조건 만족 시 M2 동결 · **M3 진입** (2026-07-08 Wed).

---

## 참고

- 마일스톤 원본: `./milestone.md`
- 릴리스 스코프: `../../release/version/0.0.1v/release.md`
- 다음 마일스톤: `../0.0.3v/milestone.md` · `./ux-check.md` (M3 UX 확인)
- FE 대응 마일스톤: `../../../pes/fe/fe-milestones/version/0.0.2v/milestone.md`
