# ADR005: Controller ↔ Service 경계에 Command/Query record 도입

- **상태**: Accepted
- **날짜**: 2026-05-12
- **관련**: `.claude/rules/api-conventions.md`, `.claude/rules/domain-conventions.md` §7 (책임 분리)

## 컨텍스트

LearningFacadeController(21 엔드포인트)와 Service 사이의 인자 전달이 두 가지 비대칭으로 진행되고 있었다.

1. **presentation 레이어 DTO가 application으로 누수**
   - `LearningFacadeCommandService.updateTopic(UserEntity user, Long axisId, Long topicId, LearningFacadeRequest.UpdateTopic command)` 가 presentation의 Request 객체를 그대로 받는다.
   - Service 단위 테스트가 presentation 패키지를 import해야 하며, Request DTO의 Jackson 직렬화 결합도가 application까지 따라온다.

2. **Service 메서드 시그니처가 path/auth/body 값을 펼친 긴 파라미터 나열**
   - `createMaterial(Long userId, String name, String materialType, String url, String author, String platform, String aiProvider, String webSource, String memo, List<Long> linkedTopicIds)` — 10개 인자.
   - 필드 추가/순서 변경마다 Service·Controller·테스트·Mock의 호출부가 모두 수정 대상이 된다.

이 비대칭은 LearningFacade BC에 한정된 문제가 아니다. 다른 BC(Card, Deck, Review, User, UserSchedule, …)도 동일한 패턴이 자라고 있어 같은 규칙을 본 BC에서 먼저 표준화해 후속 BC가 따르도록 한다.

## 결정

`{BC}.application.dto` 패키지에 BC별 Command/Query record 묶음을 정의하고, 모든 Service public 메서드는 그 record **1개**만 인자로 받는다.

- Command record: 변경 의도 (`Create…`, `Update…`, `Add…`, `Remove…`, `Reorder…` 등)
- Query record: 조회 의도 (`Get…`)
- record는 nested로 묶어 BC별로 4개 클래스로 응집: `LearningFacadeCommand`, `LearningFacadeQuery`, `LearningMaterialCommand`, `LearningMaterialQuery`.
- 모든 path variable / auth 값(`userId`, `axisId`, `topicId`, `materialId`)을 record 필드로 포함한다.
- presentation 레이어의 Request DTO는 Controller 안에서만 다룬다. Controller가 Request → Command/Query를 명시적으로 변환해 Service에 전달.
- 예외: `LearningFacadeCommand.CreateFacade`만 `Long userId` 대신 `UserEntity user` 필드를 보유 — `LearningFacade.create(UserEntity, String)`가 FK 엔티티를 직접 요구하기 때문이며, `UserRepository` 신규 주입은 본 PR scope 밖.

## 결과 (Consequences)

**긍정적**

- presentation.dto.Request가 application 레이어로 새지 않는다 (게이트: `grep "presentation.dto" application/dto/` → 0건 보장).
- Service 메서드 시그니처가 항상 `(Command)` / `(Query)` 1인자로 균일 — 테스트·Mock 작성 시 입력 구성이 한 곳(record)에 집중된다.
- 향후 필드 추가/삭제 시 **record만 수정**하면 되어 시그니처 변경 비용이 시스템적으로 줄어든다.
- BC별 record를 한 클래스에 nested로 묶어 IDE 자동완성 시 그 BC의 모든 동작 의도를 한눈에 본다.
- `@AuthenticationPrincipal UserEntity user` → `user.getId()` 추출이 Controller에서 명시적으로 일어나 인증 경계가 또렷해진다.

**트레이드오프 / 부정적**

- 시그니처 변경 **1회 비용**: Service·Controller·테스트의 모든 호출부를 동시에 수정해야 한다 (본 PR이 그 비용).
- Controller에 `new ...Command.XxxYyy(userId, axisId, request.field1(), ...)` 명시적 매핑이 늘어 한 줄에 처리되던 호출이 4-5줄로 늘어난다 — 가독성에서 양면성. 명시성을 얻는 대가.
- `LearningFacadeCommand.CreateFacade`만 `UserEntity` 필드 — 일관성 깨짐. `UserRepository` 주입으로 해소 가능하나 본 PR scope 밖.
- record 정의 파일 4개 + 21개 nested record가 새로 생긴다 — 작은 record 객체의 양이 늘어난다.

## 대안 비교

| 대안 | 장점 | 거부 사유 |
| --- | --- | --- |
| 현재 유지 (long parameter list + Request DTO 직접 전달) | 변경 없음 | DTO 누수가 시간이 갈수록 더 깊이 박힘. BC 추가 시 매번 같은 비용 반복 |
| presentation.dto.Request를 그대로 application으로 전달하되 도메인 record로 감싸지 않음 | Controller 매핑 코드 짧아짐 | 결합도 그대로. Jackson · Bean Validation 어노테이션이 application까지 따라옴 |
| Command/Query를 BC별 클래스로 묶지 않고 record 1개씩 별도 파일 | 파일별 독립 | 파일 폭발(21개+). 같은 BC의 의도 묶음이 시야에서 흩어짐 |
| Command/Query 도입 + Result 객체로 응답도 분리 (한 번에) | 응답 누수까지 정리 | 본 PR scope가 커져 검토 부담. 응답 분리는 별도 PR로 |

## 다시 검토할 시점

- BC가 5개 이상 본 패턴을 따르게 된 시점 — 공통 추상 `Command`/`Query` 마커 인터페이스 도입 검토.
- `UserEntity` 예외 (CreateFacade) — `UserRepository`를 LearningFacade BC에 주입해 `Long userId`로 통일하는 결정 검토.
- Service 반환 타입(`*Response`/`*Breakdown`)이 presentation에 있는 현 구조의 응답 누수도 별도 ADR로 정리할 시점 — 본 PR의 후속 PR에서 `Result` 객체로 분리 예정.
