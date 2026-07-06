# Issue: 새 Card 생성 시 축 인식 실패 · 화면 이탈 시 축 사라짐

## 배경
사용자 리포트:
> "새로운 Card를 만들 때 학습 축이 인식이 안됩니다. 지도에서 축이 만들어지고도 이유는 모르겠는데,
> 바로 지워집니다. data가 남아있지 않은지 화면을 나가면 바로 삭제되어 새카드 만들 때 덱 인식이 안되는 것 같습니다."

## 조사 결과 — 원인 후보 5

| # | 후보 | 근거 | 심각도 |
|---|---|---|---|
| 1 | `LearningFacade.removeAxis()` orphanRemoval hard delete | `LearningFacade.java:96-99` axes.remove → JPA orphanRemoval + FK ON DELETE CASCADE | **HIGH** |
| 2 | 프론트가 임시 Deck 만들고 이탈 시 DELETE 호출 → soft delete | `Deck.softDelete()` deleted=true, 조회 시 필터 | MEDIUM |
| 3 | `addAxis` save 후 이벤트 발행. 이벤트 핸들러 예외 시 축까지 롤백 | `LearningFacadeCommandService.java:69-85` @Transactional 경계 미확인 | MEDIUM |
| 4 | LearningAxis에 deleted_at 없음 (Hard Delete만) | 스키마 V2 — 정책 이슈 | 정책 |
| 5 | `@EntityGraph` axes eager, topics lazy — 조회 시 missing 가능성 | `LearningFacadeJpaRepository.java:17-19` | LOW |

## 결정: 이슈 1(Deck×Axis 통합) 작업에 흡수

근거:
- 후보 1(HIGH) — 이슈 1의 D1-e(LearningAxis Soft Delete 도입) 결정으로 **원천 봉쇄**
- 후보 2(MEDIUM) — 이슈 1의 D1-a(수동 Deck 생성 경로 제거) 결정으로 **원천 봉쇄** (사용자가 임시 Deck을 만들 진입점 자체가 없어짐)
- 후보 3·5 — 이슈 1 완료 후 로컬 재현 확인. 재현 시에만 후속 story 오픈

## 검증 계획 (이슈 1 완료 후)
1. 축 생성 → 화면 이탈 → 재진입 → 축 목록 GET 응답 확인
2. 축 삭제 → Deck 목록 응답에 해당 Axis Deck 미포함 확인
3. Card 생성 시 Deck.axisId non-null 회귀 확인
4. 재현 실패 → 이슈 종결. 재현 시 → 후보 3·5 재조사 (`@Transactional` 로그, EntityGraph fetch trace)

## 이관 산출물
- 별도 story 없음. 이슈 1의 BE-Story 1·2에 흡수.
