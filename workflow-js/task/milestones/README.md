# milestones

> `workflow/product/pes/`에 정의된 큰 작업 단위(Product)들 위에 올라타는 **1주 단위 작업량 분배 + 결과 추적** 영역.

---

## 운영 모델

- **1주 = 1 마일스톤 = 1 버전 폴더** (`version/0.0.X v/`)
- 각 버전 폴더는 다음 6 파일로 구성:
  - `milestone.md` — 본주 잡힌 양 + 일정 + 종료 신호
  - `infra.md` — 인프라 진척 + 배포 산출물
  - `performance.md` — 성능 / 자원 / 안정성 baseline
  - `outcome.md` — 머지된 Story + 사용자·기능·기술 자산 변화
  - `cost.md` — 비용 성과 (AWS / LLM / 기타)
  - `review.md` — 회고 + 다음 버전 결정 보정

> 버전마다 추가 기록 항목 발견 시 같은 폴더 안에 새 파일을 추가한다 (예: `security.md`, `accessibility.md`).

### 버전 명명 (SemVer 응용)

- **PATCH (0.0.X v)** — 주차 진행 마일스톤. 매주 1회 bump
- **MINOR (0.X.0 v)** — 운영 방식 큰 변경 (예: 파일 종류 추가, 추적 방식 변경)
- **MAJOR (X.0.0 v)** — 마일스톤 패키지 자체 운영 모델 변경

### 새 버전 진입 시

```
cp -r version/0.0.X v version/0.0.Y v
# milestone.md만 다시 작성 (나머지 5 파일은 템플릿 그대로 사용 후 D6에 채움)
```

---

## 폴더 구조

```
milestones/
├── README.md           (본 문서)
├── references/
│   └── 001.md          (마일스톤 패키지 의도)
└── version/
    ├── 0.0.1v/         (M1 — 2026-06-23 ~ 2026-06-28)
    │   ├── milestone.md
    │   ├── infra.md
    │   ├── performance.md
    │   ├── outcome.md
    │   ├── cost.md
    │   └── review.md
    └── 0.0.2v/         (M2 — 다음 주, 예정)
        └── ...
```

---

## 진척 인덱스

| 버전 | 기간 | 테마 | 동결 상태 |
| --- | --- | --- | --- |
| [0.0.1v](./version/0.0.1v/) | 2026-06-23 ~ 06-28 | 로깅 + 메트릭 + 첫 dev 배포 + AI Epic 2 + User BC 종료 | 진행 중 |

> 새 버전이 생기면 본 표 상단에 추가하고 이전 행은 그대로 둔다.

---

## 다른 영역과의 관계

| 영역 | 관계 |
| --- | --- |
| `workflow/product/pes/` | Product = 큰 작업 단위. 마일스톤은 거기서 1주 분량을 잘라 가져옴 |
| `workflow/product/pes/brainstorming/` | 마일스톤 결과 → brainstorming 후보 상태 전이 (promoted/resolved 등). 새 버전 생성 시 trigger |
| `workflow/living-docs/` | 마일스톤에서 머지된 Story가 living-docs 갱신을 트리거 (fe-user-senario, fe-boundary-trace 등) |
| `docs/adr/` | 마일스톤 진행 중 결정된 큰 선택은 ADR로 별도 정착 |

---

## 작성 시 자가 점검

각 버전 진입 시:

- [ ] 본주 잡힌 양이 평균 속도의 ±50% 범위인가 (과도/과소 방지)
- [ ] 의존 chain이 시각화되어 있는가 (병렬 가능 묶음 + 직렬 구간)
- [ ] 종료 신호가 정량적인가 ("머지 N개" / "응답 200 OK" 등)
- [ ] 종료 시 채워야 할 5 파일 항목이 미리 골격으로 있어 D6에 회상하지 않아도 되는가
- [ ] 본주 결과가 어떤 brainstorming 후보 상태를 전이시킬지 예상 1줄

종료 시:

- [ ] 5 sub-file이 모두 채워졌는가
- [ ] review.md의 종료 신호 8개가 셀프 체크되었는가
- [ ] 다음 버전 우선순위가 결정되었는가
- [ ] brainstorming 갱신이 필요하면 0.0.X v snapshot 갱신 트리거를 review.md에 기록했는가
