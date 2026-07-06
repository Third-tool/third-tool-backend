# [Fix · Brainstorming] BE 측 — 0.0.1v (2026-06-27)

> **본 파일의 역할**: M1 planning 표류 신호 중 **BE 책임 영역**의 가설·결정 누적. 자유 형식.
> **짝 파일**: `./fe.md` — FE 측 책임 영역. 양쪽 책임 이슈는 양 파일에 모두 등장.
> **다음 단계**: 각 이슈가 결정·발견을 거쳐 tier 확정되면 `workflow/task/fix/{tier}/version/0.0.1v/` 양식으로 정식 fix 작성.

작성 시점: 2026-06-27 (M1 D5) · 기준 브랜치: `feat/045-dockerfile-multi-stage`

---

## 트래킹 컨텍스트

| 영역 | 사실 |
| --- | --- |
| M1 스코프 | `workflow/task/milestones/version/0.0.1v/milestone.md` 19 Story |
| 진행 중 작업 | Tier 1 15 Story 중 다수 머지 (PR #169·#171·#172). 현재 Dockerfile multi-stage |
| 발견 시점 | M1 진행 중 사용자 테스트에서 표류 신호 3건 발견 |
| fix tier 정의 | `workflow/task/fix/{one-line-spec,feature-story,pes,sdd-lite,sdd}/version/0.0.1v/{tier}.md` |

---

### Issue 1 — 소셜 로그인 (Naver / Kakao) 미동작 — **BE 측**

**현상 / 트리거**
사용자 테스트: 카카오·네이버 로그인 둘 다 실패. 정확한 실패 지점(어디서·어떤 응답)이 아직 미확인.

**원인 가설 (BE 측 책임 영역)**

| # | 가설 | 가능성 사유 |
| --- | --- | --- |
| (a) | `application-dev.yml`의 `KAKAO_CLIENT_ID/SECRET`, `NAVER_CLIENT_ID/SECRET` 환경변수 미주입 — Spring Boot가 placeholder 미해석 시 부팅 자체 실패. **하지만 부팅이 됐다면 더미값으로 주입되어 OAuth 호출 단계에서 401** | 가장 흔한 케이스. `docs/operations/troubleshooting/ts002-environment-variables.md` 가이드 존재 |
| (b) | `SocialOAuthFlow` 구현체 누락 — Story 4-2로 Map 주입 패턴 적용. `KakaoOAuthFlow`/`NaverOAuthFlow` 빈이 안 등록되면 `SOCIAL_PROVIDER_NOT_SUPPORTED` (`USER007`, 400) | `SocialLoginController.java:60-65` 라인에서 명시 로그 — 서버 로그에서 즉시 확인 가능 |
| (c) | OAuth 토큰 교환 시 `redirect_uri` 미스매치 — `application-dev.yml`의 `kakao.redirect-uri=http://localhost:5173/oauth/kakao/callback`이 카카오 개발자 콘솔에 등록된 URI와 다르면 provider가 거부 | FE가 다른 콜백 경로 쓰는 경우(예: `/auth/kakao/callback`) provider 쪽이 reject |
| (d) | 카카오 / 네이버 개발자 콘솔에 등록된 client_id 자체가 없거나 만료 — 환경변수는 주입돼 있지만 값이 잘못됨 | 발생 시 BE 로그에 provider 4xx 응답 |

**가능성 큰 가설**: (a) 또는 (b). 둘 다 BE Task 로그 1줄로 즉시 판별 가능 — "Bean … not registered" / "No SocialOAuthFlow registered for provider"

**영향 범위**
회원가입 + 로그인 흐름 전체. 자체 로그인(`POST /login`)은 동작하므로 소셜만 분리 가능. **FE의 회원가입·로그인 라우트가 소셜 로그인을 메인 흐름으로 두면** 신규 사용자 진입 차단.

**결정해야 할 것 (사용자/운영자 결정)**
- dev 환경에서 소셜 로그인을 **0.0.1v 합격 신호에 넣을 것인가** — `milestone.md` 종료 신호 8건 중 인증 신호는 자체 로그인만 명시(라인 103). 소셜 미동작은 M1 합격에 영향 없음.
- M2로 미룰지, 즉시 fix할지.

**권장 fix 방향 (1차)**
1. BE 부팅 직후 한 번 `POST /social/login/{provider}` curl로 어떤 ErrorCode가 떨어지는지 확인 → 가설 좁히기
2. (a) 환경변수 누락이면 → `application-dev.yml` 또는 운영자 `.env` 한 줄 갱신
3. (b) Flow 누락이면 → `KakaoOAuthFlow`/`NaverOAuthFlow` 빈 등록 확인 (이미 머지됐어야 함)
4. (c) redirect_uri 미스매치면 → 카카오·네이버 개발자 콘솔 등록 URI 확인 + FE와 정합

**workspectrum tier 추천 (BE 측)**
- 1차 발견 단계에서 가설 좁힌 후 결정.
- (a) 가설 확정 시: **`one-line-spec`** (`application-dev.yml` 또는 가이드 문서 한 줄)
- (b) 가설 확정 시: **`feature-story`** (Flow 빈 등록 + 단위 테스트 1건)
- (c) 가설 확정 시: **`feature-story`** (yml 한 줄 + 카카오/네이버 콘솔 URI 변경 안내 1건) 또는 환경별 URI를 정리하는 거면 **`pes`**
- (a)+(c) 복합이면 → **`pes`** (env + 콘솔 정합 동반)

대안 tier: 인증 흐름 전반이 dev/prod에서 다르게 깨지는 게 확인되면 **`sdd-lite`** (FE에 인증 매체·콜백 경로 인계).

---

### Issue 2 — LearningFacade 유저당 1개 제약 비즈니스 정합성 — **BE 측 (주)**

**현상 / 트리거**
직전 대화에서 사용자가 `LF002 LEARNING_FACADE_ALREADY_EXISTS` (409) 로그를 보고 "유저당 1개가 적절한가" 의문 제기.

**현재 도메인 결정 (사실 인용)**
- 불변식 출처: `.claude/rules/conventions.md` §1.6 이중 방어 표 — "유저당 LearningFacade 1개 (v1)"
- 강제 메커니즘 3중:
  1. Application Service 선체크 (`LearningFacadeCommandService`)
  2. DB `UNIQUE(user_id) on learning_facade`
  3. ErrorCode 응답 `LF002` (409 CONFLICT) — `src/main/java/com/example/thirdtool/Common/Exception/ErrorCode/ErrorCode.java`
- v1 명시 — 향후 v2로 풀 가능성 시사

**원인 가설 (이건 가설이 아니라 도메인 결정의 의도 확인)**
- 도메인 의도는 "1 유저 = 1 직업 컨셉 = 1 학습 트리". 사용자가 백엔드 개발자라면 그 한 가지 컨셉 안에서 축·주제·자료를 트리로 쌓는다. 컨셉이 바뀌면 facade를 갈아엎는 게 정합 (v1).
- 의문의 사업적 배경: "1 유저가 백엔드 + 디자인 같이 학습한다면? 두 facade가 동시에 필요할 수 있다"

**영향 범위**
- 현 상태(A 유지): 사용자가 직업 컨셉 바꾸려면 facade 통째 삭제 → 모든 축·주제·자료 cascade 삭제. UX 부담 큼.
- 다수 허용(B): 도메인 모델·DB·API·Card BC와의 관계(Card도 facade에 종속?) 전반 재검토 필요.
- 중간형(C): "active=1, archived=N" — 활성 전환 개념 도입.

**결정해야 할 것 (사용자 결정 — 사업·UX 영역)**
- **(A) v1 유지** — "1 유저 = 1 컨셉" 가정 유지. 컨셉 바뀌면 facade 갈아엎기 UX 제공 (별도 endpoint).
- **(B) v2 다수 허용** — `UNIQUE(user_id)` 폐기. facade 선택·전환 UX 신설.
- **(C) v2 active 전환형** — `UNIQUE(user_id) WHERE active=true` 부분 인덱스 (MySQL 미지원이라 도메인 검증으로) + archived 다수 허용.

**권장 fix 방향 (1차 — 사용자 결정 종속)**
- 결정 전 단계: **결정 X. 사용자 의사결정 우선.**
- (A) 결정 시: BE fix 불요. 단, `LF002` 받았을 때 "이미 facade 있다"는 친절한 메시지로 통일 검토 (one-line-spec).
- (B) 결정 시: Product 단위 재설계. DOMAIN.md 불변식 폐기, ADR 신설, DB 마이그레이션, API 갱신.
- (C) 결정 시: Epic 1개 + Story 3-5개 (Facade.active 필드 추가, 전환 endpoint, 도메인 검증, DB 제약).

**workspectrum tier 추천 (BE 측)**
- (A) 유지: tier 없음 (fix 불요). 또는 메시지 통일이면 **`one-line-spec`**
- (B) 다수 허용: **`sdd`** (Product 1 (LearningFacade) 단위 결정 번복. 폐기 결정 + 새 결정 + 대안 검토 + ADR + DB 마이그레이션 + DOMAIN.md 재작성. 양식 트리거 6개 중 5+ 충족)
- (C) 중간형: **`pes`** (Story 3-5개 명세 추가. ErrorCode 신설 가능성. AC 재정의. 다른 BC 영향 0 — Card는 deck에 종속이라 facade와 직접 관계 없음)

대안 tier: (B)와 (C)가 같이 검토되면 → **`sdd`** 먼저 결정 → 결정 후 **`pes`** 다수 작성.

---

### Issue 3 — LearningFacade 생성 직후 홈 이동 흐름 — **FE 책임 (BE 영향 0)**

**BE 측 입장**: 무관. FE 라우팅 결정.

다만 **BE가 알아둘 것**: facade 생성 후 사용자가 즉시 축·주제 등록으로 가는 흐름이 정착되면, BE 측 `POST /api/v1/learning-facade/axes` 등 후속 호출 빈도가 즉시 늘어남. 부하 측면은 M1 종료 신호 외 — 무관.

상세는 `./fe.md` Issue 3 참조.

---

## 누적 메모 (자유 형식 영역)

이 아래는 사용자가 자유롭게 누적. 새 가설·외부 입력·관련 컨텍스트를 시간순으로 쌓아간다. Claude는 사용자 명시 요청 없으면 미리 채우지 않음.

- 2026-06-27 — 본 brainstorming 초기 작성 (Issue 1·2 BE 측 + Issue 3 BE 영향 0건 명시)
- (이후 추가)

---

## 참조

- 짝 파일: `./fe.md`
- fix tier 정의: `../../../{tier}/version/0.0.1v/{tier}.md`
- 이전 fe-handoff: `workflow/task/pes/fe-handoff/0.0.1v.md`
- M1 milestone: `workflow/task/milestones/version/0.0.1v/milestone.md`
- ErrorCode 사실 소스: `src/main/java/com/example/thirdtool/Common/Exception/ErrorCode/ErrorCode.java`
- 도메인 불변식 사실 소스: `.claude/rules/conventions.md` §1.6
