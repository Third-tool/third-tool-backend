# FE-ADR 후보 우선순위 (workspectrum → fe-workspectrum 미러링 결과)

> `workspectrum/sdd/ADR-CANDIDATES.md`의 FE 대응 문서. 백엔드 ADR 후보 목록에서 FE 결정이 필요한 항목을 추출하고, FE 전용 결정(라이브러리 채택·상태 관리·라우팅 패턴 등)을 추가한 목록.
> 본 파일은 `workflow/`(gitignored) 하위 로컬 계획 자료. 각 항목이 FE-ADR로 정식 발행되는 시점은 해당 Epic / Story 진입 시 — 그때 `docs/adr/FE-ADR{NNN}.md` (또는 팀 합의 위치)로 이관.
>
> 우선순위 정의:
> - **P0** — 진행 중 Product가 즉시 의존하거나 이미 결정이 필요한 항목
> - **P1** — 곧 진입할 Product/Epic이 의존. FE-ADR 부재 시 Story 시점에 즉흥 결정 위험
> - **P2** — 미래 결정. 현재는 Open Question / 잠정 정책으로 충분

---

## P0 — 즉시 조치 (진행 중 / 차단 가능)

| # | 제목 | 근거 | 조치 |
| --- | --- | --- | --- |
| P0-1 | **인증 토큰 보관 확정** (AT HttpOnly Cookie / RT React 메모리) | `product-auth.md` 설계 결정. 백엔드 ADR-AUTH-001 정합. 현재 `lib/api/client.ts`에 이미 구현됨 → 결정 문서화만 필요 | product-auth Epic 1 진입 전 FE-ADR 작성 (BE ADR 참조 + FE 실체 위치 명시) |
| P0-2 | **상태 관리 라이브러리 유지 결정** (Context + TanStack Query, Zustand/Jotai 미도입) | 현재 App.tsx가 AuthProvider + DeckProvider 2개 Context로 충분. TanStack Query가 서버 상태 담당 → 별도 스토어 도입 근거 없음 | 신규 Feature 도입 시 별도 스토어 유혹 차단 근거 문서화 |
| P0-3 | **라우팅 라이브러리 유지** (React Router v6, TanStack Router 미도입) | 현재 `createBrowserRouter` 잘 동작 중. 이관 비용 대비 이득 낮음 | Product 신규 진입 시 라이브러리 재검토 트리거 명시 |

## P1 — 진행 중 / 직전 Product 의존

| # | 제목 | 출처 | 비고 |
| --- | --- | --- | --- |
| P1-1 | **폼 라이브러리 도입 시점** (native → react-hook-form + Zod resolver) | Product.md(User), product-card 폼, product-media 폼 | 신규 폼이 3개 이상 늘어나는 시점. 현재는 native + Zod 수동. Feature-story 단위로 규모 산정 |
| P1-2 | **드래그·리오더 라이브러리** (dnd-kit) 채택 | product-learningFacade(축/주제 reorder), product-ai-interactive-roadmap(diff reorder), product-media(첨부 순서) | 3곳 재사용 명확 → 도입 결정 임박. Bundle size + 접근성(키보드 지원) 근거 |
| P1-3 | **CORS + SameSite 프로덕션 정합성** | product-auth Epic 4, product-fe-cdn Epic 5 | dev(Lax) → prod(None + Secure) 전환 시 검증 매트릭스 표준화 |
| P1-4 | **`.env.production` / `.env.development` / `.env.local` 정책** | product-fe-cdn | `VITE_API_BASE_URL` 외에 MSW/디버그 스위치 등 확장 시 규칙 문서화 |
| P1-5 | **REFRESH_TERMINAL_CODES 매핑 표** 유지 정책 | product-auth Epic 1 | 백엔드 ErrorCode 확장 시 FE 인터셉터 매핑 갱신 프로세스 명문화 |
| P1-6 | **낙관 update + 롤백 표준** | product-card, product-learningFacade | `useMutation` `onMutate/onError` 패턴 재사용성 (공용 hook `useOptimisticMutation` 후보) |
| P1-7 | **CloudFront 도메인 확정 + Cache-Control 이중 정책** (ADR-CDN-004 FE 대응) | product-fe-cdn | 백엔드 ADR-CDN-004와 정합해서 FE-ADR 발행 |
| P1-8 | **Zod 스키마 위치 정책** (`lib/api/schemas/`에 콜로케이션, `types/` 미사용) | 현재 관행 문서화 | 신규 개발자 온보딩 기준 |

