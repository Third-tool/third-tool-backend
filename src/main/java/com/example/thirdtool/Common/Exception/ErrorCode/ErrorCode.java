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
    DECK_NOT_FOUND("DECK001",           "덱을 찾을 수 없습니다.",                                                              HttpStatus.NOT_FOUND),
    DECK_ALREADY_DELETED("DECK002",     "이미 삭제된 덱입니다.",                                                               HttpStatus.BAD_REQUEST),
    DECK_PARENT_NOT_FOUND("DECK003",    "부모 덱이 존재하지 않습니다.",                                                        HttpStatus.NOT_FOUND),
    DECK_ALGORITHM_REQUIRED("DECK004",  "덱에 알고리즘이 필요합니다.",                                                         HttpStatus.BAD_REQUEST),
    INVALID_REQUEST("INVALID001",       "하위 덱은 알고리즘을 직접 지정할 수 없습니다. 부모 덱의 알고리즘을 상속받습니다.",    HttpStatus.BAD_REQUEST),
    DECK_ALGORITHM_NOT_SET("DECK005",   "Deck의 알고리즘 타입이 설정되지 않았습니다. Deck 생성 시 알고리즘을 반드시 지정하세요.", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_ALGORITHM("DECK006",    "지원하지 않는 알고리즘 타입입니다.",                                                  HttpStatus.BAD_REQUEST),
    ALGORITHM_BEAN_NOT_FOUND("DECK007", "스프링 컨텍스트에서 해당 알고리즘 빈을 찾을 수 없습니다.",                            HttpStatus.INTERNAL_SERVER_ERROR),
    DECK_HIERARCHY_CYCLE("DECK008",     "덱 계층 관계에 순환이 발생했습니다.",                                                 HttpStatus.BAD_REQUEST),

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

    // ─── Rank ─────────────────────────────────────────────
    RANK_NOT_FOUND("RANK001",       "유저 랭크 값을 찾을 수 없습니다.",    HttpStatus.NOT_FOUND),
    CARD_RANK_NOT_FOUND("RANK002",  "카드 랭크를 찾을 수 없습니다.",       HttpStatus.NOT_FOUND),
    INVALID_RANK_RANGE("RANK003",   "랭크 구간이 겹치거나 연결되지 않았습니다.", HttpStatus.BAD_REQUEST),

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
}}