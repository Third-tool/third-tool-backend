# Pinned Topology — `runbook-authoring` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님 (runbook 자체는 별도. 본 파일은 그것을 **쓰는 법**)
> - vocabulary 아님 (실제 시나리오·쿼리·명령어는 여기 없다)

**목적**: 장애 대응 문서 (runbook) 의 작성·구조·유지 원칙을 pin. 프로덕션 진입 (M7) 이후 반복 확장 · 릴리스 후 관찰 데이터 축적 시 계속 갱신.

---

## 0. 유효 구간 (Validity)

| 항목 | 값 |
| --- | --- |
| Pinned at | 2026-07-21 |
| Valid for | 본 워크플로우 v1 구간 — re-pin trigger 발생 전까지 |
| Owner | 메인 Claude + 사용자 합의 |
| Re-pin trigger | 아래 §4 |

---

## 1. How Claude Code must use this file — 행동 계약

1. **runbook 신설·수정 작업 시 이 파일을 먼저 읽는다.** §2의 구조·시나리오 개수·판별/대응 3섹션은 본 구간 **고정 제약**.
2. **runbook 안에 실측 로그 예시 없이 가상 시나리오만 채우려는 정황이 보이면 STOP하고 보고한다.** 실효성 없음.
3. **판별 쿼리·임시 대응·근본 대응 3섹션 중 하나라도 누락된 정황이 보이면 보고**한다.
4. **runbook을 코드 저장소가 아닌 외부 wiki에만 두려는 정황이 보이면 보고**한다. 진실 소스 분산 위험.
5. **runbook이 6개월 이상 갱신 없는 정황이 관찰되면 보고**한다. stale runbook은 오히려 위험.
6. **이 파일에 어휘를 추가하지 않는다.** 실제 시나리오는 §3 포인터로.

---

## 2. The pinned topology

### Nodes
- `runbook 파일` — 특정 장애 유형에 대한 대응 절차 문서 (`docs/runbooks/{topic}.md`)
- `장애 시나리오` — 재현 가능한 특정 문제 (에러·성능 저하·데이터 이상 등)
- `판별 쿼리` — 문제 발생 여부 판정 (log grep · SQL · 메트릭 임계값)
- `임시 대응` — 즉시 실행 가능한 완화책 (rollback · 재시작 · 트래픽 차단)
- `근본 대응` — 재발 방지 (코드 수정 · config 조정 · 인프라 개선)
- `실측 로그 예시` — 실제 관찰된 로그·메트릭 값 (스크린샷 or grep 결과)
- `runbook 인덱스` — `docs/runbooks/README.md` 등 진입 지도
- `갱신 이력` — runbook 하단의 최신 갱신일·갱신 사유
- `장애 후 회고` — 실제 장애 발생 시 runbook의 실효성 판정 (사용된 절차 · 부족한 부분)
- `실측 baseline` — 정상 상태의 참조값 (P95 latency · 5xx rate 등)

### Edges
- 장애 발생 → `runbook 인덱스` → 해당 `runbook 파일`
- `runbook 파일` → `장애 시나리오` × N (최소 5+ 시나리오)
- 각 시나리오 → `판별 쿼리` → `임시 대응` → `근본 대응` : 3섹션 순서
- `임시 대응` 실행 → 서비스 복구 → `근본 대응` 별도 이슈로 등록
- `실측 로그 예시` → 각 시나리오 : 가상 예시 대신 실측 근거
- 실장애 종료 → `장애 후 회고` → runbook 갱신 (해당하는 시나리오 보강)
- `실측 baseline` → `판별 쿼리` : 정상 대비 이상 판정 기준
- runbook 갱신 → `갱신 이력` 하단 반영

### Boundaries
- **파일 위치 경계**: runbook은 코드 저장소 `docs/runbooks/{topic}.md`. 외부 wiki 병행 시 진실 소스 명시 (저장소 우선).
- **시나리오 최소 경계**: 각 runbook 최소 **5개 이상 시나리오** 커버. 3개 이하면 불완전으로 간주.
- **3섹션 경계**: 각 시나리오는 `판별 쿼리` · `임시 대응` · `근본 대응` 3섹션 모두 명시. 누락 금지.
- **실측 근거 경계**: 각 시나리오에 **실측 로그·메트릭 예시** 첨부. 가상 예시만 있는 시나리오 금지.
- **갱신 경계**: 최신 갱신일 명시. **6개월 이상 미갱신** 시 stale 판정 후 회고 필요.
- **회고 반영 경계**: 실장애 발생 시 사용된 runbook을 회고 (실효성 판정 · 부족한 부분 보강).
- **인덱스 경계**: 5개 이상 runbook 존재 시 `docs/runbooks/README.md` 인덱스 필수.

### Invariants
- 각 runbook에 최소 5개 시나리오 없는 사례 0건 (완성된 runbook 기준)
  - 감지법: runbook 파일 시나리오 개수 리뷰
- 각 시나리오가 판별/임시/근본 3섹션 중 하나라도 누락한 사례 0건
  - 감지법: 시나리오별 섹션 존재 확인
- 각 시나리오에 실측 로그 예시가 없는 사례 0건 (가상 예시만)
  - 감지법: 로그 sample · 스크린샷 · grep 결과 첨부 여부 리뷰
- runbook이 코드 저장소가 아닌 곳에만 존재한 사례 0건
  - 감지법: `docs/runbooks/` 폴더 존재 확인
- 5개 이상 runbook 존재 시 인덱스 파일 없는 사례 0건
  - 감지법: `docs/runbooks/README.md` 존재 확인
- 6개월 이상 미갱신 runbook 방치 사례 0건
  - 감지법: 각 runbook 갱신일 vs 오늘 diff · 회고 이력 확인
- 실장애 발생 후 runbook 회고 없는 사례 0건 (v1 릴리스 이후)
  - 감지법: 장애 이력 vs runbook 갱신 이력 매칭

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 시나리오·판별 쿼리·대응 명령어 → `docs/runbooks/{topic}.md` 본문
- 실제 실측 로그·메트릭 스크린샷 → runbook 첨부·image
- 실제 임시 대응 명령어 (aws cli·gcloud·kubectl 등) → runbook 코드 블록
- 실측 baseline 값 (P95 latency 등) → `workflow/task/milestones/version/{Nv}/performance.md`
- runbook 인덱스 형식 → `docs/runbooks/README.md`
- 관측 지표·알람 원칙 → `observability` topology
- 장애 회고 형식 → 향후 `incident-postmortem` topology 검토 (LOW 우선순위)
- 왜 이렇게 박혔는지 → 프로덕션 운영 시점 (M8+) 회고

---

## 4. Re-pin trigger

- runbook 위치를 코드 저장소 밖으로 이관 (Notion·Confluence 단독 등)
- 3섹션 (판별/임시/근본) 구조 변경 (다른 프레임워크 도입)
- 실측 근거 필수성 폐기 (가상 예시 허용)
- 시나리오 최소 개수 하향 (5개 → 3개 등)
- 자동 runbook 생성 도입 (LLM 기반 · 실측 관찰 자동 요약)
- Chaos Engineering 도입 (예방적 시나리오 확보 방식 전환)
- 6개월 미갱신 판정 기준 변경 (12개월 등)
