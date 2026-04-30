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
    USER_NOT_FOUND("USER001",  "사용자를 찾을 수 없습니다.",  HttpStatus.NOT_FOUND),
    UNAUTHORIZED("USER002",    "인증이 필요합니다.",          HttpStatus.UNAUTHORIZED),

    // ─── Deck ─────────────────────────────────────────────
    DECK_NOT_FOUND("DECK001", "덱을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DECK_ALREADY_DELETED("DECK002", "이미 삭제된 덱입니다.", HttpStatus.BAD_REQUEST),
    DECK_NAME_DUPLICATE("DECK003", "이미 존재하는 덱 이름입니다.", HttpStatus.CONFLICT),
    DECK_NAME_BLANK("DECK004", "덱 이름은 비어 있을 수 없습니다.", HttpStatus.BAD_REQUEST),
    DECK_FORBIDDEN("DECK005", "본인의 덱이 아닙니다.", HttpStatus.FORBIDDEN),

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

    // ─── LearningFacade ───────────────────────────────────
    LEARNING_FACADE_NOT_FOUND("LF001",       "LearningFacade를 찾을 수 없습니다.",   HttpStatus.NOT_FOUND),
    LEARNING_FACADE_ALREADY_EXISTS("LF002",  "이미 LearningFacade가 존재합니다.",    HttpStatus.CONFLICT),
    LEARNING_FACADE_CONCEPT_BLANK("LF003",   "직업적 컨셉은 비어 있을 수 없습니다.", HttpStatus.BAD_REQUEST),
    LEARNING_FACADE_FORBIDDEN("LF004",       "본인의 LearningFacade가 아닙니다.",    HttpStatus.FORBIDDEN),

    // ─── LearningAxis ─────────────────────────────────────
    LEARNING_AXIS_NOT_FOUND("LA001",              "세부 축을 찾을 수 없습니다.",                             HttpStatus.NOT_FOUND),
    LEARNING_AXIS_NAME_BLANK("LA002",             "축 이름은 비어 있을 수 없습니다.",                        HttpStatus.BAD_REQUEST),
    LEARNING_AXIS_DUPLICATE_NAME("LA003",         "동일한 이름의 축이 이미 존재합니다.",                     HttpStatus.CONFLICT),
    LEARNING_AXIS_REORDER_MISMATCH("LA004",       "순서 변경 id 목록이 현재 축 id 집합과 일치하지 않습니다.", HttpStatus.BAD_REQUEST),

    // ─── AxisTopic (v2) ────────────────────────────────────
    LEARNING_AXIS_TOPIC_NOT_FOUND("LT001",        "주제를 찾을 수 없습니다.",           HttpStatus.NOT_FOUND),
    LEARNING_AXIS_TOPIC_NAME_BLANK("LT002",       "주제 이름은 비어 있을 수 없습니다.", HttpStatus.BAD_REQUEST),

    // ─── LearningMaterial ─────────────────────────────────
    LEARNING_MATERIAL_NOT_FOUND("LM001",                          "학습 자료를 찾을 수 없습니다.",                                  HttpStatus.NOT_FOUND),
    LEARNING_MATERIAL_PROFICIENCY_UNRATED_NOT_ALLOWED("LM002",    "숙련도는 UNRATED로 되돌릴 수 없습니다.",                         HttpStatus.BAD_REQUEST),
    LEARNING_MATERIAL_NAME_BLANK("LM005",                         "자료명은 비어 있을 수 없습니다.",                                HttpStatus.BAD_REQUEST),
    LEARNING_MATERIAL_TYPE_REQUIRED("LM006",                      "자료 유형(Top-down / Bottom-up)을 선택해야 합니다.",              HttpStatus.BAD_REQUEST),

    // ─── TopicMaterial (v2) ────────────────────────────────
    LEARNING_TOPIC_MATERIAL_ALREADY_LINKED("TM001", "이미 연결된 주제입니다.",       HttpStatus.CONFLICT),
    LEARNING_TOPIC_MATERIAL_NOT_LINKED("TM002",     "연결되지 않은 주제입니다.",     HttpStatus.NOT_FOUND),

    // ─── Deck (확장) ──────────────────────────────────────
    DECK_HIERARCHY_CYCLE("DECK006", "덱 계층 구조에 순환 참조가 발생했습니다.", HttpStatus.BAD_REQUEST),

    // ─── UserSchedule ─────────────────────────────────────
    SCHEDULE_NOT_FOUND("SCHEDULE001",            "스케줄을 찾을 수 없습니다.",                  HttpStatus.NOT_FOUND),
    SCHEDULE_HISTORY_LIMIT_EXCEEDED("SCHEDULE002", "스케줄 이력 조회 한도를 초과했습니다.",     HttpStatus.BAD_REQUEST),

            // ─── 파일 / 스토리지 ──────────────────────────────────
    FILE_EMPTY("FILE001",                   "파일이 비어있습니다.",               HttpStatus.BAD_REQUEST),
    FILE_UNSUPPORTED_EXTENSION("FILE002",   "지원하지 않는 확장자입니다.",        HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAIL("FILE003",             "파일 업로드에 실패했습니다.",         HttpStatus.SERVICE_UNAVAILABLE),
    FILE_DELETE_FAIL("FILE004",             "파일 삭제에 실패했습니다.",           HttpStatus.SERVICE_UNAVAILABLE),

    // ─── Library ──────────────────────────────────────────
    ALREADY_EXISTS("LIBRARY001",  "이미 존재하는 리소스입니다.",  HttpStatus.CONFLICT),
    ACCESS_DENIED("LIBRARY002",   "접근 권한이 없습니다.",        HttpStatus.FORBIDDEN);

    // ─── 필드 ─────────────────────────────────────────────
    private final String     code;
    private final String     message;
    private final HttpStatus status;
    // ✅ getter는 @Getter가 자동 생성 — 수동 정의 불필요
}
