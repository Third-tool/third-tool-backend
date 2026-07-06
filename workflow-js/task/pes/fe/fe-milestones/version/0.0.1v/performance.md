# FE 0.0.1v / Performance baseline

> FE M1 종료 시점에 **브라우저에서 실제 로드되는 FE의 성능 baseline**을 기록.
> 백엔드 `performance.md`가 서버 P95/P99·JVM·HikariCP를 다루는 반면, 본 파일은 **Web Vitals · Lighthouse · 번들 사이즈 · TanStack Query 캐시** 등 FE 진영의 성능을 다룬다.
> 본 파일은 D6에 CloudFront 첫 배포가 완료된 후 측정.

---

## 측정 도구

- **Chrome DevTools Lighthouse** — Performance / Accessibility / Best Practices / SEO 4카테고리. Mobile + Desktop 각각
- **Chrome DevTools Performance panel** — LCP · CLS · INP · TTFB 실측
- **Chrome DevTools Coverage** — 미사용 JS · CSS 비율
- **`npm run build` 결과** — Vite 번들 사이즈 (chunk별)
- **CloudFront `CacheHitRate` 메트릭** — CDN 캐시 효율 (CloudWatch)
- (M2 이후) **web-vitals npm 라이브러리** — 실사용자 RUM (선택)

---

## Web Vitals baseline (M1 — smoke 수준)

CF 첫 배포 활성 후 다음 페이지에서 각각 1회 측정. 브라우저 캐시 clear 후 시작.

### `/` (LandingPage)

| 지표 | M1 측정값 | Good 임계 (Google) | 참고 |
| --- | --- | --- | --- |
| LCP (Largest Contentful Paint) | `_____ ms` | ≤ 2500 ms | 초기 로드 응답성 |
| FID / INP (Interaction to Next Paint) | `_____ ms` | ≤ 200 ms | INP 우선 (2024+ 표준) |
| CLS (Cumulative Layout Shift) | `_____` | ≤ 0.1 | 시각적 안정성 |
| TTFB (Time to First Byte) | `_____ ms` | ≤ 800 ms | CF Edge → Origin |
| FCP (First Contentful Paint) | `_____ ms` | ≤ 1800 ms | 초기 페인트 |
| Speed Index | `_____ ms` | ≤ 3400 ms | 시각적 완료 |

### `/login` (LoginPage)

| 지표 | M1 측정값 | Good 임계 |
| --- | --- | --- |
| LCP | `_____ ms` | ≤ 2500 |
| INP | `_____ ms` | ≤ 200 |
| CLS | `_____` | ≤ 0.1 |
| TTFB | `_____ ms` | ≤ 800 |

### `/home` (인증 후, HomePage)

| 지표 | M1 측정값 | Good 임계 |
| --- | --- | --- |
| LCP | `_____ ms` | ≤ 2500 |
| INP | `_____ ms` | ≤ 200 |
| CLS | `_____` | ≤ 0.1 |
| TTFB | `_____ ms` | ≤ 800 |
| API `/user` 응답까지 시간 | `_____ ms` | 참고용 |
| API `/learning-facade` 응답까지 시간 | `_____ ms` | 참고용 |

> M1은 P50만 확인 (단일 실측). P95는 M2 이후 RUM 도입 시.

---

## Lighthouse baseline (Tier 2 · Want)

CF 첫 배포 활성 후 실행. Mobile · Desktop 2회.

### Mobile (Moto G Power 프로파일)

| 카테고리 | 점수 (0~100) | M2 목표 |
| --- | --- | --- |
| Performance | `_____` | ≥ 80 |
| Accessibility | `_____` | ≥ 90 |
| Best Practices | `_____` | ≥ 90 |
| SEO | `_____` | ≥ 80 |

### Desktop

| 카테고리 | 점수 | M2 목표 |
| --- | --- | --- |
| Performance | `_____` | ≥ 90 |
| Accessibility | `_____` | ≥ 95 |
| Best Practices | `_____` | ≥ 95 |
| SEO | `_____` | ≥ 90 |

### Lighthouse 지적 사항 요약 (Top 5)

| 순위 | 지적 | 영향 카테고리 | M2 개선 후보 |
| --- | --- | --- | --- |
| 1 | `_____` | `_____` | `_____` |
| 2 | `_____` | `_____` | `_____` |
| 3 | `_____` | `_____` | `_____` |
| 4 | `_____` | `_____` | `_____` |
| 5 | `_____` | `_____` | `_____` |

---

## 번들 사이즈 baseline

`npm run build` 후 `dist/` 실측.

### 총량

| 항목 | M1 측정값 | M2 목표 |
| --- | --- | --- |
| `dist/` 전체 (unzipped) | `_____ MB` | 유지 또는 감소 |
| `dist/` 전체 (gzipped) | `_____ KB` | 유지 |
| `dist/index.html` | `_____ KB` | ≤ 5 KB |

### JS chunk 분포 (Vite 기본)

| chunk | M1 측정값 | 비고 |
| --- | --- | --- |
| main entry (`index-*.js`) | `_____ KB (gzip: _____ KB)` | 초기 로드 |
| vendor (`vendor-*.js`) | `_____ KB (gzip)` | React + TanStack Query + Axios + Zod + Router |
| Tailwind CSS (`index-*.css`) | `_____ KB (gzip)` | Tailwind v4 |
| 최대 단일 chunk | `_____ KB` | 코드 스플리팅 후보 |

### 라이브러리 기여도 상위 5개 (Bundle Analyzer 시 채움)

