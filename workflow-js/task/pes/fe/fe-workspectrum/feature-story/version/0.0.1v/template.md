# [Story] {이름 — 동사로 시작}

> 본 문서는 FE `feature-story` 빈 스켈레톤. 양식 가이드는 `./feature-story.md` 참조. 작성 시점에 본 인용 박스 제거.

## 사용자 가치
- As a {역할}
- I want {원하는 행위}
- so that {얻는 가치}

## 설명
{컴포넌트·hook·endpoint 함수 시그니처 / Zod 스키마 필드 / ApiError code 처리 / 라우트 경로를 한 단락으로}

## 인수 조건
- Given {전제} / When {행위} / Then {결과}
- Given ... / When ... / Then ...
- *(엣지 케이스 — 사유)* Given ... / When ... / Then ...

## Definition of Done
- [ ] 구현 (컴포넌트·hook·파일명)
- [ ] 단위 테스트 — N건, 해피/엣지/예외 매트릭스 (Vitest + Testing Library)
- [ ] MSW handler (외부 API 호출 시)
- [ ] 시각 회귀 / 수동 브라우저 확인 (해당 시)
- [ ] ADR 작성 (해당 시)

## 비범위
- {범위 밖 + 사유}

## INVEST
- Independent: {1줄}
- Negotiable: {1줄}
- Valuable: {1줄}
- Estimable: {1줄}
- Small: {1줄}
- Testable: {1줄}
