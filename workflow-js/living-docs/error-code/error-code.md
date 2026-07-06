# ErrorCode Catalog (Living)

> **성격**: living-docs — 항상 최신본. `Common/Exception/ErrorCode/ErrorCode` enum이 진실 소스, 본 문서는 팀 참조용 카탈로그.
> **성장 방향**: 코드가 늘면 BC별 파일로 분화 (`error-code/card.md`, `error-code/deck.md` …).
> **관련 topology**: `workflow/topologys/versions/{Nv}/common-core.md` — 등록 규칙·네이밍 컨벤션은 topology.
> **변경 절차**: 새 코드 등록 시 (1) enum 등록 → (2) 도메인/서비스에서 `{Bc}DomainException.of(코드)` throw → (3) 본 문서에 반영.

---

## HTTP 상태 매핑 원칙

| HTTP | 의미 | 대표 접미사 |
| --- | --- | --- |
| 400 BAD_REQUEST | 검증 실패 (blank / 범위 / 개수 / 잘못된 값) | `_BLANK`, `_MIN_REQUIRED`, `_OUT_OF_RANGE`, `_LIMIT_EXCEEDED`, `_INVALID` |
| 401 UNAUTHORIZED | 인증 실패 | `AUTH_*`, `REFRESH_TOKEN_*`, `PASSWORD_NOT_MATCHED` |
| 403 FORBIDDEN | 권한 부족 (본인 자산 아님) | `_FORBIDDEN` |
| 404 NOT_FOUND | 리소스 없음 | `_NOT_FOUND` |
| 409 CONFLICT | 상태 충돌 · 중복 | `_ALREADY_*`, `_DUPLICATE_*`, `_HAS_ACTIVE_*` |
| 413 PAYLOAD_TOO_LARGE | 업로드 크기 초과 | `PAYLOAD_TOO_LARGE` |
| 500 INTERNAL_SERVER_ERROR | 서버 내부 오류 | `INTERNAL_ERROR`, `_AUTO_CREATE_FAILED` |
| 502 BAD_GATEWAY | 외부 응답 형식 오류 | `_SUGGESTION_INVALID_RESPONSE` |
| 503 SERVICE_UNAVAILABLE | 외부 서비스 일시 실패 | `_SUGGESTION_TIMEOUT`, `FILE_UPLOAD_FAIL`, `FILE_DELETE_FAIL` |

응답 형식은 `GlobalExceptionHandler`가 일괄 `{code, message}` JSON으로 변환. Controller에서 try-catch 금지.

---

## 카탈로그 (BC별)

### 공통 (C···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| C001 | INVALID_INPUT | 잘못된 입력 값입니다. | 400 |
| C002 | NOT_FOUND | 데이터를 찾을 수 없습니다. | 404 |
| C500 | INTERNAL_ERROR | 서버 내부 오류가 발생했습니다. | 500 |

### User (USER···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| USER001 | USER_NOT_FOUND | 사용자를 찾을 수 없습니다. | 404 |
| USER002 | UNAUTHORIZED | 인증이 필요합니다. | 401 |
| USER003 | USER_ALREADY_EXISTS | 이미 가입된 사용자입니다. | 409 |
| USER004 | USER_LOCKED | 잠긴 계정입니다. | 403 |
| USER005 | PASSWORD_NOT_MATCHED | 사용자를 찾을 수 없습니다. *(보안상 USER001과 동일 메시지)* | 401 |
| USER006 | USER_IS_SOCIAL | 소셜 계정은 자체 로그인을 사용할 수 없습니다. | 400 |
| USER007 | SOCIAL_PROVIDER_NOT_SUPPORTED | 지원하지 않는 소셜 제공자입니다. | 400 |
| USER008 | SOCIAL_MEMBER_ALREADY_LINKED | 이미 연동된 소셜 계정입니다. | 409 |

