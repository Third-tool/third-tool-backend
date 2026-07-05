package com.example.thirdtool.Common.Exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
@Getter
@RequiredArgsConstructor           // ✅ 생성자는 Lombok에게만 위임 — 수동 생성자 제거
public enum ErrorCode {

    // ─── 공통 ─────────────────────────────────────────────
    INVALID_INPUT("C001",    "잘못된 입력 값입니다.",       HttpStatus.BAD_REQUEST),
    NOT_FOUND("C002",        "데이터를 찾을 수 없습니다.",   HttpStatus.NOT_FOUND),

    // ─── User ─────────────────────────────────────────────
    USER_NOT_FOUND("USER001",          "사용자를 찾을 수 없습니다.",       HttpStatus.NOT_FOUND),
    UNAUTHORIZED("USER002",            "인증이 필요합니다.",               HttpStatus.UNAUTHORIZED),
    USER_ALREADY_EXISTS("USER003",     "이미 가입된 사용자입니다.",         HttpStatus.CONFLICT),
    USER_LOCKED("USER004",             "잠긴 계정입니다.",                  HttpStatus.FORBIDDEN),
    // 로그인 실패는 USER_NOT_FOUND와 PASSWORD_NOT_MATCHED로 코드만 구분.
    // 보안상 외부 응답 메시지는 USER_NOT_FOUND와 동일하게 유지한다.
    PASSWORD_NOT_MATCHED("USER005",    "사용자를 찾을 수 없습니다.",       HttpStatus.UNAUTHORIZED),
    USER_IS_SOCIAL("USER006",          "소셜 계정은 자체 로그인을 사용할 수 없습니다.", HttpStatus.BAD_REQUEST),
    SOCIAL_PROVIDER_NOT_SUPPORTED("USER007", "지원하지 않는 소셜 제공자입니다.", HttpStatus.BAD_REQUEST),
    SOCIAL_MEMBER_ALREADY_LINKED("USER008",  "이미 연동된 소셜 계정입니다.",     HttpStatus.CONFLICT),