| 라이브러리 | 크기 (KB gzip) | 대체 후보 |
| --- | --- | --- |
| react + react-dom | `_____` | 없음 (필수) |
| @tanstack/react-query | `_____` | 유지 |
| react-router-dom | `_____` | 유지 |
| axios | `_____` | 유지 |
| zod | `_____` | 유지 |
| isomorphic-dompurify | `_____` | 사용 위치 확인 (marked 렌더) |
| marked | `_____` | Card mainNote 렌더 전용 → dynamic import 검토 |
| @iconify/react | `_____` | Icon tree-shaking 확인 |

---

## TanStack Query 캐시 관찰 (선택 · 개발자 도구)

React Query DevTools + Chrome DevTools에서 관찰.

| 쿼리 | M1 캐시 hit 관찰 | staleTime | gcTime |
| --- | --- | --- | --- |
| `['user']` | `_____` | 30s (기본) | 5m |
| `['learning-facade']` | `_____` | 30s | 5m |
| `['cards', ...]` | `_____` | 30s | 5m |
| `['deck', ...]` | `_____` | 30s | 5m |

> M1은 관찰만. M2 이후 staleTime tuning 후보를 review.md에 축적.

---

## CDN 성능 (BE M1 통과 후 D6 관측)

CloudFront CloudWatch 메트릭 (첫 24시간).

| 메트릭 | M1 관측값 | 목표 |
| --- | --- | --- |
| `CacheHitRate` (정적 자산) | `_____ %` | > 95% |
| `CacheHitRate` (index.html) | `_____ %` | 낮음 (no-cache라 정상) |
| `OriginLatency` (S3 fetch) | `_____ ms` | < 200 ms |
| `4xxErrorRate` | `_____ %` | < 0.1% |
| `5xxErrorRate` | `_____ %` | 0% |
| 총 Request 수 (24h) | `_____` | 참고 (트래픽 없음) |

---

## 자원 사용 (브라우저 관점)

Chrome DevTools Performance 패널로 `/home` 진입 시 5초간 측정.

| 항목 | M1 측정값 | 참고 |
| --- | --- | --- |
| JS heap size 최대 | `_____ MB` | 가벼울수록 좋음 |
| Long Task 발생 (>50ms) | `_____ 건` | INP 영향 |
| Layout Shift 발생 | `_____ 건` | CLS 영향 |
| Long Frame (>16ms) | `_____ 건` | 부드러움 |
| main thread blocking 총합 | `_____ ms` | TBT 지표 |

---

## 안정성 baseline (FE)

| 항목 | M1 측정값 |
| --- | --- |
| `npm run typecheck` 통과 | (예/아니오) `_____` |
| `npm run lint --max-warnings 0` 통과 | (예/아니오) `_____` |
| `npm test` (Vitest) 통과 | (건수 / 전체) `_____ / _____` |
| 브라우저 콘솔 에러 (D6 초기 로드) | `_____` 건 |
| Sentry 도입 여부 | 아니오 (M1 범위 외) |
| 24시간 CF 5xx 누적 | `_____` |

---

## 발견된 병목 / 의외점

> D6 종료 시점에 채움.

| 병목 | 증상 | 가설 | M2 액션 후보 |
| --- | --- | --- | --- |
| (예시) main chunk 큼 | 초기 로드 LCP 3s+ | vendor 분리 필요 | Vite `manualChunks` 설정 |
| | | | |

---

## 성능 향상 측면 — 본주 작업이 만든 변화

| 작업 | 성능 영향 |
| --- | --- |
| CloudFront edge 캐시 도입 | LCP 개선 (Origin 왕복 제거) — 실측: 로컬 `_____ ms` vs CF `_____ ms` |
| 해시 자산 immutable 캐시 | 재방문 시 LCP 대폭 감소 (예상) |
| index.html no-cache | 배포 즉시 반영 (LCP 손해 → TTFB 증가로 수용) |
| GHA 자동 배포 | 배포 리드타임 감소 (수동 5+분 → 자동 `_____ 분`) |
| Zod 스키마 사전 검증 | 잘못된 서버 응답 대응 UX (성능 X, 안정성 O) |

---

## M2 성능 액션 후보

- **코드 스플리팅** — 라우트 단위 dynamic import (`features/ai-roadmap`, `features/admin` 등 대형 feature)
- **`marked` + `DOMPurify` lazy load** — Card 상세 진입 시만 로드 (`/archive/:cardId`)
- **font 최적화** — self-hosting + preload
- **이미지 최적화** (`product-media.md` 진입 시)
- **React Query staleTime tuning** — 자주 변하지 않는 쿼리(`['user']`) 5분+
- **Vite build.rollupOptions.output.manualChunks** — vendor 분리 최적화
- **Preconnect / dns-prefetch** — `api.thirdtool.dev`
- **Lighthouse CI 자동화** — GHA step 통합

---

## M2 이후 RUM 도입 검토

M1은 단일 실측 (P50). 실사용자 P95/P99·지역별 성능·재방문 캐시 hit는 RUM 없이 못 봄.

- **web-vitals npm** — 가장 가볍게 → Google Analytics 4로 전송 또는 자체 endpoint
- **Sentry Performance** — Sentry 도입 시 자동. `FE-ADR-CANDIDATES.md` P1-11
- **CloudFront Real-time logs** — CDN 관점 (사용자 관점 아님)

결정은 `FE-ADR-CANDIDATES.md` P2 항목으로 유예.
