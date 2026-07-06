# Issue: 기존 0.0.2v 이슈(#1~#3)와 신 계층 정합 검증

## 배경
0.0.2v에 이미 존재하던 3개 이슈는 신 계층(concepts[] + Layer + Roadmap/Selection + Card→axis 직접)이 확정되면 관점과 관계가 바뀐다:

- **#1 Deck×Axis 통합** — Option A로 "axis=deck" 확정된 상태. 신 모델에서 axis가 layer 하위이므로 관계 재해석 필요.
- **#2 Card 축 인식 실패** — 이미 #1에 흡수됨. Card 매핑 자체가 [#7](./issue-07-card-axis-direct-mapping.md)에서 axis 직접으로 바뀌므로 #2의 원인 해석이 바뀔 가능성.
- **#3 maxDuration 모드 UI** — 카드 관측 UI. Card 매핑 계층 변경([#7](./issue-07-card-axis-direct-mapping.md))의 영향.

## 조사 결과 — 관계 매핑

| 기존 이슈 | 신 계층 영향 |
|---|---|
| #1 deck-axis 통합 | axis가 layer 하위이므로 deck.axis_id는 유지되지만 layer 단위 조회·집계 요구가 새로 생김 (예: layer 진행률) |
| #2 card 축 인식 | topic 계층 폐기 + Card→axis 직접([#7](./issue-07-card-axis-direct-mapping.md))으로 원인 자체가 사라짐. #1 흡수 유지 or 완전 폐기 |
| #3 maxDuration UI | Card 매핑 axis 직접 전환으로 카드 관측 대상 계층이 명확해짐. UI 스토리 재작성 여지 |
| #5 Layer 서버 도메인 | deck.axis_id는 layer_id를 얻기 위해 axis→layer 조인 필요 |

## 옵션 비교

### 마이그레이션 순서 (신 계층 vs 기존 이슈)

**Option A — 기존 이슈 #1·#3 먼저 소화, 그 후 신 계층 도입 (채택)**
- 이유:
  - #1 (`fix/axis-soft-delete-cascade`, `fix/deck-single-create-path`)은 이미 브랜치가 진행 중(현재 `fix/deck-single-create-path`).
  - 신 계층은 규모가 크고 세션 UI가 붙어야 하므로 별도 큰 마일스톤.
  - 기존 이슈 hotfix 성격 → 짧게 소화 후 신 계층 대규모 리팩토링 시작.
- 리스크: #1의 "axis=deck"이 신 모델에서 "layer=(axis=deck 집합)"으로 재해석되지만 데이터 관계 자체는 유지되므로 이중 마이그레이션 아님.

**Option B — 신 계층 먼저 도입, 기존 이슈는 신 계층 위에서 재정의**
- 이유: 기존 이슈의 근본 원인이 신 계층에서 자연 해소될 수 있음 (예: #2 card 축 인식).
- 리스크: 신 계층 도입 완료까지 기존 이슈의 사용자 관측 문제 방치.

**Option C — 병렬**
- 브랜치 충돌·리뷰 부담. 반대.

## 선택: Option A

## 부속 결정

### 기존 이슈별 최종 처리 결정

**#1 deck-axis 통합**
- 현재 진행 그대로 완주. 브랜치 (`fix/deck-single-create-path`) 이후 신 계층 도입 시 axis의 layer FK 재지향 마이그레이션에 포함 (별도 story 아님, [#5-2](./issue-05-layer-server-domain.md)의 일부).

**#2 card 축 인식 실패**
- #1에 흡수된 상태 유지. **[#7](./issue-07-card-axis-direct-mapping.md)에서 완전 해소** 명시. 별도 story 미승격.

**#3 maxDuration 모드 UI**
- FE 단독 소화 가능 (BE 신 계층 대기 불필요). 원래 계획대로 FE-Story 2개 (SDD 신작 + Settings 화면) 진행.
- 다만 카드 매핑이 axis 직접으로 바뀌면 UI 표현 어휘("이 카드의 topic" → "이 카드의 axis")가 함께 변경 필요. [#7](./issue-07-card-axis-direct-mapping.md) 완료 후 최종 문구 리비전.

### PR/브랜치 전략
1. `fix/deck-single-create-path` (진행 중) → 머지.
2. #1 후속 (Story 1: soft delete cascade) → 머지.
3. #3 FE-Story 2개 → 머지.
4. **여기서부터 신 계층 대규모 리팩토링 시작**:
   - #4 concepts[] → #5 Layer → #6 Roadmap/Selection → **{#7 Card FK + #13 Deck 폐기} 클러스터** → #14 Review 재설계 순서.
   - 각 이슈는 별도 브랜치 (`feature/006-concept-list`, `feature/007-layer-server`, `feature/008-roadmap-selection-dualaxis`, `feature/009-card-axis-and-deck-abolition`, `feature/010-review-axis-layer-scope`).
   - **#7과 #13은 동일 마이그레이션 클러스터**: `card.axis_id` 신설과 `card.deck_id` 제거가 같은 Flyway 배치에 들어가야 정합. 브랜치·PR도 통합.
   - #8 SDD 개정은 각 이슈 PR과 함께 부분 반영, 최종 통합 개정 PR은 신 계층 완료 후.
   - #9 AI 3층·#10 role 카탈로그는 도메인 완료 후.
   - #11 selections 정책은 #6·#9 진행 중 함께 정의.

### 순서 검증 지점
- #4·#5·#6은 서로 독립적으로 도메인 골격 구성 가능 (마이그레이션은 각자 별도 V파일).
- #7·#13은 반드시 #6 뒤 (AxisTopic 폐기 확정 후 Card→axis + Deck 폐기).
- #14는 반드시 #7·#13 뒤 (Deck 폐기 후 Review 참조 계층 이동).
- #9는 #6까지 완료 후 (roadmap 원문이 SelectionsSuggestionPort 컨텍스트 필수).

## 이관 산출물

이 이슈는 코드 산출물이 없고 **정합 검증·순서 결정** 산출물만.
- **Decision-Doc #12-1**: 위 PR/브랜치 전략을 workflow 상단 앵커 문서(예: `workflow/task/fix/README.md` 또는 별도)에 명시. 실제 문서화는 [#8](./issue-08-terminology-redefinition-sdd.md)의 Docs-Story에 편입.
- **Decision-Doc #12-2**: #2 이슈 파일에 "완전 해소는 #7 대상" 메모 추가 (개정 대상: `issue-card-axis-recognition.md`).

## 관련 이슈 / 문서

- 이전 이슈: `issue-deck-axis-integration.md`, `issue-card-axis-recognition.md`, `issue-maxduration-mode-ui.md`.
- 신 계층: [#4](./issue-04-concept-list.md) ~ [#11](./issue-11-selections-version-policy.md).
- 브랜치 참고: `.claude/rules/workflow.md` §9 (Story 단위 PR), `.claude/rules/git.md` (브랜치 명명).
