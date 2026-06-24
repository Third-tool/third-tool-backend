# ts001: ADR 번호가 평행 브랜치에서 중복 점유되어 10개 PR이 conflict 상태가 된 사건

**영역**: workflow | **심각도**: Medium | **발견**: self (PR 머지 시도 시점) | **복구 시간**: 약 3시간

> **면접 포인트**
> "Git 작업 중 예상치 못한 충돌을 해결한 경험이 있나요? git rebase --onto를 써본 적 있나요?"
> → 두 개의 평행 브랜치가 같은 ADR 번호를 점유한 채 각자 진행되다가, 하나가 develop에 먼저 머지되면서 나머지 브랜치와 그 위에 stacked된 PR 9개가 동시에 conflict 상태가 됐다. git rebase --onto로 cascade를 해소했다.

---

## 증상 (Symptom)

- `feat/009-cookie-auth` 브랜치(Cookie 인증 ADR008 작성)를 develop에 머지하려 했을 때, PR과 그 위에 stacked된 PR 9개 **전부**가 `CONFLICTING` 상태
- 충돌 파일: `docs/adr/ADR008.md`
- 영향 범위: 현재 작업 중인 스토리 9개 전체 진행 불가

---

## 가설과 검증 (시간 순)

**1차 가설: develop의 ADR007이 마지막이므로 Cookie를 ADR008로 유지해도 된다**
- 확인: `git log develop --oneline -- docs/adr/` 실행
- 결과: develop에는 이미 `docs/adr/ADR008.md`가 존재 (Logging ADR)
- → **오답**. 발견 시점의 develop 상태를 잘못 가정했다.

**2차 가설: feat/009 브랜치가 분기된 후 develop에 feat/006(Logging ADR008)이 먼저 머지됐다**
- 확인: `git log develop --oneline --graph` + `gh pr list --state merged`
- 결과: `feat/006-deck-progress-status`가 `feat/009` 작업 도중에 develop에 머지됨. 이 브랜치가 `ADR008.md`(로깅 결정)를 포함했다.
- → **정답**. 두 브랜치가 독립적으로 ADR008 번호를 선택한 것이 충돌의 원인.

**3차 확인: 후속 9개 PR에 cascade rebase가 필요한지**
- 확인: `gh pr list --head feat/009` 및 상위 stacked PR 목록 확인
- 결과: `feat/009` 위에 9개의 stacked PR이 있고, 각각 `feat/009`를 base로 한다.
- → feat/009를 rebase하면 9개 PR 전부도 rebase가 필요하다.

---

## 근본 원인 (Root Cause)

**ADR 번호를 "브랜치 생성 시점에 confirm"하는 팀 컨벤션이 없었다.**

- `feat/009`가 분기된 시점: develop의 최신 ADR은 ADR007 → ADR008 선택이 논리적으로 맞음
- `feat/006`이 develop에 먼저 머지됨: 이 브랜치도 ADR008을 작성해 포함
- 결과: develop에는 Logging ADR008이 자리잡고, feat/009는 여전히 Cookie를 ADR008로 작성 중

평행 작업이 많을수록 이 충돌 패턴은 반복될 수 있다. 번호 예약이나 확정 시점 규약 없이는 구조적으로 발생한다.

---

## 해결 (Fix)

### Step 1: Cookie ADR를 ADR009로 재번호
```bash
git mv docs/adr/ADR008.md docs/adr/ADR009.md
# 파일 내부 title, refs도 ADR009로 수정
git commit -m "docs(adr): Cookie AT/RT 결정 ADR009로 재번호 (ADR008 충돌 해소)"
```

### Step 2: feat/009를 최신 develop 위로 rebase
```bash
git fetch origin
git rebase origin/develop
# ADR008.md 충돌 발생 → Logging ADR008 유지, Cookie는 ADR009.md로 해소
git rebase --continue
```

### Step 3: 상위 stacked PR 9개 순서대로 --onto rebase

각 stacked 브랜치를 이전 브랜치 위로 재이동:
```bash
git rebase --onto feat/009 <old-base-sha> feat/010
git rebase --onto feat/010 <old-base-sha> feat/011
# ... 9번 반복
```

`git rebase --onto <newbase> <upstream> <branch>` 패턴:
- `newbase`: 새롭게 붙일 위치
- `upstream`: 기존 base (커밋 SHA 또는 브랜치명)
- `branch`: 이동할 브랜치

### Step 4: 각 PR의 base 브랜치 갱신
```bash
gh pr edit <number> --base feat/009   # 각 PR마다 실행
```

---

## 재발 방지 (Prevention)

**팀 컨벤션에 명시**: "ADR 번호는 develop 머지 시점에 최종 확정한다. 브랜치 작업 중에는 임시 번호를 사용하거나, develop HEAD의 마지막 ADR 번호를 merge 직전에 확인한다."

실용적 체크리스트:
- [ ] PR 생성 전: `git log origin/develop --oneline -- docs/adr/`로 develop 현재 마지막 ADR 번호 확인
- [ ] 동기간에 ADR 작성 중인 다른 브랜치 있는지 팀에 공유

---

## 배운 점

1. **git rebase --onto의 실제 용도**: 브랜치의 "시작점"을 다른 곳으로 이식할 때 사용한다. 단순 `rebase`가 최신화라면, `--onto`는 이식이다.
2. **stacked PR의 cascade 비용**: 브랜치를 stacked으로 쌓으면 하나가 깨질 때 전부 cascade로 영향을 받는다. develop에 자주 머지해 base를 짧게 유지하는 것이 더 저렴하다.
3. **ADR 번호 = 암묵적 전역 자원**: 명시적 예약이나 확정 시점 규약 없이는 충돌이 구조적으로 발생한다.
