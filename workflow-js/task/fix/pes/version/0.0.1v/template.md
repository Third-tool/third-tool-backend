# [Fix] {수정 요지} ({YYYY-MM-DD})

> 본 문서는 `fix/pes` 빈 스켈레톤. 양식 가이드는 `./pes.md` 참조. 작성 시점에 본 인용 박스 제거.

## 대상 (원본)
- 원본 Product: {파일 경로}
- 영향 받는 Epic / Story: {Epic N / Story N-M, N-K}
- 본 fix 발견 시점: {YYYY-MM-DD}

## 트리거
{1~3 문장 — 명세 표류 출처·인터랙션 결과·외부 입력}

## 변경 내역

### Story별 명세 변경 이력

#### Story N-M: {원본 제목}
- **변경 전 AC**: Given ... / When ... / Then ...
- **변경 후 AC**: Given ... / When ... / Then ... *(엣지 — 사유)*
- **DoD 추가/변경**: {체크 항목}
- **사유**: {왜}

#### Story N-K: {원본 제목}
- ...

### ErrorCode / DTO 변경 (해당 시)
- 신규 ErrorCode: `{CODE}` ({HTTP}, "{message}") — {사유}
- DTO 필드 변경: {DTO명}.{field} {추가/제거/타입변경}

## 검증
- 단위: `./gradlew test --tests "..."` 그린
- 슬라이스: {결과}
- 통합: {결과}

## 영향
- 원본 명세 (`{경로}`) — [명세 변경 이력] 블록에 본 fix 식별자 기재
- DOMAIN.md / PACKAGE.md 갱신 — {섹션 명}
- 다른 BC: {경로 / N/A}
- ADR 트리거: {ADR NNN 후보 / N/A}