### Auth · Refresh Token (AUTH···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| AUTH001 | AUTH_TOKEN_MISSING | 인증 토큰이 누락되었습니다. | 401 |
| AUTH002 | AUTH_TOKEN_EXPIRED | 인증 토큰이 만료되었습니다. | 401 |
| AUTH003 | AUTH_TOKEN_INVALID | 인증 토큰이 유효하지 않습니다 (위조/서명 불일치). | 401 |
| AUTH004 | AUTH_USER_NOT_FOUND | 토큰의 사용자가 더 이상 존재하지 않습니다. | 401 |
| AUTH005 | AUTH_FORBIDDEN | 접근 권한이 없습니다. | 403 |
| AUTH101 | REFRESH_TOKEN_INVALID | 유효하지 않은 Refresh Token입니다. | 401 |
| AUTH102 | REFRESH_TOKEN_NOT_FOUND | Refresh Token이 화이트리스트에 존재하지 않습니다. | 401 |
| AUTH103 | REFRESH_TOKEN_REUSED | 이미 사용된 Refresh Token이 재사용 시도되었습니다. | 401 |
| AUTH104 | REFRESH_TOKEN_MISSING | Refresh Token이 요청에 포함되지 않았습니다. | 400 |

### Deck (DECK···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| DECK001 | DECK_NOT_FOUND | 덱을 찾을 수 없습니다. | 404 |
| DECK002 | DECK_ALREADY_DELETED | 이미 삭제된 덱입니다. | 400 |
| DECK003 | DECK_NAME_DUPLICATE | 이미 존재하는 덱 이름입니다. | 409 |
| DECK004 | DECK_NAME_BLANK | 덱 이름은 비어 있을 수 없습니다. | 400 |
| DECK005 | DECK_FORBIDDEN | 본인의 덱이 아닙니다. | 403 |
| DECK006 | DECK_AUTO_CREATE_FAILED | Deck 자동 생성에 실패했습니다. | 500 |
| DECK006 *(중복 코드)* | DECK_HIERARCHY_CYCLE | 덱 계층 구조에 순환 참조가 발생했습니다. | 400 |

> **NOTE**: DECK006이 `DECK_AUTO_CREATE_FAILED`와 `DECK_HIERARCHY_CYCLE` 두 이름에 붙어 있음 — enum 코드 중복. **정리 필요** (별도 이슈로 관리).

### Card (CARD···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| CARD001 | CARD_NOT_FOUND | 카드를 찾을 수 없습니다. | 404 |
| CARD002 | CARD_IMAGE_NOT_FOUND | 카드 이미지를 찾을 수 없습니다. | 404 |
| CARD003 | CARD_RANK_EMPTY | 해당 랭크의 카드가 없습니다. | 404 |
| CARD004 | CARD_FEEDBACK_NOT_ALLOWED | 해당 카드에는 피드백을 줄 수 없습니다. | 409 |
| CARD005 | CARD_RESET_NOT_ALLOWED | 영구 모드 카드만 초기화할 수 있습니다. | 409 |
| CARD010 | CARD_MAIN_NOTE_EMPTY | MainNote는 텍스트 또는 이미지 중 최소 하나를 포함해야 합니다. | 400 |
| CARD020 | CARD_SUMMARY_EMPTY | Summary는 비어 있을 수 없습니다. | 400 |
| CARD021 | CARD_SUMMARY_SENTENCE_OUT_OF_RANGE | Summary는 1~3문장 범위여야 합니다. | 400 |
| CARD030 | CARD_KEYWORD_BLANK | 키워드 단서는 비어 있을 수 없습니다. | 400 |
| CARD031 | CARD_KEYWORD_MIN_REQUIRED | 카드는 키워드를 최소 1개 이상 가져야 합니다. | 400 |
| CARD032 | CARD_KEYWORD_NOT_FOUND | 해당 키워드 단서를 찾을 수 없습니다. | 404 |
| CARD033 | CARD_KEYWORD_LAST_CANNOT_REMOVE | 마지막 키워드는 제거할 수 없습니다. | 400 |
| CARD040 | CARD_AXIS_ID_REQUIRED | 카드 생성 시 축 id는 필수입니다. | 400 | *(M4 LT E4)* |
| CARD041 | CARD_TOPIC_ID_DEPRECATED | topic_id 필드는 곧 폐기됩니다. axisId를 사용하세요. | 400 (경고성) | *(M4 LT E4 · soft-deprecate 안내)* |