    // ─── Refresh Token (Story 2-1) ────────────────────────
    REFRESH_TOKEN_INVALID("AUTH101",   "유효하지 않은 Refresh Token입니다.",               HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("AUTH102", "Refresh Token이 화이트리스트에 존재하지 않습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_REUSED("AUTH103",    "이미 사용된 Refresh Token이 재사용 시도되었습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_MISSING("AUTH104",   "Refresh Token이 요청에 포함되지 않았습니다.",       HttpStatus.BAD_REQUEST),

    // ─── Access Token / Auth (Story 3-2) ───────────────────
    AUTH_TOKEN_MISSING("AUTH001",   "인증 토큰이 누락되었습니다.",                    HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_EXPIRED("AUTH002",   "인증 토큰이 만료되었습니다.",                    HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_INVALID("AUTH003",   "인증 토큰이 유효하지 않습니다 (위조/서명 불일치).", HttpStatus.UNAUTHORIZED),
    AUTH_USER_NOT_FOUND("AUTH004",  "토큰의 사용자가 더 이상 존재하지 않습니다.",      HttpStatus.UNAUTHORIZED),
    AUTH_FORBIDDEN("AUTH005",       "접근 권한이 없습니다.",                           HttpStatus.FORBIDDEN),

    // ─── Deck ─────────────────────────────────────────────
    DECK_NOT_FOUND("DECK001", "덱을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DECK_ALREADY_DELETED("DECK002", "이미 삭제된 덱입니다.", HttpStatus.BAD_REQUEST),
    DECK_NAME_DUPLICATE("DECK003", "이미 존재하는 덱 이름입니다.", HttpStatus.CONFLICT),
    DECK_NAME_BLANK("DECK004", "덱 이름은 비어 있을 수 없습니다.", HttpStatus.BAD_REQUEST),
    DECK_FORBIDDEN("DECK005", "본인의 덱이 아닙니다.", HttpStatus.FORBIDDEN),
    DECK_AUTO_CREATE_FAILED("DECK006", "Deck 자동 생성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // ─── Card ─────────────────────────────────────────────
    CARD_NOT_FOUND("CARD001",                  "카드를 찾을 수 없습니다.",                                                     HttpStatus.NOT_FOUND),
    CARD_IMAGE_NOT_FOUND("CARD002",            "카드 이미지를 찾을 수 없습니다.",                                              HttpStatus.NOT_FOUND),
    CARD_RANK_EMPTY("CARD003",                 "해당 랭크의 카드가 없습니다.",                                                 HttpStatus.NOT_FOUND),
    CARD_FEEDBACK_NOT_ALLOWED("CARD004",       "해당 카드에는 피드백을 줄 수 없습니다.",                                       HttpStatus.CONFLICT),
    CARD_RESET_NOT_ALLOWED("CARD005",          "영구 모드 카드만 초기화할 수 있습니다.",                                       HttpStatus.CONFLICT),
    CARD_MAIN_NOTE_EMPTY("CARD010",            "MainNote는 텍스트 또는 이미지 중 최소 하나를 포함해야 합니다.",                HttpStatus.BAD_REQUEST),
    CARD_SUMMARY_EMPTY("CARD020",              "Summary는 비어 있을 수 없습니다.",                                             HttpStatus.BAD_REQUEST),
    CARD_SUMMARY_SENTENCE_OUT_OF_RANGE("CARD021", "Summary는 1~3문장 범위여야 합니다.",                                        HttpStatus.BAD_REQUEST),
    CARD_KEYWORD_BLANK("CARD030",              "키워드 단서는 비어 있을 수 없습니다.",                                         HttpStatus.BAD_REQUEST),
    CARD_KEYWORD_MIN_REQUIRED("CARD031",       "카드는 키워드를 최소 1개 이상 가져야 합니다.",                                 HttpStatus.BAD_REQUEST),
    CARD_KEYWORD_NOT_FOUND("CARD032",          "해당 키워드 단서를 찾을 수 없습니다.",                                        HttpStatus.NOT_FOUND),
    CARD_KEYWORD_LAST_CANNOT_REMOVE("CARD033", "마지막 키워드는 제거할 수 없습니다. 카드는 최소 1개의 키워드를 유지해야 합니다.", HttpStatus.BAD_REQUEST),

    // ─── Tag ──────────────────────────────────────────────
    TAG_VALUE_BLANK("TAG001",           "태그 이름은 비어 있을 수 없습니다.",                    HttpStatus.BAD_REQUEST),
    CARD_TAG_LIMIT_EXCEEDED("TAG002",   "카드당 태그는 최대 3개까지만 연결할 수 있습니다.",      HttpStatus.BAD_REQUEST),
    CARD_TAG_NOT_FOUND("TAG003",        "해당 태그를 카드에서 찾을 수 없습니다.",                HttpStatus.NOT_FOUND),
    CARD_TAG_ALREADY_EXISTS("TAG004",   "이미 연결된 태그입니다.",                              HttpStatus.CONFLICT),

    // ─── Review ───────────────────────────────────────────
    REVIEW_SESSION_NOT_FOUND("REVIEW001", "리뷰 세션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    REVIEW_DECK_HAS_NO_CARDS("REVIEW002", "카드가 없는 덱으로는 리뷰 세션을 시작할 수 없습니다.", HttpStatus.BAD_REQUEST),
    REVIEW_SESSION_ALREADY_FINISHED("REVIEW003", "이미 종료된 리뷰 세션입니다.", HttpStatus.BAD_REQUEST),
    REVIEW_COMPARING_REQUIRED("REVIEW004", "현재 카드를 먼저 비교(COMPARING) 단계로 전환해야 다음으로 이동할 수 있습니다.", HttpStatus.BAD_REQUEST),
    REVIEW_SESSION_FORBIDDEN("REVIEW005", "본인의 리뷰 세션이 아닙니다.", HttpStatus.FORBIDDEN),
    // ─── Daily Batch (REV E1 · M5) ─────────────────────────
    DAILY_BATCH_NOT_FOUND("DAILY001",   "일일 학습 배치를 찾을 수 없습니다.",                 HttpStatus.NOT_FOUND),
    DAILY_BATCH_CLOSED("DAILY002",      "오늘 학습 세션이 종료되었습니다.",                    HttpStatus.CONFLICT),
    DAILY_BATCH_FORBIDDEN("DAILY003",   "본인의 일일 배치가 아닙니다.",                        HttpStatus.FORBIDDEN),
    DAILY_BATCH_CLOSED_FOR_NEW_SESSION("DAILY004", "closed batch로는 새 세션을 시작할 수 없습니다.", HttpStatus.CONFLICT),
    DAILY_BATCH_HAS_NO_CARDS("DAILY005", "오늘 학습할 카드가 없습니다.",                       HttpStatus.BAD_REQUEST),

    // ─── LearningFacade ───────────────────────────────────
    LEARNING_FACADE_NOT_FOUND("LF001",       "LearningFacade를 찾을 수 없습니다.",   HttpStatus.NOT_FOUND),
    LEARNING_FACADE_ALREADY_EXISTS("LF002",  "이미 LearningFacade가 존재합니다.",    HttpStatus.CONFLICT),
    LEARNING_FACADE_CONCEPT_BLANK("LF003",   "컨셉 값은 비어있을 수 없습니다.",     HttpStatus.BAD_REQUEST),
    LEARNING_FACADE_FORBIDDEN("LF004",       "본인의 LearningFacade가 아닙니다.",    HttpStatus.FORBIDDEN),

    // ─── LearningFacade.concepts[] (Story-LT-E1-S2 / S5) ──
    LEARNING_FACADE_CONCEPTS_SIZE_INVALID("LF005", "컨셉은 1~5개 이내여야 합니다.",   HttpStatus.BAD_REQUEST),
    LEARNING_FACADE_CONCEPT_DUPLICATE("LF006",     "이미 등록된 컨셉입니다.",          HttpStatus.CONFLICT),
    LEARNING_FACADE_CONCEPT_TOO_LONG("LF007",      "컨셉 값은 100자 이내여야 합니다.", HttpStatus.BAD_REQUEST),
    LEARNING_FACADE_CONCEPTS_REORDER_MISMATCH("LF008",
            "전달된 concept id 목록이 현재 컨셉 집합과 일치하지 않습니다.",       HttpStatus.BAD_REQUEST),

    // ─── LearningLayer (Story-LT-E2-S1~S5) ────────────────
    LEARNING_LAYER_NOT_FOUND("LL001",           "Layer를 찾을 수 없습니다.",                                        HttpStatus.NOT_FOUND),
    LEARNING_LAYER_NAME_BLANK("LL002",          "Layer 이름은 비어 있을 수 없습니다.",                              HttpStatus.BAD_REQUEST),
    LEARNING_LAYER_DUPLICATE_NAME("LL003",      "동일한 이름의 Layer가 이미 존재합니다.",                          HttpStatus.CONFLICT),
    LEARNING_LAYER_ALREADY_DELETED("LL004",     "이미 삭제된 Layer입니다.",                                        HttpStatus.BAD_REQUEST),
    LEARNING_LAYER_HAS_ACTIVE_AXES("LL005",     "활성 축이 있는 Layer는 삭제할 수 없습니다.",                      HttpStatus.CONFLICT),
    LEARNING_LAYER_REORDER_MISMATCH("LL006",    "전달된 Layer id 목록이 현재 Layer id 집합과 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    LEARNING_LAYER_NAME_TOO_LONG("LL007",       "Layer 이름은 100자 이내여야 합니다.",                              HttpStatus.BAD_REQUEST),
    LAYER_HAS_NO_AXES("LL008",                  "Layer에 활성 축이 없어 리뷰 세션을 시작할 수 없습니다.",           HttpStatus.BAD_REQUEST),
    LAYER_REVIEW_ACCESS_DENIED("LL009",         "본인의 Layer가 아닙니다.",                                        HttpStatus.FORBIDDEN),
    REVIEW_SCOPE_INVALID("LL010",               "Review 스코프 값이 유효하지 않습니다.",                            HttpStatus.BAD_REQUEST),

    // ─── LearningAxis ─────────────────────────────────────
    LEARNING_AXIS_NOT_FOUND("LA001",              "세부 축을 찾을 수 없습니다.",                             HttpStatus.NOT_FOUND),
    LEARNING_AXIS_NAME_BLANK("LA002",             "축 이름은 비어 있을 수 없습니다.",                        HttpStatus.BAD_REQUEST),
    LEARNING_AXIS_DUPLICATE_NAME("LA003",         "동일한 이름의 축이 이미 존재합니다.",                     HttpStatus.CONFLICT),
    LEARNING_AXIS_REORDER_MISMATCH("LA004",       "순서 변경 id 목록이 현재 축 id 집합과 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    LEARNING_AXIS_ALREADY_DELETED("LA005",        "이미 삭제된 축입니다.",                                   HttpStatus.BAD_REQUEST),

    // ─── AxisTopic (v2) ────────────────────────────────────
    LEARNING_AXIS_TOPIC_NOT_FOUND("LT001",        "주제를 찾을 수 없습니다.",                                HttpStatus.NOT_FOUND),
    LEARNING_AXIS_TOPIC_NAME_BLANK("LT002",       "주제 이름은 비어 있을 수 없습니다.",                      HttpStatus.BAD_REQUEST),
    LEARNING_AXIS_TOPIC_REORDER_MISMATCH("LT003", "전달된 주제 id 목록이 현재 주제 집합과 일치하지 않습니다.", HttpStatus.BAD_REQUEST),

    // ─── AxisRoadmapNode (Story-LT-E3-S3-6~S3-8, 이슈 #15) ─
    // 이슈 #6의 axis_roadmap.content TEXT 통짜 SUPERSEDED — 챕터 노드 first-class로 재편.
    ROADMAP_NODE_NOT_FOUND("RN001",       "Roadmap 노드를 찾을 수 없습니다.",                              HttpStatus.NOT_FOUND),
    ROADMAP_NODE_TITLE_BLANK("RN002",     "Roadmap 노드 title은 비어 있을 수 없습니다.",                    HttpStatus.BAD_REQUEST),
    ROADMAP_NODE_BODY_BLANK("RN003",      "Roadmap 노드 body는 비어 있을 수 없습니다.",                     HttpStatus.BAD_REQUEST),
    ROADMAP_NODE_ORDER_MISMATCH("RN004",  "전달된 Roadmap 노드 id 목록이 현재 노드 집합과 일치하지 않습니다.", HttpStatus.BAD_REQUEST),

    // ─── AxisSelection (Story-LT-E3-S3-9~S3-11, 이슈 #16 · 이슈 #11 계승) ─
    // 컨테이너 정책: name UNIQUE per axis, created_at DESC, hard delete.
    AXIS_SELECTION_NOT_FOUND("AS001",           "Selection을 찾을 수 없습니다.",                                    HttpStatus.NOT_FOUND),
    AXIS_SELECTION_NAME_BLANK("AS002",          "Selection 이름은 비어 있을 수 없습니다.",                          HttpStatus.BAD_REQUEST),
    AXIS_SELECTION_NAME_ALREADY_EXISTS("AS003", "동일한 이름의 Selection이 이미 존재합니다.",                        HttpStatus.CONFLICT),

    // ─── AxisSelectionNode (Story-LT-E3-S3-9~S3-11, 이슈 #16) ─
    // 컨테이너 hard delete 정책 계승 — 자식 노드 soft delete 없음.
    SELECTION_NODE_NOT_FOUND("SN001",       "Selection 노드를 찾을 수 없습니다.",                              HttpStatus.NOT_FOUND),
    SELECTION_NODE_TITLE_BLANK("SN002",     "Selection 노드 title은 비어 있을 수 없습니다.",                    HttpStatus.BAD_REQUEST),
    SELECTION_NODE_BODY_BLANK("SN003",      "Selection 노드 body는 비어 있을 수 없습니다.",                     HttpStatus.BAD_REQUEST),
    SELECTION_NODE_ORDER_MISMATCH("SN004",  "전달된 Selection 노드 id 목록이 현재 노드 집합과 일치하지 않습니다.", HttpStatus.BAD_REQUEST),

    // ─── LearningMaterial ─────────────────────────────────
    LEARNING_MATERIAL_NOT_FOUND("LM001",                          "학습 자료를 찾을 수 없습니다.",                                  HttpStatus.NOT_FOUND),
    LEARNING_MATERIAL_PROFICIENCY_UNRATED_NOT_ALLOWED("LM002",    "숙련도는 UNRATED로 되돌릴 수 없습니다.",                         HttpStatus.BAD_REQUEST),
    LEARNING_MATERIAL_NAME_BLANK("LM005",                         "자료명은 비어 있을 수 없습니다.",                                HttpStatus.BAD_REQUEST),
    LEARNING_MATERIAL_TYPE_REQUIRED("LM006",                      "자료 유형을 선택해야 합니다.",                                   HttpStatus.BAD_REQUEST),
    LEARNING_MATERIAL_TYPE_INVALID("LM007",                       "알 수 없는 자료 유형입니다.",                                    HttpStatus.BAD_REQUEST),

    // ─── TopicMaterial (v2) ────────────────────────────────
    LEARNING_TOPIC_MATERIAL_ALREADY_LINKED("TM001", "이미 연결된 주제입니다.",       HttpStatus.CONFLICT),
    LEARNING_TOPIC_MATERIAL_NOT_LINKED("TM002",     "연결되지 않은 주제입니다.",     HttpStatus.NOT_FOUND),

    // ─── TopicRevision (Epic-003) ─────────────────────────
    REVISION_REASON_NOT_FOUND("TR001",              "선택한 수정 이유를 찾을 수 없거나 비활성 상태입니다.", HttpStatus.NOT_FOUND),

    // ─── LearningFacade Suggestion (AI) ───────────────────
    // Story 1-3: AI 호출 실패는 사용자에게 5xx로 노출되지 않고
    // 상위 Service가 catch → 빈 목록 + suggestionsAvailable=false로 변환한다(ADR010).
    // HttpStatus 값은 fallback이 적용되지 않을 때(예: 운영 디버깅 우회 호출)의 기본 매핑.
    LEARNING_FACADE_SUGGESTION_TIMEOUT("LF_SUGGEST_001",
            "AI 제안 요청이 시간 내 완료되지 않았습니다.",                HttpStatus.SERVICE_UNAVAILABLE),
    LEARNING_FACADE_SUGGESTION_INVALID_RESPONSE("LF_SUGGEST_002",
            "AI 응답 형식이 유효하지 않습니다.",                          HttpStatus.BAD_GATEWAY),
    LEARNING_FACADE_SUGGESTION_AUTH_FAILED("LF_SUGGEST_003",
            "AI 인증 정보가 유효하지 않습니다.",                          HttpStatus.SERVICE_UNAVAILABLE),

    // ─── Deck (확장) ──────────────────────────────────────
    DECK_HIERARCHY_CYCLE("DECK006", "덱 계층 구조에 순환 참조가 발생했습니다.", HttpStatus.BAD_REQUEST),

    // ─── UserSchedule ─────────────────────────────────────
    SCHEDULE_NOT_FOUND("SCHEDULE001",            "스케줄을 찾을 수 없습니다.",                  HttpStatus.NOT_FOUND),
    SCHEDULE_HISTORY_LIMIT_EXCEEDED("SCHEDULE002", "스케줄 이력 조회 한도를 초과했습니다.",     HttpStatus.BAD_REQUEST),
    USER_SCHEDULE_INPUT_TOO_SHORT("SCHEDULE003",   "학습 목표 일수는 1일 이상이어야 합니다.",   HttpStatus.BAD_REQUEST),

            // ─── 파일 / 스토리지 ──────────────────────────────────
    FILE_EMPTY("FILE001",                   "파일이 비어있습니다.",               HttpStatus.BAD_REQUEST),
    FILE_UNSUPPORTED_EXTENSION("FILE002",   "지원하지 않는 확장자입니다.",        HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAIL("FILE003",             "파일 업로드에 실패했습니다.",         HttpStatus.SERVICE_UNAVAILABLE),
    FILE_DELETE_FAIL("FILE004",             "파일 삭제에 실패했습니다.",           HttpStatus.SERVICE_UNAVAILABLE),
    PAYLOAD_TOO_LARGE("FILE005",            "허용된 최대 업로드 크기를 초과했습니다.", HttpStatus.PAYLOAD_TOO_LARGE),

    // ─── Library ──────────────────────────────────────────
    // Story-5-3: ACCESS_DENIED("LIBRARY002") 제거 — 사용처 0건 + AUTH_FORBIDDEN(AUTH005)과 의미 중복.
    ALREADY_EXISTS("LIBRARY001",  "이미 존재하는 리소스입니다.",  HttpStatus.CONFLICT),

    // ─── 공통 fallback (Story 3-1) ────────────────────────
    INTERNAL_ERROR("C500",        "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    // ─── 필드 ─────────────────────────────────────────────
    private final String     code;
    private final String     message;
    private final HttpStatus status;
    // ✅ getter는 @Getter가 자동 생성 — 수동 정의 불필요
}
