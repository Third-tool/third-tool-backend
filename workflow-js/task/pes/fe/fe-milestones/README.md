# fe-milestones

> `workflow/task/pes/fe/fe-workspectrum/`에 정의된 큰 작업 단위(FE Product)들 위에 올라타는 **1주 단위 작업량 분배 + FE 결과 추적** 영역.
> 백엔드 `workflow/task/milestones/`의 FE 버전. 골격은 그대로, 지표는 FE 관점(UX·Web Vitals·번들·배포 상태)으로 재해석.

---

## 운영 모델

- **1주 = 1 마일스톤 = 1 버전 폴더** (`version/0.0.X v/`)
- 각 버전 폴더는 다음 7 파일로 구성:
  - `milestone.md` — 본주 잡힌 양 + 일정 + 종료 신호
  - `infra.md` — FE 배포 인프라 진척 (S3/CloudFront/GHA/env/도메인)
  - `performance.md` — FE 성능 baseline (Web Vitals · Lighthouse · 번들 · TanStack Query)
  - `outcome.md` — 머지된 FE Story + UX 자산 변화 + 사용자 경험 향상
  - `cost.md` — FE 비용 성과 (CloudFront/S3/도메인 + 도구 라이선스)
  - `review.md` — 회고 + 다음 버전 진입 신호
  - `ux-test.md` — FE 화면 직접 사용 점검표 (a11y·visual·flow·edge)

> 버전마다 추가 기록 항목 발견 시 같은 폴더 안에 새 파일을 추가한다 (예: `accessibility.md`, `bundle.md`, `i18n.md`).

### 버전 명명 (SemVer 응용)

- **PATCH (0.0.X v)** — 주차 진행 마일스톤. 매주 1회 bump
- **MINOR (0.X.0 v)** — 운영 방식 큰 변경 (예: 파일 종류 추가, 추적 방식 변경)
- **MAJOR (X.0.0 v)** — 마일스톤 패키지 자체 운영 모델 변경

### 새 버전 진입 시

```
cp -r version/0.0.X v version/0.0.Y v
# milestone.md만 다시 작성 (나머지 6 파일은 템플릿 그대로 사용 후 D6-7에 채움)
```

---

## 폴더 구조

```
fe-milestones/
├── README.md           (본 문서)
├── references/
│   └── 001.md          (FE 마일스톤 패키지 의도)
└── version/
    ├── 0.0.1v/         (M1 — 2026-06-23 ~ 2026-06-28, BE M1과 동기)
    │   ├── milestone.md
    │   ├── infra.md
    │   ├── performance.md
    │   ├── outcome.md
    │   ├── cost.md
    │   ├── review.md
    │   └── ux-test.md
    └── 0.0.2v/         (M2 — 다음 주, 예정)
        └── ...
```

---

## 진척 인덱스

| 버전 | 기간 | 테마 | 동결 상태 |
| --- | --- | --- | --- |
| [0.0.1v](./version/0.0.1v/) | 2026-06-23 ~ 06-28 | BE 1차 배포 인프라(BE M1)에 맞춘 FE 첫 배포 · `thirdtool.dev` 진입 · 스키마 정합 · UX 검증 baseline | 진행 중 |

> 새 버전이 생기면 본 표 상단에 추가하고 이전 행은 그대로 둔다.

---

## 백엔드 마일스톤과의 동기화

FE 마일스톤 버전은 **백엔드 마일스톤 버전과 정확히 같은 주기**로 발행한다.

- BE `0.0.1v` (2026-06-23 ~ 06-28) ↔ FE `0.0.1v` (동일 주간)
- BE 산출물이 FE의 이번 주 작업 범위를 결정:
  - BE가 `thirdtool.dev` 인프라 spec을 완성했으므로 → FE는 배포·검증
  - BE가 `UserUpdateRequestDTO`에서 `username` 제거했으므로 → FE는 관련 참조 정리
  - BE가 AI Suggestion Static 응답 가능해졌으므로 → FE는 통합 준비만(Controller 노출은 M2)

각 FE 마일스톤의 `milestone.md`는 `## 백엔드 M{N} 참조` 섹션에서 대응 관계를 명시한다.

---

## 다른 영역과의 관계

| 영역 | 관계 |
| --- | --- |
| `workflow/task/pes/fe/fe-workspectrum/` | FE Product = 큰 작업 단위. 마일스톤은 거기서 1주 분량을 잘라 가져옴 |
| `workflow/task/pes/brainstorming/` | 마일스톤 결과 → brainstorming 후보 상태 전이. 새 버전 생성 시 trigger |
| `workflow/task/milestones/` | 백엔드 마일스톤. 동일 버전끼리 짝지어 동기. 백엔드 `infra.md` D8·D9·D10 항목이 FE M1의 진입점 |
| `workflow/task/pes/handoff/` | BE→FE 핸드오프 문서 (`aws-setup-0.0.1v.md` 등). FE 마일스톤이 이 문서를 소비 |
| `docs/adr/` | 마일스톤 진행 중 결정된 FE 결정은 FE-ADR로 별도 정착 (`FE-ADR-CANDIDATES.md` 참조) |

---

## 작성 시 자가 점검

각 버전 진입 시:

- [ ] 본주 잡힌 양이 평균 속도의 ±50% 범위인가 (과도/과소 방지)
- [ ] 백엔드 대응 마일스톤 참조가 명시되어 있는가 (`## 백엔드 M{N} 참조` 섹션)
- [ ] 의존 chain이 시각화되어 있는가 (BE 사용자 액션 대기 항목 포함)
- [ ] 종료 신호가 정량적인가 (예: "Lighthouse Performance ≥ 80" / "https://thirdtool.dev 200 OK")
- [ ] 종료 시 채워야 할 6 파일 항목이 미리 골격으로 있어 D6-7에 회상하지 않아도 되는가
- [ ] 본주 결과가 어떤 brainstorming 후보 상태를 전이시킬지 예상 1줄

종료 시:

- [ ] 6 sub-file(infra/performance/outcome/cost/review/ux-test)이 모두 채워졌는가
- [ ] review.md의 종료 신호가 셀프 체크되었는가
- [ ] 백엔드 마일스톤의 대응 항목 결과와 정합성이 확인되었는가
- [ ] 다음 버전 우선순위가 결정되었는가
- [ ] brainstorming 갱신이 필요하면 트리거를 review.md에 기록했는가

---

## FE 관점 지표 요약 (백엔드 대비 재해석)

백엔드 마일스톤이 "인프라·비용·성능" 3축으로 결과를 추적한다면 FE는 다음 4축으로 재해석:

| 축 | FE 지표 예시 | 파일 |
| --- | --- | --- |
| **UX 성과** | 사용자 시나리오 통과율, 인터랙션 완료율, empty state 인지, error 회복 UX | `outcome.md`, `ux-test.md` |
| **성능** | LCP / FID / CLS / INP / TTFB, Lighthouse 4카테고리 점수, 번들 사이즈, TanStack Query 캐시 hit | `performance.md` |
| **배포·인프라** | CloudFront 배포 상태, GHA workflow 성공률, 캐시 hit rate, 도메인 라우팅, 환경변수 wiring | `infra.md` |
| **비용** | CloudFront/S3/Route53 (BE와 공유), 도구 라이선스(Figma·Sentry 등), 개발 시간 투자 | `cost.md` |