### Tag (TAG···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| TAG001 | TAG_VALUE_BLANK | 태그 이름은 비어 있을 수 없습니다. | 400 |
| TAG002 | CARD_TAG_LIMIT_EXCEEDED | 카드당 태그는 최대 3개까지만 연결할 수 있습니다. | 400 |
| TAG003 | CARD_TAG_NOT_FOUND | 해당 태그를 카드에서 찾을 수 없습니다. | 404 |
| TAG004 | CARD_TAG_ALREADY_EXISTS | 이미 연결된 태그입니다. | 409 |

### Review (REVIEW···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| REVIEW001 | REVIEW_SESSION_NOT_FOUND | 리뷰 세션을 찾을 수 없습니다. | 404 |
| REVIEW002 | REVIEW_DECK_HAS_NO_CARDS | 카드가 없는 덱으로는 리뷰 세션을 시작할 수 없습니다. | 400 |
| REVIEW003 | REVIEW_SESSION_ALREADY_FINISHED | 이미 종료된 리뷰 세션입니다. | 400 |
| REVIEW004 | REVIEW_COMPARING_REQUIRED | 현재 카드를 먼저 비교(COMPARING) 단계로 전환해야 다음으로 이동할 수 있습니다. | 400 |
| REVIEW005 | REVIEW_SESSION_FORBIDDEN | 본인의 리뷰 세션이 아닙니다. | 403 |

### LearningFacade (LF···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| LF001 | LEARNING_FACADE_NOT_FOUND | LearningFacade를 찾을 수 없습니다. | 404 |
| LF002 | LEARNING_FACADE_ALREADY_EXISTS | 이미 LearningFacade가 존재합니다. | 409 |
| LF003 | LEARNING_FACADE_CONCEPT_BLANK | 컨셉 값은 비어있을 수 없습니다. | 400 |
| LF004 | LEARNING_FACADE_FORBIDDEN | 본인의 LearningFacade가 아닙니다. | 403 |
| LF005 | LEARNING_FACADE_CONCEPTS_SIZE_INVALID | 컨셉은 1~5개 이내여야 합니다. | 400 |
| LF006 | LEARNING_FACADE_CONCEPT_DUPLICATE | 이미 등록된 컨셉입니다. | 409 |
| LF007 | LEARNING_FACADE_CONCEPT_TOO_LONG | 컨셉 값은 100자 이내여야 합니다. | 400 |
| LF008 | LEARNING_FACADE_CONCEPTS_REORDER_MISMATCH | 전달된 concept id 목록이 현재 컨셉 집합과 일치하지 않습니다. | 400 |

### LearningFacade Suggestion / AI (LF_SUGGEST···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| LF_SUGGEST_001 | LEARNING_FACADE_SUGGESTION_TIMEOUT | AI 제안 요청이 시간 내 완료되지 않았습니다. | 503 |
| LF_SUGGEST_002 | LEARNING_FACADE_SUGGESTION_INVALID_RESPONSE | AI 응답 형식이 유효하지 않습니다. | 502 |
| LF_SUGGEST_003 | LEARNING_FACADE_SUGGESTION_AUTH_FAILED | AI 인증 정보가 유효하지 않습니다. | 503 |

> **NOTE**: AI 호출 실패는 사용자에게 5xx로 노출되지 않음 — 상위 Service가 catch → 빈 목록 + `suggestionsAvailable=false`로 변환 (ADR010). HTTP 매핑은 fallback 미적용 시(운영 디버깅 우회)의 기본값.

