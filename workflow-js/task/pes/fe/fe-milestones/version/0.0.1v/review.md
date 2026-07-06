# FE 0.0.1v / Review (회고)

> FE M1 종료 시점에 작성. 본 파일이 채워지면 0.0.1v는 동결되고 다음 마일스톤 진입.

---

## 작성 메타

- 작성일: `_____` (예상 2026-06-29 월요일)
- 작성자: 운영자 (1인, FE 진영)
- 본 버전 동결 여부: ☐ 동결 / ☐ 0.0.1.1v 패치 발행 (미합격 처리)
- 백엔드 M1 동결 상태와 정합 확인: `_____` (BE `review.md` 종료 신호 통과 여부)

---

## 계획 vs 실제 (정량)

| 항목 | 계획 (M1 milestone.md) | 실제 | 차이 사유 |
| --- | --- | --- | --- |
| Story 머지 수 (Tier 1) | 8 | `_____` | `_____` |
| Story 머지 수 (Tier 2 — Want) | 3 | `_____` | `_____` |
| 전체 머지 수 | 11 | `_____` | `_____` |
| `https://thirdtool.dev` 첫 로드 시점 | D5(토) | `_____` | `_____` |
| 첫 로그인 성공 시점 | D5(토) | `_____` | `_____` |
| BE M1 완료 시점과의 gap | 동시 | `_____` | `_____` |
| GHA workflow 첫 성공 소요 시간 | < 5분 | `_____ 분` | `_____` |
| 본주 작업 시간 누적 (시간) | (목표 미설정) | `_____` | `_____` |

---

## 종료 신호 충족 여부

milestone.md에 정의된 8 신호 중 충족된 항목 체크:

- [ ] **머지 신호** (Tier 1 75% = 6 Story 이상 머지)
- [ ] **환경변수 신호** (`.env.production` 커밋 + `VITE_API_BASE_URL` 반영)
- [ ] **첫 배포 신호** (GHA workflow 1회 이상 성공)
- [ ] **도메인 신호** (`https://thirdtool.dev` SPA 로드)
- [ ] **인증 흐름 신호** (로그인 → Set-Cookie 수신 → `/home` 진입)
- [ ] **계약 정합 신호** (`username` 필드 grep 0건)
- [ ] **인터셉터 신호** (MSW 8종 401 handler + Vitest 통과)
- [ ] **문구 신호** ("실패" 어휘 grep 0건 + CI job 등록)

**합격 기준**: 8 신호 중 6 이상. 6 미만이면 0.0.1.1v 패치로 연장. 도메인·인증 신호는 BE M1 통과 시점에 좌우되므로 BE와 결합 판단.

---

## UX 시나리오 검증 결과

`ux-test.md`의 종합 통과 기준 6개 묶음 결과:

| 묶음 | 통과 여부 | 미통과 원인 |
| --- | --- | --- |
| 배포·도메인 핵심 | `_____` | `_____` |
| 인증 핵심 | `_____` | `_____` |
| Card done 정합 | `_____` | `_____` |
| LearningFacade done 정합 | `_____` | `_____` |
| 에러 회복 UX | `_____` | `_____` |
| 성능 baseline | `_____` | `_____` |

---

## 가장 큰 막힘 (Top 3)

| 순위 | 막힘 | 소요 시간 | 해결 방식 | 학습 |
| --- | --- | --- | --- | --- |
| 1 | `_____` | `_____` | `_____` | `_____` |
| 2 | `_____` | `_____` | `_____` | `_____` |
| 3 | `_____` | `_____` | `_____` | `_____` |

---

## 기대 vs 의외

| 영역 | 기대 | 실제 |
| --- | --- | --- |
| 본주 속도 (Story/주) | 11 | `_____` |
| 가장 시간 든 Story | (예상: FE-3-2 CORS + SameSite E2E) | `_____` |
| 가장 빨리 끝난 Story | (예상: FE-4 username grep) | `_____` |
| FE 인프라 일 평균 비용 | ~$0.00 | `_____` |
| Lighthouse Performance (Tier 2) | 미측정 | `_____` |
| 발견된 신규 리스크 (M2로 이월) | — | `_____` |

---

## BE M1 대응 정합 확인

| BE M1 항목 | FE M1 대응 | 정합 결과 |
| --- | --- | --- |
| BE Story 5-4 UserUpdateRequestDTO 정리 | FE `username` grep + 제거 | `_____` |
| BE D8 CORS + OAuth URI → `thirdtool.dev` | FE E2E 검증 (FE-3-2) | `_____` |
| BE D9 GHA `deploy-fe.yml` spec | FE GHA 첫 실행 성공 | `_____` |
| BE D2·D3·D4 (BE 도메인) | FE에서 `api.thirdtool.dev` 호출 성공 | `_____` |
| BE D5·D6·D7 (FE 인프라) | FE `https://thirdtool.dev` 로드 | `_____` |
| BE Story 3 2-1·2-2 (AI Static Adapter) | (M1 통합 없음, M2에서) | — |

