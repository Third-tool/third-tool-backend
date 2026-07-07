# [Product] {이름}

> 본 문서는 `pes` 빈 스켈레톤. 양식 가이드는 `./pes.md` 참조. 작성 시점에 본 인용 박스 제거.

## 성과 (Outcome)
**{한 문장 — 핵심 책임 볼드}**

## 성공 지표
- {정량 지표 1}
- {정량 지표 2}
- {정량 지표 3}

## 범위 (Scope)
- {포함 1}
- {포함 2}

## 비범위 (Out of Scope)
- {제외 1} — 사유
- {제외 2} — 사유

## Epic 목록
- [ ] Epic 1: {제목}
- [ ] Epic 2: {제목}

## 제품 완료 기준
- [ ] 모든 Epic 완료
- [ ] ADR {NNN} 작성
- [ ] API 스펙 (Swagger) 갱신
- [ ] DOMAIN.md / PACKAGE.md 반영

---

# [Epic 1] {이름}

## 목표
{한 문장 — Epic 끝나면 무엇이 가능}

## 포함 Story
- [ ] Story 1-1: {제목}
- [ ] Story 1-2: {제목}

## Epic 인수 시나리오
{X → Y → Z 흐름}

## Epic 완료 기준
- [ ] 포함 Story 완료
- [ ] 통합 테스트 통과
- [ ] ADR 작성 (해당 시)

---

## [Story 1-1] {이름}

### 사용자 가치
- As a {역할}
- I want {원하는 행위}
- so that {얻는 가치}

### 설명
{메서드 시그니처·불변식·ErrorCode·포트명 한 단락}

### 인수 조건
- Given ... / When ... / Then ...
- Given ... / When ... / Then ...
- *(엣지 케이스 — 사유)* Given ... / When ... / Then ...

### Definition of Done
- [ ] 구현
- [ ] 단위 테스트 ({N건})
- [ ] 슬라이스 테스트 (해당 시)
- [ ] 통합 테스트 (해당 시)
- [ ] ADR (해당 시)

### 비범위
- {범위 밖 + 사유}

### INVEST
- Independent: {1줄}
- Negotiable: {1줄}
- Valuable: {1줄}
- Estimable: {1줄}
- Small: {1줄}
- Testable: {1줄}