### LearningLayer (LL···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| LL001 | LEARNING_LAYER_NOT_FOUND | Layer를 찾을 수 없습니다. | 404 |
| LL002 | LEARNING_LAYER_NAME_BLANK | Layer 이름은 비어 있을 수 없습니다. | 400 |
| LL003 | LEARNING_LAYER_DUPLICATE_NAME | 동일한 이름의 Layer가 이미 존재합니다. | 409 |
| LL004 | LEARNING_LAYER_ALREADY_DELETED | 이미 삭제된 Layer입니다. | 400 |
| LL005 | LEARNING_LAYER_HAS_ACTIVE_AXES | 활성 축이 있는 Layer는 삭제할 수 없습니다. | 409 |
| LL006 | LEARNING_LAYER_REORDER_MISMATCH | 전달된 Layer id 목록이 현재 Layer id 집합과 일치하지 않습니다. | 400 |
| LL007 | LEARNING_LAYER_NAME_TOO_LONG | Layer 이름은 100자 이내여야 합니다. | 400 |

### LearningAxis (LA···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| LA001 | LEARNING_AXIS_NOT_FOUND | 세부 축을 찾을 수 없습니다. | 404 |
| LA002 | LEARNING_AXIS_NAME_BLANK | 축 이름은 비어 있을 수 없습니다. | 400 |
| LA003 | LEARNING_AXIS_DUPLICATE_NAME | 동일한 이름의 축이 이미 존재합니다. | 409 |
| LA004 | LEARNING_AXIS_REORDER_MISMATCH | 순서 변경 id 목록이 현재 축 id 집합과 일치하지 않습니다. | 400 |
| LA005 | LEARNING_AXIS_ALREADY_DELETED | 이미 삭제된 축입니다. | 400 |

### AxisTopic (LT···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| LT001 | LEARNING_AXIS_TOPIC_NOT_FOUND | 주제를 찾을 수 없습니다. | 404 |
| LT002 | LEARNING_AXIS_TOPIC_NAME_BLANK | 주제 이름은 비어 있을 수 없습니다. | 400 |
| LT003 | LEARNING_AXIS_TOPIC_REORDER_MISMATCH | 전달된 주제 id 목록이 현재 주제 집합과 일치하지 않습니다. | 400 |

### AxisRoadmapNode (RN···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| RN001 | ROADMAP_NODE_NOT_FOUND | Roadmap 노드를 찾을 수 없습니다. | 404 |
| RN002 | ROADMAP_NODE_TITLE_BLANK | Roadmap 노드 title은 비어 있을 수 없습니다. | 400 |
| RN003 | ROADMAP_NODE_BODY_BLANK | Roadmap 노드 body는 비어 있을 수 없습니다. | 400 |
| RN004 | ROADMAP_NODE_ORDER_MISMATCH | 전달된 Roadmap 노드 id 목록이 현재 노드 집합과 일치하지 않습니다. | 400 |

### AxisSelection (AS···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| AS001 | AXIS_SELECTION_NOT_FOUND | Selection을 찾을 수 없습니다. | 404 |
| AS002 | AXIS_SELECTION_NAME_BLANK | Selection 이름은 비어 있을 수 없습니다. | 400 |
| AS003 | AXIS_SELECTION_NAME_ALREADY_EXISTS | 동일한 이름의 Selection이 이미 존재합니다. | 409 |

### AxisSelectionNode (SN···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| SN001 | SELECTION_NODE_NOT_FOUND | Selection 노드를 찾을 수 없습니다. | 404 |
| SN002 | SELECTION_NODE_TITLE_BLANK | Selection 노드 title은 비어 있을 수 없습니다. | 400 |
| SN003 | SELECTION_NODE_BODY_BLANK | Selection 노드 body는 비어 있을 수 없습니다. | 400 |
| SN004 | SELECTION_NODE_ORDER_MISMATCH | 전달된 Selection 노드 id 목록이 현재 노드 집합과 일치하지 않습니다. | 400 |

### LearningMaterial (LM···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| LM001 | LEARNING_MATERIAL_NOT_FOUND | 학습 자료를 찾을 수 없습니다. | 404 |
| LM002 | LEARNING_MATERIAL_PROFICIENCY_UNRATED_NOT_ALLOWED | 숙련도는 UNRATED로 되돌릴 수 없습니다. | 400 |
| LM005 | LEARNING_MATERIAL_NAME_BLANK | 자료명은 비어 있을 수 없습니다. | 400 |
| LM006 | LEARNING_MATERIAL_TYPE_REQUIRED | 자료 유형을 선택해야 합니다. | 400 |
| LM007 | LEARNING_MATERIAL_TYPE_INVALID | 알 수 없는 자료 유형입니다. | 400 |

