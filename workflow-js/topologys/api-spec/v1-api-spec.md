# Pinned Topology — `api-spec` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 URL · 실제 엔드포인트 · 실제 DTO 필드명은 여기 없다 — 그건 코드의 몫)
>
> 이 구간 동안 graph(노드·엣지·경계·불변식)는 **변하지 않는 제약**이다.

---

## 0. 유효 구간 (Validity)

| 항목 | 값 |
| --- | --- |
| Pinned at | 2026-06-15 |
| Valid for | 본 워크플로우 v1 구간 — re-pin trigger 발생 전까지 |
| Owner | 메인 Claude + 사용자 합의 |
| Re-pin trigger | 아래 §4 |

---

## 1. How Claude Code must use this file — 행동 계약

1. **API 작업 시작 시 이 파일을 먼저 읽는다.** §2의 graph는 본 구간 동안 **고정 제약**.
2. **이 topology 안에서 실제 vocabulary(URL·필드명·메서드 시그니처)만 생성한다.** 노드·엣지·경계는 건드리지 않는다.
3. **메서드 매핑 / 에러 envelope 형태 / 버전 경계를 바꿔야 할 것 같으면 STOP하고 보고한다.** PR 안에 슬쩍 끼우지 않는다.
4. **코드가 envelope 형태를 우회하는 정황(Controller에서 응답 가공)을 발견하면 임의로 맞추지 않고 보고**한다.
5. **이 파일에 실제 코드·필드를 추가하지 않는다.** 그건 §3 포인터를 따라 코드로 간다.

---

## 2. The pinned topology

### Nodes
- `Resource` — REST 리소스 단위. 명사 중심으로 식별됨
- `URL 버전 envelope` — 모든 리소스를 감싸는 버전 prefix 단위
- `HTTP 행위` — 생성·조회·부분수정·전체교체·삭제의 5종 행위
- `Request DTO` — 클라이언트 입력 검증의 1차 경계
- `Response DTO` — 응답 직렬화의 단위
- `Error envelope` — 사용자 노출 오류의 단일 형태(코드 + 메시지 쌍)
- `ErrorCode 레지스트리` — 등록된 비즈니스 오류의 단일 진실 소스
- `전역 예외 변환점` — 도메인 예외 → Error envelope의 단일 진입점

### Edges
- `Resource` ↪ `URL 버전 envelope` : 버전 prefix 안에서만 노출
- `Request DTO` → `Resource` : 1:1, 검증 통과 후 진입
- `Resource` → `Response DTO` : 1:1, 직렬화는 응답 시점
- `Resource` ↯ `Error envelope` : 예외 흐름은 `전역 예외 변환점`을 경유해서만
- `ErrorCode 레지스트리` → `Error envelope` : code 필드의 단일 출처
- 부모 리소스 → 자식 리소스 : path 중첩으로만 표현(계층 표현)
- 검색·필터 : query parameter로 표현 (path가 아님)

### Boundaries
- **버전 경계**: URL prefix로 버전 고정. 동일 버전 내에서는 **비파괴 변경만** 허용 (필드 추가 O, 필드 제거·이름 변경 X)
- **검증 경계**: Bean Validation은 Request DTO에서. 도메인 규칙은 그 아래 레이어
- **부분 vs 전체 경계**: 부분 수정과 전체 교체는 **서로 다른 HTTP 메서드**를 사용. 한 메서드로 두 의미를 섞지 않는다
- **에러 변환 경계**: 도메인/애플리케이션 예외 → HTTP 응답 변환은 `전역 예외 변환점` 단 한 곳
- **find-or-create 경계**: 시스템 전역 유니크 리소스는 클라이언트가 식별자가 아닌 값으로 전달, 서버가 재사용/생성 결정

### Invariants
- 사용자 노출 오류는 100% `Error envelope` 형태(code + message)로 표출된다
  - 감지법: Controller 코드에 try-catch · ResponseEntity 가공 0건 (리뷰 + grep)
- 모든 throw되는 비즈니스 오류는 `ErrorCode 레지스트리`에 등록된 코드를 가진다
  - 감지법: 새 오류 도입 PR은 레지스트리 변경을 동반 (리뷰 체크리스트)
- 부분 수정과 전체 교체는 같은 endpoint를 공유하지 않는다
  - 감지법: API 변경 시 메서드 매핑 표 점검
- 버전 prefix가 박힌 응답에서 기존 필드가 제거되지 않는다
  - 감지법: 컨트롤러 slice 테스트 + Response DTO diff

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 URL · 실제 path · 실제 query parameter → Controller 코드 + Swagger UI(`/swagger-ui.html`)
- 실제 Request/Response DTO 필드 → `application/dto/` · `presentation/` 의 DTO 클래스
- 실제 ErrorCode 식별자 · 메시지 → `Common/Exception/ErrorCode/` 레지스트리
- 실제 `전역 예외 변환점` 구현 → `Common/Exception/GlobalExceptionHandler`
- URL · 메서드 매핑 · DTO 패턴의 상세 규칙 → `.claude/rules/conventions.md` §2 (API 컨벤션)
- 왜 이렇게 박혔는지 → 향후 ADR 작성 시 `docs/adr/`

---

## 4. Re-pin trigger

다음 중 하나라도 발생하면 **작업을 멈추고 재고정**한다:

- API 패러다임 변경(REST → GraphQL · RPC 등)
- 버전 경계 운영 방식 변경(URL prefix 외 다른 수단 도입)
- `Error envelope` 형태 변경 (필드 추가/이름 변경 필요)
- `전역 예외 변환점`을 우회하는 새 흐름 도입 필요 (예: 스트리밍·SSE)
- 부분/전체 교체 메서드 매핑 원칙 변경
- find-or-create가 아닌 새 형태의 리소스 생명주기 패턴이 필요해진다
