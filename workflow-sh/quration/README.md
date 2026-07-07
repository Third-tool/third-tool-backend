# workflow/quration/

> **목적**: 백엔드 개발자 **포트폴리오·자소서 큐레이션** 오퍼레이션 룸.
> **재료 창고 (원본)**: `workflow/task/milestones/version/{Nv}/` (박제) · `workflow/living-docs/` (인덱스) · `workflow/topologys/` (수평제약) · `docs/adr/` (결정 원본).
> **여기서 하는 일**: 창고의 재료를 큐레이션 관점으로 발췌 · 태깅 · 서사화 · 선정.

---

## 4-anchor 재확인

이 프로젝트의 `workflow/` 구조:

| 구간 | 역할 | 시간축 |
| --- | --- | --- |
| `task/milestones/version/{Nv}/` | 그 버전 결과·수치 스냅샷 (**박제**) | 그 시점 사진 |
| `living-docs/` | 지금 상태 자산 인덱스 (덮어쓰기) | 항상 최신 |
| `topologys/` | 수평 제약 pin (BC 경계·의존 방향·SLO 목표) | 구간 동안 고정 |
| `quration/` (본 폴더) | **포트폴리오 큐레이션 오퍼레이션** | 지원할 때마다 재구성 |

**원칙**: 원본은 창고에 그대로. 여기는 큐레이션 서사 + 참조 링크만.

---

## 폴더 구조

```
quration/
├── README.md          ← 본 파일. 진입 인덱스.
├── rules.md           ← 큐레이션 규칙 (등재 트리거·표현 형식·선정 기준)
├── themes.md          ← 8 theme 카탈로그 (아키텍처/DB/API/도메인/인증/인프라/관측성/협업)
├── milestones/
│   ├── 0.0.1v.md      ← M1 후보 (8건 시드 — 인프라·인증)
│   ├── 0.0.2v.md      ← M2 후보 (10건 시드 — 도메인·아키텍처·DB)
│   ├── 0.0.3v.md      ← M3 진행 중 (골격)
│   └── 0.0.4v.md      ← M4 미시작 (골격)
└── portfolio/
    ├── master-index.md ← 전체 후보 milestone × theme 매트릭스
    └── selected.md     ← 지원 시 최종 선정본 (스냅샷)
```

---

## 사용 흐름

### 1) 새 milestone이 완료되었을 때 (Curation intake)

1. `workflow/task/milestones/version/{Nv}/`의 outcome · adr · troubleshooting 스캔
2. `rules.md` §1 등재 트리거 (T1~T6) 중 하나라도 해당하는 항목 발굴
3. `milestones/{Nv}.md`에 `rules.md` §2 형식으로 후보 추가
4. `themes.md`의 8 theme 중 1~2개 태깅
5. `portfolio/master-index.md` 매트릭스 갱신

### 2) 자소서·포트폴리오 지원할 때 (Selection)

1. `portfolio/master-index.md`로 전체 매트릭스 훑기
2. JD(직무·기업) 관점으로 관련 theme 확인
3. `rules.md` §3 우선순위 기준으로 5~7개 선정
4. `portfolio/selected.md`에 배치 순서·근거 기록
5. 원본 STAR 서사 → 자소서 문장으로 재작성 (`selected.md` 내부)

### 3) 지원 후 회고

1. `selected.md` "회고" 섹션 기입 — 어떤 후보가 유효했나
2. 다음 지원 시 조정 사항 메모
3. 지원 이력 표에 기록

---

## Milestone 후보 현황 (2026-07-03 기준)

| Milestone | 후보 수 | 강한 theme | 특징 |
| --- | --- | --- | --- |
| **0.0.1v** | 8 | infra-deployment · auth-security | 인프라 셋업 서사 (신입/주니어 자소서 강함) |
| **0.0.2v** | 10 | domain · architecture · database | 결정·트레이드오프 서사 (시니어급 어필) |
| **0.0.3v** | 골격 | (진행 중) | workflow 3-anchor 정착 잠재 후보 |
| **0.0.4v** | 골격 | (미시작) | 프로덕션 운영·성능 최적화 예상 |

**총 18후보 시드**. milestone 진행에 따라 계속 증가.

---

## 참조

- 진입 규칙: [`rules.md`](rules.md)
- Theme 카탈로그: [`themes.md`](themes.md)
- 매트릭스 뷰: [`portfolio/master-index.md`](portfolio/master-index.md)
- 최종 선정본: [`portfolio/selected.md`](portfolio/selected.md)
- **원본 재료**:
  - `../task/milestones/version/{Nv}/` (박제 결과)
  - `../living-docs/` (현재 자산 인덱스)
  - `../topologys/` (수평 제약 pin)
  - `../../docs/adr/` (ADR 원본)

*최신 갱신: 2026-07-03 · 초기 셋업 · 18후보 시드 완료*