### TopicMaterial (TM···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| TM001 | LEARNING_TOPIC_MATERIAL_ALREADY_LINKED | 이미 연결된 주제입니다. | 409 |
| TM002 | LEARNING_TOPIC_MATERIAL_NOT_LINKED | 연결되지 않은 주제입니다. | 404 |

### TopicRevision (TR···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| TR001 | REVISION_REASON_NOT_FOUND | 선택한 수정 이유를 찾을 수 없거나 비활성 상태입니다. | 404 |

### UserSchedule (SCHEDULE···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| SCHEDULE001 | SCHEDULE_NOT_FOUND | 스케줄을 찾을 수 없습니다. | 404 |
| SCHEDULE002 | SCHEDULE_HISTORY_LIMIT_EXCEEDED | 스케줄 이력 조회 한도를 초과했습니다. | 400 |
| SCHEDULE003 | MODE_60D_CLAMPED | 입력이 60일을 초과하여 MODE_60D로 자동 조정되었습니다. | 200 (정보성) | *(M4 CARD E1 · `wasClamped=true` 응답 플래그와 동반)* |

### 파일 · 스토리지 (FILE···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| FILE001 | FILE_EMPTY | 파일이 비어있습니다. | 400 |
| FILE002 | FILE_UNSUPPORTED_EXTENSION | 지원하지 않는 확장자입니다. | 400 |
| FILE003 | FILE_UPLOAD_FAIL | 파일 업로드에 실패했습니다. | 503 |
| FILE004 | FILE_DELETE_FAIL | 파일 삭제에 실패했습니다. | 503 |
| FILE005 | PAYLOAD_TOO_LARGE | 허용된 최대 업로드 크기를 초과했습니다. | 413 |

### Library (LIBRARY···)
| 코드 | 이름 | 메시지 | HTTP |
| --- | --- | --- | --- |
| LIBRARY001 | ALREADY_EXISTS | 이미 존재하는 리소스입니다. | 409 |

---

## 알려진 정리 후보
- **DECK006 중복** — `DECK_AUTO_CREATE_FAILED`와 `DECK_HIERARCHY_CYCLE`이 같은 코드. 하나를 DECK007로 재할당 필요.
- **USER005 메시지 재검토** — 보안상 USER001과 동일 메시지지만 코드는 별도. 프론트 처리 시 혼동 가능.
- **CARD041 (`CARD_TOPIC_ID_DEPRECATED`)** — soft-deprecate 안내용 · v1 릴리스 이후 `card.topic_id` 컬럼 DROP 시점에 함께 삭제 예정.
- **폐기 완료된 코드** (M4 반영): 기존 `CARD_MAX_VIEW_EXCEEDED`·`CARD_MAX_DURATION_EXCEEDED` 등 OnFieldBudget 관련 코드가 있었다면 (`ErrorCode.java` 소스 확인 필요) M4에서 폐기 예정.

---

## M4에서 반영된 변경 요약

- **신규**: `CARD_AXIS_ID_REQUIRED` (400) · `CARD_TOPIC_ID_DEPRECATED` (400 경고) · `MODE_60D_CLAMPED` (200 정보성)
- **의미 변화**: `ArchiveReason` enum 값이 3개로 재정의 (`MANUAL` / `SCHEDULE_EXHAUSTED` / `MODE_DOWNGRADED`) — 별도 ErrorCode는 아니지만 도메인 검증 로직에서 참조. `ArchiveReason` 자체는 도메인 예외 아닌 상태값.
- **폐기 상정**: OnFieldBudget 관련 예외 코드 (있었다면). ErrorCode enum 실 소스 확인 필요.

---

*최신 갱신: 2026-07-21 · **M4 반영** — Card 재편 3종 코드 추가 · 소스: `Common/Exception/ErrorCode/ErrorCode.java`*
