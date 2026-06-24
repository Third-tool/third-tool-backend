# docs/release — 릴리즈 결과 기록

## 원칙

`workflow/product/milestones/version` 의 버전 명세를 그대로 가져온다.  
버전 릴리즈가 확정되고 commit이 완료된 시점에 결과값을 이 폴더에 올린다.

---

## 파일 명명

```
v{major}.{minor}.{patch}.md
```

예: `v1.0.0.md`, `v1.1.0.md`, `v2.0.0.md`

---

## 파일 내용 — 릴리즈 결과 템플릿

```markdown
# v{X.Y.Z} — {릴리즈 제목}

**날짜**: YYYY-MM-DD  
**출처**: workflow/product/milestones/{파일명}

## 포함된 기능 (완료된 Story)
- story-NNN: {설명}

## 변경 사항 요약
{milestone 명세의 목표·결과를 그대로 옮긴다}

## 관련 PR
- #{PR번호} — {제목}

## 메모 (선택)
{릴리즈 과정에서 있었던 주요 결정·트레이드오프·후속 과제}
```

---

## 언제 올리나

1. 해당 버전의 모든 Story PR이 develop에 머지됨
2. `main` 태그 또는 배포 완료가 확인됨
3. 위 두 조건이 충족된 commit 시점에 이 폴더에 결과 파일을 추가한다

---

## 이 폴더에 두지 않는 것

- 진행 중인 milestone 명세 → `workflow/product/milestones/`에서 관리
- 배포 인프라 결정 → `deployment/adr/`
- 장애·운영 이슈 → `operations/troubleshooting/`
