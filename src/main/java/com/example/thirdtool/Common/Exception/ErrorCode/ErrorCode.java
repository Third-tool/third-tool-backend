package com.example.thirdtool.Common.Exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT("C001", "잘못된 입력 값입니다.", HttpStatus.BAD_REQUEST),
    NOT_FOUND("C002", "데이터를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // User
    USER_NOT_FOUND("USER001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // Deck
    DECK_NOT_FOUND("DECK001", "덱을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DECK_ALREADY_DELETED("DECK002", "이미 삭제된 덱입니다.", HttpStatus.BAD_REQUEST),
    DECK_PARENT_NOT_FOUND("DECK003", "부모 덱이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    DECK_ALGORITHM_REQUIRED("DECK004", "덱에 알고리즘이 필요합니다." , HttpStatus.BAD_REQUEST ),
    INVALID_REQUEST("INVALID001", "하위 덱은 알고리즘을 직접 지정할 수 없습니다. 부모 덱의 알고리즘을 상속받습니다." , HttpStatus.BAD_REQUEST ),

    // Card
    CARD_NOT_FOUND("CARD001", "카드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CARD_IMAGE_NOT_FOUND("CARD002", "카드 이미지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CARD_RANK_EMPTY("CARD003", "해당 랭크의 카드가 없습니다.", HttpStatus.NOT_FOUND),
    CARD_FEEDBACK_NOT_ALLOWED("CARD004", "해당 카드에는 피드백을 줄 수 없습니다.", HttpStatus.CONFLICT),
    CARD_RESET_NOT_ALLOWED("CARD005", "영구 모드 카드만 초기화할 수 있습니다.", HttpStatus.CONFLICT),

    // Rank
    RANK_NOT_FOUND("RANK001", "유저 랭크 값을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CARD_RANK_NOT_FOUND("RANK002", "" , HttpStatus.NOT_FOUND ),

    // 파일/스토리지
    FILE_EMPTY("FILE001", "파일이 비어있습니다.", HttpStatus.BAD_REQUEST),
    FILE_UNSUPPORTED_EXTENSION("FILE002", "지원하지 않는 확장자입니다.", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAIL("FILE003", "파일 업로드에 실패했습니다.", HttpStatus.SERVICE_UNAVAILABLE),
    FILE_DELETE_FAIL("FILE004", "파일 삭제에 실패했습니다.", HttpStatus.SERVICE_UNAVAILABLE),
    DECK_HIERARCHY_CYCLE("FILE005", "파일 계층 관계에 문제가 생겼습니다." , HttpStatus.BAD_REQUEST );

    private final String code;
    private final String message;
    private final HttpStatus status;
}