## P1 — 대응 필요 (인프라 / 배포)

| # | 제목 | 출처 |
| --- | --- | --- |
| P1-9 | **GHA `deploy-fe.yml` OIDC + Cache-Control 분리 sync** (ADR-CDN-005 FE 대응) | product-fe-cdn Epic 4 |
| P1-10 | **Lighthouse CI 통합 시점** (GHA step vs 별도 workflow) | product-fe-cdn 열린 질문 |
| P1-11 | **Sentry 도입 시점 + requestId breadcrumb** | product-auth 열린 질문 |

## P2 — 미래 결정 / Open Question 단계

| # | 제목 | 출처 |
| --- | --- | --- |
| P2-1 | **i18n 도입 트리거** (한국어 단일 → 다국어) | 프로덕트 확장 시 |
| P2-2 | **디자인 토큰 관리** (Tailwind v4 inline vs CSS variable 분리 + dark mode) | product-learningFacade 열린 질문 |
| P2-3 | **명령 팔레트(⌘K) 검색 진입점** | product-search 열린 질문 |
| P2-4 | **Service Worker 오프라인 캐시 정책** | product-card 열린 질문, product-notification Epic 4 |
| P2-5 | **FCM SDK + Service Worker 위치** | product-notification (backlog) |
| P2-6 | **트리 가상화 도입** (react-virtual 등) | product-learningFacade — 노드 수 임계 도래 시 |
| P2-7 | **미리보기 배포(Preview URL)** — Vercel 스타일 | product-fe-cdn 열린 질문 |
| P2-8 | **admin role 계층 확장** (SUPER_ADMIN 도입 시) | product-admin (backlog) |
| P2-9 | **다중 디바이스 세션 UI** (RT 분리) | product-auth 열린 질문 (BE v2 대기) |
| P2-10 | **CSRF 토큰 명시 도입** | product-auth 열린 질문 |
| P2-11 | **로드맵 세션 스트리밍 응답 UI** (SSE/WebSocket) | product-ai-interactive-roadmap 열린 질문 |
| P2-12 | **PR 미리보기 URL / A/B 테스트 인프라** | product-fe-cdn v2 |

---

## 백엔드 ADR과의 정합 매트릭스

FE가 반드시 BE ADR을 참조해야 하는 항목 (BE 결정 = FE 정합 강제):

| BE ADR | FE-ADR 대응 |
| --- | --- |
| ADR-AUTH-001 (Token Storage Strategy) | FE-ADR P0-1 (인증 토큰 보관 확정) |
| ADR-CDN-001~005 (S3+CF+OAC / us-east-1 ACM / SPA fallback / cache-control / GHA trigger) | FE-ADR P1-7, P1-9 |
| ADR-SEARCH-001~004 (OpenSearch / 동기화 / 인덱스 구조 / opensearch-java) | FE는 서버 API 계약만 소비 → FE-ADR 불필요 |
| ADR-AI-001~004 (Port-Adapter / Spring AI / Fallback 정책 / 단계) | FE는 API 계약 + suggestionsAvailable 플래그만 → FE-ADR 불필요 |
| ADR-MEDIA-001~003 (presigned URL / CloudFront OAC / nightly cleanup) | FE는 계약 소비 → FE-ADR 최소 (업로드 hook 위치만) |
| ADR-NOTIFICATION-001~003 (FCM / 트리거 이벤트 / preference) | FE-ADR P2-5 (SDK 도입 결정) |
| ADR-ADMIN-001~003 (엔드포인트 격리 / role in-user / 감사) | FE-ADR P2-8 (role 계층) |

---

## 후속 액션 제안

1. **즉시** — P0-1 (인증 토큰) FE-ADR 초안 작성. product-auth Epic 1 진입 시 참조
2. **다음 Product 진입 전** — 각 P1 항목 중 해당 Product가 의존하는 것부터 FE-ADR 작성
3. **분기별 점검** — P2 항목을 Product 진입에 따라 P1로 승격 또는 Open Question 유지 판단

## 관련 문서

- 백엔드 원본: `workflow/task/pes/workspectrum/sdd/ADR-CANDIDATES.md`
- FE Product 인덱스: `sdd/{done,in-progress,backlog}/product-*.md`
- 백엔드 ADR: `docs/adr/` (BE 저장소)
- FE-ADR 예정 위치: 팀 합의 후 결정 (`docs/adr/FE-*.md` 또는 별도 저장소)