---

## FE-ADR-CANDIDATES 상태 전이

`FE-ADR-CANDIDATES.md` 기준.

- [ ] **P0-1** (인증 토큰 보관) — 결정 문서화 완료 여부: `_____` (M1에 FE-ADR 문서 작성했는가)
- [ ] **P0-2** (상태관리 유지) — 결정 문서화 완료 여부: `_____`
- [ ] **P0-3** (라우팅 유지) — 결정 문서화 완료 여부: `_____`
- [ ] **P1-7** (CloudFront 도메인 + Cache-Control) — 결정 문서화 완료 여부: `_____`
- [ ] **P1-9** (GHA deploy-fe.yml OIDC) — 결정 문서화 완료 여부: `_____`
- [ ] **P1-2** (dnd-kit 도입) — FE-ADR 초안 작성 여부 (Tier 2): `_____`

---

## 다음 마일스톤(FE M2 / 0.0.2v) 결정 보정

### 다음 주 잡힐 양 (속도 실측 후 조정)

- 본주 속도가 11 Story 통과 → M2는 동등하거나 +10% 잡기 가능
- 본주 속도가 7~9 Story → M2는 8~9 정도 보수적
- 본주 속도가 7 미만 → M2는 6~7 + 0.0.1v 잔여 흡수

### M2 우선순위 (M1 결과 반영)

- [ ] M1에서 미완료된 Tier 1 Story → M2 최우선
- [ ] AI Suggestion 통합 UI 착수 (BE Controller 노출 후)
- [ ] MePage 수정/삭제 UI 확장 (`Product.md` Epic 3)
- [ ] 2단계 confirm dialog 컴포넌트 표준화
- [ ] LoginPage USER_IS_SOCIAL 인라인 안내 + 소셜 CTA
- [ ] Lighthouse Performance 개선 첫 사이클 (M1 baseline 기반)
- [ ] `backend-boundary/error-codes.md` 갱신 (BE ErrorCode 확장 반영)
- [ ] `product-search.md` 첫 발 (BE M2가 검색 VO 진입 시)
- [ ] dnd-kit 실제 도입 (`product-learningFacade.md` Epic 2)

### brainstorming 0.0.2v 갱신 트리거

본주 결과로 FE 관점 후보들의 상태 전이 다수 발생. `brainstorming/0.0.2v/` (신설 예정)에서 반영.

특히 상태 전이 후보:
- **promoted**: `product-fe-cdn.md` Epic 1~5 (첫 배포 완결)
- **promoted**: `product-auth.md` MSW 8종 handler
- **promoted**: `Product.md` (User) LoginPage 상수화
- **promoted**: `FE-ADR-CANDIDATES.md` P0-1 (문서 작성 완료 시)
- **신규 후보**: Lighthouse baseline 이후 Performance 개선 우선순위 (LCP · 코드 스플리팅)
- **신규 후보**: BE 계약 drift 자동 탐지 (Zod schema vs BE Swagger 대조)
- **신규 후보**: `.env.local` git 실수 방지 pre-commit hook
- **신규 후보**: CloudFront Cache Hit Rate 모니터링 대시보드
- **신규 후보**: Web Vitals RUM 도입 시점

---

## 본 버전 동결 선언

위 종료 신호 6+ 충족 시 다음을 실행:

- [ ] `version/0.0.1v/` 폴더 내 6 파일(`infra.md`/`performance.md`/`outcome.md`/`cost.md`/`review.md`/`ux-test.md`) 모두 작성 완료
- [ ] git commit: `docs(fe-milestone): 0.0.1v 동결 — Tier 1 N/8 머지`
- [ ] 다음 폴더 생성 시 본 버전을 복사 후 갱신: `cp -r version/0.0.1v version/0.0.2v` → `milestone.md`만 새로 작성
- [ ] fe-brainstorming 0.0.2v 신설 검토
- [ ] `FE-ADR-CANDIDATES.md` 상태 전이 반영

종료 신호 6 미만이면:
- [ ] 0.0.1.1v 발행: `cp -r version/0.0.1v version/0.0.1.1v` → `milestone.md`에 연장 사유 + 남은 Story 명시
- [ ] 다음 주를 0.0.1.1v 패치 + 0.0.2v 둘로 나누지 않고 0.0.1.1v 완수 후 0.0.2v 진입
- [ ] BE M1 미통과가 원인이라면 BE M1 재시도 결과에 맞춰 자동 재조정 (FE는 대기)
