# [Fix] {수정 요지 — 동사로 시작} ({YYYY-MM-DD})

> 본 문서는 `fix/feature-story` 빈 스켈레톤. 양식 가이드는 `./feature-story.md` 참조. 작성 시점에 본 인용 박스 제거.

## 대상 (원본)
- 원본 PR / Story: {식별자 + 링크}
- 본 fix 발견 시점: {YYYY-MM-DD}
- 영향 받는 메서드 / 클래스: {경로 + 메서드명}

## 트리거
- {리뷰 코멘트 / 테스트 실패 / 클로드 인터랙션 / 명세 표류}

## 변경 내역

### 코드
- {파일:라인} {무엇} ({before → after 요지})

### AC 보강 (해당 시)
- *(추가/갱신)* Given ... / When ... / Then ...

## 검증
- 단위: {N건 추가/수정} — `./gradlew test --tests "..."` 그린
- (해당 시) 슬라이스 / 통합: {결과}

## 영향
- 원본 명세 갱신 필요? — {DOMAIN.md 섹션 / N/A}
- 원본 ADR — {N/A 또는 ADR NNN 보강 메모}
- 다른 Story / BC — {N/A}
