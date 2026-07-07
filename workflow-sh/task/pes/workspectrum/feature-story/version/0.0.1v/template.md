# [Story] {이름 — 동사로 시작}

> 본 문서는 `feature-story` 빈 스켈레톤. 양식 가이드는 `./feature-story.md` 참조. 작성 시점에 본 인용 박스 제거.

## 사용자 가치
- As a {역할}
- I want {원하는 행위}
- so that {얻는 가치}

## 설명
{도메인 메서드 시그니처·정적 팩토리·불변식·예외 패턴(ErrorCode)·포트명을 한 단락으로}

## 인수 조건
- Given {전제} / When {행위} / Then {결과}
- Given ... / When ... / Then ...
- *(엣지 케이스 — 사유)* Given ... / When ... / Then ...

## Definition of Done
- [ ] 구현 (클래스명·메서드명)
- [ ] 단위 테스트 — N건, 해피/엣지/예외 매트릭스
- [ ] 슬라이스 테스트 (해당 시)
- [ ] 통합 테스트 (해당 시)
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
