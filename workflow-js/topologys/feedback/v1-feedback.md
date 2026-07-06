# Pinned Topology — `feedback` (Phase: `v1`) — Reviewer 피드백 loop

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님 (운영 절차는 별도 룰 파일)
> - vocabulary 아님 (실제 reviewer prompt · 실제 reviewer 보고 양식은 여기 없다)

---

## 0. 유효 구간 (Validity)

| 항목 | 값 |
| --- | --- |
| Pinned at | 2026-06-15 |
| Valid for | 본 워크플로우 v1 구간 — re-pin trigger 발생 전까지 |
| Owner | 메인 Claude + 사용자 합의 |
| Re-pin trigger | 아래 §4 |

---

## 1. How Claude Code must use this file — 행동 계약

1. **Story 작업 완료 후 push 직전에 이 파일을 본다.** §2의 loop는 본 구간 동안 **고정 제약**.
2. **이 topology 안에서 reviewer prompt 어휘만 자유롭게 채운다.** 발사 순서·관점 수·사용자 사전 확인 경계는 건드리지 않는다.
3. **단일 메인이 자가 점검으로 reviewer를 대체하고 싶어지면 STOP하고 보고한다.** 그건 노드의 제거다.
4. **reviewer가 코드를 직접 수정하려 들면 보고**한다. 그건 읽기 전용 경계 위반이다.
5. **이 파일에 prompt 본문을 채우지 않는다.** 그건 §3 포인터로.

---

## 2. The pinned topology

### Nodes
- `작업 산출물` — Story 단위로 누적된 코드/테스트/문서 변경의 묶음
- `변경 요약본` — 메인이 단독 작성하는 사용자 사전 확인 자료
- `사용자 사전 확인` — 5관점 발사 전 사용자의 추가 의심 영역 수집 단계
- `5관점 reviewer` — Domain · Architecture · API/Exception · Test · Sceptical 의 병렬 subagent 그룹
- `종합 보고` — 메인이 5건 결과를 통합한 단일 보고
- `사용자 의사결정` — 수용 / 거부 / 추가 작업 / 보강 후 재발사 중 택일
- `보강 루프` — 의사결정이 "추가 작업"일 때 작업 산출물로 복귀
- `push 게이트` — 의사결정 통과 후 push 가능 지점

### Edges
- `작업 산출물` → `변경 요약본` : 메인 단독, 1:1
- `변경 요약본` → `사용자 사전 확인` : 사용자 응답 수집
- `사용자 사전 확인` → `5관점 reviewer` : 추가 의심 영역을 Sceptical prompt 상단에 주입 후 병렬 발사
- `5관점 reviewer` → `종합 보고` : N:1 집계 (5건 → 1건)
- `종합 보고` → `사용자 의사결정` : 1:1
- `사용자 의사결정` → `push 게이트` : 통과
- `사용자 의사결정` → `보강 루프` → `작업 산출물` : 거부/추가 작업 시 복귀

### Boundaries
- **발사 경계**: `변경 요약본` + `사용자 사전 확인` 통과 전에는 `5관점 reviewer` 발사 금지
- **읽기 전용 경계**: `5관점 reviewer`는 코드 수정 권한 없음. 수정은 사용자 의사결정 후 메인만
- **병렬 경계**: 5관점은 단일 메시지에서 동시 발사. 직렬 호출 금지 (관점 독립성)
- **push 경계**: `사용자 의사결정` 통과 전 push 금지

### Invariants
- 순서 위반 0건: `변경 요약본` → `사용자 사전 확인` → `5관점 reviewer` → `종합 보고` → `사용자 의사결정` → `push 게이트`
  - 감지법: 워크플로우 룰 + Story 회고 시 점검
- 관점 수는 5 미만으로 축소되지 않는다
  - 감지법: 리뷰 호출 코드 패턴 검사
- reviewer가 코드 변경을 커밋한 사례 0건
  - 감지법: 커밋 author 검토 + reviewer subagent 권한 설정
- 사용자 의사결정 없이 push된 사례 0건
  - 감지법: push 시점 회고

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 reviewer prompt 골격 · 관점별 필터 · 출력 형식 → `.claude/rules/review.md` §4
- 변경 요약본 작성 형식 (카테고리 표 · 핵심 결정 표기) → `.claude/rules/review.md` §3
- 스킵 조건 · 비용/시간 가이드 → `.claude/rules/review.md` §6, §8
- 작업 흐름 전체에서의 위치 → `.claude/rules/workflow.md` Step 4
- push 게이트 통과 후 push 절차 → `.claude/rules/pr-commit.md` §6

---

## 4. Re-pin trigger

- 관점 수 변경 (5 → 4 또는 5 → 6) — 관점 부재가 누락의 원인이므로 신중히
- `사용자 사전 확인` 단계 제거 또는 위치 변경
- reviewer 권한 확대 (읽기 → 쓰기)
- 병렬 발사 → 직렬 발사 변경
- subagent 구현체 변경으로 reviewer 호출 패턴이 달라짐
- push 게이트 자동화 (사용자 의사결정 우회 흐름 도입)
